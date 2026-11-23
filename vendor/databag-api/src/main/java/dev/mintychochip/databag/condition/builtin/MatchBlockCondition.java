package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import java.util.Objects;

/**
 * Matches the block subject in the current condition context.
 *
 * @param condition nested predicate evaluated against the current context
 */
public record MatchBlockCondition(Condition condition) implements Condition {

  /**
   * Requires a non-null nested predicate.
   */
  public MatchBlockCondition {
    Objects.requireNonNull(condition, "condition");
  }

  @Override
  public boolean test(ConditionContext context) {
    if (context.blockId() == null) {
      return false;
    }
    return condition.test(context);
  }
}
