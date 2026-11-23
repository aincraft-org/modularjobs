package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import net.kyori.adventure.key.Key;

/**
 * Matches the fluid at the subject's feet (water/lava).
 *
 * @param fluidKey the expected fluid key
 */
public record FluidCondition(Key fluidKey) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (context.fluid() == null) {
      return false;
    }
    return BiomeCondition.keysEqual(fluidKey, context.fluid());
  }
}
