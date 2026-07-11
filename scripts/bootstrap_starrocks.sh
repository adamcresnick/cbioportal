#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: scripts/bootstrap_starrocks.sh [--with-fixture]

Environment:
  STARROCKS_HOST      FE host (default: 127.0.0.1)
  STARROCKS_PORT      FE MySQL protocol port (default: 9030)
  STARROCKS_USER      database user (default: root)
  STARROCKS_PASSWORD  database password (default: empty)
  STARROCKS_DATABASE  target database (default: cbioportal)
  MYSQL_CLIENT        mysql-compatible client executable (default: mysql)
EOF
}

load_fixture=false
case "${1:-}" in
  "") ;;
  --with-fixture) load_fixture=true ;;
  --help|-h) usage; exit 0 ;;
  *) usage >&2; exit 2 ;;
esac

host=${STARROCKS_HOST:-127.0.0.1}
port=${STARROCKS_PORT:-9030}
user=${STARROCKS_USER:-root}
password=${STARROCKS_PASSWORD:-}
database=${STARROCKS_DATABASE:-cbioportal}
mysql_client=${MYSQL_CLIENT:-mysql}

if [[ ! "$database" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
  printf 'STARROCKS_DATABASE must be a simple SQL identifier: %s\n' "$database" >&2
  exit 2
fi

if ! command -v "$mysql_client" >/dev/null 2>&1; then
  printf 'mysql-compatible client not found: %s\n' "$mysql_client" >&2
  exit 127
fi

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
connection=(--protocol=TCP --host="$host" --port="$port" --user="$user" --default-character-set=utf8mb4)
export MYSQL_PWD=$password

"$mysql_client" "${connection[@]}" --execute="CREATE DATABASE IF NOT EXISTS \`$database\`"
"$mysql_client" "${connection[@]}" "$database" < "$root/src/main/resources/db-scripts/starrocks/schema.sql"
"$mysql_client" "${connection[@]}" "$database" < "$root/src/main/resources/db-scripts/starrocks/derived.sql"

if [[ "$load_fixture" == true ]]; then
  "$mysql_client" "${connection[@]}" "$database" < "$root/src/test/resources/starrocks/seed.sql"
fi

"$mysql_client" "${connection[@]}" "$database" --batch --skip-column-names \
  --execute="SELECT CONCAT('StarRocks schema ready: ', COUNT(*), ' tables') FROM information_schema.tables WHERE table_schema = '$database' AND table_type = 'BASE TABLE'"
