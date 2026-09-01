package dev.mintychochip.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Persisted state for one player within one complete job tree.
 *
 * @param playerId the owning player id
 * @param jobKey the root job-tree key
 * @param currentNodeKey the active node within the tree
 * @param experience the accumulated tree-wide experience
 */
public record PlayerJobStateRecord(
    @NotNull String playerId,
    @NotNull String jobKey,
    @NotNull String currentNodeKey,
    @NotNull BigDecimal experience) {

  /** Rejects incomplete persisted state. */
  public PlayerJobStateRecord {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(jobKey, "jobKey");
    Objects.requireNonNull(currentNodeKey, "currentNodeKey");
    Objects.requireNonNull(experience, "experience");
  }
}
