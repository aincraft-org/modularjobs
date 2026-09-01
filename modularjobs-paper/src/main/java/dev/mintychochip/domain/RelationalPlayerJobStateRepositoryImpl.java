package dev.mintychochip.domain;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.mintychochip.domain.model.JobRecord;
import dev.mintychochip.domain.model.PlayerJobStateRecord;
import dev.mintychochip.domain.repository.PlayerJobStateRepository;
import dev.mintychochip.repository.ConnectionSource;
import dev.mintychochip.repository.SqlStatements;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MySQL-backed {@link PlayerJobStateRepository} for active player job state records.
 *
 * <p>Owns no connection pool itself; it draws connections on demand from the provided shared {@link
 * ConnectionSource} (composition-owned). Each operation checks out one connection and closes it via
 * try-with-resources.
 *
 * <p>Reads are cached in a Caffeine cache (10-minute write expiry, 10k entries) to reduce DB load;
 * {@code loadAll*}-style bulk reads prefer cached entries and only fall back to fresh rows for
 * uncached keys. {@link #save} and {@link #delete} refresh or invalidate the cache accordingly,
 * keeping single-key reads consistent with writes through this instance.
 *
 * <p>Failure semantics: unchecked {@link RuntimeException} wrapping the underlying {@link
 * SQLException} is thrown on any connection or SQL failure; callers must treat a throw as
 * "operation not performed". Nullability: {@link #load(String, String)} returns {@code null} when
 * the player/job pair has no persisted row, when the job is unknown to the in-memory job
 * repository, or when the row is absent.
 *
 * <p>Table naming is parameterized; writes use MySQL {@code ON DUPLICATE KEY UPDATE} semantics, so
 * {@link #save} is an upsert keyed by (player_id, job_key).
 */
final class RelationalPlayerJobStateRepositoryImpl implements PlayerJobStateRepository {

  private static final Duration CACHE_TIME_TO_LIVE = Duration.ofMinutes(10);
  private static final int CACHE_MAXIMUM_SIZE = 10_000;

  private final MemoryJobRepositoryImpl jobRepository;
  private final ConnectionSource connectionSource;
  private final String tableName;
  private final String saveQuery;
  private final String loadQuery;
  private final String loadAllByJobQuery;
  private final String loadAllForPlayerQuery;
  private final String deleteQuery;
  private final Cache<PlayerJobStateRepository.Key, PlayerJobStateRecord> readCache =
      Caffeine.newBuilder()
          .expireAfterWrite(CACHE_TIME_TO_LIVE)
          .maximumSize(CACHE_MAXIMUM_SIZE)
          .build();

  private RelationalPlayerJobStateRepositoryImpl(
      @NotNull MemoryJobRepositoryImpl jobRepository,
      @NotNull ConnectionSource connectionSource,
      @NotNull String tableName) {
    this.jobRepository = jobRepository;
    this.connectionSource = connectionSource;
    this.tableName = tableName;
    this.saveQuery = bindTable(SqlStatements.load("job_progression/save.sql"));
    this.loadQuery = bindTable(SqlStatements.load("job_progression/load.sql"));
    this.loadAllByJobQuery = bindTable(SqlStatements.load("job_progression/load-all-by-job.sql"));
    this.loadAllForPlayerQuery =
        bindTable(SqlStatements.load("job_progression/load-all-for-player.sql"));
    this.deleteQuery = bindTable(SqlStatements.load("job_progression/delete.sql"));
  }

  @Contract(pure = true)
  private @NotNull String bindTable(@NotNull String sql) {
    return sql.replace("{table}", tableName);
  }

  @Contract(pure = true)
  private @NotNull String withLimit(@NotNull String sql, int limit) {
    return sql.replace("{limit}", Integer.toString(limit));
  }

  static @NotNull PlayerJobStateRepository create(
      @NotNull MemoryJobRepositoryImpl jobRepository,
      @NotNull ConnectionSource connectionSource,
      @NotNull String tableName) {
    return new RelationalPlayerJobStateRepositoryImpl(jobRepository, connectionSource, tableName);
  }

  private boolean belongsToTree(@NotNull PlayerJobStateRecord record) {
    JobRecord rootRecord = jobRepository.rootFor(record.currentNodeKey());
    return rootRecord != null && record.jobKey().equals(rootRecord.jobKey());
  }

  @Override
  public boolean save(@NotNull PlayerJobStateRecord record) {
    if (!belongsToTree(record)) {
      throw new IllegalArgumentException(
          "Current node " + record.currentNodeKey() + " is not in tree " + record.jobKey());
    }
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(saveQuery)) {
      String jobKey = record.jobKey();
      ps.setString(1, record.playerId());
      ps.setString(2, jobKey);
      ps.setString(3, record.currentNodeKey());
      ps.setBigDecimal(4, record.experience());
      if (ps.executeUpdate() > 0) {
        readCache.put(new Key(record.playerId(), jobKey), record);
        return true;
      }
      return false;
    } catch (SQLException e) {
      throw new dev.mintychochip.repository.WriteBackException(
          "Relational repository operation failed", e);
    }
  }

  @Override
  public @Nullable PlayerJobStateRecord load(@NotNull String playerId, @NotNull String jobKey) {
    Key key = new Key(playerId, jobKey);
    PlayerJobStateRecord stateRecord = readCache.getIfPresent(key);
    if (stateRecord != null) {
      return stateRecord;
    }
    JobRecord rootRecord = jobRepository.rootFor(jobKey);
    if (rootRecord == null || !jobKey.equals(rootRecord.jobKey())) {
      return null;
    }
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(loadQuery)) {
      ps.setString(1, playerId);
      ps.setString(2, jobKey);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        String currentNodeKey = rs.getString("current_node_key");
        BigDecimal experience = rs.getBigDecimal("experience");
        stateRecord = new PlayerJobStateRecord(playerId, jobKey, currentNodeKey, experience);
        if (!belongsToTree(stateRecord)) {
          return null;
        }
        readCache.put(key, stateRecord);
        return stateRecord;
      }
    } catch (SQLException e) {
      throw new dev.mintychochip.repository.WriteBackException(
          "Relational repository operation failed", e);
    }
  }

  @Override
  public @NotNull List<PlayerJobStateRecord> loadAllForJob(@NotNull String jobKey, int limit) {
    JobRecord rootRecord = jobRepository.rootFor(jobKey);
    if (rootRecord == null || !jobKey.equals(rootRecord.jobKey())) {
      return List.of();
    }
    List<PlayerJobStateRecord> records = new ArrayList<>();
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(withLimit(loadAllByJobQuery, limit))) {
      ps.setString(1, jobKey);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String playerId = rs.getString("player_id");
          Key key = new Key(playerId, jobKey);
          PlayerJobStateRecord stateRecord = readCache.getIfPresent(key);
          if (stateRecord != null) {
            records.add(stateRecord);
            continue;
          }
          String currentNodeKey = rs.getString("current_node_key");
          BigDecimal experience = rs.getBigDecimal("experience");
          stateRecord = new PlayerJobStateRecord(playerId, jobKey, currentNodeKey, experience);
          if (!belongsToTree(stateRecord)) {
            continue;
          }
          readCache.put(key, stateRecord);
          records.add(stateRecord);
        }
      }
      return records;
    } catch (SQLException e) {
      throw new dev.mintychochip.repository.WriteBackException(
          "Relational repository operation failed", e);
    }
  }

  @Override
  public @NotNull List<PlayerJobStateRecord> loadAllForPlayer(@NotNull String playerId, int limit) {
    List<PlayerJobStateRecord> records = new ArrayList<>();
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps =
            connection.prepareStatement(withLimit(loadAllForPlayerQuery, limit))) {
      ps.setString(1, playerId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String jobKey = rs.getString("job_key");
          Key key = new Key(playerId, jobKey);
          PlayerJobStateRecord stateRecord = readCache.getIfPresent(key);
          if (stateRecord != null) {
            records.add(stateRecord);
            continue;
          }
          JobRecord rootRecord = jobRepository.rootFor(jobKey);
          if (rootRecord == null || !jobKey.equals(rootRecord.jobKey())) {
            continue;
          }
          String currentNodeKey = rs.getString("current_node_key");
          BigDecimal experience = rs.getBigDecimal("experience");
          stateRecord = new PlayerJobStateRecord(playerId, jobKey, currentNodeKey, experience);
          if (!belongsToTree(stateRecord)) {
            continue;
          }
          readCache.put(key, stateRecord);
          records.add(stateRecord);
        }
      }
      return records;
    } catch (SQLException e) {
      throw new dev.mintychochip.repository.WriteBackException(
          "Relational repository operation failed", e);
    }
  }

  @Override
  public boolean delete(@NotNull String playerId, @NotNull String jobKey) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(deleteQuery)) {
      ps.setString(1, playerId);
      ps.setString(2, jobKey);
      if (ps.executeUpdate() > 0) {
        readCache.invalidate(new Key(playerId, jobKey));
        return true;
      }
      return false;
    } catch (SQLException e) {
      throw new dev.mintychochip.repository.WriteBackException(
          "Relational repository operation failed", e);
    }
  }
}
