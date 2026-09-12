-- ============================================================
-- Sentinel · 业务闭环扩展：逆向售后 / 风险预警 / 库存台账 / 开放API
-- 幂等：CREATE TABLE IF NOT EXISTS + INSERT IGNORE，可重复执行
-- ============================================================

SET NAMES utf8mb4;
USE `sentinel`;

-- 1. 逆向售后退货单
CREATE TABLE IF NOT EXISTS `after_sale` (
  `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_no`      VARCHAR(64)  NOT NULL COMMENT '关联订单号',
  `merchant_id`   BIGINT       DEFAULT NULL,
  `type`          VARCHAR(32)  NOT NULL DEFAULT 'RETURN' COMMENT 'RETURN退货/EXCHANGE换货',
  `reason`        VARCHAR(255) DEFAULT NULL COMMENT '退货原因',
  `refund_amount` DECIMAL(12,2) DEFAULT 0 COMMENT '退款金额',
  `currency`      VARCHAR(8)   DEFAULT 'CNY',
  `status`        VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING受理/REFUNDING退款中/REFUNDED已退款/RESHIPPED已重发/CLOSED关闭',
  `buyer_name`    VARCHAR(64)  DEFAULT NULL,
  `buyer_address` VARCHAR(255) DEFAULT NULL,
  `created_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`    TINYINT DEFAULT 0,
  UNIQUE KEY `uk_after_sale_order` (`order_no`),
  KEY `idx_after_sale_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逆向售后退货单';

-- 2. 风险预警自动处置规则
CREATE TABLE IF NOT EXISTS `risk_rule` (
  `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name`          VARCHAR(64) NOT NULL,
  `scene`         VARCHAR(32) NOT NULL COMMENT 'SLA/ANOMALY',
  `trigger_status` VARCHAR(32) DEFAULT NULL COMMENT 'RISK/BREACHED',
  `action`        VARCHAR(64) NOT NULL COMMENT 'NOTIFY通知/CREATE_WORKORDER建工单',
  `action_config` VARCHAR(255) DEFAULT NULL,
  `enabled`       TINYINT DEFAULT 1,
  `created_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`    TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险预警自动处置规则';

-- 3. 库存台账（SKU × 仓库）
CREATE TABLE IF NOT EXISTS `inventory` (
  `id`           BIGINT AUTO_INCREMENT PRIMARY KEY,
  `sku`          VARCHAR(64) NOT NULL,
  `product_id`   BIGINT      DEFAULT NULL,
  `merchant_id`  BIGINT      DEFAULT NULL,
  `warehouse_id` BIGINT      DEFAULT NULL,
  `on_hand`      INT         DEFAULT 0 COMMENT '在库可用',
  `reserved`     INT         DEFAULT 0 COMMENT '已占用',
  `created_at`   DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`   TINYINT DEFAULT 0,
  UNIQUE KEY `uk_inventory_sku_wh` (`sku`, `warehouse_id`),
  KEY `idx_inventory_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU 库存台账';

-- 4. 库存出入库流水（业务单据驱动）
CREATE TABLE IF NOT EXISTS `inventory_flow` (
  `id`           BIGINT AUTO_INCREMENT PRIMARY KEY,
  `sku`          VARCHAR(64) NOT NULL,
  `biz_no`       VARCHAR(64) DEFAULT NULL COMMENT '业务单据号(订单/运单)',
  `biz_type`     VARCHAR(32) NOT NULL COMMENT 'OUT出库/IN入库/REFUND退回/ADJUST调整',
  `qty`          INT NOT NULL,
  `warehouse_id` BIGINT DEFAULT NULL,
  `created_at`   DATETIME DEFAULT CURRENT_TIMESTAMP,
  `is_deleted`   TINYINT DEFAULT 0,
  KEY `idx_flow_sku` (`sku`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存出入库流水';

-- 5. 开放 API 密钥
CREATE TABLE IF NOT EXISTS `api_key` (
  `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
  `app_name`   VARCHAR(64) NOT NULL,
  `api_key`    VARCHAR(64) NOT NULL,
  `secret`     VARCHAR(64) NOT NULL,
  `scope`      VARCHAR(128) DEFAULT 'order:read' COMMENT '授权范围',
  `status`     TINYINT DEFAULT 1 COMMENT '1启用 0停用',
  `created_by` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT DEFAULT 0,
  UNIQUE KEY `uk_api_key` (`api_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放 API 密钥';

-- 6. 初始数据：SKU 库存回填（基于现有商品，仓库 1，初始 500）
INSERT IGNORE INTO `inventory` (`sku`, `product_id`, `merchant_id`, `warehouse_id`, `on_hand`, `reserved`)
SELECT p.sku, p.id, p.merchant_id, 1, 500, 0
FROM `product` p
WHERE p.is_deleted = 0;

-- 7. 初始风险处置规则
INSERT IGNORE INTO `risk_rule` (`name`, `scene`, `trigger_status`, `action`, `action_config`, `enabled`) VALUES
('SLA 违约自动建工单', 'SLA', 'BREACHED', 'CREATE_WORKORDER', '{"level":"HIGH"}', 1),
('SLA 预警自动通知',   'SLA', 'RISK',     'NOTIFY',           '{"channel":"email"}', 1);
