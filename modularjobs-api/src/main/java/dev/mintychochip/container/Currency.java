package dev.mintychochip.container;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Represents a currency for payable amounts. Extensible for custom currencies via plugins. */
public interface Currency {

  Currency USD = of("USD", "$");
  Currency EUR = of("EUR", "€");
  Currency GBP = of("GBP", "£");
  Currency JPY = of("JPY", "¥");
  Currency CNY = of("CNY", "¥");

  /**
   * Creates a new currency with the given identifier and symbol. Use this for custom currencies in
   * plugins.
   */
  @Contract(pure = true)
  static @NotNull Currency of(@NotNull String identifier, @NotNull String symbol) {
    return new CurrencyImpl(identifier, symbol);
  }

  /** Identifier. */
  @NotNull
  String identifier();

  /** Symbol. */
  @NotNull
  String symbol();
}
