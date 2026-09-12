-- ============================================================
-- Sentinel · 国内物流协同 TMS - 新增数据表
-- 在 sentinel.sql 基线之上新增的 TMS 域表（幂等，可重复执行）
-- 注意：本文件只 CREATE TABLE IF NOT EXISTS，无 DROP，避免误删业务数据
-- ============================================================

USE `austin`;

-- -----------------------------------------------------------
-- 1. 商家（卖家）主数据
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `merchant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `merchant_code` VARCHAR(32) NOT NULL COMMENT '商家编码',
    `merchant_name` VARCHAR(128) NOT NULL COMMENT '商家名称',
    `user_id` BIGINT DEFAULT NULL COMMENT '关联sentinel_user.id(登录账号)',
    `contact_name` VARCHAR(64) DEFAULT NULL COMMENT '联系人',
    `contact_phone` VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    `contact_email` VARCHAR(128) DEFAULT NULL COMMENT '联系邮箱',
    `country` VARCHAR(32) DEFAULT 'CN' COMMENT '所在国家',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_merchant_code` (`merchant_code`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家(卖家)主数据';

-- -----------------------------------------------------------
-- 2. 承运商主数据
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `carrier` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `carrier_code` VARCHAR(32) NOT NULL COMMENT '承运商编码',
    `carrier_name` VARCHAR(128) NOT NULL COMMENT '承运商名称',
    `type` VARCHAR(16) NOT NULL COMMENT '默认运输方式:rail/air/sea/express',
    `country` VARCHAR(32) DEFAULT 'CN' COMMENT '所在国家',
    `api_endpoint` VARCHAR(256) DEFAULT NULL COMMENT '轨迹对接接口(预留)',
    `api_key` VARCHAR(256) DEFAULT NULL COMMENT '对接密钥(预留)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_carrier_code` (`carrier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='承运商主数据';

-- -----------------------------------------------------------
-- 3. 物流渠道（承运商 × 目的国 × 时效）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `carrier_channel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `carrier_id` BIGINT NOT NULL COMMENT '承运商ID',
    `channel_code` VARCHAR(64) NOT NULL COMMENT '渠道编码',
    `channel_name` VARCHAR(128) NOT NULL COMMENT '渠道名称',
    `type` VARCHAR(16) NOT NULL COMMENT '运输方式:rail/air/sea/express',
    `dest_country` VARCHAR(8) NOT NULL COMMENT '目的国(RU/US/BR/DE)',
    `transit_days_min` INT NOT NULL DEFAULT 1 COMMENT 'SLA最小时效(天)',
    `transit_days_max` INT NOT NULL DEFAULT 10 COMMENT 'SLA最大时效(天)',
    `tracking_prefix` VARCHAR(8) DEFAULT NULL COMMENT 'tracking_no前缀',
    `min_billable_weight_kg` DECIMAL(10,3) DEFAULT 0 COMMENT '最小计费重量',
    `vol_divisor` INT DEFAULT 5000 COMMENT '体积重系数(空/快5000,铁/海6000)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_channel_code` (`channel_code`),
    KEY `idx_carrier_id` (`carrier_id`),
    KEY `idx_dest_country` (`dest_country`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流渠道(承运人×目的国×时效)';

-- -----------------------------------------------------------
-- 4. 运费价卡（渠道 × 区域 × 重量段）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `carrier_rate` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `channel_id` BIGINT NOT NULL COMMENT '渠道ID',
    `zone` VARCHAR(16) NOT NULL DEFAULT 'DEFAULT' COMMENT '区域(目的国/分区),DEFAULT=渠道全域',
    `min_weight_kg` DECIMAL(10,3) NOT NULL COMMENT '重量段下限',
    `max_weight_kg` DECIMAL(10,3) DEFAULT NULL COMMENT '重量段上限(NULL=开放上限)',
    `mode` VARCHAR(20) NOT NULL COMMENT '计费方式:PER_KG单价 | FIRST_CONTINUED首续重',
    `first_weight_kg` DECIMAL(10,3) DEFAULT 0 COMMENT '首重(kg)',
    `first_price` DECIMAL(12,4) DEFAULT 0 COMMENT '首重价格',
    `continued_weight_kg` DECIMAL(10,3) DEFAULT 0 COMMENT '续重单位(kg)',
    `continued_price` DECIMAL(12,4) DEFAULT 0 COMMENT '续重单价',
    `price` DECIMAL(12,4) DEFAULT 0 COMMENT 'PER_KG单价',
    `currency` VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    `effective_from` DATE NOT NULL COMMENT '生效日期',
    `effective_to` DATE DEFAULT NULL COMMENT '失效日期(NULL=长期有效)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_channel_id` (`channel_id`),
    KEY `idx_zone` (`zone`),
    KEY `idx_weight` (`min_weight_kg`, `max_weight_kg`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运费价卡(渠道×区域×重量段)';

-- -----------------------------------------------------------
-- 5. 发货仓库
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `warehouse` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `warehouse_code` VARCHAR(32) NOT NULL COMMENT '仓库编码',
    `warehouse_name` VARCHAR(128) NOT NULL COMMENT '仓库名称',
    `country` VARCHAR(8) DEFAULT 'CN' COMMENT '所在国家',
    `city` VARCHAR(64) DEFAULT NULL COMMENT '城市',
    `address` VARCHAR(256) DEFAULT NULL COMMENT '地址',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_warehouse_code` (`warehouse_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发货仓库';

-- -----------------------------------------------------------
-- 6. 商品 SKU（HS编码/申报/重量）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `merchant_id` BIGINT NOT NULL COMMENT '商家ID',
    `sku` VARCHAR(64) NOT NULL COMMENT 'SKU编码',
    `name` VARCHAR(256) NOT NULL COMMENT '商品名称',
    `hs_code` VARCHAR(32) DEFAULT NULL COMMENT '海关HS编码',
    `declared_value` DECIMAL(12,2) DEFAULT 0 COMMENT '单件申报价值',
    `currency` VARCHAR(8) DEFAULT 'CNY' COMMENT '申报币种',
    `weight_kg` DECIMAL(10,3) NOT NULL COMMENT '单件实重kg',
    `volume_l` DECIMAL(10,3) DEFAULT 0 COMMENT '单件体积L',
    `origin_country` VARCHAR(8) DEFAULT 'CN' COMMENT '原产国',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_merchant_sku` (`merchant_id`, `sku`),
    KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU(HS编码/申报/重量)';

-- -----------------------------------------------------------
-- 7. 运单（出库生成，运费快照）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `waybill` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `waybill_no` VARCHAR(64) NOT NULL COMMENT '运单号',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `merchant_id` BIGINT DEFAULT NULL COMMENT '商家ID',
    `channel_id` BIGINT NOT NULL COMMENT '渠道ID',
    `carrier_id` BIGINT DEFAULT NULL COMMENT '承运商ID',
    `tracking_no` VARCHAR(64) NOT NULL COMMENT '跟踪号',
    `carrier_code` VARCHAR(32) DEFAULT NULL COMMENT '承运商编码',
    `weight_kg` DECIMAL(10,3) DEFAULT NULL COMMENT '实重kg',
    `volume_l` DECIMAL(10,3) DEFAULT NULL COMMENT '体积L',
    `billable_weight_kg` DECIMAL(10,3) DEFAULT NULL COMMENT '计费重kg',
    `declared_value` DECIMAL(12,2) DEFAULT 0 COMMENT '申报价值',
    `declared_currency` VARCHAR(8) DEFAULT 'CNY' COMMENT '申报币种',
    `freight_cost` DECIMAL(12,2) NOT NULL COMMENT '出库时运费快照',
    `freight_currency` VARCHAR(8) DEFAULT 'CNY' COMMENT '运费币种',
    `zone` VARCHAR(16) DEFAULT NULL COMMENT '计费区域',
    `promise_eta` DATETIME DEFAULT NULL COMMENT '承诺妥投ETA',
    `actual_delivered_at` DATETIME DEFAULT NULL COMMENT '实际妥投时间',
    `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态:ACTIVE/DELIVERED/CANCELED',
    `billed` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已入账单',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_waybill_no` (`waybill_no`),
    UNIQUE KEY `uk_tracking_no` (`tracking_no`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_channel_id` (`channel_id`),
    KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运单(出库生成,运费快照)';

-- -----------------------------------------------------------
-- 8. 承运商账单（承运商 × 账期）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bill` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `bill_no` VARCHAR(64) NOT NULL COMMENT '账单号',
    `carrier_id` BIGINT NOT NULL COMMENT '承运商ID',
    `period_start` DATE NOT NULL COMMENT '账期开始',
    `period_end` DATE NOT NULL COMMENT '账期结束',
    `currency` VARCHAR(8) DEFAULT 'CNY' COMMENT '币种',
    `total_amount` DECIMAL(12,2) DEFAULT 0 COMMENT '账单总额',
    `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '状态:DRAFT/SUBMITTED/VERIFIED/SETTLED/REJECTED',
    `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注',
    `submitted_by` VARCHAR(64) DEFAULT NULL COMMENT '提交人',
    `submitted_at` DATETIME DEFAULT NULL COMMENT '提交时间',
    `verified_by` VARCHAR(64) DEFAULT NULL COMMENT '核销人',
    `verified_at` DATETIME DEFAULT NULL COMMENT '核销时间',
    `settled_by` VARCHAR(64) DEFAULT NULL COMMENT '结算人',
    `settled_at` DATETIME DEFAULT NULL COMMENT '结算时间',
    `rejected_by` VARCHAR(64) DEFAULT NULL COMMENT '驳回人',
    `rejected_at` DATETIME DEFAULT NULL COMMENT '驳回时间',
    `reject_reason` VARCHAR(256) DEFAULT NULL COMMENT '驳回原因',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bill_no` (`bill_no`),
    UNIQUE KEY `uk_carrier_period` (`carrier_id`, `period_start`, `period_end`),
    KEY `idx_carrier_id` (`carrier_id`),
    KEY `idx_status` (`status`),
    KEY `idx_period` (`period_start`, `period_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='承运商账单(承运人×账期)';

-- -----------------------------------------------------------
-- 9. 操作审计日志（谁在何时对哪个业务对象做了什么，企业合规要求）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `operator` VARCHAR(64) NOT NULL COMMENT '操作人(username/用户名)',
    `operator_role` VARCHAR(32) DEFAULT NULL COMMENT '操作人角色',
    `module` VARCHAR(32) NOT NULL COMMENT '业务模块:order/waybill/workorder/bill/sla/notify',
    `action` VARCHAR(64) NOT NULL COMMENT '动作:CREATE/GENERATE/ADVANCE/ANOMALY/CLAIM/SUBMIT/VERIFY/SETTLE/REJECT/DIAGNOSE/NOTIFY',
    `target_no` VARCHAR(64) DEFAULT NULL COMMENT '业务对象单号(订单/运单/账单号)',
    `detail` VARCHAR(512) DEFAULT NULL COMMENT '操作详情(JSON或描述)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_module` (`module`),
    KEY `idx_target` (`target_no`),
    KEY `idx_operator` (`operator`),
    KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';

-- -----------------------------------------------------------
-- 10. 账单明细（每运单一行）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bill_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `bill_id` BIGINT NOT NULL COMMENT '账单ID',
    `waybill_id` BIGINT NOT NULL COMMENT '运单ID',
    `waybill_no` VARCHAR(64) NOT NULL COMMENT '运单号',
    `order_no` VARCHAR(64) DEFAULT NULL COMMENT '订单号',
    `merchant_id` BIGINT DEFAULT NULL COMMENT '商家ID',
    `tracking_no` VARCHAR(64) DEFAULT NULL COMMENT '跟踪号',
    `weight_kg` DECIMAL(10,3) DEFAULT NULL COMMENT '实重kg',
    `billable_weight_kg` DECIMAL(10,3) DEFAULT NULL COMMENT '计费重kg',
    `freight_cost` DECIMAL(12,2) NOT NULL COMMENT '运费',
    `currency` VARCHAR(8) DEFAULT 'CNY' COMMENT '币种',
    `billed_at` DATETIME DEFAULT NULL COMMENT '入账时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bill_waybill` (`bill_id`, `waybill_id`),
    KEY `idx_bill_id` (`bill_id`),
    KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单明细(每运单一行)';
