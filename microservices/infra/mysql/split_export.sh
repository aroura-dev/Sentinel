#!/usr/bin/env bash
# ============================================================
# sentinel-ms 分库导出：从 monolith 的 sentinel 库(3307) 按服务归属
# 把每张表 structure+data 导出成各服务 initdb 的 00_schema_data.sql，
# 保证与新微服务各进程的独立 MySQL 数据完全一致（快照式、幂等可重建）。
# 前提：sentinel-mysql(127.0.0.1:3307, root/root123_A) 已灌好全量种子。
# 用法： bash infra/mysql/split_export.sh
# ============================================================
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"          # sentinel-ms/infra
DEST="$ROOT/mysql"
MYSQL=(mysql -h127.0.0.1 -P3307 -uroot -proot123_A --default-character-set=utf8mb4)

# 表 → 服务 归属
auth="sentinel_user role role_menu"
msg="message_template sms_record channel_account unsubscribe"
agent="agent_call_log anomaly_knowledge"
logistics="after_sale api_key bill bill_item carrier carrier_channel carrier_rate inventory inventory_flow logistics_order logistics_track merchant notification_record operation_log product risk_rule warehouse waybill workorder"

svcs=(auth msg agent logistics)

mkdir -p "$DEST"
for svc in "${svcs[@]}"; do
  out="$DEST/$svc/init/00_schema_data.sql"
  mkdir -p "$(dirname "$out")"
  {
    echo "-- sentinel-ms : sentinel_${svc}（由 模块化单体版 库快照导出生成，勿手改）"
    echo "CREATE DATABASE IF NOT EXISTS sentinel_${svc} DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
    echo "USE sentinel_${svc};"
    echo "SET NAMES utf8mb4;"
    echo ""
  } > "$out"
  # mysqldump 在 sentinel-mysql 容器内执行（按 socket 连本容器 MySQL）
  # shellcheck disable=SC2086
  docker exec sentinel-mysql mysqldump -uroot -proot123_A --default-character-set=utf8mb4 --skip-comments --no-tablespaces sentinel ${!svc} >> "$out"
  echo "  [ok] $svc -> $(basename "$out")"
done

# ---- logistics 额外迁移：merchant.owner_username 冗余列（替代跨库 JOIN sentinel_user）----
OWNER="$DEST/logistics/init/02_owner_username.sql"
{
  echo "-- 一次性迁移：merchant 冗余 owner_username（替代跨库 JOIN sentinel_user，供物流服务行级过滤）"
  echo "-- 注意：mysql 5.7 不支持 ADD COLUMN IF NOT EXISTS，本脚本一次性执行；重复执行会报 duplicate column（可忽略或先 DROP）"
  echo "ALTER TABLE merchant ADD COLUMN owner_username VARCHAR(64) NULL COMMENT '商户登录用户名（冗余）' AFTER user_id;"
  echo ""
  echo "-- 离线快照回填：merchant.user_id → sentinel_user.username（auth/logistics 已分库，无法再跨库 JOIN）"
} > "$OWNER"
"${MYSQL[@]}" sentinel -N -e "
SELECT CONCAT('UPDATE merchant SET owner_username = ', QUOTE(COALESCE(u.username,'')), ' WHERE id = ', m.id, ' AND (owner_username IS NULL OR owner_username = '''');')
FROM merchant m LEFT JOIN sentinel_user u ON m.user_id = u.id;
" >> "$OWNER" 2>/dev/null || true
echo "  [ok] logistics -> 02_owner_username.sql"

echo "完成。产物见 infra/mysql/<svc>/init/*.sql"
