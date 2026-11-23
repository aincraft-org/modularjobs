package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import org.jetbrains.annotations.Nullable;

/**
 * Matches snapshot coordinates against optional per-axis bounds; a {@code null}
 * bound means that side of the axis is unrestricted.
 *
 * @param minX minimum x bound, or {@code null} for unbounded
 * @param maxX maximum x bound, or {@code null} for unbounded
 * @param minY minimum y bound, or {@code null} for unbounded
 * @param maxY maximum y bound, or {@code null} for unbounded
 * @param minZ minimum z bound, or {@code null} for unbounded
 * @param maxZ maximum z bound, or {@code null} for unbounded
 */
public record PositionCondition(
    @Nullable Double minX,
    @Nullable Double maxX,
    @Nullable Double minY,
    @Nullable Double maxY,
    @Nullable Double minZ,
    @Nullable Double maxZ)
    implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    boolean anyBound = minX != null || maxX != null
        || minY != null || maxY != null
        || minZ != null || maxZ != null;
    if (!anyBound) {
      return false;
    }
    return axis(context.x(), minX, maxX) && axis(context.y(), minY, maxY) && axis(context.z(), minZ, maxZ);
  }

  private static boolean axis(Double value, @Nullable Double min, @Nullable Double max) {
    if (min == null && max == null) {
      return true;
    }
    if (value == null) {
      return false;
    }
    return withinBounds(value, min, max);
  }

  private static boolean withinBounds(double value, @Nullable Double min, @Nullable Double max) {
    if (min != null && value < min) {
      return false;
    }
    if (max != null && value > max) {
      return false;
    }
    return true;
  }
}
