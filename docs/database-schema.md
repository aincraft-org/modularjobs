# Database schema ownership

ModularJobs supports **MySQL 8 only**.

## Rule

| Store | Who creates tables? | Who only connects? |
|-------|---------------------|--------------------|
| **MySQL** | Ops / CI / script **once** | Paper plugin and `web/rest-api` |

The **game process and REST API never run DDL**. That is intentional: multi-instance
servers, least privilege, reviewable migrations, backups, and upgrades do not belong
in `onEnable` or the API request path.

## Source of truth

- `modularjobs-paper/src/main/resources/sql/mysql.sql`

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

Identical `jdbc-url` + `username` sections share one Hikari pool.

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

The Paper plugin and `web/rest-api` must point at the same MySQL database. The
REST API stores editor payloads in `editor_sessions`; Paper fetches those payloads
through the REST API and applies task changes through its existing repositories.

The plugin does not launch MySQL, manage its process, create a data directory,
or replace operator backups and upgrades. For local development, run MySQL 8
externally (for example, Docker/Podman) and apply the schema before starting either
process.

## Startup behavior

1. Open the shared Hikari pool to MySQL.
2. Verify required tables exist (`job_progression`, `archive_job_progression`,
   `job_tasks`, …).
3. Verify migration-sensitive columns exist, including both
   `current_node_key` columns.
4. Missing tables or columns → **fail enable** with the applicable schema or
   migration script; runtime performs no DDL.

## Job task updates

The plugin does not seed an empty `job_tasks` table. Operators create and update
task payables through the editor/repository path or an explicit reviewed SQL
operation; MySQL is the only runtime source of task data. Back up live task data
before bulk edits or deletes.

The `fisher` → `fisherman` job-key rename likewise requires an operator-managed data
update for existing player-state, upgrade, task, and payable rows before deploying
the renamed catalog.

SQLite, PostgreSQL, and MariaDB are **not supported**.
