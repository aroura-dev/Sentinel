-- ============================================================
-- Sentinel 售后模块体验数据 v2（可重复执行）
--   原则：
--     1. 售后退款(after_sale) 与 理赔流程(workorder.claim_*) 各用不同订单，绝不跨页重复；
--     2. 每条原因/金额/责任方各不相同，贴近真实售后；
--     3. 重跑会先清空上一版演示（不影响真实注册数据之外的其它业务表）。
--   前提：先执行 zz-sentinel-after-sale.sql（workorder 理赔列）
-- ============================================================

USE `sentinel`;

-- 清空上一版演示：重置工单理赔标记（工单本身保留），清掉售后单避免同单重复
DELETE FROM after_sale WHERE is_deleted = 0;
UPDATE workorder
SET claim_status = 'NONE', claim_submitted_at = NULL, claim_approved_at = NULL,
    claim_paid_at = NULL, claim_reject_reason = NULL
WHERE is_deleted = 0;

-- ---------- 一、售后退款 after_sale（6 单，每单不同原因/状态） ----------

-- 1) 受理中 PENDING：内件破损，申请退货退款
INSERT INTO after_sale (order_no, merchant_id, type, reason, refund_amount, currency, status, buyer_name, buyer_address)
SELECT o.order_no, o.merchant_id, 'RETURN', '外包装完好但内件破损，申请退货退款',
       o.declared_value, o.declared_currency, 'PENDING', COALESCE(NULLIF(TRIM(o.buyer_phone), ''), o.buyer_id), o.buyer_address
FROM logistics_order o
WHERE o.order_no IN ('OMT-TMS-SEED-0001')
  AND NOT EXISTS (SELECT 1 FROM after_sale a WHERE a.order_no = o.order_no)
LIMIT 1;

-- 2) 受理中 PENDING：拍错型号，无理由退货
INSERT INTO after_sale (order_no, merchant_id, type, reason, refund_amount, currency, status, buyer_name, buyer_address)
SELECT o.order_no, o.merchant_id, 'RETURN', '型号拍错了，7 天无理由退货',
       o.declared_value, o.declared_currency, 'PENDING', COALESCE(NULLIF(TRIM(o.buyer_phone), ''), o.buyer_id), o.buyer_address
FROM logistics_order o
WHERE o.order_no IN ('OMT-TMS-SEED-0007')
  AND NOT EXISTS (SELECT 1 FROM after_sale a WHERE a.order_no = o.order_no)
LIMIT 1;

-- 3) 已退款 REFUNDED：到货损坏，已审核退款到账
INSERT INTO after_sale (order_no, merchant_id, type, reason, refund_amount, currency, status, buyer_name, buyer_address)
SELECT o.order_no, o.merchant_id, 'RETURN', '到货开箱损坏无法使用，经核实已退款',
       o.declared_value, o.declared_currency, 'REFUNDED', COALESCE(NULLIF(TRIM(o.buyer_phone), ''), o.buyer_id), o.buyer_address
FROM logistics_order o
WHERE o.order_no IN ('OMT-TMS-1788003105876')
  AND NOT EXISTS (SELECT 1 FROM after_sale a WHERE a.order_no = o.order_no)
LIMIT 1;

-- 4) 已退款 REFUNDED：派送失败买家拒收，退回退款
INSERT INTO after_sale (order_no, merchant_id, type, reason, refund_amount, currency, status, buyer_name, buyer_address)
SELECT o.order_no, o.merchant_id, 'RETURN', '派送两次买家均未签收，拒收退回并退款',
       o.declared_value, o.declared_currency, 'REFUNDED', COALESCE(NULLIF(TRIM(o.buyer_phone), ''), o.buyer_id), o.buyer_address
FROM logistics_order o
WHERE o.order_no IN ('OMT-TMS-SEED-0010')
  AND NOT EXISTS (SELECT 1 FROM after_sale a WHERE a.order_no = o.order_no)
LIMIT 1;

-- 5) 已重发 RESHIPPED：商家错发，换货重出（换货不退钱）
INSERT INTO after_sale (order_no, merchant_id, type, reason, refund_amount, currency, status, buyer_name, buyer_address)
SELECT o.order_no, o.merchant_id, 'EXCHANGE', '商家错发货品，已按正确规格换货重发',
       0, o.declared_currency, 'RESHIPPED', COALESCE(NULLIF(TRIM(o.buyer_phone), ''), o.buyer_id), o.buyer_address
FROM logistics_order o
WHERE o.order_no IN ('OMT-JP-0001')
  AND NOT EXISTS (SELECT 1 FROM after_sale a WHERE a.order_no = o.order_no)
LIMIT 1;

-- 6) 已关闭 CLOSED：买家沟通后撤销
INSERT INTO after_sale (order_no, merchant_id, type, reason, refund_amount, currency, status, buyer_name, buyer_address)
SELECT o.order_no, o.merchant_id, 'RETURN', '买家电话沟通后决定保留商品，撤销退货',
       o.declared_value, o.declared_currency, 'CLOSED', COALESCE(NULLIF(TRIM(o.buyer_phone), ''), o.buyer_id), o.buyer_address
FROM logistics_order o
WHERE o.order_no IN ('OMT-SG-0001')
  AND NOT EXISTS (SELECT 1 FROM after_sale a WHERE a.order_no = o.order_no)
LIMIT 1;

-- ---------- 二、理赔流程 workorder.claim_*（6 单，全部与上方退款订单不同） ----------

-- 1) 待审批：整件丢失，按货值全额索赔
INSERT INTO workorder (order_no, type, level, description, status, liability,
                       claim_amount, compensation_amount, currency, claim_status, claim_submitted_at)
SELECT o.order_no, 'lost', 'P0', '整件包裹在干线运输中丢失，买家要求按货值全额索赔', 'PROCESSING', 'carrier',
       o.declared_value, 0, o.declared_currency, 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM logistics_order o
WHERE o.order_no IN ('OMT-TMS-SEED-0002')
  AND NOT EXISTS (SELECT 1 FROM workorder w WHERE w.order_no = o.order_no AND w.is_deleted = 0 AND w.claim_status IS NOT NULL AND w.claim_status <> 'NONE')
LIMIT 1;

-- 2) 待审批：丢件但物流记录中断，先定责
INSERT INTO workorder (order_no, type, level, description, status, liability,
                       claim_amount, compensation_amount, currency, claim_status, claim_submitted_at)
SELECT o.order_no, 'lost', 'P1', '运单在转运后物流记录中断，疑似丢失，进入责任认定', 'PROCESSING', 'carrier',
       o.declared_value, 0, o.declared_currency, 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 6 HOUR)
FROM logistics_order o
WHERE o.order_no IN ('OMT-TMS-SEED-0008')
  AND NOT EXISTS (SELECT 1 FROM workorder w WHERE w.order_no = o.order_no AND w.is_deleted = 0 AND w.claim_status IS NOT NULL AND w.claim_status <> 'NONE')
LIMIT 1;

-- 3) 待赔付：丢件已定责，按货值核定待付款
INSERT INTO workorder (order_no, type, level, description, status, liability,
                       claim_amount, compensation_amount, currency, resolution,
                       claim_status, claim_submitted_at, claim_approved_at)
SELECT o.order_no, 'lost', 'P0', '确认丢件，承运商责任，按货值全额核定赔付', 'RESOLVED', 'carrier',
       o.declared_value, o.declared_value, o.declared_currency, '丢件已定责承运商，同意全额赔付',
       'APPROVED', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM logistics_order o
WHERE o.order_no IN ('OMT-TMS-1787973356126')
  AND NOT EXISTS (SELECT 1 FROM workorder w WHERE w.order_no = o.order_no AND w.is_deleted = 0 AND w.claim_status IS NOT NULL AND w.claim_status <> 'NONE')
LIMIT 1;

-- 4) 待赔付：中转延误超承诺，核定赔付货值六成
INSERT INTO workorder (order_no, type, level, description, status, liability,
                       claim_amount, compensation_amount, currency, resolution,
                       claim_status, claim_submitted_at, claim_approved_at)
SELECT o.order_no, 'customs_delay', 'P1', '中转延误超承诺 3 天，买家索赔时效损失', 'RESOLVED', 'carrier',
       GREATEST(o.declared_value, 200), ROUND(GREATEST(o.declared_value, 200) * 0.6, 2),
       o.declared_currency, '承运时效延误，核定赔付货值 60%',
       'APPROVED', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM logistics_order o
WHERE o.order_no IN ('OMT-TMS-SEED-0004')
  AND NOT EXISTS (SELECT 1 FROM workorder w WHERE w.order_no = o.order_no AND w.is_deleted = 0 AND w.claim_status IS NOT NULL AND w.claim_status <> 'NONE')
LIMIT 1;

-- 5) 已赔付：历史结案（丢件全额赔付到账）
INSERT INTO workorder (order_no, type, level, description, status, liability,
                       claim_amount, compensation_amount, currency, resolution,
                       claim_status, claim_submitted_at, claim_approved_at, claim_paid_at)
SELECT o.order_no, 'lost', 'P1', '此前一票丢失理赔已结案，全额赔付买家到账', 'RESOLVED', 'carrier',
       o.declared_value, o.declared_value, o.declared_currency, '已按货值全额赔付',
       'PAID', DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)
FROM logistics_order o
WHERE o.order_no IN ('OMT-CA-0001')
  AND NOT EXISTS (SELECT 1 FROM workorder w WHERE w.order_no = o.order_no AND w.is_deleted = 0 AND w.claim_status IS NOT NULL AND w.claim_status <> 'NONE')
LIMIT 1;

-- 6) 已驳回：延误实为发货延迟（商家责任），驳回承运理赔
INSERT INTO workorder (order_no, type, level, description, status, liability,
                       claim_amount, compensation_amount, currency, claim_status,
                       claim_submitted_at, claim_approved_at, claim_reject_reason)
SELECT o.order_no, 'customs_delay', 'P2', '延迟系商家晚发货导致，非承运运输时效问题，驳回索赔', 'RESOLVED', 'merchant',
       GREATEST(o.declared_value, 200), 0, o.declared_currency, 'REJECTED',
       DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), '发货延迟属商家责任，驳回对承运商的理赔'
FROM logistics_order o
WHERE o.order_no IN ('OMT-TMS-SEED-0005')
  AND NOT EXISTS (SELECT 1 FROM workorder w WHERE w.order_no = o.order_no AND w.is_deleted = 0 AND w.claim_status IS NOT NULL AND w.claim_status <> 'NONE')
LIMIT 1;
