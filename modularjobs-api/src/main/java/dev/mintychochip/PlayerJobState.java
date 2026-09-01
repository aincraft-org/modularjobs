package dev.mintychochip;

import java.math.BigDecimal;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable snapshot of one player's state within one complete {@link Job} tree.
 *
 * <p>Experience and level belong to the tree. {@link #currentNode()} identifies the player's active
 * specialization without splitting that shared progress into another job.
 */
public interface PlayerJobState {

  /** Complete job tree that owns this state. */
  @Contract(pure = true)
  @NotNull
  Job job();

  /** Active specialization node within {@link #job()}. */
  @Contract(pure = true)
  @NotNull
  JobNode currentNode();

  /** Owning player. */
  @Contract(pure = true)
  @NotNull
  UUID playerId();

  /** Accumulated tree-wide experience. */
  @Contract(pure = true)
  @NotNull
  BigDecimal experience();

  /** Level derived from {@link #experience()} and the job's tree-wide leveling curve. */
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

  /** Returns an otherwise identical state with the supplied active node. */
  @Contract(pure = true)
  @NotNull
  PlayerJobState withCurrentNode(@NotNull JobNodeKey nodeKey);

  /** Adds to the current tree-wide experience. */
  @Contract(pure = true)
  default @NotNull PlayerJobState addExperience(@NotNull BigDecimal experience) {
    return withExperience(experience().add(experience));
  }
}
