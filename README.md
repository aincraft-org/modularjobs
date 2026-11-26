# ModularJobs

[![Build](https://img.shields.io/github/actions/workflow/status/aincraft-org/modularjobs/ci.yml?branch=master&label=build)](https://github.com/aincraft-org/modularjobs/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/aincraft-org/modularjobs)](LICENSE)
[![Release](https://img.shields.io/github/v/release/aincraft-org/modularjobs)](https://github.com/aincraft-org/modularjobs/releases/latest)
![Platform](https://img.shields.io/badge/Paper-26.2-blue)

Extensible job progression plugin for PaperMC (**26.2** / Java **25**).

## Modules

| Path | Role |
|------|------|
| `modularjobs-api` | Pure public contracts (no Paper) |
| `modularjobs-common` | Paper-free shared DTOs, codecs, and default API implementations |
| `modularjobs-paper` | Paper plugin implementation (shadow jar) |
| `web` | Docs + session-editor + rest-api |

## Build

```bash
./gradlew :modularjobs-paper:build
# artifact: modularjobs-paper/build/libs/modularjobs-paper-26.8.11.1-all.jar
```
Unit tests:

```bash
./gradlew :modularjobs-api:test :modularjobs-common:test :modularjobs-paper:test
```

Session stack:

```bash
cd web/rest-api && cargo test
cd web/session-editor && npm test && npm run build
```

Git hooks (once per clone):

```bash
./scripts/install-git-hooks.sh
# SKIP_PRECOMMIT=1 git commit ...  # emergency bypass
```

CI: `.github/workflows/ci.yml` — Java 25 + MySQL 8 (`./gradlew clean check` + shadow jar), Rust rest-api, React session-editor. Nightly Paper jar: `.github/workflows/nightly.yml`.

## Operator quick start

1. Drop `modularjobs-paper-26.8.11.1.jar` into `plugins/`.
2. Start once to generate configs under `plugins/ModularJobs/`.
3. Configure database, economy, and permissions (below).
4. Restart or reload after config changes.

### Starter content

The bundled `jobs.yml`, `boost_sources_default.json`, and
`upgrade_trees/*.json` files form a generic starter pack. Job task tables start
empty; create task payables through the editor after configuring persistence. Tasks are then stored
in the backend selected by the `payable` section.

### Database (MySQL or SQLite)

The Paper plugin supports **MySQL 8** and **SQLite**. MySQL suits shared or multi-server
deployments; SQLite suits one standalone Paper server. PostgreSQL and MariaDB remain unsupported.
The separate web editor REST API remains MySQL-only.

1. Provision the selected schema out-of-band. The plugin never runs DDL:

```bash
# MySQL
export DATABASE_URL=mysql://user:pass@host:3306/modularjobs
./scripts/apply-mysql-schema.sh

# SQLite
./scripts/apply-sqlite-schema.sh \
  /absolute/path/to/plugins/ModularJobs/modularjobs.db
```

2. Configure each `database.yml` section with `type: mysql` or `type: sqlite`. Reuse one target
   unless you intentionally maintain multiple fully provisioned databases:

```yaml
payable:
  type: sqlite
  jdbc-url: jdbc:sqlite:plugins/ModularJobs/modularjobs.db

timed-boost:
  type: sqlite
  jdbc-url: jdbc:sqlite:plugins/ModularJobs/modularjobs.db

progression:
  type: sqlite
  jdbc-url: jdbc:sqlite:plugins/ModularJobs/modularjobs.db

upgrades:
  type: sqlite
  jdbc-url: jdbc:sqlite:plugins/ModularJobs/modularjobs.db
```

SQLite credentials are omitted and its pool is fixed at one connection. MySQL sections retain
`jdbc-url`, `username`, `password`, and optional pool tuning. Missing tables or columns make the
plugin fail at startup. See `docs/database-schema.md`.

### Economy

Money payables are optional. ModularJobs uses Mint2 when it is enabled and otherwise
uses the economy registered through Vault. Both adapters deposit into the provider's
default currency; Mint2 always wins when both integrations are installed. Without a
provider, the default blackhole policy accepts positive economy payables but discards
the currency.

```yaml
# config.yml
economy:
  required: false
  missing-provider: blackhole # blackhole | fail
```

- `missing-provider: blackhole` (default): keep the server running and discard
  positive money payables when no provider is available.
- `missing-provider: fail`: fail enable when neither Mint2 nor Vault is available.
- `required: true` remains a compatibility shorthand for `fail` when no explicit
  `missing-provider` is set.

Mint2 and Vault deposit failures return false and are logged by their adapters.
The payment pipeline never retries through the other provider after an unknown outcome.

### Permissions

| Node | Default | Purpose |
|------|---------|---------|
| `modularjobs.admin` | op | Level/exp admin, boost, web editor, applyedits |
| `jobs.command.browse` | true | Browse jobs through inventory, chat, or dialog |
| `jobs.command.list` | true | List jobs |
| `jobs.command.stats` | true | Own stats |
| `jobs.command.admin.stats` | op | Others' stats |
| `jobs.command.archive` | true | Own archive |
| `jobs.command.admin.archive` | op | Others' archive |
| `jobs.command.leaveall` | true | Leave all jobs |

Admin commands (`/jobs boost`, `/jobs editor`, `/jobs applyedits`) require `modularjobs.admin`.

### Payment rules

```yaml
pay-in-creative: true
pay-while-riding: false
disabled-worlds: []
kill-contribution-cutoff: 0.5
```

### Profession API

ModularJobs always registers its core `ProfessionService` Bukkit service for
dependent plugins. Optional Recipe/Buff/Station/NodeHarvest services remain behind:

```yaml
profession-apis:
  register-bukkit-services: false
```

The public API exposes profession catalog, progression, recipes, buffs, stations,
and resource-node hooks without requiring a separate server-specific progression
pack.

### Gathering interaction gates

ModularJobs owns profession progression, task data, and payment. Server operators
may use their own protection or interaction-gating plugins; cancelled events
receive no payment.

### Integrations and UI

Mint2, Vault, mcMMO, Bolt, LWC, Choco, and PlaceholderAPI are optional integrations.
ModularJobs uses native Paper inventory screens for browse, info and statistics.
It has no upgrade-tree command: servers bring their own tree UI. Browse also supports interactive Adventure chat and
the native Paper Dialog API through `/jobs browse chat` and `/jobs browse dialog`.
Native scoreboard sidebars and experience boss bars need no external UI library.
The external Preferences plugin is not required; when present, it provides a
per-player XP boss bar color preference via /preferences, and without it the bar
stays default green.


## Version

Plugin and project version: **26.8.11.1** (see `plugin.yml`, root `build.gradle.kts`, `CHANGELOG.md`).

Future releases use `YY.M.D.REVISION` tags, for example `v26.8.11.1`; the final component resets daily and increments for same-day releases.
