package dev.mintychochip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

class JobTreeContractTest {

  @Test
  void jobOwnsRootedNodeTreeAndTreeWideRules() {
    JobKey jobKey = new JobKey(Key.key("modularjobs", "miner"));
    JobNode root = node(jobKey, "miner", null);
    JobNode prospector = node(jobKey, "prospector", root.nodeKey());
    Job job =
        new TestJob(jobKey, root, Map.of(root.nodeKey(), root, prospector.nodeKey(), prospector));

    assertEquals(root, job.rootNode());
    assertEquals(prospector, job.node(prospector.nodeKey()));
    assertEquals(List.of(root, prospector), job.pathTo(prospector.nodeKey()));
    assertEquals(100, job.maxLevel());
    assertEquals(root.displayName(), job.displayName());
    assertNull(root.parentKey());
  }

  @Test
  void playerStateTracksOneActiveNodeWithinTheWholeTree() {
    JobKey jobKey = new JobKey(Key.key("modularjobs", "miner"));
    JobNode root = node(jobKey, "miner", null);
    JobNode prospector = node(jobKey, "prospector", root.nodeKey());
    Job job =
        new TestJob(jobKey, root, Map.of(root.nodeKey(), root, prospector.nodeKey(), prospector));
    PlayerJobState initial =
        new TestPlayerJobState(
            UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
            job,
            root,
            BigDecimal.valueOf(350));

    PlayerJobState specialized = initial.withCurrentNode(prospector.nodeKey());

    assertEquals(job, specialized.job());
    assertEquals(prospector, specialized.currentNode());
    assertEquals(initial.experience(), specialized.experience());
    assertEquals(3, specialized.level());
  }

  private static @NotNull JobNode node(
      @NotNull JobKey jobKey, @NotNull String value, @Nullable JobNodeKey parentKey) {
    JobNodeKey nodeKey = new JobNodeKey(Key.key("modularjobs", value));
    return new TestNode(
        jobKey,
        nodeKey,
        parentKey,
        Component.text(Character.toUpperCase(value.charAt(0)) + value.substring(1)),
        Component.text(value));
  }

  private record TestJob(
      @NotNull JobKey jobKey, @NotNull JobNode rootNode, @NotNull Map<JobNodeKey, JobNode> nodes)
      implements Job {

    @Override
    public int maxLevel() {
      return 100;
    }

    @Override
    public @NotNull LevelingCurve levelingCurve() {
      return parameters -> BigDecimal.valueOf(parameters.level() * 100L);
    }

    @Override
    public @NotNull Map<Key, PayableCurve> payableCurves() {
      return Map.of();
    }
  }

  private record TestNode(
      @NotNull JobKey jobKey,
      @NotNull JobNodeKey nodeKey,
      @Nullable JobNodeKey parentKey,
      @NotNull Component displayName,
      @NotNull Component description)
      implements JobNode {

    @Override
    public @NotNull String getPlainName() {
      return nodeKey.key().value();
    }
  }

  private record TestPlayerJobState(
      @NotNull UUID playerId,
      @NotNull Job job,
      @NotNull JobNode currentNode,
      @NotNull BigDecimal experience)
      implements PlayerJobState {

    @Override
    public int level() {
      int level = experience.divideToIntegralValue(BigDecimal.valueOf(100)).intValue();
      return Math.max(1, Math.min(job.maxLevel(), level));
    }

    @Override
    public @NotNull BigDecimal experienceForLevel(int level) {
      return job.levelingCurve().evaluate(new LevelingCurve.Parameters(level));
    }

    @Override
    public @NotNull PlayerJobState withExperience(@NotNull BigDecimal experience) {
      return new TestPlayerJobState(playerId, job, currentNode, experience);
    }

    @Override
    public @NotNull PlayerJobState withCurrentNode(@NotNull JobNodeKey nodeKey) {
      JobNode node = job.node(nodeKey);
      if (node == null) {
        throw new IllegalArgumentException("Unknown job node: " + nodeKey.asString());
      }
      return new TestPlayerJobState(playerId, job, node, experience);
    }
  }
}
