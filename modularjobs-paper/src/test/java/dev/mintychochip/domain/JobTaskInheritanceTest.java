package dev.mintychochip.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mintychochip.Job;
import dev.mintychochip.JobKey;
import dev.mintychochip.JobNode;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.JobTask;
import dev.mintychochip.LevelingCurve;
import dev.mintychochip.PayableCurve;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

class JobTaskInheritanceTest {

  @Test
  void nearestChildReplacesWholeMatchingTaskAndKeepsNonConflictingAncestors() {
    JobKey jobKey = new JobKey(Key.key("modularjobs", "miner"));
    TestNode root = node(jobKey, "miner", null);
    TestNode child = node(jobKey, "prospector", root.nodeKey());
    Job job = new TestJob(jobKey, root, Map.of(root.nodeKey(), root, child.nodeKey(), child));
    Key action = Key.key("modularjobs", "break");
    Key sharedContext = Key.key("minecraft", "diamond_ore");
    JobTask rootShared = new JobTask(root.nodeKey(), action, sharedContext, List.of());
    JobTask rootOnly =
        new JobTask(root.nodeKey(), action, Key.key("minecraft", "coal_ore"), List.of());
    JobTask childReplacement = new JobTask(child.nodeKey(), action, sharedContext, List.of());
    Map<JobNodeKey, List<JobTask>> definitions =
        Map.of(
            root.nodeKey(), List.of(rootShared, rootOnly),
            child.nodeKey(), List.of(childReplacement));

    List<JobTask> resolved =
        JobServiceImpl.resolveInheritedTasks(
            job, child.nodeKey(), nodeKey -> definitions.getOrDefault(nodeKey, List.of()));

    assertEquals(List.of(childReplacement, rootOnly), resolved);
  }

  private static @NotNull TestNode node(
      @NotNull JobKey jobKey, @NotNull String value, @Nullable JobNodeKey parentKey) {
    return new TestNode(
        jobKey,
        new JobNodeKey(Key.key("modularjobs", value)),
        parentKey,
        Component.text(value),
        Component.empty());
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
      return parameters -> BigDecimal.valueOf(parameters.level());
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
}
