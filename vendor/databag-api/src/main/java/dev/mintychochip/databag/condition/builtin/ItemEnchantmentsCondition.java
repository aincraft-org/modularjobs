package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import dev.mintychochip.databag.condition.ItemSubject;
import net.kyori.adventure.key.Key;
import java.util.Map;
import java.util.Objects;

/**
 * Requires each configured enchantment on the subject item, with optional
 * inclusive level bounds. Absent subjects fail closed.
 *
 * @param enchantments required enchantment keys and optional level bounds
 */
public record ItemEnchantmentsCondition(Map<Key, LevelBound> enchantments)
    implements Condition {

  /**
   * Inclusive minimum and maximum enchantment levels.
   *
   * @param minimum the minimum level, or {@code null} for no lower bound
   * @param maximum the maximum level, or {@code null} for no upper bound
   */
  public record LevelBound(Integer minimum, Integer maximum) {
    /**
     * Validates the inclusive enchantment-level bounds.
     */
    public LevelBound {
      if (minimum != null && minimum < 0) {
        throw new IllegalArgumentException("minimum level must be >= 0");
      }
      if (maximum != null && maximum < 0) {
        throw new IllegalArgumentException("maximum level must be >= 0");
      }
      if (minimum != null && maximum != null && minimum > maximum) {
        throw new IllegalArgumentException("minimum exceeds maximum");
      }
    }

    /**
     * Returns whether {@code level} lies within the configured bounds.
     *
     * @param level the enchantment level to test
     * @return {@code true} when {@code level} is within bounds
     */
    public boolean matches(int level) {
      if (minimum != null && level < minimum.intValue()) {
        return false;
      }
      if (maximum != null && level > maximum.intValue()) {
        return false;
      }
      return true;
    }
  }

  /**
   * Copies enchantment requirements defensively and requires at least one.
   */
  public ItemEnchantmentsCondition {
    enchantments = Map.copyOf(Objects.requireNonNull(enchantments, "enchantments"));
    if (enchantments.isEmpty()) {
      throw new IllegalArgumentException("enchantments must not be empty");
    }
  }

  @Override
  public boolean test(ConditionContext context) {
    ItemSubject subject = context.itemSubject();
    if (subject == null || subject.material() == null) {
      return false;
    }
    Map<Key, Integer> actual = subject.enchantments();
    for (Map.Entry<Key, LevelBound> requirement : enchantments.entrySet()) {
      Key key = requirement.getKey();
      LevelBound bound = requirement.getValue();
      boolean found = false;
      for (Map.Entry<Key, Integer> entry : actual.entrySet()) {
        if (!BiomeCondition.keysEqual(key, entry.getKey())) {
          continue;
        }
        found = true;
        if (bound != null && !bound.matches(entry.getValue().intValue())) {
          return false;
        }
        break;
      }
      if (found == false) {
        return false;
      }
    }
    return true;
  }
}
