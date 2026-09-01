package dev.mintychochip.service;

import dev.mintychochip.Job;
import dev.mintychochip.JobNode;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.config.YamlConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Immutable perk-unlock definitions keyed separately from job domain objects. */
public final class JobPerkCatalog {

  private final Map<JobNodeKey, Map<Integer, List<String>>> unlocksByNode;

  private JobPerkCatalog(@NotNull Map<JobNodeKey, Map<Integer, List<String>>> unlocksByNode) {
    this.unlocksByNode = Map.copyOf(unlocksByNode);
  }

  /** Loads {@code perk-unlocks} sections from the existing jobs configuration. */
  public static @NotNull JobPerkCatalog load(@NotNull YamlConfiguration configuration) {
    Map<JobNodeKey, Map<Integer, List<String>>> unlocksByNode = new HashMap<>();
    for (String jobName : configuration.getKeys(false)) {
      ConfigurationSection jobSection = configuration.getConfigurationSection(jobName);
      if (jobSection == null) {
        continue;
      }
      ConfigurationSection unlockSection = jobSection.getConfigurationSection("perk-unlocks");
      if (unlockSection == null) {
        continue;
      }

      Map<Integer, List<String>> unlocks = new HashMap<>();
      for (String levelName : unlockSection.getKeys(false)) {
        int level;
        try {
          level = Integer.parseInt(levelName);
        } catch (NumberFormatException ignored) {
          continue;
        }
        List<String> perks = unlockSection.getStringList(levelName);
        if (!perks.isEmpty()) {
          unlocks.put(level, List.copyOf(perks));
        }
      }
      if (!unlocks.isEmpty()) {
        unlocksByNode.put(new JobNodeKey(Key.key("modularjobs", jobName)), Map.copyOf(unlocks));
      }
    }
    return new JobPerkCatalog(unlocksByNode);
  }

  /** Returns the cumulative unlocks visible on the root-to-node path. */
  @Contract(pure = true)
  public @NotNull Map<Integer, List<String>> unlocks(
      @NotNull Job job, @NotNull JobNodeKey nodeKey) {
    Map<Integer, List<String>> merged = new LinkedHashMap<>();
    for (JobNode node : job.pathTo(nodeKey)) {
      for (Map.Entry<Integer, List<String>> entry :
          unlocksByNode.getOrDefault(node.nodeKey(), Map.of()).entrySet()) {
        merged
            .computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>())
            .addAll(entry.getValue());
      }
    }
    merged.replaceAll((ignored, perks) -> List.copyOf(perks));
    return Map.copyOf(merged);
  }

  /** Returns every unlock defined anywhere in the job tree. */
  @Contract(pure = true)
  public @NotNull Map<Integer, List<String>> allUnlocks(@NotNull Job job) {
    Map<Integer, List<String>> merged = new LinkedHashMap<>();
    for (JobNode node : job.nodes().values()) {
      for (Map.Entry<Integer, List<String>> entry :
          unlocksByNode.getOrDefault(node.nodeKey(), Map.of()).entrySet()) {
        merged
            .computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>())
            .addAll(entry.getValue());
      }
    }
    merged.replaceAll((ignored, perks) -> List.copyOf(perks));
    return Map.copyOf(merged);
  }

  /** Returns the highest eligible storage unlock on the active node path. */
  @Contract(pure = true)
  public @NotNull Optional<String> highestStorageUnlock(
      @NotNull Job job, @NotNull JobNodeKey nodeKey, int level) {
    int highestLevel = Integer.MIN_VALUE;
    String highestPerk = null;
    for (Map.Entry<Integer, List<String>> entry : unlocks(job, nodeKey).entrySet()) {
      int unlockLevel = entry.getKey();
      if (unlockLevel > level || unlockLevel < highestLevel) {
        continue;
      }
      for (String perk : entry.getValue()) {
        if (perk.startsWith("storage.")) {
          highestLevel = unlockLevel;
          highestPerk = perk;
        }
      }
    }
    return Optional.ofNullable(highestPerk);
  }
}
