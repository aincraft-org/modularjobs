package dev.mintychochip.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlProfessionDefinitionLoaderTest {

  @Test
  void loadsOrderedDefinitionsAndNormalizesFields() {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.set(
        "professions",
        List.of(
            Map.of(
                "id", " Mining ",
                "storage-key", " Miner ",
                "category", "GaThErInG",
                "display-name", "Mining"),
            Map.of(
                "id", "cooking",
                "storage-key", "cooking",
                "category", "crafting",
                "display-name", "Cooking")));

    ProfessionIndex index =
        YamlProfessionDefinitionLoader.loadFromConfiguration(yaml, Set.of("miner", "cooking"));

    assertEquals(
        List.of("mining", "cooking"),
        index.tracks().stream().map(ProfessionDefinition::id).toList());
    assertEquals(ProfessionCategory.GATHERING, index.tracks().get(0).category());
    assertEquals("mining", index.resolve("miner").orElseThrow().id());
  }

  @Test
  void rejectsMissingRoot() {
    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                YamlProfessionDefinitionLoader.loadFromConfiguration(
                    new YamlConfiguration(), Set.of()));
    assertTrue(failure.getMessage().contains("professions.yml: professions"));
  }

  @Test
  void rejectsNonListRoot() {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.set("professions", "not-a-list");

    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class,
            () -> YamlProfessionDefinitionLoader.loadFromConfiguration(yaml, Set.of()));
    assertTrue(failure.getMessage().contains("professions.yml: professions"));
  }

  @Test
  void rejectsEmptyRoot() {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.set("professions", List.of());

    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class,
            () -> YamlProfessionDefinitionLoader.loadFromConfiguration(yaml, Set.of()));
    assertTrue(failure.getMessage().contains("professions.yml: professions"));
  }

  @Test
  void rejectsScalarEntry() {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.set("professions", List.of("not-a-map"));

    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class,
            () -> YamlProfessionDefinitionLoader.loadFromConfiguration(yaml, Set.of()));
    assertTrue(failure.getMessage().contains("professions[0] must be a mapping"));
  }

  @Test
  void rejectsMissingRequiredFields() {
    for (String field : List.of("id", "storage-key", "category", "display-name")) {
      Map<String, Object> entry = entry("mining", "miner", "gathering", "Mining");
      entry.remove(field);
      YamlConfiguration yaml = yamlWith(entry);

      IllegalArgumentException failure =
          assertThrows(
              IllegalArgumentException.class,
              () -> YamlProfessionDefinitionLoader.loadFromConfiguration(yaml, Set.of("miner")));
      assertTrue(failure.getMessage().contains("professions[0]." + field), failure.getMessage());
    }
  }

  @Test
  void rejectsWrongTypedRequiredFields() {
    for (String field : List.of("id", "storage-key", "category", "display-name")) {
      Map<String, Object> entry = entry("mining", "miner", "gathering", "Mining");
      entry.put(field, 42);
      YamlConfiguration yaml = yamlWith(entry);

      IllegalArgumentException failure =
          assertThrows(
              IllegalArgumentException.class,
              () -> YamlProfessionDefinitionLoader.loadFromConfiguration(yaml, Set.of("miner")));
      assertTrue(failure.getMessage().contains("professions[0]." + field), failure.getMessage());
    }
  }

  @Test
  void rejectsBlankRequiredFields() {
    for (String field : List.of("id", "storage-key", "category", "display-name")) {
      Map<String, Object> entry = entry("mining", "miner", "gathering", "Mining");
      entry.put(field, "  ");
      YamlConfiguration yaml = yamlWith(entry);

      IllegalArgumentException failure =
          assertThrows(
              IllegalArgumentException.class,
              () -> YamlProfessionDefinitionLoader.loadFromConfiguration(yaml, Set.of("miner")));
      assertTrue(failure.getMessage().contains("professions[0]." + field), failure.getMessage());
    }
  }

  @Test
  void rejectsInvalidKeyCharacters() {
    Map<String, Object> badId = entry("bad:key", "miner", "gathering", "Mining");
    IllegalArgumentException idFailure =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                YamlProfessionDefinitionLoader.loadFromConfiguration(
                    yamlWith(badId), Set.of("miner")));
    assertTrue(idFailure.getMessage().contains("professions[0].id"));
    assertTrue(idFailure.getMessage().contains("must match [a-z0-9._/-]+"));

    Map<String, Object> badStorageKey = entry("mining", "bad key", "gathering", "Mining");
    IllegalArgumentException storageFailure =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                YamlProfessionDefinitionLoader.loadFromConfiguration(
                    yamlWith(badStorageKey), Set.of("bad key")));
    assertTrue(storageFailure.getMessage().contains("professions[0].storage-key"));
    assertTrue(storageFailure.getMessage().contains("must match [a-z0-9._/-]+"));
  }

  @Test
  void rejectsUnknownCategoryWithAcceptedValues() {
    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                YamlProfessionDefinitionLoader.loadFromConfiguration(
                    yamlWith(entry("mining", "miner", "combat", "Mining")), Set.of("miner")));
    assertTrue(
        failure.getMessage().contains("must be gathering, processing, or crafting; got 'combat'"));
  }

  @Test
  void rejectsUnknownStorageKey() {
    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                YamlProfessionDefinitionLoader.loadFromConfiguration(
                    yamlWith(entry("mining", "miner", "gathering", "Mining")), Set.of("farmer")));
    assertTrue(failure.getMessage().contains("references unknown jobs.yml job"));
  }

  @Test
  void rejectsDuplicateIds() {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.set(
        "professions",
        List.of(
            entry("mining", "miner", "gathering", "Mining"),
            entry("mining", "deep_miner", "gathering", "Deep Mining")));

    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                YamlProfessionDefinitionLoader.loadFromConfiguration(
                    yaml, Set.of("miner", "deep_miner")));
    assertTrue(failure.getMessage().contains("professions.yml"));
    assertTrue(failure.getMessage().contains("mining"));
    assertTrue(failure.getMessage().contains("0"));
    assertTrue(failure.getMessage().contains("1"));
  }

  @Test
  void rejectsDuplicateStorageKeys() {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.set(
        "professions",
        List.of(
            entry("mining", "miner", "gathering", "Mining"),
            entry("excavation", "miner", "gathering", "Excavation")));

    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class,
            () -> YamlProfessionDefinitionLoader.loadFromConfiguration(yaml, Set.of("miner")));
    assertTrue(failure.getMessage().contains("professions.yml"));
    assertTrue(failure.getMessage().contains("miner"));
    assertTrue(failure.getMessage().contains("0"));
    assertTrue(failure.getMessage().contains("1"));
  }

  @Test
  void rejectsCrossKindAmbiguity() {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.set(
        "professions",
        List.of(
            entry("mining", "miner", "gathering", "Mining"),
            entry("miner", "excavator", "gathering", "Excavation")));

    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                YamlProfessionDefinitionLoader.loadFromConfiguration(
                    yaml, Set.of("miner", "excavator")));
    assertTrue(failure.getMessage().contains("professions.yml"));
    assertTrue(failure.getMessage().contains("miner"));
    assertTrue(failure.getMessage().contains("0"));
    assertTrue(failure.getMessage().contains("1"));
  }

  @Test
  void loadsBundledResourceContract() throws Exception {
    YamlConfiguration professions;
    YamlConfiguration jobs;
    try (InputStream professionsIn =
            Thread.currentThread().getContextClassLoader().getResourceAsStream("professions.yml");
        InputStream jobsIn =
            Thread.currentThread().getContextClassLoader().getResourceAsStream("jobs.yml")) {
      assertNotNull(professionsIn, "bundled professions.yml must be on test classpath");
      assertNotNull(jobsIn, "bundled jobs.yml must be on test classpath");
      professions =
          YamlConfiguration.loadConfiguration(
              new InputStreamReader(professionsIn, StandardCharsets.UTF_8));
      jobs =
          YamlConfiguration.loadConfiguration(
              new InputStreamReader(jobsIn, StandardCharsets.UTF_8));
    }

    ProfessionIndex index =
        YamlProfessionDefinitionLoader.loadFromConfiguration(
            professions, Set.copyOf(jobs.getKeys(false)));

    assertEquals(15, index.tracks().size());
    assertEquals("mining", index.tracks().get(0).id());
    assertEquals("engineering", index.tracks().get(14).id());
    assertEquals("woodcutting", index.resolve("lumberjack").orElseThrow().id());
    assertEquals("fishing", index.resolve("modularjobs:fisherman").orElseThrow().id());
    assertTrue(index.resolve("fisher").isEmpty());
  }

  @Test
  void loadsDataFolderFile(@TempDir java.nio.file.Path dataFolder) throws Exception {
    java.nio.file.Files.writeString(
        dataFolder.resolve("professions.yml"),
        "professions:\n"
            + "  - id: mining\n"
            + "    storage-key: miner\n"
            + "    category: gathering\n"
            + "    display-name: Mining\n");

    ProfessionIndex index =
        YamlProfessionDefinitionLoader.loadFromDataFolder(dataFolder.toFile(), Set.of("miner"));

    assertEquals("mining", index.tracks().get(0).id());
  }

  private static YamlConfiguration yamlWith(Map<String, Object> entry) {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.set("professions", List.of(entry));
    return yaml;
  }

  private static Map<String, Object> entry(
      String id, String storageKey, Object category, Object displayName) {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("id", id);
    entry.put("storage-key", storageKey);
    entry.put("category", category);
    entry.put("display-name", displayName);
    return entry;
  }
}
