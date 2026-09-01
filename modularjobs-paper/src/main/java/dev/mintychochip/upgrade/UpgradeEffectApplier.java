package dev.mintychochip.upgrade;

import dev.mintychochip.service.RecipeService;
import dev.mintychochip.upgrade.NodeEffect.PermissionEffect;
import dev.mintychochip.upgrade.NodeEffect.RecipeUnlockEffect;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Applies and unapplies upgrade effects when nodes are unlocked or reset. Permissions and recipe
 * unlocks are mutable grants; boosts flow through {@link UpgradeBoostDataService}.
 */
public final class UpgradeEffectApplier implements NodeEffectApplier {
  private final UpgradePermissionManager permissionManager;
  private final @Nullable RecipeService recipeService;

  /** Upgrade effect applier. */
  public UpgradeEffectApplier(@NotNull UpgradePermissionManager permissionManager) {
    this(permissionManager, null);
  }

  /** Upgrade effect applier. */
  public UpgradeEffectApplier(
      @NotNull UpgradePermissionManager permissionManager, @Nullable RecipeService recipeService) {
    this.permissionManager = permissionManager;
    this.recipeService = recipeService;
  }

  @Override
  public @NotNull Set<NodeEffect> derive(@NotNull SkillTreeState state, @NotNull SkillTree tree) {
    Set<NodeEffect> result = new HashSet<>();
    for (SkillNode node : tree.nodes()) {
      int level = state.levelOf(node.key().value());
      result.addAll(node.activeEffects(level));
    }
    return Set.copyOf(result);
  }

  @Override
  public void syncEffects(
      @NotNull Player player,
      @NotNull SkillTreeState previous,
      @NotNull SkillTreeState current,
      @NotNull SkillTree tree) {
    syncEffects(player, Map.of(tree, previous), Map.of(tree, current));
  }

  @Override
  public void syncEffects(
      @NotNull Player player,
      @NotNull Map<SkillTree, SkillTreeState> previousByTree,
      @NotNull Map<SkillTree, SkillTreeState> currentByTree) {
    Set<String> oldPermissions = derivePermissions(previousByTree);
    Set<String> newPermissions = derivePermissions(currentByTree);

    Set<String> toRevoke = new HashSet<>(oldPermissions);
    toRevoke.removeAll(newPermissions);
    for (String permission : toRevoke) {
      permissionManager.revokePermission(player, permission);
    }

    Set<String> toGrant = new HashSet<>(newPermissions);
    toGrant.removeAll(oldPermissions);
    for (String permission : toGrant) {
      permissionManager.grantPermission(player, permission);
    }

    Set<Key> oldRecipes = deriveRecipes(previousByTree);
    Set<Key> newRecipes = deriveRecipes(currentByTree);
    if (recipeService != null) {
      Set<Key> revokeRecipes = new HashSet<>(oldRecipes);
      revokeRecipes.removeAll(newRecipes);
      for (Key recipe : revokeRecipes) {
        recipeService.revoke(player.getUniqueId(), recipe);
      }
      Set<Key> grantRecipes = new HashSet<>(newRecipes);
      grantRecipes.removeAll(oldRecipes);
      for (Key recipe : grantRecipes) {
        recipeService.grant(player.getUniqueId(), recipe);
      }
    }
  }

  private @NotNull Set<String> derivePermissions(@NotNull Map<SkillTree, SkillTreeState> byTree) {
    Set<String> permissions = new HashSet<>();
    for (Map.Entry<SkillTree, SkillTreeState> entry : byTree.entrySet()) {
      for (SkillNode node : entry.getKey().nodes()) {
        int level = entry.getValue().levelOf(node.key().value());
        for (NodeEffect effect : node.activeEffects(level)) {
          if (effect instanceof PermissionEffect perm) {
            permissions.addAll(perm.permissions());
          }
        }
      }
    }
    return permissions;
  }

  private @NotNull Set<Key> deriveRecipes(@NotNull Map<SkillTree, SkillTreeState> byTree) {
    Set<Key> recipes = new HashSet<>();
    for (Map.Entry<SkillTree, SkillTreeState> entry : byTree.entrySet()) {
      for (SkillNode node : entry.getKey().nodes()) {
        int level = entry.getValue().levelOf(node.key().value());
        for (NodeEffect effect : node.activeEffects(level)) {
          if (effect instanceof RecipeUnlockEffect recipe) {
            recipes.add(recipe.recipeKey());
          }
        }
      }
    }
    return recipes;
  }

  @Override
  public void restoreAllForTrees(
      @NotNull Player player, @NotNull Map<SkillTree, SkillTreeState> byTree) {
    Set<NodeEffect> union = new HashSet<>();
    for (Map.Entry<SkillTree, SkillTreeState> entry : byTree.entrySet()) {
      union.addAll(derive(entry.getValue(), entry.getKey()));
    }
    permissionManager.cleanupPlayer(player.getUniqueId());
    for (NodeEffect effect : union) {
      applyEffect(player, effect);
    }
  }

  @Override
  public void unapplyAll(
      @NotNull Player player, @NotNull SkillTreeState state, @NotNull SkillTree tree) {
    for (NodeEffect effect : derive(state, tree)) {
      revokeEffect(player, effect);
    }
  }

  private void applyEffect(@NotNull Player player, @NotNull NodeEffect effect) {
    if (effect instanceof PermissionEffect perm) {
      for (String permission : perm.permissions()) {
        permissionManager.grantPermission(player, permission);
      }
      return;
    }
    if (effect instanceof RecipeUnlockEffect recipe && recipeService != null) {
      recipeService.grant(player.getUniqueId(), recipe.recipeKey());
    }
  }

  private void revokeEffect(@NotNull Player player, @NotNull NodeEffect effect) {
    if (effect instanceof PermissionEffect perm) {
      for (String permission : perm.permissions()) {
        permissionManager.revokePermission(player, permission);
      }
      return;
    }
    if (effect instanceof RecipeUnlockEffect recipe && recipeService != null) {
      recipeService.revoke(player.getUniqueId(), recipe.recipeKey());
    }
  }

  /** Apply node effects. */
  public void applyNodeEffects(@NotNull Player player, @NotNull UpgradeNode node) {
    for (UpgradeEffect effect : node.effects()) {
      if (effect instanceof UpgradeEffect.PermissionEffect perm) {
        for (String permission : perm.permissions()) {
          permissionManager.grantPermission(player, permission);
        }
      }
    }
  }

  /** Unapply node effects. */
  public void unapplyNodeEffects(@NotNull Player player, @NotNull UpgradeNode node) {
    for (UpgradeEffect effect : node.effects()) {
      if (effect instanceof UpgradeEffect.PermissionEffect perm) {
        for (String permission : perm.permissions()) {
          permissionManager.revokePermission(player, permission);
        }
      }
    }
  }

  /** Restore effects. */
  public void restoreEffects(
      @NotNull Player player, @NotNull UpgradeTree tree, @NotNull Set<String> unlockedNodeKeys) {
    Map<String, UpgradeNode> activeNodes = new HashMap<>();

    for (String nodeKey : unlockedNodeKeys) {
      var nodeOpt = tree.getNode(nodeKey);
      if (nodeOpt.isEmpty()) {
        continue;
      }
      UpgradeNode node = nodeOpt.get();

      PerkPolicy policy = tree.getPerkPolicy(node.perkId());

      if (policy == PerkPolicy.MAX) {
        UpgradeNode existing = activeNodes.get(node.perkId());
        if (existing == null || node.level() > existing.level()) {
          activeNodes.put(node.perkId(), node);
        }
      } else {
        activeNodes.put(node.key().asString(), node);
      }
    }

    for (UpgradeNode node : activeNodes.values()) {
      applyNodeEffects(player, node);
    }
  }
}
