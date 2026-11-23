# Database schema ownership

The Paper plugin supports **MySQL 8** and **SQLite**. The Rust editor-session API remains MySQL-only.

## Rule

| Store | Who creates tables? | Who only connects? |
|-------|---------------------|--------------------|
| **Paper / MySQL** | Ops / CI / script **once** | Paper plugin |
| **Paper / SQLite** | Operator / provisioning script **once** | Paper plugin |
| **Editor sessions / MySQL** | Ops / CI / script **once** | `web/rest-api` |

The **game process and REST API never run DDL**. This keeps startup free of schema mutation and makes
changes reviewable. Provision every distinct database before starting the consuming process.

## Sources of truth

- `modularjobs-paper/src/main/resources/sql/mysql.sql`
- `modularjobs-paper/src/main/resources/sql/sqlite.sql`

## Provision MySQL

```bash
# Local / CI
export DATABASE_URL=mysql://user:pass@host:3306/modularjobs
./scripts/apply-mysql-schema.sh

# Or, with the MySQL client configured for the target database
mysql --host=host --port=3306 --user=user --password modularjobs \
  < modularjobs-paper/src/main/resources/sql/mysql.sql
```

Then point the plugin at that database:

```yaml
# database.yml
payable:
  type: mysql
  jdbc-url: jdbc:mysql://host:3306/modularjobs
  username: modularjobs
  password: secret
  maximum-pool-size: 10

progression:
  type: mysql
  jdbc-url: jdbc:mysql://host:3306/modularjobs
  username: modularjobs
  password: secret

timed-boost:
  type: mysql
  jdbc-url: jdbc:mysql://host:3306/modularjobs
  username: modularjobs
  password: secret

upgrades:
  type: mysql
  jdbc-url: jdbc:mysql://host:3306/modularjobs
  username: modularjobs
  password: secret
```

Identical backend + `jdbc-url` + username sections share one Hikari pool.

## Provision SQLite

SQLite is intended for one standalone Paper server. It is not a shared multi-server store and is
not used by `web/rest-api`.

```bash
./scripts/apply-sqlite-schema.sh \
  /absolute/path/to/plugins/ModularJobs/modularjobs.db
```

Then select SQLite in each Paper persistence section that should use that file:

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

SQLite credentials are omitted. Its pool size is fixed at one connection, and each connection
enables foreign-key enforcement. Reusing the exact URL shares the pool. A relative SQLite path is
resolved from the Paper server working directory; use an absolute JDBC path when that location must
be unambiguous.

Each section may independently select `mysql` or `sqlite`. Every distinct target must receive the
complete matching schema because startup validates all required tables. If `progression` or
`upgrades` is absent, that section falls back to `payable`.

## Upgrade existing installations

Back up MySQL and stop every ModularJobs instance before applying migrations.
Neither the game process nor REST API runs DDL.

### Player job state trees

Existing `job_progression` and `archive_job_progression` tables need the active
node column:

```bash
mysql --host=host --port=3306 --user=user --password modularjobs \
  < scripts/migrate-add-current-node-key.sql
```

This migration is idempotent. It adds `current_node_key` when absent, initializes
legacy rows to their previous `job_key`, and enforces `NOT NULL`. On an already
current schema it preserves existing active-node values.

The SQL cannot infer `jobs.yml` parent chains. If an old `job_key` is now a child
node, consolidate that player's duplicate root/child rows according to your
server's policy, change `job_key` to the owning root, and keep the selected child
in `current_node_key`. Complete this data mapping before starting the new plugin.
One row must remain per `(player_id, root job_key)`.

### Currency symbols

Installations created before currency-symbol persistence also run:

```bash
mysql --host=host --port=3306 --user=user --password modularjobs \
  < scripts/migrate-add-currency-symbol.sql
```

That migration adds `job_task_payables.currency_symbol` and intentionally fails
if applied twice. Legacy rows keep a `NULL` symbol because the original symbol
cannot be recovered; ModularJobs renders the stored currency identifier.

## Shared editor session database

The secure editor-session service remains MySQL-only. Point `web/rest-api` at a provisioned MySQL
database containing `editor_sessions`. Paper fetches editor payloads through the REST API and then
applies task changes to the backend selected by its `payable` section.

An SQLite-backed Paper server may still use the web editor: the REST API keeps sessions in MySQL,
while imported job-task changes are persisted to Paper's SQLite file.

The plugin does not launch a database process, create a data directory, or replace operator backups
and upgrades.

## Startup behavior

1. Open or reuse the Hikari pool for each configured Paper backend.
2. Verify required tables exist (`job_progression`, `archive_job_progression`, `job_tasks`, …).
3. Verify migration-sensitive columns exist, including both `current_node_key` columns.
4. Missing tables or columns → **fail enable** with the matching schema/provisioning script; runtime
   performs no DDL.

## Job task updates

The plugin does not seed an empty `job_tasks` table. Operators create and update task payables
through the editor/repository path or an explicit reviewed SQL operation. Back up live task data
before bulk edits or deletes.

The `fisher` → `fisherman` job-key rename likewise requires an operator-managed data
update for existing player-state, upgrade, task, and payable rows before deploying
the renamed catalog.

PostgreSQL, MariaDB, and MongoDB are **not supported**.
