package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import java.util.Locale;
import java.util.Objects;

/**
 * Matches a Bukkit world name or namespaced key. Vanilla dimensions are
 * {@code minecraft:overworld} etc.; named worlds use the plain name. Fails closed when both
 * world name and world key snapshots are absent.
 *
 * @param worldName the world name or namespaced key to match (case-insensitive)
 */
public record WorldCondition(String worldName) implements Condition {

  /**
   * Requires a non-blank world name.
   */
  public WorldCondition {
    Objects.requireNonNull(worldName, "worldName");
    if (worldName.isBlank()) {
      throw new IllegalArgumentException("worldName must be non-blank");
    }
  }

  @Override
  public boolean test(ConditionContext context) {
    if (context.worldName() == null && context.worldKey() == null) {
      return false;
    }
    String expected = worldName.toLowerCase(Locale.ROOT);
    if (context.worldName() != null
        && (context.worldName().equalsIgnoreCase(worldName)
            || context.worldName().equalsIgnoreCase(stripNamespace(worldName)))) {
      return true;
    }
    if (context.worldKey() != null) {
      String key = context.worldKey().asString().toLowerCase(Locale.ROOT);
      String value = context.worldKey().value().toLowerCase(Locale.ROOT);
      return expected.equals(key) || expected.equals(value)
          || stripNamespace(expected).equals(value);
    }
    return false;
  }

  private static String stripNamespace(String raw) {
    int colon = raw.indexOf(':');
    return colon >= 0 ? raw.substring(colon + 1) : raw;
  }
}
