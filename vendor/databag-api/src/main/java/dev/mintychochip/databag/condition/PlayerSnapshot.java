package dev.mintychochip.databag.condition;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable player-only state composed with its entity state.
 *
 * @param entity shared living-entity snapshot
 * @param gameMode player game mode, or {@code null}
 * @param hunger food level, or {@code null}
 * @param experience XP progress, or {@code null}
 * @param level XP level, or {@code null}
 * @param absorption absorption amount, or {@code null}
 * @param airRemaining remaining air ticks, or {@code null}
 * @param jobKeys attached job keys
 * @param scores scoreboard values
 */
public record PlayerSnapshot(
    EntitySnapshot entity,
    @Nullable String gameMode,
    @Nullable Double hunger,
    @Nullable Double experience,
    @Nullable Double level,
    @Nullable Double absorption,
    @Nullable Double airRemaining,
    Set<String> jobKeys,
    Map<String, Integer> scores) implements SubjectSnapshot {

  public PlayerSnapshot {
    Objects.requireNonNull(entity, "entity");
    jobKeys = Set.copyOf(jobKeys == null ? Set.of() : jobKeys);
    scores = Map.copyOf(scores == null ? Map.of() : scores);
  }
}
