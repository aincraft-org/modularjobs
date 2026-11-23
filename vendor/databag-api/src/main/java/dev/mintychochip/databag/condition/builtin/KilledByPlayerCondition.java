package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;

/**
 * Matches whether the attacking-player slot was present when the snapshot was captured.
 *
 * @param expected whether an attacking player must be present
 */
public record KilledByPlayerCondition(boolean expected) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (!context.death()) {
      return false;
    }
    return expected == (context.attackingPlayer() != null);
  }
}
