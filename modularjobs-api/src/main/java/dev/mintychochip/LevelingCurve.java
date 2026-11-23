package dev.mintychochip;

import java.math.BigDecimal;
import java.util.Objects;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A pure function mapping leveling inputs to an experience value.
 *
 * <p>Implementations must be deterministic and side-effect free.
 */
@FunctionalInterface
public interface LevelingCurve {

  /**
   * Evaluates the curve for the given parameters.
   *
   * @param parameters the curve inputs
   * @return the curve value for {@code parameters}
   */
  @Contract(pure = true)
  @NotNull
  BigDecimal evaluate(@NotNull Parameters parameters);

  /**
   * Derives the highest level whose threshold is met by the supplied experience.
   *
   * @return the derived level, clamped to {@code [1, maxLevel]}
   */
  @Contract(pure = true)
  static int levelForExperience(
      @NotNull LevelingCurve curve, int maxLevel, @NotNull BigDecimal experience) {
    Objects.requireNonNull(curve, "curve");
    Objects.requireNonNull(experience, "experience");
    if (maxLevel <= 0) {
      return 1;
    }

    int low = 1;
    int upperBound = maxLevel;
    int result = 1;
    while (low <= upperBound) {
      int candidate = (low + upperBound) >>> 1;
      BigDecimal threshold = curve.evaluate(new Parameters(candidate));
      if (experience.compareTo(threshold) >= 0) {
        result = candidate;
        low = candidate + 1;
      } else {
        upperBound = candidate - 1;
      }
    }
    return result;
  }

  /** Inputs to {@link LevelingCurve#evaluate(Parameters)}. */
  record Parameters(int level) {}
}
