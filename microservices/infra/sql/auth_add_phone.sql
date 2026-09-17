-- 手机验证码登录：sentinel_user 增加 phone 字段并给演示账号种手机号
USE sentinel_auth;
ALTER TABLE sentinel_user ADD COLUMN phone VARCHAR(20) NULL COMMENT '手机号(验证码登录)' AFTER username;
UPDATE sentinel_user SET phone = CASE username
  WHEN 'zhangwei'    THEN '13800000001'
  WHEN 'liuyang' THEN '13800000002'
  WHEN 'wangfang'       THEN '13800000003'
  WHEN 'chenhao' THEN '13800000004'
  WHEN 'zhaomin'  THEN '13800000005'
  ELSE NULL END
WHERE username IN ('zhangwei','liuyang','wangfang','chenhao','zhaomin');
