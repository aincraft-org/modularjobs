package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import dev.mintychochip.databag.condition.RelationalOperator;
import java.math.BigDecimal;

/**
 * Matches the player's total playtime in ticks against a relational threshold.
 *
 * @param operator comparison applied to the snapshot playtime value
 * @param ticks expected playtime in ticks
 */
public record PlaytimeCondition(RelationalOperator operator, long ticks) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (context.present() == false) {
      return false;
    }
    Long actual = context.playtime();
    if (actual == null) {
      return false;
    }
    return operator.test(BigDecimal.valueOf(actual), BigDecimal.valueOf(ticks));
  }
}
