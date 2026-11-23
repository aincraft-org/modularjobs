package dev.mintychochip.domain;

import dev.mintychochip.Job;
import dev.mintychochip.JobKey;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.LevelingCurve;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.domain.model.PlayerJobStateRecord;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Immutable player progress identified by tree and active-node keys. */
final class PlayerJobStateImpl implements PlayerJobState {

  private final UUID playerId;
  private final JobKey jobKey;
  private final JobNodeKey currentNodeKey;
  private final BigDecimal experience;
  private final int maxLevel;
  private final LevelingCurve levelingCurve;
  private final int level;

  /** Creates state at the job tree's root node. */
  PlayerJobStateImpl(@NotNull UUID playerId, @NotNull Job job, @NotNull BigDecimal experience) {
    this(playerId, job, job.rootNode().nodeKey(), experience);
  }

  /** Creates state at a validated node without retaining the job aggregate. */
  PlayerJobStateImpl(
      @NotNull UUID playerId,
      @NotNull Job job,
      @NotNull JobNodeKey currentNodeKey,
      @NotNull BigDecimal experience) {
    this(
        playerId,
        Objects.requireNonNull(job, "job").jobKey(),
        currentNodeKey,
        experience,
        job.maxLevel(),
        job.levelingCurve());
    if (!belongsTo(job)) {
      throw new IllegalArgumentException("Current node does not belong to the job tree");
    }
  }

  private PlayerJobStateImpl(
      @NotNull UUID playerId,
      @NotNull JobKey jobKey,
      @NotNull JobNodeKey currentNodeKey,
      @NotNull BigDecimal experience,
      int maxLevel,
      @NotNull LevelingCurve levelingCurve) {
    this.playerId = Objects.requireNonNull(playerId, "playerId");
    this.jobKey = Objects.requireNonNull(jobKey, "jobKey");
    this.currentNodeKey = Objects.requireNonNull(currentNodeKey, "currentNodeKey");
    this.experience = Objects.requireNonNull(experience, "experience");
    this.maxLevel = maxLevel;
    this.levelingCurve = Objects.requireNonNull(levelingCurve, "levelingCurve");
    this.level = LevelingCurve.levelForExperience(levelingCurve, maxLevel, experience);
  }

  /** Returns state with the given experience; returns {@code this} when unchanged. */
  @Override
  @Contract(pure = true)
  public @NotNull PlayerJobState withExperience(@NotNull BigDecimal experience) {
    if (this.experience.compareTo(Objects.requireNonNull(experience, "experience")) == 0) {
      return this;
    }
    return new PlayerJobStateImpl(
        playerId, jobKey, currentNodeKey, experience, maxLevel, levelingCurve);
  }

  @Override
  @Contract(pure = true)
  public @NotNull BigDecimal experienceForLevel(int level) {
    return levelingCurve.evaluate(new LevelingCurve.Parameters(level));
  }

  @Override
  @Contract(pure = true)
  public @NotNull JobKey jobKey() {
    return jobKey;
  }

  @Override
  @Contract(pure = true)
  public @NotNull JobNodeKey currentNodeKey() {
    return currentNodeKey;
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
  @Contract(pure = true)
  public int level() {
    return level;
  }

  @Override
  @Contract(pure = true)
  public @NotNull String toString() {
    return "PlayerJobStateImpl["
        + "player="
        + playerId
        + ", job="
        + jobKey.asString()
        + ", currentNode="
        + currentNodeKey.asString()
        + ", experience="
        + experience
        + ", level="
        + level
        + "]";
  }

  /** Reconstructs state after validating its keys against the supplied aggregate. */
  @Contract(pure = true)
  static @NotNull PlayerJobStateImpl fromRecord(
      @NotNull PlayerJobStateRecord record, @NotNull Job job) {
    Objects.requireNonNull(record, "record");
    Objects.requireNonNull(job, "job");
    if (!job.jobKey().asString().equals(record.jobKey())) {
      throw new IllegalArgumentException("Persisted state belongs to a different job tree");
    }
    JobNodeKey currentNodeKey = new JobNodeKey(Key.key(record.currentNodeKey()));
    if (job.node(currentNodeKey) == null) {
      throw new IllegalArgumentException(
          "Persisted state references unknown job node: " + record.currentNodeKey());
    }
    return new PlayerJobStateImpl(
        UUID.fromString(record.playerId()), job, currentNodeKey, record.experience());
  }
}
