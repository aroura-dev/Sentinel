USE sentinel_msg;

-- 必须显式指定 id，并保持与 compose 的 SMS_ENGINE_TEMPLATE_ID 一致（当前 1011）。
--
-- 原写法不带 id，依赖 AUTO_INCREMENT，而表 DDL 里写着 AUTO_INCREMENT=1013，
-- 实际拿到的 id 由该自增值与已插入行的最大值共同决定 —— 实测稳定得到 1013。
-- 于是 auth-service 按 1011 去取模板时命中 TEMPLATE_NOT_FOUND(A0002)，
-- 所有验证码短信发送失败，短信验证码登录/注册/改密整条链路不可用。
--
-- 显式写死 id 后，无论 AUTO_INCREMENT 如何变化都能对上。
INSERT IGNORE INTO message_template
  (id,name,send_channel,send_account,msg_type,msg_content,is_deleted)
VALUES
  (1011,'sentinel:sms-code',30,9001,10,'{"content":"【Sentinel】您正在登录物流智能处置平台，登录验证码：{$code}，5 分钟内有效。请勿向他人泄露，如非本人操作请忽略。","url":""}',0);
