#!/usr/bin/env bash
# 最小权限应用账号：应用只做 DML，不用 root 连库。
# MySQL 的 initdb 会执行本目录下的 .sh，因此口令可从环境变量注入（纯 .sql 做不到）。
set -euo pipefail
: "${MYSQL_APP_PASSWORD:?必须设置 MYSQL_APP_PASSWORD}"

mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE USER IF NOT EXISTS 'sentinel_msg'@'%' IDENTIFIED BY '${MYSQL_APP_PASSWORD}';
ALTER USER 'sentinel_msg'@'%' IDENTIFIED BY '${MYSQL_APP_PASSWORD}';
GRANT SELECT, INSERT, UPDATE, DELETE ON sentinel_msg.* TO 'sentinel_msg'@'%';
FLUSH PRIVILEGES;
SQL

echo "[initdb] 已创建最小权限账号 sentinel_msg（仅 sentinel_msg 的 DML 权限）"
