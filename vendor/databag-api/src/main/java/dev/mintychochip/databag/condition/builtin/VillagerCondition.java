package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

/**
 * Matches a villager's biome type, profession, and/or trading level. Level
 * supports inclusive min/max bounds; configured fields must all match.
 *
 * @param type the villager biome type key, or {@code null} to ignore
 * @param profession the villager profession key, or {@code null} to ignore
 * @param minimumLevel the minimum trading level, or {@code null} for no lower bound
 * @param maximumLevel the maximum trading level, or {@code null} for no upper bound
 */
public record VillagerCondition(
    @Nullable Key type,
    @Nullable Key profession,
    @Nullable Integer minimumLevel,
    @Nullable Integer maximumLevel) implements Condition {

  /**
   * Requires a configured field and validates level bounds.
   */
  public VillagerCondition {
    if (type == null && profession == null && minimumLevel == null && maximumLevel == null) {
      throw new IllegalArgumentException(
          "villager predicate requires type, profession, or level bounds");
    }
    if (minimumLevel != null && maximumLevel != null && minimumLevel > maximumLevel) {
      throw new IllegalArgumentException("minimum level exceeds maximum");
    }
  }

  @Override
  public boolean test(ConditionContext context) {
    if (context.entityType() == null
        || BiomeCondition.keysEqual(Key.key("minecraft:villager"), context.entityType()) == false) {
      return false;
    }
    if (type != null) {
      Key actual = context.villagerType();
      if (actual == null || BiomeCondition.keysEqual(type, actual) == false) {
        return false;
      }
    }
    if (profession != null) {
      Key actual = context.villagerProfession();
      if (actual == null || BiomeCondition.keysEqual(profession, actual) == false) {
        return false;
      }
    }
    if (minimumLevel != null || maximumLevel != null) {
      Integer actual = context.villagerLevel();
      if (actual == null) {
        return false;
      }
      int level = actual.intValue();
      if (minimumLevel != null && level < minimumLevel.intValue()) {
        return false;
      }
      if (maximumLevel != null && level > maximumLevel.intValue()) {
        return false;
      }
    }
    return true;
  }
}
