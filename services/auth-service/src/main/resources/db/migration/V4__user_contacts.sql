SET NAMES utf8mb4;

-- 与 02_phone.sql 同模式：先加列再灌数据。
-- phone 由 02_phone.sql 添加；email 此前从未在本库建过，
-- 缺失会导致 ERROR 1054 并中断 mysql-auth 初始化（干净克隆时栈起不来）。
ALTER TABLE sentinel_user ADD COLUMN email VARCHAR(120) NULL COMMENT '邮箱' AFTER phone;

UPDATE sentinel_user SET phone = '13800000001', email = 'zhangwei@example.com', status = 1 WHERE username = '张伟';
UPDATE sentinel_user SET phone = '13800000002', email = 'liuyang@example.com', status = 1 WHERE username = '刘洋';
UPDATE sentinel_user SET phone = '13800000003', email = 'wangfang@example.com', status = 1 WHERE username = '王芳';
UPDATE sentinel_user SET phone = '13800000004', email = 'chenhao@example.com', status = 1 WHERE username = '陈浩';
UPDATE sentinel_user SET phone = '13800000005', email = 'zhaomin@example.com', status = 1 WHERE username = '赵敏';