package dev.mintychochip.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.Job;
import dev.mintychochip.JobKey;
import dev.mintychochip.JobNode;
import dev.mintychochip.JobNodeKey;
import dev.mintychochip.JobTask;
import dev.mintychochip.LevelingCurve;
import dev.mintychochip.PayableCurve;
import dev.mintychochip.PlayerJobState;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Context;
import dev.mintychochip.service.JobService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Drives shipped {@link JobResolver} plain-name / namespaced resolve and fuzzy suggest. */
class JobResolverImplTest {

  private Job miner;
  private Job fisherman;
  private Job lumberjack;
  private JobResolver resolver;

  @BeforeEach
  void setUp() {
    miner = jobWithChild("modularjobs", "miner", "Miner", "prospector", "Prospector");
    fisherman = job("modularjobs", "fisherman", "Fisherman");
    lumberjack = job("other", "lumberjack", "Lumberjack");
    Map<String, Job> byKey = new HashMap<>();
    byKey.put(miner.key().asString(), miner);
    byKey.put("modularjobs:prospector", miner);
    byKey.put(fisherman.key().asString(), fisherman);
    byKey.put(lumberjack.key().asString(), lumberjack);
    resolver = new JobResolver(new FakeJobService(List.of(miner, fisherman, lumberjack), byKey));
  }

  @Test
  void resolvePlainNameCaseInsensitive() {
    Job found = resolver.resolve("miner");
    assertNotNull(found);
    assertEquals(miner.key(), found.key());

    Job upper = resolver.resolve("FISHERMAN");
    assertNotNull(upper);
    assertEquals(fisherman.key(), upper.key());
  }

  @Test
  void resolveFullNamespacedKey() {
    Job found = resolver.resolve("modularjobs:miner");
    assertNotNull(found);
    assertEquals("Miner", found.getPlainName());
  }

  @Test
  void resolveUnknownReturnsNull() {
    assertNull(resolver.resolve("blacksmith"));
    assertNull(resolver.resolve("modularjobs:missing"));
  }

  @Test
  void resolveInNamespacePrefersNamespacedThenPlain() {
    Job byNs = resolver.resolveInNamespace("miner", "modularjobs");
    assertNotNull(byNs);
    assertEquals(miner.key(), byNs.key());

    Job otherNs = resolver.resolveInNamespace("lumberjack", "other");
    assertNotNull(otherNs);
    assertEquals(lumberjack.key(), otherNs.key());

    assertEquals(miner, resolver.resolveInNamespace("modularjobs:miner", "modularjobs"));
    assertEquals(miner, resolver.resolveInNamespace("modularjobs:prospector", "modularjobs"));
    assertNull(resolver.resolveInNamespace("other:lumberjack", "modularjobs"));

    assertNull(resolver.resolveInNamespace("miner", "other"));
  }

  @Test
  void resolvesAndSuggestsSpecializationNodes() {
    Job found = resolver.resolve("Prospector");

    assertNotNull(found);
    assertEquals(miner, found);
    assertEquals(miner, resolver.resolve("modularjobs:prospector"));
    assertTrue(resolver.getPlainNames().contains("Prospector"));
    assertEquals("Prospector", resolver.suggestSimilar("prosp", 1).get(0));
  }

  @Test
  void suggestSimilarPrefersPrefixMatches() {
    List<String> suggestions = resolver.suggestSimilar("min", 5);
    assertFalseEmpty(suggestions);
    assertEquals("Miner", suggestions.get(0), "prefix match should rank first: " + suggestions);
  }

  @Test
  void suggestSimilarLimitsResults() {
    List<String> suggestions = resolver.suggestSimilar("m", 1);
    assertEquals(1, suggestions.size());
  }

  @Test
  void getPlainNamesListsAllJobs() {
    List<String> names = resolver.getPlainNames();
    assertEquals(4, names.size());
    assertTrue(names.contains("Miner"));
    assertTrue(names.contains("Prospector"));
    assertTrue(names.contains("Fisherman"));
    assertTrue(names.contains("Lumberjack"));
  }

  private static void assertFalseEmpty(@NotNull List<String> suggestions) {
    assertNotNull(suggestions);
    assertTrue(!suggestions.isEmpty(), "expected non-empty suggestions");
  }

  private static @NotNull Job job(
      @NotNull String namespace, @NotNull String value, @NotNull String displayName) {
    JobKey jobKey = new JobKey(Key.key(namespace, value));
    JobNodeKey nodeKey = new JobNodeKey(jobKey.key());
    JobNode root =
        new TestNode(
            jobKey,
            nodeKey,
            null,
            Component.text(displayName),
            Component.text(displayName + " job"));
    return new TestJob(jobKey, root, Map.of(nodeKey, root));
  }

  private static @NotNull Job jobWithChild(
      @NotNull String namespace,
      @NotNull String rootValue,
      @NotNull String rootDisplayName,
      @NotNull String childValue,
      @NotNull String childDisplayName) {
    JobKey jobKey = new JobKey(Key.key(namespace, rootValue));
    JobNodeKey rootKey = new JobNodeKey(jobKey.key());
    JobNodeKey childKey = new JobNodeKey(Key.key(namespace, childValue));
    JobNode root =
        new TestNode(
            jobKey,
            rootKey,
            null,
            Component.text(rootDisplayName),
            Component.text(rootDisplayName + " job"));
    JobNode child =
        new TestNode(
            jobKey,
            childKey,
            rootKey,
            Component.text(childDisplayName),
            Component.text(childDisplayName + " specialization"));
    return new TestJob(jobKey, root, Map.of(rootKey, root, childKey, child));
  }

  private record TestJob(
      @NotNull JobKey jobKey, @NotNull JobNode rootNode, @NotNull Map<JobNodeKey, JobNode> nodes)
      implements Job {

    @Override
    public int maxLevel() {
      return 50;
    }

    @Override
    public @NotNull LevelingCurve levelingCurve() {
      return parameters -> java.math.BigDecimal.valueOf(parameters.level() * 100L);
    }

    @Override
    public @NotNull Map<Key, PayableCurve> payableCurves() {
      return Map.of();
    }
  }

  private record TestNode(
      @NotNull JobKey jobKey,
      @NotNull JobNodeKey nodeKey,
      JobNodeKey parentKey,
      @NotNull Component displayName,
      @NotNull Component description)
      implements JobNode {

    @Override
    public @NotNull String getPlainName() {
      return PlainTextComponentSerializer.plainText().serialize(displayName);
    }
  }

  /** Collaborator fake — SUT is JobResolver. */
  private static final class FakeJobService implements JobService {

    private final List<Job> jobs;
    private final Map<String, Job> byKey;

    FakeJobService(@NotNull List<Job> jobs, @NotNull Map<String, Job> byKey) {
      this.jobs = jobs;
      this.byKey = byKey;
    }

    @Override
    public @NotNull List<Job> getJobs() {
      return jobs;
    }

    @Override
    public @NotNull Job getJob(@NotNull String jobKey) {
      Job job = byKey.get(jobKey);
      if (job == null) {
        throw new IllegalArgumentException("unknown job: " + jobKey);
      }
      return job;
    }

    @Override
    public @NotNull JobTask getTask(
        @NotNull Job job,
        @NotNull JobNodeKey nodeKey,
        @NotNull ActionType type,
        @NotNull Context context) {
      throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Map<ActionType, List<JobTask>> getAllTasks(
        @NotNull Job job, @NotNull JobNodeKey nodeKey) {
      return Map.of();
    }

    @Override
    public boolean update(@NotNull PlayerJobState state) {
      return false;
    }

    @Override
    public boolean joinJob(@NotNull String playerId, @NotNull String jobKey) {
      return false;
    }

    @Override
    public boolean leaveJob(@NotNull String playerId, @NotNull String jobKey) {
      return false;
    }

    @Override
    public @NotNull PlayerJobState getPlayerJobState(
        @NotNull String playerId, @NotNull String jobKey) {
      throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull List<PlayerJobState> getPlayerJobStates(@NotNull UUID playerId) {
      return List.of();
    }

    @Override
    public @NotNull List<PlayerJobState> getPlayerJobStates(@NotNull Key jobKey, int limit) {
      return List.of();
    }

    @Override
    public @NotNull List<PlayerJobState> getArchivedPlayerJobStates(@NotNull UUID playerId) {
      return List.of();
    }
  }
}
