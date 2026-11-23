package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;

/**
 * Matches {@link ConditionContext#baby()} on a living entity.
 *
 * @param expected whether the living entity must be a baby
 */
public record BabyCondition(boolean expected) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (!context.livingPresent()) {
      return false;
    }
    return context.baby() == expected;
  }
}
