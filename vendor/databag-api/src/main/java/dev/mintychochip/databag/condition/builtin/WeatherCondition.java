package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import dev.mintychochip.databag.condition.WeatherState;

/**
 * Matches snapshot weather against {@code state}. Fails closed when the weather snapshot is
 * absent.
 *
 * @param state the required weather state
 */
public record WeatherCondition(WeatherState state) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (context.weather() == null) {
      return false;
    }
    return state == context.weather();
  }
}
