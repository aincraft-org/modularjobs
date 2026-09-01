package dev.mintychochip.paper;

import dev.mintychochip.container.Context;
import java.util.Locale;
import java.util.Objects;
import net.kyori.adventure.key.Key;
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Maps Bukkit types to pure {@link Context} variants for the payment path. */
public final class BukkitContexts {

  private BukkitContexts() {}

  /** Block. */
  @Contract(pure = true)
  public static @NotNull Context.BlockContext block(@NotNull Block block) {
    Objects.requireNonNull(block, "block");
    return new Context.BlockContext(
        adventureKey(block.getWorld().getKey()),
        block.getX(),
        block.getY(),
        block.getZ(),
        adventureKey(block.getType().getKey()));
  }

  /** Item. */
  @Contract(pure = true)
  public static @NotNull Context.ItemContext item(@NotNull ItemStack stack) {
    Objects.requireNonNull(stack, "stack");
    return new Context.ItemContext(adventureKey(stack.getType().getKey()), stack.getAmount());
  }

  /** API member. */
  @Deprecated
  @Contract(value = "null -> fail", pure = true)
  public static @NotNull Context.MaterialContext material(@NotNull Material material) {
    Objects.requireNonNull(material, "material");
    return new Context.MaterialContext(adventureKey(material.getKey()));
  }

  /** Entity. */
  @Contract(pure = true)
  public static @NotNull Context.EntityContext entity(@NotNull Entity entity) {
    Objects.requireNonNull(entity, "entity");
    return new Context.EntityContext(adventureKey(entity.getType().getKey()));
  }

  /** Dye. */
  @Contract(pure = true)
  public static @NotNull Context.DyeContext dye(@NotNull DyeColor color) {
    Objects.requireNonNull(color, "color");
    return new Context.DyeContext(Key.key("minecraft", color.name().toLowerCase(Locale.ENGLISH)));
  }

  /** Enchantment. */
  @Contract(pure = true)
  public static @NotNull Context.EnchantmentContext enchantment(
      @NotNull Enchantment enchantment, int level) {
    Objects.requireNonNull(enchantment, "enchantment");
    return new Context.EnchantmentContext(adventureKey(enchantment.getKey()), level);
  }

  /** Potion. */
  @Contract(pure = true)
  public static @NotNull Context.PotionContext potion(@NotNull PotionType type) {
    Objects.requireNonNull(type, "type");
    return new Context.PotionContext(adventureKey(type.getKey()));
  }

  /** Chunk. */
  @Contract(pure = true)
  public static @NotNull Context.ChunkContext chunk(@NotNull Chunk chunk) {
    Objects.requireNonNull(chunk, "chunk");
    return new Context.ChunkContext(
        adventureKey(chunk.getWorld().getKey()), chunk.getX(), chunk.getZ());
  }

  private static @NotNull Key adventureKey(@NotNull NamespacedKey key) {
    return Key.key(key.getNamespace(), key.getKey());
  }
}
