package dev.mintychochip.databag.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import dev.mintychochip.databag.DataBag;
import java.util.Locale;
import net.kyori.adventure.key.Key;

/**
 * Flattens extension-condition JSON fields into a {@link DataBag} (and back).
 * Field names are stored as {@code condition:<lowercase-name>}.
 */
final class ArgumentBags {

  private static final String NS = "condition";

  private ArgumentBags() {}

  static DataBag fromJson(JsonObject json) {
    DataBag bag = DataBag.create();
    for (var entry : json.entrySet()) {
      String name = entry.getKey();
      if ("condition".equals(name) || "type".equals(name)) {
        continue;
      }
      Key key;
      try {
        key = Key.key(NS, name.toLowerCase(Locale.ROOT));
      } catch (RuntimeException e) {
        throw new IllegalArgumentException("invalid condition field name: " + name, e);
      }
      put(bag, key, entry.getValue());
    }
    return bag;
  }

  static void writeFields(JsonObject json, DataBag bag) {
    for (Key key : bag.keys()) {
      String field = key.value();
      bag.getBoolean(key).ifPresent(v -> json.addProperty(field, v));
      bag.getInt(key).ifPresent(v -> json.addProperty(field, v));
      bag.getLong(key).ifPresent(v -> json.addProperty(field, v));
      bag.getFloat(key).ifPresent(v -> json.addProperty(field, v));
      bag.getDouble(key).ifPresent(v -> json.addProperty(field, v));
      bag.getString(key).ifPresent(v -> addStringOrJson(json, field, v));
    }
  }

  private static void put(DataBag bag, Key key, JsonElement element) {
    if (element == null || element.isJsonNull()) {
      return;
    }
    if (element.isJsonPrimitive()) {
      JsonPrimitive primitive = element.getAsJsonPrimitive();
      if (primitive.isBoolean()) {
        bag.setBoolean(key, primitive.getAsBoolean());
        return;
      }
      if (primitive.isNumber()) {
        putNumber(bag, key, primitive);
        return;
      }
      bag.setString(key, primitive.getAsString());
      return;
    }
    bag.setString(key, element.toString());
  }

  private static void addStringOrJson(JsonObject json, String field, String value) {
    if (value.startsWith("{") || value.startsWith("[")) {
      try {
        json.add(field, JsonParser.parseString(value));
        return;
      } catch (JsonSyntaxException ignored) {
        // fall through to a string property
      }
    }
    json.addProperty(field, value);
  }

  private static void putNumber(DataBag bag, Key key, JsonPrimitive primitive) {
    try {
      long asLong = primitive.getAsLong();
      if (asLong >= Integer.MIN_VALUE && asLong <= Integer.MAX_VALUE
          && primitive.getAsDouble() == (double) asLong) {
        bag.setInt(key, (int) asLong);
        return;
      }
      bag.setLong(key, asLong);
    } catch (NumberFormatException ignored) {
      bag.setDouble(key, primitive.getAsDouble());
    }
  }
}
