#!/usr/bin/env bash
# ============================================================
# sentinel-ms 一键构建+起栈+验收（可复现命令，README 推荐入口）
#   bash infra/tools/up.sh
# 行为：mvn 打包全部服务 -> 出 dist/ -> compose build/up(整套13容器)
#      -> 等网关 -> SMOKE_DOCKER 模式端到端验收
# 拆除： bash infra/tools/down.sh
# ============================================================
set -e
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

# 可选 Maven settings（通过 MAVEN_SETTINGS 指定）
S=""
if [ -n "${MAVEN_SETTINGS:-}" ] && [ -f "$MAVEN_SETTINGS" ]; then
  S="-s $MAVEN_SETTINGS"
fi

echo "== 1/5 构建服务 jar =="
mvn $S -B -N install -q
mvn $S -B -pl services/auth-service,services/msg-service,services/logistics-service,services/agent-service,gateway -am package -DskipTests

echo "== 2/5 出 dist/ =="
mkdir -p dist
cp services/auth-service/target/auth-service.jar dist/
cp services/msg-service/target/msg-service.jar dist/
cp services/logistics-service/target/logistics-service.jar dist/
cp services/agent-service/target/agent-service.jar dist/
cp gateway/target/sentinel-gateway.jar dist/

echo "== 3/5 构建镜像 =="
(cd infra/docker && docker compose -f compose.infra.yml build)

echo "== 4/5 起整套 =="
(cd infra/docker && docker compose -f compose.infra.yml up -d)

echo "== 5/5 等网关并验收 =="
for i in $(seq 1 60); do
  if curl -sf -o /dev/null -X POST http://localhost:8080/api/auth/login -d 'username=zhangwei&password=Admin@123' 2>/dev/null; then
    echo "gateway ready ~$((i*5))s"; break
  fi
  sleep 5
done
SMOKE_DOCKER=1 bash infra/tools/smoke_closed_loop.sh
echo
echo "✅ up.sh 完成：栈已就绪(gateway :8080)，验收通过"
