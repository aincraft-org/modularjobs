package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import dev.mintychochip.databag.condition.RelationalOperator;
import java.math.BigDecimal;

/**
 * Matches the entity's invulnerability frames (no-damage ticks).
 *
 * @param operator comparison applied to the snapshot tick count
 * @param ticks expected invulnerability tick count
 */
public record InvulnerableFramesCondition(RelationalOperator operator, int ticks)
    implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (!context.livingPresent()) {
      return false;
    }
    Integer actual = context.invulnerableTicks();
    if (actual == null) {
      return false;
    }
    return operator.test(BigDecimal.valueOf(actual), BigDecimal.valueOf(ticks));
  }
}
