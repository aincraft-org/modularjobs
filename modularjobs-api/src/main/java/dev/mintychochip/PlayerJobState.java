package dev.mintychochip;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable snapshot of one player's identity and progress within one complete {@link Job} tree.
 *
 * <p>The state retains typed tree and active-node keys rather than embedding the aggregate. Its
 * level is derived from {@link #experience()} using the tree-wide curve captured by the runtime
 * implementation.
 */
public interface PlayerJobState {

  /** Typed identity of the complete job tree that owns this state. */
  @Contract(pure = true)
  @NotNull
  JobKey jobKey();

  /** Typed identity of the active root or specialization node. */
  @Contract(pure = true)
  @NotNull
  JobNodeKey currentNodeKey();

  /** Returns whether both typed keys resolve within the supplied aggregate. */
  @Contract(pure = true)
  default boolean belongsTo(@NotNull Job job) {
    Objects.requireNonNull(job, "job");
    return job.jobKey().equals(jobKey()) && job.node(currentNodeKey()) != null;
  }

  /** Owning player. */
  @Contract(pure = true)
  @NotNull
  UUID playerId();

  /** Accumulated tree-wide experience. */
  @Contract(pure = true)
  @NotNull
  BigDecimal experience();

  /** Level derived from {@link #experience()} and the owning tree's leveling curve. */
  @Contract(pure = true)
  int level();

  /** Total experience required to reach {@code level}. */
  @Contract(pure = true)
  @NotNull
  BigDecimal experienceForLevel(int level);

  /** Returns an otherwise identical state with the supplied tree-wide experience. */
  @Contract(pure = true)
  @NotNull
  PlayerJobState withExperience(@NotNull BigDecimal experience);

  /** Adds to the current tree-wide experience. */
  @Contract(pure = true)
  default @NotNull PlayerJobState addExperience(@NotNull BigDecimal experience) {
    return withExperience(experience().add(experience));
  }
}
