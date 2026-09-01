package dev.mintychochip.container;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Renders payable domain values for presentation surfaces. */
@FunctionalInterface
public interface PayableRenderer {

  /**
   * Renders the supplied payable.
   *
   * @param payable payable to present
   * @return rendered payable component
   * @throws IllegalArgumentException when the payable type has no presentation
   */
  @Contract(pure = true)
  @NotNull
  Component render(@NotNull Payable payable);
}
