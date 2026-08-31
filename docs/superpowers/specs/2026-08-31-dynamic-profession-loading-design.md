# Dynamic Profession Loading

**Date:** 2026-08-31
**Status:** Approved

## Problem

`ProfessionCatalog` is a public static class in `modularjobs-api` whose Java source owns all
profession IDs, storage keys, categories, display names, and lookup indexes. Changing that content
requires recompiling the API and Paper plugin. The static catalog is also referenced directly by
the Paper implementation and recipe loader, bypassing the composition root.

Profession content must instead be loaded from an operator-editable resource during plugin
startup. The API should retain data and service contracts, but it should not own server-specific
content or global state.

## Decisions

- Delete `ProfessionCatalog`; do not retain a compatibility facade, deprecated alias, or static
  singleton.
- Keep `ProfessionDefinition`, `ProfessionCategory`, and `ProfessionService` in
  `modularjobs-api` as the public contracts.
- Add `modularjobs-paper/src/main/resources/professions.yml` as the bundled default.
- On first startup, copy the bundled file to the plugin data folder without overwriting an
  existing file. Load the data-folder copy once during composition. Changes take effect after the
  next restart.
- Store loaded definitions in an immutable, insertion-ordered Paper-side index injected into
  `ProfessionServiceImpl`. No mutable registry or global state.
- Treat the file atomically. Any invalid entry or invalid cross-reference aborts composition and
  prevents the plugin from enabling with a precise configuration error.
- Preserve canonical-ID, storage-key, case-insensitive, and namespaced-suffix resolution. For
  example, `mining`, `miner`, and `modularjobs:miner` all resolve to the `mining` definition.
- Do not add runtime reload, third-party registration, database persistence, or an editor surface.

## Resource Contract

`professions.yml` uses a list so source order is explicit and duplicate IDs remain detectable:

```yaml
professions:
  - id: mining
    storage-key: miner
    category: gathering
    display-name: Mining

  - id: woodcutting
    storage-key: lumberjack
    category: gathering
    display-name: Woodcutting
```

Each entry requires exactly these semantic fields:

- `id`: canonical profession identifier.
- `storage-key`: job/progression key in `jobs.yml`, without a namespace.
- `category`: case-insensitive `gathering`, `processing`, or `crafting`.
- `display-name`: non-blank plain display label.

IDs and storage keys are trimmed and normalized with `Locale.ROOT`. Both must use lowercase key
characters and must be unique across the file. A value may be both one definition's canonical ID
and that same definition's storage key, but it must not resolve to two different definitions.

The old `LEGACY_ALIASES` entries are identical to the affected definitions' storage keys
(`miner`, `lumberjack`, `farmer`, `fisherman`, `alchemist`, and `blacksmith`). Storage-key lookup
therefore preserves every current result without a separate alias map or an `aliases` field.
`fisher` remains unresolved, matching current behavior.

The bundled file contains the existing 15 definitions in their current order:

| ID | Storage key | Category | Display name |
| --- | --- | --- | --- |
| `mining` | `miner` | `gathering` | Mining |
| `woodcutting` | `lumberjack` | `gathering` | Woodcutting |
| `herbalism` | `herbalism` | `gathering` | Herbalism |
| `farming` | `farmer` | `gathering` | Farming |
| `fishing` | `fisherman` | `gathering` | Fishing |
| `smelting` | `smelting` | `processing` | Smelting |
| `milling` | `milling` | `processing` | Milling |
| `tanning` | `tanning` | `processing` | Tanning |
| `refining` | `refining` | `processing` | Refining |
| `cooking` | `cooking` | `crafting` | Cooking |
| `alchemy` | `alchemist` | `crafting` | Alchemy |
| `armorsmithing` | `armorsmithing` | `crafting` | Armorsmithing |
| `weaponsmithing` | `blacksmith` | `crafting` | Weaponsmithing |
| `tailoring` | `tailoring` | `crafting` | Tailoring |
| `engineering` | `engineering` | `crafting` | Engineering |

## Components and Data Flow

### API module

- Remove `ProfessionCatalog.java` and its static-catalog tests.
- Retain `ProfessionDefinition` as the immutable DTO returned to consumers.
- Retain `ProfessionService.tracks()` and `ProfessionService.resolve(...)` as the supported public
  lookup surface. Update documentation that currently calls the service or definitions
  “built-in” or describes a static catalog.

The API module remains Paper-free and gains no YAML dependency or resource content.

### Paper module

- `YamlProfessionDefinitionLoader` owns first-run resource copying, YAML parsing, field validation,
  and validation that every `storage-key` identifies a loaded `jobs.yml` job.
- `ProfessionIndex` owns the immutable ordered list plus canonical-ID and storage-key maps. Its
  constructor rejects ambiguous lookup keys. It contains no Bukkit or file I/O code.
- `ProfessionServiceImpl` receives `JobService` and `ProfessionIndex`. Every profession lookup in
  level, experience, and join paths uses the injected index.
- `ProfessionWiring.create(...)` builds objects in this order:
  1. copy and parse `professions.yml`;
  2. validate definitions against the already-composed `JobService`;
  3. construct `ProfessionServiceImpl`;
  4. load recipes using that `ProfessionService`;
  5. return the remaining profession services.
- `YamlRecipeDefinitionLoader` receives `ProfessionService` and uses
  `professionService.resolve(...)` instead of calling a static catalog.

The top-level `PluginContext` ordering remains jobs/domain first and profession wiring second, so
cross-reference validation requires no new dependency cycle.

## Resolution Semantics

`ProfessionIndex.resolve(input)` preserves current observable behavior:

1. null or blank input returns empty;
2. trim and lowercase with `Locale.ROOT`;
3. when a colon exists, discard the namespace prefix and resolve the suffix;
4. resolve canonical ID first;
5. resolve storage key second;
6. otherwise return empty.

The loaded list and lookup maps never change after successful startup. `tracks()` returns the
immutable source-order list directly; lookups remain constant-time.

## Validation and Error Handling

Startup fails before services or listeners are registered when any of these conditions occurs:

- `professions.yml` cannot be copied or read;
- root `professions` is absent, not a list, or empty;
- an entry is not a mapping;
- a required field is absent, has the wrong type, or is blank;
- an ID or storage key has invalid key characters;
- a category is not one of the three `ProfessionCategory` values;
- an ID, storage key, or cross-kind lookup key resolves ambiguously;
- a storage key has no corresponding job from `jobs.yml`.

Errors identify the file and entry index, plus the relevant field and conflicting value where
applicable. The loader does not skip entries, install a partial index, or fall back to compiled
content.

Recipe loading retains its existing fail-fast handling for unknown professions, but resolution now
uses the already-validated service. Existing data-folder files are never overwritten.

## Compatibility and Documentation

This is an intentional source and binary API break for callers that directly reference
`ProfessionCatalog`. The clean replacement is `Bridge.professionService()` or the registered
Bukkit `ProfessionService`; no shim is retained.

Update:

- API development documentation to remove the static catalog and describe the dynamically loaded
  service;
- profession/event integration documentation where it calls the service a built-in catalog;
- `jobs.yml` comments that attribute aliases to `ProfessionCatalog`;
- affected JavaDoc and tests.

No database schema, saved progression key, canonical profession ID, storage key, category, display
name, or recipe format changes.

## Testing

Follow test-driven development and preserve observable contracts:

- `ProfessionIndex` tests: immutable source order; canonical, storage-key, case-insensitive, and
  namespaced resolution; blank/unknown values; ambiguous keys rejected.
- YAML loader tests: all valid fields/categories; malformed root and entries; missing/blank/wrong
  typed fields; invalid keys; duplicate IDs/storage keys/cross-kind keys; empty file; missing job
  references; exact diagnostic context.
- Bundled-resource contract test: the shipped file loads all 15 current definitions in order and
  preserves current resolution results, including `fisher` remaining unresolved.
- Resource alignment test: every shipped profession storage key exists in shipped `jobs.yml`.
- `ProfessionServiceImpl` tests: lookups and progression mapping use the injected index.
- Recipe loader tests: known profession resolution is delegated to `ProfessionService`; unknown
  professions still fail.
- Resource-loading smoke test: under MockBukkit, load the bundled default through the real
  `YamlProfessionDefinitionLoader`, verify the data-folder file is created, and expose the loaded
  definitions through a real `ProfessionServiceImpl` backed by a stub `JobService`.
- Run focused API and Paper profession tests, then the repository's API/common/Paper test tasks and
  Paper build.

## Non-goals

- Hot reload or a reload command.
- Runtime mutation or profession registration by dependent plugins.
- Moving general job definitions out of `jobs.yml` or deriving professions from every job.
- Database persistence or migrations.
- New aliases beyond canonical IDs and storage keys.
- Changes to progression, rewards, gates, recipes, or profession eligibility semantics.
