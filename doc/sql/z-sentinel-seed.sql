-- ============================================================
-- Sentinel 种子数据（幂等，可安全重复执行）
-- 用法：mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -proot123_A sentinel < doc/sql/sentinel-seed.sql
-- 注意：不要重跑 sentinel.sql（其 DROP TABLE 会清库）。
-- 幂等策略：显式 ID 用 INSERT IGNORE；唯一键（username/order_no/status_code/id=3 模板）用 ON DUPLICATE KEY UPDATE。
-- ============================================================
USE `sentinel`;

-- -----------------------------------------------------------
-- 1. RBAC 账号（bcrypt 哈希；明文见 README「账号」表）
-- -----------------------------------------------------------
INSERT INTO `sentinel_user` (`username`, `password`, `nickname`, `role`) VALUES
('admin',    '$2b$10$zMgQzt/OgPHd7qZHdqb.LOo8yvm9ePV7nlNlvN1EWGwaZxvA.RkHu', '系统管理员', 'ADMIN'),
('operator', '$2b$10$bEjPnY/h/XVMiaxzMCIoAe2hHyYeRzlkAdHv9FM2neZzN2lXa8Yu2', '运营专员',   'OPERATOR'),
('cs',       '$2b$10$Zs8/cCH2E6BBlmqKEcJ1vOm6Vkx8Kk89ufWSlzcY2U12vPE47V0XS', '客服',       'CUSTOMER_SERVICE'),
('merchant', '$2b$10$EFnvExNqe0eMYHay4Ym1RuS9xwcwhCBSd73OJJARy6bzbPJjJrENC', '商家',       'MERCHANT')
ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`), `role` = VALUES(`role`), `status` = 1;

-- -----------------------------------------------------------
-- 2. 渠道账号（sandbox 标注，无真实凭据）
-- -----------------------------------------------------------
INSERT IGNORE INTO `channel_account` (`id`, `name`, `send_channel`, `account_config`, `creator`) VALUES
(9001, 'sandbox-sms',    30,  '{}', 'sentinel'),
(9002, 'sandbox-push',   20,  '{}', 'sentinel'),
(9003, 'sandbox-email',  40,  '{}', 'sentinel'),
(9004, 'sandbox-feishu', 110, '{}', 'sentinel');

-- -----------------------------------------------------------
-- 3. 消息模板
--    id=3 为通知自动分发模板（sentinel.dispatch.template-id=3，userId 类型 + SMS 渠道）
-- -----------------------------------------------------------
INSERT INTO `message_template` (`id`, `name`, `audit_status`, `msg_status`, `id_type`, `send_channel`, `template_type`, `msg_type`, `msg_content`, `send_account`, `creator`, `updator`, `auditor`, `team`, `proposer`, `created`, `updated`)
VALUES (3, 'Sentinel 物流通知', 20, 30, 10, 30, 10, 10, '{"content":"您的订单 {$orderNo} 物流状态已更新，请及时关注"}', 9001, 'sentinel', 'sentinel', 'sentinel', 'sentinel', 'sentinel', UNIX_TIMESTAMP(), UNIX_TIMESTAMP())
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `audit_status` = VALUES(`audit_status`), `msg_status` = VALUES(`msg_status`), `id_type` = VALUES(`id_type`), `send_channel` = VALUES(`send_channel`), `msg_content` = VALUES(`msg_content`), `send_account` = VALUES(`send_account`);

-- 多语言文案模板（sentinel:{node}:{language}，供 ContentGenAgent「模板优先」命中）
INSERT IGNORE INTO `message_template` (`id`, `name`, `audit_status`, `msg_status`, `id_type`, `send_channel`, `template_type`, `msg_type`, `msg_content`, `send_account`, `creator`, `updator`, `auditor`, `team`, `proposer`, `created`, `updated`) VALUES
(1001, 'sentinel:IMPORT_CUSTOMS:ru', 20, 30, 10, 20, 10, 10, 'Здравствуйте! Ваш заказ проходит таможенное оформление, ожидайте.', 9002, 'sentinel', 'sentinel', 'sentinel', 'sentinel', 'sentinel', UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
(1002, 'sentinel:IMPORT_CUSTOMS:en', 20, 30, 10, 20, 10, 10, 'Hello! Your package is undergoing customs clearance, please wait.', 9002, 'sentinel', 'sentinel', 'sentinel', 'sentinel', 'sentinel', UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
(1003, 'sentinel:IMPORT_CUSTOMS:es', 20, 30, 10, 20, 10, 10, 'Hola! Su paquete está en aduanas, por favor espere.', 9002, 'sentinel', 'sentinel', 'sentinel', 'sentinel', 'sentinel', UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
(1004, 'sentinel:LAST_MILE:ru',      20, 30, 10, 20, 10, 10, 'Здравствуйте! Ваш заказ передан в местную доставку, скоро будет доставлен.', 9002, 'sentinel', 'sentinel', 'sentinel', 'sentinel', 'sentinel', UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
(1005, 'sentinel:LAST_MILE:en',      20, 30, 10, 20, 10, 10, 'Hello! Your package is out for local delivery.', 9002, 'sentinel', 'sentinel', 'sentinel', 'sentinel', 'sentinel', UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
(1006, 'sentinel:LAST_MILE:es',      20, 30, 10, 20, 10, 10, 'Hola! Su paquete está en reparto local.', 9002, 'sentinel', 'sentinel', 'sentinel', 'sentinel', 'sentinel', UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
(1007, 'sentinel:DELIVERED:ru',      20, 30, 10, 20, 10, 10, 'Поздравляем! Ваш заказ доставлен. Спасибо за покупку!', 9002, 'sentinel', 'sentinel', 'sentinel', 'sentinel', 'sentinel', UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
(1008, 'sentinel:DELIVERED:en',      20, 30, 10, 20, 10, 10, 'Great news! Your package has been delivered. Thank you!', 9002, 'sentinel', 'sentinel', 'sentinel', 'sentinel', 'sentinel', UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
(1009, 'sentinel:DELIVERED:es',      20, 30, 10, 20, 10, 10, '¡Buenas noticias! Su paquete ha sido entregado. ¡Gracias!', 9002, 'sentinel', 'sentinel', 'sentinel', 'sentinel', 'sentinel', UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
(1010, 'sentinel:CUSTOMS_DELAY:ru',  20, 30, 10, 20, 10, 10, 'Здравствуйте! Ваш заказ задержан на таможне, мы решаем вопрос.', 9002, 'sentinel', 'sentinel', 'sentinel', 'sentinel', 'sentinel', UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
(1011, 'sentinel:CUSTOMS_DELAY:en',  20, 30, 10, 20, 10, 10, 'Hello! Your package is delayed in customs, we are handling it.', 9002, 'sentinel', 'sentinel', 'sentinel', 'sentinel', 'sentinel', UNIX_TIMESTAMP(), UNIX_TIMESTAMP()),
(1012, 'sentinel:CUSTOMS_DELAY:es',  20, 30, 10, 20, 10, 10, 'Hola! Su paquete está retenido en aduanas, lo estamos gestionando.', 9002, 'sentinel', 'sentinel', 'sentinel', 'sentinel', 'sentinel', UNIX_TIMESTAMP(), UNIX_TIMESTAMP());

-- -----------------------------------------------------------
-- 4. 异常知识库（扩展覆盖全部状态码）
-- -----------------------------------------------------------
INSERT INTO `anomaly_knowledge` (`status_code`, `type`, `description`, `avg_duration_hours`, `suggestion`) VALUES
('EXP-0010', 'normal',      '下单，包裹创建', 0, '无需处理'),
('EXP-0020', 'normal',      '仓库出库', 0, '无需处理'),
('EXP-0030', 'normal',      '国内揽收成功', 0, '无需处理'),
('EXP-0040', 'normal',      '出口报关完成', 0, '无需处理'),
('EXP-0050', 'normal',      '国际运输途中', 0, '无需处理'),
('EXP-0060', 'normal',      '目的国清关中', 0, '无需处理'),
('EXP-0070', 'normal',      '本地派送中', 0, '无需处理'),
('EXP-0080', 'normal',      '已签收', 0, '无需处理'),
('CUS-1102', 'customs_delay', '海关抽检，包裹在目的国海关停留', 48, '1.联系物流商核实 2.通知买家预计延误时间 3.超过72h建议补发安抚通知'),
('CUS-1103', 'customs_delay', '海关查验（X光/开箱），清关放行延迟', 60, '1.提供发票/认证材料 2.通知买家预计延迟 3.持续跟进'),
('CUS-1104', 'customs_delay', '目的国清关政策调整（如认证 EAC），清关周期拉长', 96, '1.确认合规资质 2.评估补发 3.同步买家预期'),
('CUS-1105', 'customs_delay', '清关文件缺失，需补充材料', 72, '1.联系商家补充材料 2.通知买家 3.跟进清关进度'),
('EXP-0051', 'lost',         '国际运输途中丢件', 0, '1.联系物流商核实 2.创建丢件工单 3.建议退款'),
('EXP-0052', 'lost',         '运输途中包裹破损/部分丢失', 24, '1.核实破损情况 2.协商部分退款或补发 3.通知买家'),
('EXP-0062', 'returned',     '包裹退回发件地', 0, '1.核实退回原因 2.通知买家 3.跟进退款'),
('EXP-0063', 'returned',     '收件人拒收，包裹退回', 24, '1.联系买家确认原因 2.确认是否重发 3.跟进退回物流'),
('EXP-0071', 'delivery_failed', '本地派送失败（买家不在家）', 24, '1.联系买家预约派送时间 2.重新安排派送'),
('EXP-0072', 'delivery_failed', '地址错误，无法派送', 24, '1.联系买家核对地址 2.更新派送地址 3.重新派送'),
('EXP-0073', 'delivery_failed', '超时未取件，包裹退回网点', 24, '1.提醒买家取件 2.确认是否改派 3.跟进')
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`), `suggestion` = VALUES(`suggestion`);

-- -----------------------------------------------------------
-- 5. 物流订单（32 单，覆盖各状态；含清关滞留>48h、终态、异常）
-- -----------------------------------------------------------
INSERT IGNORE INTO `logistics_order` (`order_no`, `buyer_id`, `buyer_phone`, `buyer_language`, `merchant_name`, `destination_country`, `current_node`, `created_at`, `updated_at`) VALUES
('OMT-SEED-0001', 'buyer_001', '+79031234567', 'ru', '深圳蓝鲸科技', 'RU', 'DELIVERED',       DATE_SUB(NOW(), INTERVAL 8 DAY),   DATE_SUB(NOW(), INTERVAL 12 HOUR)),
('OMT-SEED-0002', 'buyer_002', '+79031112233', 'ru', '义乌百灵贸易', 'RU', 'DELIVERED',       DATE_SUB(NOW(), INTERVAL 7 DAY),   DATE_SUB(NOW(), INTERVAL 20 HOUR)),
('OMT-SEED-0003', 'buyer_003', '+79267778899', 'ru', '广州启航电子', 'RU', 'DELIVERED',       DATE_SUB(NOW(), INTERVAL 6 DAY),   DATE_SUB(NOW(), INTERVAL 30 HOUR)),
('OMT-SEED-0004', 'buyer_004', '+79160001122', 'ru', '东莞智联制造', 'RU', 'LAST_MILE',       DATE_SUB(NOW(), INTERVAL 4 DAY),   DATE_SUB(NOW(), INTERVAL 6 HOUR)),
('OMT-SEED-0005', 'buyer_005', '+79262223344', 'ru', '杭州云图科技', 'RU', 'IMPORT_CUSTOMS',  DATE_SUB(NOW(), INTERVAL 5 DAY),   DATE_SUB(NOW(), INTERVAL 60 HOUR)),
('OMT-SEED-0006', 'buyer_006', '+79091112233', 'ru', '深圳蓝鲸科技', 'RU', 'IMPORT_CUSTOMS',  DATE_SUB(NOW(), INTERVAL 5 DAY),   DATE_SUB(NOW(), INTERVAL 70 HOUR)),
('OMT-SEED-0007', 'buyer_007', '+79265554433', 'ru', '义乌百灵贸易', 'RU', 'CUSTOMS_DELAY',   DATE_SUB(NOW(), INTERVAL 5 DAY),   DATE_SUB(NOW(), INTERVAL 30 HOUR)),
('OMT-SEED-0008', 'buyer_008', '+79163332211', 'ru', '广州启航电子', 'RU', 'IN_TRANSIT',      DATE_SUB(NOW(), INTERVAL 3 DAY),   DATE_SUB(NOW(), INTERVAL 12 HOUR)),
('OMT-SEED-0009', 'buyer_009', '+79098887766', 'ru', '东莞智联制造', 'RU', 'IN_TRANSIT',      DATE_SUB(NOW(), INTERVAL 3 DAY),   DATE_SUB(NOW(), INTERVAL 8 HOUR)),
('OMT-SEED-0010', 'buyer_010', '+79264445566', 'ru', '杭州云图科技', 'RU', 'DELIVERY_FAILED', DATE_SUB(NOW(), INTERVAL 3 DAY),   DATE_SUB(NOW(), INTERVAL 20 HOUR)),
('OMT-SEED-0011', 'buyer_011', '+79093334455', 'ru', '深圳蓝鲸科技', 'RU', 'EXPORT_CUSTOMS',  DATE_SUB(NOW(), INTERVAL 2 DAY),   DATE_SUB(NOW(), INTERVAL 5 HOUR)),
('OMT-SEED-0012', 'buyer_012', '+79268889900', 'ru', '义乌百灵贸易', 'RU', 'DOMESTIC_PICKED', DATE_SUB(NOW(), INTERVAL 1 DAY),   DATE_SUB(NOW(), INTERVAL 4 HOUR)),
('OMT-SEED-0013', 'buyer_013', '+79160098877', 'ru', '广州启航电子', 'RU', 'WAREHOUSE_OUT',   DATE_SUB(NOW(), INTERVAL 1 DAY),   DATE_SUB(NOW(), INTERVAL 2 HOUR)),
('OMT-SEED-0014', 'buyer_014', '+79097776655', 'ru', '东莞智联制造', 'RU', 'CREATED',         DATE_SUB(NOW(), INTERVAL 6 HOUR),  DATE_SUB(NOW(), INTERVAL 6 HOUR)),
('OMT-SEED-0015', 'buyer_015', '+79265556677', 'ru', '杭州云图科技', 'RU', 'LOST',            DATE_SUB(NOW(), INTERVAL 4 DAY),   DATE_SUB(NOW(), INTERVAL 50 HOUR)),
('OMT-SEED-0016', 'buyer_016', '+79092223344', 'ru', '深圳蓝鲸科技', 'RU', 'RETURNED',        DATE_SUB(NOW(), INTERVAL 4 DAY),   DATE_SUB(NOW(), INTERVAL 40 HOUR)),
('OMT-SEED-0017', 'buyer_us_001', '+12025550123', 'en', '深圳蓝鲸科技', 'US', 'IN_TRANSIT',     DATE_SUB(NOW(), INTERVAL 3 DAY),   DATE_SUB(NOW(), INTERVAL 10 HOUR)),
('OMT-SEED-0018', 'buyer_us_002', '+12135550145', 'en', '广州启航电子', 'US', 'IMPORT_CUSTOMS', DATE_SUB(NOW(), INTERVAL 4 DAY),   DATE_SUB(NOW(), INTERVAL 26 HOUR)),
('OMT-SEED-0019', 'buyer_us_003', '+13105550167', 'en', '东莞智联制造', 'US', 'DELIVERED',      DATE_SUB(NOW(), INTERVAL 8 DAY),   DATE_SUB(NOW(), INTERVAL 40 HOUR)),
('OMT-SEED-0020', 'buyer_us_004', '+14155550189', 'en', '杭州云图科技', 'US', 'LAST_MILE',      DATE_SUB(NOW(), INTERVAL 4 DAY),   DATE_SUB(NOW(), INTERVAL 5 HOUR)),
('OMT-SEED-0021', 'buyer_us_005', '+16505550101', 'en', '义乌百灵贸易', 'US', 'CREATED',        DATE_SUB(NOW(), INTERVAL 5 HOUR),  DATE_SUB(NOW(), INTERVAL 5 HOUR)),
('OMT-SEED-0022', 'buyer_br_001', '+5511955001234', 'es', '深圳蓝鲸科技', 'BR', 'IMPORT_CUSTOMS', DATE_SUB(NOW(), INTERVAL 5 DAY),  DATE_SUB(NOW(), INTERVAL 55 HOUR)),
('OMT-SEED-0023', 'buyer_br_002', '+5511955005678', 'es', '广州启航电子', 'BR', 'IN_TRANSIT',    DATE_SUB(NOW(), INTERVAL 2 DAY),   DATE_SUB(NOW(), INTERVAL 6 HOUR)),
('OMT-SEED-0024', 'buyer_br_003', '+5511955009012', 'es', '东莞智联制造', 'BR', 'DELIVERED',     DATE_SUB(NOW(), INTERVAL 7 DAY),   DATE_SUB(NOW(), INTERVAL 60 HOUR)),
('OMT-SEED-0025', 'buyer_de_001', '+4915112345678', 'en', '深圳蓝鲸科技', 'DE', 'IMPORT_CUSTOMS', DATE_SUB(NOW(), INTERVAL 4 DAY),  DATE_SUB(NOW(), INTERVAL 30 HOUR)),
('OMT-SEED-0026', 'buyer_de_002', '+4915212345678', 'en', '义乌百灵贸易', 'DE', 'IN_TRANSIT',    DATE_SUB(NOW(), INTERVAL 2 DAY),   DATE_SUB(NOW(), INTERVAL 4 HOUR)),
('OMT-SEED-0027', 'buyer_de_003', '+4915312345678', 'en', '广州启航电子', 'DE', 'DELIVERED',     DATE_SUB(NOW(), INTERVAL 6 DAY),   DATE_SUB(NOW(), INTERVAL 80 HOUR)),
('OMT-SEED-0028', 'buyer_017', '+79093336677', 'ru', '深圳蓝鲸科技', 'RU', 'IMPORT_CUSTOMS',  DATE_SUB(NOW(), INTERVAL 6 DAY),   DATE_SUB(NOW(), INTERVAL 65 HOUR)),
('OMT-SEED-0029', 'buyer_018', '+79269998811', 'ru', '杭州云图科技', 'RU', 'DELIVERED',       DATE_SUB(NOW(), INTERVAL 9 DAY),   DATE_SUB(NOW(), INTERVAL 96 HOUR)),
('OMT-SEED-0030', 'buyer_019', '+79164445522', 'ru', '东莞智联制造', 'RU', 'LAST_MILE',       DATE_SUB(NOW(), INTERVAL 5 DAY),   DATE_SUB(NOW(), INTERVAL 3 HOUR)),
('OMT-SEED-0031', 'buyer_020', '+79091119988', 'ru', '义乌百灵贸易', 'RU', 'DOMESTIC_PICKED', DATE_SUB(NOW(), INTERVAL 18 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
('OMT-SEED-0032', 'buyer_021', '+79268887799', 'ru', '广州启航电子', 'RU', 'EXPORT_CUSTOMS',  DATE_SUB(NOW(), INTERVAL 2 DAY),   DATE_SUB(NOW(), INTERVAL 6 HOUR));

-- -----------------------------------------------------------
-- 6. 物流轨迹（已签收订单完整链路 + 部分在途）
-- -----------------------------------------------------------
INSERT IGNORE INTO `logistics_track` (`id`, `order_no`, `node`, `raw_status`, `raw_desc`, `location`, `track_time`) VALUES
(200001, 'OMT-SEED-0001', 'CREATED',        'EXP-0010', 'Shipment created, awaiting pickup',        'Shenzhen Warehouse',           DATE_SUB(NOW(), INTERVAL 190 HOUR)),
(200002, 'OMT-SEED-0001', 'WAREHOUSE_OUT', 'EXP-0020', 'Package departed from warehouse',           'Shenzhen Warehouse',           DATE_SUB(NOW(), INTERVAL 183 HOUR)),
(200003, 'OMT-SEED-0001', 'DOMESTIC_PICKED','EXP-0030', 'Shipment picked up by carrier',            'Shenzhen · Parcel Pickup',     DATE_SUB(NOW(), INTERVAL 176 HOUR)),
(200004, 'OMT-SEED-0001', 'EXPORT_CUSTOMS', 'EXP-0040', 'Export customs cleared, awaiting departure','Khorgos Border Crossing',      DATE_SUB(NOW(), INTERVAL 160 HOUR)),
(200005, 'OMT-SEED-0001', 'IN_TRANSIT',    'EXP-0050', 'In transit to destination country',        'Moscow-bound train',           DATE_SUB(NOW(), INTERVAL 130 HOUR)),
(200006, 'OMT-SEED-0001', 'IMPORT_CUSTOMS','EXP-0060', 'Customs clearance in progress',            'Domodedovo Customs · Moscow',  DATE_SUB(NOW(), INTERVAL 80 HOUR)),
(200007, 'OMT-SEED-0001', 'LAST_MILE',     'EXP-0070', 'Customs cleared, out for delivery',        'Moscow · Local Delivery',      DATE_SUB(NOW(), INTERVAL 30 HOUR)),
(200008, 'OMT-SEED-0001', 'DELIVERED',     'EXP-0080', 'Delivered, signed by recipient',           'Moscow',                       DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(200009, 'OMT-SEED-0002', 'CREATED',        'EXP-0010', 'Shipment created, awaiting pickup',        'Shenzhen Warehouse',           DATE_SUB(NOW(), INTERVAL 168 HOUR)),
(200010, 'OMT-SEED-0002', 'WAREHOUSE_OUT', 'EXP-0020', 'Package departed from warehouse',           'Shenzhen Warehouse',           DATE_SUB(NOW(), INTERVAL 161 HOUR)),
(200011, 'OMT-SEED-0002', 'IN_TRANSIT',    'EXP-0050', 'In transit to destination country',        'Moscow-bound train',           DATE_SUB(NOW(), INTERVAL 120 HOUR)),
(200012, 'OMT-SEED-0002', 'IMPORT_CUSTOMS','EXP-0060', 'Customs clearance in progress',            'Domodedovo Customs · Moscow',  DATE_SUB(NOW(), INTERVAL 60 HOUR)),
(200013, 'OMT-SEED-0002', 'DELIVERED',     'EXP-0080', 'Delivered, signed by recipient',           'Moscow',                       DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(200014, 'OMT-SEED-0019', 'CREATED',        'EXP-0010', 'Shipment created, awaiting pickup',        'Shenzhen Warehouse',           DATE_SUB(NOW(), INTERVAL 190 HOUR)),
(200015, 'OMT-SEED-0019', 'EXPORT_CUSTOMS','EXP-0040', 'Export customs cleared, awaiting departure','Hong Kong Intl Airport',       DATE_SUB(NOW(), INTERVAL 150 HOUR)),
(200016, 'OMT-SEED-0019', 'IMPORT_CUSTOMS','EXP-0060', 'Customs clearance in progress',            'Los Angeles Intl · Customs',   DATE_SUB(NOW(), INTERVAL 90 HOUR)),
(200017, 'OMT-SEED-0019', 'DELIVERED',     'EXP-0080', 'Delivered, signed by recipient',           'Los Angeles',                  DATE_SUB(NOW(), INTERVAL 40 HOUR)),
(200018, 'OMT-SEED-0004', 'CREATED',        'EXP-0010', 'Shipment created, awaiting pickup',        'Shenzhen Warehouse',           DATE_SUB(NOW(), INTERVAL 96 HOUR)),
(200019, 'OMT-SEED-0004', 'IMPORT_CUSTOMS','EXP-0060', 'Customs clearance in progress',            'Domodedovo Customs · Moscow',  DATE_SUB(NOW(), INTERVAL 30 HOUR)),
(200020, 'OMT-SEED-0004', 'LAST_MILE',     'EXP-0070', 'Customs cleared, out for delivery',        'Moscow · Local Delivery',      DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(200021, 'OMT-SEED-0005', 'CREATED',        'EXP-0010', 'Shipment created, awaiting pickup',        'Shenzhen Warehouse',           DATE_SUB(NOW(), INTERVAL 120 HOUR)),
(200022, 'OMT-SEED-0005', 'IMPORT_CUSTOMS','EXP-0060', 'Customs clearance in progress',            'Domodedovo Customs · Moscow',  DATE_SUB(NOW(), INTERVAL 60 HOUR)),
(200023, 'OMT-SEED-0008', 'CREATED',        'EXP-0010', 'Shipment created, awaiting pickup',        'Shenzhen Warehouse',           DATE_SUB(NOW(), INTERVAL 72 HOUR)),
(200024, 'OMT-SEED-0008', 'IN_TRANSIT',    'EXP-0050', 'In transit to destination country',        'Moscow-bound train',           DATE_SUB(NOW(), INTERVAL 12 HOUR));

-- -----------------------------------------------------------
-- 7. 通知记录（近 7 天，驱动看板渠道分布）
-- -----------------------------------------------------------
INSERT IGNORE INTO `notification_record` (`id`, `order_no`, `node`, `role`, `channel`, `content`, `language`, `status`, `trace_id`) VALUES
(300001, 'OMT-SEED-0001', 'IMPORT_CUSTOMS', 'buyer', 'sms',  'Здравствуйте! Ваш заказ проходит таможенное оформление.', 'ru', 'SENT', 'SEED-0001'),
(300002, 'OMT-SEED-0001', 'DELIVERED',      'buyer', 'push', 'Поздравляем! Ваш заказ доставлен.',                        'ru', 'SENT', 'SEED-0001'),
(300003, 'OMT-SEED-0002', 'IMPORT_CUSTOMS', 'buyer', 'sms',  'Здравствуйте! Ваш заказ проходит таможенное оформление.', 'ru', 'SENT', 'SEED-0002'),
(300004, 'OMT-SEED-0019', 'IMPORT_CUSTOMS', 'buyer', 'push', 'Hello! Your package is undergoing customs clearance.',    'en', 'SENT', 'SEED-0019'),
(300005, 'OMT-SEED-0019', 'DELIVERED',      'buyer', 'email','Great news! Your package has been delivered.',             'en', 'SENT', 'SEED-0019'),
(300006, 'OMT-SEED-0004', 'LAST_MILE',      'buyer', 'push', 'Здравствуйте! Ваш заказ передан в местную доставку.',      'ru', 'SENT', 'SEED-0004'),
(300007, 'OMT-SEED-0005', 'IMPORT_CUSTOMS', 'buyer', 'sms',  'Здравствуйте! Ваш заказ проходит таможенное оформление.', 'ru', 'PENDING', 'SEED-0005'),
(300008, 'OMT-SEED-0007', 'CUSTOMS_DELAY',  'buyer', 'sms',  'Здравствуйте! Ваш заказ задержан на таможне.',            'ru', 'SENT', 'SEED-0007'),
(300009, 'OMT-SEED-0007', 'CUSTOMS_DELAY',  'merchant', 'feishu', '订单 OMT-SEED-0007 清关延误，请关注。',           'zh', 'SENT', 'SEED-0007'),
(300010, 'OMT-SEED-0010', 'DELIVERY_FAILED','buyer', 'push', 'Здравствуйте! Доставка не удалась, будет повторная.',      'ru', 'SENT', 'SEED-0010'),
(300011, 'OMT-SEED-0015', 'LOST',           'buyer', 'sms',  'Здравствуйте! Ваш заказ утерян, мы решаем вопрос.',       'ru', 'SENT', 'SEED-0015'),
(300012, 'OMT-SEED-0016', 'RETURNED',       'merchant', 'feishu', '订单 OMT-SEED-0016 已退回，请跟进退款。',       'zh', 'SENT', 'SEED-0016'),
(300013, 'OMT-SEED-0022', 'IMPORT_CUSTOMS', 'buyer', 'sms',  'Hola! Su paquete está en aduanas, por favor espere.',     'es', 'SENT', 'SEED-0022'),
(300014, 'OMT-SEED-0025', 'IMPORT_CUSTOMS', 'buyer', 'push', 'Hello! Your package is undergoing customs clearance.',    'en', 'SENT', 'SEED-0025'),
(300015, 'OMT-SEED-0028', 'IMPORT_CUSTOMS', 'buyer', 'sms',  'Здравствуйте! Ваш заказ проходит таможенное оформление.', 'ru', 'SENT', 'SEED-0028'),
(300016, 'OMT-SEED-0003', 'DELIVERED',      'buyer', 'push', 'Поздравляем! Ваш заказ доставлен.',                        'ru', 'SENT', 'SEED-0003');

-- -----------------------------------------------------------
-- 8. 异常工单（异常订单对应）
-- -----------------------------------------------------------
INSERT IGNORE INTO `workorder` (`id`, `order_no`, `type`, `level`, `description`, `agent_diagnosis`, `sop`, `status`) VALUES
(400001, 'OMT-SEED-0007', 'customs_delay',    'P1', '订单在物流环节发生清关延误异常（节点 CUSTOMS_DELAY）',
        '{"type":"customs_delay","level":"P1","sop":"1.联系物流商核实 2.通知买家预计延误时间","reason":"海关抽检，包裹在目的国海关停留"}',
        '1.联系物流商核实 2.通知买家预计延误时间 3.超过72h建议补发安抚通知', 'OPEN'),
(400002, 'OMT-SEED-0010', 'delivery_failed',  'P2', '订单在物流环节发生派送失败异常（节点 DELIVERY_FAILED）',
        '{"type":"delivery_failed","level":"P2","sop":"1.联系买家预约派送时间 2.重新安排派送","reason":"买家不在家，派送失败"}',
        '1.联系买家预约派送时间 2.重新安排派送', 'PROCESSING'),
(400003, 'OMT-SEED-0015', 'lost',             'P0', '订单在物流环节发生丢件异常（节点 LOST）',
        '{"type":"lost","level":"P0","sop":"1.联系物流商核实 2.创建丢件工单 3.建议退款","reason":"国际运输途中丢件"}',
        '1.联系物流商核实 2.创建丢件工单 3.建议退款', 'OPEN'),
(400004, 'OMT-SEED-0016', 'returned',         'P1', '订单在物流环节发生退回异常（节点 RETURNED）',
        '{"type":"returned","level":"P1","sop":"1.核实退回原因 2.通知买家 3.跟进退款","reason":"包裹退回发件地"}',
        '1.核实退回原因 2.通知买家 3.跟进退款', 'CLOSED');

-- -----------------------------------------------------------
-- 9. Agent 调用日志（近 7 天，驱动看板 Agent 趋势）
-- -----------------------------------------------------------
INSERT IGNORE INTO `agent_call_log` (`id`, `agent_name`, `input`, `output`, `tools_called`, `token_usage`, `latency_ms`, `status`, `trace_id`, `created_at`) VALUES
(500001, 'ContentGenAgent', '{"node":"IMPORT_CUSTOMS","language":"ru"}', '{"content":"Здравствуйте! Ваш заказ проходит таможенное оформление."}', 'queryTemplate', 320, 1200, 'success', 'SEED-0001', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(500002, 'ContentGenAgent', '{"node":"DELIVERED","language":"ru"}', '{"content":"Поздравляем! Ваш заказ доставлен."}', 'queryTemplate', 280, 900, 'success', 'SEED-0001', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(500003, 'ContentGenAgent', '{"node":"IMPORT_CUSTOMS","language":"en"}', '{"content":"Hello! Your package is undergoing customs clearance."}', 'queryTemplate', 300, 1100, 'success', 'SEED-0019', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(500004, 'AnomalyDiagnoseAgent', '{"type":"customs_delay","statusCode":"CUS-1102"}', '{"reason":"海关抽检，包裹在目的国海关停留","priority":"P1"}', 'querySimilarAnomaly,decodeStatusCode', 680, 3800, 'success', 'SEED-0007', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(500005, 'WorkorderAgent', '{"anomalyDesc":"订单在物流环节发生清关延误异常"}', '{"type":"customs_delay","level":"P1","sop":"1.联系物流商核实..."}', 'classifyAnomaly,querySOP', 720, 4100, 'success', 'SEED-0007', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(500006, 'ContentGenAgent', '{"node":"CUSTOMS_DELAY","language":"ru"}', '{"content":"Здравствуйте! Ваш заказ задержан на таможне."}', 'queryTemplate', 310, 1000, 'success', 'SEED-0007', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(500007, 'ChannelRouteAgent', '{"priority":"P1","buyerId":"buyer_007"}', '{"primary":"sms","backup":"push","reason":"SMS 配额充足"}', 'queryChannelHealth,queryUserPreference', 540, 2900, 'success', 'SEED-0007', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(500008, 'AnomalyDiagnoseAgent', '{"type":"lost","statusCode":"EXP-0051"}', '{"reason":"国际运输途中丢件","priority":"P0"}', 'querySimilarAnomaly', 700, 3600, 'success', 'SEED-0015', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(500009, 'CsRouteAgent', '{"message":"我的包裹到哪了？","buyerId":"buyer_005"}', '{"intent":"query_track","route":"auto","reply":"请提供您的订单号..."}', 'queryTrack', 430, 2400, 'success', 'SEED-CS-1', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(500010, 'ContentGenAgent', '{"node":"IMPORT_CUSTOMS","language":"es"}', '{"content":"Hola! Su paquete está en aduanas."}', 'queryTemplate', 290, 950, 'success', 'SEED-0022', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(500011, 'UnsubscribePredictAgent', '{"buyerId":"buyer_010","node":"DELIVERY_FAILED"}', '{"probability":0.31,"advice":"reduce"}', 'queryUserNotifyHistory', 510, 2700, 'success', 'SEED-0010', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(500012, 'ContentGenAgent', '{"node":"DELIVERY_FAILED","language":"ru"}', '{"content":"Здравствуйте! Доставка не удалась."}', 'queryTemplate', 300, 1050, 'degraded', 'SEED-0010', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(500013, 'AnomalyDiagnoseAgent', '{"type":"delivery_failed","statusCode":"EXP-0071"}', '{"reason":"买家不在家","priority":"P2"}', 'querySimilarAnomaly', 660, 3400, 'success', 'SEED-0010', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(500014, 'CsRouteAgent', '{"message":"包裹一直没到，我要投诉","buyerId":"buyer_010"}', '{"intent":"complaint","route":"human","reply":"已为您转接人工客服"}', 'transferHuman', 390, 2100, 'success', 'SEED-CS-2', DATE_SUB(NOW(), INTERVAL 6 HOUR));
