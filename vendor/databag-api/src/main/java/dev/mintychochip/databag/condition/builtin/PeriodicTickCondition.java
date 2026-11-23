package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;

/**
 * Matches when (ticksLived - offset) is divisible by period.
 *
 * @param period positive tick interval
 * @param offset tick offset subtracted before the modulo check
 */
public record PeriodicTickCondition(int period, int offset) implements Condition {

  /**
   * Requires a positive tick period.
   */
  public PeriodicTickCondition {
    if (period <= 0) {
      throw new IllegalArgumentException("period must be > 0, got " + period);
    }
  }

  @Override
  public boolean test(ConditionContext context) {
    Integer lived = context.ticksLived();
    if (lived == null) {
      return false;
    }
    return (lived - offset) % period == 0;
  }
}
