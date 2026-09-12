-- 手机验证码登录：sentinel_user 增加 phone 字段并给演示账号种手机号
USE sentinel_auth;
ALTER TABLE sentinel_user ADD COLUMN phone VARCHAR(20) NULL COMMENT '手机号(验证码登录)' AFTER username;
UPDATE sentinel_user SET phone = CASE username
  WHEN 'admin'    THEN '13800000001'
  WHEN 'operator' THEN '13800000002'
  WHEN 'cs'       THEN '13800000003'
  WHEN 'merchant' THEN '13800000004'
  WHEN 'finance'  THEN '13800000005'
  ELSE NULL END
WHERE username IN ('admin','operator','cs','merchant','finance');
