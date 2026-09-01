package dev.mintychochip.container.boost;

import dev.mintychochip.container.BoostSource;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData;
import dev.mintychochip.container.boost.BoostData.TimedBoostData;
import dev.mintychochip.container.boost.TimedBoostDataService.Target.GlobalTarget;
import dev.mintychochip.container.boost.TimedBoostDataService.Target.PlayerTarget;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Timed boost data service. */
public interface TimedBoostDataService {

  /** Target. */
  sealed interface Target permits GlobalTarget, PlayerTarget {

    /** Global target. */
    record GlobalTarget() implements Target {}

    /** Player target. */
    record PlayerTarget(@NotNull UUID playerId) implements Target {}
  }

  /**
   * Active timed boost row. Expiry is computed from {@link #started} + {@link #duration}; expired
   * rows are deleted by {@link TimedBoostDataService#findApplicableBoosts(Target)}.
   */
  record ActiveBoostData(
      @NotNull String targetIdentifier,
      @NotNull String sourceIdentifier,
      @NotNull Instant started,
      @Nullable Duration duration,
      @NotNull BoostSource boostSource) {

    @Contract(pure = true)
    public boolean isExpired() {
      return isExpired(System.currentTimeMillis());
    }

    /** Expiry check against an explicit clock (for tests / deterministic cleanup). */
    @Contract(pure = true)
    public boolean isExpired(long nowMillis) {
      if (duration == null) {
        return false; // Permanent boost
      }
      long expiresAt = started.toEpochMilli() + duration.toMillis();
      return nowMillis > expiresAt;
    }
  }

  /** Find applicable boosts. */
  @NotNull
  List<ActiveBoostData> findApplicableBoosts(@NotNull Target target);

  /** Find boosts. */
  @NotNull
  List<ActiveBoostData> findBoosts(@NotNull Target target);

  /** Add data. */
  <T extends TimedBoostData & SerializableBoostData> void addData(
      @NotNull T data, @NotNull Target target);

  /**
   * Remove a timed boost from a target.
   *
   * @param target the target (player or global)
   * @param sourceIdentifier the boost source key string
   * @return true if a boost was removed, false otherwise
   */
  boolean removeBoost(@NotNull Target target, @NotNull String sourceIdentifier);
}
