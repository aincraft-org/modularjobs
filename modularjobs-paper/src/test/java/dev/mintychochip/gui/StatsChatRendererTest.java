package dev.mintychochip.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.JobKey;
import dev.mintychochip.JobStats;
import java.math.BigDecimal;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class StatsChatRendererTest {
  private static final PlainTextComponentSerializer PLAIN =
      PlainTextComponentSerializer.plainText();

  @Test
  void rendersProgressFromSharedStatsComponents() {
    JobStats stats =
        new JobStats(
            new JobKey(Key.key("modularjobs", "miner")),
            Component.text("Miner"),
            1,
            100,
            new BigDecimal("150"),
            new BigDecimal("50"),
            new BigDecimal("100"),
            new BigDecimal("50"),
            new BigDecimal("50.0"),
            false);

    List<Component> lines = new StatsChatRenderer().render("Alex", true, List.of(stats));
    String rendered = lines.stream().map(PLAIN::serialize).reduce("", (a, b) -> a + "\n" + b);

    assertTrue(rendered.contains("Job Statistics"));
    assertTrue(rendered.contains("Miner · Level 1/100"));
    assertTrue(rendered.contains("██████████░░░░░░░░░░ 50.0% · 50/100 XP"));
  }

  @Test
  void emptyStatsOffersClickableBrowseAction() {
    List<Component> lines = new StatsChatRenderer().render("Alex", true, List.of());
    Component browseLine = lines.get(2);

    assertTrue(PLAIN.serialize(browseLine).contains("Browse jobs"));
    assertEquals(ClickEvent.runCommand("/j browse chat"), browseLine.clickEvent());
  }
}
