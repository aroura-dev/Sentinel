SET NAMES utf8mb4;
USE sentinel;

INSERT INTO inventory (id, sku, product_id, merchant_id, warehouse_id, on_hand, reserved, created_at, updated_at, is_deleted) VALUES
(1,  'SKU-LJ-001', 1,  1, 1, 420, 36, '2026-09-10 09:15:00', '2026-09-17 09:20:00', 0),
(2,  'SKU-LJ-002', 2,  1, 1, 180, 12, '2026-09-10 09:20:00', '2026-09-16 15:30:00', 0),
(3,  'SKU-LJ-003', 3,  1, 2, 96,  8,  '2026-09-11 10:10:00', '2026-09-17 08:40:00', 0),
(4,  'SKU-BL-001', 4,  2, 1, 260, 20, '2026-09-11 11:00:00', '2026-09-16 17:10:00', 0),
(5,  'SKU-BL-002', 5,  2, 2, 75,  6,  '2026-09-12 09:30:00', '2026-09-17 09:05:00', 0),
(6,  'SKU-BL-003', 6,  2, 2, 12,  3,  '2026-09-12 10:00:00', '2026-09-17 10:22:00', 0),
(7,  'SKU-QH-001', 7,  3, 1, 330, 25, '2026-09-13 09:00:00', '2026-09-16 16:30:00', 0),
(8,  'SKU-QH-002', 8,  3, 2, 145, 9,  '2026-09-13 10:15:00', '2026-09-17 09:45:00', 0),
(9,  'SKU-YT-001', 9,  4, 1, 88,  7,  '2026-09-14 09:20:00', '2026-09-17 10:10:00', 0),
(10, 'SKU-YT-002', 10, 4, 2, 0,   0,  '2026-09-14 10:00:00', '2026-09-17 10:30:00', 0)
ON DUPLICATE KEY UPDATE on_hand = VALUES(on_hand), reserved = VALUES(reserved), updated_at = VALUES(updated_at);

INSERT INTO inventory_flow (id, sku, biz_no, biz_type, qty, warehouse_id, created_at, is_deleted) VALUES
(1,  'SKU-LJ-001', 'PO-20260912-001',   'IN',     200, 1, '2026-09-12 09:10:00', 0),
(2,  'SKU-LJ-002', 'PO-20260912-002',   'IN',     120, 1, '2026-09-12 09:40:00', 0),
(3,  'SKU-LJ-001', 'OMT-SEED-0001',     'OUT',    2,   1, '2026-09-13 14:20:00', 0),
(4,  'SKU-LJ-002', 'OMT-SEED-0002',     'OUT',    1,   1, '2026-09-13 15:05:00', 0),
(5,  'SKU-LJ-003', 'OMT-SEED-0016',     'REFUND', 3,   2, '2026-09-14 10:30:00', 0),
(6,  'SKU-LJ-003', 'ADJ-20260915-001',  'ADJUST', 2,   2, '2026-09-15 11:00:00', 0),
(7,  'SKU-BL-001', 'PO-20260916-003',   'IN',     300, 1, '2026-09-16 09:00:00', 0),
(8,  'SKU-QH-001', 'OMT-TMS-SEED-0005', 'OUT',    1,   1, '2026-09-16 16:20:00', 0),
(9,  'SKU-QH-002', 'OMT-TMS-SEED-0008', 'OUT',    2,   2, '2026-09-16 17:10:00', 0),
(10, 'SKU-LJ-001', 'OMT-SEED-0019',     'REFUND', 1,   1, '2026-09-17 08:30:00', 0),
(11, 'SKU-BL-002', 'PO-20260917-001',   'IN',     80,  2, '2026-09-17 09:00:00', 0),
(12, 'SKU-LJ-001', 'OMT-SEED-0022',     'OUT',    4,   1, '2026-09-17 09:40:00', 0),
(13, 'SKU-YT-001', 'OMT-SEED-0025',     'OUT',    3,   2, '2026-09-17 10:10:00', 0),
(14, 'SKU-BL-003', 'ADJ-20260917-002',  'ADJUST', 5,   2, '2026-09-17 10:25:00', 0),
(15, 'SKU-YT-002', 'PO-20260917-002',   'IN',     150, 2, '2026-09-17 10:35:00', 0)
ON DUPLICATE KEY UPDATE sku = VALUES(sku), biz_no = VALUES(biz_no), biz_type = VALUES(biz_type), qty = VALUES(qty), warehouse_id = VALUES(warehouse_id), created_at = VALUES(created_at);

INSERT INTO api_key (id, app_name, company, contact_name, contact_phone, contact_email, api_key, secret, scope, remark, status, created_by, created_at, is_deleted) VALUES
-- key 1 额外持有 track:read 与 workorder:write：/api/omnimerchant/** 启用 API Key + scope 校验后，
-- 这两个权限是它调用对外轨迹查询与工单回调接口的必需项（另外两个应用不应持有）。
(1, '物流运营控制台', '深圳蓝鲸科技', '陈浩', '13800000004', 'chenhao@example.com', 'sk_sentinel_demo_001', 'secret_demo_8f2c91a7', 'order:read,waybill:read,track:read,workorder:write', '商家订单与运单只读接入；OmniMerchant 轨迹查询与工单回调', 1, '张伟', '2026-09-15 09:20:00', 0),
(2, '财务对账系统', 'Sentinel 财务中心', '赵敏', '13800000005', 'zhaomin@example.com', 'sk_sentinel_demo_002', 'secret_demo_4b81d3e6', 'bill:read,reconcile:read', '账单与差异对账只读接入', 1, '张伟', '2026-09-16 10:10:00', 0),
(3, '承运商数据交换', '中欧班列承运有限公司', '王海', '13800138001', 'wh@lanjing.cn', 'sk_sentinel_demo_003', 'secret_demo_1a7e5c92', 'waybill:read,track:read', '承运商轨迹查询接口', 1, '刘洋', '2026-09-17 09:30:00', 0)
ON DUPLICATE KEY UPDATE app_name = VALUES(app_name), company = VALUES(company), contact_name = VALUES(contact_name), contact_phone = VALUES(contact_phone), contact_email = VALUES(contact_email), scope = VALUES(scope), remark = VALUES(remark), status = VALUES(status), created_by = VALUES(created_by);
-- 数据合理性修复
-- 库存流水绑定同商家业务单据
UPDATE inventory_flow SET biz_no = 'OMT-SEED-0004'    WHERE id = 3  AND sku = 'SKU-LJ-001';
UPDATE inventory_flow SET biz_no = 'OMT-SEED-0008'    WHERE id = 4  AND sku = 'SKU-LJ-002';
UPDATE inventory_flow SET biz_no = 'OMT-SEED-0006'    WHERE id = 8  AND sku = 'SKU-QH-001';
UPDATE inventory_flow SET biz_no = 'OMT-SEED-0010'    WHERE id = 9  AND sku = 'SKU-QH-002';
UPDATE inventory_flow SET biz_no = 'RET-20260917-001' WHERE id = 10 AND sku = 'SKU-LJ-001';
UPDATE inventory_flow SET biz_no = 'OMT-SEED-0020'    WHERE id = 12 AND sku = 'SKU-LJ-001';
UPDATE inventory_flow SET biz_no = 'OMT-SEED-0011'    WHERE id = 13 AND sku = 'SKU-YT-001';
UPDATE logistics_order o
JOIN merchant m ON m.id = o.merchant_id
SET o.merchant_name = m.merchant_name
WHERE o.merchant_name IS NULL OR o.merchant_name <> m.merchant_name;

UPDATE inventory
SET on_hand = 150, reserved = 18
WHERE sku = 'SKU-YT-002' AND warehouse_id = 2;

UPDATE bill SET submitted_by = '赵敏' WHERE submitted_by IN ('finance', 'zhaomin');
UPDATE bill SET verified_by  = '赵敏' WHERE verified_by  IN ('finance', 'zhaomin');
UPDATE bill SET settled_by   = '赵敏' WHERE settled_by   IN ('finance', 'zhaomin');
UPDATE bill SET rejected_by  = '赵敏' WHERE rejected_by  IN ('finance', 'zhaomin');

UPDATE workorder
SET status = 'CLOSED'
WHERE id = 400003 AND status = 'OPEN' AND resolution = '丢失理赔已赔付';