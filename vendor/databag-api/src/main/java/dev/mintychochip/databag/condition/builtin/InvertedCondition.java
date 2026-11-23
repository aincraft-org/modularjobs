package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import java.util.Objects;

/**
 * Vanilla {@code minecraft:inverted}.
 *
 * @param term predicate whose result is negated
 */
public record InvertedCondition(Condition term) implements Condition {

  /**
   * Requires a non-null predicate to negate.
   */
  public InvertedCondition {
    Objects.requireNonNull(term, "term");
  }

  @Override
  public boolean test(ConditionContext context) {
    return !term.test(context);
  }
}
