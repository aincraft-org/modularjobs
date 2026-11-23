package dev.mintychochip.databag.condition;

/**
 * Weather dimension on {@link ConditionContext}.
 */
public enum WeatherState {
  /** Thundering weather ({@code thundering=true}). */
  THUNDERING,
  /** Raining weather ({@code raining=true}) without requiring thunder. */
  RAINING,
  /** Clear weather ({@code raining=false} and {@code thundering=false}). */
  CLEAR
}
