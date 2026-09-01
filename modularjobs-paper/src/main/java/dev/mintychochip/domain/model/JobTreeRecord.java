package dev.mintychochip.domain.model;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Configuration records for one complete rooted job tree. */
public record JobTreeRecord(@NotNull JobRecord root, @NotNull Map<String, JobRecord> nodes) {

  /** Validates the root and stores an immutable node map. */
  public JobTreeRecord {
    Objects.requireNonNull(root, "root");
    nodes = Map.copyOf(nodes);
    if (root.parentKey() != null) {
      throw new IllegalArgumentException("Job tree root cannot reference a parent");
    }
    if (!root.equals(nodes.get(root.jobKey()))) {
      throw new IllegalArgumentException("Job tree records must contain their root");
    }
    for (Map.Entry<String, JobRecord> entry : nodes.entrySet()) {
      if (!entry.getKey().equals(entry.getValue().jobKey())) {
        throw new IllegalArgumentException("Job tree record key does not match its node");
      }
    }
  }

  /** Returns all node records in deterministic key order. */
  @Contract(pure = true)
  public @NotNull List<JobRecord> orderedNodes() {
    return nodes.values().stream().sorted(Comparator.comparing(JobRecord::jobKey)).toList();
  }
}
