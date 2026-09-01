package dev.mintychochip.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.domain.model.JobRecord;
import dev.mintychochip.domain.model.PlayerJobStateRecord;
import dev.mintychochip.domain.repository.PlayerJobStateRepository;
import dev.mintychochip.domain.repository.PlayerJobStateRepository.Key;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Drives shipped {@link WriteBackPlayerJobStateRepositoryImpl}: flush re-queue must not clobber
 * newer XP; loadAllForJob pending-delete keys on job key (not player id).
 */
class WriteBackPlayerJobStateIntegrityTest {

  private MemoryDelegate delegate;
  private WriteBackPlayerJobStateRepositoryImpl writeBack;

  @BeforeEach
  void setUp() {
    delegate = new MemoryDelegate();
    writeBack = WriteBackPlayerJobStateRepositoryImpl.createUnscheduled(delegate, 50, 50);
  }

  @Test
  void requeueFailedBatchKeepsHigherExperienceAlreadyStaged() {
    JobRecord job = job("modularjobs:miner");
    Key key = new Key("player-1", job.jobKey());
    PlayerJobStateRecord older =
        new PlayerJobStateRecord("player-1", job.jobKey(), job.jobKey(), new BigDecimal("100"));
    PlayerJobStateRecord newer =
        new PlayerJobStateRecord("player-1", job.jobKey(), job.jobKey(), new BigDecimal("150"));

    writeBack.save(newer);
    Map<Key, PlayerJobStateRecord> batch = new HashMap<>();
    batch.put(key, older);
    writeBack.requeueFailedBatch(batch, Set.of());

    PlayerJobStateRecord loaded = writeBack.load("player-1", job.jobKey());
    assertEquals(
        0,
        new BigDecimal("150").compareTo(loaded.experience()),
        "re-queue must not putAll older XP over newer pending");
  }

  @Test
  void requeueFailedBatchRestoresWhenNothingNewer() {
    JobRecord job = job("modularjobs:miner");
    Key key = new Key("player-1", job.jobKey());
    PlayerJobStateRecord only =
        new PlayerJobStateRecord("player-1", job.jobKey(), job.jobKey(), new BigDecimal("40"));
    Map<Key, PlayerJobStateRecord> batch = new HashMap<>();
    batch.put(key, only);
    writeBack.requeueFailedBatch(batch, Set.of());
    assertEquals(
        0, new BigDecimal("40").compareTo(writeBack.load("player-1", job.jobKey()).experience()));
  }

  @Test
  void loadAllForJobPendingDeleteMatchesJobKeyNotPlayerId() {
    JobRecord miner = job("modularjobs:miner");
    JobRecord fisher = job("modularjobs:fisherman");
    // playerId deliberately equals a job key string to catch the old bug
    String playerId = "modularjobs:miner";
    writeBack.save(
        new PlayerJobStateRecord(playerId, miner.jobKey(), miner.jobKey(), new BigDecimal("10")));
    writeBack.save(
        new PlayerJobStateRecord(playerId, fisher.jobKey(), fisher.jobKey(), new BigDecimal("20")));
    writeBack.delete(playerId, miner.jobKey());

    List<PlayerJobStateRecord> forMiner = writeBack.loadAllForJob(miner.jobKey(), 100);
    assertTrue(forMiner.isEmpty(), "pending delete for miner must remove miner rows");

    List<PlayerJobStateRecord> forFisher = writeBack.loadAllForJob(fisher.jobKey(), 100);
    assertEquals(1, forFisher.size());
    assertEquals(fisher.jobKey(), forFisher.getFirst().jobKey());
  }

  @Test
  void preferHigherExperienceChoosesMax() {
    JobRecord job = job("modularjobs:miner");
    PlayerJobStateRecord low =
        new PlayerJobStateRecord("p", job.jobKey(), job.jobKey(), new BigDecimal("5"));
    PlayerJobStateRecord high =
        new PlayerJobStateRecord("p", job.jobKey(), job.jobKey(), new BigDecimal("9"));
    assertEquals(high, writeBack.preferHigherExperience(low, high));
    assertEquals(high, writeBack.preferHigherExperience(high, low));
  }

  private static @NotNull JobRecord job(@NotNull String key) {
    return new JobRecord(key, key, "desc", 100, "x", Map.of(), null);
  }

  private static final class MemoryDelegate implements PlayerJobStateRepository {
    private final Map<String, PlayerJobStateRecord> store = new ConcurrentHashMap<>();

    private static @NotNull String cacheKey(@NotNull String playerId, @NotNull String jobKey) {
      return playerId + "\0" + jobKey;
    }

    @Override
    public boolean save(@NotNull PlayerJobStateRecord record) {
      store.put(cacheKey(record.playerId(), record.jobKey()), record);
      return true;
    }

    @Override
    public @Nullable PlayerJobStateRecord load(@NotNull String playerId, @NotNull String jobKey) {
      return store.get(cacheKey(playerId, jobKey));
    }

    @Override
    public @NotNull List<PlayerJobStateRecord> loadAllForJob(@NotNull String jobKey, int limit) {
      return store.values().stream().filter(r -> jobKey.equals(r.jobKey())).limit(limit).toList();
    }

    @Override
    public @NotNull List<PlayerJobStateRecord> loadAllForPlayer(
        @NotNull String playerId, int limit) {
      return store.values().stream()
          .filter(r -> playerId.equals(r.playerId()))
          .limit(limit)
          .toList();
    }

    @Override
    public boolean delete(@NotNull String playerId, @NotNull String jobKey) {
      return store.remove(cacheKey(playerId, jobKey)) != null;
    }
  }
}
