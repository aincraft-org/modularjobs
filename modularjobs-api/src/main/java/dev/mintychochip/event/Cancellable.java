package dev.mintychochip.event;

/** Pure cancel contract for domain events (no Bukkit dependency). */
public interface Cancellable extends org.aincraft.api.event.Cancellable {

  /** Returns whether cancelled. */
  @Override
  boolean isCancelled();

  /** Sets the cancelled. */
  @Override
  void setCancelled(boolean cancelled);
}
