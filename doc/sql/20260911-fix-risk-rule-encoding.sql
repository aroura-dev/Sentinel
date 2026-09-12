-- 修复历史导入导致的 risk_rule 规则名称乱码
-- 幂等，可重复执行

SET NAMES utf8mb4;
USE `sentinel`;

UPDATE `risk_rule`
SET `name` = 'SLA 违约自动建工单'
WHERE `scene` = 'SLA' AND `trigger_status` = 'BREACHED' AND `action` = 'CREATE_WORKORDER';

UPDATE `risk_rule`
SET `name` = 'SLA 预警自动通知'
WHERE `scene` = 'SLA' AND `trigger_status` = 'RISK' AND `action` = 'NOTIFY';
