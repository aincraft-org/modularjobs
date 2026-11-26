package dev.mintychochip.profession.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.Job;
import dev.mintychochip.JobKey;
import dev.mintychochip.JobNode;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.JobTask;
import dev.mintychochip.LevelingCurve;
import dev.mintychochip.PayableCurve;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.action.ActionTypeImpl;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Context;
import dev.mintychochip.profession.content.CraftTaskSnapshot;
import dev.mintychochip.service.JobService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class CraftRecipeContentValidatorTest {

  private static final ActionType CRAFT =
      new ActionTypeImpl("Craft", Key.key("modularjobs", "craft"));
  private static final ActionType SMELT =
      new ActionTypeImpl("Smelt", Key.key("modularjobs", "smelt"));

  @Test
  void collectCraftTasksIncludesChildDefinitionsWithoutDuplicatingInheritedTasks() {
    Job blacksmith = jobWithChild("blacksmith", "weaponsmith");
    Job miner = job("miner");
    JobNode root = blacksmith.rootNode();
    JobNode child =
        blacksmith.nodes().values().stream()
            .filter(node -> !node.equals(root))
            .findFirst()
            .orElseThrow();
    JobTask rootCraft =
        new JobTask(
            root.nodeKey(),
            Key.key("modularjobs", "craft"),
            Key.key("minecraft", "iron_sword"),
            List.of());
    JobTask childCraft =
        new JobTask(
            child.nodeKey(),
            Key.key("modularjobs", "craft"),
            Key.key("minecraft", "golden_sword"),
            List.of());
    JobTask smeltTask =
        new JobTask(
            root.nodeKey(),
            Key.key("modularjobs", "smelt"),
            Key.key("minecraft", "iron_ingot"),
            List.of());

    JobService jobService =
        new StubJobService(
            List.of(blacksmith, miner),
            Map.of(
                root.nodeKey(),
                Map.of(CRAFT, List.of(rootCraft), SMELT, List.of(smeltTask)),
                child.nodeKey(),
                Map.of(CRAFT, List.of(rootCraft, childCraft), SMELT, List.of(smeltTask)),
                miner.rootNode().nodeKey(),
                Map.of()));

    List<CraftTaskSnapshot> snapshots = CraftRecipeContentValidator.collectCraftTasks(jobService);

    assertEquals(2, snapshots.size());
    assertEquals(
        Set.of(root.nodeKey(), child.nodeKey()),
        snapshots.stream()
            .map(CraftTaskSnapshot::nodeKey)
            .collect(java.util.stream.Collectors.toSet()));
    assertEquals(
        Set.of(Key.key("minecraft", "iron_sword"), Key.key("minecraft", "golden_sword")),
        snapshots.stream()
            .map(CraftTaskSnapshot::contextKey)
            .collect(java.util.stream.Collectors.toSet()));
  }

  @Test
  void summaryLineReportsCounts() {
    Key output = Key.key("minecraft", "stone_bricks");
    var report =
        dev.mintychochip.profession.content.CraftRecipeContentValidation.validate(
            List.of(
                new CraftTaskSnapshot(
                    new JobNodeKey(Key.key("modularjobs", "artisan")), output, output)),
            List.of());

    String summary = CraftRecipeContentValidator.summaryLine(report);
    assertTrue(summary.contains("1 craft task(s) without recipe metadata"));
    assertTrue(summary.contains("0 recipe(s) without craft task(s)"));
  }

  private static @NotNull Job job(@NotNull String value) {
    JobKey jobKey = new JobKey(Key.key("modularjobs", value));
    JobNodeKey nodeKey = new JobNodeKey(jobKey.key());
    JobNode root =
        new StubNode(jobKey, nodeKey, null, Component.text(value), Component.text(value));
    return new StubJob(jobKey, root, Map.of(nodeKey, root));
  }

  private static @NotNull Job jobWithChild(@NotNull String rootValue, @NotNull String childValue) {
    JobKey jobKey = new JobKey(Key.key("modularjobs", rootValue));
    JobNodeKey rootKey = new JobNodeKey(jobKey.key());
    JobNodeKey childKey = new JobNodeKey(Key.key("modularjobs", childValue));
    JobNode root =
        new StubNode(jobKey, rootKey, null, Component.text(rootValue), Component.text(rootValue));
    JobNode child =
        new StubNode(
            jobKey, childKey, rootKey, Component.text(childValue), Component.text(childValue));
    return new StubJob(jobKey, root, Map.of(rootKey, root, childKey, child));
  }

  private record StubJob(
      @NotNull JobKey jobKey, @NotNull JobNode rootNode, @NotNull Map<JobNodeKey, JobNode> nodes)
      implements Job {

    @Override
    public @NotNull LevelingCurve levelingCurve() {
      return level -> BigDecimal.ONE;
    }

    @Override
    public @NotNull Map<Key, PayableCurve> payableCurves() {
      return Map.of();
    }

    @Override
    public int maxLevel() {
      return 100;
    }
  }

  private record StubNode(
      @NotNull JobKey jobKey,
      @NotNull JobNodeKey nodeKey,
      JobNodeKey parentKey,
      @NotNull Component displayName,
      @NotNull Component description)
      implements JobNode {

    @Override
    public @NotNull String getPlainName() {
      return nodeKey.key().value();
    }
  }

  private static final class StubJobService implements JobService {
    private final List<Job> jobs;
    private final Map<JobNodeKey, Map<ActionType, List<JobTask>>> tasksByNode;

    private StubJobService(
        @NotNull List<Job> jobs,
        @NotNull Map<JobNodeKey, Map<ActionType, List<JobTask>>> tasksByNode) {
      this.jobs = List.copyOf(jobs);
      this.tasksByNode = Map.copyOf(tasksByNode);
    }

    @Override
    public @NotNull List<Job> getJobs() {
      return jobs;
    }

    @Override
    public @NotNull Map<ActionType, List<JobTask>> getAllTasks(
        @NotNull Job job, @NotNull JobNodeKey nodeKey) {
      return tasksByNode.getOrDefault(nodeKey, Map.of());
    }

    @Override
    public @NotNull Job getJob(@NotNull String jobKey) {
      throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull JobTask getTask(
        @NotNull Job job,
        @NotNull JobNodeKey nodeKey,
        @NotNull ActionType type,
        @NotNull Context context) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean update(@NotNull PlayerJobState state) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean joinJob(@NotNull String playerId, @NotNull String jobKey) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean leaveJob(@NotNull String playerId, @NotNull String jobKey) {
      throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull PlayerJobState getPlayerJobState(
        @NotNull String playerId, @NotNull String jobKey) {
      throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull List<PlayerJobState> getPlayerJobStates(@NotNull UUID playerId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull List<PlayerJobState> getPlayerJobStates(@NotNull Key jobKey, int limit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull List<PlayerJobState> getArchivedPlayerJobStates(@NotNull UUID playerId) {
      throw new UnsupportedOperationException();
    }
  }
}
