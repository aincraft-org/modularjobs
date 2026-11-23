package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import dev.mintychochip.databag.condition.RelationalOperator;
import java.math.BigDecimal;
import net.kyori.adventure.key.Key;

/**
 * Compares a potion effect remaining duration (ticks) to {@code expected}. Fails closed when
 * the living subject or named effect snapshot is absent.
 *
 * @param effectKey the potion effect key to match
 * @param operator the relational comparison against the remaining duration
 * @param expected the duration in ticks to compare
 */
public record PotionDurationCondition(
    Key effectKey, RelationalOperator operator, int expected) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (!context.livingPresent()) {
      return false;
    }
    for (var entry : context.effects().entrySet()) {
      if (BiomeCondition.keysEqual(effectKey, entry.getKey())) {
        return operator.test(
            BigDecimal.valueOf(entry.getValue().duration()),
            BigDecimal.valueOf(expected));
      }
    }
    return false;
  }
}
