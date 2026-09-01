package dev.mintychochip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes a complete rooted job tree and its tree-wide progression and payout rules.
 *
 * <p>A job is identified by its typed {@linkplain #jobKey() job key}; individual root and
 * specialization definitions are exposed as {@link JobNode} values. {@link #key()} exposes the
 * underlying Adventure key for registry interoperability.
 */
public interface Job extends Keyed {

  /** Returns this job's typed identity. */
  @Contract(pure = true)
  @NotNull
  JobKey jobKey();

  /** Returns the underlying Adventure key for {@link Keyed} interoperability. */
  @Override
  @Contract(pure = true)
  default @NotNull Key key() {
    return jobKey().key();
  }

  /** Returns the root node where every player enters this job tree. */
  @Contract(pure = true)
  @NotNull
  JobNode rootNode();

  /** Returns every node in this tree, keyed by typed node identity. */
  @Contract(pure = true)
  @NotNull
  Map<JobNodeKey, JobNode> nodes();

  /** Returns a node in this tree, or {@code null} when the key is unknown. */
  @Contract(pure = true)
  default @Nullable JobNode node(@NotNull JobNodeKey nodeKey) {
    return nodes().get(Objects.requireNonNull(nodeKey, "nodeKey"));
  }

  /**
   * Returns the root-to-node path for the requested node.
   *
   * @throws IllegalArgumentException if the node is unknown or does not descend from this root
   * @throws IllegalStateException if the tree contains a parent cycle or dangling parent
   */
  @Contract(pure = true)
  default @NotNull List<JobNode> pathTo(@NotNull JobNodeKey nodeKey) {
    JobNode current = node(Objects.requireNonNull(nodeKey, "nodeKey"));
    if (current == null) {
      throw new IllegalArgumentException("Unknown job node: " + nodeKey.asString());
    }

    List<JobNode> reversed = new ArrayList<>();
    Set<JobNodeKey> visited = new HashSet<>();
    while (current != null) {
      if (!visited.add(current.nodeKey())) {
        throw new IllegalStateException(
            "Job tree contains a cycle at " + current.nodeKey().asString());
      }
      reversed.add(current);
      JobNodeKey parentKey = current.parentKey();
      if (parentKey == null) {
        break;
      }
      current = node(parentKey);
      if (current == null) {
        throw new IllegalStateException("Unknown parent job node: " + parentKey.asString());
      }
    }
    Collections.reverse(reversed);
    if (reversed.isEmpty() || !reversed.get(0).nodeKey().equals(rootNode().nodeKey())) {
      throw new IllegalArgumentException("Job node does not descend from this tree's root");
    }
    return List.copyOf(reversed);
  }

  /** Returns the root node's display name. */
  @Contract(pure = true)
  default @NotNull Component displayName() {
    return rootNode().displayName();
  }

  /** Returns the root node's plain-text name. */
  @Contract(pure = true)
  default @NotNull String getPlainName() {
    return rootNode().getPlainName();
  }

  /** Returns the root node's description. */
  @Contract(pure = true)
  default @NotNull Component description() {
    return rootNode().description();
  }

  /** Returns the curve used to calculate experience thresholds. */
  @Contract(pure = true)
  @NotNull
  LevelingCurve levelingCurve();

  /**
   * Returns the configured payout curves keyed by payable type.
   *
   * <p>The returned map describes the job configuration and should be treated as read-only.
   */
  @Contract(pure = true)
  @NotNull
  Map<Key, PayableCurve> payableCurves();

  /** Max level. */
  @Contract(pure = true)
  int maxLevel();
}
