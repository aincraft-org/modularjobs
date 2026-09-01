package dev.mintychochip.payment;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Facade over {@link MobDamageTrackerStore} for tracking damage dealt to an entity and retrieving
 * the final contribution breakdown when tracking ends.
 */
public final class MobDamageTracker {

  private final MobDamageTrackerStore store;

  /** Creates a tracker backed by the given contribution store. */
  public MobDamageTracker(@NotNull MobDamageTrackerStore store) {
    this.store = store;
  }

  /** Stops tracking {@code entity} and returns its accumulated {@link DamageContribution}. */
  public @NotNull DamageContribution endTracking(@NotNull Entity entity) {
    return store.removeContribution(entity);
  }

  /**
   * Reports whether damage on {@code entity} is currently being tracked.
   *
   * @return true when damage on {@code entity} is currently being tracked
   */
  public boolean isTracking(@NotNull Entity entity) {
    return store.hasContribution(entity);
  }
}
