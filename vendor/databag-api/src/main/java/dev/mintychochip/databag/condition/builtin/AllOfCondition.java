package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import java.util.List;

/**
 * Vanilla {@code minecraft:all_of}: every term holds. Empty terms is true.
 *
 * @param terms conditions that must all hold; the list is copied defensively
 */
public record AllOfCondition(List<Condition> terms) implements Condition {

  /** Creates an instance, copying the term list defensively. */
  public AllOfCondition {
    terms = List.copyOf(terms);
  }

  @Override
  public boolean test(ConditionContext context) {
    for (Condition term : terms) {
      if (!term.test(context)) {
        return false;
      }
    }
    return true;
  }
}
