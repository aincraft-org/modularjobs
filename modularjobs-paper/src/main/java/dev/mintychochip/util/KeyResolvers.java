package dev.mintychochip.util;

import dev.mintychochip.container.Context.BlockContext;
import dev.mintychochip.container.Context.ChunkContext;
import dev.mintychochip.container.Context.DyeContext;
import dev.mintychochip.container.Context.EnchantmentContext;
import dev.mintychochip.container.Context.EntityContext;
import dev.mintychochip.container.Context.ItemContext;
import dev.mintychochip.container.Context.KeyContext;
import dev.mintychochip.container.Context.MaterialContext;
import dev.mintychochip.container.Context.PotionContext;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for the shared {@link KeyResolver} (replaces Guice UtilModule). Strategies read pure
 * {@link dev.mintychochip.container.Context} key and coordinate fields.
 */
public final class KeyResolvers {

  /** Prevents instantiation of this static factory class. */
  private KeyResolvers() {}

  /**
   * Creates a resolver with strategies for all built-in context types.
   *
   * @return configured resolver
   */
  @Contract(pure = true)
  public static @NotNull KeyResolver create() {
    KeyResolver resolver = new KeyResolver();
    resolver.addStrategy(BlockContext.class, BlockContext::materialKey);
    resolver.addStrategy(MaterialContext.class, MaterialContext::materialKey);
    resolver.addStrategy(DyeContext.class, DyeContext::dyeColorKey);
    resolver.addStrategy(EntityContext.class, EntityContext::entityTypeKey);
    resolver.addStrategy(ItemContext.class, ItemContext::materialKey);
    resolver.addStrategy(KeyContext.class, KeyContext::key);
    resolver.addStrategy(PotionContext.class, PotionContext::potionTypeKey);
    resolver.addStrategy(
        EnchantmentContext.class,
        context -> {
          Key base = context.enchantmentKey();
          return Key.key(base.namespace(), base.value() + "_" + context.level());
        });
    resolver.addStrategy(ChunkContext.class, ChunkContext::worldKey);
    return resolver;
  }
}
