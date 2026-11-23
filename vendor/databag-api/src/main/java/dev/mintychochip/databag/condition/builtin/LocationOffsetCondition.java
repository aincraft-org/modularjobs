package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import dev.mintychochip.databag.condition.OffsetContextResolver;
import java.util.Objects;

/**
 * Evaluates an inner location predicate against a context-relative offset.
 *
 * @param offsetX block offset on the X axis
 * @param offsetY block offset on the Y axis
 * @param offsetZ block offset on the Z axis
 * @param condition nested predicate evaluated on the resolved offset context
 */
public record LocationOffsetCondition(
    int offsetX,
    int offsetY,
    int offsetZ,
    Condition condition) implements Condition {

  /**
   * Requires a non-null nested predicate.
   */
  public LocationOffsetCondition {
    Objects.requireNonNull(condition, "condition");
  }

  @Override
  public boolean test(ConditionContext context) {
    OffsetContextResolver resolver = context.offsetResolver();
    if (resolver == null) {
      return false;
    }
    ConditionContext offset = resolver.resolve(offsetX, offsetY, offsetZ);
    if (offset == null) {
      return false;
    }
    return condition.test(offset);
  }
}
