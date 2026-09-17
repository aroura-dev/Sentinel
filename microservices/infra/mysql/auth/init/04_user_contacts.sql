USE sentinel_auth;
SET NAMES utf8mb4;

UPDATE sentinel_user SET phone = '13800000001', email = 'zhangwei@example.com', status = 1 WHERE username = '张伟';
UPDATE sentinel_user SET phone = '13800000002', email = 'liuyang@example.com', status = 1 WHERE username = '刘洋';
UPDATE sentinel_user SET phone = '13800000003', email = 'wangfang@example.com', status = 1 WHERE username = '王芳';
UPDATE sentinel_user SET phone = '13800000004', email = 'chenhao@example.com', status = 1 WHERE username = '陈浩';
UPDATE sentinel_user SET phone = '13800000005', email = 'zhaomin@example.com', status = 1 WHERE username = '赵敏';