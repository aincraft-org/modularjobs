package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;

/**
 * Matches {@link ConditionContext#swimming()} on a living entity.
 *
 * @param expected whether the entity must be swimming
 */
public record SwimmingCondition(boolean expected) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (!context.livingPresent()) {
      return false;
    }
    return context.swimming() == expected;
  }
}
