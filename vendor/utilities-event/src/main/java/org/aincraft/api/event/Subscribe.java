package org.aincraft.api.event;

import org.jetbrains.annotations.ApiStatus.AvailableSince;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event listener for annotation-driven registration.
 *
 * <p>The method must have exactly one parameter whose type extends {@link Event}. Registered via
 * {@link EventBus#register(Object)}.
 *
 * <pre>{@code
 * class MyListener {
 *   @Subscribe(priority = EventPriority.HIGH, ignoreCancelled = true)
 *   public void onPlayerJoin(PlayerJoinEvent event) { ... }
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@AvailableSince("2.0.0")
public @interface Subscribe {

  EventPriority priority() default EventPriority.NORMAL;

  /**
   * When true the method is skipped if the event is already cancelled ({@link
   * Cancellable#isCancelled()}).
   */
  boolean ignoreCancelled() default false;
}
