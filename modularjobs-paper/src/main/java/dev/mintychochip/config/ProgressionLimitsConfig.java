package dev.mintychochip.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Join-eligibility knobs loaded from {@code config.yml} under {@code progression-limits}.
 *
 * <p>{@code slotGroups} name sets of jobs that share a slot count (for example {@code primary: 2},
 * {@code secondary: 1}). With no groups, a player may hold any number of jobs. With groups, every
 * job must belong to exactly one group; a job in no group cannot be joined. Jobs are named by their
 * root key value ({@code miner} for {@code modularjobs:miner}), as in {@code autoJoinJobs}.
 *
 * <p>{@code autoJoinJobs} are joined automatically on login when the player is not already in them.
 * {@code worldJoinRestriction} gates joins to the player's current world via the payout
 * disabled-worlds rule (a job cannot be joined while in a world that pays nothing).
 */
public record ProgressionLimitsConfig(
    @NotNull List<SlotGroup> slotGroups,
    @NotNull List<String> autoJoinJobs,
    boolean worldJoinRestriction) {

  /**
   * A named set of jobs that share {@code slots} active places.
   *
   * @param name group name as configured ({@code primary})
   * @param slots how many jobs of the group a player may hold active at once, at least 1
   * @param jobs lowercase root key values of the member jobs
   */
  public record SlotGroup(@NotNull String name, int slots, @NotNull Set<String> jobs) {

    /** Validates and freezes the member set. */
    public SlotGroup {
      if (slots < 1) {
        throw new IllegalArgumentException("slot group " + name + " needs slots >= 1");
      }
      jobs = Set.copyOf(jobs);
    }
  }

  /** Validates and freezes the lists; rejects a job named by two groups. */
  public ProgressionLimitsConfig {
    slotGroups = List.copyOf(slotGroups);
    autoJoinJobs = List.copyOf(autoJoinJobs);
    Map<String, String> owner = new HashMap<>();
    for (SlotGroup group : slotGroups) {
      for (String job : group.jobs()) {
        String previous = owner.putIfAbsent(job, group.name());
        if (previous != null) {
          throw new IllegalArgumentException(
              "job " + job + " is in slot groups " + previous + " and " + group.name());
        }
      }
    }
  }

  /** Defaults: no slot groups (unlimited), no auto-join, world restriction on. */
  @Contract(pure = true)
  public static @NotNull ProgressionLimitsConfig defaults() {
    return new ProgressionLimitsConfig(List.of(), List.of(), true);
  }

  /** Whether slot groups are configured; without them joins are unlimited. */
  @Contract(pure = true)
  public boolean hasSlotGroups() {
    return !slotGroups.isEmpty();
  }

  /**
   * Returns the group that holds {@code job}, or {@code null} when none does.
   *
   * @param job root key value of the job ({@code miner}); case-insensitive
   */
  @Contract(pure = true)
  public @Nullable SlotGroup groupOf(@NotNull String job) {
    String lower = job.toLowerCase(Locale.ROOT);
    for (SlotGroup group : slotGroups) {
      if (group.jobs().contains(lower)) {
        return group;
      }
    }
    return null;
  }

  /**
   * Builds a config from the raw {@code progression-limits} map section.
   *
   * @throws IllegalArgumentException on the removed {@code max-jobs} key, a malformed slot group,
   *     or a job named by two groups
   */
  @Contract(pure = true)
  public static @NotNull ProgressionLimitsConfig fromMap(@NotNull Map<?, ?> source) {
    if (source.containsKey("max-jobs")) {
      throw new IllegalArgumentException(
          "progression-limits.max-jobs was replaced by progression-limits.slot-groups");
    }
    List<SlotGroup> groups = new ArrayList<>();
    Map<?, ?> rawGroups = asMap(source.get("slot-groups"));
    if (rawGroups != null) {
      for (Map.Entry<?, ?> entry : rawGroups.entrySet()) {
        groups.add(parseGroup(String.valueOf(entry.getKey()), asMap(entry.getValue())));
      }
    }
    boolean worldJoin = true;
    Object rawWorld = source.get("world-join-restriction");
    if (rawWorld instanceof Boolean b) {
      worldJoin = b;
    }
    return new ProgressionLimitsConfig(groups, names(source.get("auto-join-jobs")), worldJoin);
  }

  private static @NotNull SlotGroup parseGroup(@NotNull String name, @Nullable Map<?, ?> raw) {
    if (raw == null || !(raw.get("slots") instanceof Number slots)) {
      throw new IllegalArgumentException("slot group " + name + " needs a numeric slots value");
    }
    return new SlotGroup(name, slots.intValue(), new LinkedHashSet<>(names(raw.get("jobs"))));
  }

  private static @NotNull List<String> names(@Nullable Object raw) {
    List<String> names = new ArrayList<>();
    if (raw instanceof List<?> list) {
      for (Object item : list) {
        if (item instanceof String s && !s.isBlank()) {
          names.add(s.trim().toLowerCase(Locale.ROOT));
        }
      }
    }
    return names;
  }

  private static @Nullable Map<?, ?> asMap(@Nullable Object raw) {
    if (raw instanceof ConfigurationSection section) {
      return section.getValues(false);
    }
    return raw instanceof Map<?, ?> map ? map : null;
  }

  /** Loads from {@code config.yml}. */
  public static @NotNull ProgressionLimitsConfig fromPlugin(@NotNull Plugin plugin) {
    ConfigurationSection section = plugin.getConfig().getConfigurationSection("progression-limits");
    if (section == null) {
      return defaults();
    }
    return fromMap(section.getValues(false));
  }
}
