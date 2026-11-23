package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import net.kyori.adventure.key.Key;

/**
 * Matches a frog variant (minecraft:cold, minecraft:temperate, minecraft:warm).
 *
 * @param variant the expected frog variant key
 */
public record FrogCondition(Key variant) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (context.entityType() == null
        || !BiomeCondition.keysEqual(Key.key("minecraft:frog"), context.entityType())) {
      return false;
    }
    return context.frogVariant() != null && BiomeCondition.keysEqual(variant, context.frogVariant());
  }
}
