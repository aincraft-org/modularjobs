package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

/**
 * Matches a wolf's collar color and/or variant. Collar predicates fail on untamed or
 * uncollared wolves; variant predicates require the modern variant snapshot. Fails closed when
 * the entity is not a wolf.
 *
 * @param collarColor the required collar dye color key, or {@code null} to ignore
 * @param variant the required wolf variant key, or {@code null} to ignore
 */
public record WolfVariantCondition(@Nullable Key collarColor, @Nullable Key variant)
    implements Condition {

  /**
   * Requires at least one wolf field to match.
   */
  public WolfVariantCondition {
    if (collarColor == null && variant == null) {
      throw new IllegalArgumentException("wolf predicate requires collar_color, variant, or both");
    }
  }

  @Override
  public boolean test(ConditionContext context) {
    if (context.entityType() == null
        || BiomeCondition.keysEqual(Key.key("minecraft:wolf"), context.entityType()) == false) {
      return false;
    }
    if (collarColor != null) {
      Key actual = context.collarColor();
      if (actual == null || BiomeCondition.keysEqual(collarColor, actual) == false) {
        return false;
      }
    }
    if (variant != null) {
      Key actual = context.wolfVariant();
      if (actual == null || BiomeCondition.keysEqual(variant, actual) == false) {
        return false;
      }
    }
    return true;
  }
}
