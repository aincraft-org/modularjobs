package dev.mintychochip.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mintychochip.Job;
import dev.mintychochip.JobStats;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.gui.StatsChatRenderer;
import dev.mintychochip.gui.StatsGui;
import dev.mintychochip.service.JobService;
import dev.mintychochip.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Stats command. */
public class StatsCommand implements JobsCommand {

  private final JobService jobService;
  private final StatsGui statsGui;
  private final StatsChatRenderer statsChatRenderer;

  /** Stats command. */
  public StatsCommand(
      @NotNull JobService jobService,
      @NotNull StatsGui statsGui,
      @NotNull StatsChatRenderer statsChatRenderer) {
    this.jobService = jobService;
    this.statsGui = statsGui;
    this.statsChatRenderer = statsChatRenderer;
  }

  @Override
  public @NotNull LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("stats")
        // /jobs stats chat <playerName> - admin variant (chat output)
        .then(
            Commands.literal("chat")
                .then(
                    Commands.argument("player", StringArgumentType.word())
                        .requires(
                            source -> source.getSender().hasPermission("jobs.command.admin.stats"))
                        .executes(
                            context -> {
                              CommandSourceStack source = context.getSource();
                              CommandSender sender = source.getSender();

                              String playerName = context.getArgument("player", String.class);
                              OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(playerName);

                              if (target == null) {
                                Messages.send(sender, "<error>Player not found: " + playerName);
                                return 0;
                              }

                              displayStatsChat(sender, target);
                              return Command.SINGLE_SUCCESS;
                            }))
                // /jobs stats chat - player variant (chat output)
                .requires(source -> source.getSender().hasPermission("jobs.command.stats"))
                .executes(
                    context -> {
                      CommandSourceStack source = context.getSource();
                      CommandSender sender = source.getSender();

                      if (!(sender instanceof Player player)) {
                        Messages.send(sender, "<error>This command can only be used by players.");
                        return 0;
                      }

                      displayStatsChat(player, player);
                      return Command.SINGLE_SUCCESS;
                    }))
        // /jobs stats <playerName> - admin variant (GUI output)
        .then(
            Commands.argument("player", StringArgumentType.word())
                .requires(source -> source.getSender().hasPermission("jobs.command.admin.stats"))
                .executes(
                    context -> {
                      CommandSourceStack source = context.getSource();
                      CommandSender sender = source.getSender();

                      String playerName = context.getArgument("player", String.class);
                      OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(playerName);

                      if (target == null) {
                        Messages.send(sender, "<error>Player not found: " + playerName);
                        return 0;
                      }

                      displayStats(sender, target);
                      return Command.SINGLE_SUCCESS;
                    }))
        // /jobs stats - player variant (GUI output)
        .requires(source -> source.getSender().hasPermission("jobs.command.stats"))
        .executes(
            context -> {
              CommandSourceStack source = context.getSource();
              CommandSender sender = source.getSender();

              if (!(sender instanceof Player player)) {
                Messages.send(sender, "<error>This command can only be used by players.");
                return 0;
              }

              displayStats(player, player);
              return Command.SINGLE_SUCCESS;
            });
  }

  /** Displays stats in the native inventory GUI, falling back to chat if sender is not a player. */
  private void displayStats(@NotNull CommandSender viewer, @NotNull OfflinePlayer target) {
    if (viewer instanceof Player player) {
      displayStatsGui(player, target);
    } else {
      displayStatsChat(viewer, target);
    }
  }

  /** Displays stats in the native inventory GUI. */
  private void displayStatsGui(@NotNull Player viewer, @NotNull OfflinePlayer target) {
    final List<PlayerJobState> states = jobService.getPlayerJobStates(target.getUniqueId());
    statsGui.open(viewer, target, states, 1);
  }

  /** Displays stats through the shared Adventure chat renderer. */
  private void displayStatsChat(@NotNull CommandSender viewer, @NotNull OfflinePlayer target) {
    List<PlayerJobState> states = jobService.getPlayerJobStates(target.getUniqueId());
    List<JobStats> stats = new ArrayList<>(states.size());
    for (PlayerJobState state : states) {
      Job job = jobService.getJob(state);
      stats.add(JobStats.from(job, state));
    }

    String targetName = target.getName() != null ? target.getName() : "Unknown";
    boolean ownStats =
        viewer instanceof Player player && player.getUniqueId().equals(target.getUniqueId());
    statsChatRenderer.open(viewer, targetName, ownStats, stats);
  }
}
