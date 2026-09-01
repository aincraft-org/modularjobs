package dev.mintychochip.container.boost.factories;

import dev.mintychochip.container.Boost;
import java.math.BigDecimal;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for constructing {@link Boost} instances.
 *
 * <p>Internal API: obtain an instance through {@link Boost#factory()} rather than implementing or
 * instantiating this interface directly.
 */
@Internal
public interface BoostFactory {

  /**
   * Creates a boost that adds the given amount.
   *
   * @param amount amount added by the boost
   * @return an additive boost
   */
  @NotNull
  Boost additive(@NotNull BigDecimal amount);

  /**
   * Creates a boost that multiplies by the given amount.
   *
   * @param amount factor the boost multiplies by
   * @return a multiplicative boost
   */
  @NotNull
  Boost multiplicative(@NotNull BigDecimal amount);
}
