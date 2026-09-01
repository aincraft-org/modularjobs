package dev.mintychochip.container;

import dev.mintychochip.PlayerJobState;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/** Applies a {@link Payable} reward to a player's current job state. */
@FunctionalInterface
public interface PayableHandler {

  /**
   * Pays out the reward described by the given context.
   *
   * @param context details of the player, payable, and job state
   * @throws IllegalArgumentException if the context is invalid or the payable cannot be applied to
   *     this handler
   */
  void pay(@NotNull PayableContext context);

  /**
   * Immutable context describing a single payout.
   *
   * @param playerId unique identifier of the receiving player
   * @param payable the reward to pay
   * @param jobState state the reward is granted for
   */
  record PayableContext(
      @NotNull UUID playerId, @NotNull Payable payable, @NotNull PlayerJobState jobState) {}

  /** Controls how a payout is presented visually to the player. */
  @FunctionalInterface
  interface PayableVisualController {
    /**
     * Renders the payout described by the given context.
     *
     * @param context details of the payout to display
     */
    void display(@NotNull PayableContext context);
  }
}
