-- ============================================================
-- Sentinel 国内化数据迁移：把存量跨境数据转为国内（可重复执行）
-- 省份码：GD广东/ZJ浙江/JS江苏/SH上海/BJ北京/SC四川/HB湖北/HEN河南/SD山东/FJ福建/HN湖南/AH安徽/HEB河北/LN辽宁/SN陕西/CQ重庆
-- ============================================================

-- 1. 承运商 国内化
UPDATE carrier SET
  carrier_name = CASE carrier_code
    WHEN 'CARGOWAY' THEN '华南干线快运' WHEN 'AIRGO' THEN '畅达快递' WHEN 'SEAGO' THEN '粤通专线' ELSE carrier_name END
WHERE carrier_code IN ('CARGOWAY','AIRGO','SEAGO');

-- 2. 渠道 国内化（主 6 条 + 其余泛化）
UPDATE carrier_channel SET
  channel_code = CASE channel_code
    WHEN 'RU-RAIL-EXPR' THEN 'GD-RAIL' WHEN 'RU-AIR' THEN 'GD-EXPR' WHEN 'US-AIR' THEN 'ZJ-EXPR'
    WHEN 'BR-AIR' THEN 'SC-EXPR' WHEN 'DE-AIR' THEN 'JS-EXPR' WHEN 'SEA-RU' THEN 'GD-RAIL2'
    ELSE channel_code END,
  channel_name = CASE channel_code
    WHEN 'RU-RAIL-EXPR' THEN '华南干线快运' WHEN 'RU-AIR' THEN '广东省内快递' WHEN 'US-AIR' THEN '华东快递专线'
    WHEN 'BR-AIR' THEN '西南快递专线' WHEN 'DE-AIR' THEN '江苏快递专线' WHEN 'SEA-RU' THEN '华南干线整车'
    ELSE channel_name END,
  dest_country = CASE dest_country
    WHEN 'RU' THEN 'GD' WHEN 'US' THEN 'ZJ' WHEN 'BR' THEN 'SC' WHEN 'DE' THEN 'JS'
    WHEN 'GB' THEN 'ZJ' WHEN 'FR' THEN 'ZJ' WHEN 'ES' THEN 'GD' WHEN 'IT' THEN 'ZJ'
    WHEN 'NL' THEN 'ZJ' WHEN 'CA' THEN 'BJ' WHEN 'AU' THEN 'GD' WHEN 'JP' THEN 'SH'
    WHEN 'KR' THEN 'SD' WHEN 'MX' THEN 'GD' WHEN 'PL' THEN 'JS' WHEN 'SG' THEN 'GD' ELSE dest_country END,
  type = CASE WHEN type IN ('air','sea') THEN 'express' ELSE type END,
  transit_days_min = CASE WHEN transit_days_min > 6 THEN 2 ELSE transit_days_min END,
  transit_days_max = CASE WHEN transit_days_max > 8 THEN 4 ELSE transit_days_max END;

-- 3. 价卡 区域国内化
UPDATE carrier_rate SET zone = CASE zone
  WHEN 'RU' THEN 'GD' WHEN 'US' THEN 'ZJ' WHEN 'BR' THEN 'SC' WHEN 'DE' THEN 'JS'
  WHEN 'GB' THEN 'ZJ' WHEN 'FR' THEN 'ZJ' WHEN 'ES' THEN 'GD' WHEN 'IT' THEN 'ZJ'
  WHEN 'NL' THEN 'ZJ' WHEN 'CA' THEN 'BJ' WHEN 'AU' THEN 'GD' WHEN 'JP' THEN 'SH'
  WHEN 'KR' THEN 'SD' WHEN 'MX' THEN 'GD' WHEN 'PL' THEN 'JS' WHEN 'SG' THEN 'GD' ELSE zone END;

-- 4. 订单 国内化（目的地→省份、语言→zh、收件人中文、城市→省份中心城市 + 真实地址/邮编）
-- 注意：buyer_city/buyer_address/buyer_postal 的 CASE 必须引用**旧** destination_country，
-- 因此要放在 destination_country 赋值之前（MySQL 的 SET 从左到右按更新后的值求值）。
UPDATE logistics_order SET
  buyer_city = CASE destination_country
    WHEN 'RU' THEN '广州' WHEN 'US' THEN '杭州' WHEN 'BR' THEN '成都' WHEN 'DE' THEN '南京'
    WHEN 'GB' THEN '杭州' WHEN 'FR' THEN '杭州' WHEN 'ES' THEN '广州' WHEN 'IT' THEN '杭州'
    WHEN 'NL' THEN '杭州' WHEN 'CA' THEN '北京' WHEN 'AU' THEN '广州' WHEN 'JP' THEN '上海'
    WHEN 'KR' THEN '济南' WHEN 'MX' THEN '广州' WHEN 'PL' THEN '南京' WHEN 'SG' THEN '广州' ELSE buyer_city END,
  buyer_address = CASE destination_country
    WHEN 'RU' THEN '广东省广州市天河区华穗路88号' WHEN 'US' THEN '浙江省杭州市滨江区江南大道588号'
    WHEN 'BR' THEN '四川省成都市武侯区天府大道北段1700号' WHEN 'DE' THEN '江苏省南京市建邺区江东中路369号'
    WHEN 'GB' THEN '浙江省杭州市滨江区江南大道588号' WHEN 'FR' THEN '浙江省杭州市滨江区江南大道588号'
    WHEN 'ES' THEN '广东省广州市天河区华穗路88号' WHEN 'IT' THEN '浙江省杭州市滨江区江南大道588号'
    WHEN 'NL' THEN '浙江省杭州市滨江区江南大道588号' WHEN 'CA' THEN '北京市朝阳区建国路88号'
    WHEN 'AU' THEN '广东省广州市天河区华穗路88号' WHEN 'JP' THEN '上海市浦东新区世纪大道100号'
    WHEN 'KR' THEN '山东省济南市历下区经十路11001号' WHEN 'MX' THEN '广东省广州市天河区华穗路88号'
    WHEN 'PL' THEN '江苏省南京市建邺区江东中路369号' WHEN 'SG' THEN '广东省广州市天河区华穗路88号'
    ELSE buyer_address END,
  buyer_postal = CASE destination_country
    WHEN 'RU' THEN '510623' WHEN 'US' THEN '310051' WHEN 'BR' THEN '610041' WHEN 'DE' THEN '210019'
    WHEN 'GB' THEN '310051' WHEN 'FR' THEN '310051' WHEN 'ES' THEN '510623' WHEN 'IT' THEN '310051'
    WHEN 'NL' THEN '310051' WHEN 'CA' THEN '100020' WHEN 'AU' THEN '510623' WHEN 'JP' THEN '200120'
    WHEN 'KR' THEN '250014' WHEN 'MX' THEN '510623' WHEN 'PL' THEN '210019' WHEN 'SG' THEN '510623'
    ELSE buyer_postal END,
  destination_country = CASE destination_country
    WHEN 'RU' THEN 'GD' WHEN 'US' THEN 'ZJ' WHEN 'BR' THEN 'SC' WHEN 'DE' THEN 'JS'
    WHEN 'GB' THEN 'ZJ' WHEN 'FR' THEN 'ZJ' WHEN 'ES' THEN 'GD' WHEN 'IT' THEN 'ZJ'
    WHEN 'NL' THEN 'ZJ' WHEN 'CA' THEN 'BJ' WHEN 'AU' THEN 'GD' WHEN 'JP' THEN 'SH'
    WHEN 'KR' THEN 'SD' WHEN 'MX' THEN 'GD' WHEN 'PL' THEN 'JS' WHEN 'SG' THEN 'GD'
    ELSE destination_country END,
  buyer_language = 'zh',
  buyer_id = ELT(1 + MOD(id, 16),
    '张伟','王芳','李娜','刘洋','陈静','杨磊','黄敏','周强',
    '吴婷','徐峰','孙丽','马超','朱琳','胡军','郭静','何勇'),
  buyer_phone = CONCAT(ELT(1 + MOD(id, 8), '138','139','158','159','188','137','136','150'),
    10000000 + MOD(id * 79259, 90000000))
WHERE destination_country IN ('RU','US','BR','DE','GB','FR','ES','IT','NL','CA','AU','JP','KR','MX','PL','SG','RUSSIA','USA','GERMANY');

-- 5. 运单 区域国内化
UPDATE waybill SET zone = CASE zone
  WHEN 'RU' THEN 'GD' WHEN 'US' THEN 'ZJ' WHEN 'BR' THEN 'SC' WHEN 'DE' THEN 'JS'
  WHEN 'GB' THEN 'ZJ' WHEN 'FR' THEN 'ZJ' WHEN 'ES' THEN 'GD' WHEN 'IT' THEN 'ZJ'
  WHEN 'NL' THEN 'ZJ' WHEN 'CA' THEN 'BJ' WHEN 'AU' THEN 'GD' WHEN 'JP' THEN 'SH'
  WHEN 'KR' THEN 'SD' WHEN 'MX' THEN 'GD' WHEN 'PL' THEN 'JS' WHEN 'SG' THEN 'GD' ELSE zone END;

-- 6. 轨迹 国内化（位置/描述按节点改写）
UPDATE logistics_track SET
  location = CASE node
    WHEN 'CREATED' THEN '深圳仓' WHEN 'WAREHOUSE_OUT' THEN '深圳仓' WHEN 'DOMESTIC_PICKED' THEN '深圳·揽收'
    WHEN 'EXPORT_CUSTOMS' THEN '广州分拨中心' WHEN 'IN_TRANSIT' THEN '干线运输中'
    WHEN 'IMPORT_CUSTOMS' THEN '目的地分拨中心' WHEN 'LAST_MILE' THEN '目的地·末端派送'
    WHEN 'DELIVERED' THEN '目的地' WHEN 'CUSTOMS_DELAY' THEN '目的地分拨中心'
    WHEN 'DELIVERY_FAILED' THEN '目的地·末端派送' WHEN 'LOST' THEN '干线运输中' WHEN 'RETURNED' THEN '广州分拨中心'
    ELSE location END,
  raw_desc = CASE node
    WHEN 'CREATED' THEN '包裹已下单，等待揽收' WHEN 'WAREHOUSE_OUT' THEN '包裹已从仓库出库'
    WHEN 'DOMESTIC_PICKED' THEN '快递员已揽收' WHEN 'EXPORT_CUSTOMS' THEN '包裹已到达中转分拨中心，等待发运'
    WHEN 'IN_TRANSIT' THEN '包裹正在干线运输途中' WHEN 'IMPORT_CUSTOMS' THEN '包裹已到达目的地分拨中心，正在分拣'
    WHEN 'LAST_MILE' THEN '包裹已进入末端派送' WHEN 'DELIVERED' THEN '包裹已签收'
    WHEN 'CUSTOMS_DELAY' THEN '包裹在中转环节延误，正在处理' WHEN 'DELIVERY_FAILED' THEN '派送失败，将重新派送'
    WHEN 'LOST' THEN '包裹在运输途中丢失，正在核查' WHEN 'RETURNED' THEN '包裹已退回发件仓'
    ELSE raw_desc END;

-- 7. 通知模板 → 中文（12 条外语模板改名为 zh 并填中文，再按节点去重）
UPDATE message_template SET
  name = CASE
    WHEN name LIKE 'sentinel:IMPORT_CUSTOMS%' THEN 'sentinel:IMPORT_CUSTOMS:zh'
    WHEN name LIKE 'sentinel:LAST_MILE%' THEN 'sentinel:LAST_MILE:zh'
    WHEN name LIKE 'sentinel:DELIVERED%' THEN 'sentinel:DELIVERED:zh'
    WHEN name LIKE 'sentinel:CUSTOMS_DELAY%' THEN 'sentinel:CUSTOMS_DELAY:zh'
    ELSE name END,
  msg_content = CASE
    WHEN name LIKE 'sentinel:IMPORT_CUSTOMS%' THEN '您好，您的包裹已到达目的地分拨中心，正在分拣，请耐心等待。'
    WHEN name LIKE 'sentinel:LAST_MILE%' THEN '您好，您的包裹已进入末端派送，快递员将很快与您联系。'
    WHEN name LIKE 'sentinel:DELIVERED%' THEN '您的包裹已签收，感谢您的信任与支持！'
    WHEN name LIKE 'sentinel:CUSTOMS_DELAY%' THEN '您好，您的包裹在中转环节出现延误，我们正在加紧处理，请耐心等待。'
    ELSE msg_content END
WHERE name LIKE 'sentinel:%';
DELETE t1 FROM message_template t1
  JOIN message_template t2 ON t1.name = t2.name AND t1.id > t2.id
WHERE t1.name LIKE 'sentinel:%';

-- 8. 知识库 国内化（跨境 7 条改国内语义）
UPDATE anomaly_knowledge SET
  description = CASE status_code
    WHEN 'EXP-0040' THEN '中转分拨完成' WHEN 'EXP-0050' THEN '干线运输途中' WHEN 'EXP-0060' THEN '到达分拨中'
    WHEN 'CUS-1102' THEN '中转分拨环节滞留，包裹停留时间较长' WHEN 'CUS-1103' THEN '中转分拨查验（X光/开箱），放行延迟'
    WHEN 'CUS-1104' THEN '中转网络调整，转运周期拉长' WHEN 'CUS-1105' THEN '中转信息缺失，需补充面单信息'
    WHEN 'EXP-0051' THEN '干线运输途中丢件' ELSE description END,
  suggestion = CASE status_code
    WHEN 'EXP-0040' THEN '等待发运' WHEN 'EXP-0050' THEN '等待到达' WHEN 'EXP-0060' THEN '等待分拣'
    WHEN 'CUS-1102' THEN '联系承运商加快处理' WHEN 'CUS-1103' THEN '配合补充信息' WHEN 'CUS-1104' THEN '建议关注最新时效'
    WHEN 'CUS-1105' THEN '补充面单/地址信息' WHEN 'EXP-0051' THEN '联系承运商核查' ELSE suggestion END
WHERE status_code IN ('EXP-0040','EXP-0050','EXP-0060','CUS-1102','CUS-1103','CUS-1104','CUS-1105','EXP-0051');

-- 9. 工单 文案国内化
UPDATE workorder SET
  description = REPLACE(description, '清关', '中转'),
  resolution = REPLACE(resolution, '清关', '中转')
WHERE description LIKE '%清关%' OR resolution LIKE '%清关%';

-- 10. 通知记录 文案国内化（按节点全量转中文；任何外语内容一律转 zh，幂等）
UPDATE notification_record SET
  content = CASE node
    WHEN 'CREATED'          THEN '您的包裹已下单，等待揽收。'
    WHEN 'ORDER_SHIPPED'    THEN '您的订单已发货，正在准备运输。'
    WHEN 'WAREHOUSE_OUT'    THEN '您的包裹已从仓库出库。'
    WHEN 'DOMESTIC_PICKED'  THEN '快递员已揽收您的包裹。'
    WHEN 'EXPORT_CUSTOMS'   THEN '您的包裹已到达中转分拨中心，等待发运。'
    WHEN 'IN_TRANSIT'       THEN '您的包裹正在干线运输途中。'
    WHEN 'IMPORT_CUSTOMS'   THEN '您的包裹已到达目的地分拨中心，正在分拣。'
    WHEN 'LAST_MILE'        THEN '您的包裹已进入末端派送，快递员将很快与您联系。'
    WHEN 'DELIVERED'        THEN '您的包裹已签收，感谢您的信任与支持！'
    WHEN 'CUSTOMS_DELAY'    THEN '您的包裹在中转环节延误，正在处理，请耐心等待。'
    WHEN 'DELIVERY_FAILED'  THEN '您的包裹派送失败，将重新派送，请保持电话畅通。'
    WHEN 'LOST'             THEN '您的包裹在运输途中丢失，正在核查处理，请耐心等待。'
    WHEN 'RETURNED'         THEN '您的包裹已退回发件仓。'
    ELSE content END,
  language = 'zh'
WHERE node IN ('CREATED','ORDER_SHIPPED','WAREHOUSE_OUT','DOMESTIC_PICKED','EXPORT_CUSTOMS','IN_TRANSIT','IMPORT_CUSTOMS','LAST_MILE','DELIVERED','CUSTOMS_DELAY','DELIVERY_FAILED','LOST','RETURNED');

-- 兜底：未知节点仍含外语内容 → 统一中文（字面关键词检测，避免字节字符区间误判）
UPDATE notification_record SET content = '您的物流状态已更新，请关注最新进展。', language = 'zh'
WHERE content REGEXP 'Здрав|Ваш|заказ|Поздрав|посыл|утерян|Извин|достав|тамож|задерж|Great|Hello|Hola|¡Buenas|Your ';

-- 11. Agent 调用日志 国内化（历史提示语/输出中的外语 → 中文，幂等）
-- ① 输入：语言指令统一 zh（提示语模式）
UPDATE agent_call_log SET
  input = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
      input,
      '请用 ru 语言','请用 中文 语言'),
      '请用 en 语言','请用 中文 语言'),
      '请用 es 语言','请用 中文 语言'),
      '请用 de 语言','请用 中文 语言'),
      '请用 pt 语言','请用 中文 语言'),
      '请用 fr 语言','请用 中文 语言'),
      '请用 it 语言','请用 中文 语言'),
      '请用 ja 语言','请用 中文 语言'),
      '请用 ko 语言','请用 中文 语言'),
      '请用 nl 语言','请用 中文 语言'),
      '请用 pl 语言','请用 中文 语言')
WHERE input REGEXP '请用 .*语言';

-- ② 输入：语言字段统一 zh（JSON 模式：language / buyer_language）
UPDATE agent_call_log SET
  input = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
      input,
      '"language":"ru"','"language":"zh"'),
      '"language":"en"','"language":"zh"'),
      '"language":"es"','"language":"zh"'),
      '"language":"pt"','"language":"zh"'),
      '"language":"de"','"language":"zh"'),
      '"language":"fr"','"language":"zh"'),
      '"language":"it"','"language":"zh"'),
      '"language":"ja"','"language":"zh"'),
      '"language":"ko"','"language":"zh"'),
      '"language":"nl"','"language":"zh"'),
      '"language":"pl"','"language":"zh"'),
      '"buyer_language":"ru"','"buyer_language":"zh"')
WHERE input LIKE '%"language":"%' OR input LIKE '%"buyer_language":"ru"%';

-- ③ 输入：旧提示语跨境语义 → 国内
UPDATE agent_call_log SET input = REPLACE(input, '跨境', '国内') WHERE input LIKE '%跨境%';

-- ④ 输出：外语模板/降级文案 → 中文
UPDATE agent_call_log SET output = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
      output,
      'Здравствуйте! Ваш заказ передан в местную доставку, скоро будет доставлен.', '您好，您的包裹已进入末端派送，快递员将很快与您联系。'),
      'Здравствуйте! Ваш заказ проходит таможенное оформление, ожидайте.', '您好，您的包裹已到达目的地分拨中心，正在分拣。'),
      'Поздравляем! Ваш заказ доставлен. Спасибо за покупку!', '您的包裹已签收，感谢您的信任与支持！'),
      'Здравствуйте! Ваш заказ задержан на таможне, мы решаем вопрос.', '您好，您的包裹在中转环节延误，正在处理，请耐心等待。'),
      'Здравствуйте! Статус вашего заказа: ', '您的订单状态：'),
      'Great news! Your package has been delivered. Thank you!', '您的包裹已签收，感谢您的信任与支持！'),
      'Hello! Your package is undergoing customs clearance, please wait.', '您好，您的包裹已到达目的地分拨中心，正在分拣。'),
      'Hello! Your package is out for local delivery.', '您的包裹已进入末端派送，快递员将很快与您联系。'),
      'Hello! Your package is delayed in customs, we are handling it.', '您好，您的包裹在中转环节延误，正在处理，请耐心等待。'),
      'Hello! Your order status: ', '您的订单状态：'),
      '¡Buenas noticias! Su paquete ha sido entregado. ¡Gracias!', '您的包裹已签收，感谢您的信任与支持！'),
      'Hola! Su paquete está en reparto local.', '您的包裹已进入末端派送，快递员将很快与您联系。')
WHERE output REGEXP 'Здрав|Great|Hello|Hola|¡Buenas';

-- 兜底：仍含西里尔文/英文字句的输出 → 统一中文（字面关键词检测，历史 LLM 外语长篇文案）
UPDATE agent_call_log SET output = '您的物流状态已更新，请关注最新进展。'
WHERE output REGEXP 'Здрав|Ваш|заказ|Поздрав|посыл|утерян|Извин|Great|Hello|Hola|¡Buenas|Your ' OR output REGEXP '^"[A-Za-z]{4,}';

-- 12. 乱码自愈（历史误用 latin1 加载导致的中文双编码还原；无乱码时零影响，幂等）
-- 还原公式：CONVERT(CAST(CONVERT(col USING latin1) AS BINARY) USING utf8mb4)
UPDATE merchant SET
  merchant_name = CONVERT(CAST(CONVERT(merchant_name USING latin1) AS BINARY) USING utf8mb4)
WHERE merchant_name REGEXP 'æ|å|ç|ä';

UPDATE product SET
  name = CONVERT(CAST(CONVERT(name USING latin1) AS BINARY) USING utf8mb4)
WHERE name REGEXP 'æ|å|ç|ä';

UPDATE warehouse SET
  warehouse_name = CONVERT(CAST(CONVERT(warehouse_name USING latin1) AS BINARY) USING utf8mb4),
  city           = CONVERT(CAST(CONVERT(city USING latin1) AS BINARY) USING utf8mb4),
  address        = CONVERT(CAST(CONVERT(address USING latin1) AS BINARY) USING utf8mb4)
WHERE warehouse_name REGEXP 'æ|å|ç|ä';

UPDATE logistics_order SET
  merchant_name  = CONVERT(CAST(CONVERT(merchant_name USING latin1) AS BINARY) USING utf8mb4),
  buyer_id       = CONVERT(CAST(CONVERT(buyer_id USING latin1) AS BINARY) USING utf8mb4),
  buyer_address  = CONVERT(CAST(CONVERT(buyer_address USING latin1) AS BINARY) USING utf8mb4)
WHERE buyer_id REGEXP 'æ|å|ç|ä' OR merchant_name REGEXP 'æ|å|ç|ä';

UPDATE workorder SET
  description = CONVERT(CAST(CONVERT(description USING latin1) AS BINARY) USING utf8mb4),
  resolution  = CONVERT(CAST(CONVERT(resolution USING latin1) AS BINARY) USING utf8mb4)
WHERE description REGEXP 'æ|å|ç|ä' OR resolution REGEXP 'æ|å|ç|ä';

UPDATE role SET
  name        = CONVERT(CAST(CONVERT(name USING latin1) AS BINARY) USING utf8mb4),
  description = CONVERT(CAST(CONVERT(description USING latin1) AS BINARY) USING utf8mb4)
WHERE name REGEXP 'æ|å|ç|ä';

UPDATE sentinel_user SET
  nickname = CONVERT(CAST(CONVERT(nickname USING latin1) AS BINARY) USING utf8mb4)
WHERE nickname REGEXP 'æ|å|ç|ä';

-- 13. 运单号 国内化（旧国外渠道码 → 国内省份码，按 id 生成唯一单号避免撞唯一键）
UPDATE waybill SET waybill_no = CONCAT('WB-', zone, '-', LPAD(id, 6, '0'))
WHERE waybill_no LIKE '%RU%' OR waybill_no LIKE '%US%' OR waybill_no LIKE '%BR%'
   OR waybill_no LIKE '%DE%' OR waybill_no LIKE '%AU%' OR waybill_no LIKE '%CA%'
   OR waybill_no LIKE '%FR%' OR waybill_no LIKE '%GB%' OR waybill_no LIKE '%JP%'
   OR waybill_no LIKE '%SG%' OR waybill_no LIKE '%AIR%' OR waybill_no LIKE '%RAIL%'
   OR waybill_no LIKE '%SEA%';

-- 14. 账单明细：按 waybill_id 同步运单号（保持引用一致）
UPDATE bill_item b JOIN waybill w ON b.waybill_id = w.id
SET b.waybill_no = w.waybill_no
WHERE b.waybill_no IS NULL OR b.waybill_no <> w.waybill_no;

-- 15. 工单英文状态码/处理建议 → 中文
UPDATE workorder SET
  description = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(description,
    'customs_delay','中转延误'),
    'delivery_failed','派送失败'),
    'returned','退回'),
    'lost','丢件'),
    'clearance delay','中转分拨延误'),
    'in transit','在途'),
  resolution = REPLACE(REPLACE(REPLACE(REPLACE(resolution,
    'customs delay over SLA, carrier liability, refund by declared value','中转延误超时效，承运商责任，按申报货值退款'),
    'lost_full_compensation','丢件全额赔付'),
    'delivery failed','派送失败'),
    'clearance delay','中转分拨延误')
WHERE description REGEXP '[A-Za-z]{4,}' OR resolution REGEXP '[A-Za-z]{4,}';

-- 16. 清理测试垃圾数据
DELETE FROM anomaly_knowledge WHERE status_code = 'TEST-001';
