-- 2026-09-17: 手机验证码登录所需字段。
-- 该脚本面向已经创建的旧数据库；全新数据库已包含在 sentinel.sql 中。
ALTER TABLE `sentinel_user`
    ADD COLUMN `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号' AFTER `username`,
    ADD UNIQUE KEY `uk_phone` (`phone`);