package dev.mintychochip.domain;

import dev.mintychochip.domain.model.PlayerJobStateRecord;
import dev.mintychochip.domain.repository.PlayerJobStateRepository;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Coordinates player job state persistence between a primary "live" store and an archival store,
 * adding {@linkplain #archive(String, String) archive} and {@linkplain #restore(String, String)
 * restore} migration on top of the live repository.
 */
public final class PlayerJobStateService {

  /** Primary store used by routine read/write operations. */
  private final PlayerJobStateRepository live;

  /** Secondary store holding archived state records. */
  private final PlayerJobStateRepository archive;

  /**
   * Wires live and archive state repositories for routine operations and migration.
   *
   * @param live the live repository backing normal state operations
   * @param archive the repository used to hold archived state records
   */
  public PlayerJobStateService(
      @NotNull PlayerJobStateRepository live, @NotNull PlayerJobStateRepository archive) {
    this.live = live;
    this.archive = archive;
  }

  /**
   * Saves a state record to the live store.
   *
   * @param record the record to persist
   * @return {@code true} if the record was stored
   */
  public boolean save(@NotNull PlayerJobStateRecord record) {
    return live.save(record);
  }

  /**
   * Loads a state record from the live store.
   *
   * @param playerId the owning player id
   * @param jobKey the job key
   * @return the matching record, or {@code null} if absent
   */
  public @Nullable PlayerJobStateRecord load(@NotNull String playerId, @NotNull String jobKey) {
    return live.load(playerId, jobKey);
  }

  /**
   * Loads up to {@code limit} live state records for a job.
   *
   * @param jobKey the job key to match
   * @param limit the maximum number of records to return
   * @return the matching live records
   */
  public @NotNull List<PlayerJobStateRecord> loadAllForJob(@NotNull String jobKey, int limit) {
    return live.loadAllForJob(jobKey, limit);
  }

  /**
   * Loads up to {@code limit} live state records for a player.
   *
   * @param playerId the player id to match
   * @param limit the maximum number of records to return
   * @return the matching live records
   */
  public @NotNull List<PlayerJobStateRecord> loadAllForPlayer(@NotNull String playerId, int limit) {
    return live.loadAllForPlayer(playerId, limit);
  }

  /**
   * Deletes a state record from the live store.
   *
   * @param playerId the owning player id
   * @param jobKey the job key
   * @return {@code true} if a record was deleted
   */
  public boolean delete(@NotNull String playerId, @NotNull String jobKey) {
    return live.delete(playerId, jobKey);
  }

  /**
   * Moves a state record from the live store to the archive.
   *
   * @param playerId the owning player id
   * @param jobKey the job key
   * @return {@code true} if the record was migrated, {@code false} if it was absent or the copy
   *     failed
   */
  public boolean archive(@NotNull String playerId, @NotNull String jobKey) {
    return migrate(live, archive, playerId, jobKey);
  }

  /**
   * Moves an archived state record back to the live store.
   *
   * @param playerId the owning player id
   * @param jobKey the job key
   * @return {@code true} if the record was restored, {@code false} if it was absent or the copy
   *     failed
   */
  public boolean restore(@NotNull String playerId, @NotNull String jobKey) {
    return migrate(archive, live, playerId, jobKey);
  }

  /**
   * Loads up to {@code limit} archived state records for a player.
   *
   * @param playerId the player id to match
   * @param limit the maximum number of records to return
   * @return the matching archived records
   */
  public @NotNull List<PlayerJobStateRecord> loadAllArchivedForPlayer(
      @NotNull String playerId, int limit) {
    return archive.loadAllForPlayer(playerId, limit);
  }

  /**
   * Migrates a single state record between two repositories: loads it from {@code from}, saves a
   * copy to {@code to}, and only deletes the source once the copy succeeds.
   *
   * @param from the source repository
   * @param to the destination repository
   * @param playerId the owning player id
   * @param jobKey the job key
   * @return {@code true} if the record was migrated, {@code false} otherwise
   */
  private boolean migrate(
      @NotNull PlayerJobStateRepository from,
      @NotNull PlayerJobStateRepository to,
      @NotNull String playerId,
      @NotNull String jobKey) {
    PlayerJobStateRecord record = from.load(playerId, jobKey);
    if (record == null) {
      return false;
    }
    if (to.save(record)) {
      return from.delete(playerId, jobKey);
    }
    return false;
  }
}
