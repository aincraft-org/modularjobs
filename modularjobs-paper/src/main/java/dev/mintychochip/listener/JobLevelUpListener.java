package dev.mintychochip.listener;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.hooks.PermissionHook;
import dev.mintychochip.paper.event.BukkitJobJoinEvent;
import dev.mintychochip.paper.event.BukkitJobLeaveEvent;
import dev.mintychochip.paper.event.BukkitJobLevelEvent;
import dev.mintychochip.service.JobPerkCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/** Grants/revokes job perk permissions on level-up, join, and leave. */
public final class JobLevelUpListener implements Listener {

  private final PermissionHook permissions;
  private final JobPerkCatalog perkCatalog;

  /** Job level up listener. */
  public JobLevelUpListener(
      @NotNull PermissionHook permissions, @NotNull JobPerkCatalog perkCatalog) {
    this.permissions = permissions;
    this.perkCatalog = perkCatalog;
  }

  /** Event handler. */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onLevelUp(@NotNull BukkitJobLevelEvent event) {
    Player player = event.getPlayer();
    PlayerJobState state = event.getPlayerJobState();
    Job job = state.job();
    int oldLevel = event.getOldLevel();
    int newLevel = event.getNewLevel();

    Map<Integer, List<String>> unlocks = perkCatalog.unlocks(job, state.currentNode().nodeKey());
    for (int level = oldLevel + 1; level <= newLevel; level++) {
      List<String> perks = unlocks.get(level);
      if (perks == null) {
        continue;
      }
      for (String perk : perks) {
        if (perk.startsWith("storage.")) {
          for (String storagePerm : getAllStoragePermissions(job)) {
            if (!storagePerm.equals(perk)) {
              permissions.revokePerkPermission(player, storagePerm);
            }
          }
        }
        permissions.grantPerkPermission(player, perk);
        notifyPerkUnlock(player, perk);
      }
    }
  }

  /** Event handler. */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onJobJoin(@NotNull BukkitJobJoinEvent event) {
    Player player = event.getPlayer();
    PlayerJobState state = event.getPlayerJobState();
    Job job = state.job();
    int level = state.level();

    Map<Integer, List<String>> unlocks = perkCatalog.unlocks(job, state.currentNode().nodeKey());
    for (Map.Entry<Integer, List<String>> entry : unlocks.entrySet()) {
      if (entry.getKey() <= level) {
        for (String perk : entry.getValue()) {
          if (!perk.startsWith("storage.")) {
            permissions.grantPerkPermission(player, perk);
          }
        }
      }
    }
    String highestStoragePerk =
        perkCatalog.highestStorageUnlock(job, state.currentNode().nodeKey(), level).orElse(null);
    if (highestStoragePerk != null) {
      permissions.grantPerkPermission(player, highestStoragePerk);
    }

    if (event.isRejoin()) {
      player.sendMessage(
          Component.text()
              .append(Component.text("[", NamedTextColor.GRAY))
              .append(Component.text("Jobs", NamedTextColor.GOLD))
              .append(Component.text("] ", NamedTextColor.GRAY))
              .append(
                  Component.text(
                      "Rejoined at level " + level + ". Perks restored.", NamedTextColor.GREEN))
              .build());
    }
  }

  /** Event handler. */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onJobLeave(@NotNull BukkitJobLeaveEvent event) {
    Player player = event.getPlayer();
    PlayerJobState state = event.getPlayerJobState();
    Job job = state.job();

    for (List<String> perks : perkCatalog.allUnlocks(job).values()) {
      for (String perk : perks) {
        permissions.revokePerkPermission(player, perk);
      }
    }

    player.sendMessage(
        Component.text()
            .append(Component.text("[", NamedTextColor.GRAY))
            .append(Component.text("Jobs", NamedTextColor.GOLD))
            .append(Component.text("] ", NamedTextColor.GRAY))
            .append(Component.text("You left ", NamedTextColor.RED))
            .append(job.displayName())
            .append(Component.text(". Job perks revoked.", NamedTextColor.RED))
            .build());
  }

  private @NotNull List<String> getAllStoragePermissions(@NotNull Job job) {
    List<String> storage = new ArrayList<>();
    for (List<String> perks : perkCatalog.allUnlocks(job).values()) {
      for (String perk : perks) {
        if (perk.startsWith("storage.")) {
          storage.add(perk);
        }
      }
    }
    return storage;
  }

  private void notifyPerkUnlock(@NotNull Player player, @NotNull String perkName) {
    player.sendMessage(
        Component.text()
            .append(Component.text("[", NamedTextColor.GRAY))
            .append(Component.text("Jobs", NamedTextColor.GOLD))
            .append(Component.text("] ", NamedTextColor.GRAY))
            .append(Component.text("You unlocked the ", NamedTextColor.GREEN))
            .append(Component.text(perkName, NamedTextColor.YELLOW))
            .append(Component.text(" perk!", NamedTextColor.GREEN))
            .build());
  }
}
