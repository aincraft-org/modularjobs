package dev.mintychochip.databag.condition;

import net.kyori.adventure.key.Key;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/** Immutable metadata snapshot of an inventory item stack.
 *
 * @param material item material id, or {@code null} when empty
 * @param amount stack size
 * @param customModelData optional custom model data component
 * @param displayName optional display name text
 * @param lore copied lore lines, never {@code null}
 */
public record ItemSnapshot(
    @Nullable Key material,
    int amount,
    @Nullable Integer customModelData,
    @Nullable String displayName,
    List<String> lore) {

  /**
   * Copies lore defensively and rejects negative amounts.
   */
  public ItemSnapshot {
    lore = List.copyOf(lore == null ? List.of() : lore);
    if (amount < 0) {
      throw new IllegalArgumentException("amount must be >= 0");
    }
  }
  /**
   * Returns a zero-size snapshot with no material or metadata.
   *
   * @return an empty item snapshot
   */
  public static ItemSnapshot empty() {
    return new ItemSnapshot(null, 0, null, null, List.of());
  }
}
