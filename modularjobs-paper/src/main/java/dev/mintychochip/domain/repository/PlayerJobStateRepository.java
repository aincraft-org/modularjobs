package dev.mintychochip.domain.repository;

import dev.mintychochip.domain.model.PlayerJobStateRecord;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Repository contract for persisting and querying {@link PlayerJobStateRecord}s by player-and-job
 * identity. Implementations determine storage and extraction semantics, including ordering of batch
 * loads and the meaning of the {@code limit} cap.
 */
public interface PlayerJobStateRepository {

  /**
   * Persists a player state record, replacing any existing record with the same key.
   *
   * @param record the record to store
   * @return {@code true} if the record was persisted
   */
  boolean save(@NotNull PlayerJobStateRecord record);

  /**
   * Loads the state record for the given player and job.
   *
   * @param playerId the owning player id
   * @param jobKey the job key
   * @return the matching record, or {@code null} if absent
   */
  @Nullable
  PlayerJobStateRecord load(@NotNull String playerId, @NotNull String jobKey);

  /**
   * Loads up to {@code limit} state records for the given job.
   *
   * @param jobKey the job key to match
   * @param limit the maximum number of records to return
   * @return the matching records
   */
  @NotNull
  List<PlayerJobStateRecord> loadAllForJob(@NotNull String jobKey, int limit);

  /**
   * Loads up to {@code limit} state records for the given player.
   *
   * @param playerId the player id to match
   * @param limit the maximum number of records to return
   * @return the matching records
   */
  @NotNull
  List<PlayerJobStateRecord> loadAllForPlayer(@NotNull String playerId, int limit);

  /**
   * Deletes the state record for the given player and job.
   *
   * @param playerId the owning player id
   * @param jobKey the job key
   * @return {@code true} if a record was deleted
   */
  boolean delete(@NotNull String playerId, @NotNull String jobKey);

  /** Composite identity of a player state record. */
  record Key(@NotNull String playerId, @NotNull String jobKey) {}
}
