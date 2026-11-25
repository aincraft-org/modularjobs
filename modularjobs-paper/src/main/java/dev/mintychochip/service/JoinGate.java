package dev.mintychochip.service;

import dev.mintychochip.Job;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.config.ProgressionLimitsConfig;
import dev.mintychochip.config.ProgressionLimitsConfig.SlotGroup;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Enforces join eligibility for {@code /jobs join}, {@code /jobs swap} and every browse renderer's
 * join action.
 *
 * <p>Rules, first violation wins: with slot groups configured, the job must belong to a group
 * ({@link JoinResult#NO_SLOT}) that still has a free slot ({@link JoinResult#SLOTS_FULL}); the
 * player needs {@code jobs.join.<job>}; with world join restriction on, a job cannot be joined
 * while in a world that pays nothing (the payout disabled-worlds set).
 */
public final class JoinGate {

  /** Outcome of a join-eligibility check. */
  public enum JoinResult {
    ALLOWED,
    SLOTS_FULL,
    NO_SLOT,
    PERMISSION_DENIED,
    WORLD_DENIED
  }

  private final ProgressionLimitsConfig config;
  private final Set<String> disabledWorlds;

  /**
   * Creates a join eligibility gate using slot groups and world restrictions.
   *
   * @param config join-eligibility knobs
   * @param disabledWorlds case-insensitive payout disabled-world names (from {@code
   *     PaymentSettings.disabledWorlds()})
   */
  public JoinGate(@NotNull ProgressionLimitsConfig config, @NotNull Set<String> disabledWorlds) {
    this.config = config;
    this.disabledWorlds = disabledWorlds;
  }

  /** Returns the slot group that holds {@code job}, or {@code null} when none does. */
  public @Nullable SlotGroup slotGroup(@NotNull Job job) {
    return config.groupOf(job.key().value());
  }

  /** Counts the active jobs in {@code current} that occupy a slot of {@code group}. */
  public int used(@NotNull SlotGroup group, @NotNull List<PlayerJobState> current) {
    int used = 0;
    for (PlayerJobState state : current) {
      if (group.jobs().contains(state.jobKey().key().value().toLowerCase(Locale.ROOT))) {
        used++;
      }
    }
    return used;
  }

  /**
   * Checks whether {@code player} may join {@code job} given their current jobs.
   *
   * @param player the joining player
   * @param job the job being joined
   * @param current the player's current (non-archived) job states
   * @return the first violated rule, or {@link JoinResult#ALLOWED}
   */
  public @NotNull JoinResult canJoin(
      @NotNull Player player, @NotNull Job job, @NotNull List<PlayerJobState> current) {
    if (config.hasSlotGroups()) {
      SlotGroup group = slotGroup(job);
      if (group == null) {
        return JoinResult.NO_SLOT;
      }
      if (used(group, current) >= group.slots()) {
        return JoinResult.SLOTS_FULL;
      }
    }
    if (!player.hasPermission("jobs.join." + job.getPlainName().toLowerCase(Locale.ROOT))) {
      return JoinResult.PERMISSION_DENIED;
    }
    if (config.worldJoinRestriction()
        && player.getWorld() != null
        && disabledWorlds.contains(player.getWorld().getName().toLowerCase(Locale.ROOT))) {
      return JoinResult.WORLD_DENIED;
    }
    return JoinResult.ALLOWED;
  }

  /**
   * Checks whether {@code player} may put {@code from} on hold and make {@code to} active: the join
   * rules for {@code to} with {@code from}'s slot already free.
   *
   * @param current the player's current (non-archived) job states, {@code from} included
   */
  public @NotNull JoinResult canSwap(
      @NotNull Player player,
      @NotNull Job from,
      @NotNull Job to,
      @NotNull List<PlayerJobState> current) {
    List<PlayerJobState> remaining =
        current.stream().filter(state -> !state.jobKey().equals(from.jobKey())).toList();
    return canJoin(player, to, remaining);
  }
}
