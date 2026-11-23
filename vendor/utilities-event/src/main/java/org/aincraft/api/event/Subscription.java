package org.aincraft.api.event;

import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;

/** Handle returned by {@link EventBus#subscribe} that can be used to unsubscribe. */
@AvailableSince("2.0.0")
public interface Subscription {

  /** The event type this subscription listens for. */
  @NotNull
  Class<? extends Event> eventType();

  /** Priority used for ordering. */
  @NotNull
  EventPriority priority();

  /** Whether the subscription skips cancelled events. */
  boolean ignoreCancelled();

  /** Whether this subscription is still active on the bus. */
  boolean isActive();

  /** Unsubscribe from the bus. Idempotent. */
  void unsubscribe();
}
