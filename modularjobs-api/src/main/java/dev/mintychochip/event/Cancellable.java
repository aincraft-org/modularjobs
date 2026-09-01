package dev.mintychochip.event;

import org.jetbrains.annotations.Contract;

/** Pure cancel contract for domain events (no Bukkit dependency). */
public interface Cancellable extends org.aincraft.api.event.Cancellable {

  /** Returns whether cancelled. */
  @Override
  @Contract(pure = true)
  boolean isCancelled();

  /** Sets the cancelled. */
  @Override
  void setCancelled(boolean cancelled);
}
