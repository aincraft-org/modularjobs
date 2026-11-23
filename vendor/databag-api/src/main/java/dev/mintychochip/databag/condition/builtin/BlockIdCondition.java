package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import net.kyori.adventure.key.Key;

/**
 * Matches a block's id (for example {@code minecraft:chest}).
 *
 * @param blockId expected block id; fails closed when the snapshot has no block id
 */
public record BlockIdCondition(Key blockId) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (context.blockId() == null) {
      return false;
    }
    return BiomeCondition.keysEqual(blockId, context.blockId());
  }
}
