package dev.mintychochip.databag.condition;

import java.util.Objects;
import java.util.random.RandomGenerator;
import org.jetbrains.annotations.Nullable;

/** Immutable references to execution-only loot inputs. */
public record LootSnapshot(
    @Nullable ItemSubject tool,
    @Nullable Double explosionRadius,
    @Nullable RandomGenerator random) {

  public LootSnapshot {
    if (explosionRadius != null && explosionRadius < 0) {
      throw new IllegalArgumentException("explosionRadius must be >= 0");
    }
  }

  public LootSnapshot withRandom(RandomGenerator value) {
    return new LootSnapshot(tool, explosionRadius, Objects.requireNonNull(value, "value"));
  }
}
