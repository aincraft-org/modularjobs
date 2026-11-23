package dev.mintychochip.databag.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.Conditions;
import dev.mintychochip.databag.condition.PlayerResourceType;
import dev.mintychochip.databag.condition.RelationalOperator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared JSON helpers for vanilla loot-condition read/write. */
final class ConditionJsonSupport {

  private ConditionJsonSupport() {}

  static String conditionId(JsonObject json) {
    if (json.has("condition") && json.get("condition").isJsonPrimitive()) {
      return json.get("condition").getAsString();
    }
    if (json.has("type") && json.get("type").isJsonPrimitive()) {
      return json.get("type").getAsString();
    }
    throw new IllegalArgumentException("missing condition id");
  }

  static String stripNamespace(String id) {
    int colon = id.indexOf(':');
    return colon >= 0 ? id.substring(colon + 1) : id;
  }

  static String namespaceOf(String id) {
    int colon = id.indexOf(':');
    return colon > 0 ? id.substring(0, colon).toLowerCase(Locale.ROOT) : "";
  }

  static String requiredString(JsonObject json, String field) {
    if (!json.has(field) || json.get(field).isJsonNull()) {
      throw new IllegalArgumentException("missing " + field);
    }
    return json.get(field).getAsString();
  }

  static String firstString(JsonElement element) {
    if (element.isJsonArray() && !element.getAsJsonArray().isEmpty()) {
      return element.getAsJsonArray().get(0).getAsString();
    }
    return element.getAsString();
  }

  static List<String> stringList(JsonElement element) {
    if (element.isJsonArray()) {
      List<String> values = new ArrayList<>();
      for (JsonElement entry : element.getAsJsonArray()) {
        values.add(entry.getAsString());
      }
      return values;
    }
    return List.of(element.getAsString());
  }

  static Condition anyOfConditions(List<Condition> terms) {
    if (terms.isEmpty()) {
      return null;
    }
    if (terms.size() == 1) {
      return terms.getFirst();
    }
    return Conditions.anyOf(terms.toArray(Condition[]::new));
  }

  static String liquidKey(JsonObject json) {
    if (json.has("value") && json.get("value").isJsonPrimitive()) {
      return json.get("value").getAsString();
    }
    throw new IllegalArgumentException("liquid condition requires 'value'");
  }

  static PlayerResourceType parseResource(String raw) {
    return switch (raw.toUpperCase(Locale.ROOT)) {
      case "HEALTH", "HP" -> PlayerResourceType.HEALTH;
      case "HUNGER", "FOOD", "FOOD_LEVEL" -> PlayerResourceType.HUNGER;
      case "EXPERIENCE", "XP", "EXP" -> PlayerResourceType.EXPERIENCE;
      case "LEVEL", "XP_LEVEL" -> PlayerResourceType.LEVEL;
      case "ABSORPTION" -> PlayerResourceType.ABSORPTION;
      case "AIR", "OXYGEN", "AIR_REMAINING" -> PlayerResourceType.AIR;
      default -> throw new IllegalArgumentException("Unknown player resource type: " + raw);
    };
  }

  static void throwIfUnsupportedVanilla(String id, String normalized) {
    String reason = switch (normalized) {
      case "match_tool" -> "requires tool item from loot execution context";
      case "survives_explosion" -> "requires explosion radius from loot execution context";
      case "random_chance_with_enchanted_bonus" ->
          "requires attacker enchantment level from loot context";
      case "table_bonus" -> "requires tool enchantment level from loot context";
      case "enchantment_active_check" -> "requires enchanted_location loot context";
      case "damage_source_properties" ->
          "requires damage source capture in snapshot; attacker slots exist but damage type/tags do not";
      case "value_check" -> "requires vanilla number providers";
      case "reference" -> "requires predicate file resolver";
      case "environment_attribute_check" -> "requires environment attribute values in snapshot";
      default -> null;
    };
    if (reason != null) {
      throw new IllegalArgumentException("Unsupported vanilla condition " + id + ": " + reason);
    }
  }

  static RelationalOperator parseOperator(String raw) {
    return switch (raw.toLowerCase(Locale.ROOT)) {
      case "less_than", "<" -> RelationalOperator.LESS_THAN;
      case "less_than_or_equal", "<=" -> RelationalOperator.LESS_THAN_OR_EQUAL;
      case "greater_than", ">" -> RelationalOperator.GREATER_THAN;
      case "greater_than_or_equal", ">=" -> RelationalOperator.GREATER_THAN_OR_EQUAL;
      case "equal", "==" -> RelationalOperator.EQUAL;
      case "not_equal", "!=" -> RelationalOperator.NOT_EQUAL;
      default -> throw new IllegalArgumentException("Unknown operator: " + raw);
    };
  }

  static String operatorName(RelationalOperator operator) {
    return switch (operator) {
      case LESS_THAN -> "<";
      case LESS_THAN_OR_EQUAL -> "<=";
      case GREATER_THAN -> ">";
      case GREATER_THAN_OR_EQUAL -> ">=";
      case EQUAL -> "==";
      case NOT_EQUAL -> "!=";
    };
  }
}
