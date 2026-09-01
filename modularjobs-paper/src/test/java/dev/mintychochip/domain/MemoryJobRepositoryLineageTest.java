package dev.mintychochip.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.mintychochip.config.YamlConfiguration;
import dev.mintychochip.domain.model.JobRecord;
import java.util.Map;
import java.util.Set;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

class MemoryJobRepositoryLineageTest {

  @Test
  void yamlParentUsesTheJobCatalogNamespace() throws Exception {
    StubYamlConfiguration configuration = new StubYamlConfiguration();
    configuration.loadFromString(
        """
        miner:
          display-name: Miner
          max-level: 200
          leveling-curve: level
          payable-curves:
            experience: base
        prospector:
          parent: miner
          display-name: Prospector
        """);

    Map<String, JobRecord> records =
        new MemoryJobRepositoryImpl.YamlRecordLoader().load(configuration);

    assertEquals("modularjobs:miner", records.get("modularjobs:prospector").parentKey());
    assertEquals(200, records.get("modularjobs:prospector").maxLevel());
    assertEquals("level", records.get("modularjobs:prospector").levellingCurve());
  }

  @Test
  void rejectsTreeWideRulesOnChildNodes() throws Exception {
    StubYamlConfiguration configuration = new StubYamlConfiguration();
    configuration.loadFromString(
        """
        miner:
          display-name: Miner
          max-level: 200
          leveling-curve: level
        prospector:
          parent: miner
          display-name: Prospector
          max-level: 100
        """);

    assertThrows(
        IllegalArgumentException.class,
        () -> new MemoryJobRepositoryImpl.YamlRecordLoader().load(configuration));
  }

  @Test
  void acceptsAnAcyclicParentChain() {
    Map<String, JobRecord> records =
        records(job("miner", null), job("prospector", "miner"), job("gemologist", "prospector"));

    assertDoesNotThrow(() -> new MemoryJobRepositoryImpl(records));
  }

  @Test
  void groupsEveryDescendantUnderItsRootJobTree() {
    JobRecord miner = job("miner", null);
    JobRecord prospector = job("prospector", "miner");
    JobRecord gemologist = job("gemologist", "prospector");
    MemoryJobRepositoryImpl repository =
        new MemoryJobRepositoryImpl(records(miner, prospector, gemologist));

    assertEquals(miner, repository.rootFor(prospector.jobKey()));
    assertEquals(
        Set.of(miner.jobKey(), prospector.jobKey(), gemologist.jobKey()),
        repository.loadTree(miner.jobKey()).stream()
            .map(JobRecord::jobKey)
            .collect(java.util.stream.Collectors.toSet()));
  }

  @Test
  void rejectsAnUnknownParent() {
    Map<String, JobRecord> records = records(job("prospector", "miner"));

    assertThrows(IllegalArgumentException.class, () -> new MemoryJobRepositoryImpl(records));
  }

  @Test
  void rejectsSelfParenting() {
    Map<String, JobRecord> records = records(job("miner", "miner"));

    assertThrows(IllegalArgumentException.class, () -> new MemoryJobRepositoryImpl(records));
  }

  @Test
  void rejectsParentCycles() {
    Map<String, JobRecord> records =
        records(job("miner", "prospector"), job("prospector", "miner"));

    assertThrows(IllegalArgumentException.class, () -> new MemoryJobRepositoryImpl(records));
  }

  private static @NotNull JobRecord job(@NotNull String key, @Nullable String parentKey) {
    return new JobRecord(
        namespaced(key),
        key,
        key,
        100,
        "level",
        Map.of(),
        parentKey == null ? null : namespaced(parentKey));
  }

  private static @NotNull Map<String, JobRecord> records(@NotNull JobRecord... records) {
    return java.util.Arrays.stream(records)
        .collect(java.util.stream.Collectors.toMap(JobRecord::jobKey, record -> record));
  }

  private static @NotNull String namespaced(@NotNull String key) {
    return "modularjobs:" + key;
  }

  private static final class StubYamlConfiguration
      extends org.bukkit.configuration.file.YamlConfiguration implements YamlConfiguration {

    @Override
    public @NotNull Plugin getPlugin() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void reload() {}

    @Override
    public void save() {}
  }
}
