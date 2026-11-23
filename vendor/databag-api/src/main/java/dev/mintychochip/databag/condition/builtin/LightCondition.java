package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import org.jetbrains.annotations.Nullable;

/**
 * Matches combined, sky, and block light levels against optional bounds.
 * Absent snapshot values fail closed for the configured bound groups.
 *
 * @param minLevel the minimum combined light level, or {@code null} for no lower bound
 * @param maxLevel the maximum combined light level, or {@code null} for no upper bound
 * @param minSky the minimum sky light level, or {@code null} for no lower bound
 * @param maxSky the maximum sky light level, or {@code null} for no upper bound
 * @param minBlock the minimum block light level, or {@code null} for no lower bound
 * @param maxBlock the maximum block light level, or {@code null} for no upper bound
 */
public record LightCondition(
    @Nullable Integer minLevel,
    @Nullable Integer maxLevel,
    @Nullable Integer minSky,
    @Nullable Integer maxSky,
    @Nullable Integer minBlock,
    @Nullable Integer maxBlock)
    implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (minLevel == null && maxLevel == null
        && minSky == null && maxSky == null
        && minBlock == null && maxBlock == null) {
      return false;
    }
    if (minLevel != null || maxLevel != null) {
      Integer lightLevel = context.lightLevel();
      if (lightLevel == null || !withinBounds(lightLevel, minLevel, maxLevel)) {
        return false;
      }
    }
    if (minSky != null || maxSky != null) {
      Integer skyLight = context.skyLight();
      if (skyLight == null || !withinBounds(skyLight, minSky, maxSky)) {
        return false;
      }
    }
    if (minBlock != null || maxBlock != null) {
      Integer blockLight = context.blockLight();
      if (blockLight == null || !withinBounds(blockLight, minBlock, maxBlock)) {
        return false;
      }
    }
    return true;
  }

  private static boolean withinBounds(int value, @Nullable Integer min, @Nullable Integer max) {
    if (min != null && value < min) {
      return false;
    }
    if (max != null && value > max) {
      return false;
    }
    return true;
  }
}
