package org.aincraft.api.event;

import org.jetbrains.annotations.ApiStatus.AvailableSince;

/** Convenience base for cancellable events. */
@AvailableSince("2.0.0")
public abstract class AbstractCancellableEvent implements Event, Cancellable {

  private volatile boolean cancelled;

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
