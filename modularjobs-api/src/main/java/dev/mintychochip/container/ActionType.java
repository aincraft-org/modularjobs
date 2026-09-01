package dev.mintychochip.container;

import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Represents a type of in-game action a player can perform. */
@NonExtendable
public interface ActionType extends Keyed {

  /** Name. */
  @Contract(pure = true)
  @NotNull
  String name();
}
