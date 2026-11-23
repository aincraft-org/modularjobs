package dev.mintychochip.databag.condition;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * The item stack a subject-scoped condition evaluates against. Null material
 * represents an absent or empty stack; amount is then zero. Damageable items
 * expose max durability and current damage; non-damageable items use nulls.
 * Enchantments map enchantment key to level.
 *
 * @param material the item material, or {@code null} for an empty stack
 * @param amount the stack amount
 * @param maxDurability the maximum durability, or {@code null} when not damageable
 * @param damage the current damage, or {@code null} when not damageable
 * @param enchantments enchantment keys and their levels
 * @param trim the armor trim, or {@code null} when absent
 */
public record ItemSubject(
    @Nullable Key material,
    int amount,
    @Nullable Integer maxDurability,
    @Nullable Integer damage,
    Map<Key, Integer> enchantments,
    @Nullable TrimSnapshot trim) implements SubjectSnapshot {

  /**
   * Validates durability and amount invariants and copies enchantments defensively.
   */
  public ItemSubject {
    if (amount < 0) {
      throw new IllegalArgumentException("amount must be >= 0");
    }
    if (material == null && amount != 0) {
      throw new IllegalArgumentException("empty subject must have amount 0");
    }
    if (maxDurability == null && damage != null) {
      throw new IllegalArgumentException("damage requires max durability");
    }
    if (maxDurability != null) {
      if (maxDurability < 0) {
        throw new IllegalArgumentException("maxDurability must be >= 0");
      }
      if (damage != null && (damage < 0 || damage > maxDurability)) {
        throw new IllegalArgumentException(
            "damage must be within [0, maxDurability]");
      }
    }
    enchantments = Map.copyOf(enchantments == null ? Map.of() : enchantments);
    trim = trim;
  }

  /**
   * Returns an empty item subject.
   *
   * @return an empty item subject
   */
  public static ItemSubject empty() {
    return new ItemSubject(null, 0, null, null, Map.of(), null);
  }

  /**
   * Creates an item subject with the given material and amount.
   *
   * @param material the item material
   * @param amount the stack amount
   * @return the item subject
   */
  public static ItemSubject of(Key material, int amount) {
    return new ItemSubject(material, amount, null, null, Map.of(), null);
  }

  /**
   * Creates an item subject with durability data.
   *
   * @param material the item material
   * @param amount the stack amount
   * @param maxDurability the maximum durability, or {@code null} when not damageable
   * @param damage the current damage, or {@code null} when not damageable
   * @return the item subject
   */
  public static ItemSubject of(
      Key material,
      int amount,
      @Nullable Integer maxDurability,
      @Nullable Integer damage) {
    return new ItemSubject(material, amount, maxDurability, damage, Map.of(), null);
  }

  /**
   * Creates an item subject with durability, enchantment, and trim data.
   *
   * @param material the item material
   * @param amount the stack amount
   * @param maxDurability the maximum durability, or {@code null} when not damageable
   * @param damage the current damage, or {@code null} when not damageable
   * @param enchantments enchantment keys and their levels
   * @param trim the armor trim, or {@code null} when absent
   * @return the item subject
   */
  public static ItemSubject of(
      Key material,
      int amount,
      @Nullable Integer maxDurability,
      @Nullable Integer damage,
      Map<Key, Integer> enchantments,
      @Nullable TrimSnapshot trim) {
    return new ItemSubject(
        material, amount, maxDurability, damage, enchantments, trim);
  }
}
