-- ============================================================
-- Sentinel 传统业务模块 · 首批5个 表结构迁移（订单审核）
-- 用法：mysql -h127.0.0.1 -P3307 -uroot -proot123_A austin < doc/sql/z-sentinel-batch1.sql
-- 说明：只跑一次；存量订单 review_status 默认 APPROVED（不破坏现有演示流程），
--       新建订单由 FulfillmentService 显式置为 PENDING 走审核。
-- ============================================================
ALTER TABLE logistics_order
  ADD COLUMN review_status VARCHAR(16) NOT NULL DEFAULT 'APPROVED' COMMENT '订单审核状态: PENDING/APPROVED/REJECTED' AFTER current_node,
  ADD COLUMN reviewed_by VARCHAR(64) DEFAULT NULL COMMENT '审核人',
  ADD COLUMN reviewed_at DATETIME DEFAULT NULL COMMENT '审核时间',
  ADD COLUMN reject_reason VARCHAR(255) DEFAULT NULL COMMENT '驳回原因';
