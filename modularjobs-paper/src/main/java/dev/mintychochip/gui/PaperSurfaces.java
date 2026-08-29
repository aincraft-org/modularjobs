package dev.mintychochip.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar.Color;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

/** Native Paper scoreboard and boss-bar surfaces shared by ModularJobs commands. */
public final class PaperSurfaces {
  private static final int MAX_SCOREBOARD_LINES = 15;
  private static final String OBJECTIVE_NAME = "modularjobs";
  private static final ChatColor[] ENTRY_SUFFIXES =
      {
        ChatColor.BLACK,
        ChatColor.DARK_BLUE,
        ChatColor.DARK_GREEN,
        ChatColor.DARK_AQUA,
        ChatColor.DARK_RED,
        ChatColor.DARK_PURPLE,
        ChatColor.GOLD,
        ChatColor.GRAY,
        ChatColor.DARK_GRAY,
        ChatColor.BLUE,
        ChatColor.GREEN,
        ChatColor.AQUA,
        ChatColor.RED,
        ChatColor.LIGHT_PURPLE,
        ChatColor.YELLOW
      };

  private final ScoreboardManager scoreboardManager;
  private final Map<UUID, Scoreboard> previousScoreboards = new HashMap<>();
  private final Map<UUID, Scoreboard> activeScoreboards = new HashMap<>();
  private final Map<String, org.bukkit.boss.BossBar> bossBars = new HashMap<>();

  /** Creates a native surface manager using the server's scoreboard manager. */
  public PaperSurfaces() {
    scoreboardManager = Bukkit.getScoreboardManager();
    if (scoreboardManager == null) {
      throw new IllegalStateException("Bukkit scoreboard manager is unavailable");
    }
  }

  /** Shows or replaces an audience's sidebar scoreboard. */
  public void showScoreboard(UUID audience, String title, List<String> lines) {
    Player player = Bukkit.getPlayer(require(audience, "audience"));
    if (player == null) {
      return;
    }

    if (!activeScoreboards.containsKey(audience)) {
      previousScoreboards.put(audience, player.getScoreboard());
    }
    Scoreboard old = activeScoreboards.remove(audience);
    if (old != null) {
      for (String entry : new ArrayList<>(old.getEntries())) {
        old.resetScores(entry);
      }
    }

    Scoreboard scoreboard = scoreboardManager.getNewScoreboard();
    Objective objective =
        scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, require(title, "title"));
    objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    int score = MAX_SCOREBOARD_LINES;
    int index = 0;
    for (String line : lines == null ? List.<String>of() : lines) {
      if (line == null || line.isBlank() || score == 0) {
        continue;
      }
      String entry = uniqueEntry(line, index++);
      objective.getScore(entry).setScore(score--);
    }

    activeScoreboards.put(audience, scoreboard);
    player.setScoreboard(scoreboard);
  }

  /** Hides an audience's sidebar and restores its pre-surface scoreboard. */
  public void hideScoreboard(UUID audience) {
    UUID id = require(audience, "audience");
    Scoreboard active = activeScoreboards.remove(id);
    Scoreboard previous = previousScoreboards.remove(id);
    if (active != null) {
      for (String entry : new ArrayList<>(active.getEntries())) {
        active.resetScores(entry);
      }
    }
    Player player = Bukkit.getPlayer(id);
    if (player != null && previous != null) {
      player.setScoreboard(previous);
    }
  }

  /** Shows or replaces one keyed native boss bar for an audience. */
  public void showBossBar(UUID audience, String barKey, String title, double progress, Color color) {
    UUID id = require(audience, "audience");
    String key = require(barKey, "barKey");
    Player player = Bukkit.getPlayer(id);
    if (player == null) {
      return;
    }

    String composite = id + ":" + key;
    org.bukkit.boss.BossBar previous = bossBars.remove(composite);
    if (previous != null) {
      previous.removePlayer(player);
      previous.removeAll();
    }
    org.bukkit.boss.BossBar bossBar =
        Bukkit.createBossBar(
            require(title, "title"), toBarColor(require(color, "color")), BarStyle.SOLID);
    bossBar.setProgress(clamp(progress));
    bossBar.addPlayer(player);
    bossBars.put(composite, bossBar);
  }

  /** Hides one keyed native boss bar. */
  public void hideBossBar(UUID audience, String barKey) {
    UUID id = require(audience, "audience");
    String key = require(barKey, "barKey");
    org.bukkit.boss.BossBar bossBar = bossBars.remove(id + ":" + key);
    if (bossBar != null) {
      Player player = Bukkit.getPlayer(id);
      if (player != null) {
        bossBar.removePlayer(player);
      }
      bossBar.removeAll();
    }
  }

  /** Hides all native boss bars tracked for an audience. */
  public void hideAllBossBars(UUID audience) {
    UUID id = require(audience, "audience");
    List<String> keys = new ArrayList<>();
    String prefix = id + ":";
    for (String key : bossBars.keySet()) {
      if (key.startsWith(prefix)) {
        keys.add(key.substring(prefix.length()));
      }
    }
    for (String key : keys) {
      hideBossBar(id, key);
    }
  }

  /** Removes every native surface managed by this instance. */
  public void closeAll() {
    for (UUID audience : new ArrayList<>(activeScoreboards.keySet())) {
      hideScoreboard(audience);
    }
    for (String composite : new ArrayList<>(bossBars.keySet())) {
      int separator = composite.indexOf(':');
      UUID audience = UUID.fromString(composite.substring(0, separator));
      hideBossBar(audience, composite.substring(separator + 1));
    }
    previousScoreboards.clear();
  }

  private static String uniqueEntry(String line, int index) {
    String suffix = ENTRY_SUFFIXES[index % ENTRY_SUFFIXES.length].toString();
    int maxLineLength = 40 - suffix.length();
    if (line.length() > maxLineLength) {
      line = line.substring(0, maxLineLength);
    }
    return line + suffix;
  }

  private static double clamp(double progress) {
    if (Double.isNaN(progress) || progress < 0.0) {
      return 0.0;
    }
    return Math.min(progress, 1.0);
  }

  private static BarColor toBarColor(Color color) {
    return BarColor.valueOf(color.name());
  }

  private static <T> T require(T value, String name) {
    if (value == null) {
      throw new NullPointerException(name);
    }
    return value;
  }
}
