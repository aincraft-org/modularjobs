package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import java.util.List;

/**
 * Vanilla {@code minecraft:any_of}: at least one term holds. Empty terms is false.
 *
 * @param terms conditions to try; the list is copied defensively
 */
public record AnyOfCondition(List<Condition> terms) implements Condition {

  /** Creates an instance, copying the term list defensively. */
  public AnyOfCondition {
    terms = List.copyOf(terms);
  }

  @Override
  public boolean test(ConditionContext context) {
    for (Condition term : terms) {
      if (term.test(context)) {
        return true;
      }
    }
    return false;
  }
}
