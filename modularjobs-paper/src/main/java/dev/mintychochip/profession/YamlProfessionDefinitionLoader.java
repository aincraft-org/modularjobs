package dev.mintychochip.profession;

import dev.mintychochip.service.JobService;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

final class YamlProfessionDefinitionLoader {

  private static final String CONFIG_FILE = "professions.yml";
  private static final String ROOT = "professions";
  private static final Pattern KEY_PATTERN = Pattern.compile("[a-z0-9._/-]+");

  private YamlProfessionDefinitionLoader() {}

  static @NotNull ProfessionIndex load(
      @NotNull JavaPlugin plugin, @NotNull JobService jobService) {
    plugin.saveResource(CONFIG_FILE, false);
    Set<String> availableJobs =
        jobService.getJobs().stream()
            .map(job -> job.key().value().toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
    return loadFromDataFolder(plugin.getDataFolder(), availableJobs);
  }

  static @NotNull ProfessionIndex loadFromDataFolder(
      @NotNull File dataFolder, @NotNull Set<String> availableJobStorageKeys) {
    File file = new File(dataFolder, CONFIG_FILE);
    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
    return loadFromConfiguration(yaml, availableJobStorageKeys);
  }

  static @NotNull ProfessionIndex loadFromConfiguration(
      @NotNull ConfigurationSection configuration,
      @NotNull Set<String> availableJobStorageKeys) {
    Object raw = configuration.get(ROOT);
    if (!(raw instanceof List<?> entries)) {
      throw error(ROOT, "must be a list");
    }
    if (entries.isEmpty()) {
      throw error(ROOT, "must not be empty");
    }

    Set<String> normalizedJobs =
        availableJobStorageKeys.stream()
            .map(value -> value.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
    List<ProfessionDefinition> definitions = new ArrayList<>(entries.size());
    for (int index = 0; index < entries.size(); index++) {
      Object rawEntry = entries.get(index);
      if (!(rawEntry instanceof Map<?, ?> fields)) {
        throw error("professions[" + index + "]", "must be a mapping");
      }
      definitions.add(parseEntry(fields, index, normalizedJobs));
    }
    try {
      return new ProfessionIndex(definitions);
    } catch (IllegalArgumentException failure) {
      throw new IllegalArgumentException(CONFIG_FILE + ": " + failure.getMessage(), failure);
    }
  }

  private static @NotNull ProfessionDefinition parseEntry(
      @NotNull Map<?, ?> fields, int index, @NotNull Set<String> normalizedJobs) {
    String path = "professions[" + index + "]";
    String id = requiredString(fields, "id", index).trim().toLowerCase(Locale.ROOT);
    String storageKey =
        requiredString(fields, "storage-key", index).trim().toLowerCase(Locale.ROOT);
    String categoryValue = requiredString(fields, "category", index);
    String displayName = requiredString(fields, "display-name", index).trim();

    if (!KEY_PATTERN.matcher(id).matches()) {
      throw error(path + ".id", "must match [a-z0-9._/-]+");
    }
    if (!KEY_PATTERN.matcher(storageKey).matches()) {
      throw error(path + ".storage-key", "must match [a-z0-9._/-]+");
    }

    ProfessionCategory category;
    try {
      category = ProfessionCategory.valueOf(categoryValue.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException failure) {
      throw error(
          path + ".category",
          "must be gathering, processing, or crafting; got '" + categoryValue + "'");
    }

    if (!normalizedJobs.contains(storageKey)) {
      throw error(path + ".storage-key", "references unknown jobs.yml job '" + storageKey + "'");
    }
    return new ProfessionDefinition(id, storageKey, category, displayName);
  }

  private static @NotNull String requiredString(
      @NotNull Map<?, ?> fields, @NotNull String field, int index) {
    String path = "professions[" + index + "]." + field;
    Object value = fields.get(field);
    if (!(value instanceof String string)) {
      throw error(path, "must be a string");
    }
    if (string.isBlank()) {
      throw error(path, "must not be blank");
    }
    return string;
  }

  private static @NotNull IllegalArgumentException error(
      @NotNull String path, @NotNull String message) {
    return new IllegalArgumentException(CONFIG_FILE + ": " + path + " " + message);
  }
}
