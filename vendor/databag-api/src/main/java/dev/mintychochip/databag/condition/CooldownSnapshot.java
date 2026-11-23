package dev.mintychochip.databag.condition;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

/**
 * Snapshot of a single active cooldown. {@code source} distinguishes Paper
 * material cooldowns from custom registered cooldowns; remaining ticks is
 * the authoritative countdown.
 *
 * @param key the cooldown key
 * @param source the cooldown source identifier
 * @param remainingTicks ticks remaining on the cooldown
 */
public record CooldownSnapshot(Key key, String source, int remainingTicks) {

  /**
   * Validates required fields and rejects negative remaining ticks.
   */
  public CooldownSnapshot {
    java.util.Objects.requireNonNull(key, "key");
    java.util.Objects.requireNonNull(source, "source");
    if (remainingTicks < 0) {
      throw new IllegalArgumentException("remainingTicks must be >= 0");
    }
  }

  /**
   * Returns whether the cooldown still has time remaining.
   *
   * @return {@code true} when {@link #remainingTicks()} is greater than zero
   */
  public boolean isActive() {
    return remainingTicks > 0;
  }
}
