package dev.mintychochip.container;

import dev.mintychochip.PlayerJobState;
import dev.mintychochip.databag.condition.ConditionContext;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable context supplied while evaluating a boost for a player's current job state.
 *
 * @param type action that triggered the boost evaluation
 * @param jobState current state associated with the player, or {@code null} outside a job payout
 * @param playerId unique identifier of the player
 * @param worldName name of the world in which the action occurred
 * @param payable payable affected by the boost
 * @param conditions player snapshot for {@link dev.mintychochip.databag.condition.Condition}
 */
public record BoostContext(
    @NotNull ActionType type,
    @Nullable PlayerJobState jobState,
    @NotNull UUID playerId,
    @NotNull String worldName,
    @NotNull Payable payable,
    @NotNull ConditionContext conditions) {

  /** Builds a context with an absent condition snapshot (tests / fail-closed). */
  public BoostContext(
      @NotNull ActionType type,
      @Nullable PlayerJobState jobState,
      @NotNull UUID playerId,
      @NotNull String worldName,
      @NotNull Payable payable) {
    this(type, jobState, playerId, worldName, payable, ConditionContext.absent());
  }
}
