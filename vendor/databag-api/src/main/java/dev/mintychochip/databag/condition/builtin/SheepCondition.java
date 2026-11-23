package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

/**
 * Matches a sheep's sheared state and wool color. Both checks are optional; missing criteria
 * are skipped. Fails closed when the sheep sheared snapshot is absent.
 *
 * @param sheared the required sheared flag, or {@code null} to ignore
 * @param color the required wool color key, or {@code null} to ignore
 */
public record SheepCondition(@Nullable Boolean sheared, @Nullable Key color) implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (sheared != null) {
      if (context.sheared() == null || !sheared.equals(context.sheared())) {
        return false;
      }
    }
    if (color != null) {
      if (context.woolColor() == null || !BiomeCondition.keysEqual(color, context.woolColor())) {
        return false;
      }
    }
    return sheared != null || color != null;
  }
}
