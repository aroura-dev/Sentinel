#!/usr/bin/env bash
# ============================================================
# Sentinel 本地开发启动（Docker 中间件 + 宿主机后端/前端）
# 用法：bash start-local.sh
#
# 默认配置与 docker-compose.sentinel.yml 保持一致：
#   MySQL   127.0.0.1:3307  root/root123_A
#   Redis   127.0.0.1:6379  sentinel
#   Backend http://127.0.0.1:8080
#   Frontend http://127.0.0.1:5173
# ============================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

COMPOSE_FILE="docker-compose.sentinel.yml"
DOCKER_BIN="${DOCKER_BIN:-docker}"
JAR="sentinel-web/target/sentinel-web-0.0.1-SNAPSHOT.jar"

for cmd in "$DOCKER_BIN" java mvn npm; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "缺少命令：$cmd" >&2
    exit 1
  fi
done

# 加载本地环境变量；可覆盖数据库、Redis 和模型配置
if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
fi

SENTINEL_DATABASE_HOST="${SENTINEL_DATABASE_HOST:-127.0.0.1}"
SENTINEL_DATABASE_PORT="${SENTINEL_DATABASE_PORT:-3307}"
SENTINEL_DATABASE_PASSWORD="${SENTINEL_DATABASE_PASSWORD:-root123_A}"
SENTINEL_REDIS_HOST="${SENTINEL_REDIS_HOST:-127.0.0.1}"
SENTINEL_REDIS_PORT="${SENTINEL_REDIS_PORT:-6379}"
SENTINEL_REDIS_PASSWORD="${SENTINEL_REDIS_PASSWORD:-sentinel}"

echo ">> 启动 MySQL 和 Redis 容器..."
"$DOCKER_BIN" compose -f "$COMPOSE_FILE" up -d --wait mysql redis

if [ ! -f "$JAR" ]; then
  echo ">> 未找到后端 jar，开始打包（首次较慢）..."
  mvn -ntp -pl sentinel-web -am -DskipTests package
fi

if [ ! -d sentinel-frontend/node_modules ]; then
  echo ">> 安装前端依赖..."
  (cd sentinel-frontend && npm ci)
fi

echo ">> 启动后端 sentinel-web (8080)..."
java -jar "$JAR" \
  --sentinel.database.ip="$SENTINEL_DATABASE_HOST" \
  --sentinel.database.port="$SENTINEL_DATABASE_PORT" \
  --sentinel.database.password="$SENTINEL_DATABASE_PASSWORD" \
  --sentinel.redis.ip="$SENTINEL_REDIS_HOST" \
  --sentinel.redis.port="$SENTINEL_REDIS_PORT" \
  --sentinel.redis.password="$SENTINEL_REDIS_PASSWORD" \
  > deploy-run.log 2>&1 &
BACK_PID=$!
echo "   后端 PID=$BACK_PID，日志见 deploy-run.log"

echo ">> 启动前端 sentinel-frontend (5173)..."
(cd sentinel-frontend && npm run dev -- --host 127.0.0.1 --port 5173 > ../frontend-run.log 2>&1) &
FRONT_PID=$!
echo "   前端 PID=$FRONT_PID，日志见 frontend-run.log"

cleanup() {
  echo ""
  echo ">> 停止宿主机进程..."
  kill "$BACK_PID" "$FRONT_PID" 2>/dev/null || true
}
trap cleanup INT TERM EXIT

echo ""
echo "============================================="
echo "  前端管理端: http://127.0.0.1:5173  (admin/Admin@123)"
echo "  后端 API  : http://127.0.0.1:8080"
echo "  Swagger   : http://127.0.0.1:8080/swagger-ui/index.html"
echo "  按 Ctrl+C 停止后端和前端"
echo "============================================="
wait
