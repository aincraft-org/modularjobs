package dev.mintychochip.domain;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.mintychochip.domain.model.PlayerJobStateRecord;
import dev.mintychochip.domain.repository.PlayerJobStateRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Write-back caching {@link PlayerJobStateRepository} decorating a relational delegate.
 *
 * <p>All mutations are staged in memory ({@link #save} / {@link #delete}) and applied to the
 * delegate in batches by a scheduled flush, either a Bukkit async fixed-rate task ({@link #create})
 * or manual {@link #flushPending()}. Reads serve from staged state first, then a read cache, then
 * the delegate, so a just-saved value is visible to subsequent reads before it reaches the
 * database.
 *
 * <p>Lifecycle: {@link #create} registers a recurring flush task; {@link #flushPending()} must be
 * invoked before the underlying {@link ConnectionSource} shuts down (wired via {@code
 * PluginResources.onFlush}) so all staged writes drain. It synchronously waits (up to {@value
 * #FLUSH_LOCK_WAIT_MS} ms) for any in-flight flush and rethrows delegate failures so shutdown can
 * fail loudly rather than silently dropping data.
 *
 * <p>Failure semantics: pending operations remain queued until the delegate completes the entire
 * batch successfully. A normal scheduled flush logs a typed write-back failure; {@link
 * #flushPending()} propagates it. Staged writes therefore survive transient database failures
 * unless the plugin stops without a successful flush.
 *
 * <p>Nullability: {@link #load(String, String)} returns {@code null} when the key is staged for
 * delete or absent from pending state and the delegate returns {@code null}.
 */
final class WriteBackPlayerJobStateRepositoryImpl implements PlayerJobStateRepository {

  private static final Logger LOGGER =
      Logger.getLogger(WriteBackPlayerJobStateRepositoryImpl.class.getName());

  private static final Duration CACHE_TIME_TO_LIVE = Duration.ofMinutes(5);

  private static final int CACHE_MAX_SIZE = 1000;

  /** Max wait for scheduled flush to release the lock before disable flush fails loudly. */
  private static final long FLUSH_LOCK_WAIT_MS = 30_000L;

  private final PlayerJobStateRepository delegate;

  private final Cache<Key, PlayerJobStateRecord> readCache =
      Caffeine.newBuilder()
          .expireAfterWrite(CACHE_TIME_TO_LIVE)
          .maximumSize(CACHE_MAX_SIZE)
          .build();

  private final Map<Key, PlayerJobStateRecord> pendingUpserts = new ConcurrentHashMap<>();
  private final Set<Key> pendingDeletes = ConcurrentHashMap.newKeySet();
  private final AtomicBoolean flushing = new AtomicBoolean(false);
  private final ReentrantLock flushLock = new ReentrantLock();
  private final int upsertBatchSize;
  private final int deleteBatchSize;

  private WriteBackPlayerJobStateRepositoryImpl(
      @NotNull PlayerJobStateRepository delegate, int upsertBatchSize, int deleteBatchSize) {
    this.delegate = delegate;
    this.upsertBatchSize = upsertBatchSize;
    this.deleteBatchSize = deleteBatchSize;
  }

  /**
   * Creates a write-back repository and schedules a recurring Bukkit async flush task.
   *
   * @param plugin plugin owning the scheduled task (also its lifecycle)
   * @param delegate underlying relational repository to flush to
   * @param upsertBatchSize max upserts drained per flush cycle
   * @param deleteBatchSize max deletes drained per flush cycle
   * @param rate flush period magnitude
   * @param rateUnit flush period unit
   * @return a repository with a scheduled flush started
   */
  static @NotNull WriteBackPlayerJobStateRepositoryImpl create(
      @NotNull Plugin plugin,
      @NotNull PlayerJobStateRepository delegate,
      int upsertBatchSize,
      int deleteBatchSize,
      long rate,
      @NotNull TimeUnit rateUnit) {
    WriteBackPlayerJobStateRepositoryImpl repository =
        new WriteBackPlayerJobStateRepositoryImpl(delegate, upsertBatchSize, deleteBatchSize);
    Bukkit.getAsyncScheduler()
        .runAtFixedRate(plugin, task -> repository.flush(), 0L, rate, rateUnit);
    return repository;
  }

  /** Test / manual construction without scheduling a Bukkit task. */
  static @NotNull WriteBackPlayerJobStateRepositoryImpl createUnscheduled(
      @NotNull PlayerJobStateRepository delegate, int upsertBatchSize, int deleteBatchSize) {
    return new WriteBackPlayerJobStateRepositoryImpl(delegate, upsertBatchSize, deleteBatchSize);
  }

  private void flush() {
    if (!flushing.compareAndSet(false, true)) {
      return;
    }
    try {
      flushOnce(false);
    } finally {
      flushing.set(false);
    }
  }

  /**
   * Drain all pending player-state writes before ConnectionSource shutdown. Waits with sleep (not
   * busy-spin) for an in-flight scheduled flush.
   */
  void flushPending() {
    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(FLUSH_LOCK_WAIT_MS);
    while (!flushing.compareAndSet(false, true)) {
      if (System.nanoTime() >= deadline) {
        throw new IllegalStateException(
            "Timed out waiting for player job state write-back flush lock after "
                + FLUSH_LOCK_WAIT_MS
                + "ms");
      }
      try {
        Thread.sleep(10L);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(
            "Interrupted waiting for player job state write-back flush", e);
      }
    }
    try {
      while (flushOnce(true)) {
        // drain batches
      }
    } finally {
      flushing.set(false);
    }
  }

  /**
   * Applies one batch of pending deletes and upserts to the delegate.
   *
   * @param rethrow if true, propagate delegate failures (disable path); scheduled flush logs
   * @return true if any work was flushed
   */
  private boolean flushOnce(boolean rethrow) {
    flushLock.lock();
    try {
      Set<Key> batchDeletes = new HashSet<>();
      Iterator<Key> deleteIterator = pendingDeletes.iterator();
      while (deleteIterator.hasNext() && batchDeletes.size() < deleteBatchSize) {
        batchDeletes.add(deleteIterator.next());
      }
      Map<Key, PlayerJobStateRecord> batchUpserts = new HashMap<>();
      Iterator<Entry<Key, PlayerJobStateRecord>> iterator = pendingUpserts.entrySet().iterator();
      while (iterator.hasNext() && batchUpserts.size() < upsertBatchSize) {
        Entry<Key, PlayerJobStateRecord> entry = iterator.next();
        batchUpserts.put(entry.getKey(), entry.getValue());
      }
      if (batchDeletes.isEmpty() && batchUpserts.isEmpty()) {
        return false;
      }
      try {
        for (Key key : batchDeletes) {
          delegate.delete(key.playerId(), key.jobKey());
        }
        batchUpserts.forEach((ignored, record) -> delegate.save(record));
      } catch (dev.mintychochip.repository.WriteBackException failure) {
        if (rethrow) {
          throw failure;
        }
        LOGGER.log(
            Level.SEVERE,
            "Player job state write-back flush failed; retained "
                + batchUpserts.size()
                + " upsert(s) and "
                + batchDeletes.size()
                + " delete(s)",
            failure);
        return true;
      }
      batchDeletes.forEach(pendingDeletes::remove);
      batchUpserts.forEach((key, value) -> pendingUpserts.remove(key, value));
      return true;
    } finally {
      flushLock.unlock();
    }
  }

  /**
   * Re-queue a failed batch without clobbering a newer staged experience value. Uses max-experience
   * merge so concurrent awards that staged higher XP after the batch was taken out of pending are
   * preserved.
   */
  void requeueFailedBatch(
      @NotNull Map<Key, PlayerJobStateRecord> batchUpserts, @NotNull Set<Key> batchDeletes) {
    for (Entry<Key, PlayerJobStateRecord> entry : batchUpserts.entrySet()) {
      pendingUpserts.merge(entry.getKey(), entry.getValue(), this::preferHigherExperience);
    }
    pendingDeletes.addAll(batchDeletes);
  }

  /**
   * Keep the record with the higher absolute experience (@NotNull monotonic awards). On tie keep
   * {@code existing} (already in pending).
   */
  @Contract(pure = true)
  @NotNull
  PlayerJobStateRecord preferHigherExperience(
      @NotNull PlayerJobStateRecord existing, @NotNull PlayerJobStateRecord incoming) {
    BigDecimal existingXp = existing.experience();
    BigDecimal incomingXp = incoming.experience();
    if (incomingXp.compareTo(existingXp) > 0) {
      return incoming;
    }
    return existing;
  }

  @Override
  public boolean save(@NotNull PlayerJobStateRecord record) {
    flushLock.lock();
    try {
      Key key = new Key(record.playerId(), record.jobKey());
      pendingDeletes.remove(key);
      pendingUpserts.put(key, record);
      readCache.put(key, record);
      return true;
    } finally {
      flushLock.unlock();
    }
  }

  @Override
  public @Nullable PlayerJobStateRecord load(@NotNull String playerId, @NotNull String jobKey) {
    Key key = new Key(playerId, jobKey);
    if (pendingDeletes.contains(key)) {
      return null;
    }
    PlayerJobStateRecord record = pendingUpserts.get(key);
    if (record != null) {
      return record;
    }
    return readCache.get(key, ignored -> delegate.load(playerId, jobKey));
  }

  @Override
  public @NotNull List<PlayerJobStateRecord> loadAllForJob(@NotNull String jobKey, int limit) {
    List<PlayerJobStateRecord> base = delegate.loadAllForJob(jobKey, limit);
    Map<Key, PlayerJobStateRecord> merged = new HashMap<>();
    for (PlayerJobStateRecord record : base) {
      merged.put(new Key(record.playerId(), record.jobKey()), record);
    }
    for (Key key : pendingDeletes) {
      if (jobKey.equals(key.jobKey())) {
        merged.remove(key);
      }
    }
    for (var entry : pendingUpserts.entrySet()) {
      Key key = entry.getKey();
      PlayerJobStateRecord record = entry.getValue();
      if (record == null) {
        continue;
      }
      if (jobKey.equals(key.jobKey())) {
        merged.put(key, record);
      }
    }
    List<PlayerJobStateRecord> records = new ArrayList<>(merged.values());
    records.sort(
        Comparator.comparing(
                PlayerJobStateRecord::experience, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(PlayerJobStateRecord::playerId)
            .thenComparing(PlayerJobStateRecord::jobKey));
    return records;
  }

  @Override
  public @NotNull List<PlayerJobStateRecord> loadAllForPlayer(@NotNull String playerId, int limit) {
    List<PlayerJobStateRecord> base = delegate.loadAllForPlayer(playerId, limit);
    Map<Key, PlayerJobStateRecord> merged = new HashMap<>();
    for (PlayerJobStateRecord record : base) {
      Key key = new Key(record.playerId(), record.jobKey());
      merged.put(key, record);
    }
    for (Key key : pendingDeletes) {
      if (playerId.equals(key.playerId())) {
        merged.remove(key);
      }
    }
    for (var entry : pendingUpserts.entrySet()) {
      Key key = entry.getKey();
      PlayerJobStateRecord record = entry.getValue();
      if (record == null) {
        continue;
      }
      if (playerId.equals(key.playerId())) {
        merged.put(key, record);
      }
    }
    List<PlayerJobStateRecord> records = new ArrayList<>(merged.values());
    records.sort(
        Comparator.comparing(
                PlayerJobStateRecord::experience, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(PlayerJobStateRecord::playerId)
            .thenComparing(PlayerJobStateRecord::jobKey));
    return records;
  }

  @Override
  public boolean delete(@NotNull String playerId, @NotNull String jobKey) {
    flushLock.lock();
    try {
      Key key = new Key(playerId, jobKey);
      pendingUpserts.remove(key);
      pendingDeletes.add(key);
      readCache.invalidate(key);
      return true;
    } finally {
      flushLock.unlock();
    }
  }
}
