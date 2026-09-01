package dev.mintychochip.action;

import dev.mintychochip.container.ActionType;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Immutable action type descriptor exposed through the public action contract. */
public record ActionTypeImpl(@NotNull String name, @NotNull Key key) implements ActionType {

  @Override
  @Contract(pure = true)
  public @NotNull String toString() {
    return name;
  }
}
