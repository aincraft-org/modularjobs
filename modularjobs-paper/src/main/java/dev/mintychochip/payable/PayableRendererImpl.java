package dev.mintychochip.payable;

import dev.mintychochip.container.Currency;
import dev.mintychochip.container.Payable;
import dev.mintychochip.container.PayableAmount;
import dev.mintychochip.container.PayableRenderer;
import java.math.RoundingMode;
import java.text.NumberFormat;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

/** Renders the built-in payable types used by Paper presentation surfaces. */
final class PayableRendererImpl implements PayableRenderer {

  private static final Key ECONOMY = Key.key("modularjobs", "economy");
  private static final Key EXPERIENCE = Key.key("modularjobs", "experience");
  private static final TextColor ECONOMY_COLOR = TextColor.color(0x7ED278);
  private static final TextColor EXPERIENCE_COLOR = TextColor.color(0xDAC65C);
  private static final int DECIMAL_PLACES = 2;

  @Override
  public @NotNull Component render(@NotNull Payable payable) {
    String amount = format(payable.amount());
    Key type = payable.type().key();
    if (ECONOMY.equals(type)) {
      String symbol = payable.amount().currency().orElse(Currency.USD).symbol();
      return Component.text(symbol + amount, ECONOMY_COLOR);
    }
    if (EXPERIENCE.equals(type)) {
      return Component.text(amount + "xp", EXPERIENCE_COLOR);
    }
    throw new IllegalArgumentException("No renderer for payable type: " + type.asString());
  }

  private static @NotNull String format(@NotNull PayableAmount amount) {
    NumberFormat formatter = NumberFormat.getNumberInstance();
    formatter.setMinimumFractionDigits(DECIMAL_PLACES);
    formatter.setMaximumFractionDigits(DECIMAL_PLACES);
    formatter.setRoundingMode(RoundingMode.HALF_UP);
    return formatter.format(amount.value());
  }
}
