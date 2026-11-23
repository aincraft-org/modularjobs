package dev.mintychochip.databag.condition;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;

/**
 * Process-wide registry of {@link ConditionHandler}s. Built-in vanilla /
 * {@code modularjobs:*} kinds stay in the gson serializer; this is for
 * third-party ids so ModularJobs does not own them.
 */
public final class ConditionHandlers {

  private static final ConcurrentHashMap<String, ConditionHandler> BY_ID = new ConcurrentHashMap<>();

  private ConditionHandlers() {}

  /**
   * Registers a handler for its {@link ConditionHandler#id()}. Duplicate ids with
   * a different handler instance fail.
   *
   * @param handler the handler to register
   * @throws IllegalStateException when a different handler is already registered
   *     for the same id
   */
  public static void register(ConditionHandler handler) {
    Objects.requireNonNull(handler, "handler");
    String id = handler.id().asString();
    ConditionHandler previous = BY_ID.putIfAbsent(id, handler);
    if (previous != null && previous != handler) {
      throw new IllegalStateException("ConditionHandler already registered for " + id);
    }
  }

  /**
   * Removes the handler registered for {@code id}, if any.
   *
   * @param id the handler id to unregister
   */
  public static void unregister(Key id) {
    BY_ID.remove(Objects.requireNonNull(id, "id").asString());
  }

  /**
   * Returns the handler registered for {@code id}.
   *
   * @param id the handler id
   * @return the registered handler, or empty when none is registered
   */
  public static Optional<ConditionHandler> get(Key id) {
    return Optional.ofNullable(BY_ID.get(Objects.requireNonNull(id, "id").asString()));
  }

  /**
   * Returns the handler registered for a raw id string.
   *
   * <p>Blank ids return empty. An exact registry match is tried first; otherwise a
   * namespaced {@link Key} is parsed from {@code rawId}.
   *
   * @param rawId the handler id string
   * @return the registered handler, or empty when none matches
   */
  public static Optional<ConditionHandler> get(String rawId) {
    if (rawId == null || rawId.isBlank()) {
      return Optional.empty();
    }
    ConditionHandler exact = BY_ID.get(rawId);
    if (exact != null) {
      return Optional.of(exact);
    }
    int colon = rawId.indexOf(':');
    if (colon <= 0) {
      return Optional.empty();
    }
    try {
      return get(Key.key(rawId));
    } catch (RuntimeException ignored) {
      return Optional.empty();
    }
  }

  /**
   * Returns the first registered handler that can serialize {@code condition}.
   *
   * @param condition the condition instance
   * @return a handler whose {@link ConditionHandler#write(Condition)} accepts the
   *     condition, or empty when none do
   */
  public static Optional<ConditionHandler> findWriter(Condition condition) {
    Objects.requireNonNull(condition, "condition");
    return BY_ID.entrySet().stream()
        .sorted(java.util.Map.Entry.comparingByKey())
        .map(java.util.Map.Entry::getValue)
        .filter(handler -> handler.write(condition).isPresent())
        .findFirst();
  }
}
