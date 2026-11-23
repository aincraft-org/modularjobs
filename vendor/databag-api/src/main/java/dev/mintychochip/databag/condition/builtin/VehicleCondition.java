package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;

/**
 * Applies an inner condition to the entity's vehicle.
 *
 * @param condition nested predicate evaluated against the vehicle context
 */
public record VehicleCondition(Condition condition) implements Condition {

  /**
   * Requires a non-null nested predicate.
   */
  public VehicleCondition {
    java.util.Objects.requireNonNull(condition, "condition");
  }

  @Override
  public boolean test(ConditionContext context) {
    ConditionContext v = context.vehicle();
    return v != null && condition.test(v);
  }
}
