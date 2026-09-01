package dev.mintychochip.common.event;

import dev.mintychochip.event.EventBus;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

/** Thread-safe synchronous {@link EventBus} implementation. */
public final class EventBusImpl implements EventBus {

  private final List<Consumer<Object>> listeners = new CopyOnWriteArrayList<>();

  @Override
  public void subscribe(@NotNull Consumer<Object> listener) {
    listeners.add(Objects.requireNonNull(listener, "listener"));
  }

  @Override
  public <T> @NotNull T publish(@NotNull T event) {
    Objects.requireNonNull(event, "event");
    for (Consumer<Object> listener : listeners) {
      listener.accept(event);
    }
    return event;
  }
}
