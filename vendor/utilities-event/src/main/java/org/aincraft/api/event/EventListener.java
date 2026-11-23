package org.aincraft.api.event;

import org.jetbrains.annotations.ApiStatus.AvailableSince;

/**
 * Functional handler for a specific {@link Event} subtype.
 *
 * @param <E> event type
 */
@FunctionalInterface
@AvailableSince("2.0.0")
public interface EventListener<E extends Event> {

  void handle(E event) throws Exception;
}
