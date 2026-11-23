package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;

/**
 * Matches {@link ConditionContext#onFire()} on a living entity.
 *
 * @param expected whether the entity must be on fire
 */
public record OnFireCondition(boolean expected) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (!context.livingPresent()) {
      return false;
    }
    return context.onFire() == expected;
  }
}
