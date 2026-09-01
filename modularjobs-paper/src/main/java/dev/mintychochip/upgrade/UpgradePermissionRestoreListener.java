package dev.mintychochip.upgrade;

import dev.mintychochip.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/** Restores upgrade permissions on login and cleans up on logout. */
public final class UpgradePermissionRestoreListener implements Listener {
  private final UpgradeService upgradeService;
  private final UpgradeEffectApplier effectApplier;
  private final UpgradePermissionManager permissionManager;
  private final Registry<SkillTree> skillTreeRegistry;

  /** Upgrade permission restore listener. */
  public UpgradePermissionRestoreListener(
      @NotNull UpgradeService upgradeService,
      @NotNull UpgradeEffectApplier effectApplier,
      @NotNull UpgradePermissionManager permissionManager,
      @NotNull Registry<SkillTree> skillTreeRegistry) {
    this.upgradeService = upgradeService;
    this.effectApplier = effectApplier;
    this.permissionManager = permissionManager;
    this.skillTreeRegistry = skillTreeRegistry;
  }

  /** Restore all upgrade permissions from all job trees when player joins. */
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    Player player = event.getPlayer();
    String playerId = player.getUniqueId().toString();

    // v2 trees: one union restore across ALL active trees. The applier cleans
    // up this plugin's attachment exactly once, then grants the union, so a
    // per-tree restore can never wipe another job's permissions.
    Map<SkillTree, SkillTreeState> byTree = new HashMap<>();
    for (SkillTree tree : skillTreeRegistry) {
      byTree.put(tree, upgradeService.getSkillTreeState(playerId, tree.jobKey()));
    }
    effectApplier.restoreAllForTrees(player, byTree);

    // Legacy trees: restore effects from unlocked nodes on top; the union
    // restore above already cleared the attachment, so nothing stale survives.
    upgradeService
        .getAllTrees()
        .forEach(
            tree -> {
              PlayerUpgradeData data = upgradeService.getPlayerData(playerId, tree.jobKey());
              if (!data.unlockedNodes().isEmpty()) {
                effectApplier.restoreEffects(player, tree, data.unlockedNodes());
              }
            });
  }

  /** Cleanup all permission attachments when player quits. */
  public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
    permissionManager.cleanupPlayer(event.getPlayer().getUniqueId());
  }
}
