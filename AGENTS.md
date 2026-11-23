# ModularJobs — agent notes

PaperMC job progression plugin (Java 25 toolchain, Gradle multi-module) plus
MySQL-backed REST API (Rust) and React secure session editor.

## Modules

| Path | Role |
|------|------|
| `modularjobs-api` | Pure public contracts (no Paper); depends on `dev.conditions:api` |
| `modularjobs-common` | Shared DTOs (editor payload, …) |
| `modularjobs-paper` | Paper plugin implementation (shadow jar) |
| `web` | Docs + session-editor + rest-api |
| `scripts/` | Out-of-band ops helpers (schema apply) |

Build plugin: `./gradlew :modularjobs-paper:build` (shadowJar → `modularjobs-paper/build/libs/modularjobs-paper-<version>-all.jar`, e.g. `modularjobs-paper-26.8.11.1-all.jar`).
Unit tests: `./gradlew :modularjobs-api:test :modularjobs-common:test :modularjobs-paper:test`.  
Static analysis (Error Prone on compile; Spotless, Checkstyle Google Checks, PMD, SpotBugs on `check`):  
`./gradlew clean check` is the fail-closed CI gate — reports under `*/build/reports/{checkstyle,pmd,spotbugs}/`.  
Configs: `config/checkstyle/`, `config/pmd/`, `config/spotbugs/`.  
Rust API: `cd web/rest-api && cargo test` / `cargo run --release`.  
React editor: `cd web/session-editor && npm test && npm run build`.

### Git hooks + CI

- **Install hooks** (once per clone): `./scripts/install-git-hooks.sh`  
  Sets `core.hooksPath=.githooks`. Pre-commit runs compile/tests for staged Java,
  `cargo check` for `web/rest-api`, `npm test` for `web/session-editor`.  
  Skip once: `SKIP_PRECOMMIT=1 git commit ...`
- **GitHub Actions**: `.github/workflows/ci.yml`  
  - `java` — JDK 25, MySQL 8 service, `./gradlew clean check`, shadow jar + report artifacts
  - `rest-api` — Rust stable + MySQL 8, `cargo test`
- **Nightly**: `.github/workflows/nightly.yml` — rolling `nightly` pre-release of the Paper shadow jar

## Database schema ownership (important)

**Paper supports MySQL 8 and SQLite. The Java plugin and Rust API never create tables.**

| Store | Who applies DDL | Runtime process |
|-------|-----------------|-----------------|
| Paper / MySQL | Ops / CI / script **once** | Connect + verify only |
| Paper / SQLite | Operator / script **once** | Connect + verify only |
| Rust editor sessions / MySQL | Ops / CI / script **once** | Connect only |

- Sources of truth: `modularjobs-paper/src/main/resources/sql/{mysql,sqlite}.sql`
- Apply: `./scripts/apply-mysql-schema.sh` or `./scripts/apply-sqlite-schema.sh <file>`
- Policy: `SchemaPolicy` — never runs DDL in-process (`auto-schema` is ignored)
- Fail-fast: `SchemaPresence` on connect; missing tables → hard error, no CREATE
- SQLite: Paper-only, one pooled connection, no shaded JDBC driver (Paper provides it)
- Rust `web/rest-api`: MySQL-only
- Details: `docs/database-schema.md`

Do **not** reintroduce boot-time `CREATE TABLE` or claim PostgreSQL/MariaDB support.
Tests may apply the matching SQL file explicitly; production paths stay connect-only.

## Secure session stack

```
Plugin export (payload JSON)
    → POST web/rest-api /api/v1/sessions  (or create via API)
    → MySQL table editor_sessions
    → PUT save with Bearer / X-Session-Token
```

- Payload contract: version, metadata.sessionToken, jobs map, registered action/payable types  
  (`modularjobs-paper` editor JSON, `web/rest-api` models, `web/session-editor` types; shared DTOs in `modularjobs-common`)
- Auth: session **code** (public) + **token** (secret). Wrong/missing token → 401; no cross-session overwrite
- React client base URL: `VITE_SESSION_API_URL` (default `http://127.0.0.1:18787`)
- **Do not** use `bytebin.lucko.me` for the production secure editor path  
  (`web/session-editor` + Rust API). Legacy Vue `bytebin.ts` is deprecated for that path.
- Plugin in-game export may still mention bytebin historically; full plugin cutover is separate work

### API env

```bash
export DATABASE_URL=mysql://user:pass@host:3306/modularjobs
export BIND_ADDR=127.0.0.1:18787
# optional hardening (see web/rest-api/README.md)
# export SESSION_CREATE_SECRET=...
# export CORS_ALLOW_ORIGINS=http://127.0.0.1:5173
# schema must already exist
cargo run --release --manifest-path web/rest-api/Cargo.toml
```

Session ownership: code (public) + token (secret headers). Create is open unless
`SESSION_CREATE_SECRET` is set; GET/PUT require Bearer / `X-Session-Token`.

### Plugin remote DB (`database.yml`)

```yaml
payable:
  type: mysql
  jdbc-url: jdbc:mysql://host:3306/modularjobs
  username: modularjobs
  password: secret
```

## Living specs (domain catalogs)

Durable design intent + feature horizons live under `docs/living-specs/`.
Read the domain catalog before implementing; flip checkboxes when work ships.
Index: `docs/living-specs/README.md`. One-shot design dumps under
`docs/superpowers/` are historical — horizons in living specs are authoritative.

## Architecture notes (core)

- Composition root: `PluginContext` + package `*Wiring` (no Guice)
- Repository pattern + HikariCP against MySQL 8 or SQLite for Paper
- Commands: Paper Brigadier; themed text via `dev.mintychochip.util.Messages` (not Mint)
- Tests: JUnit 5; MockBukkit for Bukkit-touching tests; SQLite SQL tests are deterministic;
  live MySQL tests use `MODULARJOBS_TEST_MYSQL_*` or `localhost:13306` and skip when unavailable

## Agent rules

- Senior Java: SOLID, DRY, SRP; concise feedback
- Prefer existing patterns over new frameworks
- Never run DDL in production Java or Rust processes
- Prefer `claude-context` / codebase search when locating code; Context7 for library docs when generating setup/config against external APIs
- Atomic commits: one logical change per commit when landing work

## Working log (high level)

- Guice removed (2026-08): manual wiring
- MockBukkit 26.2 / Paper 26.2 / Java toolchain 25
- Java domain package `dev.mintychochip` (2026-08-21; was `net.aincraft`)
- Recipe depreciation + `recipes.yml` startup loading + craft/task cross-validation
- MySQL DDL + fidelity tests; connect-only remote schema ownership
- `web/rest-api` (Rust) + React `web/session-editor` for secure sessions