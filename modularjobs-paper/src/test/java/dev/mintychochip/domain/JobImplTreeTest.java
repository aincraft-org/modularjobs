package dev.mintychochip.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.mintychochip.JobNodeKey;
import dev.mintychochip.PayableCurve;
import dev.mintychochip.domain.model.JobRecord;
import dev.mintychochip.domain.model.JobTreeRecord;
import dev.mintychochip.domain.model.PlayerJobStateRecord;
import dev.mintychochip.test.MockBukkitSupport;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class JobImplTreeTest {

  private Plugin plugin;

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
    plugin = MockBukkit.createMockPlugin("ModularJobs");
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void preservesEveryRawNodeAndFormulaAcrossTreeMapping() {
    JobRecord root =
        new JobRecord(
            "modularjobs:miner",
            "<gray>Miner",
            "Mines blocks",
            100,
            "level * level + 7",
            Map.of("experience", "base * level + 3"),
            null);
    JobRecord child =
        new JobRecord(
            "modularjobs:prospector",
            "<gold>Prospector",
            "Finds valuable ore",
            100,
            "child expression remains raw",
            Map.of("economy", "child payable remains raw"),
            root.jobKey());
    JobTreeRecord source =
        new JobTreeRecord(root, Map.of(root.jobKey(), root, child.jobKey(), child));

    JobImpl job = JobImpl.fromRecords(root, source.orderedNodes(), plugin);

    assertEquals(source, job.toRecord());
    PayableCurve experienceCurve = job.payableCurves().get(Key.key("modularjobs", "experience"));
    assertNotNull(experienceCurve);
    assertEquals(
        0,
        new BigDecimal("23")
            .compareTo(
                experienceCurve.evaluate(new PayableCurve.Parameters(new BigDecimal("10"), 2, 1))));
    assertEquals(
        List.of("miner", "prospector"),
        job.pathTo(new JobNodeKey(Key.key("modularjobs", "prospector"))).stream()
            .map(node -> node.nodeKey().key().value())
            .toList());
  }

  @Test
  void playerStateRoundTripRestoresActiveChildAgainstCompleteTree() {
    JobRecord root =
        new JobRecord("modularjobs:miner", "Miner", "Mines", 100, "level * 100", Map.of(), null);
    JobRecord child =
        new JobRecord(
            "modularjobs:prospector",
            "Prospector",
            "Finds ore",
            100,
            "unused child curve",
            Map.of(),
            root.jobKey());
    JobImpl job = JobImpl.fromRecords(root, List.of(root, child), plugin);
    JobNodeKey childKey = new JobNodeKey(Key.key("modularjobs", "prospector"));
    PlayerJobStateImpl state =
        new PlayerJobStateImpl(
            UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
            job,
            job.node(childKey),
            new BigDecimal("420"));

    PlayerJobStateRecord record = PersistenceConverters.toRecord(state);
    PlayerJobStateImpl restored = PlayerJobStateImpl.fromRecord(record, job);

    assertEquals(job.jobKey().asString(), record.jobKey());
    assertEquals(childKey.asString(), record.currentNodeKey());
    assertEquals(childKey, restored.currentNode().nodeKey());
    assertEquals(2, restored.job().nodes().size());
    assertEquals(new BigDecimal("420"), restored.experience());
  }
}
