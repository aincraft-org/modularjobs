package dev.mintychochip.container;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default immutable {@link PayableAmount} holding a {@link BigDecimal} value and an optional {@link
 * Currency}.
 *
 * <p>Instances are created through the package-private constructors in the {@code
 * dev.mintychochip.container} package and are value-typed on both the amount and the currency.
 */
final class PayableAmountImpl implements PayableAmount {

  private final BigDecimal amount;
  private final Currency currency;

  /**
   * Creates an amount without an explicit currency.
   *
   * @param amount the reward quantity, must not be {@code null}
   */
  PayableAmountImpl(@NotNull BigDecimal amount) {
    this.amount = Objects.requireNonNull(amount, "amount cannot be null");
    this.currency = null;
  }

  /**
   * Creates an amount carrying the given currency.
   *
   * @param amount the reward quantity, must not be {@code null}
   * @param currency the currency of the amount, or {@code null} if the amount has no currency (for
   *     example a plain experience quantity)
   */
  PayableAmountImpl(@NotNull BigDecimal amount, @Nullable Currency currency) {
    this.amount = Objects.requireNonNull(amount, "amount cannot be null");
    this.currency = currency;
  }

  /** {@inheritDoc} */
  @Override
  @Contract(pure = true)
  public @NotNull BigDecimal value() {
    return amount;
  }

  /** {@inheritDoc} */
  @Contract(pure = true)
  @Override
  public @NotNull Optional<Currency> currency() {
    return Optional.ofNullable(currency);
  }

  @Override
  @Contract(pure = true)
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PayableAmountImpl that)) {
      return false;
    }
    return Objects.equals(amount, that.amount) && Objects.equals(currency, that.currency);
  }

  @Override
  @Contract(pure = true)
  public int hashCode() {
    return Objects.hash(amount, currency);
  }

  /**
   * Returns the amount as its plain decimal string, or the amount followed by its currency symbol
   * when a currency is present.
   *
   * @return the textual representation of this amount
   */
  @Contract(pure = true)
  @Override
  public @NotNull String toString() {
    return currency().isEmpty() ? amount.toString() : amount + " " + currency().get().symbol();
  }
}
