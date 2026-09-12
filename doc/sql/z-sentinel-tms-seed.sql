-- ============================================================
-- Sentinel TMS 种子数据（幂等，可安全重复执行）
-- 先执行：sentinel.sql -> sentinel-tms.sql -> z-sentinel-seed.sql -> z-sentinel-tms-seed.sql
-- 幂等策略：显式 ID 用 INSERT IGNORE；唯一键用 ON DUPLICATE KEY UPDATE。
-- ============================================================
USE `austin`;

-- -----------------------------------------------------------
-- 1. 财务角色账号（bcrypt 哈希，明文 Finance@123）
-- -----------------------------------------------------------
INSERT INTO `sentinel_user` (`username`, `password`, `nickname`, `role`, `status`) VALUES
('finance', '$2a$10$6kLzPvqf.Ev8jVD.HpYW8ejTSA3Rp3eECIracYmJH8uK1eORQH46K', '财务', 'FINANCE', 1)
ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`), `role` = 'FINANCE', `status` = 1;

-- -----------------------------------------------------------
-- 2. 商家（卖家）主数据
-- -----------------------------------------------------------
INSERT IGNORE INTO `merchant` (`id`, `merchant_code`, `merchant_name`, `user_id`, `contact_name`, `contact_phone`, `contact_email`, `country`, `status`) VALUES
(1, 'MCH-0001', '深圳蓝鲸科技', (SELECT id FROM `sentinel_user` WHERE `username` = 'merchant' LIMIT 1), '王海', '13800138001', 'wh@lanjing.cn', 'CN', 1),
(2, 'MCH-0002', '义乌百灵贸易', NULL, '李慧', '13800138002', 'lihui@bailing.cn', 'CN', 1),
(3, 'MCH-0003', '广州启航电子', NULL, '陈航', '13800138003', 'chenhang@qihang.cn', 'CN', 1),
(4, 'MCH-0004', '杭州云图科技', NULL, '赵琳', '13800138004', 'zhaolin@yuntu.cn', 'CN', 1)
ON DUPLICATE KEY UPDATE `merchant_name` = VALUES(`merchant_name`), `status` = 1;

-- -----------------------------------------------------------
-- 3. 承运商
-- -----------------------------------------------------------
INSERT IGNORE INTO `carrier` (`id`, `carrier_code`, `carrier_name`, `type`, `country`, `status`) VALUES
(1, 'CARGOWAY', '中欧班列承运有限公司', 'rail', 'CN', 1),
(2, 'AIRGO',   '环球国际空运',           'air',  'CN', 1),
(3, 'SEAGO',   '远洋集运物流',           'sea',  'CN', 1)
ON DUPLICATE KEY UPDATE `carrier_name` = VALUES(`carrier_name`), `status` = 1;

-- -----------------------------------------------------------
-- 4. 物流渠道
-- -----------------------------------------------------------
INSERT IGNORE INTO `carrier_channel`
(`id`, `carrier_id`, `channel_code`, `channel_name`, `type`, `dest_country`, `transit_days_min`, `transit_days_max`, `tracking_prefix`, `min_billable_weight_kg`, `vol_divisor`, `status`) VALUES
(1, 1, 'RU-RAIL-EXPR', '中欧班列快线-俄罗斯', 'rail', 'RU', 12, 25, 'RR', 0.500, 6000, 1),
(2, 2, 'RU-AIR',       '俄罗斯空运专线',     'air',  'RU', 5,  9,  'CY', 0.000, 5000, 1),
(3, 2, 'US-AIR',       '美国空运专线',       'air',  'US', 5,  10, 'YU', 0.000, 5000, 1),
(4, 2, 'BR-AIR',       '巴西空运专线',       'air',  'BR', 8,  15, 'BZ', 0.000, 5000, 1),
(5, 2, 'DE-AIR',       '德国空运专线',       'air',  'DE', 6,  12, 'DG', 0.000, 5000, 1),
(6, 3, 'SEA-RU',       '俄罗斯海运大货',     'sea',  'RU', 28, 45, 'SE', 1.000, 6000, 1)
ON DUPLICATE KEY UPDATE `channel_name` = VALUES(`channel_name`), `status` = 1;

-- -----------------------------------------------------------
-- 5. 运费价卡（渠道 × 区域 × 重量段）
--   铁路/海运 FIRST_CONTINUED（首重/续重）；空运 PER_KG（单价）
-- -----------------------------------------------------------
INSERT IGNORE INTO `carrier_rate`
(`id`, `channel_id`, `zone`, `min_weight_kg`, `max_weight_kg`, `mode`, `first_weight_kg`, `first_price`, `continued_weight_kg`, `continued_price`, `price`, `currency`, `effective_from`, `effective_to`, `status`) VALUES
-- RU-RAIL-EXPR (FIRST_CONTINUED: 首0.5kg/续1kg)
(1,  1, 'RU', 0,    2,    'FIRST_CONTINUED', 0.5, 45.0000, 1, 20.0000, 0, 'CNY', '2026-01-01', NULL, 1),
(2,  1, 'RU', 2,    10,   'FIRST_CONTINUED', 0.5, 45.0000, 1, 18.0000, 0, 'CNY', '2026-01-01', NULL, 1),
(3,  1, 'RU', 10,   30,   'FIRST_CONTINUED', 0.5, 45.0000, 1, 15.0000, 0, 'CNY', '2026-01-01', NULL, 1),
(4,  1, 'RU', 30,   NULL, 'FIRST_CONTINUED', 0.5, 45.0000, 1, 12.0000, 0, 'CNY', '2026-01-01', NULL, 1),
-- RU-AIR (PER_KG)
(5,  2, 'RU', 0,    2,    'PER_KG', 0, 0, 0, 0, 90.0000, 'CNY', '2026-01-01', NULL, 1),
(6,  2, 'RU', 2,    5,    'PER_KG', 0, 0, 0, 0, 85.0000, 'CNY', '2026-01-01', NULL, 1),
(7,  2, 'RU', 5,    10,   'PER_KG', 0, 0, 0, 0, 80.0000, 'CNY', '2026-01-01', NULL, 1),
(8,  2, 'RU', 10,   30,   'PER_KG', 0, 0, 0, 0, 72.0000, 'CNY', '2026-01-01', NULL, 1),
(9,  2, 'RU', 30,   NULL, 'PER_KG', 0, 0, 0, 0, 65.0000, 'CNY', '2026-01-01', NULL, 1),
-- US-AIR (PER_KG)
(10, 3, 'US', 0,    2,    'PER_KG', 0, 0, 0, 0, 95.0000, 'CNY', '2026-01-01', NULL, 1),
(11, 3, 'US', 2,    5,    'PER_KG', 0, 0, 0, 0, 90.0000, 'CNY', '2026-01-01', NULL, 1),
(12, 3, 'US', 5,    10,   'PER_KG', 0, 0, 0, 0, 85.0000, 'CNY', '2026-01-01', NULL, 1),
(13, 3, 'US', 10,   30,   'PER_KG', 0, 0, 0, 0, 78.0000, 'CNY', '2026-01-01', NULL, 1),
(14, 3, 'US', 30,   NULL, 'PER_KG', 0, 0, 0, 0, 70.0000, 'CNY', '2026-01-01', NULL, 1),
-- BR-AIR (PER_KG)
(15, 4, 'BR', 0,    2,    'PER_KG', 0, 0, 0, 0, 100.0000, 'CNY', '2026-01-01', NULL, 1),
(16, 4, 'BR', 2,    5,    'PER_KG', 0, 0, 0, 0, 95.0000, 'CNY', '2026-01-01', NULL, 1),
(17, 4, 'BR', 5,    10,   'PER_KG', 0, 0, 0, 0, 90.0000, 'CNY', '2026-01-01', NULL, 1),
(18, 4, 'BR', 10,   30,   'PER_KG', 0, 0, 0, 0, 82.0000, 'CNY', '2026-01-01', NULL, 1),
(19, 4, 'BR', 30,   NULL, 'PER_KG', 0, 0, 0, 0, 74.0000, 'CNY', '2026-01-01', NULL, 1),
-- DE-AIR (PER_KG)
(20, 5, 'DE', 0,    2,    'PER_KG', 0, 0, 0, 0, 92.0000, 'CNY', '2026-01-01', NULL, 1),
(21, 5, 'DE', 2,    5,    'PER_KG', 0, 0, 0, 0, 88.0000, 'CNY', '2026-01-01', NULL, 1),
(22, 5, 'DE', 5,    10,   'PER_KG', 0, 0, 0, 0, 83.0000, 'CNY', '2026-01-01', NULL, 1),
(23, 5, 'DE', 10,   30,   'PER_KG', 0, 0, 0, 0, 76.0000, 'CNY', '2026-01-01', NULL, 1),
(24, 5, 'DE', 30,   NULL, 'PER_KG', 0, 0, 0, 0, 68.0000, 'CNY', '2026-01-01', NULL, 1),
-- SEA-RU (FIRST_CONTINUED: 首1kg/续1kg)
(25, 6, 'RU', 0,    30,   'FIRST_CONTINUED', 1, 30.0000, 1, 8.0000, 0, 'CNY', '2026-01-01', NULL, 1),
(26, 6, 'RU', 30,   100,  'FIRST_CONTINUED', 1, 30.0000, 1, 6.0000, 0, 'CNY', '2026-01-01', NULL, 1),
(27, 6, 'RU', 100,  NULL, 'FIRST_CONTINUED', 1, 30.0000, 1, 5.0000, 0, 'CNY', '2026-01-01', NULL, 1)
ON DUPLICATE KEY UPDATE `price` = VALUES(`price`), `status` = 1;

-- -----------------------------------------------------------
-- 6. 发货仓库
-- -----------------------------------------------------------
INSERT IGNORE INTO `warehouse` (`id`, `warehouse_code`, `warehouse_name`, `country`, `city`, `address`, `status`) VALUES
(1, 'WH-SZ', '深圳仓', 'CN', '深圳', '宝安区福永街道国际物流园A区', 1),
(2, 'WH-SH', '上海仓', 'CN', '上海', '青浦区华新镇仓储基地B栋', 1)
ON DUPLICATE KEY UPDATE `warehouse_name` = VALUES(`warehouse_name`), `status` = 1;

-- -----------------------------------------------------------
-- 7. 商品 SKU
-- -----------------------------------------------------------
INSERT IGNORE INTO `product`
(`id`, `merchant_id`, `sku`, `name`, `hs_code`, `declared_value`, `currency`, `weight_kg`, `volume_l`, `origin_country`, `status`) VALUES
(1, 1, 'SKU-LJ-001', '智能手表',        '9102.12', 320.00, 'CNY', 0.450, 0.80, 'CN', 1),
(2, 1, 'SKU-LJ-002', '蓝牙耳机',        '8518.30', 180.00, 'CNY', 0.280, 0.50, 'CN', 1),
(3, 1, 'SKU-LJ-003', 'USB数据线3条装',  '8544.42', 45.00,  'CNY', 0.150, 0.30, 'CN', 1),
(4, 2, 'SKU-BL-001', '针织毛衣',        '6110.30', 120.00, 'CNY', 0.600, 2.20, 'CN', 1),
(5, 2, 'SKU-BL-002', '围巾',            '6117.10', 60.00,  'CNY', 0.300, 1.10, 'CN', 1),
(6, 2, 'SKU-BL-003', '收纳箱',          '3924.90', 85.00,  'CNY', 1.200, 6.00, 'CN', 1),
(7, 3, 'SKU-QH-001', 'LED台灯',         '9405.40', 150.00, 'CNY', 0.800, 3.50, 'CN', 1),
(8, 3, 'SKU-QH-002', '无线充电器',      '8504.40', 210.00, 'CNY', 0.350, 1.20, 'CN', 1),
(9, 4, 'SKU-YT-001', '便携吸尘器',      '8508.11', 420.00, 'CNY', 1.800, 8.50, 'CN', 1),
(10, 4, 'SKU-YT-002', '智能门锁',       '8301.70', 680.00, 'CNY', 3.200, 9.00, 'CN', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `status` = 1;

-- -----------------------------------------------------------
-- 8. TMS 履约订单（OMT-TMS-SEED-*，覆盖各状态）
--    运费/申报值来自真实价卡与商品主数据
-- -----------------------------------------------------------
INSERT IGNORE INTO `logistics_order`
(`order_no`, `buyer_id`, `buyer_phone`, `buyer_language`, `merchant_id`, `merchant_name`, `destination_country`,
 `current_node`, `channel_id`, `carrier_id`, `warehouse_id`, `items_json`, `declared_value`, `declared_currency`,
 `freight_cost`, `freight_currency`, `promise_eta`, `sla_status`, `waybill_no`, `buyer_address`, `buyer_city`, `buyer_postal`,
 `created_at`, `updated_at`) VALUES
( 'OMT-TMS-SEED-0001', 'michael.johnson@gmail.com', '+14155550101', 'en', 1, '深圳蓝鲸科技', 'US',
 'DELIVERED', 3, 2, 1, '[{"sku":"SKU-LJ-002","product_id":2,"qty":2,"unit_weight_kg":0.28,"unit_volume_l":0.5,"unit_declared_value":180,"currency":"CNY"}]', 360.00, 'CNY',
 53.20, 'CNY', DATE_SUB(NOW(), INTERVAL 9 DAY), 'NORMAL', 'WB-US-SEED-0001', '789 5th Avenue', 'New York', '10001',
 DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),
( 'OMT-TMS-SEED-0002', 'sarah.williams@gmail.com', '+13125550102', 'en', 1, '深圳蓝鲸科技', 'US',
 'IN_TRANSIT', 3, 2, 1, '[{"sku":"SKU-LJ-001","product_id":1,"qty":2,"unit_weight_kg":0.45,"unit_volume_l":0.8,"unit_declared_value":320,"currency":"CNY"}]', 640.00, 'CNY',
 85.50, 'CNY', DATE_ADD(NOW(), INTERVAL 4 DAY), 'NORMAL', 'WB-US-SEED-0002', '456 Sunset Blvd', 'Los Angeles', '90028',
 DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),
( 'OMT-TMS-SEED-0003', 'd.ivanov@yandex.ru', '+79161234567', 'ru', 1, '深圳蓝鲸科技', 'RU',
 'WAREHOUSE_OUT', 2, 2, 1, '[{"sku":"SKU-LJ-001","product_id":1,"qty":1,"unit_weight_kg":0.45,"unit_volume_l":0.8,"unit_declared_value":320,"currency":"CNY"}]', 320.00, 'CNY',
 40.50, 'CNY', DATE_ADD(NOW(), INTERVAL 8 DAY), 'NORMAL', 'WB-RU-SEED-0003', 'ул. Тверская, д. 12, кв. 34', 'Москва', '125009',
 DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
( 'OMT-TMS-SEED-0004', 'a.smirnova@mail.ru', '+79261024567', 'ru', 2, '义乌百灵贸易', 'RU',
 'IMPORT_CUSTOMS', 1, 1, 2, '[{"sku":"SKU-BL-001","product_id":4,"qty":1,"unit_weight_kg":0.6,"unit_volume_l":2.2,"unit_declared_value":120,"currency":"CNY"}]', 120.00, 'CNY',
 65.00, 'CNY', DATE_ADD(NOW(), INTERVAL 10 DAY), 'NORMAL', 'WB-RR-SEED-0004', 'Невский пр., д. 88, кв. 12', 'Санкт-Петербург', '190000',
 DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
( 'OMT-TMS-SEED-0005', 'o.kuznetsova@gmail.com', '+79250004567', 'ru', 2, '义乌百灵贸易', 'RU',
 'CUSTOMS_DELAY', 1, 1, 2, '[{"sku":"SKU-BL-002","product_id":5,"qty":2,"unit_weight_kg":0.3,"unit_volume_l":1.1,"unit_declared_value":60,"currency":"CNY"}]', 120.00, 'CNY',
 65.00, 'CNY', DATE_SUB(NOW(), INTERVAL 2 DAY), 'BREACHED', 'WB-RR-SEED-0005', 'Ленина пр., д. 54, кв. 7', 'Екатеринбург', '620000',
 DATE_SUB(NOW(), INTERVAL 20 DAY), NOW()),
( 'OMT-TMS-SEED-0006', 'joao.silva@gmail.com', '+5511987654321', 'pt', 3, '广州启航电子', 'BR',
 'LAST_MILE', 4, 2, 2, '[{"sku":"SKU-QH-001","product_id":7,"qty":1,"unit_weight_kg":0.8,"unit_volume_l":3.5,"unit_declared_value":150,"currency":"CNY"}]', 150.00, 'CNY',
 80.00, 'CNY', DATE_ADD(NOW(), INTERVAL 3 DAY), 'NORMAL', 'WB-BR-SEED-0006', 'Rua Oscar Freire 379', 'São Paulo', '01426-001',
 DATE_SUB(NOW(), INTERVAL 11 DAY), NOW()),
( 'OMT-TMS-SEED-0007', 'lukas.weber@gmail.com', '+49301234567', 'de', 3, '广州启航电子', 'DE',
 'DELIVERED', 5, 2, 2, '[{"sku":"SKU-QH-002","product_id":8,"qty":1,"unit_weight_kg":0.35,"unit_volume_l":1.2,"unit_declared_value":210,"currency":"CNY"}]', 210.00, 'CNY',
 32.20, 'CNY', DATE_SUB(NOW(), INTERVAL 3 DAY), 'NORMAL', 'WB-DE-SEED-0007', 'Friedrichstraße 123', 'Berlin', '10117',
 DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),
( 'OMT-TMS-SEED-0008', 'emily.davis@gmail.com', '+13125550104', 'en', 4, '杭州云图科技', 'US',
 'LOST', 3, 2, 1, '[{"sku":"SKU-YT-001","product_id":9,"qty":1,"unit_weight_kg":1.8,"unit_volume_l":8.5,"unit_declared_value":420,"currency":"CNY"}]', 420.00, 'CNY',
 171.00, 'CNY', NULL, 'BREACHED', 'WB-US-SEED-0008', '300 Lakeview Dr', 'Chicago', '60601',
 DATE_SUB(NOW(), INTERVAL 15 DAY), NOW()),
( 'OMT-TMS-SEED-0009', 'e.volkova@yandex.ru', '+79166654321', 'ru', 4, '杭州云图科技', 'RU',
 'IN_TRANSIT', 6, 3, 1, '[{"sku":"SKU-YT-002","product_id":10,"qty":1,"unit_weight_kg":3.2,"unit_volume_l":9,"unit_declared_value":680,"currency":"CNY"}]', 680.00, 'CNY',
 54.00, 'CNY', DATE_ADD(NOW(), INTERVAL 30 DAY), 'NORMAL', 'WB-SE-SEED-0009', 'ул. Светланская, д. 43, кв. 5', 'Владивосток', '690000',
 DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
( 'OMT-TMS-SEED-0010', 's.petrov@yandex.ru', '+79033456789', 'ru', 2, '义乌百灵贸易', 'RU',
 'DELIVERY_FAILED', 1, 1, 2, '[{"sku":"SKU-BL-001","product_id":4,"qty":2,"unit_weight_kg":0.6,"unit_volume_l":2.2,"unit_declared_value":120,"currency":"CNY"}]', 240.00, 'CNY',
 65.00, 'CNY', DATE_SUB(NOW(), INTERVAL 1 DAY), 'BREACHED', 'WB-RR-SEED-0010', 'Красный пр., д. 21', 'Новосибирск', '630000',
 DATE_SUB(NOW(), INTERVAL 12 DAY), NOW())
ON DUPLICATE KEY UPDATE `current_node` = VALUES(`current_node`), `sla_status` = VALUES(`sla_status`);

-- -----------------------------------------------------------
-- 9. TMS 运单（出库生成，运费快照）
-- -----------------------------------------------------------
INSERT IGNORE INTO `waybill`
(`id`, `waybill_no`, `order_no`, `merchant_id`, `channel_id`, `carrier_id`, `tracking_no`, `carrier_code`,
 `weight_kg`, `volume_l`, `billable_weight_kg`, `declared_value`, `declared_currency`, `freight_cost`, `freight_currency`,
 `zone`, `promise_eta`, `status`, `billed`, `created_at`, `updated_at`) VALUES
(1, 'WB-US-SEED-0001', 'OMT-TMS-SEED-0001', 1, 3, 2, 'YU-SEED-0001', 'AIRGO', 0.560, 1.00, 0.560, 360.00, 'CNY', 53.20, 'CNY', 'US', DATE_SUB(NOW(), INTERVAL 9 DAY), 'DELIVERED', 1, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),
(2, 'WB-US-SEED-0002', 'OMT-TMS-SEED-0002', 1, 3, 2, 'YU-SEED-0002', 'AIRGO', 0.900, 1.60, 0.900, 640.00, 'CNY', 85.50, 'CNY', 'US', DATE_ADD(NOW(), INTERVAL 4 DAY), 'ACTIVE', 1, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),
(3, 'WB-RU-SEED-0003', 'OMT-TMS-SEED-0003', 1, 2, 2, 'CY-SEED-0003', 'AIRGO', 0.450, 0.80, 0.450, 320.00, 'CNY', 40.50, 'CNY', 'RU', DATE_ADD(NOW(), INTERVAL 8 DAY), 'ACTIVE', 0, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(4, 'WB-RR-SEED-0004', 'OMT-TMS-SEED-0004', 2, 1, 1, 'RR-SEED-0004', 'CARGOWAY', 0.600, 2.20, 0.600, 120.00, 'CNY', 65.00, 'CNY', 'RU', DATE_ADD(NOW(), INTERVAL 10 DAY), 'ACTIVE', 1, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(5, 'WB-RR-SEED-0005', 'OMT-TMS-SEED-0005', 2, 1, 1, 'RR-SEED-0005', 'CARGOWAY', 0.600, 2.20, 0.600, 120.00, 'CNY', 65.00, 'CNY', 'RU', DATE_SUB(NOW(), INTERVAL 2 DAY), 'ACTIVE', 1, DATE_SUB(NOW(), INTERVAL 20 DAY), NOW()),
(6, 'WB-BR-SEED-0006', 'OMT-TMS-SEED-0006', 3, 4, 2, 'BZ-SEED-0006', 'AIRGO', 0.800, 3.50, 0.800, 150.00, 'CNY', 80.00, 'CNY', 'BR', DATE_ADD(NOW(), INTERVAL 3 DAY), 'ACTIVE', 0, DATE_SUB(NOW(), INTERVAL 11 DAY), NOW()),
(7, 'WB-DE-SEED-0007', 'OMT-TMS-SEED-0007', 3, 5, 2, 'DG-SEED-0007', 'AIRGO', 0.350, 1.20, 0.350, 210.00, 'CNY', 32.20, 'CNY', 'DE', DATE_SUB(NOW(), INTERVAL 3 DAY), 'DELIVERED', 1, DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),
(8, 'WB-US-SEED-0008', 'OMT-TMS-SEED-0008', 4, 3, 2, 'YU-SEED-0008', 'AIRGO', 1.800, 8.50, 1.800, 420.00, 'CNY', 171.00, 'CNY', 'US', DATE_SUB(NOW(), INTERVAL 9 DAY), 'ACTIVE', 1, DATE_SUB(NOW(), INTERVAL 15 DAY), NOW()),
(9, 'WB-SE-SEED-0009', 'OMT-TMS-SEED-0009', 4, 6, 3, 'SE-SEED-0009', 'SEAGO', 3.200, 9.00, 3.200, 680.00, 'CNY', 54.00, 'CNY', 'RU', DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE', 1, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(10, 'WB-RR-SEED-0010', 'OMT-TMS-SEED-0010', 2, 1, 1, 'RR-SEED-0010', 'CARGOWAY', 1.200, 4.40, 1.200, 240.00, 'CNY', 65.00, 'CNY', 'RU', DATE_SUB(NOW(), INTERVAL 1 DAY), 'ACTIVE', 1, DATE_SUB(NOW(), INTERVAL 12 DAY), NOW())
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`), `billed` = VALUES(`billed`);

-- -----------------------------------------------------------
-- 10. TMS 轨迹（种子订单关键节点）
-- -----------------------------------------------------------
INSERT IGNORE INTO `logistics_track` (`order_no`, `node`, `raw_status`, `raw_desc`, `location`, `carrier_code`, `track_time`) VALUES
('OMT-TMS-SEED-0001', 'WAREHOUSE_OUT', 'EXP-0020', 'Package departed warehouse', '深圳仓', 'AIRGO', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 10 DAY)) * 1000),
('OMT-TMS-SEED-0001', 'DELIVERED', 'EXP-0080', 'Delivered, signed by recipient', 'New York', 'AIRGO', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000),
('OMT-TMS-SEED-0002', 'WAREHOUSE_OUT', 'EXP-0020', 'Package departed warehouse', '深圳仓', 'AIRGO', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 4 DAY)) * 1000),
('OMT-TMS-SEED-0002', 'IN_TRANSIT', 'EXP-0050', 'International flight en route', 'Los Angeles', 'AIRGO', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 2 DAY)) * 1000),
('OMT-TMS-SEED-0003', 'WAREHOUSE_OUT', 'EXP-0020', 'Package departed warehouse', '深圳仓', 'AIRGO', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 1 DAY)) * 1000),
('OMT-TMS-SEED-0004', 'IMPORT_CUSTOMS', 'EXP-0060', 'Customs clearance in progress', 'Moscow', 'CARGOWAY', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 1 DAY)) * 1000),
('OMT-TMS-SEED-0005', 'CUSTOMS_DELAY', 'CUS-1102', 'Customs inspection delay', 'Moscow', 'CARGOWAY', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 2 DAY)) * 1000),
('OMT-TMS-SEED-0006', 'LAST_MILE', 'EXP-0070', 'Out for local delivery', 'São Paulo', 'AIRGO', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 1 DAY)) * 1000),
('OMT-TMS-SEED-0007', 'DELIVERED', 'EXP-0080', 'Delivered', 'Berlin', 'AIRGO', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 3 DAY)) * 1000),
('OMT-TMS-SEED-0008', 'LOST', 'EXP-0051', 'Package lost in transit', 'Chicago', 'AIRGO', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 9 DAY)) * 1000),
('OMT-TMS-SEED-0009', 'IN_TRANSIT', 'EXP-0050', 'Sea vessel en route', 'Pacific Ocean', 'SEAGO', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 3 DAY)) * 1000),
('OMT-TMS-SEED-0010', 'DELIVERY_FAILED', 'EXP-0071', 'Recipient not home', 'Novosibirsk', 'CARGOWAY', UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 1 DAY)) * 1000);

-- -----------------------------------------------------------
-- 11. 承运商账单 + 明细（bill_item 运费 = waybill.freight_cost）
-- -----------------------------------------------------------
INSERT IGNORE INTO `bill`
(`id`, `bill_no`, `carrier_id`, `period_start`, `period_end`, `currency`, `total_amount`, `status`, `submitted_by`, `submitted_at`, `verified_by`, `verified_at`, `settled_by`, `settled_at`) VALUES
(1, 'BILL-AIRGO-202608', 2, '2026-08-01', '2026-08-31', 'CNY', 341.90, 'SUBMITTED', 'finance', DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL, NULL, NULL),
(2, 'BILL-CARGOWAY-202608', 1, '2026-08-01', '2026-08-31', 'CNY', 195.00, 'DRAFT', NULL, NULL, NULL, NULL, NULL, NULL),
(3, 'BILL-SEAGO-202607', 3, '2026-07-01', '2026-07-31', 'CNY', 54.00, 'SETTLED', 'finance', DATE_SUB(NOW(), INTERVAL 20 DAY), 'finance', DATE_SUB(NOW(), INTERVAL 18 DAY), 'finance', DATE_SUB(NOW(), INTERVAL 15 DAY))
ON DUPLICATE KEY UPDATE `total_amount` = VALUES(`total_amount`), `status` = VALUES(`status`);

INSERT IGNORE INTO `bill_item`
(`id`, `bill_id`, `waybill_id`, `waybill_no`, `order_no`, `merchant_id`, `tracking_no`, `weight_kg`, `billable_weight_kg`, `freight_cost`, `currency`, `billed_at`) VALUES
(1, 1, 1, 'WB-US-SEED-0001', 'OMT-TMS-SEED-0001', 1, 'YU-SEED-0001', 0.560, 0.560, 53.20, 'CNY', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 1, 2, 'WB-US-SEED-0002', 'OMT-TMS-SEED-0002', 1, 'YU-SEED-0002', 0.900, 0.900, 85.50, 'CNY', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3, 1, 7, 'WB-DE-SEED-0007', 'OMT-TMS-SEED-0007', 3, 'DG-SEED-0007', 0.350, 0.350, 32.20, 'CNY', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4, 1, 8, 'WB-US-SEED-0008', 'OMT-TMS-SEED-0008', 4, 'YU-SEED-0008', 1.800, 1.800, 171.00, 'CNY', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(5, 2, 4, 'WB-RR-SEED-0004', 'OMT-TMS-SEED-0004', 2, 'RR-SEED-0004', 0.600, 0.600, 65.00, 'CNY', NOW()),
(6, 2, 5, 'WB-RR-SEED-0005', 'OMT-TMS-SEED-0005', 2, 'RR-SEED-0005', 0.600, 0.600, 65.00, 'CNY', NOW()),
(7, 2, 10, 'WB-RR-SEED-0010', 'OMT-TMS-SEED-0010', 2, 'RR-SEED-0010', 1.200, 1.200, 65.00, 'CNY', NOW()),
(8, 3, 9, 'WB-SE-SEED-0009', 'OMT-TMS-SEED-0009', 4, 'SE-SEED-0009', 3.200, 3.200, 54.00, 'CNY', DATE_SUB(NOW(), INTERVAL 20 DAY))
ON DUPLICATE KEY UPDATE `freight_cost` = VALUES(`freight_cost`);

-- -----------------------------------------------------------
-- 12. 存量工单补责任方/索赔（原 4 张异常工单）
-- -----------------------------------------------------------
UPDATE `workorder` SET `liability`='carrier', `claim_amount`=0, `compensation_amount`=0, `currency`='CNY', `sla_breach`=1 WHERE `order_no`='OMT-SEED-0007' AND `liability` IS NULL;
UPDATE `workorder` SET `liability`='carrier', `claim_amount`=420.00, `compensation_amount`=420.00, `currency`='CNY', `sla_breach`=1, `resolution`='丢失理赔已赔付', `resolved_at`=NOW() WHERE `order_no`='OMT-SEED-0015' AND `liability` IS NULL;
UPDATE `workorder` SET `liability`='merchant', `claim_amount`=120.00, `compensation_amount`=0, `currency`='CNY', `sla_breach`=0, `resolution`='商家提供错误地址，改派中' WHERE `order_no`='OMT-SEED-0010' AND `liability` IS NULL;
UPDATE `workorder` SET `liability`='platform', `claim_amount`=150.00, `compensation_amount`=50.00, `currency`='CNY', `sla_breach`=1, `resolution`='清关延误平台补偿' WHERE `order_no`='OMT-SEED-0016' AND `liability` IS NULL;

-- 新增 SLA 违约工单（OMT-TMS-SEED-0005 清关滞留超承诺时效）
INSERT IGNORE INTO `workorder` (`order_no`, `type`, `level`, `description`, `status`, `liability`, `claim_amount`, `compensation_amount`, `sla_breach`, `currency`) VALUES
('OMT-TMS-SEED-0005', 'sla_breach', 'P1', 'SLA 违约：清关滞留超过承诺时效', 'OPEN', NULL, 0, 0, 1, 'CNY');
