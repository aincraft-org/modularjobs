package dev.mintychochip.domain;

import dev.mintychochip.Job;
import dev.mintychochip.JobKey;
import dev.mintychochip.JobNode;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.LevelingCurve;
import dev.mintychochip.PayableCurve;
import dev.mintychochip.domain.model.JobRecord;
import dev.mintychochip.domain.model.JobTreeRecord;
import dev.mintychochip.math.ExpressionCurves;
import dev.mintychochip.util.KeyUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.key.Key;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Immutable rooted job tree backed by flat configuration records. */
record JobImpl(
    @NotNull JobKey jobKey,
    @NotNull JobNode rootNode,
    @NotNull Map<JobNodeKey, JobNode> nodes,
    @NotNull JobTreeRecord sourceRecord,
    int maxLevel,
    @NotNull LevelingCurve levelingCurve,
    @NotNull Map<Key, PayableCurve> payableCurves)
    implements Job {

  JobImpl {
    Objects.requireNonNull(jobKey, "jobKey");
    Objects.requireNonNull(rootNode, "rootNode");
    nodes = Map.copyOf(nodes);
    Objects.requireNonNull(sourceRecord, "sourceRecord");
    Objects.requireNonNull(levelingCurve, "levelingCurve");
    payableCurves = Map.copyOf(payableCurves);

    if (!jobKey.asString().equals(sourceRecord.root().jobKey())) {
      throw new IllegalArgumentException("Source tree record does not match the job key");
    }

    if (!jobKey.equals(rootNode.jobKey())) {
      throw new IllegalArgumentException("Root node belongs to a different job tree");
    }
    if (rootNode.parentKey() != null) {
      throw new IllegalArgumentException("Root job node cannot have a parent");
    }
    if (!rootNode.equals(nodes.get(rootNode.nodeKey()))) {
      throw new IllegalArgumentException("Root node must be present in the job node map");
    }
    for (JobNode node : nodes.values()) {
      if (!jobKey.equals(node.jobKey())) {
        throw new IllegalArgumentException(
            "Node " + node.nodeKey().asString() + " belongs to a different job tree");
      }
      JobNodeKey parentKey = node.parentKey();
      if (parentKey == null && !node.equals(rootNode)) {
        throw new IllegalArgumentException(
            "Only the root job node may omit a parent: " + node.nodeKey().asString());
      }
      if (parentKey != null && !nodes.containsKey(parentKey)) {
        throw new IllegalArgumentException("Unknown parent job node: " + parentKey.asString());
      }
      Set<JobNodeKey> visited = new HashSet<>();
      JobNode current = node;
      while (current.parentKey() != null) {
        if (!visited.add(current.nodeKey())) {
          throw new IllegalArgumentException(
              "Job tree contains a cycle at " + current.nodeKey().asString());
        }
        current = nodes.get(current.parentKey());
      }
      if (!current.equals(rootNode)) {
        throw new IllegalArgumentException(
            "Job node does not descend from the tree root: " + node.nodeKey().asString());
      }
    }
    Set<String> runtimeNodeKeys =
        nodes.keySet().stream().map(JobNodeKey::asString).collect(Collectors.toSet());
    if (!runtimeNodeKeys.equals(sourceRecord.nodes().keySet())) {
      throw new IllegalArgumentException("Runtime nodes do not match source tree records");
    }
  }

  /** Returns the exact tree records used to construct this aggregate. */
  @Contract(pure = true)
  @NotNull
  JobTreeRecord toRecord() {
    return sourceRecord;
  }

  /** Reconstructs one complete job tree from its root and descendant records. */
  @Contract(pure = true)
  static @NotNull JobImpl fromRecords(
      @NotNull JobRecord rootRecord, @NotNull List<JobRecord> treeRecords, @NotNull Plugin plugin) {
    if (rootRecord.parentKey() != null) {
      throw new IllegalArgumentException("Job tree root cannot reference a parent");
    }

    JobKey jobKey = new JobKey(KeyUtils.parseKey(plugin, rootRecord.jobKey()));
    Map<String, JobRecord> sourceNodes = new HashMap<>();
    Map<JobNodeKey, JobNode> nodes = new HashMap<>();
    for (JobRecord record : treeRecords) {
      JobNode node = JobNodeImpl.fromRecord(jobKey, record, plugin);
      JobNode previous = nodes.put(node.nodeKey(), node);
      if (previous != null) {
        throw new IllegalArgumentException("Duplicate job node: " + node.nodeKey().asString());
      }
      sourceNodes.put(record.jobKey(), record);
    }

    JobNodeKey rootNodeKey = new JobNodeKey(KeyUtils.parseKey(plugin, rootRecord.jobKey()));
    JobNode rootNode = nodes.get(rootNodeKey);
    if (rootNode == null) {
      throw new IllegalArgumentException("Job tree does not contain its root node");
    }

    JobTreeRecord sourceRecord = new JobTreeRecord(rootRecord, sourceNodes);
    return new JobImpl(
        jobKey,
        rootNode,
        nodes,
        sourceRecord,
        rootRecord.maxLevel(),
        ExpressionCurves.levelingCurve(rootRecord.levellingCurve()),
        parsePayableCurves(rootRecord, plugin));
  }

  private static @NotNull Map<Key, PayableCurve> parsePayableCurves(
      @NotNull JobRecord record, @NotNull Plugin plugin) {
    Map<Key, PayableCurve> curves = new HashMap<>();
    for (Map.Entry<String, String> entry : record.payableCurves().entrySet()) {
      Key payableTypeKey = KeyUtils.parseKey(plugin, entry.getKey());
      curves.put(payableTypeKey, ExpressionCurves.payableCurve(entry.getValue()));
    }
    return curves;
  }
}
