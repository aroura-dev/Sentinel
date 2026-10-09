-- 手机验证码登录：sentinel_user 增加 phone 字段并给演示账号种手机号
ALTER TABLE sentinel_user ADD COLUMN phone VARCHAR(20) NULL COMMENT '手机号(验证码登录)' AFTER username;
UPDATE sentinel_user SET phone = CASE username
  WHEN '张伟'    THEN '13800000001'
  WHEN '刘洋' THEN '13800000002'
  WHEN '王芳'       THEN '13800000003'
  WHEN '陈浩' THEN '13800000004'
  WHEN '赵敏'  THEN '13800000005'
  ELSE NULL END
WHERE username IN ('张伟','刘洋','王芳','陈浩','赵敏');
