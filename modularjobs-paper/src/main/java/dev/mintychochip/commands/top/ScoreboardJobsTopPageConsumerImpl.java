package dev.mintychochip.commands.top;

import dev.mintychochip.PlayerJobState;
import dev.mintychochip.commands.Page;
import dev.mintychochip.commands.TextScoreboard;
import dev.mintychochip.commands.components.PlayerComponent;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Renders leaderboard entries onto a player's side scoreboard. Only renders when the sender is a
 * player; other senders (console, command blocks) are silently ignored.
 */
public final class ScoreboardJobsTopPageConsumerImpl implements JobsTopPageConsumer {

  private static final String ENTRY_FORMAT = "<rank>. <player>: <level>";

  private final TextScoreboard scoreBoard;

  /**
   * Creates a scoreboard consumer that writes rows to the given surface.
   *
   * @param scoreBoard scoreboard surface the rows are written to
   */
  public ScoreboardJobsTopPageConsumerImpl(@NotNull TextScoreboard scoreBoard) {
    this.scoreBoard = scoreBoard;
  }

  @Override
  public void consume(
      @NotNull Component jobName,
      @NotNull Page<PlayerJobState> page,
      @NotNull CommandSender sender,
      int maxPages,
      @NotNull List<PlayerJobState> allEntries) {
    if (!(sender instanceof Player player)) {
      return;
    }
    List<PlayerJobState> data = page.data();
    int pageNumber = page.pageNumber();
    int pageSize = page.size();
    for (int i = 0; i < data.size(); i++) {
      PlayerJobState state = data.get(i);
      OfflinePlayer statePlayer = Bukkit.getOfflinePlayer(state.playerId());
      Component row =
          MiniMessage.miniMessage()
              .deserialize(
                  ENTRY_FORMAT,
                  TagResolver.builder()
                      .tag(
                          "rank",
                          Tag.inserting(Component.text(i + 1 + (pageNumber - 1) * pageSize)))
                      .tag("player", Tag.inserting(PlayerComponent.of(statePlayer)))
                      .tag("level", Tag.inserting(LevelComponent.of(state)))
                      .build());
      scoreBoard.setLine(i, row, Component.empty());
    }
    scoreBoard.setCurrent(player);
  }
}
