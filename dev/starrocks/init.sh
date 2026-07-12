#!/usr/bin/env bash
set -euo pipefail

database=${STARROCKS_DATABASE:-cbioportal}
portal_user=${PORTAL_DATABASE_USER:-cbioportal}
portal_password=${PORTAL_DATABASE_PASSWORD:-cbioportal-local-only}

for value in "$database" "$portal_user" "$portal_password"; do
  if [[ ! "$value" =~ ^[A-Za-z0-9_.-]+$ ]]; then
    printf 'Local StarRocks identifiers and password may contain only letters, digits, dot, underscore, and hyphen\n' >&2
    exit 2
  fi
done

connection=(--skip-comments --protocol=TCP --host="$STARROCKS_HOST" --port="$STARROCKS_PORT" --user=root)

until mysql "${connection[@]}" --execute='SELECT 1' >/dev/null 2>&1; do
  sleep 2
done

if ! mysql "${connection[@]}" --batch --skip-column-names --execute='SHOW BACKENDS' | grep -q 'starrocks-be'; then
  mysql "${connection[@]}" --execute='ALTER SYSTEM ADD BACKEND "starrocks-be:9050"'
fi

for _ in $(seq 1 60); do
  if mysql "${connection[@]}" --batch --skip-column-names --execute='SHOW BACKENDS' \
      | grep 'starrocks-be' | grep -q $'true\t'; then
    break
  fi
  sleep 2
done

mysql "${connection[@]}" --batch --skip-column-names --execute='SHOW BACKENDS' \
  | grep 'starrocks-be' | grep -q $'true\t'

MYSQL_CLIENT=mysql \
STARROCKS_USER=root \
STARROCKS_PASSWORD='' \
  /workspace/scripts/bootstrap_starrocks.sh --with-fixture

mysql "${connection[@]}" --execute="CREATE USER IF NOT EXISTS '$portal_user'@'%' IDENTIFIED BY '$portal_password'"
mysql "${connection[@]}" --execute="GRANT SELECT ON ALL TABLES IN DATABASE $database TO USER '$portal_user'@'%'"
mysql "${connection[@]}" --execute="GRANT INSERT, DELETE ON TABLE $database.users, $database.authorities, $database.data_access_tokens TO USER '$portal_user'@'%'"

MYSQL_PWD="$portal_password" mysql --skip-comments --protocol=TCP \
  --host="$STARROCKS_HOST" --port="$STARROCKS_PORT" --user="$portal_user" "$database" \
  --execute='SELECT COUNT(*) FROM schema_migrations' >/dev/null
