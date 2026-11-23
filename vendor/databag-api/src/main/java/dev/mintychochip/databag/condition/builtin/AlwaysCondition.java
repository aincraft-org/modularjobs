package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;

/**
 * Condition that always holds. Used as a base rule with no restrictions.
 */
public record AlwaysCondition() implements Condition {

  /** Shared singleton instance. */
  public static final AlwaysCondition INSTANCE = new AlwaysCondition();

  @Override
  public boolean test(ConditionContext context) {
    return true;
  }
}
