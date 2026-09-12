USE austin;
-- ============================================================
-- 开放 API 应用凭证：企业化元数据字段（幂等迁移，可重复执行）
-- 新增：所属企业 / 负责人 / 联系电话 / 联系邮箱 / 用途说明
-- 说明：列不存在才 ALTER；fresh 环境 z-sentinel-closed-loop.sql 已含新列，此处自动跳过
-- ============================================================
SET @db = 'austin';
SET @tb = 'api_key';

SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA=@db AND TABLE_NAME=@tb AND COLUMN_NAME='company')=0,
  'ALTER TABLE `api_key` ADD COLUMN `company` VARCHAR(128) DEFAULT NULL COMMENT ''所属企业/对接主体'' AFTER `app_name`',
  'SELECT 1');
PREPARE s1 FROM @s; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA=@db AND TABLE_NAME=@tb AND COLUMN_NAME='contact_name')=0,
  'ALTER TABLE `api_key` ADD COLUMN `contact_name` VARCHAR(64) DEFAULT NULL COMMENT ''负责人'' AFTER `company`',
  'SELECT 1');
PREPARE s2 FROM @s; EXECUTE s2; DEALLOCATE PREPARE s2;

SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA=@db AND TABLE_NAME=@tb AND COLUMN_NAME='contact_phone')=0,
  'ALTER TABLE `api_key` ADD COLUMN `contact_phone` VARCHAR(32) DEFAULT NULL COMMENT ''联系电话'' AFTER `contact_name`',
  'SELECT 1');
PREPARE s3 FROM @s; EXECUTE s3; DEALLOCATE PREPARE s3;

SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA=@db AND TABLE_NAME=@tb AND COLUMN_NAME='contact_email')=0,
  'ALTER TABLE `api_key` ADD COLUMN `contact_email` VARCHAR(128) DEFAULT NULL COMMENT ''联系邮箱'' AFTER `contact_phone`',
  'SELECT 1');
PREPARE s4 FROM @s; EXECUTE s4; DEALLOCATE PREPARE s4;

SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA=@db AND TABLE_NAME=@tb AND COLUMN_NAME='remark')=0,
  'ALTER TABLE `api_key` ADD COLUMN `remark` VARCHAR(255) DEFAULT NULL COMMENT ''用途说明'' AFTER `scope`',
  'SELECT 1');
PREPARE s5 FROM @s; EXECUTE s5; DEALLOCATE PREPARE s5;