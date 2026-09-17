USE sentinel_logistics;
SET NAMES utf8mb4;

-- 一次性迁移：merchant 冗余 owner_username（替代跨库 JOIN sentinel_user，供物流服务行级过滤）
-- 注意：mysql 5.7 不支持 ADD COLUMN IF NOT EXISTS，本脚本一次性执行；重复执行会报 duplicate column（可忽略或先 DROP）
ALTER TABLE merchant ADD COLUMN owner_username VARCHAR(64) NULL COMMENT '商户登录用户名（冗余）' AFTER user_id;

-- 离线快照回填：merchant.user_id → sentinel_user.username（auth/logistics 已分库，无法再跨库 JOIN）
UPDATE merchant SET owner_username = '' WHERE id = 2 AND (owner_username IS NULL OR owner_username = '');
UPDATE merchant SET owner_username = '' WHERE id = 3 AND (owner_username IS NULL OR owner_username = '');
UPDATE merchant SET owner_username = '' WHERE id = 4 AND (owner_username IS NULL OR owner_username = '');
UPDATE merchant SET owner_username = 'chenhao' WHERE id = 1 AND (owner_username IS NULL OR owner_username = '');
