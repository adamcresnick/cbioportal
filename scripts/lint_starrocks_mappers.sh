#!/usr/bin/env bash
set -euo pipefail

mapper_dir="${1:-src/main/resources/mappers/starrocks}"

if [[ ! -d "${mapper_dir}" ]]; then
  echo "StarRocks mapper directory does not exist: ${mapper_dir}" >&2
  exit 1
fi

rejected_pattern='argMin|argMax|groupArray|groupArrayIf|arrayStringConcat|uniqExact|multiIf|splitByString|arrayMap|ARRAY[[:space:]]+JOIN|LowCardinality|MergeTree|FORMAT[[:space:]]+TSV|OPTIMIZE[[:space:]]+TABLE|SETTINGS[[:space:]]+join_algorithm|base64Encode'

matches="$(
  grep -RInE --include='*.xml' "${rejected_pattern}" "${mapper_dir}" \
    | grep -v 'STARROCKS_LINT_ALLOW:' \
    || true
)"

if [[ -n "${matches}" ]]; then
  echo "StarRocks mapper lint failed. Remove ClickHouse-only SQL constructs or add a line-level STARROCKS_LINT_ALLOW:<reason> comment with test coverage." >&2
  echo "${matches}" >&2
  exit 1
fi
