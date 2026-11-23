package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;

/**
 * Matches {@link ConditionContext#gliding()} on a living entity (elytra).
 *
 * @param expected whether the living entity must be gliding
 */
public record GlidingCondition(boolean expected) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (!context.livingPresent()) {
      return false;
    }
    return context.gliding() == expected;
  }
}
