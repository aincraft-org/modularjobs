package dev.mintychochip.util;

import dev.mintychochip.container.Context;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves context objects to stable keys used by task persistence.
 *
 * <p>Strategies are matched by the context's exact runtime class. Missing strategies return {@code
 * null} rather than throwing.
 */
public final class KeyResolver {

  private final Map<Class<? extends Context>, KeyResolvingStrategy<?>> strategies = new HashMap<>();

  /**
   * Resolves a context using the strategy registered for its exact class.
   *
   * @param context context to resolve
   * @return resolved key, or {@code null} when no strategy is registered
   */
  public @Nullable Key resolve(@NotNull Context context) {
    Class<? extends Context> objectClass = context.getClass();
    KeyResolvingStrategy<?> raw = strategies.get(objectClass);
    if (raw == null) {
      return null;
    }
    return resolve(raw, context);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Context> @Nullable Key resolve(
      @NotNull KeyResolvingStrategy<?> raw, @NotNull Context object) {
    KeyResolvingStrategy<T> strategy = (KeyResolvingStrategy<T>) raw;
    T casted = (T) object;
    return strategy.resolve(casted);
  }

  /**
   * Registers or replaces a strategy for a context class.
   *
   * @param clazz exact context class handled by the strategy
   * @param strategy resolver invoked for matching contexts
   */
  public <T extends Context> void addStrategy(
      @NotNull Class<T> clazz, @NotNull KeyResolvingStrategy<T> strategy) {
    strategies.put(clazz, strategy);
  }

  /**
   * Strategy for converting one context type into a stable lookup key.
   *
   * @param <T> context type accepted by this strategy
   */
  @FunctionalInterface
  public interface KeyResolvingStrategy<T extends Context> {

    /**
     * Resolves a context into its persistence key.
     *
     * @param object context instance
     * @return key, or {@code null} when it cannot be resolved
     */
    @Nullable
    Key resolve(@NotNull T object);
  }
}
