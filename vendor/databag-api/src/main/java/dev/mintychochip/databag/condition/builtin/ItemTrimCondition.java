package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import dev.mintychochip.databag.condition.ItemSubject;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

/**
 * Matches the subject item's armor trim against optional material and pattern
 * IDs. Absent subjects, non-armor items, or items without a trim fail closed.
 *
 * @param material expected trim material id, or {@code null} to ignore
 * @param pattern expected trim pattern id, or {@code null} to ignore
 */
public record ItemTrimCondition(@Nullable Key material, @Nullable Key pattern)
    implements Condition {

  /**
   * Requires at least one trim field to match.
   */
  public ItemTrimCondition {
    if (material == null && pattern == null) {
      throw new IllegalArgumentException(
          "item_trim requires material, pattern, or both");
    }
  }

  @Override
  public boolean test(ConditionContext context) {
    ItemSubject subject = context.itemSubject();
    if (subject == null
        || subject.material() == null
        || subject.trim() == null
        || subject.trim().isPresent() == false) {
      return false;
    }
    return subject.trim().matches(material, pattern);
  }
}
