package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import net.kyori.adventure.key.Key;

/**
 * Checks the player has at least {@code minimum} of {@code material} in inventory.
 *
 * @param material item material id to count
 * @param minimum inclusive minimum stack count required
 */
public record RequiredItemCountCondition(Key material, int minimum) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (context.itemCounts() == null) {
      return false;
    }
    for (var entry : context.itemCounts().entrySet()) {
      if (BiomeCondition.keysEqual(material, entry.getKey()) && entry.getValue() >= minimum) {
        return true;
      }
    }
    return false;
  }
}
