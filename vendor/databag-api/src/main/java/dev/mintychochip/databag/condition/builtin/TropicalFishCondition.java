package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

/**
 * Matches a tropical fish's packed variant, named pattern, body color, and/or
 * pattern color. Configured fields must all match; absent snapshot data fails.
 *
 * @param variant the packed variant id, or {@code null} to ignore
 * @param pattern the pattern key, or {@code null} to ignore
 * @param baseColor the body color key, or {@code null} to ignore
 * @param patternColor the pattern color key, or {@code null} to ignore
 */
public record TropicalFishCondition(
    @Nullable Integer variant,
    @Nullable Key pattern,
    @Nullable Key baseColor,
    @Nullable Key patternColor) implements Condition {

  /**
   * Requires at least one tropical-fish field to match.
   */
  public TropicalFishCondition {
    if (variant == null && pattern == null && baseColor == null && patternColor == null) {
      throw new IllegalArgumentException(
          "tropical_fish predicate requires variant, pattern, or colors");
    }
  }

  @Override
  public boolean test(ConditionContext context) {
    if (context.entityType() == null
        || BiomeCondition.keysEqual(
            Key.key("minecraft:tropical_fish"), context.entityType()) == false) {
      return false;
    }
    if (variant != null) {
      Integer actual = context.tropicalFishVariant();
      if (actual == null || actual.intValue() != variant.intValue()) {
        return false;
      }
    }
    if (pattern != null) {
      Key actual = context.tropicalFishPattern();
      if (actual == null || BiomeCondition.keysEqual(pattern, actual) == false) {
        return false;
      }
    }
    if (baseColor != null) {
      Key actual = context.tropicalFishBaseColor();
      if (actual == null || BiomeCondition.keysEqual(baseColor, actual) == false) {
        return false;
      }
    }
    if (patternColor != null) {
      Key actual = context.tropicalFishPatternColor();
      if (actual == null || BiomeCondition.keysEqual(patternColor, actual) == false) {
        return false;
      }
    }
    return true;
  }
}
