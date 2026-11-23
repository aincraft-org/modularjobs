package org.aincraft.api.event;

import org.jetbrains.annotations.ApiStatus.AvailableSince;

/**
 * Indicates an {@link Event} can be cancelled.
 *
 * <p>Cancellation is cooperative: listeners may call {@link #setCancelled(boolean)} and later
 * listeners can observe {@link #isCancelled()}. When a listener is registered with {@code
 * ignoreCancelled = true} it is skipped once the event is cancelled.
 */
@AvailableSince("2.0.0")
public interface Cancellable {

  boolean isCancelled();

  void setCancelled(boolean cancelled);
}
