package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import org.jetbrains.annotations.Nullable;

/**
 * Matches world time against optional min/max bounds, optionally modulo {@code period}.
 *
 * @param min inclusive minimum day time, or {@code null} for no lower bound
 * @param max inclusive maximum day time, or {@code null} for no upper bound
 * @param period optional modulus applied before comparing bounds
 */
public record TimeCheckCondition(
    @Nullable Long min, @Nullable Long max, @Nullable Long period) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    Long dayTime = context.dayTime();
    if (dayTime == null) {
      return false;
    }
    long t = (period == null || period <= 0) ? dayTime : Math.floorMod(dayTime, period);
    return (min == null || t >= min) && (max == null || t <= max);
  }
}
