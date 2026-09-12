-- ============================================================
-- Sentinel · 订单/地址真实业务数据迁移
-- 把所有订单的买家信息替换为真实形态：真实姓名邮箱、国家区号手机号、
-- 真实城市街道邮编、语言对齐母语(ru/en/pt/de)。幂等可重跑。
-- 每国家按订单 id 轮询到对应真实买手池，覆盖全部订单。
-- ============================================================

USE `sentinel`;

-- 1. 统一历史目的国写法：Russia -> RU
UPDATE logistics_order SET destination_country = 'RU' WHERE destination_country = 'Russia' AND is_deleted = 0;

-- 2. RU 订单：俄语买家（真实俄罗斯姓名/城市/街道/邮编）
UPDATE logistics_order o SET
    buyer_id     = ELT(MOD(o.id, 8) + 1,
        'd.ivanov@yandex.ru','a.smirnova@mail.ru','s.petrov@yandex.ru','o.kuznetsova@gmail.com',
        'i.sokolov@mail.ru','e.volkova@yandex.ru','m.kozlov@gmail.com','n.morozova@mail.ru'),
    buyer_phone  = ELT(MOD(o.id, 8) + 1,
        '+79161234567','+79261024567','+79033456789','+79250004567',
        '+79015558743','+79166654321','+79274445566','+79098887766'),
    buyer_city   = ELT(MOD(o.id, 8) + 1,
        'Москва','Санкт-Петербург','Новосибирск','Екатеринбург',
        'Казань','Владивосток','Самара','Нижний Новгород'),
    buyer_address = ELT(MOD(o.id, 8) + 1,
        'ул. Тверская, д. 12, кв. 34','Невский пр., д. 88, кв. 12','Красный пр., д. 21','Ленина пр., д. 54, кв. 7',
        'ул. Баумана, д. 30','ул. Светланская, д. 43, кв. 5','ул. Молодогвардейская, д. 151','ул. Большая Покровская, д. 62'),
    buyer_postal = ELT(MOD(o.id, 8) + 1,
        '125009','190000','630000','620000',
        '420000','690000','443000','603000'),
    buyer_language = 'ru'
WHERE o.is_deleted = 0 AND o.destination_country = 'RU';

-- 3. US 订单：英语买家（真实美国姓名/城市/街道/邮编）
UPDATE logistics_order o SET
    buyer_id     = ELT(MOD(o.id, 8) + 1,
        'michael.johnson@gmail.com','sarah.williams@gmail.com','david.brown@outlook.com','emily.davis@gmail.com',
        'james.miller@yahoo.com','jessica.taylor@gmail.com','matt.wilson@gmail.com','ashley.martin@gmail.com'),
    buyer_phone  = ELT(MOD(o.id, 8) + 1,
        '+14155550101','+13125550102','+12125550103','+13125550104',
        '+17125550105','+12065550106','+16155550107','+16175550108'),
    buyer_city   = ELT(MOD(o.id, 8) + 1,
        'San Francisco','Los Angeles','New York','Chicago',
        'Houston','Seattle','Phoenix','Boston'),
    buyer_address = ELT(MOD(o.id, 8) + 1,
        '123 Market Street','456 Sunset Blvd','789 5th Avenue','300 Lakeview Dr',
        '1200 Main Street','900 Pine Street','400 N Central Ave','1 Beacon Street'),
    buyer_postal = ELT(MOD(o.id, 8) + 1,
        '94103','90028','10001','60601',
        '77002','98101','85004','02108'),
    buyer_language = 'en'
WHERE o.is_deleted = 0 AND o.destination_country = 'US';

-- 4. BR 订单：葡语买家（真实巴西姓名/城市/街道/邮编）
UPDATE logistics_order o SET
    buyer_id     = ELT(MOD(o.id, 6) + 1,
        'joao.silva@gmail.com','maria.santos@gmail.com','pedro.oliveira@gmail.com',
        'ana.souza@gmail.com','carlos.lima@gmail.com','lucia.pereira@gmail.com'),
    buyer_phone  = ELT(MOD(o.id, 6) + 1,
        '+5511987654321','+5521976543210','+5531987651234',
        '+5551987654321','+5571987654321','+5581987654321'),
    buyer_city   = ELT(MOD(o.id, 6) + 1,
        'São Paulo','Rio de Janeiro','Belo Horizonte',
        'Porto Alegre','Salvador','Recife'),
    buyer_address = ELT(MOD(o.id, 6) + 1,
        'Rua Oscar Freire 379','Av. Atlântica 2200','Av. Afonso Pena 1500',
        'Rua dos Andradas 900','Av. Oceânica 500','Av. Boa Viagem 700'),
    buyer_postal = ELT(MOD(o.id, 6) + 1,
        '01426-001','22021-001','30130-010',
        '90020-000','40170-110','51020-000'),
    buyer_language = 'pt'
WHERE o.is_deleted = 0 AND o.destination_country = 'BR';

-- 5. DE 订单：德语买家（真实德国姓名/城市/街道/邮编）
UPDATE logistics_order o SET
    buyer_id     = ELT(MOD(o.id, 6) + 1,
        'lukas.weber@gmail.com','anna.mueller@gmail.com','felix.schneider@gmail.com',
        'julia.fischer@gmail.com','paul.wagner@gmail.com','lena.hofmann@gmail.com'),
    buyer_phone  = ELT(MOD(o.id, 6) + 1,
        '+49301234567','+49891234567','+49401234567',
        '+49691234567','+49511234567','+49411234567'),
    buyer_city   = ELT(MOD(o.id, 6) + 1,
        'Berlin','München','Hamburg',
        'Frankfurt','Köln','Stuttgart'),
    buyer_address = ELT(MOD(o.id, 6) + 1,
        'Friedrichstraße 123','Leopoldstraße 45','Reeperbahn 9',
        'Hauptstraße 77','Domstraße 20','Königstraße 30'),
    buyer_postal = ELT(MOD(o.id, 6) + 1,
        '10117','80802','20359',
        '60329','50668','70173'),
    buyer_language = 'de'
WHERE o.is_deleted = 0 AND o.destination_country = 'DE';

-- 6. 清理无目的国的孤儿订单（兜底按语言）
UPDATE logistics_order o SET
    buyer_id = CASE WHEN buyer_id IS NULL OR buyer_id LIKE 'buyer%' OR buyer_id = '' THEN CONCAT('buyer.', o.id, '@mail.com') ELSE buyer_id END,
    buyer_language = CASE WHEN buyer_language NOT IN ('ru','en','es','pt','de') THEN 'en' ELSE buyer_language END
WHERE o.is_deleted = 0 AND o.destination_country NOT IN ('RU','US','BR','DE');

-- 7. 修正历史测试商家 MCH-005（乱码）为真实商家
UPDATE merchant SET
    merchant_name = '义乌晟达家居用品',
    contact_name = '周晓东',
    contact_phone = '13700000005',
    contact_email = 'zhouxd@sun-home.cn'
WHERE merchant_code = 'MCH-005' AND is_deleted = 0;

-- 8. 补充全部商家联系信息为真实形态
UPDATE merchant SET
    contact_name = '王海', contact_phone = '+8613800138001', contact_email = 'wanghai@lanjing.cn' WHERE merchant_code='MCH-0001';
UPDATE merchant SET
    contact_name = '李慧', contact_phone = '+8613800138002', contact_email = 'lihui@bailing.cn' WHERE merchant_code='MCH-0002';
UPDATE merchant SET
    contact_name = '陈航', contact_phone = '+8613800138003', contact_email = 'chenhang@qihang.cn' WHERE merchant_code='MCH-0003';
UPDATE merchant SET
    contact_name = '赵琳', contact_phone = '+8613800138004', contact_email = 'zhaolin@yuntu.cn' WHERE merchant_code='MCH-0004';
