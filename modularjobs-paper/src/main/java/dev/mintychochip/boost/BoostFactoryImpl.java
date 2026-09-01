package dev.mintychochip.boost;

import dev.mintychochip.boost.conditions.SnapshotCondition;
import dev.mintychochip.container.Boost;
import dev.mintychochip.container.boost.Condition;
import dev.mintychochip.container.boost.LogicalOperator;
import dev.mintychochip.container.boost.PlayerResourceType;
import dev.mintychochip.container.boost.PotionConditionType;
import dev.mintychochip.container.boost.RelationalOperator;
import dev.mintychochip.container.boost.WeatherState;
import dev.mintychochip.container.boost.factories.BoostFactory;
import dev.mintychochip.container.boost.factories.ConditionFactory;
import dev.mintychochip.databag.condition.Conditions;
import java.math.BigDecimal;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Boost and condition factory. Conditions are snapshot-graph types from {@code
 * dev.mintychochip.databag}, adapted onto the boost {@link Condition} interface.
 */
public final class BoostFactoryImpl implements BoostFactory, ConditionFactory {

  public static final BoostFactoryImpl INSTANCE = new BoostFactoryImpl();

  private BoostFactoryImpl() {}

  @Override
  @Contract(pure = true)
  public @NotNull Boost additive(@NotNull BigDecimal amount) {
    return new AdditiveBoostImpl(amount);
  }

  @Override
  @Contract(pure = true)
  public @NotNull Boost multiplicative(@NotNull BigDecimal amount) {
    return new MultiplicativeBoostImpl(amount);
  }

  @Override
  @Contract(pure = true)
  public @NotNull Condition biome(@NotNull String biomeKey) {
    return SnapshotCondition.wrap(Conditions.biome(toKey(biomeKey)));
  }

  @Override
  @Contract(pure = true)
  public @NotNull Condition world(@NotNull String worldName) {
    return SnapshotCondition.wrap(Conditions.world(worldName));
  }

  @Override
  @Contract(pure = true)
  public @NotNull Condition playerResource(
      @NotNull PlayerResourceType type, double expected, @NotNull RelationalOperator operator) {
    return SnapshotCondition.wrap(
        Conditions.playerResource(mapResource(type), mapOperator(operator), expected));
  }

  @Override
  @Contract(pure = true)
  public @NotNull Condition sneaking(boolean state) {
    return SnapshotCondition.wrap(Conditions.sneaking(state));
  }

  @Override
  @Contract(pure = true)
  public @NotNull Condition sprinting(boolean state) {
    return SnapshotCondition.wrap(Conditions.sprinting(state));
  }

  @Override
  @Contract(pure = true)
  public @NotNull Condition negate(@NotNull Condition condition) {
    return SnapshotCondition.wrap(Conditions.inverted(SnapshotCondition.unwrap(condition)));
  }

  @Override
  @Contract(pure = true)
  public @NotNull Condition liquid(@NotNull String materialKey) {
    return SnapshotCondition.wrap(Conditions.fluid(toKey(materialKey)));
  }

  @Override
  @Contract(pure = true)
  public @NotNull Condition potionType(@NotNull String potionEffectTypeKey) {
    return SnapshotCondition.wrap(Conditions.potionPresent(toKey(potionEffectTypeKey)));
  }

  @Override
  @Contract(pure = true)
  public @NotNull Condition potion(
      @NotNull String potionEffectTypeKey,
      int expected,
      @NotNull PotionConditionType conditionType,
      @NotNull RelationalOperator operator) {
    Key key = toKey(potionEffectTypeKey);
    dev.mintychochip.databag.condition.RelationalOperator op = mapOperator(operator);
    return SnapshotCondition.wrap(
        switch (conditionType) {
          case AMPLIFIER -> Conditions.potionAmplifier(key, op, expected);
          case DURATION -> Conditions.potionDuration(key, op, expected);
        });
  }

  @Override
  @Contract(pure = true)
  public @NotNull Condition compose(
      @NotNull Condition a, @NotNull Condition b, @NotNull LogicalOperator operator) {
    dev.mintychochip.databag.condition.Condition left = SnapshotCondition.unwrap(a);
    dev.mintychochip.databag.condition.Condition right = SnapshotCondition.unwrap(b);
    dev.mintychochip.databag.condition.Condition composed =
        switch (operator) {
          case AND -> Conditions.allOf(left, right);
          case OR -> Conditions.anyOf(left, right);
          default -> ctx -> operator.test(left.test(ctx), right.test(ctx));
        };
    return SnapshotCondition.wrap(composed);
  }

  @Override
  @Contract(pure = true)
  public @NotNull Condition weather(@NotNull WeatherState state) {
    return SnapshotCondition.wrap(Conditions.weather(mapWeather(state)));
  }

  @Override
  @Contract(pure = true)
  public @NotNull Condition job(@NotNull String jobKey) {
    return SnapshotCondition.wrap(Conditions.job(jobKey));
  }

  @Override
  @Contract(pure = true)
  public @NotNull Condition jobAny(@NotNull String... jobKeys) {
    return SnapshotCondition.wrap(Conditions.jobAny(jobKeys));
  }

  /**
   * Normalizes a raw key string into a {@link Key}: trims and lowercases it, prepending the {@code
   * minecraft:} namespace when no namespace separator is present.
   *
   * @param raw the raw key string
   * @return the normalized {@link Key}
   * @throws IllegalArgumentException if {@code raw} is null or blank
   */
  @Contract(value = "null -> fail", pure = true)
  private static @NotNull Key toKey(@NotNull String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("key must be non-blank");
    }
    String trimmed = raw.trim();
    if (trimmed.contains(":")) {
      return Key.key(trimmed.toLowerCase());
    }
    return Key.key("minecraft", trimmed.toLowerCase());
  }

  @Contract(pure = true)
  private static @NotNull dev.mintychochip.databag.condition.PlayerResourceType mapResource(
      @NotNull PlayerResourceType type) {
    return switch (type) {
      case HEALTH -> dev.mintychochip.databag.condition.PlayerResourceType.HEALTH;
      case HUNGER -> dev.mintychochip.databag.condition.PlayerResourceType.HUNGER;
      case EXPERIENCE -> dev.mintychochip.databag.condition.PlayerResourceType.EXPERIENCE;
    };
  }

  @Contract(pure = true)
  private static @NotNull dev.mintychochip.databag.condition.RelationalOperator mapOperator(
      @NotNull RelationalOperator operator) {
    return switch (operator) {
      case LESS_THAN -> dev.mintychochip.databag.condition.RelationalOperator.LESS_THAN;
      case LESS_THAN_OR_EQUAL ->
          dev.mintychochip.databag.condition.RelationalOperator.LESS_THAN_OR_EQUAL;
      case GREATER_THAN -> dev.mintychochip.databag.condition.RelationalOperator.GREATER_THAN;
      case GREATER_THAN_OR_EQUAL ->
          dev.mintychochip.databag.condition.RelationalOperator.GREATER_THAN_OR_EQUAL;
      case EQUAL -> dev.mintychochip.databag.condition.RelationalOperator.EQUAL;
      case NOT_EQUAL -> dev.mintychochip.databag.condition.RelationalOperator.NOT_EQUAL;
    };
  }

  @Contract(pure = true)
  private static @NotNull dev.mintychochip.databag.condition.WeatherState mapWeather(
      @NotNull WeatherState state) {
    return switch (state) {
      case THUNDERING -> dev.mintychochip.databag.condition.WeatherState.THUNDERING;
      case RAINING -> dev.mintychochip.databag.condition.WeatherState.RAINING;
      case CLEAR -> dev.mintychochip.databag.condition.WeatherState.CLEAR;
    };
  }
}
