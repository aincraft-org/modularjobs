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
    assertEquals(new BigDecimal("400"), job.experienceForLevel(4));
    assertEquals(3, job.levelForExperience(new BigDecimal("350")));
    assertEquals(root.displayName(), job.displayName());
    assertNull(root.parentKey());
  }

  @Test
  void playerStateCarriesOnlyTreeAndNodeIdentityWithProgressValues() {
    JobKey jobKey = new JobKey(Key.key("modularjobs", "miner"));
    JobNodeKey currentNodeKey = new JobNodeKey(Key.key("modularjobs", "prospector"));
    PlayerJobState state =
        new TestPlayerJobState(
            UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
            jobKey,
            currentNodeKey,
            BigDecimal.valueOf(350));

    assertEquals(jobKey, state.jobKey());
    assertEquals(currentNodeKey, state.currentNodeKey());
    assertEquals(new BigDecimal("350"), state.experience());
    assertEquals(3, state.level());
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
      @NotNull JobKey jobKey,
      @NotNull JobNodeKey currentNodeKey,
      @NotNull BigDecimal experience)
      implements PlayerJobState {

    @Override
    public int level() {
      return Math.max(1, experience.divideToIntegralValue(BigDecimal.valueOf(100)).intValue());
    }

    @Override
    public @NotNull BigDecimal experienceForLevel(int level) {
      return BigDecimal.valueOf(level * 100L);
    }

    @Override
    public @NotNull PlayerJobState withExperience(@NotNull BigDecimal experience) {
      return new TestPlayerJobState(playerId, jobKey, currentNodeKey, experience);
    }
  }
}
