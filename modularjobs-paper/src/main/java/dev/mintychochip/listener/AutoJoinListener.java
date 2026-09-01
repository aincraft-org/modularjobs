package dev.mintychochip.listener;

import dev.mintychochip.Job;
import dev.mintychochip.config.ProgressionLimitsConfig;
import dev.mintychochip.service.JobService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

/** Joins configured auto-join jobs on login. */
public final class AutoJoinListener implements Listener {

  private final JobService jobService;
  private final ProgressionLimitsConfig limits;

  /** Auto join listener. */
  public AutoJoinListener(@NotNull JobService jobService, @NotNull ProgressionLimitsConfig limits) {
    this.jobService = jobService;
    this.limits = limits;
  }

  /** Event handler. */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    Player player = event.getPlayer();
    for (String jobName : limits.autoJoinJobs()) {
      Job job;
      try {
        job = jobService.getJob("modularjobs:" + jobName);
      } catch (IllegalArgumentException e) {
        continue; // configured but not present — skip
      }
      if (job == null
          || jobService.getPlayerJobState(player.getUniqueId().toString(), job.key().toString())
              != null) {
        continue;
      }
      jobService.joinJob(player.getUniqueId().toString(), job.key().toString());
    }
  }
}
