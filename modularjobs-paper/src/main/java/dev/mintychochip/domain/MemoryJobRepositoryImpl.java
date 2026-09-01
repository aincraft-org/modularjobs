package dev.mintychochip.domain;

import dev.mintychochip.config.YamlConfiguration;
import dev.mintychochip.domain.model.JobRecord;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * In-memory {@link dev.mintychochip.domain.model.JobRecord} repository backed by a keyed map.
 * Records are keyed by their {@code jobKey}; lookups are O(1) and never hit a database.
 *
 * @see #load(String)
 */
public final class MemoryJobRepositoryImpl {

  private final Map<String, JobRecord> records;
  private final Map<String, String> rootKeyByNode;
  private final Map<String, List<JobRecord>> treesByRoot;

  MemoryJobRepositoryImpl(@NotNull Map<String, JobRecord> records) {
    this.records = Map.copyOf(records);
    validateLineage(this.records);

    Map<String, String> roots = new HashMap<>();
    Map<String, List<JobRecord>> trees = new HashMap<>();
    for (JobRecord record : this.records.values()) {
      JobRecord root = findRoot(record, this.records);
      roots.put(record.jobKey(), root.jobKey());
      trees.computeIfAbsent(root.jobKey(), ignored -> new ArrayList<>()).add(record);
    }
    trees.replaceAll(
        (ignored, tree) -> tree.stream().sorted(Comparator.comparing(JobRecord::jobKey)).toList());
    this.rootKeyByNode = Map.copyOf(roots);
    this.treesByRoot = Map.copyOf(trees);
  }

  /**
   * Returns every root job record in deterministic key order.
   *
   * @return immutable root records, one per job tree
   */
  @Contract(pure = true)
  public @NotNull List<JobRecord> getJobs() {
    return treesByRoot.keySet().stream().sorted().map(records::get).toList();
  }

  /**
   * Loads the record for the given job key.
   *
   * @param jobKey the job key to look up
   * @return the matching record, or {@code null} if none is present
   */
  @Contract(pure = true)
  public @Nullable JobRecord load(@NotNull String jobKey) {
    return records.get(jobKey);
  }

  /** Returns the root record owning the requested node, or {@code null} when unknown. */
  @Contract(pure = true)
  public @Nullable JobRecord rootFor(@NotNull String nodeKey) {
    String rootKey = rootKeyByNode.get(nodeKey);
    return rootKey == null ? null : records.get(rootKey);
  }

  /** Returns every node record in the tree containing the requested key. */
  @Contract(pure = true)
  public @NotNull List<JobRecord> loadTree(@NotNull String jobOrNodeKey) {
    String rootKey = rootKeyByNode.get(jobOrNodeKey);
    return rootKey == null ? List.of() : treesByRoot.get(rootKey);
  }

  private static @NotNull JobRecord findRoot(
      @NotNull JobRecord record, @NotNull Map<String, JobRecord> records) {
    JobRecord current = record;
    while (current.parentKey() != null) {
      current = records.get(current.parentKey());
    }
    return current;
  }

  private static void validateLineage(@NotNull Map<String, JobRecord> records) {
    Map<String, VisitState> states = new HashMap<>();
    for (JobRecord record : records.values()) {
      validateLineage(record, records, states);
    }
  }

  private static void validateLineage(
      @NotNull JobRecord record,
      @NotNull Map<String, JobRecord> records,
      @NotNull Map<String, VisitState> states) {
    VisitState state = states.get(record.jobKey());
    if (state == VisitState.VISITED) {
      return;
    }
    if (state == VisitState.VISITING) {
      throw new IllegalArgumentException(
          "Job lineage contains a cycle involving " + record.jobKey());
    }

    states.put(record.jobKey(), VisitState.VISITING);
    String parentKey = record.parentKey();
    if (parentKey != null) {
      if (parentKey.equals(record.jobKey())) {
        throw new IllegalArgumentException("Job cannot be its own parent: " + record.jobKey());
      }
      JobRecord parent = records.get(parentKey);
      if (parent == null) {
        throw new IllegalArgumentException(
            "Job " + record.jobKey() + " references unknown parent " + parentKey);
      }
      validateLineage(parent, records, states);
    }
    states.put(record.jobKey(), VisitState.VISITED);
  }

  private enum VisitState {
    VISITING,
    VISITED
  }

  /** Loads flat YAML node sections and assigns each root's rules to its complete tree. */
  static final class YamlRecordLoader {

    @Contract(pure = true)
    @NotNull
    Map<String, JobRecord> load(@NotNull YamlConfiguration configuration) {
      Map<String, JobRecord> nodes = new HashMap<>();
      for (String nodeName : configuration.getKeys(false)) {
        ConfigurationSection section = configuration.getConfigurationSection(nodeName);
        if (section == null) {
          continue;
        }
        String displayName = section.getString("display-name");
        if (displayName == null) {
          continue;
        }
        String parentKey = namespacedParent(section.getString("parent"));
        boolean root = parentKey == null;
        if (!root
            && (section.contains("max-level")
                || section.contains("leveling-curve")
                || section.contains("payable-curves"))) {
          throw new IllegalArgumentException(
              "Tree-wide rules may only be declared on root job node modularjobs:" + nodeName);
        }

        int maxLevel = root ? section.getInt("max-level", 1) : 0;
        String levelingCurve = "";
        if (root) {
          String configuredCurve = section.getString("leveling-curve");
          if (configuredCurve == null) {
            throw new IllegalArgumentException(
                "Root job node modularjobs:" + nodeName + " requires leveling-curve");
          }
          levelingCurve = configuredCurve;
        }
        Map<String, String> payableCurves = root ? payableCurves(section) : Map.of();
        String nodeKey = "modularjobs:" + nodeName;
        nodes.put(
            nodeKey,
            new JobRecord(
                nodeKey,
                displayName,
                section.getString("description", null),
                maxLevel,
                levelingCurve,
                payableCurves,
                parentKey));
      }

      validateLineage(nodes);
      Map<String, JobRecord> normalized = new HashMap<>();
      for (JobRecord node : nodes.values()) {
        JobRecord root = findRoot(node, nodes);
        normalized.put(
            node.jobKey(),
            new JobRecord(
                node.jobKey(),
                node.displayName(),
                node.description(),
                root.maxLevel(),
                root.levellingCurve(),
                root.payableCurves(),
                node.parentKey()));
      }
      return Map.copyOf(normalized);
    }

    private static @NotNull Map<String, String> payableCurves(
        @NotNull ConfigurationSection section) {
      ConfigurationSection curvesSection = section.getConfigurationSection("payable-curves");
      if (curvesSection == null) {
        return Map.of();
      }
      Map<String, String> curves = new HashMap<>();
      for (String curveKey : curvesSection.getKeys(false)) {
        String curve = curvesSection.getString(curveKey);
        if (curve != null) {
          curves.put(curveKey, curve);
        }
      }
      return Map.copyOf(curves);
    }

    private static @Nullable String namespacedParent(@Nullable String parentKey) {
      if (parentKey == null) {
        return null;
      }
      return parentKey.contains(":") ? parentKey : "modularjobs:" + parentKey;
    }
  }
}
