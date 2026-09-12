#!/usr/bin/env bash
# ============================================================
# Sentinel 本地一键启动（宿主机直连 Docker 中间件）
# 用法： bash start-local.sh   （Git Bash / WSL / Linux）
#
# 前提：
#   1. Docker 容器 austin-mysql(宿主机3307) / austin-redis(6380) 已在运行
#   2. 已打包后端 jar（首次可运行： mvn -pl austin-web -am package -DskipTests）
#   3. （可选）真实 LLM 需设置环境变量： export DASHSCOPE_API_KEY=sk-...
# ============================================================
set -e
cd "$(dirname "$0")"

# 加载本地环境变量（如 DASHSCOPE_API_KEY），无 .env 则跳过
if [ -f .env ]; then
  set -a
  . ./.env
  set +a
fi

JAR=austin-web/target/austin-web-0.0.1-SNAPSHOT.jar

if [ ! -f "$JAR" ]; then
  echo ">> 未找到 jar，开始打包（首次较慢）..."
  mvn -pl austin-web -am package -DskipTests
fi

echo ">> 启动后端 austin-web (8080) ..."
java -jar "$JAR" \
  --austin.database.ip=127.0.0.1 \
  --austin.database.port=3307 \
  --austin.redis.ip=127.0.0.1 \
  --austin.redis.port=6380 \
  --austin.redis.password=austin \
  > deploy-run.log 2>&1 &
BACK_PID=$!
echo "   后端 PID=$BACK_PID，日志见 deploy-run.log"

echo ">> 启动前端 sentinel-admin (3100) ..."
(cd sentinel-admin && npm start > ../frontend-run.log 2>&1) &
FRONT_PID=$!
echo "   前端 PID=$FRONT_PID，日志见 frontend-run.log"

echo ""
echo "============================================="
echo "  前端管理端: http://127.0.0.1:3100  (admin/admin123)"
echo "  后端 API  : http://127.0.0.1:8080"
echo "  Swagger   : http://127.0.0.1:8080/swagger-ui/index.html"
echo "============================================="
