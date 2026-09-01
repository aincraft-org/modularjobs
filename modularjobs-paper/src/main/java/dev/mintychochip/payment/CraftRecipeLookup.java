package dev.mintychochip.payment;

import dev.mintychochip.profession.RecipeDefinition;
import dev.mintychochip.service.RecipeService;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Shared craft-output → registered recipe resolution for gate and payment paths. */
public final class CraftRecipeLookup {

  private CraftRecipeLookup() {}

  /** Resolves the crafted result key from a Bukkit item stack. */
  @Contract(pure = true)
  public static @NotNull Key outputKeyFromItemStack(@NotNull ItemStack stack) {
    return Key.key(stack.getType().getKey().getNamespace(), stack.getType().getKey().getKey());
  }

  /** Looks up a registered recipe for the crafted output item. */
  public static @NotNull Optional<RecipeDefinition> definitionForCraftOutput(
      @NotNull RecipeService recipes, @NotNull Key outputMaterialKey) {
    return recipes.definitionForCraftOutput(outputMaterialKey);
  }
}
