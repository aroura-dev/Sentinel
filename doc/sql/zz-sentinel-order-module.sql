-- ============================================================
-- Sentinel 订单模块企业化改造：业务备注 + 运单商品明细（可重复执行）
--   business_notes：订单跨角色协同批注（已出库订单也可补充备注）
--   waybill.items_json：运单商品明细（整单出库=订单全量；分批出库=本次出库子集）
-- ============================================================

-- 1. 订单表：业务备注（协同批注，多人可见）
SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = DATABASE() AND table_name = 'logistics_order' AND column_name = 'business_notes');
SET @sql := IF(@col = 0, 'ALTER TABLE logistics_order ADD COLUMN business_notes TEXT NULL COMMENT ''业务备注（跨角色协同批注）''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 运单表：商品明细（分批出库记录本次出库商品）
SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = DATABASE() AND table_name = 'waybill' AND column_name = 'items_json');
SET @sql := IF(@col = 0, 'ALTER TABLE waybill ADD COLUMN items_json TEXT NULL COMMENT ''运单商品明细（分批出库子集）''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
