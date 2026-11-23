package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;

/**
 * Matches whether the snapshot reports an unobstructed view of the sky.
 *
 * @param expected required {@code canSeeSky} flag; fails closed when the snapshot omits it
 */
public record CanSeeSkyCondition(boolean expected) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    Boolean canSeeSky = context.canSeeSky();
    return canSeeSky != null && canSeeSky == expected;
  }
}
