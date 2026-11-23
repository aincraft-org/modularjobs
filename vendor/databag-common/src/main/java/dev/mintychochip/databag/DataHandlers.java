package dev.mintychochip.databag;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;

/**
 * Process-wide registry of {@link DataHandler}s. Keys must be unique.
 */
public final class DataHandlers {

  private static final ConcurrentHashMap<String, DataHandler<?>> BY_KEY = new ConcurrentHashMap<>();

  private DataHandlers() {}

  /**
   * Registers a handler under its namespaced key.
   *
   * @param <T> value type handled by {@code handler}
   *
   * @param handler handler to register
   * @throws NullPointerException if {@code handler} is {@code null}
   * @throws IllegalStateException if a different handler is already registered for the key
   */
  public static <T> void register(DataHandler<T> handler) {
    Objects.requireNonNull(handler, "handler");
    String id = handler.key().asString();
    DataHandler<?> previous = BY_KEY.putIfAbsent(id, handler);
    if (previous != null && previous != handler) {
      throw new IllegalStateException("DataHandler already registered for " + id);
    }
  }

  /**
   * Removes the handler registered for a key, if present.
   *
   * @param key key whose handler should be removed
   * @throws NullPointerException if {@code key} is {@code null}
   */
  public static void unregister(Key key) {
    BY_KEY.remove(Objects.requireNonNull(key, "key").asString());
  }

  /**
   * Finds the handler registered for a key.
   *
   * @param key key to look up
   * @return an optional containing the registered handler, or empty when none is registered
   * @throws NullPointerException if {@code key} is {@code null}
   */
  public static Optional<DataHandler<?>> get(Key key) {
    return Optional.ofNullable(BY_KEY.get(Objects.requireNonNull(key, "key").asString()));
  }
}
