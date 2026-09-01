package dev.mintychochip.commands.top;

import dev.mintychochip.PlayerJobState;
import dev.mintychochip.commands.Page;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Renders one page of a job leaderboard to a sender. Implementations define the output medium (chat
 * messages, scoreboard, …).
 */
@FunctionalInterface
public interface JobsTopPageConsumer {

  /**
   * Renders the given page to the sender.
   *
   * @param jobName display name of the job whose top is shown
   * @param page page of entries to render
   * @param sender recipient of the rendered output
   * @param maxPages total page count used for headers and navigation
   * @param allEntries full cached leaderboard used for context (e.g. viewer rank)
   */
  void consume(
      @NotNull Component jobName,
      @NotNull Page<PlayerJobState> page,
      @NotNull CommandSender sender,
      int maxPages,
      @NotNull List<PlayerJobState> allEntries);
}
