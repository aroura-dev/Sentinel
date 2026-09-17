-- 2026-09-17: 用户头像字段（保存上传图片的 data URL）。
-- 该脚本面向已经创建的旧数据库；全新数据库已包含在 sentinel.sql 中。
ALTER TABLE `sentinel_user`
    ADD COLUMN `avatar` MEDIUMTEXT DEFAULT NULL COMMENT '头像 data URL' AFTER `email`;