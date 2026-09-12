USE sentinel_msg;
INSERT IGNORE INTO message_template (name,send_channel,send_account,msg_type,msg_content,is_deleted)
VALUES ('sentinel:sms-code',30,9001,10,'{"content":"【Sentinel】您正在登录物流智能处置平台，登录验证码：{$code}，5 分钟内有效。请勿向他人泄露，如非本人操作请忽略。","url":""}',0);
