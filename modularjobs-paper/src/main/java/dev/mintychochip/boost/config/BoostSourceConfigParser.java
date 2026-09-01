package dev.mintychochip.boost.config;

import dev.mintychochip.boost.RuledBoostSourceImpl;
import dev.mintychochip.boost.config.BoostSourceConfig.BoostConfig;
import dev.mintychochip.boost.config.BoostSourceConfig.RuleConfig;
import dev.mintychochip.container.Boost;
import dev.mintychochip.container.BoostSource;
import dev.mintychochip.container.boost.Condition;
import dev.mintychochip.container.boost.RuledBoostSource.Rule;
import dev.mintychochip.container.boost.factories.BoostFactory;
import dev.mintychochip.container.boost.factories.ConditionFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parses BoostSourceConfig from JSON into BoostSource instances. Provides reusable parsing methods
 * for use across different config contexts.
 */
public final class BoostSourceConfigParser {

  private final BoostFactory boostFactory;
  private final ConditionConfigParser conditionParser;

  /** Creates a parser with the factories used to build boosts and conditions. */
  public BoostSourceConfigParser(
      @NotNull ConditionFactory conditionFactory, @NotNull BoostFactory boostFactory) {
    this.boostFactory = boostFactory;
    this.conditionParser = new ConditionConfigParser(conditionFactory);
  }

  /** Parse a complete BoostSourceConfig into a BoostSource. */
  @Contract(pure = true)
  public @NotNull BoostSource parse(@NotNull BoostSourceConfig config) {
    Key key = Key.key(config.key());
    List<Rule> rules = new ArrayList<>();

    for (RuleConfig ruleConfig : config.rules()) {
      Rule rule = parseRule(ruleConfig);
      rules.add(rule);
    }

    return new RuledBoostSourceImpl(rules, key, config.description());
  }

  /**
   * Build a BoostSource from individual components. Useful when constructing from non-standard
   * config formats.
   */
  @Contract(pure = true)
  public @NotNull BoostSource buildBoostSource(
      @NotNull Key key, @Nullable String description, @Nullable List<RuleConfig> ruleConfigs) {
    List<Rule> rules = new ArrayList<>();

    if (ruleConfigs != null) {
      for (RuleConfig ruleConfig : ruleConfigs) {
        Rule rule = parseRule(ruleConfig);
        rules.add(rule);
      }
    }

    return new RuledBoostSourceImpl(rules, key, description);
  }

  /** Parse a rule configuration. */
  @Contract(pure = true)
  public @NotNull Rule parseRule(@NotNull RuleConfig config) {
    Condition condition = conditionParser.parse(config.conditions());
    Boost boost = parseBoost(config.boost());
    int priority = config.priority();
    return new Rule(condition, priority, boost);
  }

  /** Parse an UpgradeTreeConfig rule configuration. */
  @Contract(pure = true)
  public @NotNull Rule parseRule(
      @NotNull dev.mintychochip.upgrade.config.UpgradeTreeConfig.RuleConfig config) {
    Condition condition = conditionParser.parse(config.conditions());
    Boost boost = parseBoost(config.boost());
    int priority = config.priority();
    return new Rule(condition, priority, boost);
  }

  /** Parse a boost configuration. */
  @Contract(pure = true)
  public @NotNull Boost parseBoost(@NotNull BoostConfig config) {
    BigDecimal amount = BigDecimal.valueOf(config.amount());
    return switch (config.type().toLowerCase()) {
      case "multiplicative" -> boostFactory.multiplicative(amount);
      case "additive" -> boostFactory.additive(amount);
      default -> throw new IllegalArgumentException("Unknown boost type: " + config.type());
    };
  }

  /** Parse an UpgradeTreeConfig boost configuration. */
  @Contract(pure = true)
  public @NotNull Boost parseBoost(
      @NotNull dev.mintychochip.upgrade.config.UpgradeTreeConfig.BoostConfig config) {
    BigDecimal amount = BigDecimal.valueOf(config.amount());
    return switch (config.type().toLowerCase()) {
      case "multiplicative" -> boostFactory.multiplicative(amount);
      case "additive" -> boostFactory.additive(amount);
      default -> throw new IllegalArgumentException("Unknown boost type: " + config.type());
    };
  }
}
