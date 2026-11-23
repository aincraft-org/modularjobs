package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import java.util.List;

/**
 * Passes if the inner condition matches any passenger.
 *
 * @param condition nested predicate evaluated against each passenger context
 */
public record PassengerCondition(Condition condition) implements Condition {

  /**
   * Requires a non-null nested predicate.
   */
  public PassengerCondition {
    java.util.Objects.requireNonNull(condition, "condition");
  }

  @Override
  public boolean test(ConditionContext context) {
    List<ConditionContext> list = context.passengers();
    if (list == null || list.isEmpty()) {
      return false;
    }
    for (ConditionContext p : list) {
      if (condition.test(p)) {
        return true;
      }
    }
    return false;
  }
}
