package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;

/**
 * Checks the player has at least {@code minimum} empty inventory slots.
 * Empty slots are counted from the main storage, hotbar, and offhand
 * according to the inclusion flags.
 *
 * @param minimum the minimum number of empty slots required
 * @param includeOffhand whether to count empty offhand slots
 * @param includeHotbar whether to count empty hotbar slots
 */
public record EmptySlotsCondition(int minimum, boolean includeOffhand, boolean includeHotbar)
    implements Condition {

  @Override
  public boolean test(ConditionContext context) {
    if (!context.present()) {
      return false;
    }
    if (context.emptyMain() == null) {
      return false;
    }
    int total = context.emptyMain();
    if (includeHotbar) {
      if (context.emptyHotbar() == null) {
        return false;
      }
      total += context.emptyHotbar();
    }
    if (includeOffhand) {
      if (context.emptyOffhand() == null) {
        return false;
      }
      total += context.emptyOffhand();
    }
    return total >= minimum;
  }
}
