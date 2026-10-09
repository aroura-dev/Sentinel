#!/usr/bin/env bash
# sentinel-ms 拆栈（保留数据卷）。要连数据一起清：down.sh -v
set -e
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT/infra/docker"
docker compose -f compose.infra.yml down "$@"
echo "已拆栈（未删卷，数据保留）"
