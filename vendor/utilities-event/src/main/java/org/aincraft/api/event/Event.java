package org.aincraft.api.event;

import org.jetbrains.annotations.ApiStatus.AvailableSince;

/**
 * Marker interface for all events dispatched through {@link EventBus}.
 *
 * <p>Implement this directly for simple events or extend {@link AbstractCancellableEvent} for
 * cancellable ones.
 */
@AvailableSince("2.0.0")
public interface Event {}
