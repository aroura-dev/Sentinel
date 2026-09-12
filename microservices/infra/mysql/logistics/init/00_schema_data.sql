-- sentinel-ms : sentinel_logistics（由 模块化单体版 库快照导出生成，勿手改）
CREATE DATABASE IF NOT EXISTS sentinel_logistics DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE sentinel_logistics;
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
DROP TABLE IF EXISTS `after_sale`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `after_sale` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) NOT NULL COMMENT 'å…³è”è®¢å•å·',
  `merchant_id` bigint(20) DEFAULT NULL,
  `type` varchar(32) NOT NULL DEFAULT 'RETURN' COMMENT 'RETURNé€€è´§/EXCHANGEæ¢è´§',
  `reason` varchar(255) DEFAULT NULL COMMENT 'é€€è´§åŽŸå› ',
  `refund_amount` decimal(12,2) DEFAULT '0.00' COMMENT 'é€€æ¬¾é‡‘é¢',
  `currency` varchar(8) DEFAULT 'CNY',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDINGå—ç†/REFUNDINGé€€æ¬¾ä¸­/REFUNDEDå·²é€€æ¬¾/RESHIPPEDå·²é‡å‘/CLOSEDå…³é—­',
  `buyer_name` varchar(64) DEFAULT NULL,
  `buyer_address` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(4) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_after_sale_order` (`order_no`),
  KEY `idx_after_sale_merchant` (`merchant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='é€†å‘å”®åŽé€€è´§å•';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `after_sale` WRITE;
/*!40000 ALTER TABLE `after_sale` DISABLE KEYS */;
INSERT INTO `after_sale` VALUES (1,'OMT-TMS-SEED-0001',1,'RETURN','å¤–åŒ…è£…å®Œå¥½ä½†å†…ä»¶ç ´æŸï¼Œç”³è¯·é€€è´§é€€æ¬¾',360.00,'CNY','PENDING','+14155550101','789 5th Avenue','2026-09-05 09:03:16','2026-09-05 09:03:16',0),(2,'OMT-TMS-SEED-0007',3,'RETURN','åž‹å·æ‹é”™äº†ï¼Œ7 å¤©æ— ç†ç”±é€€è´§',210.00,'CNY','PENDING','+49301234567','FriedrichstraÃŸe 123','2026-09-05 09:03:16','2026-09-05 09:03:16',0),(3,'OMT-TMS-SEED-0010',2,'RETURN','æ´¾é€ä¸¤æ¬¡ä¹°å®¶å‡æœªç­¾æ”¶ï¼Œæ‹’æ”¶é€€å›žå¹¶é€€æ¬¾',240.00,'CNY','REFUNDED','+79033456789','ÐšÑ€Ð°ÑÐ½Ñ‹Ð¹ Ð¿Ñ€., Ð´. 21','2026-09-05 09:03:16','2026-09-05 09:03:16',0);
/*!40000 ALTER TABLE `after_sale` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `api_key`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `api_key` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `app_name` varchar(64) NOT NULL,
  `company` varchar(128) DEFAULT NULL COMMENT 'æ‰€å±žä¼ä¸š/å¯¹æŽ¥ä¸»ä½“',
  `contact_name` varchar(64) DEFAULT NULL COMMENT 'è´Ÿè´£äºº',
  `contact_phone` varchar(32) DEFAULT NULL COMMENT 'è”ç³»ç”µè¯',
  `contact_email` varchar(128) DEFAULT NULL COMMENT 'è”ç³»é‚®ç®±',
  `api_key` varchar(64) NOT NULL,
  `secret` varchar(64) NOT NULL,
  `scope` varchar(128) DEFAULT 'order:read' COMMENT 'æŽˆæƒèŒƒå›´',
  `remark` varchar(255) DEFAULT NULL COMMENT 'ç”¨é€”è¯´æ˜Ž',
  `status` tinyint(4) DEFAULT '1' COMMENT '1å¯ç”¨ 0åœç”¨',
  `created_by` varchar(64) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint(4) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_key` (`api_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='å¼€æ”¾ API å¯†é’¥';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `api_key` WRITE;
/*!40000 ALTER TABLE `api_key` DISABLE KEYS */;
/*!40000 ALTER TABLE `api_key` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `bill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bill` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `bill_no` varchar(64) NOT NULL COMMENT 'è´¦å•å·',
  `carrier_id` bigint(20) NOT NULL COMMENT 'æ‰¿è¿å•†ID',
  `period_start` date NOT NULL COMMENT 'è´¦æœŸå¼€å§‹',
  `period_end` date NOT NULL COMMENT 'è´¦æœŸç»“æŸ',
  `currency` varchar(8) DEFAULT 'CNY' COMMENT 'å¸ç§',
  `total_amount` decimal(12,2) DEFAULT '0.00' COMMENT 'è´¦å•æ€»é¢',
  `status` varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'çŠ¶æ€:DRAFT/SUBMITTED/VERIFIED/SETTLED/REJECTED',
  `remark` varchar(256) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  `submitted_by` varchar(64) DEFAULT NULL COMMENT 'æäº¤äºº',
  `submitted_at` datetime DEFAULT NULL COMMENT 'æäº¤æ—¶é—´',
  `verified_by` varchar(64) DEFAULT NULL COMMENT 'æ ¸é”€äºº',
  `verified_at` datetime DEFAULT NULL COMMENT 'æ ¸é”€æ—¶é—´',
  `settled_by` varchar(64) DEFAULT NULL COMMENT 'ç»“ç®—äºº',
  `settled_at` datetime DEFAULT NULL COMMENT 'ç»“ç®—æ—¶é—´',
  `rejected_by` varchar(64) DEFAULT NULL COMMENT 'é©³å›žäºº',
  `rejected_at` datetime DEFAULT NULL COMMENT 'é©³å›žæ—¶é—´',
  `reject_reason` varchar(256) DEFAULT NULL COMMENT 'é©³å›žåŽŸå› ',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bill_no` (`bill_no`),
  UNIQUE KEY `uk_carrier_period` (`carrier_id`,`period_start`,`period_end`),
  KEY `idx_carrier_id` (`carrier_id`),
  KEY `idx_status` (`status`),
  KEY `idx_period` (`period_start`,`period_end`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='æ‰¿è¿å•†è´¦å•(æ‰¿è¿äººÃ—è´¦æœŸ)';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `bill` WRITE;
/*!40000 ALTER TABLE `bill` DISABLE KEYS */;
INSERT INTO `bill` VALUES (1,'BILL-AIRGO-202608',2,'2026-08-01','2026-08-31','CNY',341.90,'SUBMITTED',NULL,'finance','2026-09-03 09:03:15',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(2,'BILL-CARGOWAY-202608',1,'2026-08-01','2026-08-31','CNY',195.00,'DRAFT',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(3,'BILL-SEAGO-202607',3,'2026-07-01','2026-07-31','CNY',54.00,'SETTLED',NULL,'finance','2026-08-16 09:03:15','finance','2026-08-18 09:03:15','finance','2026-08-21 09:03:15',NULL,NULL,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:15',0);
/*!40000 ALTER TABLE `bill` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `bill_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bill_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `bill_id` bigint(20) NOT NULL COMMENT 'è´¦å•ID',
  `waybill_id` bigint(20) NOT NULL COMMENT 'è¿å•ID',
  `waybill_no` varchar(64) NOT NULL COMMENT 'è¿å•å·',
  `order_no` varchar(64) DEFAULT NULL COMMENT 'è®¢å•å·',
  `merchant_id` bigint(20) DEFAULT NULL COMMENT 'å•†å®¶ID',
  `tracking_no` varchar(64) DEFAULT NULL COMMENT 'è·Ÿè¸ªå·',
  `weight_kg` decimal(10,3) DEFAULT NULL COMMENT 'å®žé‡kg',
  `billable_weight_kg` decimal(10,3) DEFAULT NULL COMMENT 'è®¡è´¹é‡kg',
  `freight_cost` decimal(12,2) NOT NULL COMMENT 'è¿è´¹',
  `currency` varchar(8) DEFAULT 'CNY' COMMENT 'å¸ç§',
  `billed_at` datetime DEFAULT NULL COMMENT 'å…¥è´¦æ—¶é—´',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bill_waybill` (`bill_id`,`waybill_id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COMMENT='è´¦å•æ˜Žç»†(æ¯è¿å•ä¸€è¡Œ)';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `bill_item` WRITE;
/*!40000 ALTER TABLE `bill_item` DISABLE KEYS */;
INSERT INTO `bill_item` VALUES (1,1,1,'WB-ZJ-000001','OMT-TMS-SEED-0001',1,'YU-SEED-0001',0.560,0.560,53.20,'CNY','2026-09-03 09:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(2,1,2,'WB-ZJ-000002','OMT-TMS-SEED-0002',1,'YU-SEED-0002',0.900,0.900,85.50,'CNY','2026-09-03 09:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(3,1,7,'WB-JS-000007','OMT-TMS-SEED-0007',3,'DG-SEED-0007',0.350,0.350,32.20,'CNY','2026-09-03 09:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(4,1,8,'WB-ZJ-000008','OMT-TMS-SEED-0008',4,'YU-SEED-0008',1.800,1.800,171.00,'CNY','2026-09-03 09:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(5,2,4,'WB-RR-SEED-0004','OMT-TMS-SEED-0004',2,'RR-SEED-0004',0.600,0.600,65.00,'CNY','2026-09-05 09:03:15','2026-09-05 09:03:15','2026-09-05 09:03:15',0),(6,2,5,'WB-RR-SEED-0005','OMT-TMS-SEED-0005',2,'RR-SEED-0005',0.600,0.600,65.00,'CNY','2026-09-05 09:03:15','2026-09-05 09:03:15','2026-09-05 09:03:15',0),(7,2,10,'WB-RR-SEED-0010','OMT-TMS-SEED-0010',2,'RR-SEED-0010',1.200,1.200,65.00,'CNY','2026-09-05 09:03:15','2026-09-05 09:03:15','2026-09-05 09:03:15',0),(8,3,9,'WB-SE-SEED-0009','OMT-TMS-SEED-0009',4,'SE-SEED-0009',3.200,3.200,54.00,'CNY','2026-08-16 09:03:15','2026-09-05 09:03:15','2026-09-05 09:03:15',0);
/*!40000 ALTER TABLE `bill_item` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `carrier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `carrier` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `carrier_code` varchar(32) NOT NULL COMMENT 'æ‰¿è¿å•†ç¼–ç ',
  `carrier_name` varchar(128) NOT NULL COMMENT 'æ‰¿è¿å•†åç§°',
  `type` varchar(16) NOT NULL COMMENT 'é»˜è®¤è¿è¾“æ–¹å¼:rail/air/sea/express',
  `country` varchar(32) DEFAULT 'CN' COMMENT 'æ‰€åœ¨å›½å®¶',
  `api_endpoint` varchar(256) DEFAULT NULL COMMENT 'è½¨è¿¹å¯¹æŽ¥æŽ¥å£(é¢„ç•™)',
  `api_key` varchar(256) DEFAULT NULL COMMENT 'å¯¹æŽ¥å¯†é’¥(é¢„ç•™)',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€ï¼š1å¯ç”¨ 0åœç”¨',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_carrier_code` (`carrier_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='æ‰¿è¿å•†ä¸»æ•°æ®';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `carrier` WRITE;
/*!40000 ALTER TABLE `carrier` DISABLE KEYS */;
INSERT INTO `carrier` VALUES (1,'CARGOWAY','åŽå—å¹²çº¿å¿«è¿','rail','CN',NULL,NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(2,'AIRGO','ç•…è¾¾å¿«é€’','air','CN',NULL,NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(3,'SEAGO','ç²¤é€šä¸“çº¿','sea','CN',NULL,NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0);
/*!40000 ALTER TABLE `carrier` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `carrier_channel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `carrier_channel` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `carrier_id` bigint(20) NOT NULL COMMENT 'æ‰¿è¿å•†ID',
  `channel_code` varchar(64) NOT NULL COMMENT 'æ¸ é“ç¼–ç ',
  `channel_name` varchar(128) NOT NULL COMMENT 'æ¸ é“åç§°',
  `type` varchar(16) NOT NULL COMMENT 'è¿è¾“æ–¹å¼:rail/air/sea/express',
  `dest_country` varchar(8) NOT NULL COMMENT 'ç›®çš„å›½(RU/US/BR/DE)',
  `transit_days_min` int(11) NOT NULL DEFAULT '1' COMMENT 'SLAæœ€å°æ—¶æ•ˆ(å¤©)',
  `transit_days_max` int(11) NOT NULL DEFAULT '10' COMMENT 'SLAæœ€å¤§æ—¶æ•ˆ(å¤©)',
  `tracking_prefix` varchar(8) DEFAULT NULL COMMENT 'tracking_noå‰ç¼€',
  `min_billable_weight_kg` decimal(10,3) DEFAULT '0.000' COMMENT 'æœ€å°è®¡è´¹é‡é‡',
  `vol_divisor` int(11) DEFAULT '5000' COMMENT 'ä½“ç§¯é‡ç³»æ•°(ç©º/å¿«5000,é“/æµ·6000)',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€ï¼š1å¯ç”¨ 0åœç”¨',
  `remark` varchar(256) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_code` (`channel_code`),
  KEY `idx_carrier_id` (`carrier_id`),
  KEY `idx_dest_country` (`dest_country`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COMMENT='ç‰©æµæ¸ é“(æ‰¿è¿äººÃ—ç›®çš„å›½Ã—æ—¶æ•ˆ)';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `carrier_channel` WRITE;
/*!40000 ALTER TABLE `carrier_channel` DISABLE KEYS */;
INSERT INTO `carrier_channel` VALUES (1,1,'GD-RAIL','ä¸­æ¬§ç­åˆ—å¿«çº¿-ä¿„ç½—æ–¯','rail','GD',2,4,'RR',0.500,6000,1,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(2,2,'GD-EXPR','ä¿„ç½—æ–¯ç©ºè¿ä¸“çº¿','express','GD',5,4,'CY',0.000,5000,1,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(3,2,'ZJ-EXPR','ç¾Žå›½ç©ºè¿ä¸“çº¿','express','ZJ',5,4,'YU',0.000,5000,1,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(4,2,'SC-EXPR','å·´è¥¿ç©ºè¿ä¸“çº¿','express','SC',2,4,'BZ',0.000,5000,1,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(5,2,'JS-EXPR','å¾·å›½ç©ºè¿ä¸“çº¿','express','JS',6,4,'DG',0.000,5000,1,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(6,3,'GD-RAIL2','ä¿„ç½—æ–¯æµ·è¿å¤§è´§','express','GD',2,4,'SE',1.000,6000,1,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0);
/*!40000 ALTER TABLE `carrier_channel` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `carrier_rate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `carrier_rate` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `channel_id` bigint(20) NOT NULL COMMENT 'æ¸ é“ID',
  `zone` varchar(16) NOT NULL DEFAULT 'DEFAULT' COMMENT 'åŒºåŸŸ(ç›®çš„å›½/åˆ†åŒº),DEFAULT=æ¸ é“å…¨åŸŸ',
  `min_weight_kg` decimal(10,3) NOT NULL COMMENT 'é‡é‡æ®µä¸‹é™',
  `max_weight_kg` decimal(10,3) DEFAULT NULL COMMENT 'é‡é‡æ®µä¸Šé™(NULL=å¼€æ”¾ä¸Šé™)',
  `mode` varchar(20) NOT NULL COMMENT 'è®¡è´¹æ–¹å¼:PER_KGå•ä»· | FIRST_CONTINUEDé¦–ç»­é‡',
  `first_weight_kg` decimal(10,3) DEFAULT '0.000' COMMENT 'é¦–é‡(kg)',
  `first_price` decimal(12,4) DEFAULT '0.0000' COMMENT 'é¦–é‡ä»·æ ¼',
  `continued_weight_kg` decimal(10,3) DEFAULT '0.000' COMMENT 'ç»­é‡å•ä½(kg)',
  `continued_price` decimal(12,4) DEFAULT '0.0000' COMMENT 'ç»­é‡å•ä»·',
  `price` decimal(12,4) DEFAULT '0.0000' COMMENT 'PER_KGå•ä»·',
  `currency` varchar(8) NOT NULL DEFAULT 'CNY' COMMENT 'å¸ç§',
  `effective_from` date NOT NULL COMMENT 'ç”Ÿæ•ˆæ—¥æœŸ',
  `effective_to` date DEFAULT NULL COMMENT 'å¤±æ•ˆæ—¥æœŸ(NULL=é•¿æœŸæœ‰æ•ˆ)',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€ï¼š1å¯ç”¨ 0åœç”¨',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  KEY `idx_channel_id` (`channel_id`),
  KEY `idx_zone` (`zone`),
  KEY `idx_weight` (`min_weight_kg`,`max_weight_kg`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COMMENT='è¿è´¹ä»·å¡(æ¸ é“Ã—åŒºåŸŸÃ—é‡é‡æ®µ)';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `carrier_rate` WRITE;
/*!40000 ALTER TABLE `carrier_rate` DISABLE KEYS */;
INSERT INTO `carrier_rate` VALUES (1,1,'GD',0.000,2.000,'FIRST_CONTINUED',0.500,45.0000,1.000,20.0000,0.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(2,1,'GD',2.000,10.000,'FIRST_CONTINUED',0.500,45.0000,1.000,18.0000,0.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(3,1,'GD',10.000,30.000,'FIRST_CONTINUED',0.500,45.0000,1.000,15.0000,0.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(4,1,'GD',30.000,NULL,'FIRST_CONTINUED',0.500,45.0000,1.000,12.0000,0.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(5,2,'GD',0.000,2.000,'PER_KG',0.000,0.0000,0.000,0.0000,90.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(6,2,'GD',2.000,5.000,'PER_KG',0.000,0.0000,0.000,0.0000,85.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(7,2,'GD',5.000,10.000,'PER_KG',0.000,0.0000,0.000,0.0000,80.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(8,2,'GD',10.000,30.000,'PER_KG',0.000,0.0000,0.000,0.0000,72.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(9,2,'GD',30.000,NULL,'PER_KG',0.000,0.0000,0.000,0.0000,65.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(10,3,'ZJ',0.000,2.000,'PER_KG',0.000,0.0000,0.000,0.0000,95.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(11,3,'ZJ',2.000,5.000,'PER_KG',0.000,0.0000,0.000,0.0000,90.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(12,3,'ZJ',5.000,10.000,'PER_KG',0.000,0.0000,0.000,0.0000,85.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(13,3,'ZJ',10.000,30.000,'PER_KG',0.000,0.0000,0.000,0.0000,78.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(14,3,'ZJ',30.000,NULL,'PER_KG',0.000,0.0000,0.000,0.0000,70.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(15,4,'SC',0.000,2.000,'PER_KG',0.000,0.0000,0.000,0.0000,100.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(16,4,'SC',2.000,5.000,'PER_KG',0.000,0.0000,0.000,0.0000,95.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(17,4,'SC',5.000,10.000,'PER_KG',0.000,0.0000,0.000,0.0000,90.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(18,4,'SC',10.000,30.000,'PER_KG',0.000,0.0000,0.000,0.0000,82.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(19,4,'SC',30.000,NULL,'PER_KG',0.000,0.0000,0.000,0.0000,74.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(20,5,'JS',0.000,2.000,'PER_KG',0.000,0.0000,0.000,0.0000,92.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(21,5,'JS',2.000,5.000,'PER_KG',0.000,0.0000,0.000,0.0000,88.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(22,5,'JS',5.000,10.000,'PER_KG',0.000,0.0000,0.000,0.0000,83.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(23,5,'JS',10.000,30.000,'PER_KG',0.000,0.0000,0.000,0.0000,76.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(24,5,'JS',30.000,NULL,'PER_KG',0.000,0.0000,0.000,0.0000,68.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(25,6,'GD',0.000,30.000,'FIRST_CONTINUED',1.000,30.0000,1.000,8.0000,0.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(26,6,'GD',30.000,100.000,'FIRST_CONTINUED',1.000,30.0000,1.000,6.0000,0.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(27,6,'GD',100.000,NULL,'FIRST_CONTINUED',1.000,30.0000,1.000,5.0000,0.0000,'CNY','2026-01-01',NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0);
/*!40000 ALTER TABLE `carrier_rate` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `inventory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `inventory` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sku` varchar(64) NOT NULL,
  `product_id` bigint(20) DEFAULT NULL,
  `merchant_id` bigint(20) DEFAULT NULL,
  `warehouse_id` bigint(20) DEFAULT NULL,
  `on_hand` int(11) DEFAULT '0' COMMENT 'åœ¨åº“å¯ç”¨',
  `reserved` int(11) DEFAULT '0' COMMENT 'å·²å ç”¨',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(4) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inventory_sku_wh` (`sku`,`warehouse_id`),
  KEY `idx_inventory_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU åº“å­˜å°è´¦';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `inventory` WRITE;
/*!40000 ALTER TABLE `inventory` DISABLE KEYS */;
/*!40000 ALTER TABLE `inventory` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `inventory_flow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `inventory_flow` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sku` varchar(64) NOT NULL,
  `biz_no` varchar(64) DEFAULT NULL COMMENT 'ä¸šåŠ¡å•æ®å·(è®¢å•/è¿å•)',
  `biz_type` varchar(32) NOT NULL COMMENT 'OUTå‡ºåº“/INå…¥åº“/REFUNDé€€å›ž/ADJUSTè°ƒæ•´',
  `qty` int(11) NOT NULL,
  `warehouse_id` bigint(20) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint(4) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_flow_sku` (`sku`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='åº“å­˜å‡ºå…¥åº“æµæ°´';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `inventory_flow` WRITE;
/*!40000 ALTER TABLE `inventory_flow` DISABLE KEYS */;
/*!40000 ALTER TABLE `inventory_flow` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `logistics_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `logistics_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `order_no` varchar(64) NOT NULL COMMENT 'è®¢å•å·',
  `buyer_id` varchar(64) NOT NULL COMMENT 'ä¹°å®¶ID',
  `buyer_phone` varchar(32) DEFAULT NULL COMMENT 'ä¹°å®¶æ‰‹æœºå·',
  `buyer_language` varchar(10) NOT NULL DEFAULT 'zh' COMMENT 'ä¹°å®¶è¯­è¨€ï¼šru/en/es/zh',
  `merchant_id` bigint(20) DEFAULT NULL COMMENT 'å•†å®¶ID',
  `merchant_name` varchar(64) DEFAULT NULL COMMENT 'å•†å®¶åç§°',
  `destination_country` varchar(32) DEFAULT NULL COMMENT 'ç›®çš„å›½',
  `current_node` varchar(32) NOT NULL DEFAULT 'CREATED' COMMENT 'å½“å‰ç‰©æµèŠ‚ç‚¹ï¼ˆLogisticsNode.codeEnï¼‰',
  `review_status` varchar(16) NOT NULL DEFAULT 'APPROVED' COMMENT 'è®¢å•å®¡æ ¸çŠ¶æ€: PENDING/APPROVED/REJECTED',
  `channel_id` bigint(20) DEFAULT NULL COMMENT 'ç‰©æµæ¸ é“ID(carrier_channel.id)',
  `carrier_id` bigint(20) DEFAULT NULL COMMENT 'æ‰¿è¿å•†ID(carrier.id)',
  `warehouse_id` bigint(20) DEFAULT NULL COMMENT 'å‘è´§ä»“ID(warehouse.id)',
  `items_json` text COMMENT 'å•†å“è¡ŒJSON:[{sku,product_id,qty,unit_weight_kg,unit_declared_value,currency}]',
  `declared_value` decimal(12,2) DEFAULT '0.00' COMMENT 'ç”³æŠ¥ä»·å€¼',
  `declared_currency` varchar(8) DEFAULT 'CNY' COMMENT 'ç”³æŠ¥å¸ç§',
  `freight_cost` decimal(12,2) DEFAULT '0.00' COMMENT 'ä¸‹å•æ—¶è¿è´¹æŠ¥ä»·å¿«ç…§',
  `freight_currency` varchar(8) DEFAULT 'CNY' COMMENT 'è¿è´¹å¸ç§',
  `promise_eta` datetime DEFAULT NULL COMMENT 'æ‰¿è¯ºå¦¥æŠ•ETA',
  `sla_status` varchar(16) DEFAULT 'NA' COMMENT 'SLAçŠ¶æ€:NORMAL/RISK/BREACHED/NA',
  `waybill_no` varchar(64) DEFAULT NULL COMMENT 'è¿å•å·',
  `buyer_address` varchar(256) DEFAULT NULL COMMENT 'ä¹°å®¶æ”¶è´§åœ°å€',
  `buyer_city` varchar(64) DEFAULT NULL COMMENT 'ä¹°å®¶åŸŽå¸‚',
  `buyer_postal` varchar(32) DEFAULT NULL COMMENT 'ä¹°å®¶é‚®ç¼–',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤ï¼š0æœªåˆ  1å·²åˆ ',
  `reviewed_by` varchar(64) DEFAULT NULL COMMENT 'å®¡æ ¸äºº',
  `reviewed_at` datetime DEFAULT NULL COMMENT 'å®¡æ ¸æ—¶é—´',
  `reject_reason` varchar(255) DEFAULT NULL COMMENT 'é©³å›žåŽŸå› ',
  `business_notes` text COMMENT 'ä¸šåŠ¡å¤‡æ³¨ï¼ˆè·¨è§’è‰²ååŒæ‰¹æ³¨ï¼‰',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_buyer_id` (`buyer_id`),
  KEY `idx_current_node` (`current_node`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_channel_id` (`channel_id`),
  KEY `idx_sla_status` (`sla_status`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COMMENT='ç‰©æµè®¢å•è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `logistics_order` WRITE;
/*!40000 ALTER TABLE `logistics_order` DISABLE KEYS */;
INSERT INTO `logistics_order` VALUES (1,'OMT-SEED-0001','çŽ‹èŠ³','13910079259','zh',NULL,'æ·±åœ³è“é²¸ç§‘æŠ€','GD','DELIVERED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-08-28 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(2,'OMT-SEED-0002','æŽå¨œ','15810158518','zh',NULL,'ä¹‰ä¹Œç™¾çµè´¸æ˜“','GD','DELIVERED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-08-29 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(3,'OMT-SEED-0003','åˆ˜æ´‹','15910237777','zh',NULL,'å¹¿å·žå¯èˆªç”µå­','GD','DELIVERED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-08-30 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(4,'OMT-SEED-0004','é™ˆé™','18810317036','zh',NULL,'ä¸œèŽžæ™ºè”åˆ¶é€ ','GD','LAST_MILE','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-09-01 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(5,'OMT-SEED-0005','æ¨ç£Š','13710396295','zh',NULL,'æ­å·žäº‘å›¾ç§‘æŠ€','GD','IMPORT_CUSTOMS','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-08-31 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(6,'OMT-SEED-0006','é»„æ•','13610475554','zh',NULL,'æ·±åœ³è“é²¸ç§‘æŠ€','GD','IMPORT_CUSTOMS','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-08-31 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(7,'OMT-SEED-0007','å‘¨å¼º','15010554813','zh',NULL,'ä¹‰ä¹Œç™¾çµè´¸æ˜“','GD','CUSTOMS_DELAY','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-08-31 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(8,'OMT-SEED-0008','å´å©·','13810634072','zh',NULL,'å¹¿å·žå¯èˆªç”µå­','GD','IN_TRANSIT','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-09-02 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(9,'OMT-SEED-0009','å¾å³°','13910713331','zh',NULL,'ä¸œèŽžæ™ºè”åˆ¶é€ ','GD','IN_TRANSIT','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-09-02 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(10,'OMT-SEED-0010','å­™ä¸½','15810792590','zh',NULL,'æ­å·žäº‘å›¾ç§‘æŠ€','GD','DELIVERY_FAILED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-09-02 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(11,'OMT-SEED-0011','é©¬è¶…','15910871849','zh',NULL,'æ·±åœ³è“é²¸ç§‘æŠ€','GD','EXPORT_CUSTOMS','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-09-03 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(12,'OMT-SEED-0012','æœ±ç³','18810951108','zh',NULL,'ä¹‰ä¹Œç™¾çµè´¸æ˜“','GD','DOMESTIC_PICKED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-09-04 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(13,'OMT-SEED-0013','èƒ¡å†›','13711030367','zh',NULL,'å¹¿å·žå¯èˆªç”µå­','GD','WAREHOUSE_OUT','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-09-04 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(14,'OMT-SEED-0014','éƒ­é™','13611109626','zh',NULL,'ä¸œèŽžæ™ºè”åˆ¶é€ ','GD','CREATED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-09-05 03:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(15,'OMT-SEED-0015','ä½•å‹‡','15011188885','zh',NULL,'æ­å·žäº‘å›¾ç§‘æŠ€','GD','LOST','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-09-01 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(16,'OMT-SEED-0016','å¼ ä¼Ÿ','13811268144','zh',NULL,'æ·±åœ³è“é²¸ç§‘æŠ€','GD','RETURNED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-09-01 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(17,'OMT-SEED-0017','çŽ‹èŠ³','13911347403','zh',NULL,'æ·±åœ³è“é²¸ç§‘æŠ€','ZJ','IN_TRANSIT','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'æµ™æ±Ÿçœæ­å·žå¸‚æ»¨æ±ŸåŒºæ±Ÿå—å¤§é“588å·','æ­å·ž','310051','2026-09-02 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(18,'OMT-SEED-0018','æŽå¨œ','15811426662','zh',NULL,'å¹¿å·žå¯èˆªç”µå­','ZJ','IMPORT_CUSTOMS','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'æµ™æ±Ÿçœæ­å·žå¸‚æ»¨æ±ŸåŒºæ±Ÿå—å¤§é“588å·','æ­å·ž','310051','2026-09-01 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(19,'OMT-SEED-0019','åˆ˜æ´‹','15911505921','zh',NULL,'ä¸œèŽžæ™ºè”åˆ¶é€ ','ZJ','DELIVERED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'æµ™æ±Ÿçœæ­å·žå¸‚æ»¨æ±ŸåŒºæ±Ÿå—å¤§é“588å·','æ­å·ž','310051','2026-08-28 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(20,'OMT-SEED-0020','é™ˆé™','18811585180','zh',NULL,'æ­å·žäº‘å›¾ç§‘æŠ€','ZJ','LAST_MILE','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'æµ™æ±Ÿçœæ­å·žå¸‚æ»¨æ±ŸåŒºæ±Ÿå—å¤§é“588å·','æ­å·ž','310051','2026-09-01 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(21,'OMT-SEED-0021','æ¨ç£Š','13711664439','zh',NULL,'ä¹‰ä¹Œç™¾çµè´¸æ˜“','ZJ','CREATED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'æµ™æ±Ÿçœæ­å·žå¸‚æ»¨æ±ŸåŒºæ±Ÿå—å¤§é“588å·','æ­å·ž','310051','2026-09-05 04:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(22,'OMT-SEED-0022','é»„æ•','13611743698','zh',NULL,'æ·±åœ³è“é²¸ç§‘æŠ€','SC','IMPORT_CUSTOMS','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å››å·çœæˆéƒ½å¸‚æ­¦ä¾¯åŒºå¤©åºœå¤§é“åŒ—æ®µ1700å·','æˆéƒ½','610041','2026-08-31 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(23,'OMT-SEED-0023','å‘¨å¼º','15011822957','zh',NULL,'å¹¿å·žå¯èˆªç”µå­','SC','IN_TRANSIT','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å››å·çœæˆéƒ½å¸‚æ­¦ä¾¯åŒºå¤©åºœå¤§é“åŒ—æ®µ1700å·','æˆéƒ½','610041','2026-09-03 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(24,'OMT-SEED-0024','å´å©·','13811902216','zh',NULL,'ä¸œèŽžæ™ºè”åˆ¶é€ ','SC','DELIVERED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å››å·çœæˆéƒ½å¸‚æ­¦ä¾¯åŒºå¤©åºœå¤§é“åŒ—æ®µ1700å·','æˆéƒ½','610041','2026-08-29 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(25,'OMT-SEED-0025','å¾å³°','13911981475','zh',NULL,'æ·±åœ³è“é²¸ç§‘æŠ€','JS','IMPORT_CUSTOMS','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'æ±Ÿè‹çœå—äº¬å¸‚å»ºé‚ºåŒºæ±Ÿä¸œä¸­è·¯369å·','å—äº¬','210019','2026-09-01 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(26,'OMT-SEED-0026','å­™ä¸½','15812060734','zh',NULL,'ä¹‰ä¹Œç™¾çµè´¸æ˜“','JS','IN_TRANSIT','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'æ±Ÿè‹çœå—äº¬å¸‚å»ºé‚ºåŒºæ±Ÿä¸œä¸­è·¯369å·','å—äº¬','210019','2026-09-03 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(27,'OMT-SEED-0027','é©¬è¶…','15912139993','zh',NULL,'å¹¿å·žå¯èˆªç”µå­','JS','DELIVERED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'æ±Ÿè‹çœå—äº¬å¸‚å»ºé‚ºåŒºæ±Ÿä¸œä¸­è·¯369å·','å—äº¬','210019','2026-08-30 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(28,'OMT-SEED-0028','æœ±ç³','18812219252','zh',NULL,'æ·±åœ³è“é²¸ç§‘æŠ€','GD','IMPORT_CUSTOMS','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-08-30 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(29,'OMT-SEED-0029','èƒ¡å†›','13712298511','zh',NULL,'æ­å·žäº‘å›¾ç§‘æŠ€','GD','DELIVERED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-08-27 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(30,'OMT-SEED-0030','éƒ­é™','13612377770','zh',NULL,'ä¸œèŽžæ™ºè”åˆ¶é€ ','GD','LAST_MILE','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-08-31 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(31,'OMT-SEED-0031','ä½•å‹‡','15012457029','zh',NULL,'ä¹‰ä¹Œç™¾çµè´¸æ˜“','GD','DOMESTIC_PICKED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-09-04 15:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(32,'OMT-SEED-0032','å¼ ä¼Ÿ','13812536288','zh',NULL,'å¹¿å·žå¯èˆªç”µå­','GD','EXPORT_CUSTOMS','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-09-03 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(33,'OMT-TMS-SEED-0001','çŽ‹èŠ³','13912615547','zh',1,'æ·±åœ³è“é²¸ç§‘æŠ€','ZJ','DELIVERED','APPROVED',3,2,1,'[{\"sku\":\"SKU-LJ-002\",\"product_id\":2,\"qty\":2,\"unit_weight_kg\":0.28,\"unit_volume_l\":0.5,\"unit_declared_value\":180,\"currency\":\"CNY\"}]',360.00,'CNY',53.20,'CNY','2026-08-27 09:03:15','NORMAL','WB-US-SEED-0001','æµ™æ±Ÿçœæ­å·žå¸‚æ»¨æ±ŸåŒºæ±Ÿå—å¤§é“588å·','æ­å·ž','310051','2026-08-26 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(34,'OMT-TMS-SEED-0002','æŽå¨œ','15812694806','zh',1,'æ·±åœ³è“é²¸ç§‘æŠ€','ZJ','IN_TRANSIT','APPROVED',3,2,1,'[{\"sku\":\"SKU-LJ-001\",\"product_id\":1,\"qty\":2,\"unit_weight_kg\":0.45,\"unit_volume_l\":0.8,\"unit_declared_value\":320,\"currency\":\"CNY\"}]',640.00,'CNY',85.50,'CNY','2026-09-09 09:03:15','NORMAL','WB-US-SEED-0002','æµ™æ±Ÿçœæ­å·žå¸‚æ»¨æ±ŸåŒºæ±Ÿå—å¤§é“588å·','æ­å·ž','310051','2026-09-01 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(35,'OMT-TMS-SEED-0003','åˆ˜æ´‹','15912774065','zh',1,'æ·±åœ³è“é²¸ç§‘æŠ€','GD','WAREHOUSE_OUT','APPROVED',2,2,1,'[{\"sku\":\"SKU-LJ-001\",\"product_id\":1,\"qty\":1,\"unit_weight_kg\":0.45,\"unit_volume_l\":0.8,\"unit_declared_value\":320,\"currency\":\"CNY\"}]',320.00,'CNY',40.50,'CNY','2026-09-13 09:03:15','NORMAL','WB-RU-SEED-0003','å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-09-04 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(36,'OMT-TMS-SEED-0004','é™ˆé™','18812853324','zh',2,'ä¹‰ä¹Œç™¾çµè´¸æ˜“','GD','IMPORT_CUSTOMS','APPROVED',1,1,2,'[{\"sku\":\"SKU-BL-001\",\"product_id\":4,\"qty\":1,\"unit_weight_kg\":0.6,\"unit_volume_l\":2.2,\"unit_declared_value\":120,\"currency\":\"CNY\"}]',120.00,'CNY',65.00,'CNY','2026-09-15 09:03:15','BREACHED','WB-RR-SEED-0004','å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-08-28 09:03:15','2026-09-05 09:04:29',0,NULL,NULL,NULL,NULL),(37,'OMT-TMS-SEED-0005','æ¨ç£Š','13712932583','zh',2,'ä¹‰ä¹Œç™¾çµè´¸æ˜“','GD','CUSTOMS_DELAY','APPROVED',1,1,2,'[{\"sku\":\"SKU-BL-002\",\"product_id\":5,\"qty\":2,\"unit_weight_kg\":0.3,\"unit_volume_l\":1.1,\"unit_declared_value\":60,\"currency\":\"CNY\"}]',120.00,'CNY',65.00,'CNY','2026-09-03 09:03:15','BREACHED','WB-RR-SEED-0005','å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-08-16 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(38,'OMT-TMS-SEED-0006','é»„æ•','13613011842','zh',3,'å¹¿å·žå¯èˆªç”µå­','SC','LAST_MILE','APPROVED',4,2,2,'[{\"sku\":\"SKU-QH-001\",\"product_id\":7,\"qty\":1,\"unit_weight_kg\":0.8,\"unit_volume_l\":3.5,\"unit_declared_value\":150,\"currency\":\"CNY\"}]',150.00,'CNY',80.00,'CNY','2026-09-08 09:03:15','NORMAL','WB-BR-SEED-0006','å››å·çœæˆéƒ½å¸‚æ­¦ä¾¯åŒºå¤©åºœå¤§é“åŒ—æ®µ1700å·','æˆéƒ½','610041','2026-08-25 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(39,'OMT-TMS-SEED-0007','å‘¨å¼º','15013091101','zh',3,'å¹¿å·žå¯èˆªç”µå­','JS','DELIVERED','APPROVED',5,2,2,'[{\"sku\":\"SKU-QH-002\",\"product_id\":8,\"qty\":1,\"unit_weight_kg\":0.35,\"unit_volume_l\":1.2,\"unit_declared_value\":210,\"currency\":\"CNY\"}]',210.00,'CNY',32.20,'CNY','2026-09-02 09:03:15','NORMAL','WB-DE-SEED-0007','æ±Ÿè‹çœå—äº¬å¸‚å»ºé‚ºåŒºæ±Ÿä¸œä¸­è·¯369å·','å—äº¬','210019','2026-08-29 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(40,'OMT-TMS-SEED-0008','å´å©·','13813170360','zh',4,'æ­å·žäº‘å›¾ç§‘æŠ€','ZJ','LOST','APPROVED',3,2,1,'[{\"sku\":\"SKU-YT-001\",\"product_id\":9,\"qty\":1,\"unit_weight_kg\":1.8,\"unit_volume_l\":8.5,\"unit_declared_value\":420,\"currency\":\"CNY\"}]',420.00,'CNY',171.00,'CNY',NULL,'BREACHED','WB-US-SEED-0008','æµ™æ±Ÿçœæ­å·žå¸‚æ»¨æ±ŸåŒºæ±Ÿå—å¤§é“588å·','æ­å·ž','310051','2026-08-21 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(41,'OMT-TMS-SEED-0009','å¾å³°','13913249619','zh',4,'æ­å·žäº‘å›¾ç§‘æŠ€','GD','IN_TRANSIT','APPROVED',6,3,1,'[{\"sku\":\"SKU-YT-002\",\"product_id\":10,\"qty\":1,\"unit_weight_kg\":3.2,\"unit_volume_l\":9,\"unit_declared_value\":680,\"currency\":\"CNY\"}]',680.00,'CNY',54.00,'CNY','2026-10-05 09:03:15','BREACHED','WB-SE-SEED-0009','å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-08-31 09:03:15','2026-09-05 09:04:29',0,NULL,NULL,NULL,NULL),(42,'OMT-TMS-SEED-0010','å­™ä¸½','15813328878','zh',2,'ä¹‰ä¹Œç™¾çµè´¸æ˜“','GD','DELIVERY_FAILED','APPROVED',1,1,2,'[{\"sku\":\"SKU-BL-001\",\"product_id\":4,\"qty\":2,\"unit_weight_kg\":0.6,\"unit_volume_l\":2.2,\"unit_declared_value\":120,\"currency\":\"CNY\"}]',240.00,'CNY',65.00,'CNY','2026-09-04 09:03:15','BREACHED','WB-RR-SEED-0010','å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·','å¹¿å·ž','510623','2026-08-24 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `logistics_order` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `logistics_track`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `logistics_track` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `order_no` varchar(64) NOT NULL COMMENT 'è®¢å•å·',
  `node` varchar(32) NOT NULL COMMENT 'èŠ‚ç‚¹ï¼ˆLogisticsNode.codeEnï¼‰',
  `raw_status` varchar(64) DEFAULT NULL COMMENT 'ç‰©æµå•†åŽŸå§‹çŠ¶æ€ç ',
  `raw_desc` varchar(256) DEFAULT NULL COMMENT 'ç‰©æµå•†åŽŸå§‹æè¿°',
  `location` varchar(128) DEFAULT NULL COMMENT 'ä½ç½®',
  `carrier_code` varchar(32) DEFAULT NULL COMMENT 'æ‰¿è¿å•†ç¼–ç (é¢„ç•™çœŸå®žç‰©æµå•†è½¨è¿¹)',
  `track_time` datetime NOT NULL COMMENT 'è½¨è¿¹æ—¶é—´',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_node` (`node`),
  KEY `idx_carrier_code` (`carrier_code`)
) ENGINE=InnoDB AUTO_INCREMENT=200037 DEFAULT CHARSET=utf8mb4 COMMENT='ç‰©æµè½¨è¿¹è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `logistics_track` WRITE;
/*!40000 ALTER TABLE `logistics_track` DISABLE KEYS */;
INSERT INTO `logistics_track` VALUES (200001,'OMT-SEED-0001','CREATED','EXP-0010','åŒ…è£¹å·²ä¸‹å•ï¼Œç­‰å¾…æ½æ”¶','æ·±åœ³ä»“',NULL,'2026-08-28 11:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200002,'OMT-SEED-0001','WAREHOUSE_OUT','EXP-0020','åŒ…è£¹å·²ä»Žä»“åº“å‡ºåº“','æ·±åœ³ä»“',NULL,'2026-08-28 18:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200003,'OMT-SEED-0001','DOMESTIC_PICKED','EXP-0030','å¿«é€’å‘˜å·²æ½æ”¶','æ·±åœ³Â·æ½æ”¶',NULL,'2026-08-29 01:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200004,'OMT-SEED-0001','EXPORT_CUSTOMS','EXP-0040','åŒ…è£¹å·²åˆ°è¾¾ä¸­è½¬åˆ†æ‹¨ä¸­å¿ƒï¼Œç­‰å¾…å‘è¿','å¹¿å·žåˆ†æ‹¨ä¸­å¿ƒ',NULL,'2026-08-29 17:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200005,'OMT-SEED-0001','IN_TRANSIT','EXP-0050','åŒ…è£¹æ­£åœ¨å¹²çº¿è¿è¾“é€”ä¸­','å¹²çº¿è¿è¾“ä¸­',NULL,'2026-08-30 23:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200006,'OMT-SEED-0001','IMPORT_CUSTOMS','EXP-0060','åŒ…è£¹å·²åˆ°è¾¾ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒï¼Œæ­£åœ¨åˆ†æ‹£','ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒ',NULL,'2026-09-02 01:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200007,'OMT-SEED-0001','LAST_MILE','EXP-0070','åŒ…è£¹å·²è¿›å…¥æœ«ç«¯æ´¾é€','ç›®çš„åœ°Â·æœ«ç«¯æ´¾é€',NULL,'2026-09-04 03:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200008,'OMT-SEED-0001','DELIVERED','EXP-0080','åŒ…è£¹å·²ç­¾æ”¶','ç›®çš„åœ°',NULL,'2026-09-04 21:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200009,'OMT-SEED-0002','CREATED','EXP-0010','åŒ…è£¹å·²ä¸‹å•ï¼Œç­‰å¾…æ½æ”¶','æ·±åœ³ä»“',NULL,'2026-08-29 09:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200010,'OMT-SEED-0002','WAREHOUSE_OUT','EXP-0020','åŒ…è£¹å·²ä»Žä»“åº“å‡ºåº“','æ·±åœ³ä»“',NULL,'2026-08-29 16:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200011,'OMT-SEED-0002','IN_TRANSIT','EXP-0050','åŒ…è£¹æ­£åœ¨å¹²çº¿è¿è¾“é€”ä¸­','å¹²çº¿è¿è¾“ä¸­',NULL,'2026-08-31 09:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200012,'OMT-SEED-0002','IMPORT_CUSTOMS','EXP-0060','åŒ…è£¹å·²åˆ°è¾¾ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒï¼Œæ­£åœ¨åˆ†æ‹£','ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒ',NULL,'2026-09-02 21:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200013,'OMT-SEED-0002','DELIVERED','EXP-0080','åŒ…è£¹å·²ç­¾æ”¶','ç›®çš„åœ°',NULL,'2026-09-04 13:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200014,'OMT-SEED-0019','CREATED','EXP-0010','åŒ…è£¹å·²ä¸‹å•ï¼Œç­‰å¾…æ½æ”¶','æ·±åœ³ä»“',NULL,'2026-08-28 11:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200015,'OMT-SEED-0019','EXPORT_CUSTOMS','EXP-0040','åŒ…è£¹å·²åˆ°è¾¾ä¸­è½¬åˆ†æ‹¨ä¸­å¿ƒï¼Œç­‰å¾…å‘è¿','å¹¿å·žåˆ†æ‹¨ä¸­å¿ƒ',NULL,'2026-08-30 03:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200016,'OMT-SEED-0019','IMPORT_CUSTOMS','EXP-0060','åŒ…è£¹å·²åˆ°è¾¾ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒï¼Œæ­£åœ¨åˆ†æ‹£','ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒ',NULL,'2026-09-01 15:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200017,'OMT-SEED-0019','DELIVERED','EXP-0080','åŒ…è£¹å·²ç­¾æ”¶','ç›®çš„åœ°',NULL,'2026-09-03 17:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200018,'OMT-SEED-0004','CREATED','EXP-0010','åŒ…è£¹å·²ä¸‹å•ï¼Œç­‰å¾…æ½æ”¶','æ·±åœ³ä»“',NULL,'2026-09-01 09:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200019,'OMT-SEED-0004','IMPORT_CUSTOMS','EXP-0060','åŒ…è£¹å·²åˆ°è¾¾ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒï¼Œæ­£åœ¨åˆ†æ‹£','ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒ',NULL,'2026-09-04 03:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200020,'OMT-SEED-0004','LAST_MILE','EXP-0070','åŒ…è£¹å·²è¿›å…¥æœ«ç«¯æ´¾é€','ç›®çš„åœ°Â·æœ«ç«¯æ´¾é€',NULL,'2026-09-05 03:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200021,'OMT-SEED-0005','CREATED','EXP-0010','åŒ…è£¹å·²ä¸‹å•ï¼Œç­‰å¾…æ½æ”¶','æ·±åœ³ä»“',NULL,'2026-08-31 09:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200022,'OMT-SEED-0005','IMPORT_CUSTOMS','EXP-0060','åŒ…è£¹å·²åˆ°è¾¾ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒï¼Œæ­£åœ¨åˆ†æ‹£','ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒ',NULL,'2026-09-02 21:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200023,'OMT-SEED-0008','CREATED','EXP-0010','åŒ…è£¹å·²ä¸‹å•ï¼Œç­‰å¾…æ½æ”¶','æ·±åœ³ä»“',NULL,'2026-09-02 09:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200024,'OMT-SEED-0008','IN_TRANSIT','EXP-0050','åŒ…è£¹æ­£åœ¨å¹²çº¿è¿è¾“é€”ä¸­','å¹²çº¿è¿è¾“ä¸­',NULL,'2026-09-04 21:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200025,'OMT-TMS-SEED-0001','WAREHOUSE_OUT','EXP-0020','åŒ…è£¹å·²ä»Žä»“åº“å‡ºåº“','æ·±åœ³ä»“','AIRGO','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200026,'OMT-TMS-SEED-0001','DELIVERED','EXP-0080','åŒ…è£¹å·²ç­¾æ”¶','ç›®çš„åœ°','AIRGO','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200027,'OMT-TMS-SEED-0002','WAREHOUSE_OUT','EXP-0020','åŒ…è£¹å·²ä»Žä»“åº“å‡ºåº“','æ·±åœ³ä»“','AIRGO','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200028,'OMT-TMS-SEED-0002','IN_TRANSIT','EXP-0050','åŒ…è£¹æ­£åœ¨å¹²çº¿è¿è¾“é€”ä¸­','å¹²çº¿è¿è¾“ä¸­','AIRGO','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200029,'OMT-TMS-SEED-0003','WAREHOUSE_OUT','EXP-0020','åŒ…è£¹å·²ä»Žä»“åº“å‡ºåº“','æ·±åœ³ä»“','AIRGO','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200030,'OMT-TMS-SEED-0004','IMPORT_CUSTOMS','EXP-0060','åŒ…è£¹å·²åˆ°è¾¾ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒï¼Œæ­£åœ¨åˆ†æ‹£','ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒ','CARGOWAY','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200031,'OMT-TMS-SEED-0005','CUSTOMS_DELAY','CUS-1102','åŒ…è£¹åœ¨ä¸­è½¬çŽ¯èŠ‚å»¶è¯¯ï¼Œæ­£åœ¨å¤„ç†','ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒ','CARGOWAY','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200032,'OMT-TMS-SEED-0006','LAST_MILE','EXP-0070','åŒ…è£¹å·²è¿›å…¥æœ«ç«¯æ´¾é€','ç›®çš„åœ°Â·æœ«ç«¯æ´¾é€','AIRGO','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200033,'OMT-TMS-SEED-0007','DELIVERED','EXP-0080','åŒ…è£¹å·²ç­¾æ”¶','ç›®çš„åœ°','AIRGO','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200034,'OMT-TMS-SEED-0008','LOST','EXP-0051','åŒ…è£¹åœ¨è¿è¾“é€”ä¸­ä¸¢å¤±ï¼Œæ­£åœ¨æ ¸æŸ¥','å¹²çº¿è¿è¾“ä¸­','AIRGO','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200035,'OMT-TMS-SEED-0009','IN_TRANSIT','EXP-0050','åŒ…è£¹æ­£åœ¨å¹²çº¿è¿è¾“é€”ä¸­','å¹²çº¿è¿è¾“ä¸­','SEAGO','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200036,'OMT-TMS-SEED-0010','DELIVERY_FAILED','EXP-0071','æ´¾é€å¤±è´¥ï¼Œå°†é‡æ–°æ´¾é€','ç›®çš„åœ°Â·æœ«ç«¯æ´¾é€','CARGOWAY','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0);
/*!40000 ALTER TABLE `logistics_track` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `merchant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `merchant` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `merchant_code` varchar(32) NOT NULL COMMENT 'å•†å®¶ç¼–ç ',
  `merchant_name` varchar(128) NOT NULL COMMENT 'å•†å®¶åç§°',
  `user_id` bigint(20) DEFAULT NULL COMMENT 'å…³è”sentinel_user.id(ç™»å½•è´¦å·)',
  `contact_name` varchar(64) DEFAULT NULL COMMENT 'è”ç³»äºº',
  `contact_phone` varchar(32) DEFAULT NULL COMMENT 'è”ç³»ç”µè¯',
  `contact_email` varchar(128) DEFAULT NULL COMMENT 'è”ç³»é‚®ç®±',
  `country` varchar(32) DEFAULT 'CN' COMMENT 'æ‰€åœ¨å›½å®¶',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€ï¼š1å¯ç”¨ 0åœç”¨',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_code` (`merchant_code`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='å•†å®¶(å–å®¶)ä¸»æ•°æ®';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `merchant` WRITE;
/*!40000 ALTER TABLE `merchant` DISABLE KEYS */;
INSERT INTO `merchant` VALUES (1,'MCH-0001','æ·±åœ³è“é²¸ç§‘æŠ€',4,'çŽ‹æµ·','13800138001','wh@lanjing.cn','CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(2,'MCH-0002','ä¹‰ä¹Œç™¾çµè´¸æ˜“',NULL,'æŽæ…§','13800138002','lihui@bailing.cn','CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(3,'MCH-0003','å¹¿å·žå¯èˆªç”µå­',NULL,'é™ˆèˆª','13800138003','chenhang@qihang.cn','CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(4,'MCH-0004','æ­å·žäº‘å›¾ç§‘æŠ€',NULL,'èµµç³','13800138004','zhaolin@yuntu.cn','CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0);
/*!40000 ALTER TABLE `merchant` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `notification_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notification_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `order_no` varchar(64) NOT NULL COMMENT 'è®¢å•å·',
  `node` varchar(32) NOT NULL COMMENT 'è§¦å‘èŠ‚ç‚¹',
  `role` varchar(16) NOT NULL COMMENT 'æŽ¥æ”¶è§’è‰²ï¼šbuyer/merchant/customer_service',
  `channel` varchar(16) NOT NULL COMMENT 'æ¸ é“ï¼špush/sms/email/feishu',
  `content` text COMMENT 'é€šçŸ¥å†…å®¹',
  `language` varchar(10) DEFAULT 'zh' COMMENT 'è¯­è¨€',
  `status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'çŠ¶æ€ï¼šPENDING/SENT/FAILED',
  `trace_id` varchar(64) DEFAULT NULL COMMENT 'é“¾è·¯ID',
  `agent_call_log_id` bigint(20) DEFAULT NULL COMMENT 'å…³è” Agent è°ƒç”¨æ—¥å¿—',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=300018 DEFAULT CHARSET=utf8mb4 COMMENT='é€šçŸ¥è®°å½•è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `notification_record` WRITE;
/*!40000 ALTER TABLE `notification_record` DISABLE KEYS */;
INSERT INTO `notification_record` VALUES (300001,'OMT-SEED-0001','IMPORT_CUSTOMS','buyer','sms','æ‚¨çš„åŒ…è£¹å·²åˆ°è¾¾ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒï¼Œæ­£åœ¨åˆ†æ‹£ã€‚','zh','SENT','SEED-0001',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300002,'OMT-SEED-0001','DELIVERED','buyer','push','æ‚¨çš„åŒ…è£¹å·²ç­¾æ”¶ï¼Œæ„Ÿè°¢æ‚¨çš„ä¿¡ä»»ä¸Žæ”¯æŒï¼','zh','SENT','SEED-0001',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300003,'OMT-SEED-0002','IMPORT_CUSTOMS','buyer','sms','æ‚¨çš„åŒ…è£¹å·²åˆ°è¾¾ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒï¼Œæ­£åœ¨åˆ†æ‹£ã€‚','zh','SENT','SEED-0002',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300004,'OMT-SEED-0019','IMPORT_CUSTOMS','buyer','push','æ‚¨çš„åŒ…è£¹å·²åˆ°è¾¾ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒï¼Œæ­£åœ¨åˆ†æ‹£ã€‚','zh','SENT','SEED-0019',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300005,'OMT-SEED-0019','DELIVERED','buyer','email','æ‚¨çš„åŒ…è£¹å·²ç­¾æ”¶ï¼Œæ„Ÿè°¢æ‚¨çš„ä¿¡ä»»ä¸Žæ”¯æŒï¼','zh','SENT','SEED-0019',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300006,'OMT-SEED-0004','LAST_MILE','buyer','push','æ‚¨çš„åŒ…è£¹å·²è¿›å…¥æœ«ç«¯æ´¾é€ï¼Œå¿«é€’å‘˜å°†å¾ˆå¿«ä¸Žæ‚¨è”ç³»ã€‚','zh','SENT','SEED-0004',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300007,'OMT-SEED-0005','IMPORT_CUSTOMS','buyer','sms','æ‚¨çš„åŒ…è£¹å·²åˆ°è¾¾ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒï¼Œæ­£åœ¨åˆ†æ‹£ã€‚','zh','PENDING','SEED-0005',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300008,'OMT-SEED-0007','CUSTOMS_DELAY','buyer','sms','æ‚¨çš„åŒ…è£¹åœ¨ä¸­è½¬çŽ¯èŠ‚å»¶è¯¯ï¼Œæ­£åœ¨å¤„ç†ï¼Œè¯·è€å¿ƒç­‰å¾…ã€‚','zh','SENT','SEED-0007',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300009,'OMT-SEED-0007','CUSTOMS_DELAY','merchant','feishu','æ‚¨çš„åŒ…è£¹åœ¨ä¸­è½¬çŽ¯èŠ‚å»¶è¯¯ï¼Œæ­£åœ¨å¤„ç†ï¼Œè¯·è€å¿ƒç­‰å¾…ã€‚','zh','SENT','SEED-0007',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300010,'OMT-SEED-0010','DELIVERY_FAILED','buyer','push','æ‚¨çš„åŒ…è£¹æ´¾é€å¤±è´¥ï¼Œå°†é‡æ–°æ´¾é€ï¼Œè¯·ä¿æŒç”µè¯ç•…é€šã€‚','zh','SENT','SEED-0010',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300011,'OMT-SEED-0015','LOST','buyer','sms','æ‚¨çš„åŒ…è£¹åœ¨è¿è¾“é€”ä¸­ä¸¢å¤±ï¼Œæ­£åœ¨æ ¸æŸ¥å¤„ç†ï¼Œè¯·è€å¿ƒç­‰å¾…ã€‚','zh','SENT','SEED-0015',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300012,'OMT-SEED-0016','RETURNED','merchant','feishu','æ‚¨çš„åŒ…è£¹å·²é€€å›žå‘ä»¶ä»“ã€‚','zh','SENT','SEED-0016',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300013,'OMT-SEED-0022','IMPORT_CUSTOMS','buyer','sms','æ‚¨çš„åŒ…è£¹å·²åˆ°è¾¾ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒï¼Œæ­£åœ¨åˆ†æ‹£ã€‚','zh','SENT','SEED-0022',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300014,'OMT-SEED-0025','IMPORT_CUSTOMS','buyer','push','æ‚¨çš„åŒ…è£¹å·²åˆ°è¾¾ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒï¼Œæ­£åœ¨åˆ†æ‹£ã€‚','zh','SENT','SEED-0025',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300015,'OMT-SEED-0028','IMPORT_CUSTOMS','buyer','sms','æ‚¨çš„åŒ…è£¹å·²åˆ°è¾¾ç›®çš„åœ°åˆ†æ‹¨ä¸­å¿ƒï¼Œæ­£åœ¨åˆ†æ‹£ã€‚','zh','SENT','SEED-0028',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300016,'OMT-SEED-0003','DELIVERED','buyer','push','æ‚¨çš„åŒ…è£¹å·²ç­¾æ”¶ï¼Œæ„Ÿè°¢æ‚¨çš„ä¿¡ä»»ä¸Žæ”¯æŒï¼','zh','SENT','SEED-0003',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300017,'OMT-TMS-SEED-0010','DELIVERY_FAILED','buyer','push','物流派送未成功，订单OMT-TMS-SEED-0010的商品已暂存网点，稍后将重新安排配送，请留意后续通知。','zh','PENDING','OMT-TMS-SEED-0010|DELIVERY_FAILED|zh|buyer',NULL,'2026-09-05 09:04:19','2026-09-05 09:04:19',0);
/*!40000 ALTER TABLE `notification_record` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `operation_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `operator` varchar(64) NOT NULL COMMENT 'æ“ä½œäºº(username/ç”¨æˆ·å)',
  `operator_role` varchar(32) DEFAULT NULL COMMENT 'æ“ä½œäººè§’è‰²',
  `module` varchar(32) NOT NULL COMMENT 'ä¸šåŠ¡æ¨¡å—:order/waybill/workorder/bill/sla/notify',
  `action` varchar(64) NOT NULL COMMENT 'åŠ¨ä½œ:CREATE/GENERATE/ADVANCE/ANOMALY/CLAIM/SUBMIT/VERIFY/SETTLE/REJECT/DIAGNOSE/NOTIFY',
  `target_no` varchar(64) DEFAULT NULL COMMENT 'ä¸šåŠ¡å¯¹è±¡å•å·(è®¢å•/è¿å•/è´¦å•å·)',
  `detail` varchar(512) DEFAULT NULL COMMENT 'æ“ä½œè¯¦æƒ…(JSONæˆ–æè¿°)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'æ“ä½œæ—¶é—´',
  PRIMARY KEY (`id`),
  KEY `idx_module` (`module`),
  KEY `idx_target` (`target_no`),
  KEY `idx_operator` (`operator`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='æ“ä½œå®¡è®¡æ—¥å¿—';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `operation_log` WRITE;
/*!40000 ALTER TABLE `operation_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `operation_log` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `merchant_id` bigint(20) NOT NULL COMMENT 'å•†å®¶ID',
  `sku` varchar(64) NOT NULL COMMENT 'SKUç¼–ç ',
  `name` varchar(256) NOT NULL COMMENT 'å•†å“åç§°',
  `hs_code` varchar(32) DEFAULT NULL COMMENT 'æµ·å…³HSç¼–ç ',
  `declared_value` decimal(12,2) DEFAULT '0.00' COMMENT 'å•ä»¶ç”³æŠ¥ä»·å€¼',
  `currency` varchar(8) DEFAULT 'CNY' COMMENT 'ç”³æŠ¥å¸ç§',
  `weight_kg` decimal(10,3) NOT NULL COMMENT 'å•ä»¶å®žé‡kg',
  `volume_l` decimal(10,3) DEFAULT '0.000' COMMENT 'å•ä»¶ä½“ç§¯L',
  `origin_country` varchar(8) DEFAULT 'CN' COMMENT 'åŽŸäº§å›½',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€ï¼š1å¯ç”¨ 0åœç”¨',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_sku` (`merchant_id`,`sku`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COMMENT='å•†å“SKU(HSç¼–ç /ç”³æŠ¥/é‡é‡)';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `product` WRITE;
/*!40000 ALTER TABLE `product` DISABLE KEYS */;
INSERT INTO `product` VALUES (1,1,'SKU-LJ-001','æ™ºèƒ½æ‰‹è¡¨','9102.12',320.00,'CNY',0.450,0.800,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(2,1,'SKU-LJ-002','è“ç‰™è€³æœº','8518.30',180.00,'CNY',0.280,0.500,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(3,1,'SKU-LJ-003','USBæ•°æ®çº¿3æ¡è£…','8544.42',45.00,'CNY',0.150,0.300,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(4,2,'SKU-BL-001','é’ˆç»‡æ¯›è¡£','6110.30',120.00,'CNY',0.600,2.200,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(5,2,'SKU-BL-002','å›´å·¾','6117.10',60.00,'CNY',0.300,1.100,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(6,2,'SKU-BL-003','æ”¶çº³ç®±','3924.90',85.00,'CNY',1.200,6.000,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(7,3,'SKU-QH-001','LEDå°ç¯','9405.40',150.00,'CNY',0.800,3.500,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(8,3,'SKU-QH-002','æ— çº¿å……ç”µå™¨','8504.40',210.00,'CNY',0.350,1.200,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(9,4,'SKU-YT-001','ä¾¿æºå¸å°˜å™¨','8508.11',420.00,'CNY',1.800,8.500,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(10,4,'SKU-YT-002','æ™ºèƒ½é—¨é”','8301.70',680.00,'CNY',3.200,9.000,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0);
/*!40000 ALTER TABLE `product` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `risk_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `risk_rule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `scene` varchar(32) NOT NULL COMMENT 'SLA/ANOMALY',
  `trigger_status` varchar(32) DEFAULT NULL COMMENT 'RISK/BREACHED',
  `action` varchar(64) NOT NULL COMMENT 'NOTIFYé€šçŸ¥/CREATE_WORKORDERå»ºå·¥å•',
  `action_config` varchar(255) DEFAULT NULL,
  `enabled` tinyint(4) DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(4) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='é£Žé™©é¢„è­¦è‡ªåŠ¨å¤„ç½®è§„åˆ™';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `risk_rule` WRITE;
/*!40000 ALTER TABLE `risk_rule` DISABLE KEYS */;
INSERT INTO `risk_rule` VALUES (1,'SLA è¿çº¦è‡ªåŠ¨å»ºå·¥å•','SLA','BREACHED','CREATE_WORKORDER','{\"level\":\"HIGH\"}',1,'2026-09-05 09:03:13','2026-09-05 09:03:13',0),(2,'SLA é¢„è­¦è‡ªåŠ¨é€šçŸ¥','SLA','RISK','NOTIFY','{\"channel\":\"email\"}',1,'2026-09-05 09:03:13','2026-09-05 09:03:13',0);
/*!40000 ALTER TABLE `risk_rule` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `warehouse`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `warehouse` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `warehouse_code` varchar(32) NOT NULL COMMENT 'ä»“åº“ç¼–ç ',
  `warehouse_name` varchar(128) NOT NULL COMMENT 'ä»“åº“åç§°',
  `country` varchar(8) DEFAULT 'CN' COMMENT 'æ‰€åœ¨å›½å®¶',
  `city` varchar(64) DEFAULT NULL COMMENT 'åŸŽå¸‚',
  `address` varchar(256) DEFAULT NULL COMMENT 'åœ°å€',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€ï¼š1å¯ç”¨ 0åœç”¨',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_warehouse_code` (`warehouse_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='å‘è´§ä»“åº“';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `warehouse` WRITE;
/*!40000 ALTER TABLE `warehouse` DISABLE KEYS */;
INSERT INTO `warehouse` VALUES (1,'WH-SZ','æ·±åœ³ä»“','CN','æ·±åœ³','å®å®‰åŒºç¦æ°¸è¡—é“å›½é™…ç‰©æµå›­AåŒº',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(2,'WH-SH','ä¸Šæµ·ä»“','CN','ä¸Šæµ·','é’æµ¦åŒºåŽæ–°é•‡ä»“å‚¨åŸºåœ°Bæ ‹',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0);
/*!40000 ALTER TABLE `warehouse` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `waybill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `waybill` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `waybill_no` varchar(64) NOT NULL COMMENT 'è¿å•å·',
  `order_no` varchar(64) NOT NULL COMMENT 'è®¢å•å·',
  `merchant_id` bigint(20) DEFAULT NULL COMMENT 'å•†å®¶ID',
  `channel_id` bigint(20) NOT NULL COMMENT 'æ¸ é“ID',
  `carrier_id` bigint(20) DEFAULT NULL COMMENT 'æ‰¿è¿å•†ID',
  `tracking_no` varchar(64) NOT NULL COMMENT 'è·Ÿè¸ªå·',
  `carrier_code` varchar(32) DEFAULT NULL COMMENT 'æ‰¿è¿å•†ç¼–ç ',
  `weight_kg` decimal(10,3) DEFAULT NULL COMMENT 'å®žé‡kg',
  `volume_l` decimal(10,3) DEFAULT NULL COMMENT 'ä½“ç§¯L',
  `billable_weight_kg` decimal(10,3) DEFAULT NULL COMMENT 'è®¡è´¹é‡kg',
  `declared_value` decimal(12,2) DEFAULT '0.00' COMMENT 'ç”³æŠ¥ä»·å€¼',
  `declared_currency` varchar(8) DEFAULT 'CNY' COMMENT 'ç”³æŠ¥å¸ç§',
  `freight_cost` decimal(12,2) NOT NULL COMMENT 'å‡ºåº“æ—¶è¿è´¹å¿«ç…§',
  `freight_currency` varchar(8) DEFAULT 'CNY' COMMENT 'è¿è´¹å¸ç§',
  `zone` varchar(16) DEFAULT NULL COMMENT 'è®¡è´¹åŒºåŸŸ',
  `promise_eta` datetime DEFAULT NULL COMMENT 'æ‰¿è¯ºå¦¥æŠ•ETA',
  `actual_delivered_at` datetime DEFAULT NULL COMMENT 'å®žé™…å¦¥æŠ•æ—¶é—´',
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'çŠ¶æ€:ACTIVE/DELIVERED/CANCELED',
  `billed` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'æ˜¯å¦å·²å…¥è´¦å•',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  `items_json` text COMMENT 'è¿å•å•†å“æ˜Žç»†ï¼ˆåˆ†æ‰¹å‡ºåº“å­é›†ï¼‰',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_waybill_no` (`waybill_no`),
  UNIQUE KEY `uk_tracking_no` (`tracking_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_channel_id` (`channel_id`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COMMENT='è¿å•(å‡ºåº“ç”Ÿæˆ,è¿è´¹å¿«ç…§)';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `waybill` WRITE;
/*!40000 ALTER TABLE `waybill` DISABLE KEYS */;
INSERT INTO `waybill` VALUES (1,'WB-ZJ-000001','OMT-TMS-SEED-0001',1,3,2,'YU-SEED-0001','AIRGO',0.560,1.000,0.560,360.00,'CNY',53.20,'CNY','ZJ','2026-08-27 09:03:15',NULL,'DELIVERED',1,'2026-08-26 09:03:15','2026-09-05 09:03:18',0,NULL),(2,'WB-ZJ-000002','OMT-TMS-SEED-0002',1,3,2,'YU-SEED-0002','AIRGO',0.900,1.600,0.900,640.00,'CNY',85.50,'CNY','ZJ','2026-09-09 09:03:15',NULL,'ACTIVE',1,'2026-09-01 09:03:15','2026-09-05 09:03:18',0,NULL),(3,'WB-GD-000003','OMT-TMS-SEED-0003',1,2,2,'CY-SEED-0003','AIRGO',0.450,0.800,0.450,320.00,'CNY',40.50,'CNY','GD','2026-09-13 09:03:15',NULL,'ACTIVE',0,'2026-09-04 09:03:15','2026-09-05 09:03:18',0,NULL),(4,'WB-RR-SEED-0004','OMT-TMS-SEED-0004',2,1,1,'RR-SEED-0004','CARGOWAY',0.600,2.200,0.600,120.00,'CNY',65.00,'CNY','GD','2026-09-15 09:03:15',NULL,'ACTIVE',1,'2026-08-28 09:03:15','2026-09-05 09:03:18',0,NULL),(5,'WB-RR-SEED-0005','OMT-TMS-SEED-0005',2,1,1,'RR-SEED-0005','CARGOWAY',0.600,2.200,0.600,120.00,'CNY',65.00,'CNY','GD','2026-09-03 09:03:15',NULL,'ACTIVE',1,'2026-08-16 09:03:15','2026-09-05 09:03:18',0,NULL),(6,'WB-SC-000006','OMT-TMS-SEED-0006',3,4,2,'BZ-SEED-0006','AIRGO',0.800,3.500,0.800,150.00,'CNY',80.00,'CNY','SC','2026-09-08 09:03:15',NULL,'ACTIVE',0,'2026-08-25 09:03:15','2026-09-05 09:03:18',0,NULL),(7,'WB-JS-000007','OMT-TMS-SEED-0007',3,5,2,'DG-SEED-0007','AIRGO',0.350,1.200,0.350,210.00,'CNY',32.20,'CNY','JS','2026-09-02 09:03:15',NULL,'DELIVERED',1,'2026-08-29 09:03:15','2026-09-05 09:03:18',0,NULL),(8,'WB-ZJ-000008','OMT-TMS-SEED-0008',4,3,2,'YU-SEED-0008','AIRGO',1.800,8.500,1.800,420.00,'CNY',171.00,'CNY','ZJ','2026-08-27 09:03:15',NULL,'ACTIVE',1,'2026-08-21 09:03:15','2026-09-05 09:03:18',0,NULL),(9,'WB-SE-SEED-0009','OMT-TMS-SEED-0009',4,6,3,'SE-SEED-0009','SEAGO',3.200,9.000,3.200,680.00,'CNY',54.00,'CNY','GD','2026-10-05 09:03:15',NULL,'ACTIVE',1,'2026-08-31 09:03:15','2026-09-05 09:03:18',0,NULL),(10,'WB-RR-SEED-0010','OMT-TMS-SEED-0010',2,1,1,'RR-SEED-0010','CARGOWAY',1.200,4.400,1.200,240.00,'CNY',65.00,'CNY','GD','2026-09-04 09:03:15',NULL,'ACTIVE',1,'2026-08-24 09:03:15','2026-09-05 09:03:18',0,NULL);
/*!40000 ALTER TABLE `waybill` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `workorder`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `workorder` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `order_no` varchar(64) NOT NULL COMMENT 'è®¢å•å·',
  `type` varchar(32) NOT NULL COMMENT 'å¼‚å¸¸ç±»åž‹ï¼šcustoms_delay/lost/returned/delivery_failed/sla_breach',
  `level` varchar(8) NOT NULL DEFAULT 'P1' COMMENT 'çº§åˆ«ï¼šP0/P1/P2',
  `description` text COMMENT 'æè¿°',
  `agent_diagnosis` text COMMENT 'Agent è¯Šæ–­ç»“æžœï¼ˆJSONï¼‰',
  `sop` text COMMENT 'å¤„ç† SOP',
  `status` varchar(16) NOT NULL DEFAULT 'OPEN' COMMENT 'çŠ¶æ€ï¼šOPEN/PROCESSING/RESOLVED/CLOSED/PUSHED',
  `liability` varchar(16) DEFAULT NULL COMMENT 'è´£ä»»æ–¹:merchant/carrier/platform',
  `claim_amount` decimal(12,2) DEFAULT '0.00' COMMENT 'ç´¢èµ”é‡‘é¢',
  `compensation_amount` decimal(12,2) DEFAULT '0.00' COMMENT 'ç†èµ”é‡‘é¢',
  `sla_breach` tinyint(4) DEFAULT '0' COMMENT 'æ˜¯å¦SLAè¿çº¦',
  `currency` varchar(8) DEFAULT 'CNY' COMMENT 'é‡‘é¢å¸ç§',
  `resolution` varchar(512) DEFAULT NULL COMMENT 'å¤„ç†ç»“æžœ',
  `resolved_at` datetime DEFAULT NULL COMMENT 'è§£å†³æ—¶é—´',
  `claim_status` varchar(16) NOT NULL DEFAULT 'NONE' COMMENT 'ç†èµ”çŠ¶æ€:NONE/SUBMITTED/APPROVED/PAID/REJECTED',
  `claim_submitted_at` datetime DEFAULT NULL COMMENT 'ç´¢èµ”æäº¤æ—¶é—´',
  `claim_approved_at` datetime DEFAULT NULL COMMENT 'ç†èµ”å®¡æ‰¹æ—¶é—´',
  `claim_paid_at` datetime DEFAULT NULL COMMENT 'èµ”ä»˜æ—¶é—´',
  `claim_reject_reason` varchar(255) DEFAULT NULL COMMENT 'é©³å›žåŽŸå› ',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_status` (`status`),
  KEY `idx_level` (`level`),
  KEY `idx_claim_status` (`claim_status`)
) ENGINE=InnoDB AUTO_INCREMENT=400012 DEFAULT CHARSET=utf8mb4 COMMENT='å¼‚å¸¸å·¥å•è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `workorder` WRITE;
/*!40000 ALTER TABLE `workorder` DISABLE KEYS */;
INSERT INTO `workorder` VALUES (400001,'OMT-SEED-0007','customs_delay','P1','è®¢å•åœ¨ç‰©æµçŽ¯èŠ‚å‘ç”Ÿä¸­è½¬å»¶è¯¯å¼‚å¸¸ï¼ˆèŠ‚ç‚¹ CUSTOMS_DELAYï¼‰','{\"type\":\"customs_delay\",\"level\":\"P1\",\"sop\":\"1.è”ç³»ç‰©æµå•†æ ¸å®ž 2.é€šçŸ¥ä¹°å®¶é¢„è®¡å»¶è¯¯æ—¶é—´\",\"reason\":\"æµ·å…³æŠ½æ£€ï¼ŒåŒ…è£¹åœ¨ç›®çš„å›½æµ·å…³åœç•™\"}','1.è”ç³»ç‰©æµå•†æ ¸å®ž 2.é€šçŸ¥ä¹°å®¶é¢„è®¡å»¶è¯¯æ—¶é—´ 3.è¶…è¿‡72hå»ºè®®è¡¥å‘å®‰æŠšé€šçŸ¥','OPEN','carrier',0.00,0.00,1,'CNY',NULL,NULL,'NONE',NULL,NULL,NULL,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(400002,'OMT-SEED-0010','delivery_failed','P2','è®¢å•åœ¨ç‰©æµçŽ¯èŠ‚å‘ç”Ÿæ´¾é€å¤±è´¥å¼‚å¸¸ï¼ˆèŠ‚ç‚¹ DELIVERY_FAILEDï¼‰','{\"type\":\"delivery_failed\",\"level\":\"P2\",\"sop\":\"1.è”ç³»ä¹°å®¶é¢„çº¦æ´¾é€æ—¶é—´ 2.é‡æ–°å®‰æŽ’æ´¾é€\",\"reason\":\"ä¹°å®¶ä¸åœ¨å®¶ï¼Œæ´¾é€å¤±è´¥\"}','1.è”ç³»ä¹°å®¶é¢„çº¦æ´¾é€æ—¶é—´ 2.é‡æ–°å®‰æŽ’æ´¾é€','PROCESSING','merchant',120.00,0.00,0,'CNY','å•†å®¶æä¾›é”™è¯¯åœ°å€ï¼Œæ”¹æ´¾ä¸­',NULL,'SUBMITTED','2026-09-05 09:03:15',NULL,NULL,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:16',0),(400003,'OMT-SEED-0015','lost','P0','è®¢å•åœ¨ç‰©æµçŽ¯èŠ‚å‘ç”Ÿä¸¢ä»¶å¼‚å¸¸ï¼ˆèŠ‚ç‚¹ LOSTï¼‰','{\"type\":\"lost\",\"level\":\"P0\",\"sop\":\"1.è”ç³»ç‰©æµå•†æ ¸å®ž 2.åˆ›å»ºä¸¢ä»¶å·¥å• 3.å»ºè®®é€€æ¬¾\",\"reason\":\"å›½é™…è¿è¾“é€”ä¸­ä¸¢ä»¶\"}','1.è”ç³»ç‰©æµå•†æ ¸å®ž 2.åˆ›å»ºä¸¢ä»¶å·¥å• 3.å»ºè®®é€€æ¬¾','OPEN','carrier',420.00,420.00,1,'CNY','ä¸¢å¤±ç†èµ”å·²èµ”ä»˜','2026-09-05 09:03:15','SUBMITTED','2026-09-05 09:03:15',NULL,NULL,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:16',0),(400004,'OMT-SEED-0016','returned','P1','è®¢å•åœ¨ç‰©æµçŽ¯èŠ‚å‘ç”Ÿé€€å›žå¼‚å¸¸ï¼ˆèŠ‚ç‚¹ RETURNEDï¼‰','{\"type\":\"returned\",\"level\":\"P1\",\"sop\":\"1.æ ¸å®žé€€å›žåŽŸå›  2.é€šçŸ¥ä¹°å®¶ 3.è·Ÿè¿›é€€æ¬¾\",\"reason\":\"åŒ…è£¹é€€å›žå‘ä»¶åœ°\"}','1.æ ¸å®žé€€å›žåŽŸå›  2.é€šçŸ¥ä¹°å®¶ 3.è·Ÿè¿›é€€æ¬¾','CLOSED','platform',150.00,50.00,1,'CNY','ä¸­è½¬å»¶è¯¯å¹³å°è¡¥å¿',NULL,'SUBMITTED','2026-09-05 09:03:15',NULL,NULL,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(400005,'OMT-TMS-SEED-0005','sla_breach','P1','SLA è¿çº¦ï¼šä¸­è½¬æ»žç•™è¶…è¿‡æ‰¿è¯ºæ—¶æ•ˆ',NULL,NULL,'OPEN',NULL,0.00,0.00,1,'CNY',NULL,NULL,'NONE',NULL,NULL,NULL,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(400006,'OMT-TMS-SEED-0002','lost','P0','æ•´ä»¶åŒ…è£¹åœ¨å¹²çº¿è¿è¾“ä¸­ä¸¢å¤±ï¼Œä¹°å®¶è¦æ±‚æŒ‰è´§å€¼å…¨é¢ç´¢èµ”',NULL,NULL,'PROCESSING','carrier',640.00,0.00,0,'CNY',NULL,NULL,'SUBMITTED','2026-09-04 09:03:16',NULL,NULL,NULL,'2026-09-05 09:03:16','2026-09-05 09:03:16',0),(400007,'OMT-TMS-SEED-0008','lost','P1','è¿å•åœ¨è½¬è¿åŽç‰©æµè®°å½•ä¸­æ–­ï¼Œç–‘ä¼¼ä¸¢å¤±ï¼Œè¿›å…¥è´£ä»»è®¤å®š',NULL,NULL,'PROCESSING','carrier',420.00,0.00,0,'CNY',NULL,NULL,'SUBMITTED','2026-09-05 03:03:16',NULL,NULL,NULL,'2026-09-05 09:03:16','2026-09-05 09:03:16',0),(400008,'OMT-TMS-SEED-0004','customs_delay','P1','ä¸­è½¬å»¶è¯¯è¶…æ‰¿è¯º 3 å¤©ï¼Œä¹°å®¶ç´¢èµ”æ—¶æ•ˆæŸå¤±',NULL,NULL,'RESOLVED','carrier',200.00,120.00,0,'CNY','æ‰¿è¿æ—¶æ•ˆå»¶è¯¯ï¼Œæ ¸å®šèµ”ä»˜è´§å€¼ 60%',NULL,'APPROVED','2026-09-01 09:03:16','2026-09-03 09:03:16',NULL,NULL,'2026-09-05 09:03:16','2026-09-05 09:03:16',0),(400009,'OMT-TMS-SEED-0005','customs_delay','P2','å»¶è¿Ÿç³»å•†å®¶æ™šå‘è´§å¯¼è‡´ï¼Œéžæ‰¿è¿è¿è¾“æ—¶æ•ˆé—®é¢˜ï¼Œé©³å›žç´¢èµ”',NULL,NULL,'RESOLVED','merchant',200.00,0.00,0,'CNY',NULL,NULL,'REJECTED','2026-08-31 09:03:16','2026-09-01 09:03:16',NULL,'å‘è´§å»¶è¿Ÿå±žå•†å®¶è´£ä»»ï¼Œé©³å›žå¯¹æ‰¿è¿å•†çš„ç†èµ”','2026-09-05 09:03:16','2026-09-05 09:03:16',0),(400010,'OMT-TMS-SEED-0010','delivery_failed','P1','订单在物流环节发生delivery_failed异常（节点 DELIVERY_FAILED）','{\"level\":\"P1\",\"sop\":\"1. 立即联系承运商核实失败原因（如收件人拒收、地址错误、电话不通、无人签收等）；2. 核对订单收件信息准确性，确认是否需更新或补发；3. 若属可重派场景，48小时内安排重新配送；4. 若属拒收或客户主动取消，触发退货流程并同步ERP系统；5. 向客户发送致歉及解决方案短信/站内信；6. 工单闭环前需上传承运商反馈截图及处理结果说明。\",\"type\":\"delivery_failed\"}','1. 立即联系承运商核实失败原因（如收件人拒收、地址错误、电话不通、无人签收等）；2. 核对订单收件信息准确性，确认是否需更新或补发；3. 若属可重派场景，48小时内安排重新配送；4. 若属拒收或客户主动取消，触发退货流程并同步ERP系统；5. 向客户发送致歉及解决方案短信/站内信；6. 工单闭环前需上传承运商反馈截图及处理结果说明。','OPEN',NULL,0.00,0.00,0,'CNY',NULL,NULL,'NONE',NULL,NULL,NULL,NULL,'2026-09-05 09:04:24','2026-09-05 09:04:24',0),(400011,'OMT-TMS-SEED-0009','sla_breach','P1','SLA 违约：订单超承诺时效（渠道承诺 4 天，已滞留 5 天）',NULL,NULL,'OPEN',NULL,0.00,0.00,0,'CNY',NULL,NULL,'NONE',NULL,NULL,NULL,NULL,'2026-09-05 09:04:29','2026-09-05 09:04:29',0);
/*!40000 ALTER TABLE `workorder` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

