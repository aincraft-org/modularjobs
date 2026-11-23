package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import dev.mintychochip.databag.condition.ItemSnapshot;
import net.kyori.adventure.key.Key;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Matches the offhand item's material, minimum amount, and custom model data.
 * Absent offhand snapshots fail closed.
 *
 * @param materials accepted material keys; empty accepts any material
 * @param minimumAmount the minimum stack amount
 * @param customModelData the required custom model data, or {@code null} to ignore
 */
public record OffhandItemCondition(
    Set<Key> materials,
    int minimumAmount,
    @Nullable Integer customModelData) implements Condition {

  /**
   * Copies material keys defensively and rejects negative minimum amounts.
   */
  public OffhandItemCondition {
    materials = Set.copyOf(materials == null ? Set.of() : materials);
    if (minimumAmount < 0) {
      throw new IllegalArgumentException("minimumAmount must be >= 0");
    }
  }

  @Override
  public boolean test(ConditionContext context) {
    ItemSnapshot offhand = context.offhandItem();
    if (offhand == null) {
      return false;
    }
    if (offhand.material() == null || offhand.amount() <= 0) {
      return false;
    }
    if (minimumAmount > 0 && offhand.amount() < minimumAmount) {
      return false;
    }
    if (customModelData != null
        && (offhand.customModelData() == null
            || offhand.customModelData().intValue() != customModelData.intValue())) {
      return false;
    }
    if (materials.isEmpty()) {
      return true;
    }
    for (Key material : materials) {
      if (BiomeCondition.keysEqual(material, offhand.material())) {
        return true;
      }
    }
    return false;
  }
}
