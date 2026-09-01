package dev.mintychochip.service;

import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Context;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/** Registers custom action types and reports performed actions to ModularJobs. */
public interface ActionService {

  /**
   * Registers a custom action type.
   *
   * @param key unique action key
   * @param name human-readable action name
   * @return the registered action type
   * @throws IllegalArgumentException if the name is blank or the key is already registered
   */
  @NotNull
  ActionType register(@NotNull Key key, @NotNull String name);

  /**
   * Reports that a player performed an action with the given context.
   *
   * @param playerId player identifier
   * @param type registered action type
   * @param context action context
   * @throws IllegalArgumentException if the action type is not registered
   */
  void report(@NotNull UUID playerId, @NotNull ActionType type, @NotNull Context context);

  /**
   * Reports that a player performed an action with an already-resolved context key.
   *
   * @param playerId player identifier
   * @param type registered action type
   * @param contextKey task context key
   * @throws IllegalArgumentException if the action type is not registered
   */
  default void report(@NotNull UUID playerId, @NotNull ActionType type, @NotNull Key contextKey) {
    Objects.requireNonNull(contextKey, "contextKey");
    report(playerId, type, new Context.KeyContext(contextKey));
  }
}
