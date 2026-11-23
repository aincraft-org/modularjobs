#!/usr/bin/env bash
# Apply ModularJobs SQLite DDL out-of-band (never from the Paper process).
#
# Usage:
#   ./scripts/apply-sqlite-schema.sh
#   ./scripts/apply-sqlite-schema.sh /path/to/modularjobs.db
#
# Schema source of truth:
#   modularjobs-paper/src/main/resources/sql/sqlite.sql
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCHEMA="${ROOT}/modularjobs-paper/src/main/resources/sql/sqlite.sql"
DATABASE_FILE="${1:-${ROOT}/modularjobs-paper/run/plugins/ModularJobs/modularjobs.db}"

if [[ ! -f "${SCHEMA}" ]]; then
  echo "error: schema file not found: ${SCHEMA}" >&2
  exit 1
fi

if ! command -v sqlite3 >/dev/null 2>&1; then
  echo "error: sqlite3 is required to apply the SQLite schema" >&2
  exit 1
fi

mkdir -p "$(dirname "${DATABASE_FILE}")"

echo "Applying ModularJobs SQLite schema"
echo "  file:     ${SCHEMA}"
echo "  database: ${DATABASE_FILE}"

sqlite3 "${DATABASE_FILE}" < "${SCHEMA}"
sqlite3 "${DATABASE_FILE}" "PRAGMA journal_mode=WAL;"

echo "Schema apply complete."
