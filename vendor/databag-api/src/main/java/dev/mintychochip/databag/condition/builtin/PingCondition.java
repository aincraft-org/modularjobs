package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import dev.mintychochip.databag.condition.RelationalOperator;

/**
 * Matches the player's network ping against a relational threshold.
 * Fails closed when no player is present or ping is unavailable.
 *
 * @param operator comparison applied to the snapshot ping value
 * @param milliseconds expected ping in milliseconds
 */
public record PingCondition(RelationalOperator operator, int milliseconds) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (!context.present()) {
      return false;
    }
    Integer actual = context.ping();
    if (actual == null) {
      return false;
    }
    return operator.test(java.math.BigDecimal.valueOf(actual), java.math.BigDecimal.valueOf(milliseconds));
  }
}
