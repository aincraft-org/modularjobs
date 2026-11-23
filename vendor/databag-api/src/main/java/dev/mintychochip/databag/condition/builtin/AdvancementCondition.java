package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import net.kyori.adventure.key.Key;

/**
 * Checks the player has completed the given advancement.
 *
 * @param advancement advancement id to require; compared via {@link Key#asString()}
 */
public record AdvancementCondition(Key advancement) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (!context.present() || context.advancements() == null) {
      return false;
    }
    return context.advancements().contains(advancement.asString());
  }
}
