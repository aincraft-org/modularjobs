package dev.mintychochip;

import java.math.BigDecimal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A pure function mapping payout inputs to a reward amount.
 *
 * <p>Implementations must be deterministic and side-effect free.
 */
@FunctionalInterface
public interface PayableCurve {

  /**
   * Evaluates the curve for the given parameters.
   *
   * @param parameters the curve inputs
   * @return the curve value for {@code parameters}
   */
  @Contract(pure = true)
  @NotNull
  BigDecimal evaluate(@NotNull Parameters parameters);

  /** Inputs available to a payout curve. */
  record Parameters(@NotNull BigDecimal base, int level, int jobs) {}
}
