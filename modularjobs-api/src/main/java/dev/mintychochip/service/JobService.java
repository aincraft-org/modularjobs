package dev.mintychochip.service;

import dev.mintychochip.Job;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.JobTask;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Context;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides access to complete job trees, inherited node tasks, and immutable player job states.
 *
 * <p>Root and specialization keys both resolve to their owning complete {@link Job}. {@link
 * #getJob(String)} and {@link #joinJob(String, String)} reject unknown keys with {@link
 * IllegalArgumentException}; query methods return an empty result for unknown trees.
 */
public interface JobService {

  /**
   * Returns every root-owned job tree known to the service.
   *
   * @return complete job trees, one per root
   */
  @NotNull
  List<Job> getJobs();

  /**
   * Returns the complete job tree containing the given root or specialization key.
   *
   * @param jobKey root or node key
   * @return the owning complete job tree
   * @throws IllegalArgumentException if no root or node matches the key
   */
  @NotNull
  Job getJob(@NotNull String jobKey);

  /** Resolves the complete aggregate referenced by a key-only player state. */
  default @NotNull Job getJob(@NotNull PlayerJobState state) {
    return getJob(state.jobKey().asString());
  }

  /**
   * Resolves the task visible at a job node. The nearest node definition replaces an ancestor task
   * with the same action-and-context key.
   *
   * @param job complete job tree
   * @param nodeKey active node whose inherited task path is resolved
   * @param type action type
   * @param context action context
   * @return resolved task, or {@code null} when no node on the path defines it
   */
  @Nullable
  JobTask getTask(
      @NotNull Job job,
      @NotNull JobNodeKey nodeKey,
      @NotNull ActionType type,
      @NotNull Context context);

  /** Returns all tasks visible at a node after nearest-descendant replacement. */
  @NotNull
  Map<ActionType, List<JobTask>> getAllTasks(@NotNull Job job, @NotNull JobNodeKey nodeKey);

  /**
   * Persists a player state after validating its tree/node keys and curve-derived level against the
   * current aggregate.
   */
  boolean update(@NotNull PlayerJobState state);

  /**
   * Adds the given player to the given job. A job on hold (archived) comes back with its kept
   * progress. That re-activation, and a join into a slot group where the player has a job on hold
   * (leave then join is a swap), first fires a cancellable {@code JobSwapRequestEvent} and, once
   * done, a {@code JobSwapEvent}. Any other join of a new job fires neither.
   *
   * @param playerId the player identifier
   * @param jobKey the job key
   * @return {@code true} if the player joined, {@code false} otherwise
   * @throws IllegalArgumentException if the job key is unknown
   */
  boolean joinJob(@NotNull String playerId, @NotNull String jobKey);

  /**
   * Puts the given job on hold: its level and experience move to the archive and come back on the
   * next join.
   *
   * @param playerId the player identifier
   * @param jobKey the job key
   * @return {@code true} if the player left, {@code false} otherwise
   */
  boolean leaveJob(@NotNull String playerId, @NotNull String jobKey);

  /**
   * Puts the active job {@code fromJobKey} on hold and makes {@code toJobKey} active in one step,
   * so a full slot group can change members. Fires a cancellable {@code JobSwapRequestEvent} before
   * any change and a {@code JobSwapEvent} after it. If {@code to} cannot be activated, {@code from}
   * is restored.
   *
   * @param playerId the player identifier
   * @param fromJobKey an active job of the player
   * @param toJobKey a job the player does not hold active
   * @return {@code true} if the swap completed, {@code false} otherwise
   * @throws IllegalArgumentException if either job key is unknown
   */
  boolean swapJob(@NotNull String playerId, @NotNull String fromJobKey, @NotNull String toJobKey);

  /** Returns the player's state in the requested job tree, or {@code null} if absent or unknown. */
  @Nullable
  PlayerJobState getPlayerJobState(@NotNull String playerId, @NotNull String jobKey);

  /** Returns all job-tree states for the given player. */
  @NotNull
  List<PlayerJobState> getPlayerJobStates(@NotNull UUID playerId);

  /** Returns up to {@code limit} player states for one job tree, ordered by experience. */
  @NotNull
  List<PlayerJobState> getPlayerJobStates(@NotNull Key jobKey, int limit);

  /** Returns the archived job-tree states for the given player. */
  @NotNull
  List<PlayerJobState> getArchivedPlayerJobStates(@NotNull UUID playerId);
}
