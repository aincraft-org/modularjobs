package dev.mintychochip.gui;

import dev.mintychochip.JobStats;
import dev.mintychochip.util.Messages;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/** Renders shared job-stat projections as interactive Adventure chat lines. */
public final class StatsChatRenderer {

  /** Sends a complete statistics view to an Adventure-capable command sender. */
  public void open(
      @NotNull CommandSender viewer,
      @NotNull String targetName,
      boolean ownStats,
      @NotNull List<JobStats> stats) {
    for (Component line : render(targetName, ownStats, stats)) {
      viewer.sendMessage(line);
    }
  }

  /** Builds a complete statistics view without sending it. */
  public @NotNull List<Component> render(
      @NotNull String targetName, boolean ownStats, @NotNull List<JobStats> stats) {
    List<Component> lines = new ArrayList<>(stats.size() * 2 + 5);
    lines.add(Component.empty());
    Component title =
        ownStats
            ? Messages.component("<primary>Job Statistics")
            : Component.text(targetName + "'s Job Statistics", NamedTextColor.GOLD);
    lines.add(
        Messages.component("<neutral>━━━━━━━━━ ")
            .append(title)
            .append(Messages.component(" <neutral>━━━━━━━━━")));

    if (stats.isEmpty()) {
      lines.add(
          Messages.component("<secondary>[Browse jobs]")
              .hoverEvent(HoverEvent.showText(Messages.component("<neutral>Open the job catalog")))
              .clickEvent(ClickEvent.runCommand("/j browse chat")));
    } else {
      for (JobStats job : stats) {
        lines.add(jobTitle(job));
        lines.add(jobProgress(job));
      }
    }

    lines.add(Messages.component("<neutral>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    lines.add(Component.empty());
    return List.copyOf(lines);
  }

  private static @NotNull Component jobTitle(@NotNull JobStats stats) {
    Component title =
        Component.text("  ")
            .append(stats.displayName())
            .append(
                Messages.component(
                    " <neutral>· Level <secondary>"
                        + stats.level()
                        + "<neutral>/<secondary>"
                        + stats.maxLevel()));
    Component hover =
        Messages.component(
            "<neutral>Total XP: <accent>"
                + plain(stats.totalExperience())
                + "\n<neutral>Remaining XP: <secondary>"
                + plain(stats.remainingExperience()));
    return title
        .hoverEvent(HoverEvent.showText(hover))
        .clickEvent(ClickEvent.runCommand("/j info " + stats.jobKey().asString()));
  }

  private static @NotNull Component jobProgress(@NotNull JobStats stats) {
    NamedTextColor barColor = progressColor(stats.progressPercentage());
    Component line =
        Component.text("  ")
            .append(Component.text(stats.progressBar(20), barColor))
            .append(
                Messages.component(
                    " <secondary>" + stats.progressPercentage().toPlainString() + "%"));
    if (stats.atMaxLevel()) {
      return line.append(
          Messages.component(
              " <neutral>· <success>MAX LEVEL <neutral>· <accent>"
                  + plain(stats.totalExperience())
                  + " XP"));
    }
    return line.append(
        Messages.component(
            " <neutral>· <accent>"
                + plain(stats.levelExperience())
                + "<neutral>/<accent>"
                + plain(stats.requiredExperience())
                + " XP"));
  }

  private static @NotNull NamedTextColor progressColor(@NotNull BigDecimal percentage) {
    if (percentage.compareTo(BigDecimal.valueOf(75)) >= 0) {
      return NamedTextColor.AQUA;
    }
    if (percentage.compareTo(BigDecimal.valueOf(50)) >= 0) {
      return NamedTextColor.YELLOW;
    }
    if (percentage.compareTo(BigDecimal.valueOf(25)) >= 0) {
      return NamedTextColor.GOLD;
    }
    return NamedTextColor.RED;
  }

  private static @NotNull String plain(@NotNull BigDecimal value) {
    return value.signum() == 0 ? "0" : value.stripTrailingZeros().toPlainString();
  }
}
