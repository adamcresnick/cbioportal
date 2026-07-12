#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
data_dir="$root/dev/starrocks/.data"
docker compose -f "$root/dev/starrocks/docker-compose.yml" down --volumes --remove-orphans
if [[ -d "$data_dir" ]]; then
  docker run --rm \
    --mount "type=bind,source=$data_dir,target=/data" \
    --entrypoint bash starrocks/fe-ubuntu:3.5.19 \
    -c 'shopt -s dotglob nullglob; rm -rf /data/*'
  rm -rf "$data_dir"
fi
