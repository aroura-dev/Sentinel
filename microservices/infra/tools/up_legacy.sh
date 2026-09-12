#!/usr/bin/env bash
# ============================================================
# 启动 legacy 单体后端(:8090) 供 strangler 网关回源。
# 作用：原版 Vue UI 中尚未迁到微服务的接口，经网关统一回源到这里(全量接口)，
#       使 UI 全页面有数据；迁移进度 = 把更多前缀从 legacy 切到对应微服务。
# 前提：sentinel-mysql(:3307, 源 sentinel 基建) 在跑；与 auth 共享 Redis(:6381) 使 token 互通。
# 用法： bash infra/tools/up_legacy.sh
# ============================================================
set -e
SRC=/d/internship/sentinel
JAR="$SRC/sentinel-web/target/sentinel-web-0.0.1-SNAPSHOT.jar"
SETTINGS="$SRC/doc/maven/settings.xml"

# 已有 8090 则跳过
if netstat -ano 2>/dev/null | grep -qE ":8090\s.*LISTEN"; then echo "legacy 已在 :8090"; exit 0; fi

if [ ! -f "$JAR" ]; then
  echo "打包 legacy jar(首次较慢)..."
  (cd "$SRC" && mvn -s "$SETTINGS" -pl sentinel-web -am package -DskipTests)
fi

echo "启动 legacy :8090 (DB=sentinel@3307, Redis=6381 共享 token)"
(cd "$SRC" && DASHSCOPE_API_KEY=sk-dummy java -jar "$JAR" \
  --server.port=8090 \
  --sentinel.database.ip=127.0.0.1 --sentinel.database.port=3307 \
  --sentinel.redis.ip=127.0.0.1 --sentinel.redis.port=6381 --sentinel.redis.password=sentinel \
  > /tmp/mono.log 2>&1 &)

for i in $(seq 1 30); do sleep 5
  netstat -ano 2>/dev/null | grep -qE ":8090\s.*LISTEN" && { echo "legacy ready ~$((i*5))s"; break; }
done
