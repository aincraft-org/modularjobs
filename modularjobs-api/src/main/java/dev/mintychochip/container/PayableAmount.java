package dev.mintychochip.container;

import java.math.BigDecimal;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Type. */
@NonExtendable
public sealed interface PayableAmount permits PayableAmountImpl {

  /** Create. */
  @Contract(pure = true)
  static @NotNull PayableAmount create(@NotNull BigDecimal amount) {
    return new PayableAmountImpl(amount);
  }

  /** Create. */
  @Contract(pure = true)
  static @NotNull PayableAmount create(@NotNull BigDecimal amount, @NotNull Currency currency) {
    return new PayableAmountImpl(amount, currency);
  }

  /** Value. */
  @Contract(pure = true)
  @NotNull
  BigDecimal value();

  /** API member. */
  @NotNull
  @Contract(pure = true)
  Optional<Currency> currency();

  /**
   * Returns a new PayableAmount with the given currency, or this if already has the same currency.
   */
  @Contract(pure = true)
  default @NotNull PayableAmount withCurrency(@NotNull Currency currency) {
    return currency()
        .map(c -> c.equals(currency) ? this : create(value(), currency))
        .orElseGet(() -> create(value(), currency));
  }

  /** Returns a new PayableAmount without a currency, or this if already has no currency. */
  @Contract(pure = true)
  default @NotNull PayableAmount withoutCurrency() {
    return currency().isPresent() ? create(value()) : this;
  }
}
