#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: scripts/migrate_starrocks.sh --baseline-empty

Applies migration 1 only when the target StarRocks database is empty. If the
database already contains schema_migrations version 1, exits successfully.
Refuses non-empty unversioned databases and never drops an existing table.

Environment: STARROCKS_HOST, STARROCKS_PORT, STARROCKS_USER,
STARROCKS_PASSWORD, STARROCKS_DATABASE, MYSQL_CLIENT.
EOF
}

if [[ "${1:-}" != "--baseline-empty" ]]; then
  usage >&2
  exit 2
fi

host=${STARROCKS_HOST:?STARROCKS_HOST is required}
port=${STARROCKS_PORT:-9030}
user=${STARROCKS_USER:?STARROCKS_USER is required}
password=${STARROCKS_PASSWORD:?STARROCKS_PASSWORD is required}
database=${STARROCKS_DATABASE:?STARROCKS_DATABASE is required}
mysql_client=${MYSQL_CLIENT:-mysql}

if [[ ! "$database" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
  printf 'STARROCKS_DATABASE must be a simple SQL identifier: %s\n' "$database" >&2
  exit 2
fi

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
connection=(--skip-comments --protocol=TCP --host="$host" --port="$port" --user="$user" --batch --skip-column-names)
export MYSQL_PWD=$password

table_count=$(
  "$mysql_client" "${connection[@]}" --execute="SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$database' AND table_type = 'BASE TABLE'"
)

if [[ "$table_count" -gt 0 ]]; then
  migration_table=$(
    "$mysql_client" "${connection[@]}" --execute="SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$database' AND table_name = 'schema_migrations'"
  )
  if [[ "$migration_table" -eq 1 ]]; then
    current=$(
      "$mysql_client" "${connection[@]}" "$database" --execute='SELECT version FROM schema_migrations ORDER BY installed_on DESC LIMIT 1'
    )
    if [[ "$current" == "1" ]]; then
      printf 'StarRocks schema migration 1 is already applied\n'
      exit 0
    fi
  fi
  printf 'Refusing to baseline non-empty unversioned StarRocks database %s\n' "$database" >&2
  exit 1
fi

"$mysql_client" "${connection[@]}" "$database" < "$root/src/main/resources/db-scripts/starrocks/schema.sql"
"$mysql_client" "${connection[@]}" "$database" < "$root/src/main/resources/db-scripts/starrocks/derived.sql"
"$mysql_client" "${connection[@]}" "$database" \
  --execute="INSERT INTO schema_migrations VALUES ('1', 'Initial cBioPortal StarRocks read contract', NOW())"

printf 'Applied StarRocks schema migration 1 to %s\n' "$database"
