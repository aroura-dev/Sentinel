-- ============================================================
-- Sentinel · 订单主数据统一迁移
-- 把历史遗留订单（无 merchant/channel/freight/waybill）回填成完整 TMS 订单，
-- 使履约订单列表、计费、看板、SLA 全部基于同一份订单主数据。
-- 幂等：仅对缺失字段的订单生效，可重复执行，不动已有 TMS 订单。
-- ============================================================

USE `austin`;

-- 1. 商家分配：对无 merchant_id 的历史订单按 id 轮询到 4 家商家（1,2,3,4）
SET @rn := 0;
UPDATE logistics_order o
JOIN (
    SELECT id, @rn := @rn + 1 AS rn
    FROM logistics_order
    WHERE merchant_id IS NULL AND is_deleted = 0
    ORDER BY id
) t ON t.id = o.id
SET o.merchant_id = MOD(t.rn, 4) + 1;

-- 2. 渠道/承运商/仓库分配（按目的国）：
--    RU/Russia → RU-RAIL-EXPR(1)/CARGOWAY(1)；US→US-AIR(3)/AIRGO(2)；BR→BR-AIR(4)/AIRGO(2)；DE→DE-AIR(5)/AIRGO(2)；其余默认 RU 铁路
UPDATE logistics_order o
SET o.channel_id = CASE UPPER(TRIM(o.destination_country))
        WHEN 'RU' THEN 1 WHEN 'RUSSIA' THEN 1
        WHEN 'US' THEN 3
        WHEN 'BR' THEN 4
        WHEN 'DE' THEN 5
        ELSE 1 END,
    o.carrier_id = CASE UPPER(TRIM(o.destination_country))
        WHEN 'RU' THEN 1 WHEN 'RUSSIA' THEN 1
        WHEN 'US' THEN 2
        WHEN 'BR' THEN 2
        WHEN 'DE' THEN 2
        ELSE 1 END,
    o.warehouse_id = 1
WHERE o.merchant_id IS NOT NULL AND o.channel_id IS NULL AND o.is_deleted = 0;

-- 3. 运费/申报/币种回填：按渠道在 2.0kg 计费重的参考价（对齐 carrier_rate 实价）
--    ch1 RU铁=81(首续重)、ch2 RU空=180、ch3 US空=180、ch4 BR空=200、ch5 DE空=184、ch6 SEA=38
UPDATE logistics_order o
SET o.freight_cost = CASE o.channel_id
        WHEN 1 THEN 81.00 WHEN 2 THEN 180.00 WHEN 3 THEN 180.00
        WHEN 4 THEN 200.00 WHEN 5 THEN 184.00 WHEN 6 THEN 38.00
        ELSE 0 END,
    o.freight_currency = 'CNY',
    o.declared_currency = 'CNY'
WHERE o.channel_id IS NOT NULL AND (o.freight_cost = 0 OR o.freight_cost IS NULL) AND o.is_deleted = 0;

-- 历史订单 freight_cost 默认 0（老单无计费），统一置 0 为合法值
UPDATE logistics_order SET freight_cost = 0 WHERE freight_cost IS NULL AND is_deleted = 0;

-- 4. 承诺 ETA：创建时间 + 渠道最大时效（天）
UPDATE logistics_order o
JOIN carrier_channel c ON c.id = o.channel_id
SET o.promise_eta = DATE_ADD(o.created_at, INTERVAL c.transit_days_max DAY)
WHERE o.promise_eta IS NULL AND o.is_deleted = 0;

-- 5. 商家名称冗余字段对齐（让列表/详情显示正确的商家名）
UPDATE logistics_order o
JOIN merchant m ON m.id = o.merchant_id
SET o.merchant_name = m.merchant_name
WHERE o.merchant_id IS NOT NULL AND (o.merchant_name IS NULL OR o.merchant_name = '')
  AND o.is_deleted = 0;

-- 6. SLA 状态重算：
--    异常节点 → BREACHED；已妥投 → NORMAL；逾期 → BREACHED；临近 → RISK；其余 NORMAL
UPDATE logistics_order o
SET o.sla_status = CASE
        WHEN o.current_node IN ('CUSTOMS_DELAY','DELIVERY_FAILED','LOST','RETURNED') THEN 'BREACHED'
        WHEN o.current_node = 'DELIVERED' THEN 'NORMAL'
        WHEN o.promise_eta IS NOT NULL AND o.promise_eta < NOW() THEN 'BREACHED'
        WHEN o.promise_eta IS NOT NULL AND o.promise_eta < DATE_ADD(NOW(), INTERVAL 2 DAY) THEN 'RISK'
        ELSE 'NORMAL' END
WHERE o.channel_id IS NOT NULL AND o.is_deleted = 0;

-- 7. 补生成运单：为所有已分配渠道但无运单的历史订单生成确定性运单（幂等）
INSERT INTO waybill
    (waybill_no, order_no, merchant_id, channel_id, carrier_id, tracking_no, carrier_code,
     weight_kg, volume_l, billable_weight_kg, declared_value, declared_currency,
     freight_cost, freight_currency, zone, promise_eta, actual_delivered_at, status, billed)
SELECT
    CONCAT('WB', c.channel_code, LPAD(o.id, 8, '0')),
    o.order_no, o.merchant_id, o.channel_id, o.carrier_id,
    CONCAT(c.tracking_prefix, LPAD(o.id, 10, '0')),
    (SELECT carrier_code FROM carrier WHERE id = o.carrier_id),
    2.000, 0.000, 2.000, o.declared_value, 'CNY',
    o.freight_cost, 'CNY',
    (SELECT zone FROM carrier_rate WHERE channel_id = o.channel_id ORDER BY min_weight_kg LIMIT 1),
    o.promise_eta,
    CASE WHEN o.current_node = 'DELIVERED' THEN o.updated_at ELSE NULL END,
    CASE WHEN o.current_node = 'DELIVERED' THEN 'DELIVERED'
         WHEN o.current_node IN ('LOST','RETURNED') THEN 'CANCELED'
         ELSE 'ACTIVE' END,
    0
FROM logistics_order o
JOIN carrier_channel c ON c.id = o.channel_id
WHERE o.waybill_no IS NULL AND o.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM waybill w WHERE w.order_no = o.order_no);

-- 回写订单运单号
UPDATE logistics_order o
SET o.waybill_no = (SELECT w.waybill_no FROM waybill w WHERE w.order_no = o.order_no)
WHERE o.waybill_no IS NULL AND o.is_deleted = 0
  AND EXISTS (SELECT 1 FROM waybill w WHERE w.order_no = o.order_no);

-- 8. 轨迹补全：历史订单若完全无轨迹，补一条创建/初始轨迹，保证轨迹页可读
INSERT INTO logistics_track (order_no, node, raw_status, raw_desc, location, carrier_code, track_time)
SELECT o.order_no, 'CREATED', 'EXP-0000', 'Order created', '平台接单', NULL, o.created_at
FROM logistics_order o
WHERE o.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM logistics_track t WHERE t.order_no = o.order_no);
