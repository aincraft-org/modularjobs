package dev.mintychochip.boost;

import dev.mintychochip.container.Boost;
import java.math.BigDecimal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A directional boost that adds a fixed {@code amount} to the payout: the resulting payout is
 * {@code base + amount}. Direction is implied by the sign of {@code amount} (negative subtracts).
 */
public record AdditiveBoostImpl(@NotNull BigDecimal amount) implements Boost {

  @Override
  @Contract(pure = true)
  public @NotNull BigDecimal boost(@NotNull BigDecimal amount) {
    return amount.add(this.amount);
  }
}
