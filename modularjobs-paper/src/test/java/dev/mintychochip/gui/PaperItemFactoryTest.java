package dev.mintychochip.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class PaperItemFactoryTest {
  @BeforeAll
  static void setUp() {
    if (!MockBukkit.isMocked()) {
      MockBukkit.mock();
    }
  }

  @Test
  void ofCreatesMaterialAmountAndComponentMetadataInOrder() {
    ItemStack item =
        PaperItemFactory.of(Material.BOOK, "Job title", List.of("line one", "line two"));

    assertEquals(Material.BOOK, item.getType());
    assertEquals(1, item.getAmount());
    ItemMeta meta = item.getItemMeta();
    assertNotNull(meta);
    assertEquals(Component.text("Job title"), meta.displayName());
    assertEquals(List.of(Component.text("line one"), Component.text("line two")), meta.lore());
  }

  @Test
  void paneHasBlankDisplayNameAndNonNullLoreMetadata() {
    ItemStack item = PaperItemFactory.pane(Material.GRAY_STAINED_GLASS_PANE);

    assertEquals(Material.GRAY_STAINED_GLASS_PANE, item.getType());
    assertEquals(1, item.getAmount());
    ItemMeta meta = item.getItemMeta();
    assertNotNull(meta);
    assertEquals(Component.text(" "), meta.displayName());
    assertNotNull(meta.lore());
  }
}
