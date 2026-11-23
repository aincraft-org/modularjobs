package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;

/**
 * Matches {@link ConditionContext#onGround()} on a living entity.
 *
 * @param expected whether the entity must be on the ground
 */
public record OnGroundCondition(boolean expected) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (!context.livingPresent()) {
      return false;
    }
    return context.onGround() == expected;
  }
}
