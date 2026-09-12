-- ============================================================
-- Sentinel 售后闭环·理赔流程：工单赔付字段（可重复执行）
--   在 workorder 上承载理赔流程（索赔登记 → 审批 → 赔付/驳回），
--   与 after_sale（退货退款）一起构成完整售后闭环。
--   对存量工单：凡已有索赔/理赔金额的，回填为「待审批」进入理赔流程。
-- ============================================================

-- 1. workorder：理赔状态与节点时间
SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = DATABASE() AND table_name = 'workorder' AND column_name = 'claim_status');
SET @sql := IF(@col = 0, 'ALTER TABLE workorder ADD COLUMN claim_status VARCHAR(16) NOT NULL DEFAULT ''NONE'' COMMENT ''理赔状态:NONE/SUBMITTED/APPROVED/PAID/REJECTED''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = DATABASE() AND table_name = 'workorder' AND column_name = 'claim_submitted_at');
SET @sql := IF(@col = 0, 'ALTER TABLE workorder ADD COLUMN claim_submitted_at DATETIME DEFAULT NULL COMMENT ''索赔提交时间''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = DATABASE() AND table_name = 'workorder' AND column_name = 'claim_approved_at');
SET @sql := IF(@col = 0, 'ALTER TABLE workorder ADD COLUMN claim_approved_at DATETIME DEFAULT NULL COMMENT ''理赔审批时间''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = DATABASE() AND table_name = 'workorder' AND column_name = 'claim_paid_at');
SET @sql := IF(@col = 0, 'ALTER TABLE workorder ADD COLUMN claim_paid_at DATETIME DEFAULT NULL COMMENT ''赔付时间''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = DATABASE() AND table_name = 'workorder' AND column_name = 'claim_reject_reason');
SET @sql := IF(@col = 0, 'ALTER TABLE workorder ADD COLUMN claim_reject_reason VARCHAR(255) DEFAULT NULL COMMENT ''驳回原因''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 存量回填：历史已登记索赔/理赔的工单进入理赔流程（待审批）
UPDATE workorder
SET claim_status = 'SUBMITTED',
    claim_submitted_at = COALESCE(claim_submitted_at, resolved_at, updated_at, CURRENT_TIMESTAMP)
WHERE claim_status = 'NONE'
  AND is_deleted = 0
  AND (compensation_amount > 0 OR claim_amount > 0);
