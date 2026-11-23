package dev.mintychochip.gui;

import dev.mintychochip.Job;
import dev.mintychochip.JobTask;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Payable;
import dev.mintychochip.service.JobService;
import dev.mintychochip.upgrade.UpgradeService;
import dev.mintychochip.upgrade.UpgradeTree;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Builds the renderer-neutral data used by every job-browse surface. */
public final class JobBrowseModel {
  private static final int MAX_EXAMPLE_REWARDS = 3;

  record Reward(@NotNull String action, @NotNull BigDecimal amount, @NotNull String payableType) {}

  record Entry(
      @NotNull Job job,
      @Nullable PlayerJobState state,
      int activePlayers,
      int upgradeNodes,
      @NotNull List<Reward> exampleRewards) {
    Entry {
      exampleRewards = List.copyOf(exampleRewards);
    }

    boolean joined() {
      return state != null;
    }

    @NotNull
    String joinCommand() {
      return "/j join " + job.key().asString();
    }

    @NotNull
    String infoCommand() {
      return "/j info " + job.key().asString();
    }
  }

  private final JobService jobService;
  private final UpgradeService upgradeService;

  /** Creates the shared browse projection from job and upgrade services. */
  public JobBrowseModel(@NotNull JobService jobService, @NotNull UpgradeService upgradeService) {
    this.jobService = jobService;
    this.upgradeService = upgradeService;
  }

  @NotNull
  List<Entry> entries(@NotNull Player player) {
    Map<String, PlayerJobState> playerStates = new HashMap<>();
    for (PlayerJobState state : jobService.getPlayerJobStates(player.getUniqueId())) {
      playerStates.put(state.jobKey().key().asString(), state);
    }

    Map<String, Integer> activeCounts = activePlayerCounts();
    List<Entry> entries = new ArrayList<>();
    for (Job job : jobService.getJobs()) {
      Optional<UpgradeTree> tree = upgradeService.getTree(job.key().value());
      entries.add(
          new Entry(
              job,
              playerStates.get(job.key().asString()),
              activeCounts.getOrDefault(job.key().asString(), 0),
              tree.map(value -> value.allNodes().size()).orElse(0),
              exampleRewards(job)));
    }
    return List.copyOf(entries);
  }

  private @NotNull Map<String, Integer> activePlayerCounts() {
    Map<String, Integer> counts = new HashMap<>();
    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
      UUID playerId = onlinePlayer.getUniqueId();
      for (PlayerJobState state : jobService.getPlayerJobStates(playerId)) {
        counts.merge(state.jobKey().key().asString(), 1, Integer::sum);
      }
    }
    return counts;
  }

  private @NotNull List<Reward> exampleRewards(@NotNull Job job) {
    Map<ActionType, List<JobTask>> allTasks = jobService.getAllTasks(job, job.rootNode().nodeKey());
    List<Reward> rewards = new ArrayList<>(MAX_EXAMPLE_REWARDS);
    for (Map.Entry<ActionType, List<JobTask>> action : allTasks.entrySet()) {
      if (rewards.size() >= MAX_EXAMPLE_REWARDS) {
        break;
      }
      if (action.getValue().isEmpty()) {
        continue;
      }
      List<Payable> payables = action.getValue().get(0).payables();
      if (payables.isEmpty()) {
        continue;
      }
      Payable payable = payables.get(0);
      rewards.add(
          new Reward(
              action.getKey().key().value(),
              payable.amount().value(),
              payable.type().key().value()));
    }
    return List.copyOf(rewards);
  }
}
