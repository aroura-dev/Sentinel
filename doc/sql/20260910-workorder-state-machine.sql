-- P0-1: 异常工单状态机（乐观锁 version + 状态索引）
-- 幂等执行：存在则跳过
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'workorder' AND COLUMN_NAME = 'version'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `workorder` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'workorder' AND INDEX_NAME = 'idx_status_updated'
);
SET @ddl2 := IF(@idx_exists = 0,
  'ALTER TABLE `workorder` ADD INDEX `idx_status_updated` (`status`, `updated_at`)',
  'SELECT 1');
PREPARE stmt2 FROM @ddl2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;