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
  `name` varchar(100) NOT NULL DEFAULT '' COMMENT 'æ ‡é¢˜',
  `audit_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'å½“å‰æ¶ˆæ¯å®¡æ ¸çŠ¶æ€ï¼š 10.å¾…å®¡æ ¸ 20.å®¡æ ¸æˆåŠŸ 30.è¢«æ‹’ç»',
  `flow_id` varchar(50) DEFAULT NULL COMMENT 'å·¥å•ID',
  `msg_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'å½“å‰æ¶ˆæ¯çŠ¶æ€ï¼š10.æ–°å»º 20.åœç”¨ 30.å¯ç”¨ 40.ç­‰å¾…å‘é€ 50.å‘é€ä¸­ 60.å‘é€æˆåŠŸ 70.å‘é€å¤±è´¥',
  `cron_task_id` bigint(20) DEFAULT NULL COMMENT 'å®šæ—¶ä»»åŠ¡Id (xxl-job-adminè¿”å›ž)',
  `cron_crowd_path` varchar(500) DEFAULT NULL COMMENT 'å®šæ—¶å‘é€äººç¾¤çš„æ–‡ä»¶è·¯å¾„',
  `expect_push_time` varchar(100) DEFAULT NULL COMMENT 'æœŸæœ›å‘é€æ—¶é—´ï¼š0:ç«‹å³å‘é€ å®šæ—¶ä»»åŠ¡ä»¥åŠå‘¨æœŸä»»åŠ¡:cronè¡¨è¾¾å¼',
  `id_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'æ¶ˆæ¯çš„å‘é€IDç±»åž‹ï¼š10. userId 20.did 30.æ‰‹æœºå· 40.openId 50.email 60.ä¼ä¸šå¾®ä¿¡userId',
  `send_channel` int(10) NOT NULL DEFAULT '0' COMMENT 'æ¶ˆæ¯å‘é€æ¸ é“ï¼š10.IM 20.Push 30.çŸ­ä¿¡ 40.Email 50.å…¬ä¼—å· 60.å°ç¨‹åº 70.ä¼ä¸šå¾®ä¿¡ 80.é’‰é’‰æœºå™¨äºº 90.é’‰é’‰å·¥ä½œé€šçŸ¥ 100.ä¼ä¸šå¾®ä¿¡æœºå™¨äºº 110.é£žä¹¦æœºå™¨äºº 110. é£žä¹¦åº”ç”¨æ¶ˆæ¯ ',
  `template_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '10.è¿è¥ç±» 20.æŠ€æœ¯ç±»æŽ¥å£è°ƒç”¨',
  `msg_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '10.é€šçŸ¥ç±»æ¶ˆæ¯ 20.è¥é”€ç±»æ¶ˆæ¯ 30.éªŒè¯ç ç±»æ¶ˆæ¯',
  `shield_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '10.å¤œé—´ä¸å±è”½ 20.å¤œé—´å±è”½ 30.å¤œé—´å±è”½(æ¬¡æ—¥æ—©ä¸Š9ç‚¹å‘é€)',
  `msg_content` varchar(4096) NOT NULL DEFAULT '' COMMENT 'æ¶ˆæ¯å†…å®¹ å ä½ç¬¦ç”¨{$var}è¡¨ç¤º',
  `send_account` int(10) NOT NULL DEFAULT '0' COMMENT 'å‘é€è´¦å· ä¸€ä¸ªæ¸ é“ä¸‹å¯å­˜åœ¨å¤šä¸ªè´¦å·',
  `creator` varchar(45) NOT NULL DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `updator` varchar(45) NOT NULL DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `auditor` varchar(45) NOT NULL DEFAULT '' COMMENT 'å®¡æ ¸äºº',
  `team` varchar(45) NOT NULL DEFAULT '' COMMENT 'ä¸šåŠ¡æ–¹å›¢é˜Ÿ',
  `proposer` varchar(45) NOT NULL DEFAULT '' COMMENT 'ä¸šåŠ¡æ–¹',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'æ˜¯å¦åˆ é™¤ï¼š0.ä¸åˆ é™¤ 1.åˆ é™¤',
  `created` int(11) NOT NULL DEFAULT '0' COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated` int(11) NOT NULL DEFAULT '0' COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  KEY `idx_channel` (`send_channel`)
) ENGINE=InnoDB AUTO_INCREMENT=1013 DEFAULT CHARSET=utf8mb4 COMMENT='æ¶ˆæ¯æ¨¡æ¿ä¿¡æ¯';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `message_template` WRITE;
/*!40000 ALTER TABLE `message_template` DISABLE KEYS */;
INSERT INTO `message_template` VALUES (3,'Sentinel ç‰©æµé€šçŸ¥',20,NULL,30,NULL,NULL,NULL,10,30,10,10,0,'{\"content\":\"æ‚¨çš„è®¢å• {$orderNo} ç‰©æµçŠ¶æ€å·²æ›´æ–°ï¼Œè¯·åŠæ—¶å…³æ³¨\"}',9001,'sentinel','sentinel','sentinel','sentinel','sentinel',0,1788570194,1788570194),(1001,'sentinel:IMPORT_CUSTOMS:zh',20,NULL,30,NULL,NULL,NULL,10,20,10,10,0,'æ‚¨å¥½ï¼Œæ‚¨çš„åŒ…è£¹å·²åˆ°è¾¾ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒï¼Œæ­£åœ¨åˆ†æ‹£ï¼Œè¯·è€å¿ƒç­‰å¾…ã€‚',9002,'sentinel','sentinel','sentinel','sentinel','sentinel',0,1788570194,1788570194),(1004,'sentinel:LAST_MILE:zh',20,NULL,30,NULL,NULL,NULL,10,20,10,10,0,'æ‚¨å¥½ï¼Œæ‚¨çš„åŒ…è£¹å·²è¿›å…¥æœ«ç«¯æ´¾é€ï¼Œå¿«é€’å‘˜å°†å¾ˆå¿«ä¸Žæ‚¨è”ç³»ã€‚',9002,'sentinel','sentinel','sentinel','sentinel','sentinel',0,1788570194,1788570194),(1007,'sentinel:DELIVERED:zh',20,NULL,30,NULL,NULL,NULL,10,20,10,10,0,'æ‚¨çš„åŒ…è£¹å·²ç­¾æ”¶ï¼Œæ„Ÿè°¢æ‚¨çš„ä¿¡ä»»ä¸Žæ”¯æŒï¼',9002,'sentinel','sentinel','sentinel','sentinel','sentinel',0,1788570194,1788570194),(1010,'sentinel:CUSTOMS_DELAY:zh',20,NULL,30,NULL,NULL,NULL,10,20,10,10,0,'æ‚¨å¥½ï¼Œæ‚¨çš„åŒ…è£¹åœ¨ä¸­è½¬çŽ¯èŠ‚å‡ºçŽ°å»¶è¯¯ï¼Œæˆ‘ä»¬æ­£åœ¨åŠ ç´§å¤„ç†ï¼Œè¯·è€å¿ƒç­‰å¾…ã€‚',9002,'sentinel','sentinel','sentinel','sentinel','sentinel',0,1788570194,1788570194);
/*!40000 ALTER TABLE `message_template` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `sms_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sms_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `message_template_id` bigint(20) NOT NULL DEFAULT '0' COMMENT 'æ¶ˆæ¯æ¨¡æ¿ID',
  `phone` bigint(20) NOT NULL DEFAULT '0' COMMENT 'æ‰‹æœºå·',
  `supplier_id` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'å‘é€çŸ­ä¿¡æ¸ é“å•†çš„ID',
  `supplier_name` varchar(40) NOT NULL DEFAULT '' COMMENT 'å‘é€çŸ­ä¿¡æ¸ é“å•†çš„åç§°',
  `msg_content` varchar(600) NOT NULL DEFAULT '' COMMENT 'çŸ­ä¿¡å‘é€çš„å†…å®¹',
  `series_id` varchar(100) NOT NULL DEFAULT '' COMMENT 'ä¸‹å‘æ‰¹æ¬¡çš„ID',
  `charging_num` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'è®¡è´¹æ¡æ•°',
  `report_content` varchar(50) NOT NULL DEFAULT '' COMMENT 'å›žæ‰§å†…å®¹',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'çŸ­ä¿¡çŠ¶æ€ï¼š 10.å‘é€ 20.æˆåŠŸ 30.å¤±è´¥',
  `send_date` int(11) NOT NULL DEFAULT '0' COMMENT 'å‘é€æ—¥æœŸï¼š20211112',
  `created` int(11) NOT NULL DEFAULT '0' COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated` int(11) NOT NULL DEFAULT '0' COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`id`),
  KEY `idx_send_date` (`send_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='çŸ­ä¿¡è®°å½•ä¿¡æ¯';
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
  `name` varchar(100) NOT NULL DEFAULT '' COMMENT 'è´¦å·åç§°',
  `send_channel` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'æ¶ˆæ¯å‘é€æ¸ é“ï¼š10.IM 20.Push 30.çŸ­ä¿¡ 40.Email 50.å…¬ä¼—å· 60.å°ç¨‹åº 70.ä¼ä¸šå¾®ä¿¡ 80.é’‰é’‰æœºå™¨äºº 90.é’‰é’‰å·¥ä½œé€šçŸ¥ 100.ä¼ä¸šå¾®ä¿¡æœºå™¨äºº 110.é£žä¹¦æœºå™¨äºº 110. é£žä¹¦åº”ç”¨æ¶ˆæ¯ ',
  `account_config` varchar(1024) NOT NULL DEFAULT '' COMMENT 'è´¦å·é…ç½®',
  `creator` varchar(128) NOT NULL DEFAULT 'Java3y' COMMENT 'æ‹¥æœ‰è€…',
  `created` int(11) NOT NULL DEFAULT '0' COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated` int(11) NOT NULL DEFAULT '0' COMMENT 'æ›´æ–°æ—¶é—´',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'æ˜¯å¦åˆ é™¤ï¼š0.ä¸åˆ é™¤ 1.åˆ é™¤',
  PRIMARY KEY (`id`),
  KEY `idx_send_channel` (`send_channel`)
) ENGINE=InnoDB AUTO_INCREMENT=9005 DEFAULT CHARSET=utf8mb4 COMMENT='æ¸ é“è´¦å·ä¿¡æ¯';
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
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `buyer_id` varchar(64) NOT NULL COMMENT 'ä¹°å®¶ID',
  `channel` varchar(16) NOT NULL COMMENT 'æ¸ é“ï¼špush/sms/email/feishu',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  KEY `idx_buyer_id` (`buyer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='é€€è®¢è¡¨';
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

