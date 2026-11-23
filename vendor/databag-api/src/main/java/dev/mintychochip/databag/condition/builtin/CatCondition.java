package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import net.kyori.adventure.key.Key;

/**
 * Matches a cat variant (e.g. minecraft:siamese).
 *
 * @param variant expected cat variant id; fails closed when the snapshot has no variant
 */
public record CatCondition(Key variant) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (context.entityType() == null
        || !BiomeCondition.keysEqual(Key.key("minecraft:cat"), context.entityType())) {
      return false;
    }
    return context.catVariant() != null && BiomeCondition.keysEqual(variant, context.catVariant());
  }
}
