#!/usr/bin/env bash
# scripts/run-auth-user-migration.sh
# One-shot runner: substitutes __OLD_DB_PASS__ in the SQL template, then executes.
#
# Usage:
#   POSTGRES_PASSWORD=<pass> bash scripts/run-auth-user-migration.sh [psql options]
#
# Example (local):
#   POSTGRES_PASSWORD=EduPlatform2026! bash scripts/run-auth-user-migration.sh \
#       -U postgres -h localhost -d gcrf_main
#
# Example (inside K8s postgresql-0 pod via kubectl exec):
#   POSTGRES_PASSWORD=$POSTGRES_PASSWORD bash /tmp/run-auth-user-migration.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_TEMPLATE="$SCRIPT_DIR/migrate-auth-users-to-region.sql"

if [[ ! -f "$SQL_TEMPLATE" ]]; then
  echo "ERROR: SQL template not found at $SQL_TEMPLATE" >&2
  exit 1
fi

PASS="${POSTGRES_PASSWORD:-}"
if [[ -z "$PASS" ]]; then
  echo "ERROR: POSTGRES_PASSWORD env var is required" >&2
  exit 1
fi

TMP_SQL=$(mktemp /tmp/migrate-auth-XXXXXX.sql)
trap 'rm -f "$TMP_SQL"' EXIT

# Substitute the placeholder with the actual password
sed "s|__OLD_DB_PASS__|${PASS}|g" "$SQL_TEMPLATE" > "$TMP_SQL"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Running auth user migration..."
PGPASSWORD="$PASS" psql "$@" -f "$TMP_SQL"
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Migration script finished."
