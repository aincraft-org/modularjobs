package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import dev.mintychochip.databag.condition.PlayerResourceType;
import dev.mintychochip.databag.condition.RelationalOperator;
import java.math.BigDecimal;

/**
 * Compares a player resource snapshot value to {@code expected}.
 *
 * @param type resource field to read from the snapshot
 * @param operator comparison applied to the snapshot value
 * @param expected threshold value
 */
public record PlayerResourceCondition(
    PlayerResourceType type, RelationalOperator operator, double expected) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    boolean allowed = switch (type) {
      case HEALTH -> context.livingPresent();
      case HUNGER, EXPERIENCE, LEVEL, ABSORPTION, AIR -> context.present();
    };
    if (!allowed) {
      return false;
    }
    Double actual = switch (type) {
      case HEALTH -> context.health();
      case HUNGER -> context.hunger();
      case EXPERIENCE -> context.experience();
      case LEVEL -> context.xpLevel();
      case ABSORPTION -> context.absorption();
      case AIR -> context.airRemaining();
    };
    if (actual == null) {
      return false;
    }
    return operator.test(BigDecimal.valueOf(actual), BigDecimal.valueOf(expected));
  }
}
