package dev.mintychochip;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Immutable, renderer-neutral projection of one active job progression. */
public record JobStats(
    @NotNull JobKey jobKey,
    @NotNull Component displayName,
    int level,
    int maxLevel,
    @NotNull BigDecimal totalExperience,
    @NotNull BigDecimal levelExperience,
    @NotNull BigDecimal requiredExperience,
    @NotNull BigDecimal remainingExperience,
    @NotNull BigDecimal progressPercentage,
    boolean atMaxLevel) {

  /** Calculates display-safe progression values from a job and its player state. */
  @Contract(pure = true)
  public static @NotNull JobStats from(@NotNull Job job, @NotNull PlayerJobState state) {
    Objects.requireNonNull(job, "job");
    Objects.requireNonNull(state, "state");

    int level = state.level();
    int maxLevel = job.maxLevel();
    BigDecimal total = state.experience();
    BigDecimal currentThreshold = state.experienceForLevel(level);
    BigDecimal earned = total.subtract(currentThreshold).max(BigDecimal.ZERO);
    boolean atMax = level >= maxLevel;

    if (atMax) {
      return new JobStats(
          job.jobKey(),
          job.displayName(),
          level,
          maxLevel,
          total,
          earned,
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          BigDecimal.valueOf(100).setScale(1),
          true);
    }

    BigDecimal required = state.experienceForLevel(level + 1).subtract(currentThreshold);
    if (required.signum() <= 0) {
      throw new IllegalArgumentException("Experience thresholds must increase between levels");
    }
    BigDecimal clampedEarned = earned.min(required);
    BigDecimal remaining = required.subtract(clampedEarned);
    BigDecimal percentage =
        clampedEarned.multiply(BigDecimal.valueOf(100)).divide(required, 1, RoundingMode.HALF_UP);

    return new JobStats(
        job.jobKey(),
        job.displayName(),
        level,
        maxLevel,
        total,
        clampedEarned,
        required,
        remaining,
        percentage,
        false);
  }

  /** Returns a fixed-width, plain-text progress bar suitable for chat and placeholders. */
  @Contract(pure = true)
  public @NotNull String progressBar(int segments) {
    if (segments <= 0) {
      throw new IllegalArgumentException("segments must be positive");
    }
    int filled =
        progressPercentage
            .multiply(BigDecimal.valueOf(segments))
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
            .intValueExact();
    filled = Math.max(0, Math.min(segments, filled));
    return "█".repeat(filled) + "░".repeat(segments - filled);
  }
}
