-- ============================================================
-- 演示数据「国内中文化」迁移（幂等：只改含俄文的行）
-- 应用库：sentinel(monolith :3307) 与 sentinel_logistics(ms :33063)（数据同源）
-- 用法： mysql --default-character-set=utf8mb4 -uroot -proot123_A <库> < infra/sql/zh_domestic.sql
-- 注：
-- 1) 正则须内联字面量（变量形式 REGEXP 不可靠）。
-- 2) 校验残留俄文勿用 REGEXP（非 utf8 连接下中文 UTF-8 字节会被误判为西里尔），
--    改用字节级：HEX(col) LIKE '%D0%' OR HEX(col) LIKE '%D1%'（utf8 西里尔首字节 D0/D1，中文为 E…）。
-- ============================================================
SET NAMES utf8mb4;

-- 1) 商户（国内场景中性名，避免「跨境/国际」表述）
UPDATE merchant SET
  merchant_name = CASE id WHEN 1 THEN '苏州云帆物流' WHEN 2 THEN '上海澜海速运'
    WHEN 3 THEN '义乌通泰仓储' WHEN 4 THEN '宁波浙海集运' ELSE CONCAT('国内商户', merchant_code) END,
  contact_name  = '张经理'
WHERE merchant_name REGEXP '[А-Яа-яЁё]' OR merchant_name REGEXP '跨境|国际|贸易|进出口';
-- 若已中文化过一次（无俄文），仍要收敛「跨境/国际」表述：
UPDATE merchant SET merchant_name = CASE id WHEN 1 THEN '苏州云帆物流' WHEN 2 THEN '上海澜海速运'
  WHEN 3 THEN '义乌通泰仓储' WHEN 4 THEN '宁波浙海集运' ELSE merchant_name END
WHERE merchant_name REGEXP '跨境|国际|贸易|进出口';

-- 2) 承运商
UPDATE carrier SET carrier_name = CASE carrier_code
    WHEN 'CARGOWAY' THEN '环球货运' WHEN 'AIRGO' THEN '航空快运' WHEN 'SEAGO' THEN '海运全球'
    ELSE CONCAT('承运商', carrier_code) END
WHERE carrier_name REGEXP '[А-Яа-яЁё]';

-- 3) 物流渠道
UPDATE carrier_channel SET channel_name = CASE channel_code
    WHEN 'GD-RAIL' THEN '广东铁路专线' WHEN 'GD-EXPR' THEN '广东快运'
    WHEN 'ZJ-EXPR' THEN '浙江快运' WHEN 'SC-EXPR' THEN '四川快运'
    ELSE CONCAT('国内渠道', channel_code) END
WHERE channel_name REGEXP '[А-Яа-яЁё]';

-- 4) 仓库
UPDATE warehouse SET warehouse_name = CASE warehouse_code
    WHEN 'WH-SZ' THEN '深圳仓' WHEN 'WH-SH' THEN '上海仓' ELSE CONCAT('仓库', warehouse_code) END,
  address = CONCAT('中国·', warehouse_code, ' 境内集货仓')
WHERE warehouse_name REGEXP '[А-Яа-яЁё]' OR address REGEXP '[А-Яа-яЁё]';

-- 5) 商品
UPDATE product SET name = CASE (id MOD 4)
    WHEN 0 THEN CONCAT('智能数码配件-', id) WHEN 1 THEN CONCAT('家居日用百货-', id)
    WHEN 2 THEN CONCAT('潮流服饰箱包-', id) ELSE CONCAT('国内优选-', id) END
WHERE name REGEXP '[А-Яа-яЁё]' OR name LIKE '跨境选品-%';

-- 6) 订单：买家城市/地址/商户名 中文化（国内国内省份/城市）
UPDATE logistics_order SET
  buyer_city = ELT(1 + (id MOD 4), '苏州市', '上海市', '杭州市', '深圳市'),
  buyer_address = CONCAT('中国·', ELT(1 + (id MOD 4), '苏州工业园区星湖街', '浦东新区秀浦路', '滨江区江南大道', '宝安区福永街道'), id, '号'),
  merchant_name = COALESCE((SELECT m.merchant_name FROM merchant m WHERE m.id = logistics_order.merchant_id),
                           ELT(1 + (id MOD 4), '苏州云帆物流', '上海澜海速运', '义乌通泰仓储', '宁波浙海集运'))
WHERE buyer_city REGEXP '[А-Яа-яЁё]' OR buyer_address REGEXP '[А-Яа-яЁё]' OR merchant_name REGEXP '[А-Яа-яЁё]'
   OR merchant_name REGEXP '跨境|国际|贸易|进出口';

-- 7) 工单描述
UPDATE workorder SET description = CONCAT('订单物流异常：节点滞留待跟进（工单 #', id, '）')
WHERE description REGEXP '[А-Яа-яЁё]';

-- 8) 通知内容
UPDATE notification_record SET content = CONCAT('您的订单 ', order_no, ' 物流状态已更新，请及时关注。')
WHERE content REGEXP '[А-Яа-яЁё]';

-- 9) 售后买家地址/姓名
UPDATE after_sale SET
  buyer_name = CONCAT('买家', id),
  buyer_address = CONCAT('中国·收货地址（售后 #', id, '）')
WHERE buyer_name REGEXP '[А-Яа-яЁё]' OR buyer_address REGEXP '[А-Яа-яЁё]';
