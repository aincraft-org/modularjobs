package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import java.util.Set;
import net.kyori.adventure.key.Key;

/**
 * Checks the entity is wearing every item in {@code items}.
 * The armor set is the set of material keys of currently equipped armor.
 *
 * @param items material keys that must all appear in the snapshot armor set
 */
public record ArmorSetCondition(Set<Key> items) implements Condition {

  /**
   * Copies required armor keys and rejects an empty set.
   */
  public ArmorSetCondition {
    items = Set.copyOf(java.util.Objects.requireNonNull(items, "items"));
    if (items.isEmpty()) {
      throw new IllegalArgumentException("armor set must not be empty");
    }
  }

  @Override
  public boolean test(ConditionContext context) {
    if (context.armorSet() == null) {
      return false;
    }
    for (Key want : items) {
      boolean found = false;
      for (Key have : context.armorSet()) {
        if (BiomeCondition.keysEqual(want, have)) {
          found = true;
          break;
        }
      }
      if (!found) {
        return false;
      }
    }
    return true;
  }
}
