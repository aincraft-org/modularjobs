package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import net.kyori.adventure.key.Key;

/**
 * Matches the snapshot biome against {@code biomeKey}.
 *
 * @param biomeKey expected biome id; compared with case-insensitive key equality
 */
public record BiomeCondition(Key biomeKey) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (context.biome() == null) {
      return false;
    }
    return keysEqual(biomeKey, context.biome());
  }

  static boolean keysEqual(Key expected, Key actual) {
    return expected.equals(actual)
        || expected.asString().equalsIgnoreCase(actual.asString());
  }
}
