package dev.mintychochip.gui;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import dev.mintychochip.gui.JobBrowseModel.Entry;
import dev.mintychochip.gui.JobBrowseModel.Reward;
import dev.mintychochip.util.Messages;
import java.math.BigDecimal;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Renders the job catalog as interactive Adventure chat components. */
public final class JobBrowseChatRenderer {
  private static final Splitter WORD_SPLITTER =
      Splitter.on(CharMatcher.anyOf("_:")).omitEmptyStrings();

  private final JobBrowseModel model;

  /** Creates a chat renderer over the shared browse model. */
  public JobBrowseChatRenderer(@NotNull JobBrowseModel model) {
    this.model = model;
  }

  /** Sends the current job catalog to the player. */
  public void open(@NotNull Player player) {
    for (Component line : render(model.entries(player))) {
      player.sendMessage(line);
    }
  }

  @NotNull
  List<Component> render(@NotNull List<Entry> entries) {
    java.util.ArrayList<Component> lines = new java.util.ArrayList<>();
    lines.add(Component.empty());
    lines.add(Messages.component("<neutral>━━━━━━━━━ <primary>Browse Jobs <neutral>━━━━━━━━━"));
    if (entries.isEmpty()) {
      lines.add(Messages.component("<neutral>No jobs are currently available."));
    }

    for (Entry entry : entries) {
      Component title =
          Component.text("• ")
              .append(entry.job().displayName())
              .append(
                  Messages.component(
                      entry.joined() ? " <success>✓ Joined" : " <neutral>· Available"))
              .hoverEvent(HoverEvent.showText(details(entry)))
              .clickEvent(ClickEvent.runCommand(entry.infoCommand()));
      lines.add(title);
      lines.add(Component.text("  ").append(entry.job().description()));
      lines.add(actions(entry));
    }

    lines.add(Messages.component("<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    lines.add(Component.empty());
    return List.copyOf(lines);
  }

  private static @NotNull Component actions(@NotNull Entry entry) {
    Component result =
        Component.text("  ")
            .append(
                Messages.component("<primary>[Details]")
                    .hoverEvent(
                        HoverEvent.showText(Messages.component("<neutral>View job details")))
                    .clickEvent(ClickEvent.runCommand(entry.infoCommand())));
    if (!entry.joined()) {
      result =
          result
              .append(Component.space())
              .append(
                  Messages.component("<success>[Join]")
                      .hoverEvent(HoverEvent.showText(Messages.component("<neutral>Join this job")))
                      .clickEvent(ClickEvent.runCommand(entry.joinCommand())));
    }
    return result;
  }

  private static @NotNull Component details(@NotNull Entry entry) {
    Component details =
        Messages.component(
            "<neutral>Max level: <secondary>"
                + entry.job().maxLevel()
                + "\n<neutral>Active players: <accent>"
                + entry.activePlayers());
    if (entry.upgradeNodes() > 0) {
      details =
          details.append(
              Messages.component("\n<neutral>Upgrade nodes: <accent>" + entry.upgradeNodes()));
    }
    if (!entry.exampleRewards().isEmpty()) {
      details = details.append(Messages.component("\n<primary>Example rewards:"));
      for (Reward reward : entry.exampleRewards()) {
        details =
            details.append(
                Messages.component(
                    "\n<neutral>• <secondary>"
                        + humanize(reward.action())
                        + " <neutral>→ <success>"
                        + plain(reward.amount())
                        + " "
                        + humanize(reward.payableType())));
      }
    }
    return details;
  }

  private static @NotNull String plain(@NotNull BigDecimal value) {
    return value.signum() == 0 ? "0" : value.stripTrailingZeros().toPlainString();
  }

  static @NotNull String humanize(@NotNull String value) {
    Iterable<String> words = WORD_SPLITTER.split(value);
    StringBuilder result = new StringBuilder();
    for (String word : words) {
      if (result.length() > 0) {
        result.append(' ');
      }
      result.append(Character.toUpperCase(word.charAt(0)));
      result.append(word.substring(1).toLowerCase(java.util.Locale.ROOT));
    }
    return result.toString();
  }
}
