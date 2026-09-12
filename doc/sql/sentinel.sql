-- ============================================================
-- Sentinel · 物流数智协同平台 - 数据库表
-- 基于 sentinel 数据库扩展，新增物流/通知/工单/Agent 相关表
-- PRD v0.7 第七章数据模型
-- ============================================================

USE `sentinel`;

-- -----------------------------------------------------------
-- 1. 物流订单表 logistics_order
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `logistics_order`;
CREATE TABLE `logistics_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `buyer_id` VARCHAR(64) NOT NULL COMMENT '买家ID',
    `buyer_phone` VARCHAR(32) DEFAULT NULL COMMENT '买家手机号',
    `buyer_language` VARCHAR(10) NOT NULL DEFAULT 'zh' COMMENT '买家语言：ru/en/es/zh',
    `merchant_id` BIGINT DEFAULT NULL COMMENT '商家ID',
    `merchant_name` VARCHAR(64) DEFAULT NULL COMMENT '商家名称',
    `destination_country` VARCHAR(32) DEFAULT NULL COMMENT '目的国',
    `current_node` VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT '当前物流节点（LogisticsNode.codeEn）',
    `channel_id` BIGINT DEFAULT NULL COMMENT '物流渠道ID(carrier_channel.id)',
    `carrier_id` BIGINT DEFAULT NULL COMMENT '承运商ID(carrier.id)',
    `warehouse_id` BIGINT DEFAULT NULL COMMENT '发货仓ID(warehouse.id)',
    `items_json` TEXT COMMENT '商品行JSON:[{sku,product_id,qty,unit_weight_kg,unit_declared_value,currency}]',
    `declared_value` DECIMAL(12,2) DEFAULT 0.00 COMMENT '申报价值',
    `declared_currency` VARCHAR(8) DEFAULT 'CNY' COMMENT '申报币种',
    `freight_cost` DECIMAL(12,2) DEFAULT 0.00 COMMENT '下单时运费报价快照',
    `freight_currency` VARCHAR(8) DEFAULT 'CNY' COMMENT '运费币种',
    `promise_eta` DATETIME DEFAULT NULL COMMENT '承诺妥投ETA',
    `sla_status` VARCHAR(16) DEFAULT 'NA' COMMENT 'SLA状态:NORMAL/RISK/BREACHED/NA',
    `waybill_no` VARCHAR(64) DEFAULT NULL COMMENT '运单号',
    `buyer_address` VARCHAR(256) DEFAULT NULL COMMENT '买家收货地址',
    `buyer_city` VARCHAR(64) DEFAULT NULL COMMENT '买家城市',
    `buyer_postal` VARCHAR(32) DEFAULT NULL COMMENT '买家邮编',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_buyer_id` (`buyer_id`),
    KEY `idx_current_node` (`current_node`),
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_channel_id` (`channel_id`),
    KEY `idx_sla_status` (`sla_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流订单表';

-- -----------------------------------------------------------
-- 2. 物流轨迹表 logistics_track
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `logistics_track`;
CREATE TABLE `logistics_track` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `node` VARCHAR(32) NOT NULL COMMENT '节点（LogisticsNode.codeEn）',
    `raw_status` VARCHAR(64) DEFAULT NULL COMMENT '物流商原始状态码',
    `raw_desc` VARCHAR(256) DEFAULT NULL COMMENT '物流商原始描述',
    `location` VARCHAR(128) DEFAULT NULL COMMENT '位置',
    `carrier_code` VARCHAR(32) DEFAULT NULL COMMENT '承运商编码(预留真实物流商轨迹)',
    `track_time` DATETIME NOT NULL COMMENT '轨迹时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_node` (`node`),
    KEY `idx_carrier_code` (`carrier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流轨迹表';

-- -----------------------------------------------------------
-- 3. 通知记录表 notification_record
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `notification_record`;
CREATE TABLE `notification_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `node` VARCHAR(32) NOT NULL COMMENT '触发节点',
    `role` VARCHAR(16) NOT NULL COMMENT '接收角色：buyer/merchant/customer_service',
    `channel` VARCHAR(16) NOT NULL COMMENT '渠道：push/sms/email/feishu',
    `content` TEXT COMMENT '通知内容',
    `language` VARCHAR(10) DEFAULT 'zh' COMMENT '语言',
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SENT/FAILED',
    `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '链路ID',
    `agent_call_log_id` BIGINT DEFAULT NULL COMMENT '关联 Agent 调用日志',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_trace_id` (`trace_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知记录表';

-- -----------------------------------------------------------
-- 4. 异常工单表 workorder
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `workorder`;
CREATE TABLE `workorder` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `type` VARCHAR(32) NOT NULL COMMENT '异常类型：customs_delay/lost/returned/delivery_failed/sla_breach',
    `level` VARCHAR(8) NOT NULL DEFAULT 'P1' COMMENT '级别：P0/P1/P2',
    `description` TEXT COMMENT '描述',
    `agent_diagnosis` TEXT COMMENT 'Agent 诊断结果（JSON）',
    `sop` TEXT COMMENT '处理 SOP',
    `status` VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN/PROCESSING/RESOLVED/CLOSED/PUSHED',
    `liability` VARCHAR(16) DEFAULT NULL COMMENT '责任方:merchant/carrier/platform',
    `claim_amount` DECIMAL(12,2) DEFAULT 0.00 COMMENT '索赔金额',
    `compensation_amount` DECIMAL(12,2) DEFAULT 0.00 COMMENT '理赔金额',
    `sla_breach` TINYINT DEFAULT 0 COMMENT '是否SLA违约',
    `currency` VARCHAR(8) DEFAULT 'CNY' COMMENT '金额币种',
    `resolution` VARCHAR(512) DEFAULT NULL COMMENT '处理结果',
    `resolved_at` DATETIME DEFAULT NULL COMMENT '解决时间',
    `claim_status`       VARCHAR(16) NOT NULL DEFAULT 'NONE' COMMENT '理赔状态:NONE/SUBMITTED/APPROVED/PAID/REJECTED',
    `claim_submitted_at` DATETIME DEFAULT NULL COMMENT '索赔提交时间',
    `claim_approved_at`  DATETIME DEFAULT NULL COMMENT '理赔审批时间',
    `claim_paid_at`      DATETIME DEFAULT NULL COMMENT '赔付时间',
    `claim_reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '驳回原因',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_status` (`status`),
    KEY `idx_level` (`level`),
    KEY `idx_claim_status` (`claim_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常工单表';

-- -----------------------------------------------------------
-- 5. Agent 调用日志表 agent_call_log
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `agent_call_log`;
CREATE TABLE `agent_call_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `agent_name` VARCHAR(32) NOT NULL COMMENT 'Agent 名称',
    `input` TEXT COMMENT '输入参数（JSON）',
    `output` TEXT COMMENT '输出结果（JSON）',
    `tools_called` VARCHAR(256) DEFAULT NULL COMMENT '调用的 Tool 列表（逗号分隔）',
    `token_usage` INT DEFAULT NULL COMMENT 'Token 消耗',
    `latency_ms` INT DEFAULT NULL COMMENT '耗时（毫秒）',
    `status` VARCHAR(16) NOT NULL DEFAULT 'success' COMMENT '状态：success/failed/timeout/degraded',
    `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '链路ID',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_agent_name` (`agent_name`),
    KEY `idx_trace_id` (`trace_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 调用日志表';

-- -----------------------------------------------------------
-- 6. 异常知识库表 anomaly_knowledge
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `anomaly_knowledge`;
CREATE TABLE `anomaly_knowledge` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `status_code` VARCHAR(64) NOT NULL COMMENT '物流商状态码',
    `type` VARCHAR(32) NOT NULL COMMENT '异常类型',
    `description` TEXT NOT NULL COMMENT '中文描述',
    `avg_duration_hours` INT DEFAULT NULL COMMENT '平均处理时长（小时）',
    `suggestion` TEXT COMMENT '处理建议',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_status_code` (`status_code`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常知识库表';

-- -----------------------------------------------------------
-- 7. 退订表 unsubscribe
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `unsubscribe`;
CREATE TABLE `unsubscribe` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `buyer_id` VARCHAR(64) NOT NULL COMMENT '买家ID',
    `channel` VARCHAR(16) NOT NULL COMMENT '渠道：push/sms/email/feishu',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_buyer_id` (`buyer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退订表';

-- -----------------------------------------------------------
-- 8. 初始化异常知识库数据
-- -----------------------------------------------------------
INSERT INTO `anomaly_knowledge` (`status_code`, `type`, `description`, `avg_duration_hours`, `suggestion`) VALUES
('CUS-1102', 'customs_delay', '海关抽检，包裹在目的国海关停留', 48, '1.联系物流商核实 2.通知买家预计延误时间 3.超过72h建议补发安抚通知'),
('CUS-1105', 'customs_delay', '清关文件缺失，需补充材料', 72, '1.联系商家补充材料 2.通知买家 3.跟进清关进度'),
('EXP-0051', 'lost', '国际运输途中丢件', 0, '1.联系物流商核实 2.创建丢件工单 3.建议退款'),
('EXP-0062', 'returned', '包裹退回发件地', 0, '1.核实退回原因 2.通知买家 3.跟进退款'),
('EXP-0071', 'delivery_failed', '本地派送失败（买家不在家）', 24, '1.联系买家预约派送时间 2.重新安排派送');

-- -----------------------------------------------------------
-- 9. 用户表 sentinel_user（多角色 RBAC，P1）
-- 账号数据见 sentinel-seed.sql（幂等）
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `sentinel_user`;
CREATE TABLE `sentinel_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username` VARCHAR(64) NOT NULL COMMENT '登录名',
    `password` VARCHAR(100) NOT NULL COMMENT 'bcrypt 哈希',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '显示名',
    `role` VARCHAR(24) NOT NULL COMMENT '角色：ADMIN/OPERATOR/CUSTOMER_SERVICE/MERCHANT/FINANCE',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Sentinel 用户表';
