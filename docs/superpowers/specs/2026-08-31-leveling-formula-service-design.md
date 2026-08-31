# Service-Owned Leveling Formulas and Typed Variables

> Status: proposed
> Date: 2026-08-31
> Owners: modularjobs maintainers

## Problem

`Job.levelingCurve()` exposes an opaque evaluator as job metadata. Although
`LevelingCurve` is a functional interface, its public contract exposes only
`evaluate(Parameters)`, where `Parameters` contains only `level`. Consumers
cannot inspect the configured expression, discover its variables or their
value types, explain resolved inputs, or extend the formula with deterministic
variables.

The implementation also relies on behavior outside that contract:

- `JobImpl.toRecord()` calls `levelingCurve.toString()` to recover the stored
  expression.
- `ExpressionCurves` shares a mutable exp4j `Expression` while evaluating
  different parameter values.
- `JobProgressionImpl`, `JobServiceImpl`, `LevelCommand`, and `StatsGui`
  evaluate `job.levelingCurve()` directly. Threshold and level derivation are
  therefore distributed rather than owned by one service.
- The current binary search assumes increasing thresholds but no component
  validates that invariant.

Removing the accessor without centralizing those calculations would only
create alternate backdoors. The replacement must own formula inspection,
variable registration, threshold evaluation, level derivation, and derived XP
state.

## Goals

1. Keep leveling evaluation a deterministic function of immutable inputs.
2. Remove leveling formulas from the public `Job` metadata contract.
3. Expose configured expressions and immutable typed variable declarations.
4. Let integrations register deterministic numeric variable resolvers.
5. Give callers canonical level thresholds, level derivation, and progress
   data without reimplementing curve math.
6. Make formula metadata and runtime evaluation use the same atomically
   published variable-catalog snapshot.
7. Preserve progression snapshots: a progression continues to evaluate the
   job formula embedded in its persisted `JobRecord`.
8. Eliminate shared mutable expression evaluation.

## Non-goals

- Player-, world-, permission-, time-, or other mutable-state-dependent
  leveling thresholds.
- Replacing exp4j or adding strings, enums, or arbitrary Java values to the
  expression language.
- Full expression ASTs, intermediate-operation traces, or formula rewriting.
- Runtime replacement or removal of registered variables.
- Whole-formula replacement by integrations.
- Redesigning payable curves.
- Adding a leveling-formula editing surface to `web/session-editor`. The public
  metadata is intentionally transport-safe so a later editor change can
  serialize it without exposing resolvers.

## Public API

New leveling contracts live in `dev.mintychochip.leveling`. They contain no
Paper types.

### Immutable evaluation context

```java
public record LevelingContext(
    @NotNull Key jobKey,
    int maxLevel,
    int targetLevel
) {}
```

The context contains only immutable job facts needed during formula
resolution. It deliberately excludes `Player`, `UUID`, service references,
and mutable server state. `targetLevel` is the level whose total experience
threshold is being evaluated.

### Typed variable declarations

```java
public enum FormulaVariableType {
  INTEGER,
  DECIMAL
}

public record FormulaVariable(
    @NotNull String name,
    @NotNull FormulaVariableType type,
    @NotNull String description,
    @NotNull Key provider
) {}
```

A declaration is metadata, not an executable resolver. It is safe to expose to
API consumers and serialize for user-facing tools. `provider` identifies the
plugin or subsystem that registered the variable; it does not become part of
the expression token.

Variable names use the conservative expression identifier grammar
`[A-Za-z_][A-Za-z0-9_]*`. Names are globally unique because exp4j expressions
refer to unqualified identifiers. ModularJobs reserves the built-in name
`level`:

| Name | Type | Provider | Meaning |
|------|------|----------|---------|
| `level` | `INTEGER` | `modularjobs:core` | Target level being evaluated |

All runtime values are `BigDecimal`. `INTEGER` is a semantic type used for
validation, documentation, autocomplete, and editor presentation; its
resolver must return a mathematically integral `BigDecimal`. `DECIMAL` accepts
any finite `BigDecimal`. The expression engine continues to convert values to
`double` at its boundary, matching current exp4j precision.

### Pure variable resolution

```java
@FunctionalInterface
public interface LevelingVariableResolver {
  @NotNull BigDecimal resolve(@NotNull LevelingContext context);
}
```

Resolvers must be deterministic and side-effect free for an equal context.
The service validates non-null results and the declared semantic type.

### Formula descriptor

```java
public interface LevelingFormula {
  @NotNull String expression();

  @NotNull List<FormulaVariable> variables();

  @NotNull Map<String, BigDecimal> resolveVariables(int targetLevel);

  @NotNull BigDecimal evaluate(int targetLevel);
}
```

`variables()` contains only declarations referenced by this expression, in
stable expression-discovery order. `resolveVariables(targetLevel)` and
`evaluate(targetLevel)` construct a `LevelingContext` from the formula's bound
job key and max level. Callers cannot evaluate a job's formula with another
job's context. Resolvers and metadata come from the same immutable catalog
snapshot. Returned lists and maps are immutable. The formula object is itself
immutable and safe for concurrent use.

The raw expression remains public because inspection, diagnostics, and future
editor validation require it. Parser or exp4j objects are not exposed.

### Canonical job-leveling view

```java
public interface JobLeveling {
  @NotNull Job job();

  @NotNull LevelingFormula formula();

  @NotNull BigDecimal experienceForLevel(int level);

  int levelForExperience(@NotNull BigDecimal experience);

  @NotNull LevelingState inspect(@NotNull BigDecimal experience);
}
```

`JobLeveling` is an immutable value object bound to one `Job`, its raw formula,
and one variable-catalog snapshot. It precomputes the threshold for every level
from `1` through `job.maxLevel()` once. `experienceForLevel` is therefore a
constant-time table lookup, and `levelForExperience` binary-searches the same
immutable table.

Construction validates:

- `maxLevel >= 1`;
- every threshold is non-negative;
- thresholds are strictly increasing from level 1 through max level; and
- every referenced variable has a binding in the captured catalog.

Invalid formulas fail when the `JobLeveling` value is constructed; no partial
profile or default value is published.

### Derived leveling state

```java
public record LevelingState(
    int level,
    @NotNull BigDecimal experience,
    @NotNull BigDecimal levelThreshold,
    @NotNull Optional<BigDecimal> nextLevelThreshold,
    @NotNull BigDecimal experienceIntoLevel,
    @NotNull Optional<BigDecimal> experienceToNextLevel,
    @NotNull Optional<BigDecimal> progressToNextLevel
) {}
```

Semantics are explicit:

- `level` is `levelForExperience(experience)`, clamped to `[1, maxLevel]`.
- `experience` is the caller's unmodified value, including negative or
  above-cap values.
- `levelThreshold` is the total XP threshold for `level`.
- Below level 1's threshold, level remains 1 and `experienceIntoLevel` is zero.
- Below max level, `experienceIntoLevel` is
  `clamp(experience - levelThreshold, 0, nextThreshold - levelThreshold)`.
- Below max level, `experienceToNextLevel` is
  `max(0, nextThreshold - experience)`. When experience is below the level-1
  threshold, this can exceed the width of the level-1 progress interval because
  it reports the actual XP still required to reach level 2.
- Below max level, `progressToNextLevel` is
  `clamp((experience - levelThreshold) / (nextThreshold - levelThreshold),
  0, 1)`, calculated with `MathContext.DECIMAL128`.
- At max level, all three next-level optionals are empty.
  `experienceIntoLevel` is `max(0, experience - levelThreshold)`, preserving
  earned XP above the final threshold without presenting it as next-level
  progress.

Strictly increasing thresholds guarantee a positive progress denominator.
Exact next-level thresholds derive the next level before state is returned.

### `JobService` operations

```java
@NotNull JobLeveling getLeveling(@NotNull Key jobKey);

@NotNull List<FormulaVariable> getLevelingVariables();

void registerLevelingVariable(
    @NotNull FormulaVariable declaration,
    @NotNull LevelingVariableResolver resolver);
```

`getLeveling(jobKey)` throws `IllegalArgumentException` for an unknown job and
returns the canonical current-job profile. `getLevelingVariables()` returns an
immutable, stable-order snapshot containing built-ins and integration
variables. Registration is additive and rejects invalid names, duplicate
names, built-in replacement, null metadata, and null resolvers. Resolver
results and declared semantic types are validated when a formula resolves
them.

There is deliberately no unregister or replacement operation. Paper plugin
reload is not a supported lifecycle, and immutable bindings are what make
already-issued formulas and progression snapshots stable.

## Variable-Catalog Lifecycle and Cache Coherence

The paper implementation owns an `AtomicReference<CatalogSnapshot>`. A
snapshot contains:

- a monotonically increasing internal revision;
- an immutable insertion-ordered map of variable name to declaration/resolver
  binding; and
- a profile cache owned only by that revision.

Profile cache keys include the immutable job identity, max level, and raw
expression rather than only the job key. A current job and an embedded
progression snapshot with the same key but different formulas therefore cannot
share a cached profile accidentally.

Registration validates the new binding, then uses compare-and-set to publish a
new immutable snapshot with a fresh profile cache. Readers capture the current
snapshot once at operation entry. Formula parsing, referenced-variable
metadata, resolver lookup, threshold precomputation, and `JobLeveling`
construction all use that single captured snapshot.

This design avoids a clear-and-repopulate race: an old reader can finish using
its old cache, while new readers can only populate the new snapshot's cache.
An issued `JobLeveling` remains internally consistent and concurrently safe.
Because registration is additive-only, a new registration cannot change the
value or type of any formula that was valid under an older snapshot. A formula
referencing a previously unknown variable could not have produced an older
successful `JobLeveling`; it becomes constructible only after its binding is
atomically published.

`getLevelingVariables()` returns data copied from one captured snapshot.
`JobLeveling.formula().variables()` is self-contained and always matches that
profile's runtime bindings, so consumers never need to join metadata from a
later catalog to interpret a formula.

## Persistence Boundary

`JobRecord.levellingCurve` remains the stored raw expression. The spelling can
be corrected separately only with an explicit persistence/schema migration;
this change does not rename persisted fields.

`JobImpl` stores the expression as an internal record component or internal
field, not through the public `Job` interface. `JobImpl.toRecord()` writes that
string directly. No serializer relies on a function object's `toString()`.

A `JobProgressionRecord` embeds a `JobRecord`, so progression reconstruction
creates `JobLeveling` from that exact embedded job/formula snapshot using the
current immutable variable catalog. `JobProgressionImpl` stores the resulting
`JobLeveling`; `setExperience` reuses it. This preserves current snapshot
semantics when the server's active job configuration later changes.

The same internal leveling engine constructs both public current-job profiles
and persisted progression profiles. There is one parser, validator, threshold
table implementation, and level derivation algorithm.

## Caller Migration

The implementation must remove all compatibility accessors and alternate
evaluators.

### Public `Job`

Remove `Job.levelingCurve()`. `Job` continues to expose identity, display
metadata, max level, payable curves, upgrade level, and perk unlocks.

Delete the old root `LevelingCurve` type after migrating every reference. Its
single `Parameters(int level)` wrapper has no remaining role.

### `JobProgressionImpl`

- Replace the stored bare `Job` plus direct curve access with `JobLeveling`.
- `job()` returns `jobLeveling.job()`.
- `experienceForLevel(level)` delegates to
  `jobLeveling.experienceForLevel(level)`.
- Constructor level derivation delegates to
  `jobLeveling.levelForExperience(experience)`.
- `setExperience` reuses the same immutable `JobLeveling` snapshot.

`JobProgressionView.experienceForLevel(int)` remains the convenient public
threshold query for an existing progression.

### `JobServiceImpl`

- Own the variable catalog and leveling engine dependency.
- `getLeveling` loads the current `JobRecord` and constructs/caches its profile
  from one catalog snapshot.
- New joins use the profile created from the same loaded `JobRecord` and start
  at `experienceForLevel(1)`.
- Progression mapping receives the engine/profile factory rather than
  constructing expression curves statically.

### `LevelCommand`

All set/add/remove/reset paths obtain their threshold from the relevant
`JobLeveling` or existing `JobProgressionView`. They do not retrieve an
expression from `Job`.

### `StatsGui`

Use `JobProgressionView.experienceForLevel(level + 1)`. At max level, use
`JobLeveling.inspect`/the known max-level condition rather than relying on an
exception from an opaque curve.

### Expression implementation

Replace `ExpressionCurves.levelingCurve(String)` with the internal leveling
engine. A compiled exp4j expression is not mutated concurrently. Acceptable
implementations are a fresh expression per uncached evaluation or a safe
per-thread/per-call copy; threshold precomputation means each level is normally
evaluated only once per profile snapshot.

Payable-curve behavior remains unchanged except that shared mutable exp4j
state discovered there should be tracked separately rather than expanded into
this leveling redesign.

## Error Handling

- Unknown job key: `IllegalArgumentException` from `JobService.getLeveling`.
- Invalid variable declaration or duplicate name: `IllegalArgumentException`
  at registration; the previous catalog remains published.
- Unknown expression variable, parse failure, resolver failure, type mismatch,
  negative threshold, or non-increasing threshold: a descriptive unchecked
  leveling-configuration exception while constructing the profile. The cache
  does not retain failed profiles.
- Requested threshold outside `[1, maxLevel]`: `IllegalArgumentException`.
- Negative experience: accepted and represented as level 1 with zero progress
  into the level.
- Experience above the final threshold: accepted and represented at max level
  with no next-level metrics.

Exceptions include the job key, expression when safe, target level, and
variable name where applicable. They never silently substitute zero.

## Documentation and User Visibility

Update the developer API page to:

- remove `Job.levelingCurve()` and the old `LevelingCurve` entry;
- document `JobService.getLeveling`, variable registration, and purity rules;
- list the built-in `level: INTEGER` variable;
- show how to inspect the global variable catalog and a formula's referenced
  variables; and
- show deterministic registration using a namespaced provider key.

The `FormulaVariable` record exposes only portable data (`name`, enum `type`,
plain description, Adventure `Key` provider). User-facing tools can render
that data without access to resolver implementations. A future editor payload
may mirror those fields, but changing the current task-only editor schema is
outside this cutover.

## Verification

### API contract tests

- `FormulaVariable` validates names and immutable non-null metadata.
- Returned formula variable lists and resolved-value maps are immutable.
- Integer variables reject fractional resolver values.
- Equal contexts produce equal built-in variable values.

### Paper leveling tests

- Linear and nonlinear expressions evaluate expected thresholds.
- Registered decimal and integer variables appear with types and resolved
  values and participate in evaluation.
- Unknown and duplicate variables fail without publishing partial state.
- Registering a new variable atomically publishes a new catalog snapshot;
  an existing `JobLeveling` remains self-consistent and a new profile observes
  the new catalog.
- Concurrent profile queries and registration never combine declarations from
  one revision with resolvers from another.
- Threshold tables reject negative or non-increasing results.
- `levelForExperience` covers below-level-1, exact thresholds, between-level,
  exact max, and above-max experience.
- `LevelingState` covers negative XP, below the first threshold, ordinary
  progress, exact level boundaries, and over-cap XP.
- Progression reconstruction and `setExperience` reuse one leveling snapshot.

### Migrated behavior tests

- Joining a new job starts at the level-1 threshold.
- Level command mutations persist the canonical threshold.
- Stats output uses progression threshold data and handles max level without
  formula exceptions.
- No source or test retains `Job.levelingCurve()` or the old `LevelingCurve`
  type.

Run the focused API and paper tests first, then:

```text
./gradlew :modularjobs-api:test :modularjobs-common:test :modularjobs-paper:test
```

## Decisions Log

| Date | Decision | Why |
|------|----------|-----|
| 2026-08-31 | Remove leveling access from `Job` | Formula evaluation is service behavior, not job display metadata |
| 2026-08-31 | Separate immutable declarations from functional resolvers | Types and descriptions remain safe for users and tools; executable hooks stay runtime-only |
| 2026-08-31 | Support only integer and decimal variables | Matches exp4j's numeric model without misleading arbitrary type support |
| 2026-08-31 | Exclude player and mutable runtime state | Stable thresholds are required for offline, archived, and binary-search progression |
| 2026-08-31 | Centralize all derived math in `JobLeveling` | Prevents callers from implementing inconsistent threshold and progress rules |
| 2026-08-31 | Use additive atomic catalog snapshots | Keeps formula metadata, bindings, caches, and progression snapshots coherent |
| 2026-08-31 | Clamp normalized progress but preserve raw experience | Negative and over-cap XP remain observable without producing invalid percentages |
| 2026-08-31 | Keep embedded progression formula snapshots | Preserves current persisted progression behavior across job configuration changes |
