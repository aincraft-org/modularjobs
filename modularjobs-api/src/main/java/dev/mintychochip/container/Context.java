package dev.mintychochip.container;

import java.util.Objects;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

/**
 * Pure payment/action context (no Bukkit types). Paper maps Bukkit objects via {@code
 * dev.mintychochip.paper.BukkitContexts}.
 */
@NonExtendable
public sealed interface Context
    permits Context.BlockContext,
        Context.ChunkContext,
        Context.DyeContext,
        Context.EnchantmentContext,
        Context.EntityContext,
        Context.ItemContext,
        Context.KeyContext,
        Context.MaterialContext,
        Context.PotionContext {

  /** Block at location. */
  record BlockContext(@NotNull Key worldKey, int x, int y, int z, @NotNull Key materialKey)
      implements Context {}

  /** Item material key (+ amount). */
  record ItemContext(@NotNull Key materialKey, int amount) implements Context {}

  /** Type. */
  @Deprecated
  record MaterialContext(@NotNull Key materialKey) implements Context {}

  /** Entity type. */
  record EntityContext(@NotNull Key entityTypeKey) implements Context {}

  /** Dye color. */
  record DyeContext(@NotNull Key dyeColorKey) implements Context {}

  /** Enchantment context. */
  record EnchantmentContext(@NotNull Key enchantmentKey, int level) implements Context {}

  /** Potion context. */
  record PotionContext(@NotNull Key potionTypeKey) implements Context {}

  /** Chunk context. */
  record ChunkContext(@NotNull Key worldKey, int chunkX, int chunkZ) implements Context {}

  /** Arbitrary context key supplied by an integration. */
  record KeyContext(@NotNull Key key) implements Context {
    public KeyContext {
      Objects.requireNonNull(key, "key");
    }
  }
}
