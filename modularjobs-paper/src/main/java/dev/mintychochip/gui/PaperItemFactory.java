package dev.mintychochip.gui;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Creates the small, metadata-only item stacks used by native Paper inventory screens. */
public final class PaperItemFactory {

  private PaperItemFactory() {}

  /** Creates a single-item stack with a display name and ordered lore lines. */
  public static ItemStack of(Material material, String displayName, List<String> lore) {
    ItemStack item = new ItemStack(material, 1);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(Component.text(displayName == null ? "" : displayName));
    meta.lore(
        lore == null
            ? List.of()
            : lore.stream().map(line -> Component.text(line == null ? "" : line)).toList());
    item.setItemMeta(meta);
    return item;
  }

  /** Creates a visually blank pane with metadata so it cannot show a default item name. */
  public static ItemStack pane(Material material) {
    return of(material, " ", List.of(""));
  }
}
