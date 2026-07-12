#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
compose=(docker compose -f "$root/dev/starrocks/docker-compose.yml")

mkdir -p "$root/dev/starrocks/.data/fe-meta"

"${compose[@]}" up --build --wait
"$root/scripts/starrocks/run_deployment_smoke.py" "http://localhost:${CBIOPORTAL_PORT:-8080}"

printf 'cBioPortal StarRocks stack is ready at http://localhost:%s\n' "${CBIOPORTAL_PORT:-8080}"
