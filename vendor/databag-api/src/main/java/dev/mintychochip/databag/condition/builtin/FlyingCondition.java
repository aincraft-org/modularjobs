package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;

/**
 * Player-only: matches {@link ConditionContext#flying()}. A generic living
 * entity snapshot without a player fails closed.
 *
 * @param expected whether the player must be flying
 */
public record FlyingCondition(boolean expected) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (!context.present()) {
      return false;
    }
    return context.flying() == expected;
  }
}
