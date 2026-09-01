package dev.mintychochip.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class ContextKeyContractTest {

  @SuppressWarnings("deprecation")
  @Test
  void contextsPreserveTypedIdentifiers() {
    final Key world = Key.key("modularjobs", "jobs_world");
    final Key material = Key.key("minecraft", "stone");
    final Key entityType = Key.key("minecraft", "zombie");
    final Key dyeColor = Key.key("minecraft", "red");
    final Key enchantment = Key.key("minecraft", "sharpness");
    final Key potionType = Key.key("minecraft", "strength");

    assertEquals(world, new Context.BlockContext(world, 1, 2, 3, material).worldKey());
    assertEquals(material, new Context.ItemContext(material, 4).materialKey());
    assertEquals(material, new Context.MaterialContext(material).materialKey());
    assertEquals(entityType, new Context.EntityContext(entityType).entityTypeKey());
    assertEquals(dyeColor, new Context.DyeContext(dyeColor).dyeColorKey());
    assertEquals(enchantment, new Context.EnchantmentContext(enchantment, 5).enchantmentKey());
    assertEquals(potionType, new Context.PotionContext(potionType).potionTypeKey());
    assertEquals(world, new Context.ChunkContext(world, 6, 7).worldKey());
  }

  @Test
  void keyedContextPreservesArbitraryKey() {
    final Key quest = Key.key("myplugin", "quest_complete");

    assertEquals(quest, new Context.KeyContext(quest).key());
  }

  @Test
  void keyedContextRejectsNullKey() {
    assertThrows(NullPointerException.class, () -> new Context.KeyContext(null));
  }
}
