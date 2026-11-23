package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import dev.mintychochip.databag.condition.ItemSubject;
import net.kyori.adventure.key.Key;

import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Matches a horse's color variant and/or equipped armor item IDs. Armor
 * predicates require the armor subject snapshot; configured fields must all
 * match.
 *
 * @param color the horse color key, or {@code null} to ignore
 * @param armorItems accepted horse armor material keys; empty ignores armor
 */
public record HorseCondition(
    @Nullable Key color,
    Set<Key> armorItems) implements Condition {

  /**
   * Copies armor keys defensively and requires a configured predicate.
   */
  public HorseCondition {
    armorItems = Set.copyOf(armorItems == null ? Set.of() : armorItems);
    if (color == null && armorItems.isEmpty()) {
      throw new IllegalArgumentException("horse predicate requires variant or armor");
    }
  }

  @Override
  public boolean test(ConditionContext context) {
    if (context.entityType() == null
        || BiomeCondition.keysEqual(Key.key("minecraft:horse"), context.entityType()) == false) {
      return false;
    }
    if (color != null) {
      Key actual = context.horseColor();
      if (actual == null || BiomeCondition.keysEqual(color, actual) == false) {
        return false;
      }
    }
    if (armorItems.isEmpty() == false) {
      ItemSubject armor = context.horseArmor();
      if (armor == null || armor.material() == null || armor.amount() <= 0) {
        return false;
      }
      boolean matched = false;
      for (Key item : armorItems) {
        if (BiomeCondition.keysEqual(item, armor.material())) {
          matched = true;
          break;
        }
      }
      if (matched == false) {
        return false;
      }
    }
    return true;
  }
}
