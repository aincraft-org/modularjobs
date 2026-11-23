package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import java.util.Objects;

/**
 * Passes when the inner condition matches the block the entity is standing on.
 *
 * @param condition nested predicate evaluated against the standing-on context
 */
public record SteppingOnCondition(Condition condition) implements Condition {

  /**
   * Requires a non-null nested predicate.
   */
  public SteppingOnCondition {
    Objects.requireNonNull(condition, "condition");
  }

  @Override
  public boolean test(ConditionContext context) {
    ConditionContext standing = context.standingOn();
    if (standing == null) {
      return false;
    }
    return condition.test(standing);
  }
}
