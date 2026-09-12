#!/usr/bin/env bash
# Sentinel MySQL fresh-init runner.
# Keep explicit ordering because Docker only guarantees lexical filename order.
set -euo pipefail

export MYSQL_PWD="${MYSQL_ROOT_PASSWORD:-}"
schema_dir="/schema"

echo "[sentinel-init] applying austin.sql"
mysql --protocol=socket -uroot --default-character-set=utf8mb4 < "${schema_dir}/austin.sql"

austin_scripts=(
  "sentinel.sql"
  "sentinel-tms.sql"
  "20260910-workorder-state-machine.sql"
  "20260910-outbox-event.sql"
  "20260911-liteflow-rule-db.sql"
  "z-sentinel-batch1.sql"
  "z-sentinel-closed-loop.sql"
  "20260911-fix-risk-rule-encoding.sql"
  "z-sentinel-rbac.sql"
  "z-sentinel-seed.sql"
  "20260911-fix-sentinel-user-nickname.sql"
  "z-sentinel-tms-seed.sql"
  "z-sentinel-order-migrate.sql"
  "z-sentinel-real-data.sql"
  "zz-sentinel-after-sale.sql"
  "zz-sentinel-after-sale-demo.sql"
  "zz-sentinel-api-key-enterprise.sql"
  "zz-sentinel-domestic.sql"
  "zz-sentinel-order-module.sql"
)

for file in "${austin_scripts[@]}"; do
  echo "[sentinel-init] applying ${file}"
  mysql --protocol=socket -uroot --default-character-set=utf8mb4 --database=austin < "${schema_dir}/${file}"
done


echo "[sentinel-init] done"