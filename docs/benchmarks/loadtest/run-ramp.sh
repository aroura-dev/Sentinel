#!/usr/bin/env bash
# Sentinel 并发阶梯压测。每档固定时长，边压边采样容器资源，便于事后归因瓶颈。
#
# 用法：
#   PORT=5173 DUR=60 RAMP=10 LEVELS="1 5 10 25 50 100 200" bash run-ramp.sh
#
# 产物（写在脚本所在目录）：ramp-<并发>.jtl、stats-<并发>.csv、jmeter-<并发>.log
# 汇总用同目录的 report.sh。
set -u

# 默认用宿主 JDK17 跑 JMeter；JMETER_HOME 可覆盖
JM=${JMETER_HOME:-/d/tools/apache-jmeter-5.6.3}/bin/jmeter
export JAVA_HOME=${JAVA_HOME:-/d/JDK/Java/jdk-17}
export JVM_ARGS=${JVM_ARGS:-"-Xms1g -Xmx2g"}

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR" || exit 1

PORT=${PORT:-5173}
DUR=${DUR:-60}
RAMP=${RAMP:-10}
LEVELS=${LEVELS:-"1 5 10 25 50 100 200"}

echo "### 目标 127.0.0.1:${PORT}  每档 ${DUR}s（含 ${RAMP}s 爬坡）  档位: ${LEVELS}"
echo "### 开始 $(date '+%H:%M:%S')"

for N in $LEVELS; do
  echo "=== level=${N}  起 $(date '+%H:%M:%S') ==="
  rm -f "ramp-${N}.jtl" "jmeter-${N}.log" "stats-${N}.csv"

  # 后台每 5s 采一次容器资源
  (
    end=$((SECONDS + DUR + RAMP + 30))
    while [ $SECONDS -lt $end ]; do
      docker stats --no-stream --format '{{.Name}},{{.CPUPerc}},{{.MemUsage}}' \
        sentinel-backend sentinel-mysql sentinel-redis 2>/dev/null | sed "s/^/${N},/"
      sleep 5
    done > "stats-${N}.csv"
  ) &
  STATS_PID=$!

  "$JM" -n -t sentinel-loadtest.jmx -l "ramp-${N}.jtl" \
        -Jthreads="$N" -Jrampup="$RAMP" -Jduration="$DUR" -Jport="$PORT" \
        -j "jmeter-${N}.log" 2>&1 | grep -E 'summary =|Err:'

  kill "$STATS_PID" 2>/dev/null
  wait "$STATS_PID" 2>/dev/null
  echo "=== level=${N}  完 $(date '+%H:%M:%S') ==="
done

echo "### 全部完成 $(date '+%H:%M:%S')"
