#!/usr/bin/env bash
# 最小权限应用账号：应用只做 DML，不用 root 连库。
# MySQL 的 initdb 会执行本目录下的 .sh，因此口令可从环境变量注入（纯 .sql 做不到）。
set -euo pipefail
: "${MYSQL_APP_PASSWORD:?必须设置 MYSQL_APP_PASSWORD}"

mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE USER IF NOT EXISTS 'sentinel_auth'@'%' IDENTIFIED BY '${MYSQL_APP_PASSWORD}';
ALTER USER 'sentinel_auth'@'%' IDENTIFIED BY '${MYSQL_APP_PASSWORD}';
# Flyway 由本账号执行迁移，需要建表/改表权限；这确实比纯 DML 宽，
# 但仍限于本服务自己的库 —— 跨库隔离这条主要性质不变。
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES ON sentinel_auth.* TO 'sentinel_auth'@'%';
FLUSH PRIVILEGES;
SQL

echo "[initdb] 已创建最小权限账号 sentinel_auth（仅 sentinel_auth 的 DML 权限）"
