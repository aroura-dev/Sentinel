-- sentinel-ms : sentinel_msg（由 模块化单体版 库快照导出生成，勿手改）
CREATE DATABASE IF NOT EXISTS sentinel_msg DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE sentinel_msg;
SET NAMES utf8mb4;


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `message_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `message_template` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL DEFAULT '' COMMENT '标题',
  `audit_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '当前消息审核状态： 10.待审核 20.审核成功 30.被拒绝',
  `flow_id` varchar(50) DEFAULT NULL COMMENT '工单ID',
  `msg_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '当前消息状态：10.新建 20.停用 30.启用 40.等待发送 50.发送中 60.发送成功 70.发送失败',
  `cron_task_id` bigint(20) DEFAULT NULL COMMENT '定时任务Id (xxl-job-admin返回)',
  `cron_crowd_path` varchar(500) DEFAULT NULL COMMENT '定时发送人群的文件路径',
  `expect_push_time` varchar(100) DEFAULT NULL COMMENT '期望发送时间：0:立即发送 定时任务以及周期任务:cron表达式',
  `id_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '消息的发送ID类型：10. userId 20.did 30.手机号 40.openId 50.email 60.企业微信userId',
  `send_channel` int(10) NOT NULL DEFAULT '0' COMMENT '消息发送渠道：10.IM 20.Push 30.短信 40.Email 50.公众号 60.小程序 70.企业微信 80.钉钉机器人 90.钉钉工作通知 100.企业微信机器人 110.飞书机器人 110. 飞书应用消息 ',
  `template_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '10.运营类 20.技术类接口调用',
  `msg_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '10.通知类消息 20.营销类消息 30.验证码类消息',
  `shield_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '10.夜间不屏蔽 20.夜间屏蔽 30.夜间屏蔽(次日早上9点发送)',
  `msg_content` varchar(4096) NOT NULL DEFAULT '' COMMENT '消息内容 占位符用{$var}表示',
  `send_account` int(10) NOT NULL DEFAULT '0' COMMENT '发送账号 一个渠道下可存在多个账号',
  `creator` varchar(45) NOT NULL DEFAULT '' COMMENT '创建者',
  `updator` varchar(45) NOT NULL DEFAULT '' COMMENT '更新者',
  `auditor` varchar(45) NOT NULL DEFAULT '' COMMENT '审核人',
  `team` varchar(45) NOT NULL DEFAULT '' COMMENT '业务方团队',
  `proposer` varchar(45) NOT NULL DEFAULT '' COMMENT '业务方',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否删除：0.不删除 1.删除',
  `created` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',
  `updated` int(11) NOT NULL DEFAULT '0' COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_channel` (`send_channel`)
) ENGINE=InnoDB AUTO_INCREMENT=1013 DEFAULT CHARSET=utf8mb4 COMMENT='消息模板信息';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `message_template` WRITE;
/*!40000 ALTER TABLE `message_template` DISABLE KEYS */;
INSERT INTO `message_template` VALUES (3,'Sentinel 物流通知',20,NULL,30,NULL,NULL,NULL,10,30,10,10,0,'{\"content\":\"您的订单 {$orderNo} 物流状态已更新，请及时关注\"}',9001,'sentinel','sentinel','sentinel','sentinel','sentinel',0,1788570194,1788570194),(1001,'sentinel:IMPORT_CUSTOMS:zh',20,NULL,30,NULL,NULL,NULL,10,20,10,10,0,'您好，您的包裹已到达目的地分拨中心，正在分拣，请耐心等待。',9002,'sentinel','sentinel','sentinel','sentinel','sentinel',0,1788570194,1788570194),(1004,'sentinel:LAST_MILE:zh',20,NULL,30,NULL,NULL,NULL,10,20,10,10,0,'您好，您的包裹已进入末端派送，快递员将很快与您联系。',9002,'sentinel','sentinel','sentinel','sentinel','sentinel',0,1788570194,1788570194),(1007,'sentinel:DELIVERED:zh',20,NULL,30,NULL,NULL,NULL,10,20,10,10,0,'您的包裹已签收，感谢您的信任与支持！',9002,'sentinel','sentinel','sentinel','sentinel','sentinel',0,1788570194,1788570194),(1010,'sentinel:CUSTOMS_DELAY:zh',20,NULL,30,NULL,NULL,NULL,10,20,10,10,0,'您好，您的包裹在中转环节出现延误，我们正在加紧处理，请耐心等待。',9002,'sentinel','sentinel','sentinel','sentinel','sentinel',0,1788570194,1788570194);
/*!40000 ALTER TABLE `message_template` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sms_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sms_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `message_template_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '消息模板ID',
  `phone` bigint(20) NOT NULL DEFAULT '0' COMMENT '手机号',
  `supplier_id` tinyint(4) NOT NULL DEFAULT '0' COMMENT '发送短信渠道商的ID',
  `supplier_name` varchar(40) NOT NULL DEFAULT '' COMMENT '发送短信渠道商的名称',
  `msg_content` varchar(600) NOT NULL DEFAULT '' COMMENT '短信发送的内容',
  `series_id` varchar(100) NOT NULL DEFAULT '' COMMENT '下发批次的ID',
  `charging_num` tinyint(4) NOT NULL DEFAULT '0' COMMENT '计费条数',
  `report_content` varchar(50) NOT NULL DEFAULT '' COMMENT '回执内容',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '短信状态： 10.发送 20.成功 30.失败',
  `send_date` int(11) NOT NULL DEFAULT '0' COMMENT '发送日期：20211112',
  `created` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',
  `updated` int(11) NOT NULL DEFAULT '0' COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_send_date` (`send_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信记录信息';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `sms_record` WRITE;
/*!40000 ALTER TABLE `sms_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `sms_record` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `channel_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `channel_account` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL DEFAULT '' COMMENT '账号名称',
  `send_channel` tinyint(4) NOT NULL DEFAULT '0' COMMENT '消息发送渠道：10.IM 20.Push 30.短信 40.Email 50.公众号 60.小程序 70.企业微信 80.钉钉机器人 90.钉钉工作通知 100.企业微信机器人 110.飞书机器人 110. 飞书应用消息 ',
  `account_config` varchar(1024) NOT NULL DEFAULT '' COMMENT '账号配置',
  `creator` varchar(128) NOT NULL DEFAULT 'Sentinel' COMMENT '拥有者',
  `created` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',
  `updated` int(11) NOT NULL DEFAULT '0' COMMENT '更新时间',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否删除：0.不删除 1.删除',
  PRIMARY KEY (`id`),
  KEY `idx_send_channel` (`send_channel`)
) ENGINE=InnoDB AUTO_INCREMENT=9005 DEFAULT CHARSET=utf8mb4 COMMENT='渠道账号信息';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `channel_account` WRITE;
/*!40000 ALTER TABLE `channel_account` DISABLE KEYS */;
INSERT INTO `channel_account` VALUES (9001,'sandbox-sms',30,'{}','sentinel',0,0,0),(9002,'sandbox-push',20,'{}','sentinel',0,0,0),(9003,'sandbox-email',40,'{}','sentinel',0,0,0),(9004,'sandbox-feishu',110,'{}','sentinel',0,0,0);
/*!40000 ALTER TABLE `channel_account` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `unsubscribe`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `unsubscribe` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `buyer_id` varchar(64) NOT NULL COMMENT '买家ID',
  `channel` varchar(16) NOT NULL COMMENT '渠道：push/sms/email/feishu',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_buyer_id` (`buyer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退订表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `unsubscribe` WRITE;
/*!40000 ALTER TABLE `unsubscribe` DISABLE KEYS */;
/*!40000 ALTER TABLE `unsubscribe` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

