package dev.mintychochip.domain;

import dev.mintychochip.Job;
import dev.mintychochip.JobNode;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.LevelingCurve.Parameters;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.domain.model.PlayerJobStateRecord;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable snapshot of one player's state within one complete job tree.
 *
 * <p>The level is derived from tree-wide experience. Changing {@link #currentNode()} preserves that
 * progress and only changes the active specialization.
 */
final class PlayerJobStateImpl implements PlayerJobState {

  private final Job job;
  private final JobNode currentNode;
  private final UUID playerId;
  private final BigDecimal experience;
  private final int level;

  /** Creates state at the job tree's root node. */
  PlayerJobStateImpl(@NotNull UUID playerId, @NotNull Job job, @NotNull BigDecimal experience) {
    this(playerId, job, job.rootNode(), experience);
  }

  PlayerJobStateImpl(
      @NotNull UUID playerId,
      @NotNull Job job,
      @NotNull JobNode currentNode,
      @NotNull BigDecimal experience) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(job, "job");
    Objects.requireNonNull(currentNode, "currentNode");
    Objects.requireNonNull(experience, "experience");
    if (!job.jobKey().equals(currentNode.jobKey())
        || !currentNode.equals(job.node(currentNode.nodeKey()))) {
      throw new IllegalArgumentException("Current node does not belong to the job tree");
    }
    this.playerId = playerId;
    this.job = job;
    this.currentNode = currentNode;
    this.experience = experience;
    this.level = calculateCurrentLevel();
  }

  /** Returns state with the given experience; returns {@code this} when unchanged. */
  @Override
  @Contract(pure = true)
  public @NotNull PlayerJobState withExperience(@NotNull BigDecimal experience) {
    if (this.experience.compareTo(Objects.requireNonNull(experience, "experience")) == 0) {
      return this;
    }
    return new PlayerJobStateImpl(playerId, job, currentNode, experience);
  }

  @Override
  @Contract(pure = true)
  public @NotNull PlayerJobState withCurrentNode(@NotNull JobNodeKey nodeKey) {
    JobNode node = job.node(nodeKey);
    if (node == null) {
      throw new IllegalArgumentException("Unknown job node: " + nodeKey.asString());
    }
    if (currentNode.equals(node)) {
      return this;
    }
    return new PlayerJobStateImpl(playerId, job, node, experience);
  }

  @Override
  @Contract(pure = true)
  public @NotNull BigDecimal experienceForLevel(int level) {
    return job.levelingCurve().evaluate(new Parameters(level));
  }

  @Override
  @Contract(pure = true)
  public @NotNull Job job() {
    return job;
  }

  @Override
  @Contract(pure = true)
  public @NotNull JobNode currentNode() {
    return currentNode;
  }

  @Override
  @Contract(pure = true)
  public @NotNull UUID playerId() {
    return playerId;
  }

  @Override
  @Contract(pure = true)
  public @NotNull BigDecimal experience() {
    return experience;
  }

  @Override
  public int level() {
    return level;
  }

  /**
   * Binary-searches the level whose experience threshold is met by {@link #experience}.
   *
   * @return the derived current level, clamped to {@code [1, maxLevel]}
   */
  @Contract(pure = true)
  private int calculateCurrentLevel() {
    int maxLevel = job.maxLevel();
    if (maxLevel <= 0) {
      return 1;
    }

    int low = 1;
    int level = 1; // Start at level 1, upgrade if XP thresholds are met
    while (low <= maxLevel) {
      int mid = (low + maxLevel) >>> 1;
      BigDecimal requiredXpForLevel = job.levelingCurve().evaluate(new Parameters(mid));
      if (experience.compareTo(requiredXpForLevel) >= 0) {
        level = mid;
        low = mid + 1;
      } else {
        maxLevel = mid - 1;
      }
    }
    return level;
  }

  @Override
  @Contract(pure = true)
  public @NotNull String toString() {
    return "PlayerJobStateImpl["
        + "player="
        + playerId
        + ", job="
        + job.jobKey().asString()
        + ", currentNode="
        + currentNode.nodeKey().asString()
        + ", experience="
        + experience
        + ", level="
        + level()
        + "]";
  }

  /** Reconstructs state against the complete job aggregate supplied by the catalog. */
  @Contract(pure = true)
  static @NotNull PlayerJobStateImpl fromRecord(
      @NotNull PlayerJobStateRecord record, @NotNull Job job) {
    if (!job.jobKey().asString().equals(record.jobKey())) {
      throw new IllegalArgumentException("Persisted state belongs to a different job tree");
    }
    JobNode currentNode =
        job.node(new JobNodeKey(net.kyori.adventure.key.Key.key(record.currentNodeKey())));
    if (currentNode == null) {
      throw new IllegalArgumentException(
          "Persisted state references unknown job node: " + record.currentNodeKey());
    }
    return new PlayerJobStateImpl(
        UUID.fromString(record.playerId()), job, currentNode, record.experience());
  }
}
