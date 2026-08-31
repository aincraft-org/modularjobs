# Custom Action Registration and Reporting API

**Date:** 2026-08-31
**Status:** Approved design; pending written review

## Problem

`ActionType` is public but `@NonExtendable`, while its concrete implementation lives in
`modularjobs-paper`. A dependent plugin therefore cannot construct a supported custom action type
through `modularjobs-api`. Registering any constructible entry would also require the generic
`RegistryContainer.editRegistry(...)` flow. Finally, the payout entry point is the Paper-side
`JobsPaymentHandler`, not a public API service.

The result is not merely verbose registration: a dependent plugin has no cohesive supported path to
define a custom action and report that a player performed it. Existing sealed `Context` variants
also cannot represent an arbitrary plugin-owned task key.

## Goals

- Provide one public service for registering and reporting custom actions.
- Keep `modularjobs-api` free of Bukkit and Paper types.
- Reuse the existing job lookup, curve, boost, payable, and progression pipeline without duplicating
  payment behavior.
- Let integrations use arbitrary Adventure keys as task contexts while preserving all existing typed
  contexts.
- Reject ambiguous registration and reporting mistakes immediately.

## Public API

Add `dev.mintychochip.service.ActionService` to `modularjobs-api`:

```java
public interface ActionService {

  @NotNull
  ActionType register(@NotNull Key key, @NotNull String name);

  void report(
      @NotNull UUID playerId,
      @NotNull ActionType type,
      @NotNull Context context);

  default void report(
      @NotNull UUID playerId,
      @NotNull ActionType type,
      @NotNull Key contextKey) {
    report(playerId, type, new Context.KeyContext(contextKey));
  }
}
```

Expose the service through a new `Bridge.actionService()` accessor. Consumers obtain the service once
and retain each registered action type:

```java
ActionService actions = Bridge.bridge().actionService();

ActionType questComplete =
    actions.register(Key.key("myplugin", "quest_complete"), "Quest Complete");

actions.report(
    player.getUniqueId(),
    questComplete,
    Key.key("myplugin", questId));
```

The service uses `UUID`, Adventure `Key`, `ActionType`, and `Context`; the API module gains no
Bukkit/Paper dependency.

## Context Extension

Keep `Context` sealed. Add `Context.KeyContext(@NotNull Key key)` to its explicit `permits` list. The
record represents a context whose task lookup identity is already fully resolved by the integration.
Its compact constructor rejects a null key.

Register `KeyContext::key` in `KeyResolvers.create()`. Existing `BlockContext`, `ItemContext`,
`EntityContext`, `DyeContext`, `EnchantmentContext`, `PotionContext`, `ChunkContext`, and deprecated
`MaterialContext` behavior remains unchanged.

## Registration Semantics

`ActionService.register(key, name)`:

1. rejects a null key or name;
2. rejects a blank name;
3. rejects any key already present in the action registry, including built-in keys;
4. creates an immutable `ActionTypeImpl` preserving the supplied key and name;
5. registers that instance in the existing `RegistryKeys.ACTION_TYPES` registry; and
6. returns the same canonical instance for the integration to retain.

Duplicate registration throws `IllegalArgumentException` and leaves the existing registry entry
unchanged. This is intentionally stricter than the generic `Registry.register(...)` replacement
contract so integrations cannot accidentally replace built-ins or another plugin's action. The
generic registry API and its existing semantics do not change.

Registration is intended once during a dependent plugin's enable phase. This feature does not add
ownership tracking, unregister handles, or automatic cleanup when a dependent plugin disables.

## Reporting Semantics

`ActionService.report(playerId, type, context)`:

1. rejects null arguments;
2. resolves `type.key()` in the action registry;
3. throws `IllegalArgumentException` when that key is not registered;
4. uses the registry's canonical `ActionType` instance;
5. resolves the UUID to Bukkit's `OfflinePlayer` in the Paper implementation; and
6. calls the existing `JobsPaymentHandler.pay(OfflinePlayer, ActionType, Context)`.

The Adventure-key overload creates `Context.KeyContext` and delegates to the context overload. A
registered action with no matching job task follows existing behavior and produces no payout.
Exceptions from the existing payout pipeline are not swallowed or translated.

Reporting is synchronous on the caller's thread. The service adds no scheduler, asynchronous wrapper,
online-only condition, retry policy, or alternate repository path.

## Components and Wiring

### `modularjobs-api`

- Add `service/ActionService.java` with the three methods above.
- Add `Bridge.actionService()`.
- Add `Context.KeyContext` and update the sealed `permits` list.

### `modularjobs-paper`

- Add `action/ActionServiceImpl.java`, constructed with the existing mutable action registry and
  `JobsPaymentHandler`.
- Update `KeyResolvers.create()` with the `KeyContext` strategy.
- Construct `ActionServiceImpl` after `PaymentWiring` has produced `JobsPaymentHandler`.
- Pass the service into `BridgeImpl` and return it from the new bridge accessor.

No new registry, listener, event, repository, database table, configuration entry, or editor payload
field is introduced. `EditorService` already streams the live action registry, and job tasks already
persist `action_type_key` and `context_key`.

## Data Flow

```text
Dependent plugin onEnable
  -> ActionService.register(key, name)
  -> existing action registry
  -> returned canonical ActionType

Dependent plugin event
  -> ActionService.report(player UUID, ActionType, Context or Key)
  -> canonical action registry lookup
  -> Bukkit OfflinePlayer resolution
  -> JobsPaymentHandler.pay
  -> JobService task lookup
  -> payable curves and boosts
  -> PayableHandler
  -> progression persistence
```

## Error Handling

- Null API arguments: `NullPointerException` with the offending parameter named.
- Blank display name: `IllegalArgumentException`.
- Duplicate action key: `IllegalArgumentException`; original entry remains registered.
- Report of an unknown action key: `IllegalArgumentException`; payout is not entered.
- Registered action without a matching task/context: normal no-op from the existing payment path.
- Payout, boost, payable-handler, or persistence failures: retain current propagation and logging
  behavior.

## Testing

Follow TDD with focused failing tests before implementation.

### API contract

Extend `ContextKeyContractTest` to prove `KeyContext` preserves its Adventure key and rejects null.
Compilation of `BridgeImpl` against `Bridge` proves the new bridge contract is implemented.

### Paper behavior

Add focused `ActionServiceImplTest` coverage:

- `register` returns an action with the supplied name/key and stores that same instance in the action
  registry;
- a duplicate key throws and preserves the original entry;
- a blank name is rejected without mutating the registry;
- reporting an unknown action throws before job lookup;
- reporting with a raw context key resolves `UUID -> OfflinePlayer`, delegates through a real
  `JobsPaymentHandler`, presents a `KeyContext` to `JobService`, matches a task, and invokes a
  recording `PayableHandler` once; and
- reporting uses the registry's canonical action instance.

Extend the resolver tests to prove `KeyResolvers.create()` maps `KeyContext` to its contained key.
Use the existing `MockBukkitSupport` lifecycle for the UUID-to-`OfflinePlayer` boundary; do not replace
`JobsPaymentHandler` with a mock.

### Verification commands

Run the new focused tests during red/green implementation, followed by:

```text
./gradlew :modularjobs-api:test :modularjobs-paper:test
./gradlew :modularjobs-paper:build
```

## Consumer Documentation

Update `web/fumadocs/content/docs/develop/api.mdx` with the service contract, registration/reporting
example, arbitrary-key context behavior, and duplicate/unknown-key failures. Add the new public API to
`CHANGELOG.md` under `Unreleased -> Added`.

## Compatibility

- Built-in `ActionTypes` constants and existing listeners continue unchanged.
- Existing typed contexts resolve exactly as before.
- Existing job task rows and editor payloads require no migration.
- Adding `Bridge.actionService()` requires in-repository bridge implementations and test doubles to
  implement the new accessor; no deprecated alias or parallel registration path is added.

## Non-goals

- Dynamic unregistration or plugin ownership tracking.
- Replacing the generic registry API.
- Adding custom context payloads beyond one resolved Adventure key.
- Adding new built-in action types or listeners.
- Scheduling, batching, retrying, or asynchronously executing reports.
- Changing payout eligibility, curves, boosts, payable handling, or persistence.
- Database, configuration, REST API, or session-editor contract changes.
