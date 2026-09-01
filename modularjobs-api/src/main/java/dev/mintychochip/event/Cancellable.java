package dev.mintychochip.event;

import org.jetbrains.annotations.Contract;

/** Pure cancel contract for domain events (no Bukkit dependency). */
public interface Cancellable {

  /** Returns whether cancelled. */
  @Contract(pure = true)
  boolean isCancelled();

  /** Sets the cancelled. */
  void setCancelled(boolean cancelled);
}
