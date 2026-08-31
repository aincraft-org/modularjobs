# Dynamic Profession Loading Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the compiled `ProfessionCatalog` and load the same 15 profession definitions from an operator-editable `professions.yml` during Paper plugin startup.

**Architecture:** A Paper-internal `ProfessionIndex` provides immutable ordered definitions and constant-time canonical/storage-key resolution. `YamlProfessionDefinitionLoader` copies and atomically validates `professions.yml`, then `ProfessionWiring` injects the resulting index into `ProfessionServiceImpl` and passes that service to recipe loading. The API retains only profession DTOs and `ProfessionService`; no static compatibility facade remains.

**Tech Stack:** Java 21 source / Java 25 toolchain, Paper 26.2 Bukkit YAML, JUnit 5, MockBukkit 26.2, Gradle 9.6.1, Fumadocs/Next.js documentation.

**Spec:** `docs/superpowers/specs/2026-08-31-dynamic-profession-loading-design.md`

## Global Constraints

- Delete `modularjobs-api/src/main/java/dev/mintychochip/profession/ProfessionCatalog.java`; retain no shim, alias, deprecated class, or static singleton.
- Keep `ProfessionDefinition`, `ProfessionCategory`, and `ProfessionService` Paper-free in `modularjobs-api`.
- Ship `modularjobs-paper/src/main/resources/professions.yml`; copy it once with `saveResource("professions.yml", false)` and load the data-folder copy at startup.
- Load once per plugin composition. Runtime reload and mutable registration are out of scope.
- Reject the entire file on malformed content, duplicate/ambiguous keys, or a storage key absent from loaded `jobs.yml`; never skip an entry or use compiled fallback data.
- Preserve the existing 15 definitions, their order, canonical IDs, storage keys, categories, display names, case-insensitive lookup, and namespaced-suffix lookup. `fisher` must remain unresolved.
- Treat storage keys as the existing legacy aliases; do not add a separate `aliases` field.
- Preserve unrelated working-tree changes. Before each commit, stage only the paths named by that task.
- Follow red-green-refactor: run each named focused test before implementation and observe the expected failure, then rerun it after the minimal implementation.

## File Structure

**Create:**

- `modularjobs-paper/src/main/java/dev/mintychochip/profession/ProfessionIndex.java` — immutable lookup state; no I/O or Bukkit dependency.
- `modularjobs-paper/src/main/java/dev/mintychochip/profession/YamlProfessionDefinitionLoader.java` — resource copy, YAML parsing, validation, and job cross-reference checks.
- `modularjobs-paper/src/main/resources/professions.yml` — all profession content.
- `modularjobs-paper/src/test/java/dev/mintychochip/profession/ProfessionIndexTest.java` — lookup and ambiguity contracts.
- `modularjobs-paper/src/test/java/dev/mintychochip/profession/YamlProfessionDefinitionLoaderTest.java` — parser and diagnostics contracts.
- `modularjobs-paper/src/test/java/dev/mintychochip/profession/ProfessionServiceImplTest.java` — injected-index behavior.
- `modularjobs-paper/src/test/java/dev/mintychochip/profession/StubJobService.java` — deterministic package-local test fake shared by service and wiring tests.
- `modularjobs-paper/src/test/java/dev/mintychochip/profession/ProfessionResourcesAlignmentTest.java` — bundled `professions.yml`/`jobs.yml` contract.

**Modify:**

- `modularjobs-paper/src/main/java/dev/mintychochip/profession/ProfessionServiceImpl.java`
- `modularjobs-paper/src/main/java/dev/mintychochip/profession/ProfessionWiring.java`
- `modularjobs-paper/src/main/java/dev/mintychochip/profession/config/YamlRecipeDefinitionLoader.java`
- `modularjobs-paper/src/test/java/dev/mintychochip/profession/config/YamlRecipeDefinitionLoaderTest.java`
- `modularjobs-paper/src/test/java/dev/mintychochip/profession/ProfessionWiringStartupTest.java`
- `modularjobs-api/src/main/java/dev/mintychochip/profession/ProfessionDefinition.java`
- `modularjobs-api/src/main/java/dev/mintychochip/profession/ProfessionCategory.java`
- `modularjobs-api/src/main/java/dev/mintychochip/service/ProfessionService.java`
- `modularjobs-paper/src/main/resources/jobs.yml`
- `README.md`
- `docs/living-specs/README.md`
- `docs/living-specs/professions.md`
- `web/fumadocs/content/docs/develop/api.mdx`
- `web/fumadocs/content/docs/develop/events.mdx`
- `web/fumadocs/content/docs/reference/configuration.mdx`

**Rename:**

- `modularjobs-paper/src/test/java/dev/mintychochip/profession/RecipeLoaderTestPlugin.java` → `modularjobs-paper/src/test/java/dev/mintychochip/profession/ProfessionWiringTestPlugin.java`

**Delete:**

- `modularjobs-api/src/main/java/dev/mintychochip/profession/ProfessionCatalog.java`
- `modularjobs-api/src/test/java/dev/mintychochip/profession/ProfessionCatalogTest.java`
- `modularjobs-paper/src/test/java/dev/mintychochip/profession/ProfessionCatalogJobsYmlAlignmentTest.java`

---

### Task 1: Build the Immutable Profession Index

**Files:**
- Create: `modularjobs-paper/src/main/java/dev/mintychochip/profession/ProfessionIndex.java`
- Create: `modularjobs-paper/src/test/java/dev/mintychochip/profession/ProfessionIndexTest.java`

**Interfaces:**
- Consumes: `ProfessionDefinition` and `ProfessionCategory` from `modularjobs-api`.
- Produces: package-private `final class ProfessionIndex` with:
  - `ProfessionIndex(List<ProfessionDefinition> definitions)`
  - `List<ProfessionDefinition> tracks()`
  - `Optional<ProfessionDefinition> resolve(@Nullable String input)`

- [ ] **Step 1: Write failing lookup and immutability tests**

Create tests that use deliberately non-alphabetic source order and assert canonical ID, storage key, mixed case, surrounding whitespace, namespaced suffix, unknown, blank, and null behavior:

```java
class ProfessionIndexTest {

  private static final ProfessionDefinition MINING =
      new ProfessionDefinition("mining", "miner", ProfessionCategory.GATHERING, "Mining");
  private static final ProfessionDefinition COOKING =
      new ProfessionDefinition("cooking", "cooking", ProfessionCategory.CRAFTING, "Cooking");

  @Test
  void preservesOrderAndResolvesEverySupportedKeyShape() {
    ProfessionIndex index = new ProfessionIndex(List.of(COOKING, MINING));

    assertEquals(List.of(COOKING, MINING), index.tracks());
    assertEquals(MINING, index.resolve("mining").orElseThrow());
    assertEquals(MINING, index.resolve("miner").orElseThrow());
    assertEquals(MINING, index.resolve("  MiNeR  ").orElseThrow());
    assertEquals(MINING, index.resolve("modularjobs:miner").orElseThrow());
    assertTrue(index.resolve("builder").isEmpty());
    assertTrue(index.resolve(" ").isEmpty());
    assertTrue(index.resolve(null).isEmpty());
    assertThrows(UnsupportedOperationException.class, () -> index.tracks().add(MINING));
  }
}
```

- [ ] **Step 2: Write failing ambiguity tests**

Add three tests with these exact conflicts:

```java
@Test
void rejectsDuplicateCanonicalIds() {
  assertThrows(
      IllegalArgumentException.class,
      () -> new ProfessionIndex(List.of(MINING,
          new ProfessionDefinition("mining", "deep_miner", ProfessionCategory.GATHERING, "Deep Mining"))));
}

@Test
void rejectsDuplicateStorageKeys() {
  assertThrows(
      IllegalArgumentException.class,
      () -> new ProfessionIndex(List.of(MINING,
          new ProfessionDefinition("excavation", "miner", ProfessionCategory.GATHERING, "Excavation"))));
}

@Test
void rejectsCrossKindAmbiguity() {
  assertThrows(
      IllegalArgumentException.class,
      () -> new ProfessionIndex(List.of(MINING,
          new ProfessionDefinition("miner", "excavator", ProfessionCategory.GATHERING, "Excavation"))));
}
```

Assert each exception message contains the conflicting key and both source indexes.

- [ ] **Step 3: Run the focused test and verify red**

Run:

```bash
./gradlew :modularjobs-paper:test --tests 'dev.mintychochip.profession.ProfessionIndexTest'
```

Expected: test compilation fails because `ProfessionIndex` does not exist.

- [ ] **Step 4: Implement the immutable index**

Implement the complete class with no stream allocation in lookup paths:

```java
package dev.mintychochip.profession;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ProfessionIndex {

  private final List<ProfessionDefinition> tracks;
  private final Map<String, ProfessionDefinition> byId;
  private final Map<String, ProfessionDefinition> byStorageKey;

  ProfessionIndex(@NotNull List<ProfessionDefinition> definitions) {
    Objects.requireNonNull(definitions, "definitions");
    if (definitions.isEmpty()) {
      throw new IllegalArgumentException("profession definitions must not be empty");
    }
    tracks = List.copyOf(definitions);

    Map<String, ProfessionDefinition> ids = new LinkedHashMap<>();
    Map<String, ProfessionDefinition> storageKeys = new LinkedHashMap<>();
    Map<String, Integer> idIndexes = new LinkedHashMap<>();
    Map<String, Integer> storageIndexes = new LinkedHashMap<>();
    for (int index = 0; index < tracks.size(); index++) {
      ProfessionDefinition definition = Objects.requireNonNull(tracks.get(index), "definition");
      putUnique(ids, idIndexes, definition.id(), definition, index, "id");
      putUnique(
          storageKeys,
          storageIndexes,
          definition.storageKey(),
          definition,
          index,
          "storage-key");
    }
    for (Map.Entry<String, Integer> entry : idIndexes.entrySet()) {
      Integer storageIndex = storageIndexes.get(entry.getKey());
      if (storageIndex != null && !storageIndex.equals(entry.getValue())) {
        throw new IllegalArgumentException(
            "profession lookup key '"
                + entry.getKey()
                + "' is id at index "
                + entry.getValue()
                + " and storage-key at index "
                + storageIndex);
      }
    }
    byId = Map.copyOf(ids);
    byStorageKey = Map.copyOf(storageKeys);
  }

  @NotNull List<ProfessionDefinition> tracks() {
    return tracks;
  }

  @NotNull Optional<ProfessionDefinition> resolve(@Nullable String input) {
    if (input == null || input.isBlank()) {
      return Optional.empty();
    }
    String key = input.trim().toLowerCase(Locale.ROOT);
    int colon = key.indexOf(':');
    if (colon >= 0) {
      key = key.substring(colon + 1);
    }
    ProfessionDefinition definition = byId.get(key);
    return Optional.ofNullable(definition != null ? definition : byStorageKey.get(key));
  }

  private static void putUnique(
      @NotNull Map<String, ProfessionDefinition> definitions,
      @NotNull Map<String, Integer> indexes,
      @NotNull String key,
      @NotNull ProfessionDefinition definition,
      int index,
      @NotNull String field) {
    Integer previous = indexes.putIfAbsent(key, index);
    if (previous != null) {
      throw new IllegalArgumentException(
          "duplicate profession " + field + " '" + key + "' at indexes " + previous + " and " + index);
    }
    definitions.put(key, definition);
  }
}
```

Allow Google Java Format to wrap the final exception line.

- [ ] **Step 5: Run the focused test and verify green**

Run the command from Step 3.

Expected: `ProfessionIndexTest` passes.

- [ ] **Step 6: Commit the index**

```bash
git add modularjobs-paper/src/main/java/dev/mintychochip/profession/ProfessionIndex.java modularjobs-paper/src/test/java/dev/mintychochip/profession/ProfessionIndexTest.java
git commit -m "feat: add immutable profession index"
```

---

### Task 2: Load and Validate `professions.yml`

**Files:**
- Create: `modularjobs-paper/src/main/java/dev/mintychochip/profession/YamlProfessionDefinitionLoader.java`
- Create: `modularjobs-paper/src/main/resources/professions.yml`
- Create: `modularjobs-paper/src/test/java/dev/mintychochip/profession/YamlProfessionDefinitionLoaderTest.java`

**Interfaces:**
- Consumes: `ProfessionIndex` from Task 1, Bukkit `ConfigurationSection`, `JavaPlugin`, and `JobService.getJobs()`.
- Produces: package-private `final class YamlProfessionDefinitionLoader` with:
  - `static ProfessionIndex load(JavaPlugin plugin, JobService jobService)`
  - `static ProfessionIndex loadFromDataFolder(File dataFolder, Set<String> availableJobStorageKeys)`
  - `static ProfessionIndex loadFromConfiguration(ConfigurationSection configuration, Set<String> availableJobStorageKeys)`

- [ ] **Step 1: Write the valid-parser test**

Build a real Bukkit YAML object containing a list of maps, then assert order, normalization, category parsing, and resolution:

```java
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

  assertEquals(List.of("mining", "cooking"),
      index.tracks().stream().map(ProfessionDefinition::id).toList());
  assertEquals(ProfessionCategory.GATHERING, index.tracks().get(0).category());
  assertEquals("mining", index.resolve("miner").orElseThrow().id());
}
```

- [ ] **Step 2: Write fail-fast validation tests**

Add focused tests for each contract. Construct the YAML directly so every failure is isolated:

- root missing, root string, and empty root list → message contains `professions.yml: professions`;
- scalar list entry → message contains `professions[0] must be a mapping`;
- each missing, non-string, and blank required field → message contains `professions[0].<field>`;
- `id: "bad:key"` and `storage-key: "bad key"` → message contains `must match [a-z0-9._/-]+`;
- `category: combat` → message names all accepted categories;
- storage key not in the supplied job set → message contains `references unknown jobs.yml job`;
- duplicate ID, duplicate storage key, and cross-kind ambiguity → message contains `professions.yml`, the key, and both indexes.

Use this helper to keep each test's setup explicit:

```java
private static Map<String, Object> entry(
    String id, String storageKey, Object category, Object displayName) {
  Map<String, Object> entry = new LinkedHashMap<>();
  entry.put("id", id);
  entry.put("storage-key", storageKey);
  entry.put("category", category);
  entry.put("display-name", displayName);
  return entry;
}
```

For wrong-type and missing-field cases, mutate a fresh `LinkedHashMap` before assigning it under `professions`.

- [ ] **Step 3: Write the bundled-resource contract test**

Load both `professions.yml` and `jobs.yml` from the test classpath with
`YamlConfiguration.loadConfiguration(Reader)`. Pass `Set.copyOf(jobs.getKeys(false))` to the
profession loader so the test never duplicates the operator-owned job roster in Java, then assert:

```java
assertEquals(15, index.tracks().size());
assertEquals("mining", index.tracks().get(0).id());
assertEquals("engineering", index.tracks().get(14).id());
assertEquals("woodcutting", index.resolve("lumberjack").orElseThrow().id());
assertEquals("fishing", index.resolve("modularjobs:fisherman").orElseThrow().id());
assertTrue(index.resolve("fisher").isEmpty());
```

- [ ] **Step 4: Run the loader test and verify red**

```bash
./gradlew :modularjobs-paper:test --tests 'dev.mintychochip.profession.YamlProfessionDefinitionLoaderTest'
```

Expected: test compilation fails because the loader and bundled resource do not exist.

- [ ] **Step 5: Add the bundled profession data**

Create `professions.yml` with this exact ordered content:

```yaml
# Profession identity mapped onto jobs.yml progression keys.
# This file is copied to the plugin data folder once; restart after editing it.
professions:
  - id: mining
    storage-key: miner
    category: gathering
    display-name: Mining
  - id: woodcutting
    storage-key: lumberjack
    category: gathering
    display-name: Woodcutting
  - id: herbalism
    storage-key: herbalism
    category: gathering
    display-name: Herbalism
  - id: farming
    storage-key: farmer
    category: gathering
    display-name: Farming
  - id: fishing
    storage-key: fisherman
    category: gathering
    display-name: Fishing
  - id: smelting
    storage-key: smelting
    category: processing
    display-name: Smelting
  - id: milling
    storage-key: milling
    category: processing
    display-name: Milling
  - id: tanning
    storage-key: tanning
    category: processing
    display-name: Tanning
  - id: refining
    storage-key: refining
    category: processing
    display-name: Refining
  - id: cooking
    storage-key: cooking
    category: crafting
    display-name: Cooking
  - id: alchemy
    storage-key: alchemist
    category: crafting
    display-name: Alchemy
  - id: armorsmithing
    storage-key: armorsmithing
    category: crafting
    display-name: Armorsmithing
  - id: weaponsmithing
    storage-key: blacksmith
    category: crafting
    display-name: Weaponsmithing
  - id: tailoring
    storage-key: tailoring
    category: crafting
    display-name: Tailoring
  - id: engineering
    storage-key: engineering
    category: crafting
    display-name: Engineering
```

- [ ] **Step 6: Implement resource copying and atomic parsing**

Implement these behaviors in `YamlProfessionDefinitionLoader`:

```java
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
```

Define `CONFIG_FILE = "professions.yml"`, `ROOT = "professions"`, and
`KEY_PATTERN = Pattern.compile("[a-z0-9._/-]+")`. `parseEntry(...)` must:

1. obtain each field through `requiredString(Map<?, ?>, String, int)`;
2. trim and lowercase ID/storage key with `Locale.ROOT`;
3. reject keys that fail `KEY_PATTERN.matcher(value).matches()`;
4. parse category with `ProfessionCategory.valueOf(raw.toUpperCase(Locale.ROOT))`;
5. reject a storage key absent from `normalizedJobs`;
6. construct `new ProfessionDefinition(id, storageKey, category, displayName.trim())`.

All errors must be created by:

```java
private static @NotNull IllegalArgumentException error(
    @NotNull String path, @NotNull String message) {
  return new IllegalArgumentException(CONFIG_FILE + ": " + path + " " + message);
}
```

For an invalid category, emit:

```text
professions.yml: professions[<index>].category must be gathering, processing, or crafting; got '<value>'
```

- [ ] **Step 7: Run loader and index tests and verify green**

```bash
./gradlew :modularjobs-paper:test --tests 'dev.mintychochip.profession.ProfessionIndexTest' --tests 'dev.mintychochip.profession.YamlProfessionDefinitionLoaderTest'
```

Expected: both classes pass; the bundled file produces exactly 15 tracks.

- [ ] **Step 8: Commit the loader and resource**

```bash
git add modularjobs-paper/src/main/java/dev/mintychochip/profession/YamlProfessionDefinitionLoader.java modularjobs-paper/src/main/resources/professions.yml modularjobs-paper/src/test/java/dev/mintychochip/profession/YamlProfessionDefinitionLoaderTest.java
git commit -m "feat: load profession definitions from yaml"
```

---

### Task 3: Inject Loaded Definitions into `ProfessionService`

**Files:**
- Create: `modularjobs-paper/src/test/java/dev/mintychochip/profession/ProfessionServiceImplTest.java`
- Create: `modularjobs-paper/src/test/java/dev/mintychochip/profession/StubJobService.java`
- Modify: `modularjobs-paper/src/main/java/dev/mintychochip/profession/ProfessionServiceImpl.java`
- Modify: `modularjobs-paper/src/main/java/dev/mintychochip/profession/ProfessionWiring.java`
- Modify: `modularjobs-paper/src/test/java/dev/mintychochip/profession/ProfessionWiringStartupTest.java`
- Rename: `modularjobs-paper/src/test/java/dev/mintychochip/profession/RecipeLoaderTestPlugin.java` → `modularjobs-paper/src/test/java/dev/mintychochip/profession/ProfessionWiringTestPlugin.java`

**Interfaces:**
- Consumes: `ProfessionIndex`, `YamlProfessionDefinitionLoader`, and `JobService`.
- Produces: `ProfessionServiceImpl(JobService jobService, ProfessionIndex professions)`; `ProfessionWiring.create(...)` returns a service backed by the loaded YAML.

- [ ] **Step 1: Add a deterministic shared `JobService` fake**

Create package-private `StubJobService` with factory `static StubJobService withJobs(String... storageKeys)`. It must return `Job` objects whose keys are `Key.key("modularjobs", storageKey)`, expose `lastProgressionKey` and `lastJoinedKey`, return configurable `JobProgression progression`, and default `joinResult` to true.

Implement every `JobService` method deterministically:

```java
final class StubJobService implements JobService {
  private final List<Job> jobs;
  @Nullable JobProgression progression;
  @Nullable String lastProgressionKey;
  @Nullable String lastJoinedKey;
  boolean joinResult = true;

  private StubJobService(@NotNull List<Job> jobs) {
    this.jobs = List.copyOf(jobs);
  }

  static @NotNull StubJobService withJobs(@NotNull String... storageKeys) {
    return new StubJobService(
        Arrays.stream(storageKeys)
            .map(key -> (Job) new StubJob(new JobKey(Key.key("modularjobs", key))))
            .toList());
  }

  @Override public @NotNull List<Job> getJobs() { return jobs; }

  @Override
  public @NotNull Job getJob(@NotNull String jobKey) {
    String normalized = jobKey.contains(":") ? jobKey : "modularjobs:" + jobKey;
    return jobs.stream()
        .filter(job -> job.key().asString().equals(normalized))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("unknown job: " + jobKey));
  }

  @Override public @Nullable JobTask getTask(Job job, ActionType type, Context context) { return null; }
  @Override public @NotNull Map<ActionType, List<JobTask>> getAllTasks(Job job) { return Map.of(); }
  @Override public boolean update(JobProgression progression) { return false; }

  @Override
  public boolean joinJob(@NotNull String playerId, @NotNull String jobKey) {
    lastJoinedKey = jobKey;
    return joinResult;
  }

  @Override public boolean leaveJob(String playerId, String jobKey) { return false; }

  @Override
  public @Nullable JobProgression getProgression(
      @NotNull String playerId, @NotNull String jobKey) {
    lastProgressionKey = jobKey;
    return progression;
  }

  @Override public @NotNull List<JobProgression> getProgressions(UUID playerId) { return List.of(); }
  @Override public @NotNull List<JobProgression> getProgressions(Key jobKey, int limit) { return List.of(); }
  @Override public @NotNull List<JobProgression> getArchivedProgressions(UUID playerId) { return List.of(); }

  private record StubJob(@NotNull JobKey jobKey) implements Job {
    @Override public @NotNull JobNode rootNode() { throw new UnsupportedOperationException(); }
    @Override public @NotNull Map<JobNodeKey, JobNode> nodes() { return Map.of(); }
    @Override public @NotNull LevelingCurve levelingCurve() { throw new UnsupportedOperationException(); }
    @Override public @NotNull Map<Key, PayableCurve> payableCurves() { return Map.of(); }
    @Override public int maxLevel() { return 1; }
  }
}
```

Use the exact nullability annotations required by the current `JobService` source when compiling; do not weaken production contracts.

- [ ] **Step 2: Write failing service-injection tests**

```java
class ProfessionServiceImplTest {

  private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final ProfessionDefinition MINING =
      new ProfessionDefinition("mining", "miner", ProfessionCategory.GATHERING, "Mining");

  @Test
  void exposesInjectedDefinitionsAndUsesTheirStorageKeys() {
    StubJobService jobs = StubJobService.withJobs("miner");
    ProfessionService service =
        new ProfessionServiceImpl(jobs, new ProfessionIndex(List.of(MINING)));

    assertEquals(List.of(MINING), service.tracks());
    assertEquals(MINING, service.resolve("modularjobs:miner").orElseThrow());
    assertTrue(service.level(PLAYER_ID, "mining").isEmpty());
    assertEquals("miner", jobs.lastProgressionKey);
    assertTrue(service.ensureTrack(PLAYER_ID, "miner"));
    assertEquals("miner", jobs.lastJoinedKey);
  }

  @Test
  void unknownProfessionDoesNotTouchJobService() {
    StubJobService jobs = StubJobService.withJobs("miner");
    ProfessionService service =
        new ProfessionServiceImpl(jobs, new ProfessionIndex(List.of(MINING)));

    assertTrue(service.level(PLAYER_ID, "builder").isEmpty());
    assertFalse(service.ensureTrack(PLAYER_ID, "builder"));
    assertNull(jobs.lastProgressionKey);
    assertNull(jobs.lastJoinedKey);
  }
}
```

- [ ] **Step 3: Run the service test and verify red**

```bash
./gradlew :modularjobs-paper:test --tests 'dev.mintychochip.profession.ProfessionServiceImplTest'
```

Expected: compilation fails because `ProfessionServiceImpl` lacks the index constructor.

- [ ] **Step 4: Replace static catalog calls in `ProfessionServiceImpl`**

Change the constructor and all four lookup sites:

```java
private final JobService jobService;
private final ProfessionIndex professions;

public ProfessionServiceImpl(
    @NotNull JobService jobService, @NotNull ProfessionIndex professions) {
  this.jobService = jobService;
  this.professions = professions;
}

@Override
public @NotNull List<ProfessionDefinition> tracks() {
  return professions.tracks();
}

@Override
public @NotNull Optional<ProfessionDefinition> resolve(@NotNull String idOrAlias) {
  return professions.resolve(idOrAlias);
}
```

Use `professions.resolve(...)` in `ensureTrack(...)` and `progression(...)`; leave progression/join behavior otherwise unchanged.

- [ ] **Step 5: Load the index before constructing profession services**

Change `ProfessionWiring.create(...)` to this order while recipe loading still uses its existing signature:

```java
ProfessionIndex professionIndex = YamlProfessionDefinitionLoader.load(plugin, jobService);
ProfessionService professionService = new ProfessionServiceImpl(jobService, professionIndex);
MemoryRecipeService recipeService = new MemoryRecipeService();
YamlRecipeDefinitionLoader.load(plugin, recipeService);
return new ProfessionWiring(
    professionService,
    recipeService,
    new MemoryBuffService(),
    new StubStationService(),
    new StubNodeHarvestService());
```

- [ ] **Step 6: Expand the startup smoke test**

Rename `RecipeLoaderTestPlugin` and its class declaration to `ProfessionWiringTestPlugin`. In
`ProfessionWiringStartupTest`, replace the large anonymous service with
`StubJobService.withJobs(bundledJobStorageKeys())`. Implement `bundledJobStorageKeys()` by loading
`jobs.yml` through `YamlConfiguration.loadConfiguration(Reader)` and returning
`yaml.getKeys(false).toArray(String[]::new)`; do not duplicate the roster in Java. Load the renamed
plugin and add these assertions before the existing recipe assertions:

```java
assertTrue(Files.isRegularFile(plugin.getDataFolder().toPath().resolve("professions.yml")));
assertEquals(15, wiring.professionService.tracks().size());
assertEquals("mining", wiring.professionService.resolve("miner").orElseThrow().id());
```

Update the class JavaDoc to state that it exercises both profession and recipe startup resources.

- [ ] **Step 7: Run service and startup tests and verify green**

```bash
./gradlew :modularjobs-paper:test --tests 'dev.mintychochip.profession.ProfessionServiceImplTest' --tests 'dev.mintychochip.profession.ProfessionWiringStartupTest'
```

Expected: both pass; MockBukkit proves the bundled file is copied through the real `saveResource` path.

- [ ] **Step 8: Commit service injection and startup loading**

```bash
git add modularjobs-paper/src/main/java/dev/mintychochip/profession/ProfessionServiceImpl.java modularjobs-paper/src/main/java/dev/mintychochip/profession/ProfessionWiring.java modularjobs-paper/src/test/java/dev/mintychochip/profession/ProfessionServiceImplTest.java modularjobs-paper/src/test/java/dev/mintychochip/profession/StubJobService.java modularjobs-paper/src/test/java/dev/mintychochip/profession/ProfessionWiringStartupTest.java modularjobs-paper/src/test/java/dev/mintychochip/profession/RecipeLoaderTestPlugin.java modularjobs-paper/src/test/java/dev/mintychochip/profession/ProfessionWiringTestPlugin.java
git commit -m "refactor: inject loaded profession definitions"
```

---

### Task 4: Route Recipe Parsing Through `ProfessionService`

**Files:**
- Modify: `modularjobs-paper/src/main/java/dev/mintychochip/profession/config/YamlRecipeDefinitionLoader.java`
- Modify: `modularjobs-paper/src/main/java/dev/mintychochip/profession/ProfessionWiring.java`
- Modify: `modularjobs-paper/src/test/java/dev/mintychochip/profession/config/YamlRecipeDefinitionLoaderTest.java`

**Interfaces:**
- Consumes: initialized `ProfessionService` from Task 3.
- Produces:
  - `load(Plugin plugin, RecipeService recipeService, ProfessionService professionService)`
  - `loadFromDataFolder(File dataFolder, RecipeService recipeService, ProfessionService professionService, Logger logger)`
  - `loadFromConfiguration(ConfigurationSection config, RecipeService recipeService, ProfessionService professionService, Logger logger)`
  - `parseAll(ConfigurationSection recipesSection, ProfessionService professionService)`
  - `parseDefinition(String recipeKey, ConfigurationSection entry, ProfessionService professionService)`

- [ ] **Step 1: Add a recipe test resolver owned by the test**

In `YamlRecipeDefinitionLoaderTest`, add a `ProfessionDefinition` for weaponsmithing and a complete anonymous `ProfessionService` whose `resolve(...)` recognizes `weaponsmithing`, `blacksmith`, and `custom-smith`; its other methods return the one definition, empty level/experience, and false from `ensureTrack`:

```java
private static final ProfessionDefinition WEAPONSMITHING =
    new ProfessionDefinition(
        "weaponsmithing", "blacksmith", ProfessionCategory.CRAFTING, "Weaponsmithing");

private static final ProfessionService PROFESSIONS = new ProfessionService() {
  @Override public @NotNull List<ProfessionDefinition> tracks() { return List.of(WEAPONSMITHING); }

  @Override
  public @NotNull Optional<ProfessionDefinition> resolve(@NotNull String input) {
    String key = input.toLowerCase(Locale.ROOT);
    return switch (key) {
      case "weaponsmithing", "blacksmith", "custom-smith" -> Optional.of(WEAPONSMITHING);
      default -> Optional.empty();
    };
  }

  @Override public @NotNull OptionalInt level(UUID playerId, String profession) { return OptionalInt.empty(); }
  @Override public @NotNull Optional<BigDecimal> experience(UUID playerId, String profession) { return Optional.empty(); }
  @Override public boolean ensureTrack(UUID playerId, String profession) { return false; }
};
```

- [ ] **Step 2: Update tests to require service delegation**

Pass `PROFESSIONS` into every loader/parser call. Change `parsesStarterShapeWithDistinctOutputKey` to set `profession: custom-smith`; it must still produce canonical `weaponsmithing`. Keep `rejectsUnknownProfession`, which now proves the supplied service controls rejection.

Representative calls become:

```java
YamlRecipeDefinitionLoader.parseDefinition(recipeKey, section, PROFESSIONS);
YamlRecipeDefinitionLoader.parseAll(recipesSection, PROFESSIONS);
YamlRecipeDefinitionLoader.loadFromConfiguration(config, recipes, PROFESSIONS, Logger.getGlobal());
YamlRecipeDefinitionLoader.loadFromDataFolder(dataFolder.toFile(), recipes, PROFESSIONS, Logger.getGlobal());
```

- [ ] **Step 3: Run the recipe loader test and verify red**

```bash
./gradlew :modularjobs-paper:test --tests 'dev.mintychochip.profession.config.YamlRecipeDefinitionLoaderTest'
```

Expected: compilation fails because the loader signatures do not accept `ProfessionService`.

- [ ] **Step 4: Thread `ProfessionService` through the loader**

Remove the `ProfessionCatalog` import, add `ProfessionService`, and pass the service through every method listed under **Interfaces**. Replace the static lookup with:

```java
String professionId =
    professionService
        .resolve(professionRaw)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "unknown profession for recipe " + recipeKey + ": " + professionRaw))
        .id();
```

Do not change recipe ordering, output conflict detection, level/tier validation, log messages, or resource ownership.

- [ ] **Step 5: Pass the initialized service from wiring**

Update `ProfessionWiring.create(...)`:

```java
YamlRecipeDefinitionLoader.load(plugin, recipeService, professionService);
```

- [ ] **Step 6: Run recipe and startup tests and verify green**

```bash
./gradlew :modularjobs-paper:test --tests 'dev.mintychochip.profession.config.YamlRecipeDefinitionLoaderTest' --tests 'dev.mintychochip.profession.ProfessionWiringStartupTest'
```

Expected: both pass, including all six bundled recipes.

- [ ] **Step 7: Commit recipe migration**

```bash
git add modularjobs-paper/src/main/java/dev/mintychochip/profession/config/YamlRecipeDefinitionLoader.java modularjobs-paper/src/main/java/dev/mintychochip/profession/ProfessionWiring.java modularjobs-paper/src/test/java/dev/mintychochip/profession/config/YamlRecipeDefinitionLoaderTest.java
git commit -m "refactor: resolve recipes through profession service"
```

---

### Task 5: Remove the Static Catalog and Protect Resource Alignment

**Files:**
- Create: `modularjobs-paper/src/test/java/dev/mintychochip/profession/ProfessionResourcesAlignmentTest.java`
- Delete: `modularjobs-paper/src/test/java/dev/mintychochip/profession/ProfessionCatalogJobsYmlAlignmentTest.java`
- Delete: `modularjobs-api/src/test/java/dev/mintychochip/profession/ProfessionCatalogTest.java`
- Delete: `modularjobs-api/src/main/java/dev/mintychochip/profession/ProfessionCatalog.java`

**Interfaces:**
- Consumes: real bundled YAML plus `YamlProfessionDefinitionLoader.loadFromConfiguration(...)`.
- Produces: no `ProfessionCatalog` symbol; shipped resources become the only content source.

- [ ] **Step 1: Write the replacement alignment test**

Load both resources with real Bukkit YAML rather than regex parsing:

```java
class ProfessionResourcesAlignmentTest {

  @Test
  void shippedProfessionsMatchShippedJobsAndPreserveContract() {
    YamlConfiguration jobs = loadResource("jobs.yml");
    YamlConfiguration professions = loadResource("professions.yml");

    ProfessionIndex index =
        YamlProfessionDefinitionLoader.loadFromConfiguration(
            professions, Set.copyOf(jobs.getKeys(false)));

    assertEquals(15, index.tracks().size());
    assertEquals(5, count(index, ProfessionCategory.GATHERING));
    assertEquals(4, count(index, ProfessionCategory.PROCESSING));
    assertEquals(6, count(index, ProfessionCategory.CRAFTING));
    assertEquals("mining", index.resolve("miner").orElseThrow().id());
    assertEquals("woodcutting", index.resolve("lumberjack").orElseThrow().id());
    assertEquals("fishing", index.resolve("fisherman").orElseThrow().id());
    assertEquals("alchemy", index.resolve("alchemist").orElseThrow().id());
    assertEquals("weaponsmithing", index.resolve("blacksmith").orElseThrow().id());
    assertTrue(index.resolve("fisher").isEmpty());
    assertTrue(index.resolve("builder").isEmpty());
  }

  private static long count(ProfessionIndex index, ProfessionCategory category) {
    return index.tracks().stream().filter(track -> track.category() == category).count();
  }
}
```

Implement `loadResource(String)` with `getResourceAsStream`, `assertNotNull`, UTF-8 `InputStreamReader`, and `YamlConfiguration.loadConfiguration(reader)`.

- [ ] **Step 2: Run the replacement test before deletion**

```bash
./gradlew :modularjobs-paper:test --tests 'dev.mintychochip.profession.ProfessionResourcesAlignmentTest'
```

Expected: pass, proving the resource-backed replacement covers the old catalog contract.

- [ ] **Step 3: Delete static catalog source and tests**

Delete all three old files listed above. Do not move static methods into another API class.

- [ ] **Step 4: Confirm production code has no catalog references**

Use repository search over:

```text
modularjobs-api/src/main/java
modularjobs-paper/src/main/java
modularjobs-api/src/test/java
modularjobs-paper/src/test/java
```

Expected: zero `ProfessionCatalog` matches after deleting the old alignment test. Historical files under `docs/superpowers/` are records and remain unchanged.

- [ ] **Step 5: Compile both modules and rerun profession tests**

```bash
./gradlew :modularjobs-api:compileJava :modularjobs-paper:test --tests 'dev.mintychochip.profession.*' --tests 'dev.mintychochip.profession.config.YamlRecipeDefinitionLoaderTest'
```

Expected: success; no source or test needs the deleted class.

- [ ] **Step 6: Commit the clean cutover**

```bash
git add modularjobs-api/src/main/java/dev/mintychochip/profession/ProfessionCatalog.java modularjobs-api/src/test/java/dev/mintychochip/profession/ProfessionCatalogTest.java modularjobs-paper/src/test/java/dev/mintychochip/profession/ProfessionCatalogJobsYmlAlignmentTest.java modularjobs-paper/src/test/java/dev/mintychochip/profession/ProfessionResourcesAlignmentTest.java
git commit -m "refactor: remove static profession catalog"
```

---

### Task 6: Update Public Contracts, Resource Comments, and Documentation

**Files:**
- Modify: `modularjobs-api/src/main/java/dev/mintychochip/profession/ProfessionDefinition.java`
- Modify: `modularjobs-api/src/main/java/dev/mintychochip/profession/ProfessionCategory.java`
- Modify: `modularjobs-api/src/main/java/dev/mintychochip/service/ProfessionService.java`
- Modify: `modularjobs-paper/src/main/java/dev/mintychochip/profession/ProfessionServiceImpl.java`
- Modify: `modularjobs-paper/src/main/resources/jobs.yml`
- Modify: `README.md`
- Modify: `docs/living-specs/README.md`
- Modify: `docs/living-specs/professions.md`
- Modify: `web/fumadocs/content/docs/develop/api.mdx`
- Modify: `web/fumadocs/content/docs/develop/events.mdx`
- Modify: `web/fumadocs/content/docs/reference/configuration.mdx`

**Interfaces:**
- Consumes: completed resource-backed API behavior.
- Produces: documentation that directs integrators to `ProfessionService` and operators to `professions.yml`.

- [ ] **Step 1: Correct JavaDoc terminology**

Apply these semantic replacements without changing method signatures:

```java
// ProfessionDefinition
/** One profession track supplied by the server implementation. */

// ProfessionCategory
/** Profession groups. */

// ProfessionService
/** Public profession API over loaded definitions and job progression. */
/** Loaded tracks in source order. */
/** Resolves a canonical profession ID or job storage key. */

// ProfessionServiceImpl
/** Facade over loaded profession definitions and {@link JobService} progression. */
```

Rename the `resolve` parameter from `idOrAlias` to `idOrStorageKey` in the interface and implementation; parameter names are not binary API. Update internal private parameter names in the same file.

- [ ] **Step 2: Correct bundled `jobs.yml` ownership comments**

Replace the catalog comments above `herbalism` with:

```yaml
# Profession identity, category, and job-key mappings are loaded from professions.yml.
# Every profession storage-key must name one of the jobs in this file.
```

Do not alter any job values.

- [ ] **Step 3: Update current repository documentation**

Make these exact content changes while preserving surrounding unrelated edits:

- `README.md`: describe “resource-backed profession definitions and progression” rather than a compiled profession catalog.
- `docs/living-specs/README.md`: change the professions summary to “Resource-backed profession definitions, block-break gates, Bukkit services”.
- `docs/living-specs/professions.md`:
  - set `Last updated` to `2026-08-31`;
  - describe resource-backed identity loaded by `ProfessionService`;
  - change the current checklist item to “Startup-loaded profession definitions, level, and experience API”;
  - add a decisions-log row: `2026-08-31 | Load profession definitions from professions.yml | Keep content operator-editable and API state instance-owned`.
- `web/fumadocs/content/docs/develop/api.mdx`:
  - rename “Profession catalog” to “Professions”;
  - remove the `ProfessionCatalog` row;
  - add `ProfessionService` with `tracks()`, `resolve()`, `level()`, `experience()`, and `ensureTrack()`;
  - state that definitions load from `professions.yml` and direct static lookup no longer exists.
- `web/fumadocs/content/docs/develop/events.mdx`:
  - describe `ProfessionService` as the always-registered definition/progression facade;
  - replace the stale method list with the five actual methods above.
- `web/fumadocs/content/docs/reference/configuration.mdx`:
  - add `professions.yml` to “Starter content files”;
  - state it is copied once, loaded at startup, restart-required, and startup-fatal when invalid.

Do not rewrite historical design/plan documents under `docs/superpowers/`.

- [ ] **Step 4: Run API tests and documentation validation**

```bash
./gradlew :modularjobs-api:test :modularjobs-paper:compileJava
```

Then:

```bash
npm test
```

Run the npm command from `web/fumadocs`.

Expected: Gradle succeeds and the docs verifier reports no broken documentation contracts.

- [ ] **Step 5: Commit contract and documentation updates**

```bash
git add modularjobs-api/src/main/java/dev/mintychochip/profession/ProfessionDefinition.java modularjobs-api/src/main/java/dev/mintychochip/profession/ProfessionCategory.java modularjobs-api/src/main/java/dev/mintychochip/service/ProfessionService.java modularjobs-paper/src/main/java/dev/mintychochip/profession/ProfessionServiceImpl.java modularjobs-paper/src/main/resources/jobs.yml README.md docs/living-specs/README.md docs/living-specs/professions.md web/fumadocs/content/docs/develop/api.mdx web/fumadocs/content/docs/develop/events.mdx web/fumadocs/content/docs/reference/configuration.mdx
git commit -m "docs: document resource-backed professions"
```

---

### Task 7: Verify the End-to-End Cutover

**Files:**
- Verify all files changed by Tasks 1–6.
- Modify only a file with a demonstrated verification failure attributable to this feature.

**Interfaces:**
- Consumes: complete implementation.
- Produces: proof that resource loading, service resolution, recipe parsing, module contracts, documentation, and Paper packaging all work together.

- [ ] **Step 1: Run focused behavioral tests**

```bash
./gradlew :modularjobs-paper:test --tests 'dev.mintychochip.profession.ProfessionIndexTest' --tests 'dev.mintychochip.profession.YamlProfessionDefinitionLoaderTest' --tests 'dev.mintychochip.profession.ProfessionServiceImplTest' --tests 'dev.mintychochip.profession.ProfessionResourcesAlignmentTest' --tests 'dev.mintychochip.profession.ProfessionWiringStartupTest' --tests 'dev.mintychochip.profession.config.YamlRecipeDefinitionLoaderTest'
```

Expected: all focused tests pass. The MockBukkit startup test is the smoke test for resource copying and composed service exposure.

- [ ] **Step 2: Run repository Java verification**

```bash
./gradlew :modularjobs-api:test :modularjobs-common:test :modularjobs-paper:test :modularjobs-paper:build
```

Expected: all tasks succeed and the Paper shadow JAR is produced.

- [ ] **Step 3: Run Fumadocs verification and production build**

From `web/fumadocs`:

```bash
npm test
npm run build
```

Expected: documentation verification and Next.js production build both succeed.

- [ ] **Step 4: Recheck clean cutover and packaged content**

Use repository search to confirm `ProfessionCatalog` has no matches outside historical `docs/superpowers/` records and the approved spec/plan that describe its removal. Inspect the built Paper JAR/archive and confirm it contains `professions.yml`.

Expected:

- no production/test source imports or references `ProfessionCatalog`;
- `professions.yml` is present in the Paper artifact;
- `ProfessionService` remains exposed through `Bridge` and Bukkit service registration;
- no compatibility facade or compiled profession list exists.

- [ ] **Step 5: Review the feature diff and commit only any proven cleanup**

Check that all 15 rows appear once in `professions.yml`, no Java source duplicates the content, and no unrelated working-tree path was staged. If verification required a correction, rerun the failed command and commit only that correction with a message describing the demonstrated issue. If no correction was required, create no empty commit.
