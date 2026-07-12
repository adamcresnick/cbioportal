#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
compose=(docker compose -f "$root/dev/starrocks/docker-compose.yml")
logs=$(mktemp)
trap 'rm -f "$logs"' EXIT

"${compose[@]}" logs --no-color cbioportal > "$logs"

if grep -Eiq 'jdbc:ch://|ClickHouseDriver|mappers/clickhouse|SQLSyntaxErrorException|BadSqlGrammarException|Invalid bound statement|Table .* does not exist' "$logs"; then
  printf 'Unexpected ClickHouse or SQL failure evidence in cBioPortal logs:\n' >&2
  grep -Ein 'jdbc:ch://|ClickHouseDriver|mappers/clickhouse|SQLSyntaxErrorException|BadSqlGrammarException|Invalid bound statement|Table .* does not exist' "$logs" >&2
  exit 1
fi

if "${compose[@]}" ps --services --status running | grep -Eiq 'clickhouse'; then
  printf 'A ClickHouse service is running in the StarRocks deployment\n' >&2
  exit 1
fi

printf 'No ClickHouse runtime or SQL failure evidence found\n'
