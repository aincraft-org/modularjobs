package dev.mintychochip.profession;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Built-in profession tracks plus legacy ModularJobs job-key aliases.
 *
 * <p>Canonical ids use stable profession names; each {@link ProfessionDefinition#storageKey()}
 * points at the {@code jobs.yml} node used for player-state and task resolution.
 */
public final class ProfessionCatalog {

  private static final List<ProfessionDefinition> TRACKS =
      List.of(
          // Gathering
          def("mining", "miner", ProfessionCategory.GATHERING, "Mining"),
          def("woodcutting", "lumberjack", ProfessionCategory.GATHERING, "Woodcutting"),
          def("herbalism", "herbalism", ProfessionCategory.GATHERING, "Herbalism"),
          def("farming", "farmer", ProfessionCategory.GATHERING, "Farming"),
          def("fishing", "fisherman", ProfessionCategory.GATHERING, "Fishing"),
          // Processing
          def("smelting", "smelting", ProfessionCategory.PROCESSING, "Smelting"),
          def("milling", "milling", ProfessionCategory.PROCESSING, "Milling"),
          def("tanning", "tanning", ProfessionCategory.PROCESSING, "Tanning"),
          def("refining", "refining", ProfessionCategory.PROCESSING, "Refining"),
          // Crafting
          def("cooking", "cooking", ProfessionCategory.CRAFTING, "Cooking"),
          def("alchemy", "alchemist", ProfessionCategory.CRAFTING, "Alchemy"),
          def("armorsmithing", "armorsmithing", ProfessionCategory.CRAFTING, "Armorsmithing"),
          def("weaponsmithing", "blacksmith", ProfessionCategory.CRAFTING, "Weaponsmithing"),
          def("tailoring", "tailoring", ProfessionCategory.CRAFTING, "Tailoring"),
          def("engineering", "engineering", ProfessionCategory.CRAFTING, "Engineering"));

  /** Legacy job keys → canonical profession id (not already a storageKey of a track). */
  private static final Map<String, String> LEGACY_ALIASES =
      Map.of(
          "lumberjack", "woodcutting",
          "miner", "mining",
          "farmer", "farming",
          "fisherman", "fishing",
          "alchemist", "alchemy",
          "blacksmith", "weaponsmithing");

  private static final Map<String, ProfessionDefinition> BY_ID;
  private static final Map<String, ProfessionDefinition> BY_STORAGE;

  static {
    Map<String, ProfessionDefinition> byId = new LinkedHashMap<>();
    Map<String, ProfessionDefinition> byStorage = new LinkedHashMap<>();
    for (ProfessionDefinition d : TRACKS) {
      byId.put(d.id(), d);
      byStorage.put(d.storageKey(), d);
    }
    BY_ID = Collections.unmodifiableMap(byId);
    BY_STORAGE = Collections.unmodifiableMap(byStorage);
  }

  private ProfessionCatalog() {}

  @Contract(pure = true)
  private static @NotNull ProfessionDefinition def(
      @NotNull String id,
      @NotNull String storageKey,
      @NotNull ProfessionCategory category,
      @NotNull String displayName) {
    return new ProfessionDefinition(id, storageKey, category, displayName);
  }

  /** All §8.1 tracks in catalog order. */
  @Contract(pure = true)
  public static @NotNull List<ProfessionDefinition> tracks() {
    return TRACKS;
  }

  /** Tracks by category. */
  @Contract(pure = true)
  public static @NotNull Collection<ProfessionDefinition> tracksByCategory(
      @NotNull ProfessionCategory category) {
    return TRACKS.stream().filter(t -> t.category() == category).toList();
  }

  /** By id. */
  @Contract(pure = true)
  public static @NotNull Optional<ProfessionDefinition> byId(@Nullable String id) {
    if (id == null || id.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(BY_ID.get(id.toLowerCase(Locale.ROOT)));
  }

  /** By storage key. */
  @Contract(pure = true)
  public static @NotNull Optional<ProfessionDefinition> byStorageKey(@Nullable String storageKey) {
    if (storageKey == null || storageKey.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(BY_STORAGE.get(storageKey.toLowerCase(Locale.ROOT)));
  }

  /** Resolve a canonical id, storage key, or legacy alias to a profession definition. */
  @Contract(pure = true)
  public static @NotNull Optional<ProfessionDefinition> resolve(@Nullable String idOrAlias) {
    if (idOrAlias == null || idOrAlias.isBlank()) {
      return Optional.empty();
    }
    String key = idOrAlias.toLowerCase(Locale.ROOT);
    // strip namespace if present (modularjobs:miner)
    int colon = key.indexOf(':');
    if (colon >= 0) {
      key = key.substring(colon + 1);
    }
    Optional<ProfessionDefinition> byId = byId(key);
    if (byId.isPresent()) {
      return byId;
    }
    Optional<ProfessionDefinition> byStorage = byStorageKey(key);
    if (byStorage.isPresent()) {
      return byStorage;
    }
    String aliased = LEGACY_ALIASES.get(key);
    if (aliased != null) {
      return byId(aliased);
    }
    return Optional.empty();
  }

  /** Returns whether canonical track. */
  @Contract(pure = true)
  public static boolean isCanonicalTrack(@Nullable String idOrAlias) {
    return resolve(idOrAlias).isPresent();
  }
}
