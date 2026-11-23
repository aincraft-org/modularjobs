package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import dev.mintychochip.databag.condition.EquipmentSlotKey;
import dev.mintychochip.databag.condition.ItemSubject;
import net.kyori.adventure.key.Key;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Matches an equipment slot's item subject against item IDs and an inclusive
 * count range. Absent slots evaluate as empty stacks and fail closed.
 *
 * @param slot the equipment slot to inspect
 * @param items accepted item material keys
 * @param minimum the minimum stack amount, or {@code null} for no lower bound
 * @param maximum the maximum stack amount, or {@code null} for no upper bound
 */
public record EquipmentItemCondition(
    EquipmentSlotKey slot,
    Set<Key> items,
    Integer minimum,
    Integer maximum) implements Condition {

  /**
   * Validates the slot, accepted items, and inclusive count bounds.
   */
  public EquipmentItemCondition {
    Objects.requireNonNull(slot, "slot");
    items = Set.copyOf(items == null ? Set.of() : items);
    if (items.isEmpty()) {
      throw new IllegalArgumentException("items must not be empty");
    }
    if (minimum != null && minimum < 0) {
      throw new IllegalArgumentException("minimum must be >= 0");
    }
    if (maximum != null && maximum < 0) {
      throw new IllegalArgumentException("maximum must be >= 0");
    }
    if (minimum != null && maximum != null && minimum > maximum) {
      throw new IllegalArgumentException("minimum exceeds maximum");
    }
  }

  /**
   * Tests the configured slot against an equipment snapshot.
   *
   * @param equipment slot-to-item subjects, or {@code null}
   * @return {@code true} when the slot matches; {@code false} when equipment is
   *     absent or the slot is empty or out of range
   */
  public boolean test(Map<EquipmentSlotKey, ItemSubject> equipment) {
    if (equipment == null) {
      return false;
    }
    ItemSubject subject = equipment.get(slot);
    return testSubject(subject);
  }

  @Override
  public boolean test(ConditionContext context) {
    return test(context.equipment());
  }

  private boolean testSubject(@Nullable ItemSubject subject) {
    if (subject == null || subject.material() == null || subject.amount() <= 0) {
      return false;
    }
    for (Key item : items) {
      if (!BiomeCondition.keysEqual(item, subject.material())) {
        continue;
      }
      int amount = subject.amount();
      if (minimum != null && amount < minimum.intValue()) {
        return false;
      }
      if (maximum != null && amount > maximum.intValue()) {
        return false;
      }
      return true;
    }
    return false;
  }
}
