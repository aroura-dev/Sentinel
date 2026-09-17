-- 2026-09-17: 邮箱验证码登录所需字段。
-- 该脚本面向已经创建的旧数据库；全新数据库已包含在 sentinel.sql 中。
ALTER TABLE `sentinel_user`
    ADD COLUMN `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱' AFTER `phone`,
    ADD UNIQUE KEY `uk_email` (`email`);