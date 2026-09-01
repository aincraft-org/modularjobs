package dev.mintychochip.container;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/** API-local immutable currency value created through {@link Currency#of(String, String)}. */
record CurrencyImpl(@NotNull String identifier, @NotNull String symbol) implements Currency {
  CurrencyImpl {
    Objects.requireNonNull(identifier, "identifier cannot be null");
    Objects.requireNonNull(symbol, "symbol cannot be null");
  }
}
