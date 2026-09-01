package dev.mintychochip.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.Job;
import dev.mintychochip.JobKey;
import dev.mintychochip.JobNode;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.LevelingCurve;
import dev.mintychochip.PayableCurve;
import dev.mintychochip.config.YamlConfiguration;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

class JobPerkCatalogTest {

  @Test
  void loadsPerkUnlocksByTypedJobKey() throws Exception {
    StubYamlConfiguration configuration = new StubYamlConfiguration();
    configuration.loadFromString(
        """
        miner:
          display-name: Miner
          perk-unlocks:
            5: [lavaimmunity]
            15: [fargather, storage.tworows]
        fisherman:
          display-name: Fisherman
        """);

    JobPerkCatalog catalog = JobPerkCatalog.load(configuration);
    Job miner = job("miner");
    Job fisherman = job("fisherman");

    assertEquals(
        Map.of(
            5, List.of("lavaimmunity"),
            15, List.of("fargather", "storage.tworows")),
        catalog.unlocks(miner, miner.rootNode().nodeKey()));
    assertTrue(catalog.unlocks(fisherman, fisherman.rootNode().nodeKey()).isEmpty());
  }

  @Test
  void selectsHighestStorageTierByLevelInsteadOfMapOrder() throws Exception {
    StubYamlConfiguration configuration = new StubYamlConfiguration();
    configuration.loadFromString(
        """
        miner:
          perk-unlocks:
            30: [storage.fourrows]
            5: [storage.tworows]
            20: [storage.threerows]
        """);
    JobPerkCatalog catalog = JobPerkCatalog.load(configuration);
    Job miner = job("miner");
    JobNodeKey root = miner.rootNode().nodeKey();

    assertTrue(catalog.highestStorageUnlock(miner, root, 4).isEmpty());
    assertEquals("storage.tworows", catalog.highestStorageUnlock(miner, root, 5).orElseThrow());
    assertEquals("storage.threerows", catalog.highestStorageUnlock(miner, root, 29).orElseThrow());
    assertEquals("storage.fourrows", catalog.highestStorageUnlock(miner, root, 30).orElseThrow());
  }

  @Test
  void returnedUnlocksAreImmutable() throws Exception {
    StubYamlConfiguration configuration = new StubYamlConfiguration();
    configuration.loadFromString(
        """
        miner:
          perk-unlocks:
            5: [lavaimmunity]
        """);
    Job miner = job("miner");
    Map<Integer, List<String>> unlocks =
        JobPerkCatalog.load(configuration).unlocks(miner, miner.rootNode().nodeKey());

    assertThrows(UnsupportedOperationException.class, () -> unlocks.put(10, List.of("stoneskin")));
    assertThrows(UnsupportedOperationException.class, () -> unlocks.get(5).add("stoneskin"));
  }

  private static @NotNull Job job(@NotNull String rootValue) {
    JobKey jobKey = new JobKey(Key.key("modularjobs", rootValue));
    JobNode root = node(jobKey, rootValue, null);
    return new TestJob(jobKey, root, Map.of(root.nodeKey(), root));
  }

  private static @NotNull JobNode node(
      @NotNull JobKey jobKey, @NotNull String value, @Nullable JobNodeKey parentKey) {
    return new TestNode(
        jobKey,
        new JobNodeKey(Key.key("modularjobs", value)),
        parentKey,
        Component.text(value),
        Component.empty());
  }

  private record TestJob(
      @NotNull JobKey jobKey, @NotNull JobNode rootNode, @NotNull Map<JobNodeKey, JobNode> nodes)
      implements Job {

    @Override
    public int maxLevel() {
      return 100;
    }

    @Override
    public @NotNull LevelingCurve levelingCurve() {
      return parameters -> BigDecimal.valueOf(parameters.level());
    }

    @Override
    public @NotNull Map<Key, PayableCurve> payableCurves() {
      return Map.of();
    }
  }

  private record TestNode(
      @NotNull JobKey jobKey,
      @NotNull JobNodeKey nodeKey,
      @Nullable JobNodeKey parentKey,
      @NotNull Component displayName,
      @NotNull Component description)
      implements JobNode {

    @Override
    public @NotNull String getPlainName() {
      return nodeKey.key().value();
    }
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
