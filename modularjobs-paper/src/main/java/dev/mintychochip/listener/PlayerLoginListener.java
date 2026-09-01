package dev.mintychochip.listener;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.hooks.PermissionHook;
import dev.mintychochip.service.JobPerkCatalog;
import dev.mintychochip.service.JobService;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

/** Restores job perk permissions on login. */
public final class PlayerLoginListener implements Listener {

  private final JobService jobService;
  private final PermissionHook permissions;
  private final JobPerkCatalog perkCatalog;

  /** Player login listener. */
  public PlayerLoginListener(
      @NotNull JobService jobService,
      @NotNull PermissionHook permissions,
      @NotNull JobPerkCatalog perkCatalog) {
    this.jobService = jobService;
    this.permissions = permissions;
    this.perkCatalog = perkCatalog;
  }

  /** Event handler. */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    Player player = event.getPlayer();
    List<PlayerJobState> states = jobService.getPlayerJobStates(player.getUniqueId());

    for (PlayerJobState state : states) {
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
    }
  }
}
