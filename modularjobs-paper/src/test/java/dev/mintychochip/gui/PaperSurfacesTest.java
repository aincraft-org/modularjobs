package dev.mintychochip.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar.Color;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BossBar;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class PaperSurfacesTest {
  private static ServerMock server;

  @BeforeAll
  static void setUp() {
    server = dev.mintychochip.test.MockBukkitSupport.mockServer();
  }

  @AfterAll
  static void tearDown() {
    dev.mintychochip.test.MockBukkitSupport.unmockServer();
  }

  @Test
  void scoreboardLimitsRowsAndMakesDuplicateEntriesUnique() throws Exception {
    PlayerMock player = server.addPlayer("surface-scoreboard-" + UUID.randomUUID());
    PaperSurfaces surfaces = new PaperSurfaces();

    List<String> lines = new ArrayList<>();
    lines.add("duplicate");
    lines.add("duplicate");
    lines.add("   ");
    for (int i = 0; i < 15; i++) {
      lines.add("row-" + i);
    }
    surfaces.showScoreboard(player.getUniqueId(), "Native title", lines);

    Scoreboard shown = player.getScoreboard();
    Objective objective = shown.getObjectives().iterator().next();
    assertEquals("Native title", objective.getDisplayName());
    Map<?, ?> scores = objectiveScores(objective);
    assertEquals(15, scores.size());
    assertTrue(scores.keySet().stream().noneMatch(key -> ((String) key).isBlank()));
    assertTrue(scores.keySet().stream().filter(key -> ((String) key).startsWith("duplicate")).count() == 2);
  }

  @Test
  void replacementRemovesStaleEntriesAndHideRestoresCapturedScoreboard() throws Exception {
    PlayerMock player = server.addPlayer("surface-replace-" + UUID.randomUUID());
    Scoreboard original = server.getScoreboardManager().getNewScoreboard();
    player.setScoreboard(original);
    PaperSurfaces surfaces = new PaperSurfaces();

    surfaces.showScoreboard(player.getUniqueId(), "Old", List.of("stale", "same"));
    Scoreboard first = player.getScoreboard();
    surfaces.showScoreboard(player.getUniqueId(), "New", List.of("fresh", "same"));
    Scoreboard replacement = player.getScoreboard();

    assertNotSame(first, replacement);
    Map<?, ?> scores = objectiveScores(replacement.getObjectives().iterator().next());
    assertTrue(scores.keySet().stream().anyMatch(entry -> ((String) entry).startsWith("fresh")));
    assertFalse(scores.keySet().stream().anyMatch(entry -> ((String) entry).startsWith("stale")));
    surfaces.hideScoreboard(player.getUniqueId());
    assertSame(original, player.getScoreboard());
  }

  @Test
  void bossBarsClampProgressMapColorsAndReplaceOnlyTheirKey() throws Exception {
    PlayerMock player = server.addPlayer("surface-bars-" + UUID.randomUUID());
    PaperSurfaces surfaces = new PaperSurfaces();

    surfaces.showBossBar(player.getUniqueId(), "alpha", "Alpha", -1.0, Color.RED);
    BossBar alpha = bossBars(surfaces).get(player.getUniqueId() + ":alpha");
    assertEquals(0.0, alpha.getProgress());
    surfaces.showBossBar(player.getUniqueId(), "nan", "NaN", Double.NaN, Color.BLUE);
    assertEquals(0.0, bossBars(surfaces).get(player.getUniqueId() + ":nan").getProgress());
    surfaces.showBossBar(player.getUniqueId(), "high", "High", 2.0, Color.GREEN);
    assertEquals(1.0, bossBars(surfaces).get(player.getUniqueId() + ":high").getProgress());

    for (Color color : Color.values()) {
      String key = "color-" + color.name();
      surfaces.showBossBar(player.getUniqueId(), key, key, 0.5, color);
      assertEquals(
          BarColor.valueOf(color.name()),
          bossBars(surfaces).get(player.getUniqueId() + ":" + key).getColor());
    }

    surfaces.showBossBar(player.getUniqueId(), "other", "Other", 0.5, Color.PURPLE);
    BossBar other = bossBars(surfaces).get(player.getUniqueId() + ":other");
    surfaces.showBossBar(player.getUniqueId(), "alpha", "Replacement", 0.5, Color.YELLOW);
    assertSame(other, bossBars(surfaces).get(player.getUniqueId() + ":other"));
    assertEquals("Replacement", bossBars(surfaces).get(player.getUniqueId() + ":alpha").getTitle());
    assertFalse(alpha.getPlayers().contains(player));
  }

  @Test
  void hideAllAndCloseAllRemoveNativeObjectsAndTrackedState() throws Exception {
    PlayerMock player = server.addPlayer("surface-close-" + UUID.randomUUID());
    Scoreboard original = server.getScoreboardManager().getNewScoreboard();
    player.setScoreboard(original);
    PaperSurfaces surfaces = new PaperSurfaces();

    surfaces.showScoreboard(player.getUniqueId(), "Close", List.of("line"));
    surfaces.showBossBar(player.getUniqueId(), "one", "One", 0.5, Color.PINK);
    surfaces.showBossBar(player.getUniqueId(), "two", "Two", 0.5, Color.WHITE);
    surfaces.hideAllBossBars(player.getUniqueId());
    assertTrue(bossBars(surfaces).isEmpty());
    surfaces.closeAll();
    assertTrue(bossBars(surfaces).isEmpty());
    assertSame(original, player.getScoreboard());
    assertTrue(activeScoreboards(surfaces).isEmpty());
    assertTrue(previousScoreboards(surfaces).isEmpty());
  }

  @SuppressWarnings("unchecked")
  private static Map<?, ?> objectiveScores(Objective objective) throws Exception {
    Field field = objective.getClass().getDeclaredField("scores");
    field.setAccessible(true);
    return (Map<?, ?>) field.get(objective);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, BossBar> bossBars(PaperSurfaces surfaces) throws Exception {
    Field field = PaperSurfaces.class.getDeclaredField("bossBars");
    field.setAccessible(true);
    return (Map<String, BossBar>) field.get(surfaces);
  }

  @SuppressWarnings("unchecked")
  private static Map<UUID, Scoreboard> activeScoreboards(PaperSurfaces surfaces) throws Exception {
    Field field = PaperSurfaces.class.getDeclaredField("activeScoreboards");
    field.setAccessible(true);
    return (Map<UUID, Scoreboard>) field.get(surfaces);
  }

  @SuppressWarnings("unchecked")
  private static Map<UUID, Scoreboard> previousScoreboards(PaperSurfaces surfaces)
      throws Exception {
    Field field = PaperSurfaces.class.getDeclaredField("previousScoreboards");
    field.setAccessible(true);
    return (Map<UUID, Scoreboard>) field.get(surfaces);
  }
}
