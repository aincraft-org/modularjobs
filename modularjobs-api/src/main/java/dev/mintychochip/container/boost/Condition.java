package dev.mintychochip.container.boost;

import dev.mintychochip.Bridge;
import dev.mintychochip.container.BoostContext;
import dev.mintychochip.container.boost.factories.ConditionFactory;
import org.jetbrains.annotations.NotNull;

/** Predicate evaluated against a {@link BoostContext}. */
@FunctionalInterface
public interface Condition {

  /**
   * Tests whether this condition applies to the supplied context.
   *
   * @param context context to evaluate
   * @return {@code true} when the condition applies
   */
  boolean applies(@NotNull BoostContext context);

  /**
   * Lazy factory access — must not run at class-init time (unit tests load {@link Condition}
   * without a live Bukkit server / Bridge).
   */
  private static @NotNull ConditionFactory factory() {
    return Bridge.bridge().conditionFactory();
  }

  /**
   * Creates a biome condition.
   *
   * @param biomeKey biome id or namespaced key (e.g. {@code plains}, {@code minecraft:desert})
   */
  static @NotNull Condition biome(@NotNull String biomeKey) {
    return factory().biome(biomeKey);
  }

  /**
   * Creates a world condition.
   *
   * @param worldName world name or namespaced key
   * @return a condition matching the world
   */
  static @NotNull Condition world(@NotNull String worldName) {
    return factory().world(worldName);
  }

  /** Player resource. */
  static @NotNull Condition playerResource(
      @NotNull PlayerResourceType type, double expected, @NotNull RelationalOperator operator) {
    return factory().playerResource(type, expected, operator);
  }

  /** Sneaking. */
  static @NotNull Condition sneaking(boolean state) {
    return factory().sneaking(state);
  }

  /** Sprinting. */
  static @NotNull Condition sprinting(boolean state) {
    return factory().sprinting(state);
  }

  /**
   * Creates a liquid condition.
   *
   * @param materialKey liquid material name or key ({@code water}/{@code lava})
   */
  static @NotNull Condition liquid(@NotNull String materialKey) {
    return factory().liquid(materialKey);
  }

  /**
   * Creates a potion type condition.
   *
   * @param potionEffectTypeKey effect id or namespaced key (e.g. {@code speed}, {@code
   *     minecraft:strength})
   */
  static @NotNull Condition potionType(@NotNull String potionEffectTypeKey) {
    return factory().potionType(potionEffectTypeKey);
  }

  /**
   * Creates a potion intensity condition.
   *
   * @param potionEffectTypeKey effect id or namespaced key
   */
  static @NotNull Condition potion(
      @NotNull String potionEffectTypeKey,
      int expected,
      @NotNull PotionConditionType conditionType,
      @NotNull RelationalOperator operator) {
    return factory().potion(potionEffectTypeKey, expected, conditionType, operator);
  }

  /** Weather. */
  static @NotNull Condition weather(@NotNull WeatherState state) {
    return factory().weather(state);
  }

  /** And. */
  default @NotNull Condition and(@NotNull Condition other) {
    return compose(other, LogicalOperator.AND);
  }

  /** Or. */
  default @NotNull Condition or(@NotNull Condition other) {
    return compose(other, LogicalOperator.OR);
  }

  /** Negate. */
  default @NotNull Condition negate() {
    return factory().negate(this);
  }

  /** Compose. */
  default @NotNull Condition compose(@NotNull Condition other, @NotNull LogicalOperator operator) {
    return factory().compose(this, other, operator);
  }
}
