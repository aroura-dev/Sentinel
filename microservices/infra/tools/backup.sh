#!/usr/bin/env bash
# ============================================================
# 备份四个服务库
#
#   bash infra/tools/backup.sh [输出目录]
#
# 默认输出到 ./backups/<时间戳>/，每个库一个 .sql 文件。
#
# 为什么需要它：四个库各自只有一个 Docker 卷，那是唯一副本。
# 「卷还在」在生产里是最先被打破的假设 —— 误删、磁盘故障、卷被
# down -v 顺手带走都算。
#
# 恢复： bash infra/tools/restore.sh <备份目录>
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="${1:-$ROOT/backups/$STAMP}"

# 库名 -> 容器名。容器名固定（compose 里显式指定），不随项目名变化。
declare -A DATABASES=(
  [sentinel_auth]=sentinelms-mysql-auth
  [sentinel_msg]=sentinelms-mysql-msg
  [sentinel_logistics]=sentinelms-mysql-logistics
  [sentinel_agent]=sentinelms-mysql-agent
)

mkdir -p "$OUT"
echo "备份到: $OUT"
echo

for db in "${!DATABASES[@]}"; do
  container="${DATABASES[$db]}"
  file="$OUT/$db.sql"
  printf '  %-22s ' "$db"

  if ! docker ps --format '{{.Names}}' | grep -qx "$container"; then
    echo "跳过（容器 $container 未运行）"
    continue
  fi

  # --single-transaction：InnoDB 下不加全局锁即可拿到一致快照，
  # 不会阻塞正在写入的服务。
  # --databases：让转储带上 CREATE DATABASE / USE，恢复时无需再指定库名。
  if docker exec "$container" mysqldump \
       -uroot -p"${MYSQL_ROOT_PASSWORD:-root123_A}" \
       --single-transaction --routines --triggers --databases "$db" \
       > "$file" 2>/dev/null; then
    printf '%s (%s)\n' "$(du -h "$file" | cut -f1)" "$(grep -c '^INSERT INTO' "$file" || true) 条 INSERT"
  else
    echo "失败"
    exit 1
  fi
done

# 记录一份环境信息，便于事后判断这份备份来自哪个版本
{
  echo "backup_time=$STAMP"
  echo "git_commit=$(git -C "$ROOT" rev-parse HEAD 2>/dev/null || echo unknown)"
  echo "git_dirty=$(git -C "$ROOT" status --porcelain 2>/dev/null | wc -l)"
} > "$OUT/manifest.txt"

echo
echo "✅ 备份完成：$OUT"
echo "   恢复： bash infra/tools/restore.sh $OUT"
