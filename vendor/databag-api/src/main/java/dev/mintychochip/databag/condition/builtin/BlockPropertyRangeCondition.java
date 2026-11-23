package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;

/**
 * Checks a block-state property value falls within [min, max].
 *
 * @param name block-state property name
 * @param min inclusive minimum parsed integer value
 * @param max inclusive maximum parsed integer value
 */
public record BlockPropertyRangeCondition(String name, int min, int max) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    String raw = context.blockProperties().get(name);
    if (raw == null) {
      return false;
    }
    int value;
    try {
      value = Integer.parseInt(raw);
    } catch (NumberFormatException e) {
      return false;
    }
    return value >= min && value <= max;
  }
}
