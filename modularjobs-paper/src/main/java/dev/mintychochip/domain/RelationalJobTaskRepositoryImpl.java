package dev.mintychochip.domain;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.mintychochip.domain.model.JobTaskRecord;
import dev.mintychochip.domain.model.PayableRecord;
import dev.mintychochip.repository.ConnectionSource;
import dev.mintychochip.repository.SqlStatements;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * SQL-backed repository for job task records, keyed by the tuple {@code (nodeKey, actionTypeKey,
 * contextKey)}. Reads are serviced through an LRU-style Caffeine cache (10-minute TTL, 10k entry
 * cap) that is invalidated on deletion and refreshed on successful saves; writes run in
 * transactions against the shared {@link ConnectionSource}.
 */
public final class RelationalJobTaskRepositoryImpl {

  private static final Duration CACHE_TIME_TO_LIVE = Duration.ofMinutes(10);
  private static final int CACHE_MAXIMUM_SIZE = 10_000;

  private static final String SELECT_PAYABLES = SqlStatements.load("job_tasks/select-payables.sql");
  private static final String SELECT_TASK_ID = SqlStatements.load("job_tasks/select-task-id.sql");
  private static final String INSERT_TASK = SqlStatements.load("job_tasks/insert-task.sql");
  private static final String DELETE_PAYABLES = SqlStatements.load("job_tasks/delete-payables.sql");
  private static final String INSERT_PAYABLE = SqlStatements.load("job_tasks/insert-payable.sql");
  private static final String DELETE_TASK = SqlStatements.load("job_tasks/delete-task.sql");
  private static final String SELECT_CONTEXT_KEYS =
      SqlStatements.load("job_tasks/select-context-keys.sql");
  private static final String SELECT_RECORDS_MAP =
      SqlStatements.load("job_tasks/select-records-map.sql");

  private final ConnectionSource connectionSource;

  /** Read-through cache keyed by (nodeKey, actionTypeKey, contextKey). */
  private final Cache<String, JobTaskRecord> readCache =
      Caffeine.newBuilder()
          .expireAfterWrite(CACHE_TIME_TO_LIVE)
          .maximumSize(CACHE_MAXIMUM_SIZE)
          .build();

  /**
   * Creates a repository that reads and writes job tasks through the given connection source.
   *
   * @param connectionSource the source of database connections for all operations
   */
  public RelationalJobTaskRepositoryImpl(@NotNull ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
  }

  /**
   * Loads one task owned by a job node.
   *
   * @return the matching task, or {@code null} when this node does not define the key
   */
  public @Nullable JobTaskRecord load(
      @NotNull String nodeKey, @NotNull String actionTypeKey, @NotNull String contextKey) {
    String cacheKey = createCacheKey(nodeKey, actionTypeKey, contextKey);
    JobTaskRecord taskRecord = readCache.getIfPresent(cacheKey);
    if (taskRecord != null) {
      return taskRecord;
    }
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(SELECT_PAYABLES)) {
      ps.setString(1, nodeKey);
      ps.setString(2, actionTypeKey);
      ps.setString(3, contextKey);
      try (ResultSet rs = ps.executeQuery()) {
        List<PayableRecord> records = new ArrayList<>();
        boolean taskFound = false;
        while (rs.next()) {
          taskFound = true;
          String payableTypeKey = rs.getString("payable_type_key");
          if (payableTypeKey != null) {
            BigDecimal amount = rs.getBigDecimal("amount");
            String currencyIdentifier = rs.getString("currency_identifier");
            String currencySymbol = rs.getString("currency_symbol");
            PayableRecord record =
                new PayableRecord(payableTypeKey, amount, currencyIdentifier, currencySymbol);
            records.add(record);
          }
        }
        if (!taskFound) {
          return null;
        }
        taskRecord = new JobTaskRecord(nodeKey, actionTypeKey, contextKey, records);
        readCache.put(cacheKey, taskRecord);
        return taskRecord;
      }
    } catch (SQLException e) {
      throw new dev.mintychochip.repository.WriteBackException(
          "Relational repository operation failed", e);
    }
  }

  /**
   * Persists a task record transactionally: inserts the task row when absent, otherwise replaces
   * its payables, then refreshes the cache with the stored record.
   *
   * @param record the record to store
   * @return {@code true} if the record was persisted
   */
  public boolean save(@NotNull JobTaskRecord record) {
    String cacheKey = createCacheKey(record.nodeKey(), record.actionTypeKey(), record.contextKey());
    try (Connection connection = connectionSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        // Check if task exists
        Integer taskId = null;
        try (PreparedStatement ps = connection.prepareStatement(SELECT_TASK_ID)) {
          ps.setString(1, record.nodeKey());
          ps.setString(2, record.actionTypeKey());
          ps.setString(3, record.contextKey());
          try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
              taskId = rs.getInt("task_id");
            }
          }
        }

        if (taskId == null) {
          // Insert new task
          try (PreparedStatement ps =
              connection.prepareStatement(INSERT_TASK, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, record.nodeKey());
            ps.setString(2, record.actionTypeKey());
            ps.setString(3, record.contextKey());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
              if (rs.next()) {
                taskId = rs.getInt(1);
              }
            }
          }
        } else {
          // Delete existing payables for update
          try (PreparedStatement ps = connection.prepareStatement(DELETE_PAYABLES)) {
            ps.setInt(1, taskId);
            ps.executeUpdate();
          }
        }

        // Insert payables
        if (taskId != null && record.payables() != null) {
          try (PreparedStatement ps = connection.prepareStatement(INSERT_PAYABLE)) {
            for (PayableRecord payable : record.payables()) {
              ps.setInt(1, taskId);
              ps.setString(2, payable.payableTypeKey());
              ps.setBigDecimal(3, payable.amount());
              ps.setString(4, payable.currencyIdentifier());
              ps.setString(5, payable.currencySymbol());
              ps.addBatch();
            }
            ps.executeBatch();
          }
        }

        connection.commit();
        readCache.put(cacheKey, record);
        return true;
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new dev.mintychochip.repository.WriteBackException(
          "Relational repository operation failed", e);
    }
  }

  /**
   * Deletes the task and its payables (children first to satisfy the foreign key) in a transaction,
   * invalidating the cache entry.
   *
   * @param nodeKey the job key
   * @param actionTypeKey the action type key
   * @param contextKey the context key
   * @return {@code true} if a task row was deleted, {@code false} if none matched
   */
  public boolean delete(
      @NotNull String nodeKey, @NotNull String actionTypeKey, @NotNull String contextKey) {
    String cacheKey = createCacheKey(nodeKey, actionTypeKey, contextKey);
    try (Connection connection = connectionSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        // Get task_id first
        Integer taskId = null;
        try (PreparedStatement ps = connection.prepareStatement(SELECT_TASK_ID)) {
          ps.setString(1, nodeKey);
          ps.setString(2, actionTypeKey);
          ps.setString(3, contextKey);
          try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
              taskId = rs.getInt("task_id");
            }
          }
        }

        if (taskId == null) {
          return false;
        }

        // Delete payables first (foreign key)
        try (PreparedStatement ps = connection.prepareStatement(DELETE_PAYABLES)) {
          ps.setInt(1, taskId);
          ps.executeUpdate();
        }

        // Delete task
        try (PreparedStatement ps = connection.prepareStatement(DELETE_TASK)) {
          ps.setInt(1, taskId);
          ps.executeUpdate();
        }

        connection.commit();
        readCache.invalidate(cacheKey);
        return true;
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new dev.mintychochip.repository.WriteBackException(
          "Relational repository operation failed", e);
    }
  }

  /**
   * Loads all task records for a job, grouped by action type key (map order follows action type
   * ordering).
   *
   * @param nodeKey the job key to match
   * @return a map of action type key to its task records
   */
  public @NotNull Map<String, List<JobTaskRecord>> getRecords(@NotNull String nodeKey) {
    Map<String, Map<Integer, TaskRecordAccumulator>> actionTypeTaskMap = new LinkedHashMap<>();
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(SELECT_RECORDS_MAP)) {
      ps.setString(1, nodeKey);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          int taskId = rs.getInt("task_id");
          String actionTypeKey = rs.getString("action_type_key");
          String payableTypeKey = rs.getString("payable_type_key");
          BigDecimal amount = rs.getBigDecimal("amount");
          String currencyIdentifier = rs.getString("currency_identifier");
          String currencySymbol = rs.getString("currency_symbol");
          String contextKey = rs.getString("context_key");
          Map<Integer, TaskRecordAccumulator> taskMap =
              actionTypeTaskMap.computeIfAbsent(actionTypeKey, ignored -> new LinkedHashMap<>());
          TaskRecordAccumulator accumulator =
              taskMap.computeIfAbsent(taskId, ignored -> new TaskRecordAccumulator(contextKey));
          if (payableTypeKey != null) {
            accumulator.payables.add(
                new PayableRecord(payableTypeKey, amount, currencyIdentifier, currencySymbol));
          }
        }
      }
    } catch (SQLException e) {
      throw new dev.mintychochip.repository.WriteBackException(
          "Relational repository operation failed", e);
    }

    Map<String, List<JobTaskRecord>> records = new LinkedHashMap<>();
    for (Entry<String, Map<Integer, TaskRecordAccumulator>> entry : actionTypeTaskMap.entrySet()) {
      String actionTypeKey = entry.getKey();
      List<JobTaskRecord> taskRecords =
          entry.getValue().values().stream()
              .map(
                  a ->
                      new JobTaskRecord(
                          nodeKey, actionTypeKey, a.contextKey, List.copyOf(a.payables)))
              .toList();
      records.put(actionTypeKey, taskRecords);
    }
    return records;
  }

  /**
   * Loads all task records for a single action type of a job.
   *
   * @param nodeKey the job key
   * @param actionTypeKey the action type key
   * @return the matching task records
   */
  public @NotNull List<JobTaskRecord> getRecords(
      @NotNull String nodeKey, @NotNull String actionTypeKey) {
    List<JobTaskRecord> records = new ArrayList<>();
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(SELECT_CONTEXT_KEYS)) {
      ps.setString(1, nodeKey);
      ps.setString(2, actionTypeKey);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String contextKey = rs.getString("context_key");
          JobTaskRecord record = load(nodeKey, actionTypeKey, contextKey);
          if (record != null) {
            records.add(record);
          }
        }
      }
      return records;
    } catch (SQLException e) {
      throw new dev.mintychochip.repository.WriteBackException(
          "Relational repository operation failed", e);
    }
  }

  /**
   * Loads every task record for a job across all action types.
   *
   * @param nodeKey the job key to match
   * @return all task records for the job
   */
  public @NotNull List<JobTaskRecord> getAllRecords(@NotNull String nodeKey) {
    Map<String, List<JobTaskRecord>> grouped = getRecords(nodeKey);
    List<JobTaskRecord> all = new ArrayList<>();
    for (List<JobTaskRecord> records : grouped.values()) {
      all.addAll(records);
    }
    return all;
  }

  /** Builds a {@link JobTaskRecord} while accumulating its payable rows across result rows. */
  private static final class TaskRecordAccumulator {

    private final String contextKey;
    private final List<PayableRecord> payables = new ArrayList<>();

    private TaskRecordAccumulator(@NotNull String contextKey) {
      this.contextKey = contextKey;
    }
  }

  /** Builds the cache key for a task's key tuple. */
  @Contract(pure = true)
  private static @NotNull String createCacheKey(
      @NotNull String nodeKey, @NotNull String actionTypeKey, @NotNull String contextKey) {
    return nodeKey + '\0' + actionTypeKey + '\0' + contextKey;
  }
}
