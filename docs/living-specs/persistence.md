# Persistence

## Intent

Support operator-managed Paper persistence on MySQL 8 or local SQLite while keeping
editor sessions on MySQL 8. The game plugin and REST API **never create tables**.
Success: missing schema fails fast with an actionable message; backups and upgrades
stay outside `onEnable`.

## Boundaries

- DDL sources: `modularjobs-paper/src/main/resources/sql/{mysql,sqlite}.sql`
- Apply paths: `./scripts/apply-mysql-schema.sh`, `./scripts/apply-sqlite-schema.sh`
- HikariCP pools keyed by backend + `jdbc-url` + username (`database.yml`)
- SQLite pools fixed at one connection with foreign keys enabled
- Repository pattern + dialect-specific atomic upserts for progression, boosts, upgrades
- Schema presence checks on connect (`SchemaPresence` / REST `require_table`)
- Rust REST session authority remains MySQL `editor_sessions`

## Non-goals

- PostgreSQL / MariaDB / Mongo providers
- In-process DDL, Flyway/Liquibase inside the plugin or API
- Plugin launching or provisioning a database process
- SQLite as a shared or multi-server store

## Invariants

- **Fail-fast** if required tables are missing (plugin enable / API boot).
- **MySQL** for shared Paper deployments and all editor-session authority.
- **SQLite** for one standalone Paper server; no REST session tables are consumed there.
- Tests may apply the matching SQL file explicitly; production paths stay connect-only.
- Exact decimal storage for money/XP: `DECIMAL` on MySQL, decimal text on SQLite.
- Indexed/key-bearing identifiers use bounded `VARCHAR` on MySQL and `TEXT` on SQLite.

## Explicit do-nots

- Do not add a database dialect without its DDL, atomic upserts, startup checks, and fidelity tests.
- Do not CREATE TABLE from Java or Rust production code paths.
- Do not store session **payload** authority in Paper-local caches (handoff only).

## Current

- [x] Paper MySQL 8 + SQLite backends; Rust REST API MySQL-only
- [x] Backend-specific SQL files + external apply scripts
- [x] Schema presence fail-fast on plugin and REST
- [x] Tables: progression, archive, tasks/payables, timed boosts, player_upgrades, editor_sessions

## Next

- [ ] Formal migration versioning beyond monolithic `mysql.sql` if multi-env drift becomes painful
- [ ] Read replicas / reporting off primary (not needed yet)

## Decisions log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-08 | MySQL 8 only, connect-only schema | One dialect, broad operator availability, least privilege |
| 2026-08 | Ops-owned DDL only | Multi-instance, least privilege |
| 2026-08 | Shared DB for editor sessions + game data | Single ops surface; apply path reuses job_tasks repos |
| 2026-09 | Add externally provisioned SQLite for Paper | Standalone servers can avoid a remote DB without weakening schema ownership |
