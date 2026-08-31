# Service-Owned Leveling Formulas Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the opaque `Job.levelingCurve()` API with service-owned, inspectable leveling profiles backed by typed deterministic variables and canonical level/XP derivation.

**Architecture:** Add Paper-free leveling contracts to `modularjobs-api`, then implement one Paper-domain leveling engine that atomically publishes additive variable-catalog snapshots. Each `JobLeveling` captures one catalog revision, one immutable formula descriptor, and one validated threshold table; `JobService`, progression mapping, commands, and GUI code all consume that profile instead of evaluating curves independently.

**Tech Stack:** Java 21-compatible API bytecode / Java 25 toolchain, Gradle 9.6.1 Kotlin DSL, Paper 26.2, Adventure `Key`, exp4j 0.4.8, JUnit 5.11.4, MockBukkit 26.2.

**Spec:** `docs/superpowers/specs/2026-08-31-leveling-formula-service-design.md`

## Global Constraints

- `modularjobs-api` remains Paper/Bukkit-free; every new public signature must pass `ApiSurfaceLinkageTest`.
- Preserve the concurrent, uncommitted `Job.parentKey()` lineage work in `Job`, `JobImpl`, `JobRecord`, loaders, and tests.
- Formula inputs are deterministic: job key, max level, and target level only. Never add player, UUID, world, permissions, clock, service, or mutable server-state inputs.
- Supported variable types are exactly `INTEGER` and `DECIMAL`; resolvers return `BigDecimal`.
- Variable registration is additive-only. Reject replacement, removal, duplicate names, reserved constants/functions, and replacement of `level`.
- Formula declarations, resolver bindings, formula caches, and threshold tables must come from one atomically captured catalog revision.
- `JobRecord.levellingCurve` retains its existing spelling and persisted raw string.
- Persisted `JobProgressionRecord` instances retain their embedded job/formula snapshot.
- Thresholds are non-negative and strictly increasing for levels `1..maxLevel`; invalid profiles are never cached.
- Do not change payable-curve behavior in this cutover.
- Final state has no compatibility alias, deprecated accessor, legacy `LevelingCurve`, or alternate leveling evaluator.
- The worktree contains unrelated user changes. Stage and commit only paths named by each task; never reset, checkout, broadly format, or use `git add .`.

## File Map

### Public API

- Create `modularjobs-api/src/main/java/dev/mintychochip/leveling/FormulaVariableType.java` — numeric semantic types.
- Create `modularjobs-api/src/main/java/dev/mintychochip/leveling/FormulaVariable.java` — immutable user-visible declaration.
- Create `modularjobs-api/src/main/java/dev/mintychochip/leveling/LevelingContext.java` — immutable resolver input.
- Create `modularjobs-api/src/main/java/dev/mintychochip/leveling/LevelingVariableResolver.java` — pure registration hook.
- Create `modularjobs-api/src/main/java/dev/mintychochip/leveling/LevelingFormula.java` — inspectable formula snapshot.
- Create `modularjobs-api/src/main/java/dev/mintychochip/leveling/JobLeveling.java` — canonical thresholds and derivation.
- Create `modularjobs-api/src/main/java/dev/mintychochip/leveling/LevelingState.java` — derived XP state.
- Create `modularjobs-api/src/main/java/dev/mintychochip/exception/LevelingConfigurationException.java` — descriptive profile-construction failure.
- Modify `modularjobs-api/src/main/java/dev/mintychochip/service/JobService.java` — query/register leveling operations.
- Modify `modularjobs-api/src/main/java/dev/mintychochip/Job.java` — remove the curve accessor at final cutover.
- Delete `modularjobs-api/src/main/java/dev/mintychochip/LevelingCurve.java` at final cutover.

### Paper implementation

- Create `modularjobs-paper/src/main/java/dev/mintychochip/domain/LevelingEngine.java` — catalog publication, revision-local profile cache, threshold construction.
- Create `modularjobs-paper/src/main/java/dev/mintychochip/domain/ExpressionLevelingFormula.java` — exp4j parsing, referenced-variable inspection, pure resolution/evaluation.
- Create `modularjobs-paper/src/main/java/dev/mintychochip/domain/JobLevelingImpl.java` — threshold lookup, binary search, and `LevelingState` derivation.
- Modify `DomainWiring`, `JobServiceImpl`, `PersistenceConverters`, `JobProgressionImpl`, and `JobImpl` — one engine through the composition and persistence boundaries.
- Modify `LevelCommand` and `StatsGui` — consume progression/profile thresholds.
- Modify `ExpressionCurves` — retain payable curves only.

### Tests and documentation

- Create focused API and Paper leveling tests under the existing module test roots.
- Migrate every test fixture implementing `Job` or `JobService`.
- Update `web/fumadocs/content/docs/develop/api.mdx`, `docs/living-specs/jobs-progression.md`, and `CHANGELOG.md`.

---

### Task 1: Add Paper-Free Leveling Contracts

**Files:**
- Create: `modularjobs-api/src/main/java/dev/mintychochip/leveling/FormulaVariableType.java`
- Create: `modularjobs-api/src/main/java/dev/mintychochip/leveling/FormulaVariable.java`
- Create: `modularjobs-api/src/main/java/dev/mintychochip/leveling/LevelingContext.java`
- Create: `modularjobs-api/src/main/java/dev/mintychochip/leveling/LevelingVariableResolver.java`
- Create: `modularjobs-api/src/main/java/dev/mintychochip/leveling/LevelingFormula.java`
- Create: `modularjobs-api/src/main/java/dev/mintychochip/leveling/JobLeveling.java`
- Create: `modularjobs-api/src/main/java/dev/mintychochip/leveling/LevelingState.java`
- Create: `modularjobs-api/src/main/java/dev/mintychochip/exception/LevelingConfigurationException.java`
- Create: `modularjobs-api/src/test/java/dev/mintychochip/leveling/LevelingApiContractTest.java`

**Interfaces:**
- Consumes: existing `dev.mintychochip.Job`, Adventure `Key`, `BigDecimal`, immutable JDK collections.
- Produces: the exact public types and signatures shown in the approved specification; later tasks compile only against these contracts.

- [ ] **Step 1: Write failing contract tests**

Create `LevelingApiContractTest` with concrete validation and immutability checks:

```java
@Test
void variableDeclarationExposesPortableTypedMetadata() {
  FormulaVariable variable =
      new FormulaVariable(
          "prestige_multiplier",
          FormulaVariableType.DECIMAL,
          "Configured prestige multiplier",
          Key.key("example", "prestige"));

  assertEquals("prestige_multiplier", variable.name());
  assertEquals(FormulaVariableType.DECIMAL, variable.type());
  assertEquals(Key.key("example", "prestige"), variable.provider());
}

@Test
void variableNamesUseExpressionIdentifierGrammar() {
  assertThrows(
      IllegalArgumentException.class,
      () -> new FormulaVariable("bad-name", FormulaVariableType.INTEGER, "bad", Key.key("x", "y")));
  assertThrows(
      IllegalArgumentException.class,
      () -> new FormulaVariable("1level", FormulaVariableType.INTEGER, "bad", Key.key("x", "y")));
}

@Test
void contextRejectsInvalidLevelBounds() {
  Key key = Key.key("modularjobs", "miner");
  assertThrows(IllegalArgumentException.class, () -> new LevelingContext(key, 0, 1));
  assertThrows(IllegalArgumentException.class, () -> new LevelingContext(key, 10, 0));
  assertThrows(IllegalArgumentException.class, () -> new LevelingContext(key, 10, 11));
}

@Test
void stateRequiresCoherentNextLevelFields() {
  assertThrows(
      IllegalArgumentException.class,
      () ->
          new LevelingState(
              1,
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              Optional.of(BigDecimal.TEN),
              BigDecimal.ZERO,
              Optional.empty(),
              Optional.of(BigDecimal.ZERO)));
}
```

Instantiate a `LevelingVariableResolver` lambda and reference
`LevelingFormula.class` and `JobLeveling.class` directly. Those two interfaces
are not functional interfaces; `ApiSurfaceLinkageTest` discovers and inspects
their compiled public signatures automatically.

- [ ] **Step 2: Run the API test and confirm the missing-contract failure**

Run:

```bash
./gradlew :modularjobs-api:test --tests dev.mintychochip.leveling.LevelingApiContractTest
```

Expected: test compilation fails because `dev.mintychochip.leveling` does not exist.

- [ ] **Step 3: Implement the immutable API types**

Use these signatures without Paper imports:

```java
public enum FormulaVariableType {
  INTEGER,
  DECIMAL
}

public record FormulaVariable(
    @NotNull String name,
    @NotNull FormulaVariableType type,
    @NotNull String description,
    @NotNull Key provider) {

  private static final Pattern IDENTIFIER =
      Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

  public FormulaVariable {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(provider, "provider");
    if (!IDENTIFIER.matcher(name).matches()) {
      throw new IllegalArgumentException("Invalid formula variable name: " + name);
    }
  }
}

public record LevelingContext(@NotNull Key jobKey, int maxLevel, int targetLevel) {
  public LevelingContext {
    Objects.requireNonNull(jobKey, "jobKey");
    if (maxLevel < 1) throw new IllegalArgumentException("maxLevel must be >= 1");
    if (targetLevel < 1 || targetLevel > maxLevel) {
      throw new IllegalArgumentException(
          "targetLevel must be between 1 and " + maxLevel + ": " + targetLevel);
    }
  }
}

@FunctionalInterface
public interface LevelingVariableResolver {
  @NotNull BigDecimal resolve(@NotNull LevelingContext context);
}
```

Define `LevelingFormula` and `JobLeveling` exactly as follows:

```java
public interface LevelingFormula {
  @NotNull String expression();
  @NotNull List<FormulaVariable> variables();
  @NotNull Map<String, BigDecimal> resolveVariables(int targetLevel);
  @NotNull BigDecimal evaluate(int targetLevel);
}

public interface JobLeveling {
  @NotNull Job job();
  @NotNull LevelingFormula formula();
  @NotNull BigDecimal experienceForLevel(int level);
  int levelForExperience(@NotNull BigDecimal experience);
  @NotNull LevelingState inspect(@NotNull BigDecimal experience);
}
```

Implement `LevelingState` as the seven-component record in the specification. Its compact constructor must require non-null values, require `level >= 1`, require non-negative `experienceIntoLevel` and `experienceToNextLevel`, require `progressToNextLevel` in `[0, 1]`, and require the three next-level optionals to be either all present or all empty.

Create a final unchecked exception:

```java
public final class LevelingConfigurationException extends IllegalArgumentException {
  public LevelingConfigurationException(@NotNull String message) {
    super(message);
  }

  public LevelingConfigurationException(@NotNull String message, @NotNull Throwable cause) {
    super(message, cause);
  }
}
```

Add complete Javadocs for every public type, component, method, exception condition, deterministic-resolver requirement, and `LevelingState` boundary semantic from the specification.

- [ ] **Step 4: Run API contracts and linkage checks**

Run:

```bash
./gradlew :modularjobs-api:test --tests dev.mintychochip.leveling.LevelingApiContractTest
./gradlew :modularjobs-api:test --tests dev.mintychochip.ApiSurfaceLinkageTest
```

Expected: both commands pass; no public signature references Paper, common, or paper implementation types.

- [ ] **Step 5: Commit the API value contracts**

```bash
git add modularjobs-api/src/main/java/dev/mintychochip/leveling \
  modularjobs-api/src/main/java/dev/mintychochip/exception/LevelingConfigurationException.java \
  modularjobs-api/src/test/java/dev/mintychochip/leveling/LevelingApiContractTest.java
git commit -m "feat(api): add leveling formula contracts"
```

---

### Task 2: Implement the Atomic Leveling Engine

**Files:**
- Create: `modularjobs-paper/src/main/java/dev/mintychochip/domain/LevelingEngine.java`
- Create: `modularjobs-paper/src/main/java/dev/mintychochip/domain/ExpressionLevelingFormula.java`
- Create: `modularjobs-paper/src/main/java/dev/mintychochip/domain/JobLevelingImpl.java`
- Create: `modularjobs-paper/src/test/java/dev/mintychochip/domain/LevelingEngineTest.java`

**Interfaces:**
- Consumes: Task 1 contracts; exp4j `Expression`, `ExpressionBuilder`, and the verified `Expression(Expression)` copy constructor.
- Produces: package-private `LevelingEngine#create(Job, String)`, `variables()`, and `register(FormulaVariable, LevelingVariableResolver)` for service and persistence wiring.

- [ ] **Step 1: Write failing formula, profile, boundary, and snapshot tests**

Create `LevelingEngineTest` in `dev.mintychochip.domain`. Use a `JobImpl` fixture with key `modularjobs:miner`, `Optional.empty()` parent, max level `10`, and the current legacy curve only as the temporary `Job` implementation requirement. Exercise the new engine with the raw expression passed separately.

Required test cases and assertions:

Compare numeric `BigDecimal` values by magnitude, not scale:

```java
private static void assertDecimal(String expected, BigDecimal actual) {
  assertEquals(0, new BigDecimal(expected).compareTo(actual));
}
```

```java
@Test
void builtInLevelVariableIsTypedInspectableAndEvaluated() {
  JobLeveling leveling = engine.create(job(10), "level * 100");

  assertEquals("level * 100", leveling.formula().expression());
  assertEquals(List.of("level"),
      leveling.formula().variables().stream().map(FormulaVariable::name).toList());
  assertEquals(FormulaVariableType.INTEGER, leveling.formula().variables().getFirst().type());
  assertDecimal("500", leveling.experienceForLevel(5));
  assertDecimal("5", leveling.formula().resolveVariables(5).get("level"));
}

@Test
void registeredDecimalVariableParticipatesInFormula() {
  FormulaVariable multiplier =
      new FormulaVariable(
          "prestige_multiplier",
          FormulaVariableType.DECIMAL,
          "Prestige XP multiplier",
          Key.key("example", "prestige"));
  engine.register(multiplier, context -> new BigDecimal("1.5"));

  JobLeveling leveling = engine.create(job(10), "level * 100 * prestige_multiplier");

  assertDecimal("750", leveling.experienceForLevel(5));
  assertEquals(List.of("level", "prestige_multiplier"),
      leveling.formula().variables().stream().map(FormulaVariable::name).toList());
}

@Test
void fractionalIntegerVariableIsRejectedWithVariableAndJobContext() {
  engine.register(
      new FormulaVariable("tier", FormulaVariableType.INTEGER, "Tier", Key.key("x", "tier")),
      context -> new BigDecimal("1.5"));

  LevelingConfigurationException failure =
      assertThrows(
          LevelingConfigurationException.class,
          () -> engine.create(job(10), "level * tier"));
  assertTrue(failure.getMessage().contains("tier"));
  assertTrue(failure.getMessage().contains("modularjobs:miner"));
}

@Test
void profileRejectsUnknownNegativeAndNonIncreasingThresholds() {
  assertThrows(LevelingConfigurationException.class, () -> engine.create(job(10), "level * missing"));
  assertThrows(LevelingConfigurationException.class, () -> engine.create(job(10), "-level"));
  assertThrows(LevelingConfigurationException.class, () -> engine.create(job(10), "100"));
}
```

Add boundary assertions for `levelForExperience` and `inspect`:

```java
JobLeveling leveling = engine.create(job(10), "level * 100");
assertEquals(1, leveling.levelForExperience(new BigDecimal("-50")));
assertEquals(1, leveling.levelForExperience(new BigDecimal("99")));
assertEquals(2, leveling.levelForExperience(new BigDecimal("200")));
assertEquals(10, leveling.levelForExperience(new BigDecimal("99999")));

LevelingState belowFirst = leveling.inspect(new BigDecimal("-50"));
assertDecimal("0", belowFirst.experienceIntoLevel());
assertDecimal("250", belowFirst.experienceToNextLevel().orElseThrow());
assertDecimal("0", belowFirst.progressToNextLevel().orElseThrow());

LevelingState middle = leveling.inspect(new BigDecimal("250"));
assertEquals(2, middle.level());
assertDecimal("50", middle.experienceIntoLevel());
assertDecimal("50", middle.experienceToNextLevel().orElseThrow());
assertDecimal("0.5", middle.progressToNextLevel().orElseThrow());

LevelingState exact = leveling.inspect(new BigDecimal("300"));
assertEquals(3, exact.level());
assertDecimal("0", exact.experienceIntoLevel());
assertDecimal("0", exact.progressToNextLevel().orElseThrow());
assertThrows(IllegalArgumentException.class, () -> leveling.experienceForLevel(0));
assertThrows(IllegalArgumentException.class, () -> leveling.experienceForLevel(11));

LevelingState max = leveling.inspect(new BigDecimal("1250"));
assertEquals(10, max.level());
assertTrue(max.nextLevelThreshold().isEmpty());
assertTrue(max.experienceToNextLevel().isEmpty());
assertDecimal("250", max.experienceIntoLevel());
```

Add an additive-snapshot test: capture `variables()` and a valid
`level * 100` profile, register `bonus`, then assert the captured list and old
profile remain unchanged while a newly created `level * bonus` profile exposes
and resolves `bonus`. Add a two-thread race using a fresh engine per iteration:
a query for `level * bonus` either fails before publication with
`LevelingConfigurationException` or returns a formula whose declaration,
resolved value, and evaluated result all include `bonus`; it must never return
a partial formula or throw `NullPointerException`/`ConcurrentModificationException`.

Also assert that `formula.variables()` and `resolveVariables(level)` reject
mutation, that max-level-one profiles return all next-level optionals empty,
and that registrations named `level`, `pi`, or an exp4j built-in function are
rejected before publication.

- [ ] **Step 2: Run the engine test and confirm missing implementation failures**

Run:

```bash
./gradlew :modularjobs-paper:test --tests dev.mintychochip.domain.LevelingEngineTest
```

Expected: test compilation fails because `LevelingEngine`, `ExpressionLevelingFormula`, and `JobLevelingImpl` do not exist.

- [ ] **Step 3: Implement catalog snapshots and expression evaluation**

`LevelingEngine` owns one `AtomicReference<CatalogSnapshot>`. Seed it with this declaration/binding:

```java
private static final FormulaVariable LEVEL =
    new FormulaVariable(
        "level",
        FormulaVariableType.INTEGER,
        "Target level being evaluated",
        Key.key("modularjobs", "core"));
```

The initial binding map contains
`new Binding(LEVEL, context -> BigDecimal.valueOf(context.targetLevel()))`;
its revision is zero and its profile cache is empty.

Use immutable insertion-ordered binding maps and a fresh `ConcurrentHashMap<ProfileKey, Computation>` for each published snapshot. Registration must validate before a compare-and-set loop and retry against the latest snapshot:

Keep the coordination types as package-visible static members of
`LevelingEngine`, so the two focused implementation classes can consume them
without adding top-level files:

```java
static record Binding(
    FormulaVariable declaration,
    LevelingVariableResolver resolver) {}

static record ProfileKey(Key jobKey, int maxLevel, String expression) {}

static record CatalogSnapshot(
    long revision,
    Map<String, Binding> bindings,
    ConcurrentMap<ProfileKey, Computation> profiles) {}

static final class Computation {
  private final LevelingFormula formula;
  private final BigDecimal[] thresholds;

  Computation(LevelingFormula formula, BigDecimal[] thresholds) {
    this.formula = formula;
    this.thresholds = thresholds.clone();
  }

  LevelingFormula formula() {
    return formula;
  }

  BigDecimal threshold(int level) {
    return thresholds[level];
  }
}
```

`ExpressionLevelingFormula` accepts the captured
`Map<String, LevelingEngine.Binding>`. `JobLevelingImpl` accepts a `Job` and
`LevelingEngine.Computation`; neither class reads the engine's current catalog
after construction.

```java
void register(FormulaVariable declaration, LevelingVariableResolver resolver) {
  Objects.requireNonNull(declaration, "declaration");
  Objects.requireNonNull(resolver, "resolver");
  validateRegisterableName(declaration.name());

  while (true) {
    CatalogSnapshot current = catalog.get();
    if (current.bindings().containsKey(declaration.name())) {
      throw new IllegalArgumentException("Leveling variable already registered: " + declaration.name());
    }
    LinkedHashMap<String, Binding> nextBindings = new LinkedHashMap<>(current.bindings());
    nextBindings.put(declaration.name(), new Binding(declaration, resolver));
    CatalogSnapshot next =
        new CatalogSnapshot(
            current.revision() + 1,
            Collections.unmodifiableMap(nextBindings),
            new ConcurrentHashMap<>());
    if (catalog.compareAndSet(current, next)) return;
  }
}
```

Reject `level`, exp4j constants (`pi`, `π`, `φ`, `e`), and built-in function names at registration. Validate function collisions by asking `ExpressionBuilder` to register the candidate as a variable and converting its rejection into `IllegalArgumentException` before publishing.

`ExpressionLevelingFormula` must:

1. Compile one template with every catalog variable declared.
2. Read `template.getVariableNames()` to identify variables actually used.
3. Ignore exp4j constants, reject every used name missing from the captured binding map, and order referenced bindings by first identifier occurrence in the raw expression.
4. Cache one immutable `Evaluation` per target level in a `ConcurrentHashMap<Integer, Evaluation>`.
5. Resolve every binding against `new LevelingContext(jobKey, maxLevel, targetLevel)`.
6. Validate integer values with `value.stripTrailingZeros().scale() <= 0`.
7. Evaluate with `new Expression(template)` so each invocation mutates only its private expression copy.
8. Reject non-finite double results before `BigDecimal.valueOf` and wrap parser/resolver failures in `LevelingConfigurationException` with job key, level, and variable where applicable.

Core evaluation shape:

```java
private Evaluation compute(int targetLevel) {
  LevelingContext context = new LevelingContext(jobKey, maxLevel, targetLevel);
  LinkedHashMap<String, BigDecimal> values = new LinkedHashMap<>();
  for (Binding binding : referencedBindings) {
    BigDecimal value = resolveAndValidate(binding, context);
    values.put(binding.declaration().name(), value);
  }

  Expression expressionCopy = new Expression(template);
  values.forEach((name, value) -> expressionCopy.setVariable(name, value.doubleValue()));
  double evaluated = expressionCopy.evaluate();
  if (!Double.isFinite(evaluated)) {
    throw configurationFailure(targetLevel, "formula returned a non-finite value", null);
  }
  return new Evaluation(
      Collections.unmodifiableMap(values), BigDecimal.valueOf(evaluated));
}
```

`variables()` returns `List.copyOf` referenced declarations. `resolveVariables` returns the cached immutable map. `evaluate` returns the cached result.

- [ ] **Step 4: Implement validated thresholds and state derivation**

`LevelingEngine#create(Job, String)` captures `catalog.get()` once, keys that snapshot's cache by `(job.key(), job.maxLevel(), expression)`, and computes one `Computation` containing the formula and a `BigDecimal[]` threshold table. Return a lightweight `JobLevelingImpl` bound to the caller's `Job` and that shared computation; do not cache the `Job` object itself.

Threshold construction must evaluate indices `1..maxLevel`, reject `maxLevel < 1`, reject negative thresholds, and reject `threshold[level] <= threshold[level - 1]`.

Implement binary search and state calculations with these exact boundaries:

```java
@Override
public int levelForExperience(BigDecimal experience) {
  Objects.requireNonNull(experience, "experience");
  int low = 1;
  int high = job.maxLevel();
  int result = 1;
  while (low <= high) {
    int mid = (low + high) >>> 1;
    if (experience.compareTo(thresholds[mid]) >= 0) {
      result = mid;
      low = mid + 1;
    } else {
      high = mid - 1;
    }
  }
  return result;
}
```

For non-max state, calculate interval width, clamp `experienceIntoLevel` to `[0, width]`, calculate actual remaining XP as `max(0, nextThreshold - experience)`, and calculate DECIMAL128 normalized progress clamped to `[0, 1]`. For max state, return empty next-level optionals and `max(0, experience - currentThreshold)` as `experienceIntoLevel`.

- [ ] **Step 5: Run engine tests repeatedly, including the race**

Run:

```bash
./gradlew :modularjobs-paper:test --tests dev.mintychochip.domain.LevelingEngineTest
./gradlew :modularjobs-paper:test --tests dev.mintychochip.domain.LevelingEngineTest --rerun-tasks
```

Expected: both runs pass; the race accepts only complete pre-publication failure or complete post-publication success.

- [ ] **Step 6: Commit the engine**

```bash
git add modularjobs-paper/src/main/java/dev/mintychochip/domain/LevelingEngine.java \
  modularjobs-paper/src/main/java/dev/mintychochip/domain/ExpressionLevelingFormula.java \
  modularjobs-paper/src/main/java/dev/mintychochip/domain/JobLevelingImpl.java \
  modularjobs-paper/src/test/java/dev/mintychochip/domain/LevelingEngineTest.java
git commit -m "feat: add service-owned leveling engine"
```

---

### Task 3: Expose Leveling Through `JobService`

**Files:**
- Modify: `modularjobs-api/src/main/java/dev/mintychochip/service/JobService.java:22-128`
- Modify: `modularjobs-paper/src/main/java/dev/mintychochip/domain/JobServiceImpl.java:52-252`
- Modify: `modularjobs-paper/src/main/java/dev/mintychochip/domain/DomainWiring.java:91-106`
- Create: `modularjobs-paper/src/test/java/dev/mintychochip/domain/JobServiceLevelingTest.java`
- Modify test fakes: `modularjobs-paper/src/test/java/dev/mintychochip/domain/JobResolverImplTest.java:124-195`
- Modify test fakes: `modularjobs-paper/src/test/java/dev/mintychochip/payment/JobsPaymentHandlerReloadTest.java:385-455`
- Modify test fakes: `modularjobs-paper/src/test/java/dev/mintychochip/profession/config/CraftRecipeContentValidatorTest.java:121-210`

**Interfaces:**
- Consumes: `LevelingEngine` from Task 2.
- Produces: `JobService#getLeveling(Key)`, `getLevelingVariables()`, and `registerLevelingVariable(FormulaVariable, LevelingVariableResolver)`; later callers use no engine-specific API.

- [ ] **Step 1: Write a failing service-surface test**

Create a namespaced `JobRecord` with expression `level * 100 * multiplier`, empty payable curves, max level `10`, and parent `null`. Construct `JobServiceImpl` in the domain package with a `MemoryJobRepositoryImpl`, empty `SimpleRegistryImpl<PayableType>`, a `LevelingEngine`, and `null` only for collaborators unused by leveling lookup.

```java
@Test
void servicePublishesVariablesAndQueriesCanonicalProfile() {
  JobService service = serviceWithExpression("level * 100 * multiplier");
  FormulaVariable multiplier =
      new FormulaVariable(
          "multiplier", FormulaVariableType.DECIMAL, "Server multiplier", Key.key("test", "suite"));

  service.registerLevelingVariable(multiplier, context -> new BigDecimal("1.25"));

  assertEquals(List.of("level", "multiplier"),
      service.getLevelingVariables().stream().map(FormulaVariable::name).toList());
  JobLeveling leveling = service.getLeveling(Key.key("modularjobs", "miner"));
  assertEquals(0, new BigDecimal("625").compareTo(leveling.experienceForLevel(5)));
  assertEquals(FormulaVariableType.DECIMAL, leveling.formula().variables().get(1).type());
}

@Test
void unknownJobAndDuplicateVariableAreRejected() {
  JobService service = serviceWithExpression("level * 100");
  assertThrows(
      IllegalArgumentException.class,
      () -> service.getLeveling(Key.key("modularjobs", "missing")));

  FormulaVariable value =
      new FormulaVariable("bonus", FormulaVariableType.INTEGER, "Bonus", Key.key("test", "suite"));
  service.registerLevelingVariable(value, context -> BigDecimal.ONE);
  assertThrows(
      IllegalArgumentException.class,
      () -> service.registerLevelingVariable(value, context -> BigDecimal.TEN));
}
```

- [ ] **Step 2: Add the public service methods and confirm compilation fails in Paper**

Add fully documented abstract methods to `JobService`:

```java
@NotNull JobLeveling getLeveling(@NotNull Key jobKey);
@NotNull List<FormulaVariable> getLevelingVariables();
void registerLevelingVariable(
    @NotNull FormulaVariable declaration,
    @NotNull LevelingVariableResolver resolver);
```

Run:

```bash
./gradlew :modularjobs-paper:testClasses
```

Expected: compilation fails at `JobServiceImpl` and concrete test fakes because the new abstract methods are unimplemented.

- [ ] **Step 3: Wire the engine and implement service delegation**

Add a non-null `LevelingEngine` constructor dependency and field to `JobServiceImpl`. Implement:

```java
@Override
public @NotNull JobLeveling getLeveling(@NotNull Key jobKey) {
  JobRecord record = jobRepository.load(jobKey.asString());
  if (record == null) throw new IllegalArgumentException("Unknown job: " + jobKey);
  Job job = PersistenceConverters.fromRecord(record, plugin, payableTypeRegistry);
  return levelingEngine.create(job, record.levellingCurve());
}

@Override
public @NotNull List<FormulaVariable> getLevelingVariables() {
  return levelingEngine.variables();
}

@Override
public void registerLevelingVariable(
    @NotNull FormulaVariable declaration,
    @NotNull LevelingVariableResolver resolver) {
  levelingEngine.register(declaration, resolver);
}
```

In `DomainWiring.create`, construct exactly one `LevelingEngine` and pass that instance into `JobServiceImpl`. Do not expose the engine from `DomainWiring` unless a later constructor genuinely consumes it.

Replace new-join curve evaluation with a profile built from the already-loaded record:

```java
JobLeveling leveling = levelingEngine.create(job, jobRecord.levellingCurve());
BigDecimal startExperience = leveling.experienceForLevel(1);
```

- [ ] **Step 4: Update concrete `JobService` test fakes**

In `FakeJobService` and both `StubJobService` classes, implement the three new methods explicitly. Fakes that do not exercise leveling should throw `UnsupportedOperationException` for `getLeveling` and registration and return `List.of()` for `getLevelingVariables`; do not add defaults to the production interface merely to satisfy tests.

- [ ] **Step 5: Run service, API, and affected fake-compilation tests**

Run:

```bash
./gradlew :modularjobs-api:test
./gradlew :modularjobs-paper:test --tests dev.mintychochip.domain.JobServiceLevelingTest
./gradlew :modularjobs-paper:test --tests dev.mintychochip.domain.JobResolverImplTest
./gradlew :modularjobs-paper:test --tests dev.mintychochip.payment.JobsPaymentHandlerReloadTest
./gradlew :modularjobs-paper:test --tests dev.mintychochip.profession.config.CraftRecipeContentValidatorTest
```

Expected: all pass; service lookup and new-join source now obtain level-1 XP
from the same service-owned profile implementation.

- [ ] **Step 6: Commit the service surface**

```bash
git add modularjobs-api/src/main/java/dev/mintychochip/service/JobService.java \
  modularjobs-paper/src/main/java/dev/mintychochip/domain/JobServiceImpl.java \
  modularjobs-paper/src/main/java/dev/mintychochip/domain/DomainWiring.java \
  modularjobs-paper/src/test/java/dev/mintychochip/domain/JobServiceLevelingTest.java \
  modularjobs-paper/src/test/java/dev/mintychochip/domain/JobResolverImplTest.java \
  modularjobs-paper/src/test/java/dev/mintychochip/payment/JobsPaymentHandlerReloadTest.java \
  modularjobs-paper/src/test/java/dev/mintychochip/profession/config/CraftRecipeContentValidatorTest.java
git commit -m "feat(api): expose job leveling service"
```

---

### Task 4: Centralize Progression Mapping and Level Derivation

**Files:**
- Modify: `modularjobs-paper/src/main/java/dev/mintychochip/domain/JobProgressionImpl.java:16-157`
- Modify: `modularjobs-paper/src/main/java/dev/mintychochip/domain/PersistenceConverters.java:40-84`
- Modify: `modularjobs-paper/src/main/java/dev/mintychochip/domain/JobServiceImpl.java:94-252`
- Modify: `modularjobs-paper/src/test/java/dev/mintychochip/domain/JobProgressionLevelTest.java:23-131`
- Create: `modularjobs-paper/src/test/java/dev/mintychochip/domain/ProgressionLevelingSnapshotTest.java`

**Interfaces:**
- Consumes: `LevelingEngine#create(Job, String)` and `JobLeveling`.
- Produces: every reconstructed `JobProgressionImpl` carries one immutable `JobLeveling` snapshot; `setExperience` reuses it.

- [ ] **Step 1: Rewrite progression tests against `JobLeveling` and add a snapshot failure**

Change the test fixture to create one engine/profile and construct progressions with:

```java
private LevelingEngine engine;
private JobLeveling jobLeveling;

@BeforeEach
void setUp() {
  MockBukkitSupport.mockServer();
  engine = new LevelingEngine();
  job = job("level * 100", 10);
  jobLeveling = engine.create(job, "level * 100");
}

private JobProgression progression(BigDecimal experience) {
  return new JobProgressionImpl(PLAYER_ID, jobLeveling, experience);
}
```

Replace `maxLevelZeroOrNegativeDefaultsToLevelOne` with an assertion that profile construction rejects max level zero, matching the approved invariant.

Add `ProgressionLevelingSnapshotTest` that reconstructs a `JobProgressionRecord` whose embedded formula is `level * 100`, creates a separate current-job profile with the same key and `level * 1000`, and proves the persisted progression still derives level 3 at 350 XP and returns 500 XP for level 5. Call `setExperience` and assert the new snapshot still returns the embedded formula's thresholds.

- [ ] **Step 2: Run progression tests and confirm constructor/mapping failures**

Run:

```bash
./gradlew :modularjobs-paper:test --tests dev.mintychochip.domain.JobProgressionLevelTest
./gradlew :modularjobs-paper:test --tests dev.mintychochip.domain.ProgressionLevelingSnapshotTest
```

Expected: compilation fails because `JobProgressionImpl` still accepts a bare `Job` and persistence mapping has no engine.

- [ ] **Step 3: Make `JobProgressionImpl` own a leveling snapshot**

Replace the `Job` field with `JobLeveling` and replace the constructor with:

```java
JobProgressionImpl(
    @NotNull UUID playerId,
    @NotNull JobLeveling jobLeveling,
    @NotNull BigDecimal experience) {
  this.playerId = Objects.requireNonNull(playerId, "playerId");
  this.jobLeveling = Objects.requireNonNull(jobLeveling, "jobLeveling");
  this.experience = Objects.requireNonNull(experience, "experience");
  this.level = jobLeveling.levelForExperience(experience);
}
```

Delegate public behavior:

```java
@Override public @NotNull BigDecimal experienceForLevel(int level) {
  return jobLeveling.experienceForLevel(level);
}

@Override public @NotNull Job job() {
  return jobLeveling.job();
}

@Override public @NotNull JobProgression setExperience(@NotNull BigDecimal experience) {
  if (this.experience.equals(experience)) return this;
  return new JobProgressionImpl(playerId, jobLeveling, experience);
}
```

Delete the local binary search. `toRecord()` must serialize `jobLeveling.job()` through `JobImpl`, preserving the same embedded job snapshot.

- [ ] **Step 4: Thread the engine through persistence conversion exactly once**

Change progression reconstruction to accept `LevelingEngine`:

```java
static @NotNull JobProgressionImpl fromRecord(
    @NotNull JobProgressionRecord record,
    @NotNull Plugin plugin,
    @NotNull Registry<PayableType> payableTypeRegistry,
    @NotNull LevelingEngine levelingEngine) {
  Job job = JobImpl.fromRecord(record.jobRecord(), plugin, payableTypeRegistry);
  JobLeveling leveling =
      levelingEngine.create(job, record.jobRecord().levellingCurve());
  return new JobProgressionImpl(
      UUID.fromString(record.playerId()), leveling, record.experience());
}
```

Update `PersistenceConverters.fromRecord(JobProgressionRecord, ...)` to require and pass the engine. In `JobServiceImpl`, add one private `toProgression(JobProgressionRecord)` helper and replace every progression mapping call with it. Keep plain `JobRecord -> Job` conversion engine-free.

- [ ] **Step 5: Run progression and service tests**

Run:

```bash
./gradlew :modularjobs-paper:test --tests dev.mintychochip.domain.JobProgressionLevelTest
./gradlew :modularjobs-paper:test --tests dev.mintychochip.domain.ProgressionLevelingSnapshotTest
./gradlew :modularjobs-paper:test --tests dev.mintychochip.domain.JobServiceLevelingTest
./gradlew :modularjobs-paper:test --tests dev.mintychochip.domain.JobResolverImplTest
```

Expected: all pass. `JobProgressionImpl` contains no direct expression parsing or binary search.

- [ ] **Step 6: Commit progression centralization**

```bash
git add modularjobs-paper/src/main/java/dev/mintychochip/domain/JobProgressionImpl.java \
  modularjobs-paper/src/main/java/dev/mintychochip/domain/PersistenceConverters.java \
  modularjobs-paper/src/main/java/dev/mintychochip/domain/JobServiceImpl.java \
  modularjobs-paper/src/test/java/dev/mintychochip/domain/JobProgressionLevelTest.java \
  modularjobs-paper/src/test/java/dev/mintychochip/domain/ProgressionLevelingSnapshotTest.java
git commit -m "refactor: centralize progression leveling"
```

---

### Task 5: Migrate Admin Levels and Stats Display

**Files:**
- Modify: `modularjobs-paper/src/main/java/dev/mintychochip/commands/LevelCommand.java:129-481`
- Modify: `modularjobs-paper/src/main/java/dev/mintychochip/gui/StatsGui.java:147-164`
- Modify: `modularjobs-paper/src/test/java/dev/mintychochip/gui/NativePaperGuiTest.java:151-209`

**Interfaces:**
- Consumes: `JobProgression.level()`, `experienceForLevel(int)`, and `Job.maxLevel()`.
- Produces: commands and GUI never evaluate or inspect a job-owned curve.

- [ ] **Step 1: Make the stats test prove progression owns the threshold**

In the `progression(...)` proxy, continue returning `BigDecimal.valueOf(100)` from `experienceForLevel`. Make the fixture job's temporary legacy curve throw `AssertionError("StatsGui must use JobProgression.experienceForLevel")`. Add a plain-text lore helper and assert:

```java
assertTrue(lore(last, 19).contains("Next level: 100.0"));
```

Add a max-level progression fixture (`level() == job().maxLevel()`) and assert its lore does not contain `Next level:`.

- [ ] **Step 2: Run the GUI test and confirm it hits the forbidden job curve**

Run:

```bash
./gradlew :modularjobs-paper:test --tests dev.mintychochip.gui.NativePaperGuiTest
```

Expected: the stats test fails with `AssertionError: StatsGui must use JobProgression.experienceForLevel`.

- [ ] **Step 3: Delegate Stats GUI threshold rendering to progression**

Replace the exception-driven curve block with an explicit max-level guard:

```java
if (level < prog.job().maxLevel()) {
  BigDecimal next = prog.experienceForLevel(level + 1);
  lore.add("Next level: " + next.setScale(1, RoundingMode.HALF_UP).toPlainString());
}
```

Do not catch formula exceptions in the GUI; invalid profiles fail before a progression is published.

- [ ] **Step 4: Use the loaded progression snapshot in every level mutation**

In `executeSet`, `executeAdd`, and `executeSubtract`, after confirming the raw record exists, obtain the canonical progression once:

```java
JobProgression current = jobService.getProgression(playerId, jobKey.toString());
if (current == null) {
  Messages.send(sender, "<error>Unable to load the player's job progression.</error>");
  return 0;
}
```

Use `current.level()` instead of scanning all progressions/defaulting to one, and use:

```java
BigDecimal requiredExperience = current.experienceForLevel(targetLevel);
```

or the calculated `newLevel`. Keep persisting `currentRecord.jobRecord()` so the record and threshold come from the same embedded job snapshot. Remove `dev.mintychochip.LevelingCurve` references and now-unused `List<JobProgression>` calculations/imports.

- [ ] **Step 5: Run GUI and progression behavior tests**

Run:

```bash
./gradlew :modularjobs-paper:test --tests dev.mintychochip.gui.NativePaperGuiTest
./gradlew :modularjobs-paper:test --tests dev.mintychochip.domain.JobProgressionLevelTest
./gradlew :modularjobs-paper:test --tests dev.mintychochip.domain.ProgressionLevelingSnapshotTest
```

Expected: all pass; stats reads the proxy progression threshold, max-level lore omits the next threshold, and progression snapshots retain their formula.

- [ ] **Step 6: Commit caller migration**

```bash
git add modularjobs-paper/src/main/java/dev/mintychochip/commands/LevelCommand.java \
  modularjobs-paper/src/main/java/dev/mintychochip/gui/StatsGui.java \
  modularjobs-paper/src/test/java/dev/mintychochip/gui/NativePaperGuiTest.java
git commit -m "refactor: use progression leveling data"
```

---

### Task 6: Remove the Legacy Job Curve Cutover

**Files:**
- Modify: `modularjobs-api/src/main/java/dev/mintychochip/Job.java:12-76`
- Delete: `modularjobs-api/src/main/java/dev/mintychochip/LevelingCurve.java`
- Modify: `modularjobs-api/src/test/java/dev/mintychochip/ApiOnlyConsumerTest.java:70-124`
- Modify: `modularjobs-paper/src/main/java/dev/mintychochip/domain/JobImpl.java:24-119`
- Modify: `modularjobs-paper/src/main/java/dev/mintychochip/math/ExpressionCurves.java:1-68`
- Modify: `modularjobs-paper/src/test/java/dev/mintychochip/math/ExpressionCurvesTest.java:1-77`
- Modify: `modularjobs-paper/src/test/java/dev/mintychochip/domain/JobProgressionLevelTest.java`
- Modify: `modularjobs-paper/src/test/java/dev/mintychochip/domain/JobResolverImplTest.java:110-121`
- Modify: `modularjobs-paper/src/test/java/dev/mintychochip/gui/NativePaperGuiTest.java:182-196`
- Modify: `modularjobs-paper/src/test/java/dev/mintychochip/gui/NativeUpgradeGuiTest.java:404-414`
- Modify: `modularjobs-paper/src/test/java/dev/mintychochip/payment/JobsPaymentHandlerReloadTest.java:280-305`
- Modify: `modularjobs-paper/src/test/java/dev/mintychochip/profession/config/CraftRecipeContentValidatorTest.java:80-105`

**Interfaces:**
- Consumes: all service/profile migrations from Tasks 3–5.
- Produces: `Job` is metadata-only; raw expressions remain internal; no source/test references the deleted type.

- [ ] **Step 1: Add a failing public-surface assertion**

In `ApiOnlyConsumerTest`, remove `ApiOnlyJob.levelingCurve()` and add:

```java
@Test
void jobDoesNotExposeLevelingImplementation() {
  assertFalse(
      Arrays.stream(Job.class.getMethods())
          .anyMatch(method -> method.getName().equals("levelingCurve")));
}
```

Run:

```bash
./gradlew :modularjobs-api:test --tests dev.mintychochip.ApiOnlyConsumerTest
```

Expected: test compilation fails because `ApiOnlyJob` no longer implements the still-abstract method, or the reflection assertion fails before that override is removed.

- [ ] **Step 2: Remove the public accessor and store raw expressions internally**

Delete `Job.levelingCurve()` and its Javadoc while preserving `parentKey()` and every other current method.

Change the `JobImpl` record component from:

```java
@NotNull LevelingCurve levelingCurve
```

to:

```java
@NotNull String levelingExpression
```

Keep it internal to the package-private record. `toRecord()` writes
`levelingExpression()` directly. `fromRecord()` passes `record.levellingCurve()` directly. Preserve the new `Optional<Key> parentKey` component and its serialization exactly.

Delete `modularjobs-api/src/main/java/dev/mintychochip/LevelingCurve.java`.

- [ ] **Step 3: Remove leveling behavior from `ExpressionCurves`**

Delete `LEVELING_CACHE`, the `LevelingCurve` import, and `levelingCurve(String)`. Keep `PAYABLE_CACHE` and `payableCurve(String)` unchanged. In `ExpressionCurvesTest`, remove only leveling tests and their imports/assertions; retain payable evaluation and same-expression caching coverage.

- [ ] **Step 4: Migrate every fixture and constructor**

- Pass raw expressions such as `"level * 100"` to `JobImpl` constructors in domain tests.
- Remove `LevelingCurve` imports, fields, and overrides from anonymous `Job` fixtures.
- Keep `parentKey()` fixtures and lineage assertions untouched.
- In the stats fixture, the Task 5 behavioral proof remains valid because its `JobProgression` proxy supplies `experienceForLevel`; after accessor deletion no throwing curve is needed.
- Keep new `JobService` method implementations added to test fakes in Task 3.

- [ ] **Step 5: Prove the old API and evaluator are gone**

Use repository search over `modularjobs-api/src` and `modularjobs-paper/src` for both `LevelingCurve` and `levelingCurve`. Expected: zero matches. Historical design/plan documentation may retain the old names when explaining the migration.

Run:

```bash
./gradlew :modularjobs-api:test
./gradlew :modularjobs-paper:test --tests dev.mintychochip.domain.LevelingEngineTest
./gradlew :modularjobs-paper:test --tests dev.mintychochip.domain.JobProgressionLevelTest
./gradlew :modularjobs-paper:test --tests dev.mintychochip.domain.JobResolverImplTest
./gradlew :modularjobs-paper:test --tests dev.mintychochip.gui.NativePaperGuiTest
./gradlew :modularjobs-paper:test --tests dev.mintychochip.gui.NativeUpgradeGuiTest
./gradlew :modularjobs-paper:test --tests dev.mintychochip.payment.JobsPaymentHandlerReloadTest
./gradlew :modularjobs-paper:test --tests dev.mintychochip.profession.config.CraftRecipeContentValidatorTest
./gradlew :modularjobs-paper:test --tests dev.mintychochip.math.ExpressionCurvesTest
```

Expected: all pass with no compatibility method or legacy type.

- [ ] **Step 6: Commit the clean cutover**

```bash
git add modularjobs-api/src/main/java/dev/mintychochip/Job.java \
  modularjobs-api/src/main/java/dev/mintychochip/LevelingCurve.java \
  modularjobs-api/src/test/java/dev/mintychochip/ApiOnlyConsumerTest.java \
  modularjobs-paper/src/main/java/dev/mintychochip/domain/JobImpl.java \
  modularjobs-paper/src/main/java/dev/mintychochip/math/ExpressionCurves.java \
  modularjobs-paper/src/test/java/dev/mintychochip/math/ExpressionCurvesTest.java \
  modularjobs-paper/src/test/java/dev/mintychochip/domain/JobProgressionLevelTest.java \
  modularjobs-paper/src/test/java/dev/mintychochip/domain/JobResolverImplTest.java \
  modularjobs-paper/src/test/java/dev/mintychochip/gui/NativePaperGuiTest.java \
  modularjobs-paper/src/test/java/dev/mintychochip/gui/NativeUpgradeGuiTest.java \
  modularjobs-paper/src/test/java/dev/mintychochip/payment/JobsPaymentHandlerReloadTest.java \
  modularjobs-paper/src/test/java/dev/mintychochip/profession/config/CraftRecipeContentValidatorTest.java
git commit -m "refactor(api): remove job leveling curve"
```

---

### Task 7: Document and Verify the Complete API Cutover

**Files:**
- Modify: `web/fumadocs/content/docs/develop/api.mdx:29-47`
- Modify: `docs/living-specs/jobs-progression.md:1-73`
- Modify: `CHANGELOG.md:3-28`

**Interfaces:**
- Consumes: final public signatures from Tasks 1, 3, and 6.
- Produces: accurate developer/user documentation and release notes; full-module proof.

- [ ] **Step 1: Update developer API documentation with executable signatures**

Replace the core-model table's `LevelingCurve` row and `Job.levelingCurve()` text with `JobLeveling`, `LevelingFormula`, `LevelingState`, `FormulaVariable`, and the `JobService` operations. Preserve the newly documented `Job.parentKey()` behavior.

Add an example using the exact final API:

```java
JobService jobs = Bridge.bridge().jobService();

jobs.registerLevelingVariable(
    new FormulaVariable(
        "prestige_multiplier",
        FormulaVariableType.DECIMAL,
        "Configured prestige multiplier",
        Key.key("example", "prestige")),
    context -> new BigDecimal("1.25"));

JobLeveling leveling = jobs.getLeveling(Key.key("modularjobs", "miner"));
leveling.formula().variables();
leveling.formula().resolveVariables(25);
leveling.experienceForLevel(25);
leveling.inspect(new BigDecimal("50000"));
```

Document `level: INTEGER`, additive global names, provider metadata, deterministic context restrictions, strict threshold validation, and the distinction between the global available catalog and a formula's referenced declarations.

- [ ] **Step 2: Update living specification and changelog**

In `docs/living-specs/jobs-progression.md`:

- set `Last updated` to `2026-08-31`;
- add an invariant that all threshold/level/progress derivation flows through service-owned `JobLeveling` snapshots;
- change implementation guidance from generic curve tests to pure `JobLeveling`/formula engine tests; and
- add a checked Current item for typed deterministic leveling variables and service-owned formula inspection.

In `CHANGELOG.md` under Unreleased:

- Added: typed formula-variable declarations/resolvers and `JobService` leveling inspection.
- Changed: canonical threshold, level, and derived XP state now come from immutable `JobLeveling` snapshots.
- Breaking: removed `Job.levelingCurve()` and `LevelingCurve`; integrations must query `JobService.getLeveling(Key)` and register deterministic variables through the service.

- [ ] **Step 3: Verify documentation**

Run:

```bash
npm --prefix web/fumadocs test
npm --prefix web/fumadocs run build
```

Expected: documentation verification and the production Fumadocs build pass.

- [ ] **Step 4: Run final Java verification**

Run:

```bash
./gradlew :modularjobs-api:test :modularjobs-common:test :modularjobs-paper:test
./gradlew :modularjobs-paper:build
```

Expected: all tests pass and the Paper shadow artifact builds. MySQL integration tests may use their existing explicit skip behavior when the configured test server is unavailable; no new leveling test requires MySQL.

- [ ] **Step 5: Confirm the clean-cutover acceptance conditions**

Verify all of the following before claiming completion:

- source/test search finds no `LevelingCurve` or `levelingCurve`;
- `Job` still exposes `parentKey()` and no formula accessor;
- `JobService` exposes all three leveling operations;
- formula metadata includes name, type, description, and provider;
- existing profiles remain stable after additive registration;
- persisted progressions use their embedded expression snapshot;
- negative and over-cap XP match `LevelingState` semantics;
- no payable-curve code changed beyond imports made obsolete by deleting leveling support; and
- only task-owned files are staged for the final commit.

- [ ] **Step 6: Commit documentation**

```bash
git add web/fumadocs/content/docs/develop/api.mdx \
  docs/living-specs/jobs-progression.md \
  CHANGELOG.md
git commit -m "docs: document service-owned leveling formulas"
```
