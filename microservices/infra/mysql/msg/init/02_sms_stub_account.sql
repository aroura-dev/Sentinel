-- SMS stub 账号(9001)：让 msg-service 在无真实短信网关时可落 sms_record
USE sentinel_msg;
INSERT INTO channel_account (id,name,send_channel,account_config,is_deleted)
VALUES (9001,'yunpian-stub',30,'{"url":"http://sms-stub:18999/","apikey":"test","tplId":"1","tpl_id":"1","supplierId":20,"supplierName":"yunpian-stub","scriptName":"YunPianSmsScript"}',0)
ON DUPLICATE KEY UPDATE account_config=VALUES(account_config);
