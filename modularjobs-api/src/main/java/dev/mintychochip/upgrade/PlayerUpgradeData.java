package dev.mintychochip.upgrade;

import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player's upgrade data for a specific job. Tracks unlocked nodes and available skill
 * points.
 */
public interface PlayerUpgradeData {

  /** The player's UUID as string. */
  @Contract(pure = true)
  @NotNull
  String playerId();

  /** The job key this data belongs to. */
  @Contract(pure = true)
  @NotNull
  String jobKey();

  /** Total skill points. */
  @Contract(pure = true)
  int totalSkillPoints();

  /** Available skill points. */
  @Contract(pure = true)
  int availableSkillPoints();

  /** Spent skill points. */
  @Contract(pure = true)
  int spentSkillPoints();

  /** The full skill tree state backing this data (v2 format). */
  @Contract(pure = true)
  @NotNull
  SkillTreeState state();

  /**
   * Map of node key -> purchased level (v2 format). Only contains nodes that have been unlocked
   * (level >= 1).
   */
  @Contract(pure = true)
  default @NotNull Map<String, Integer> nodeLevels() {
    return state().nodeLevels();
  }

  /** Set of unlocked node keys. */
  @Contract(pure = true)
  @NotNull
  Set<String> unlockedNodes();

  /** Returns whether has unlocked. */
  @Contract(pure = true)
  boolean hasUnlocked(@NotNull String nodeKey);

  /**
   * Map of perk levels (perkId -> max level unlocked). Only contains perks that have been unlocked
   * (level >= 1).
   */
  @Contract(pure = true)
  @NotNull
  Map<String, Integer> perkLevels();

  /**
   * Get the current level of a perk.
   *
   * @return perk level (0 if not unlocked, else the max level unlocked)
   */
  @Contract(pure = true)
  default int getPerkLevel(@NotNull String perkId) {
    return perkLevels().getOrDefault(perkId, 0);
  }

  /**
   * Get the maximum level for a perk in this job's upgrade tree. This is determined by the upgrade
   * tree configuration (max_level on nodes).
   *
   * @param perkId the perk ID to check
   * @return max level achievable for this perk, or 1 if unknown
   */
  @Contract(pure = true)
  int getMaxLevel(@NotNull String perkId);

  /**
   * Check if a perk is at its maximum level.
   *
   * @param perkId the perk ID to check
   * @return true if perk level equals max level, false otherwise
   */
  @Contract(pure = true)
  default boolean isMaxLevel(@NotNull String perkId) {
    int current = getPerkLevel(perkId);
    int max = getMaxLevel(perkId);
    return current > 0 && current >= max;
  }
}
