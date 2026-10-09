#!/usr/bin/env bash
# ============================================================
# 从 backup.sh 产出的目录恢复四个服务库
#
#   bash infra/tools/restore.sh <备份目录> [--yes]
#
# ⚠️ 这是**破坏性**操作：每个库会被 DROP 后重建。
#    默认会要求确认，除非显式传 --yes。
#
# 为什么恢复脚本要单独存在：没有恢复演练的备份不算备份 ——
# 「能不能 restore」只有在真 restore 过一次之后才知道。
#
# 备份： bash infra/tools/backup.sh
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DIR="${1:-}"
ASSUME_YES="${2:-}"

if [ -z "$DIR" ] || [ ! -d "$DIR" ]; then
  echo "用法: bash infra/tools/restore.sh <备份目录> [--yes]"
  echo
  echo "可用备份:"
  ls -1 "$ROOT/backups" 2>/dev/null | sed 's/^/  /' || echo "  （无）"
  exit 1
fi

declare -A DATABASES=(
  [sentinel_auth]=sentinelms-mysql-auth
  [sentinel_msg]=sentinelms-mysql-msg
  [sentinel_logistics]=sentinelms-mysql-logistics
  [sentinel_agent]=sentinelms-mysql-agent
)

echo "将从此目录恢复: $DIR"
[ -f "$DIR/manifest.txt" ] && sed 's/^/  /' "$DIR/manifest.txt"
echo
echo "⚠️  每个库会被 DROP 并重建，当前数据全部丢失。"
if [ "$ASSUME_YES" != "--yes" ]; then
  read -r -p "确认继续？(输入 yes) " answer
  [ "$answer" = "yes" ] || { echo "已取消"; exit 1; }
fi
echo

for db in "${!DATABASES[@]}"; do
  container="${DATABASES[$db]}"
  file="$DIR/$db.sql"
  printf '  %-22s ' "$db"

  if [ ! -f "$file" ]; then
    echo "跳过（备份中无此库）"
    continue
  fi
  if ! docker ps --format '{{.Names}}' | grep -qx "$container"; then
    echo "跳过（容器 $container 未运行）"
    continue
  fi

  # 转储带 CREATE DATABASE，故恢复时无需指定库名
  if docker exec -i "$container" mysql \
       -uroot -p"${MYSQL_ROOT_PASSWORD:-root123_A}" < "$file" 2>/dev/null; then
    tables=$(docker exec "$container" mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-root123_A}" -N \
             -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$db';" 2>/dev/null)
    echo "已恢复（当前 $tables 张表）"
  else
    echo "失败"
    exit 1
  fi
done

echo
echo "✅ 恢复完成。"
echo "   注意：服务进程里的连接池与缓存不会自动感知 —— 建议一并重启服务容器："
echo "     cd infra/docker && docker compose -f compose.infra.yml restart \\"
echo "       auth-service msg-service logistics-service agent-service"
