package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import net.kyori.adventure.key.Key;

/**
 * True when the snapshot has the named potion effect. Fails closed when the living subject is
 * absent.
 *
 * @param effectKey the potion effect key that must be present
 */
public record PotionPresentCondition(Key effectKey) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (!context.livingPresent()) {
      return false;
    }
    for (Key key : context.effects().keySet()) {
      if (BiomeCondition.keysEqual(effectKey, key)) {
        return true;
      }
    }
    return false;
  }
}
