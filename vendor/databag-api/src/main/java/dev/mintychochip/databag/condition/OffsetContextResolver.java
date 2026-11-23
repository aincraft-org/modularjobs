package dev.mintychochip.databag.condition;

import org.jetbrains.annotations.Nullable;

/**
 * Resolves a block-relative {@link ConditionContext} for location-offset predicates.
 */
@FunctionalInterface
public interface OffsetContextResolver {

  /**
   * Builds a snapshot for the given offset from the current evaluation anchor.
   *
   * @param offsetX block offset on the X axis
   * @param offsetY block offset on the Y axis
   * @param offsetZ block offset on the Z axis
   * @return offset context, or {@code null} when unavailable
   */
  @Nullable
  ConditionContext resolve(int offsetX, int offsetY, int offsetZ);
}
