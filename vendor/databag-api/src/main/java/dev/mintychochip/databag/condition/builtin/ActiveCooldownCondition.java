package dev.mintychochip.databag.condition.builtin;

import dev.mintychochip.databag.condition.Condition;
import dev.mintychochip.databag.condition.ConditionContext;
import dev.mintychochip.databag.condition.CooldownSnapshot;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

/**
 * Matches a cooldown snapshot against the catalog contract: required key,
 * optional source filter, optional active flag, and optional minimum
 * remaining ticks.
 *
 * <p>Absence semantics: Paper only records positive cooldowns, so a key with
 * no snapshot is by definition not cooling down. An {@code active:false}
 * query (with no minimum-ticks constraint) therefore succeeds when no
 * snapshot matches key/source. All other queries fail closed on absence.
 *
 * @param key the cooldown key to match
 * @param source the cooldown source filter, or {@code null} to ignore
 * @param active the required active flag, or {@code null} to ignore
 * @param minimumRemainingTicks the minimum remaining ticks, or {@code null} to ignore
 */
public record ActiveCooldownCondition(
    Key key,
    @Nullable String source,
    @Nullable Boolean active,
    @Nullable Integer minimumRemainingTicks) implements Condition {

  /** Validates the key reference and minimum tick bound. */
  public ActiveCooldownCondition {
    java.util.Objects.requireNonNull(key, "key");
    if (minimumRemainingTicks != null && minimumRemainingTicks < 0) {
      throw new IllegalArgumentException("minimumRemainingTicks must be >= 0");
    }
  }

  @Override
  public boolean test(ConditionContext context) {
    java.util.Set<CooldownSnapshot> snapshots = context.activeCooldowns();
    if (snapshots == null) {
      return active == Boolean.FALSE && minimumRemainingTicks == null;
    }
    boolean sawMatchingKey = false;
    for (CooldownSnapshot snapshot : snapshots) {
      if (BiomeCondition.keysEqual(key, snapshot.key()) == false) {
        continue;
      }
      sawMatchingKey = true;
      if (source != null && source.equals(snapshot.source()) == false) {
        continue;
      }
      if (active != null && active.booleanValue() != snapshot.isActive()) {
        continue;
      }
      if (minimumRemainingTicks != null
          && snapshot.remainingTicks() < minimumRemainingTicks.intValue()) {
        continue;
      }
      return true;
    }
    // active=false with no snapshot means the key is not cooling down.
    return active == Boolean.FALSE && minimumRemainingTicks == null && sawMatchingKey == false;
  }
}
