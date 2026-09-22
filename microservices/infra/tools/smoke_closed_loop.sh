#!/usr/bin/env bash
# ============================================================
# sentinel-ms 端到端验收（登录 -> 触发通知闭环 -> 断言三库各落行）
# 支持两种 DB 查询方式：
#   - 宿主机模式(默认)：需本机 mysql 客户端；DB 走 127.0.0.1:33062-33064
#   - Docker 模式：SMOKE_DOCKER=1，经 docker exec 容器内 mysql 查询（CI 无 mysql 客户端）
# 用法： bash infra/tools/smoke_closed_loop.sh        # host
#        SMOKE_DOCKER=1 bash infra/tools/smoke_closed_loop.sh
# ============================================================
set -e
GW=${GW:-http://localhost:8080}
DOCKER_MODE=${SMOKE_DOCKER:-0}

# 表-库查询：$1=域(auth/msg/logistics/agent)  $2=SQL
dbq() {
  local domain="$1" sql="$2"
  if [ "$DOCKER_MODE" = "1" ]; then
    docker exec "sentinelms-mysql-$domain" mysql -uroot -proot123_A -N -e "$sql" 2>/dev/null
  else
    case "$domain" in
      auth) local p=33061;; msg) local p=33062;; logistics) local p=33063;; agent) local p=33064;;
    esac
    mysql -h127.0.0.1 -P"$p" -uroot -proot123_A -N -e "$sql" 2>/dev/null
  fi
}

echo "== 1) 经网关登录 张伟 =="
# 用户名用百分号编码写死，避免 Git Bash/MSYS2 在 Windows 上把中文命令行参数
# 转成 ANSI 代码页（CP936）导致登录莫名失败。Linux/CI 上行为完全一致。
TOKEN=$(curl -s -X POST "$GW/api/auth/login" --data "username=%E5%BC%A0%E4%BC%9F&password=Admin@123" \
        | grep -oE '"token":"[a-f0-9]+"' | head -1 | cut -d'"' -f4)
[ -n "$TOKEN" ] && echo "   token=${TOKEN:0:12}..." || { echo "   登录失败"; exit 1; }

echo "== 2) 选无买家通知+数字手机号订单 =="
ON=$(dbq logistics "SELECT o.order_no FROM sentinel_logistics.logistics_order o \
  LEFT JOIN sentinel_logistics.notification_record n ON n.order_no=o.order_no AND n.role='buyer' \
  WHERE n.id IS NULL AND o.is_deleted=0 AND o.buyer_phone REGEXP '^1[0-9]{10}\$' LIMIT 1")
PH=$(dbq logistics "SELECT buyer_phone FROM sentinel_logistics.logistics_order WHERE order_no='$ON'")
echo "   order=$ON phone=$PH"; [ -n "$ON" ] || { echo "   无可用订单"; exit 1; }

echo "== 3/4) 触发通知并断言三库（最多 3 次，容忍重启后消费就绪竞态） =="
PASS=0
for attempt in 1 2 3; do
  NODE="SMOKE$RANDOM$attempt"
  HTTP=$(curl -s -o /tmp/smoke_resp.json -w "%{http_code}" -X POST \
    "$GW/api/logistics/notify/send?orderNo=$ON&node=$NODE&role=buyer&channel=sms" \
    -H "Authorization: Bearer $TOKEN")
  echo "   [try$attempt] node=$NODE http=$HTTP"
  [ "$HTTP" = "200" ] || { sleep 8; continue; }
  sleep 8

  STATUS=$(dbq logistics "SELECT status FROM sentinel_logistics.notification_record WHERE order_no='$ON' AND node='$NODE' AND role='buyer' ORDER BY id DESC LIMIT 1")
  AGENT=$(dbq agent "SELECT status FROM sentinel_agent.agent_call_log WHERE trace_id LIKE '$ON|$NODE%' ORDER BY id DESC LIMIT 1")
  SMS=$(dbq msg "SELECT CONCAT(phone,'|',status) FROM sentinel_msg.sms_record WHERE phone='$PH' ORDER BY id DESC LIMIT 1")
  echo "      [logistics]=$STATUS [agent]=$AGENT [msg]=$SMS"

  OK=1
  [ "$STATUS" = "SENT" ] || OK=0
  { [ "$AGENT" = "degraded" ] || [ "$AGENT" = "success" ]; } || OK=0
  echo "$SMS" | grep -q "^$PH|10$" || OK=0
  [ "$OK" = "1" ] && { PASS=1; break; }
  sleep 6
done

if [ "$PASS" = "1" ]; then echo; echo "✅ SMOKE PASS：登录+通知闭环三库落行"; else echo; echo "❌ SMOKE FAIL"; exit 1; fi
