package org.aincraft.api.event;

import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Full-featured event bus supporting:
 *
 * <ul>
 *   <li>Typed, priority-ordered listeners
 *   <li>Cancellable events via {@link Cancellable} / {@link AbstractCancellableEvent}
 *   <li>Synchronous posting ({@link #post}) and asynchronous posting ({@link #postAsync})
 *   <li>Functional registration and annotation-driven registration ({@link Subscribe})
 *   <li>Per-listener {@code ignoreCancelled} filtering
 *   <li>Inheritance dispatch: a listener for a supertype receives subtypes
 * </ul>
 */
@AvailableSince("2.0.0")
public interface EventBus {

  // --------------------------------------------------------- functional subscribe

  default <E extends Event> @NotNull Subscription subscribe(
      @NotNull Class<E> eventType, @NotNull EventListener<? super E> listener) {
    return subscribe(eventType, EventPriority.NORMAL, false, null, listener);
  }

  default <E extends Event> @NotNull Subscription subscribe(
      @NotNull Class<E> eventType,
      @NotNull EventPriority priority,
      @NotNull EventListener<? super E> listener) {
    return subscribe(eventType, priority, false, null, listener);
  }

  default <E extends Event> @NotNull Subscription subscribe(
      @NotNull Class<E> eventType,
      @NotNull EventPriority priority,
      boolean ignoreCancelled,
      @NotNull EventListener<? super E> listener) {
    return subscribe(eventType, priority, ignoreCancelled, null, listener);
  }

  /**
   * Core subscribe method.
   *
   * @param eventType event class to listen for (supertype listeners receive subtypes)
   * @param priority ordering priority
   * @param ignoreCancelled if true, this listener is skipped when a {@link Cancellable} event is
   *     already cancelled
   * @param executor optional per-listener executor; when non-null the listener is always dispatched
   *     on that executor even during a synchronous {@link #post}. Null means inline.
   * @param listener handler
   */
  <E extends Event> @NotNull Subscription subscribe(
      @NotNull Class<E> eventType,
      @NotNull EventPriority priority,
      boolean ignoreCancelled,
      Executor executor,
      @NotNull EventListener<? super E> listener);

  // ---------------------------------------------------------- annotation register

  /**
   * Scans {@code instance} for {@link Subscribe}-annotated methods and registers each.
   *
   * @return subscriptions created (one per method)
   */
  @NotNull
  List<Subscription> register(@NotNull Object instance);

  /**
   * Unregisters all {@link Subscribe} methods previously registered from {@code instance}. No-op if
   * the instance was never registered.
   */
  void unregister(@NotNull Object instance);

  // --------------------------------------------------------------- lifecycle

  void unsubscribe(@NotNull Subscription subscription);

  // -------------------------------------------------------------------- post

  /**
   * Posts {@code event} synchronously on the caller thread.
   *
   * <p>Listeners are invoked in priority order (LOWEST → MONITOR). Cancellable filtering is
   * respected per-listener. Exceptions from one listener do not prevent subsequent listeners.
   *
   * @return the same event instance (possibly mutated / cancelled)
   */
  <E extends Event> @NotNull E post(@NotNull E event);

  /**
   * Posts {@code event} asynchronously using the bus default executor. Listeners are still invoked
   * sequentially in priority order so cancellation propagates deterministically, but the whole
   * chain runs off the caller thread.
   *
   * @return future completing with the same event instance after all listeners have run
   */
  default <E extends Event> @NotNull CompletableFuture<E> postAsync(@NotNull E event) {
    return postAsync(event, null);
  }

  /**
   * Posts {@code event} asynchronously.
   *
   * @param executor override executor; if null the bus default is used
   */
  <E extends Event> @NotNull CompletableFuture<E> postAsync(@NotNull E event, Executor executor);
}
