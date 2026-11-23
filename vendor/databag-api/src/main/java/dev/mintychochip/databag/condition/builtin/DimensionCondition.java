package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import net.kyori.adventure.key.Key;

/**
 * Matches the snapshot dimension key.
 *
 * @param dimensionKey the expected dimension key
 */
public record DimensionCondition(Key dimensionKey) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    return context.worldKey() != null
        && BiomeCondition.keysEqual(dimensionKey, context.worldKey());
  }
}
