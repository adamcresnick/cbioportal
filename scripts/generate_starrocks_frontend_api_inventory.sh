#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
frontend_root=${CBIOPORTAL_FRONTEND_ROOT:-$root/../cbioportal-frontend-v7.0.5}

exec node "$root/scripts/starrocks-inventory/generate_frontend_api_inventory.js" \
  --frontend-root "$frontend_root" "$@"
