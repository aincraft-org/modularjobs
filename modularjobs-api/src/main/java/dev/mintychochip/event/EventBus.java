package dev.mintychochip.event;

import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

/** Synchronous, type-agnostic event publication contract. */
public interface EventBus {

  /**
   * Registers a listener that receives every subsequently published event.
   *
   * @param listener listener to register
   */
  void subscribe(@NotNull Consumer<Object> listener);

  /**
   * Publishes an event synchronously and returns the same instance.
   *
   * @param event event to publish
   * @param <T> event type
   * @return the published event
   */
  <T> @NotNull T publish(@NotNull T event);
}
