package dev.mintychochip.boost.config;

import dev.mintychochip.boost.AdditiveBoostImpl;
import dev.mintychochip.boost.MultiplicativeBoostImpl;
import dev.mintychochip.boost.conditions.SnapshotCondition;
import dev.mintychochip.boost.config.BoostSourceConfig.BoostConfig;
import dev.mintychochip.boost.config.BoostSourceConfig.ConditionConfig;
import dev.mintychochip.boost.config.BoostSourceConfig.RuleConfig;
import dev.mintychochip.container.Boost;
import dev.mintychochip.container.BoostSource;
import dev.mintychochip.container.boost.Condition;
import dev.mintychochip.container.boost.RelationalOperator;
import dev.mintychochip.container.boost.RuledBoostSource;
import dev.mintychochip.container.boost.RuledBoostSource.Rule;
import dev.mintychochip.databag.condition.builtin.AllOfCondition;
import dev.mintychochip.databag.condition.builtin.AlwaysCondition;
import dev.mintychochip.databag.condition.builtin.AnyOfCondition;
import dev.mintychochip.databag.condition.builtin.BiomeCondition;
import dev.mintychochip.databag.condition.builtin.FluidCondition;
import dev.mintychochip.databag.condition.builtin.InvertedCondition;
import dev.mintychochip.databag.condition.builtin.JobCondition;
import dev.mintychochip.databag.condition.builtin.PlayerResourceCondition;
import dev.mintychochip.databag.condition.builtin.PotionAmplifierCondition;
import dev.mintychochip.databag.condition.builtin.PotionPresentCondition;
import dev.mintychochip.databag.condition.builtin.SneakingCondition;
import dev.mintychochip.databag.condition.builtin.SprintingCondition;
import dev.mintychochip.databag.condition.builtin.WeatherCondition;
import dev.mintychochip.databag.condition.builtin.WorldCondition;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Serializes runtime {@link BoostSource}/{@link Condition} graphs back to {@link BoostSourceConfig}
 * JSON models for editor export / round-trip.
 */
public final class BoostSourceConfigSerializer {

  private BoostSourceConfigSerializer() {}

  /** Serializes a whole boost source into its JSON configuration model. */
  @Contract(pure = true)
  public static @NotNull BoostSourceConfig serialize(@NotNull BoostSource source) {
    String key = source.key() != null ? source.key().asString() : "modularjobs:unknown";
    String description = source.description();
    List<RuleConfig> rules = new ArrayList<>();

    if (source instanceof RuledBoostSource ruled) {
      for (Rule rule : ruled.rules()) {
        rules.add(serializeRule(rule));
      }
    }

    return new BoostSourceConfig(key, description, null, rules);
  }

  /** Serialize only the rules list (for upgrade effect export). */
  @Contract(pure = true)
  public static @NotNull List<RuleConfig> serializeRules(@NotNull BoostSource source) {
    if (source instanceof RuledBoostSource ruled) {
      List<RuleConfig> rules = new ArrayList<>();
      for (Rule rule : ruled.rules()) {
        rules.add(serializeRule(rule));
      }
      return rules;
    }
    return List.of();
  }

  /** Serializes a single boost rule into its JSON configuration model. */
  @Contract(pure = true)
  public static @NotNull RuleConfig serializeRule(@NotNull Rule rule) {
    return new RuleConfig(
        rule.priority(), serializeCondition(rule.condition()), serializeBoost(rule.boost()));
  }

  /**
   * Serializes a boost into its type/amount model.
   *
   * @throws IllegalArgumentException for unsupported boost implementations
   */
  @Contract(pure = true)
  public static @NotNull BoostConfig serializeBoost(@NotNull Boost boost) {
    return switch (boost) {
      case MultiplicativeBoostImpl mult ->
          new BoostConfig("multiplicative", mult.amount().doubleValue());
      case AdditiveBoostImpl add -> new BoostConfig("additive", add.amount().doubleValue());
      default ->
          throw new IllegalArgumentException(
              "Cannot serialize boost type: " + boost.getClass().getName());
    };
  }

  /**
   * Serializes a runtime condition into its JSON configuration model.
   *
   * @throws IllegalArgumentException for unsupported condition implementations
   */
  @Contract(pure = true)
  public static @NotNull ConditionConfig serializeCondition(@NotNull Condition condition) {
    if (!(condition instanceof SnapshotCondition snapshot)) {
      throw new IllegalArgumentException(
          "Cannot serialize condition type: " + condition.getClass().getName());
    }
    return serializeDataBag(snapshot.delegate());
  }

  @Contract(pure = true)
  private static @NotNull ConditionConfig serializeDataBag(
      @NotNull dev.mintychochip.databag.condition.Condition condition) {
    return switch (condition) {
      case AlwaysCondition ignored -> always();
      case AllOfCondition all -> {
        if (all.terms().isEmpty()) {
          yield always();
        }
        List<ConditionConfig> children =
            all.terms().stream().map(BoostSourceConfigSerializer::serializeDataBag).toList();
        yield new ConditionConfig(
            "and", null, null, null, children, null, null, null, null, null, null, null);
      }
      case AnyOfCondition any -> {
        List<ConditionConfig> children =
            any.terms().stream().map(BoostSourceConfigSerializer::serializeDataBag).toList();
        yield new ConditionConfig(
            "or", null, null, null, children, null, null, null, null, null, null, null);
      }
      case InvertedCondition inverted ->
          new ConditionConfig(
              "not",
              null,
              null,
              null,
              null,
              serializeDataBag(inverted.term()),
              null,
              null,
              null,
              null,
              null,
              null);
      case SneakingCondition sneak -> simple("sneaking", sneak.expected());
      case SprintingCondition sprint -> simple("sprinting", sprint.expected());
      case BiomeCondition biome -> simple("biome", biome.biomeKey().asString());
      case WorldCondition world -> simple("world", preferredWorldName(world.worldName()));
      case WeatherCondition weather ->
          simple("weather", weather.state().name().toLowerCase(Locale.ROOT));
      case FluidCondition fluid ->
          new ConditionConfig(
              "liquid",
              null,
              fluid.fluidKey().asString(),
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              true);
      case PlayerResourceCondition resource ->
          new ConditionConfig(
              "player_resource",
              operatorName(mapOperator(resource.operator())),
              resource.expected(),
              null,
              null,
              null,
              resource.type().name().toLowerCase(Locale.ROOT),
              null,
              null,
              null,
              null,
              null);
      case PotionPresentCondition potion ->
          new ConditionConfig(
              "potion_effect",
              null,
              null,
              null,
              null,
              null,
              null,
              stripMinecraft(potion.effectKey().asString()),
              null,
              null,
              null,
              null);
      case PotionAmplifierCondition potion ->
          new ConditionConfig(
              "potion_effect",
              operatorName(mapOperator(potion.operator())),
              null,
              null,
              null,
              null,
              null,
              stripMinecraft(potion.effectKey().asString()),
              potion.expected(),
              null,
              null,
              null);
      case JobCondition job -> {
        List<String> keys = new ArrayList<>(job.jobKeys());
        if (keys.size() == 1) {
          yield simple("job", stripJobNamespace(keys.getFirst()));
        }
        List<Object> values =
            keys.stream()
                .map(BoostSourceConfigSerializer::stripJobNamespace)
                .map(s -> (Object) s)
                .toList();
        yield new ConditionConfig(
            "job", null, null, values, null, null, null, null, null, null, null, null);
      }
      default ->
          throw new IllegalArgumentException(
              "Cannot serialize condition type: " + condition.getClass().getName());
    };
  }

  @Contract(pure = true)
  private static @NotNull String preferredWorldName(@NotNull String worldName) {
    if (worldName.startsWith("minecraft:")) {
      return worldName.substring("minecraft:".length());
    }
    return worldName;
  }

  @Contract(pure = true)
  private static @NotNull String stripMinecraft(@NotNull String key) {
    return key.startsWith("minecraft:") ? key.substring("minecraft:".length()) : key;
  }

  @Contract(pure = true)
  private static @NotNull RelationalOperator mapOperator(
      @NotNull dev.mintychochip.databag.condition.RelationalOperator operator) {
    return switch (operator) {
      case LESS_THAN -> RelationalOperator.LESS_THAN;
      case LESS_THAN_OR_EQUAL -> RelationalOperator.LESS_THAN_OR_EQUAL;
      case GREATER_THAN -> RelationalOperator.GREATER_THAN;
      case GREATER_THAN_OR_EQUAL -> RelationalOperator.GREATER_THAN_OR_EQUAL;
      case EQUAL -> RelationalOperator.EQUAL;
      case NOT_EQUAL -> RelationalOperator.NOT_EQUAL;
    };
  }

  @Contract(pure = true)
  private static @NotNull ConditionConfig always() {
    return new ConditionConfig(
        "always", null, null, null, null, null, null, null, null, null, null, null);
  }

  @Contract(pure = true)
  private static @NotNull ConditionConfig simple(@NotNull String type, @Nullable Object value) {
    return new ConditionConfig(
        type, null, value, null, null, null, null, null, null, null, null, null);
  }

  @Contract(pure = true)
  private static @NotNull String operatorName(@NotNull RelationalOperator operator) {
    return switch (operator) {
      case LESS_THAN -> "less_than";
      case LESS_THAN_OR_EQUAL -> "less_than_or_equal";
      case GREATER_THAN -> "greater_than";
      case GREATER_THAN_OR_EQUAL -> "greater_than_or_equal";
      case EQUAL -> "equal";
      case NOT_EQUAL -> "not_equal";
    };
  }

  @Contract(pure = true)
  private static @NotNull String stripJobNamespace(@NotNull String jobKey) {
    if (jobKey.startsWith("modularjobs:")) {
      return jobKey.substring("modularjobs:".length());
    }
    return jobKey;
  }
}
