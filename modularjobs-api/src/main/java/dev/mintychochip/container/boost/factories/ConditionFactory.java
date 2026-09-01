package dev.mintychochip.container.boost.factories;

import dev.mintychochip.container.boost.Condition;
import dev.mintychochip.container.boost.LogicalOperator;
import dev.mintychochip.container.boost.PlayerResourceType;
import dev.mintychochip.container.boost.PotionConditionType;
import dev.mintychochip.container.boost.RelationalOperator;
import dev.mintychochip.container.boost.WeatherState;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

/** Type. */
@Internal
public interface ConditionFactory {

  /**
   * Creates a biome condition.
   *
   * @param biomeKey biome id or namespaced key (resolved at evaluation)
   */
  @NotNull
  Condition biome(@NotNull String biomeKey);

  /**
   * Creates a world condition.
   *
   * @param worldName world name or namespaced key (resolved at evaluation)
   */
  @NotNull
  Condition world(@NotNull String worldName);

  /** Player resource. */
  @NotNull
  Condition playerResource(
      @NotNull PlayerResourceType type, double expected, @NotNull RelationalOperator operator);

  /** Sneaking. */
  @NotNull
  Condition sneaking(boolean state);

  /** Sprinting. */
  @NotNull
  Condition sprinting(boolean state);

  /** Negate. */
  @NotNull
  Condition negate(@NotNull Condition condition);

  /**
   * Creates a liquid condition.
   *
   * @param materialKey liquid material name or key ({@code water}/{@code lava})
   */
  @NotNull
  Condition liquid(@NotNull String materialKey);

  /**
   * Creates a potion type condition.
   *
   * @param potionEffectTypeKey effect id or namespaced key
   */
  @NotNull
  Condition potionType(@NotNull String potionEffectTypeKey);

  /**
   * Creates a potion intensity condition.
   *
   * @param potionEffectTypeKey effect id or namespaced key
   */
  @NotNull
  Condition potion(
      @NotNull String potionEffectTypeKey,
      int expected,
      @NotNull PotionConditionType conditionType,
      @NotNull RelationalOperator operator);

  /** Compose. */
  @NotNull
  Condition compose(@NotNull Condition a, @NotNull Condition b, @NotNull LogicalOperator operator);

  /** Weather. */
  @NotNull
  Condition weather(@NotNull WeatherState state);

  /** Job. */
  @NotNull
  Condition job(@NotNull String jobKey);

  /** Job any. */
  @NotNull
  Condition jobAny(@NotNull String... jobKeys);
}
