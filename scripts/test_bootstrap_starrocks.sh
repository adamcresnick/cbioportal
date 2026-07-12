#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

fake_mysql="$tmp/mysql"
trace="$tmp/invocations"

cat > "$fake_mysql" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$*" >> "$MYSQL_TRACE"
cat >/dev/null
if [[ "$*" == *"SELECT CONCAT("* ]]; then
  printf 'StarRocks schema ready: 63 tables\n'
fi
EOF
chmod +x "$fake_mysql"

MYSQL_TRACE="$trace" \
MYSQL_CLIENT="$fake_mysql" \
STARROCKS_DATABASE=cbioportal_test \
  "$root/scripts/bootstrap_starrocks.sh" --with-fixture > "$tmp/output"

[[ $(wc -l < "$trace") -eq 5 ]]
while IFS= read -r invocation; do
  [[ "$invocation" == *"--skip-comments"* ]]
done < "$trace"

grep -Fxq 'StarRocks schema ready: 63 tables' "$tmp/output"
