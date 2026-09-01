package dev.mintychochip;

import java.math.BigDecimal;
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

  /** Inputs to {@link LevelingCurve#evaluate(Parameters)}. */
  record Parameters(int level) {}
}
