#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
e2e="$root/dev/starrocks/e2e"

npm ci --prefix "$e2e" --ignore-scripts
install=(npx --prefix "$e2e" playwright install)
if [[ "${PLAYWRIGHT_INSTALL_DEPS:-false}" == "true" ]]; then
  install+=(--with-deps)
fi
"${install[@]}" chromium
CBIOPORTAL_URL=${CBIOPORTAL_URL:-http://localhost:8080} npm --prefix "$e2e" test
