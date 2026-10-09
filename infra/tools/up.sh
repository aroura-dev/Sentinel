#!/usr/bin/env bash
# ============================================================
# sentinel-ms 一键构建+起栈+验收（可复现命令，README 推荐入口）
#   bash infra/tools/up.sh
# 行为：mvn 打包全部服务 -> 出 dist/ -> compose build/up
#      -> 等网关 -> SMOKE_DOCKER 模式端到端验收
# 拆除： bash infra/tools/down.sh
#
# 端口形态：
#   默认起**安全形态** —— 只发布 8080(网关) 与 5175(演示台)，业务服务与数据库
#   不发布到宿主机（业务服务信任 X-User-* 头，端口对宿主可达即鉴权可被绕过）。
#   需要直连调试端口（宿主 mysql 客户端、直连 8081-8084 等）时：
#     DEV_PORTS=1 bash infra/tools/up.sh
#   此时调试端口仅绑定 127.0.0.1，同局域网其他机器仍连不上。
# ============================================================
set -e
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

# 可选 Maven settings（通过 MAVEN_SETTINGS 指定）
S=""
if [ -n "${MAVEN_SETTINGS:-}" ] && [ -f "$MAVEN_SETTINGS" ]; then
  S="-s $MAVEN_SETTINGS"
fi

# 端口形态：默认安全形态，DEV_PORTS=1 叠加 dev 覆盖（调试端口绑回环）
CF="-f compose.infra.yml"
if [ "${DEV_PORTS:-0}" = "1" ]; then
  CF="$CF -f compose.dev.yml"
  echo "== 端口形态：dev（调试端口开放，仅绑定 127.0.0.1）=="
else
  echo "== 端口形态：安全（只发布 8080/5175）；需要调试端口请用 DEV_PORTS=1 =="
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
(cd infra/docker && docker compose $CF build)

echo "== 4/5 起整套 =="
(cd infra/docker && docker compose $CF up -d)

echo "== 5/5 等网关并验收 =="
# 注意两点：
# 1) 登录失败时接口返回 HTTP 200 + {"status":"A0001"}，所以不能用 curl -f 判就绪，必须看响应体；
# 2) 用户名用百分号编码写死，避免 Git Bash/MSYS2 在 Windows 上把中文参数转成 CP936。
GW_READY=0
for i in $(seq 1 60); do
  body=$(curl -s -m 10 -X POST http://localhost:8080/api/auth/login \
    --data 'username=%E5%BC%A0%E4%BC%9F&password=Admin@123' 2>/dev/null || true)
  if echo "$body" | grep -q '"status":"0"'; then
    echo "gateway ready ~$((i*5))s"; GW_READY=1; break
  fi
  sleep 5
done
[ "$GW_READY" = "1" ] || { echo "网关未在超时内就绪"; exit 1; }
SMOKE_DOCKER=1 bash infra/tools/smoke_closed_loop.sh
echo
echo "✅ up.sh 完成：栈已就绪(gateway :8080)，验收通过"
