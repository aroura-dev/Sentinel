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
  `order_no` varchar(64) NOT NULL COMMENT '关联订单号',
  `merchant_id` bigint(20) DEFAULT NULL,
  `type` varchar(32) NOT NULL DEFAULT 'RETURN' COMMENT 'RETURN退货/EXCHANGE换货',
  `reason` varchar(255) DEFAULT NULL COMMENT '退货原因',
  `refund_amount` decimal(12,2) DEFAULT '0.00' COMMENT '退款金额',
  `currency` varchar(8) DEFAULT 'CNY',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING受理/REFUNDING退款中/REFUNDED已退款/RESHIPPED已重发/CLOSED关闭',
  `buyer_name` varchar(64) DEFAULT NULL,
  `buyer_address` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(4) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_after_sale_order` (`order_no`),
  KEY `idx_after_sale_merchant` (`merchant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='逆向售后退货单';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `after_sale` WRITE;
/*!40000 ALTER TABLE `after_sale` DISABLE KEYS */;
INSERT INTO `after_sale` VALUES (1,'OMT-TMS-SEED-0001',1,'RETURN','外包装完好但内件破损，申请退货退款',360.00,'CNY','PENDING','+14155550101','789 5th Avenue','2026-09-05 09:03:16','2026-09-05 09:03:16',0),(2,'OMT-TMS-SEED-0007',3,'RETURN','型号拍错了，7 天无理由退货',210.00,'CNY','PENDING','+49301234567','Friedrichstraße 123','2026-09-05 09:03:16','2026-09-05 09:03:16',0),(3,'OMT-TMS-SEED-0010',2,'RETURN','派送两次买家均未签收，拒收退回并退款',240.00,'CNY','REFUNDED','+79033456789','Красный пр., д. 21','2026-09-05 09:03:16','2026-09-05 09:03:16',0);
/*!40000 ALTER TABLE `after_sale` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `api_key`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `api_key` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `app_name` varchar(64) NOT NULL,
  `company` varchar(128) DEFAULT NULL COMMENT '所属企业/对接主体',
  `contact_name` varchar(64) DEFAULT NULL COMMENT '负责人',
  `contact_phone` varchar(32) DEFAULT NULL COMMENT '联系电话',
  `contact_email` varchar(128) DEFAULT NULL COMMENT '联系邮箱',
  `api_key` varchar(64) NOT NULL,
  `secret` varchar(64) NOT NULL,
  `scope` varchar(128) DEFAULT 'order:read' COMMENT '授权范围',
  `remark` varchar(255) DEFAULT NULL COMMENT '用途说明',
  `status` tinyint(4) DEFAULT '1' COMMENT '1启用 0停用',
  `created_by` varchar(64) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint(4) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_key` (`api_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放 API 密钥';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `api_key` WRITE;
/*!40000 ALTER TABLE `api_key` DISABLE KEYS */;
/*!40000 ALTER TABLE `api_key` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `bill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bill` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `bill_no` varchar(64) NOT NULL COMMENT '账单号',
  `carrier_id` bigint(20) NOT NULL COMMENT '承运商ID',
  `period_start` date NOT NULL COMMENT '账期开始',
  `period_end` date NOT NULL COMMENT '账期结束',
  `currency` varchar(8) DEFAULT 'CNY' COMMENT '币种',
  `total_amount` decimal(12,2) DEFAULT '0.00' COMMENT '账单总额',
  `status` varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT '状态:DRAFT/SUBMITTED/VERIFIED/SETTLED/REJECTED',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `submitted_by` varchar(64) DEFAULT NULL COMMENT '提交人',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `verified_by` varchar(64) DEFAULT NULL COMMENT '核销人',
  `verified_at` datetime DEFAULT NULL COMMENT '核销时间',
  `settled_by` varchar(64) DEFAULT NULL COMMENT '结算人',
  `settled_at` datetime DEFAULT NULL COMMENT '结算时间',
  `rejected_by` varchar(64) DEFAULT NULL COMMENT '驳回人',
  `rejected_at` datetime DEFAULT NULL COMMENT '驳回时间',
  `reject_reason` varchar(256) DEFAULT NULL COMMENT '驳回原因',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bill_no` (`bill_no`),
  UNIQUE KEY `uk_carrier_period` (`carrier_id`,`period_start`,`period_end`),
  KEY `idx_carrier_id` (`carrier_id`),
  KEY `idx_status` (`status`),
  KEY `idx_period` (`period_start`,`period_end`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='承运商账单(承运人×账期)';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `bill` WRITE;
/*!40000 ALTER TABLE `bill` DISABLE KEYS */;
INSERT INTO `bill` VALUES (1,'BILL-AIRGO-202608',2,'2026-08-01','2026-08-31','CNY',341.90,'SUBMITTED',NULL,'赵敏','2026-09-03 09:03:15',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(2,'BILL-CARGOWAY-202608',1,'2026-08-01','2026-08-31','CNY',195.00,'DRAFT',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(3,'BILL-SEAGO-202607',3,'2026-07-01','2026-07-31','CNY',54.00,'SETTLED',NULL,'赵敏','2026-08-16 09:03:15','赵敏','2026-08-18 09:03:15','赵敏','2026-08-21 09:03:15',NULL,NULL,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:15',0);
/*!40000 ALTER TABLE `bill` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `bill_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bill_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `bill_id` bigint(20) NOT NULL COMMENT '账单ID',
  `waybill_id` bigint(20) NOT NULL COMMENT '运单ID',
  `waybill_no` varchar(64) NOT NULL COMMENT '运单号',
  `order_no` varchar(64) DEFAULT NULL COMMENT '订单号',
  `merchant_id` bigint(20) DEFAULT NULL COMMENT '商家ID',
  `tracking_no` varchar(64) DEFAULT NULL COMMENT '跟踪号',
  `weight_kg` decimal(10,3) DEFAULT NULL COMMENT '实重kg',
  `billable_weight_kg` decimal(10,3) DEFAULT NULL COMMENT '计费重kg',
  `freight_cost` decimal(12,2) NOT NULL COMMENT '运费',
  `currency` varchar(8) DEFAULT 'CNY' COMMENT '币种',
  `billed_at` datetime DEFAULT NULL COMMENT '入账时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bill_waybill` (`bill_id`,`waybill_id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COMMENT='账单明细(每运单一行)';
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
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `carrier_code` varchar(32) NOT NULL COMMENT '承运商编码',
  `carrier_name` varchar(128) NOT NULL COMMENT '承运商名称',
  `type` varchar(16) NOT NULL COMMENT '默认运输方式:rail/air/sea/express',
  `country` varchar(32) DEFAULT 'CN' COMMENT '所在国家',
  `api_endpoint` varchar(256) DEFAULT NULL COMMENT '轨迹对接接口(预留)',
  `api_key` varchar(256) DEFAULT NULL COMMENT '对接密钥(预留)',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：1启用 0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_carrier_code` (`carrier_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='承运商主数据';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `carrier` WRITE;
/*!40000 ALTER TABLE `carrier` DISABLE KEYS */;
INSERT INTO `carrier` VALUES (1,'CARGOWAY','华南干线快运','rail','CN',NULL,NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(2,'AIRGO','畅达快递','air','CN',NULL,NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(3,'SEAGO','粤通专线','sea','CN',NULL,NULL,1,'2026-09-05 09:03:15','2026-09-05 09:03:18',0);
/*!40000 ALTER TABLE `carrier` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `carrier_channel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `carrier_channel` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `carrier_id` bigint(20) NOT NULL COMMENT '承运商ID',
  `channel_code` varchar(64) NOT NULL COMMENT '渠道编码',
  `channel_name` varchar(128) NOT NULL COMMENT '渠道名称',
  `type` varchar(16) NOT NULL COMMENT '运输方式:rail/air/sea/express',
  `dest_country` varchar(8) NOT NULL COMMENT '目的国(RU/US/BR/DE)',
  `transit_days_min` int(11) NOT NULL DEFAULT '1' COMMENT 'SLA最小时效(天)',
  `transit_days_max` int(11) NOT NULL DEFAULT '10' COMMENT 'SLA最大时效(天)',
  `tracking_prefix` varchar(8) DEFAULT NULL COMMENT 'tracking_no前缀',
  `min_billable_weight_kg` decimal(10,3) DEFAULT '0.000' COMMENT '最小计费重量',
  `vol_divisor` int(11) DEFAULT '5000' COMMENT '体积重系数(空/快5000,铁/海6000)',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：1启用 0停用',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_code` (`channel_code`),
  KEY `idx_carrier_id` (`carrier_id`),
  KEY `idx_dest_country` (`dest_country`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COMMENT='物流渠道(承运人×目的国×时效)';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `carrier_channel` WRITE;
/*!40000 ALTER TABLE `carrier_channel` DISABLE KEYS */;
INSERT INTO `carrier_channel` VALUES (1,1,'GD-RAIL','中欧班列快线-俄罗斯','rail','GD',2,4,'RR',0.500,6000,1,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(2,2,'GD-EXPR','俄罗斯空运专线','express','GD',5,4,'CY',0.000,5000,1,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(3,2,'ZJ-EXPR','美国空运专线','express','ZJ',5,4,'YU',0.000,5000,1,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(4,2,'SC-EXPR','巴西空运专线','express','SC',2,4,'BZ',0.000,5000,1,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(5,2,'JS-EXPR','德国空运专线','express','JS',6,4,'DG',0.000,5000,1,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(6,3,'GD-RAIL2','俄罗斯海运大货','express','GD',2,4,'SE',1.000,6000,1,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0);
/*!40000 ALTER TABLE `carrier_channel` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `carrier_rate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `carrier_rate` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `channel_id` bigint(20) NOT NULL COMMENT '渠道ID',
  `zone` varchar(16) NOT NULL DEFAULT 'DEFAULT' COMMENT '区域(目的国/分区),DEFAULT=渠道全域',
  `min_weight_kg` decimal(10,3) NOT NULL COMMENT '重量段下限',
  `max_weight_kg` decimal(10,3) DEFAULT NULL COMMENT '重量段上限(NULL=开放上限)',
  `mode` varchar(20) NOT NULL COMMENT '计费方式:PER_KG单价 | FIRST_CONTINUED首续重',
  `first_weight_kg` decimal(10,3) DEFAULT '0.000' COMMENT '首重(kg)',
  `first_price` decimal(12,4) DEFAULT '0.0000' COMMENT '首重价格',
  `continued_weight_kg` decimal(10,3) DEFAULT '0.000' COMMENT '续重单位(kg)',
  `continued_price` decimal(12,4) DEFAULT '0.0000' COMMENT '续重单价',
  `price` decimal(12,4) DEFAULT '0.0000' COMMENT 'PER_KG单价',
  `currency` varchar(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `effective_from` date NOT NULL COMMENT '生效日期',
  `effective_to` date DEFAULT NULL COMMENT '失效日期(NULL=长期有效)',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：1启用 0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_channel_id` (`channel_id`),
  KEY `idx_zone` (`zone`),
  KEY `idx_weight` (`min_weight_kg`,`max_weight_kg`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COMMENT='运费价卡(渠道×区域×重量段)';
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
  `on_hand` int(11) DEFAULT '0' COMMENT '在库可用',
  `reserved` int(11) DEFAULT '0' COMMENT '已占用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(4) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inventory_sku_wh` (`sku`,`warehouse_id`),
  KEY `idx_inventory_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU 库存台账';
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
  `biz_no` varchar(64) DEFAULT NULL COMMENT '业务单据号(订单/运单)',
  `biz_type` varchar(32) NOT NULL COMMENT 'OUT出库/IN入库/REFUND退回/ADJUST调整',
  `qty` int(11) NOT NULL,
  `warehouse_id` bigint(20) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint(4) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_flow_sku` (`sku`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存出入库流水';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `inventory_flow` WRITE;
/*!40000 ALTER TABLE `inventory_flow` DISABLE KEYS */;
/*!40000 ALTER TABLE `inventory_flow` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `logistics_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `logistics_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `buyer_id` varchar(64) NOT NULL COMMENT '买家ID',
  `buyer_phone` varchar(32) DEFAULT NULL COMMENT '买家手机号',
  `buyer_language` varchar(10) NOT NULL DEFAULT 'zh' COMMENT '买家语言：ru/en/es/zh',
  `merchant_id` bigint(20) DEFAULT NULL COMMENT '商家ID',
  `merchant_name` varchar(64) DEFAULT NULL COMMENT '商家名称',
  `destination_country` varchar(32) DEFAULT NULL COMMENT '目的国',
  `current_node` varchar(32) NOT NULL DEFAULT 'CREATED' COMMENT '当前物流节点（LogisticsNode.codeEn）',
  `review_status` varchar(16) NOT NULL DEFAULT 'APPROVED' COMMENT '订单审核状态: PENDING/APPROVED/REJECTED',
  `channel_id` bigint(20) DEFAULT NULL COMMENT '物流渠道ID(carrier_channel.id)',
  `carrier_id` bigint(20) DEFAULT NULL COMMENT '承运商ID(carrier.id)',
  `warehouse_id` bigint(20) DEFAULT NULL COMMENT '发货仓ID(warehouse.id)',
  `items_json` text COMMENT '商品行JSON:[{sku,product_id,qty,unit_weight_kg,unit_declared_value,currency}]',
  `declared_value` decimal(12,2) DEFAULT '0.00' COMMENT '申报价值',
  `declared_currency` varchar(8) DEFAULT 'CNY' COMMENT '申报币种',
  `freight_cost` decimal(12,2) DEFAULT '0.00' COMMENT '下单时运费报价快照',
  `freight_currency` varchar(8) DEFAULT 'CNY' COMMENT '运费币种',
  `promise_eta` datetime DEFAULT NULL COMMENT '承诺妥投ETA',
  `sla_status` varchar(16) DEFAULT 'NA' COMMENT 'SLA状态:NORMAL/RISK/BREACHED/NA',
  `waybill_no` varchar(64) DEFAULT NULL COMMENT '运单号',
  `buyer_address` varchar(256) DEFAULT NULL COMMENT '买家收货地址',
  `buyer_city` varchar(64) DEFAULT NULL COMMENT '买家城市',
  `buyer_postal` varchar(32) DEFAULT NULL COMMENT '买家邮编',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删 1已删',
  `reviewed_by` varchar(64) DEFAULT NULL COMMENT '审核人',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `reject_reason` varchar(255) DEFAULT NULL COMMENT '驳回原因',
  `business_notes` text COMMENT '业务备注（跨角色协同批注）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_buyer_id` (`buyer_id`),
  KEY `idx_current_node` (`current_node`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_channel_id` (`channel_id`),
  KEY `idx_sla_status` (`sla_status`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COMMENT='物流订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `logistics_order` WRITE;
/*!40000 ALTER TABLE `logistics_order` DISABLE KEYS */;
INSERT INTO `logistics_order` VALUES (1,'OMT-SEED-0001','王芳','13910079259','zh',NULL,'深圳蓝鲸科技','GD','DELIVERED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-08-28 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(2,'OMT-SEED-0002','李娜','15810158518','zh',NULL,'义乌百灵贸易','GD','DELIVERED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-08-29 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(3,'OMT-SEED-0003','刘洋','15910237777','zh',NULL,'广州启航电子','GD','DELIVERED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-08-30 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(4,'OMT-SEED-0004','陈静','18810317036','zh',NULL,'东莞智联制造','GD','LAST_MILE','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-09-01 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(5,'OMT-SEED-0005','杨磊','13710396295','zh',NULL,'杭州云图科技','GD','IMPORT_CUSTOMS','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-08-31 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(6,'OMT-SEED-0006','黄敏','13610475554','zh',NULL,'深圳蓝鲸科技','GD','IMPORT_CUSTOMS','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-08-31 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(7,'OMT-SEED-0007','周强','15010554813','zh',NULL,'义乌百灵贸易','GD','CUSTOMS_DELAY','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-08-31 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(8,'OMT-SEED-0008','吴婷','13810634072','zh',NULL,'广州启航电子','GD','IN_TRANSIT','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-09-02 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(9,'OMT-SEED-0009','徐峰','13910713331','zh',NULL,'东莞智联制造','GD','IN_TRANSIT','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-09-02 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(10,'OMT-SEED-0010','孙丽','15810792590','zh',NULL,'杭州云图科技','GD','DELIVERY_FAILED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-09-02 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(11,'OMT-SEED-0011','马超','15910871849','zh',NULL,'深圳蓝鲸科技','GD','EXPORT_CUSTOMS','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-09-03 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(12,'OMT-SEED-0012','朱琳','18810951108','zh',NULL,'义乌百灵贸易','GD','DOMESTIC_PICKED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-09-04 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(13,'OMT-SEED-0013','胡军','13711030367','zh',NULL,'广州启航电子','GD','WAREHOUSE_OUT','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-09-04 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(14,'OMT-SEED-0014','郭静','13611109626','zh',NULL,'东莞智联制造','GD','CREATED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-09-05 03:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(15,'OMT-SEED-0015','何勇','15011188885','zh',NULL,'杭州云图科技','GD','LOST','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-09-01 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(16,'OMT-SEED-0016','张伟','13811268144','zh',NULL,'深圳蓝鲸科技','GD','RETURNED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-09-01 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(17,'OMT-SEED-0017','王芳','13911347403','zh',NULL,'深圳蓝鲸科技','ZJ','IN_TRANSIT','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'浙江省杭州市滨江区江南大道588号','杭州','310051','2026-09-02 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(18,'OMT-SEED-0018','李娜','15811426662','zh',NULL,'广州启航电子','ZJ','IMPORT_CUSTOMS','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'浙江省杭州市滨江区江南大道588号','杭州','310051','2026-09-01 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(19,'OMT-SEED-0019','刘洋','15911505921','zh',NULL,'东莞智联制造','ZJ','DELIVERED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'浙江省杭州市滨江区江南大道588号','杭州','310051','2026-08-28 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(20,'OMT-SEED-0020','陈静','18811585180','zh',NULL,'杭州云图科技','ZJ','LAST_MILE','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'浙江省杭州市滨江区江南大道588号','杭州','310051','2026-09-01 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(21,'OMT-SEED-0021','杨磊','13711664439','zh',NULL,'义乌百灵贸易','ZJ','CREATED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'浙江省杭州市滨江区江南大道588号','杭州','310051','2026-09-05 04:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(22,'OMT-SEED-0022','黄敏','13611743698','zh',NULL,'深圳蓝鲸科技','SC','IMPORT_CUSTOMS','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'四川省成都市武侯区天府大道北段1700号','成都','610041','2026-08-31 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(23,'OMT-SEED-0023','周强','15011822957','zh',NULL,'广州启航电子','SC','IN_TRANSIT','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'四川省成都市武侯区天府大道北段1700号','成都','610041','2026-09-03 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(24,'OMT-SEED-0024','吴婷','13811902216','zh',NULL,'东莞智联制造','SC','DELIVERED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'四川省成都市武侯区天府大道北段1700号','成都','610041','2026-08-29 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(25,'OMT-SEED-0025','徐峰','13911981475','zh',NULL,'深圳蓝鲸科技','JS','IMPORT_CUSTOMS','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'江苏省南京市建邺区江东中路369号','南京','210019','2026-09-01 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(26,'OMT-SEED-0026','孙丽','15812060734','zh',NULL,'义乌百灵贸易','JS','IN_TRANSIT','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'江苏省南京市建邺区江东中路369号','南京','210019','2026-09-03 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(27,'OMT-SEED-0027','马超','15912139993','zh',NULL,'广州启航电子','JS','DELIVERED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'江苏省南京市建邺区江东中路369号','南京','210019','2026-08-30 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(28,'OMT-SEED-0028','朱琳','18812219252','zh',NULL,'深圳蓝鲸科技','GD','IMPORT_CUSTOMS','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-08-30 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(29,'OMT-SEED-0029','胡军','13712298511','zh',NULL,'杭州云图科技','GD','DELIVERED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-08-27 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(30,'OMT-SEED-0030','郭静','13612377770','zh',NULL,'东莞智联制造','GD','LAST_MILE','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-08-31 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(31,'OMT-SEED-0031','何勇','15012457029','zh',NULL,'义乌百灵贸易','GD','DOMESTIC_PICKED','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-09-04 15:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(32,'OMT-SEED-0032','张伟','13812536288','zh',NULL,'广州启航电子','GD','EXPORT_CUSTOMS','APPROVED',NULL,NULL,NULL,NULL,0.00,'CNY',0.00,'CNY',NULL,'NA',NULL,'广东省广州市天河区华穗路88号','广州','510623','2026-09-03 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(33,'OMT-TMS-SEED-0001','王芳','13912615547','zh',1,'深圳蓝鲸科技','ZJ','DELIVERED','APPROVED',3,2,1,'[{\"sku\":\"SKU-LJ-002\",\"product_id\":2,\"qty\":2,\"unit_weight_kg\":0.28,\"unit_volume_l\":0.5,\"unit_declared_value\":180,\"currency\":\"CNY\"}]',360.00,'CNY',53.20,'CNY','2026-08-27 09:03:15','NORMAL','WB-US-SEED-0001','浙江省杭州市滨江区江南大道588号','杭州','310051','2026-08-26 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(34,'OMT-TMS-SEED-0002','李娜','15812694806','zh',1,'深圳蓝鲸科技','ZJ','IN_TRANSIT','APPROVED',3,2,1,'[{\"sku\":\"SKU-LJ-001\",\"product_id\":1,\"qty\":2,\"unit_weight_kg\":0.45,\"unit_volume_l\":0.8,\"unit_declared_value\":320,\"currency\":\"CNY\"}]',640.00,'CNY',85.50,'CNY','2026-09-09 09:03:15','NORMAL','WB-US-SEED-0002','浙江省杭州市滨江区江南大道588号','杭州','310051','2026-09-01 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(35,'OMT-TMS-SEED-0003','刘洋','15912774065','zh',1,'深圳蓝鲸科技','GD','WAREHOUSE_OUT','APPROVED',2,2,1,'[{\"sku\":\"SKU-LJ-001\",\"product_id\":1,\"qty\":1,\"unit_weight_kg\":0.45,\"unit_volume_l\":0.8,\"unit_declared_value\":320,\"currency\":\"CNY\"}]',320.00,'CNY',40.50,'CNY','2026-09-13 09:03:15','NORMAL','WB-RU-SEED-0003','广东省广州市天河区华穗路88号','广州','510623','2026-09-04 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(36,'OMT-TMS-SEED-0004','陈静','18812853324','zh',2,'义乌百灵贸易','GD','IMPORT_CUSTOMS','APPROVED',1,1,2,'[{\"sku\":\"SKU-BL-001\",\"product_id\":4,\"qty\":1,\"unit_weight_kg\":0.6,\"unit_volume_l\":2.2,\"unit_declared_value\":120,\"currency\":\"CNY\"}]',120.00,'CNY',65.00,'CNY','2026-09-15 09:03:15','BREACHED','WB-RR-SEED-0004','广东省广州市天河区华穗路88号','广州','510623','2026-08-28 09:03:15','2026-09-05 09:04:29',0,NULL,NULL,NULL,NULL),(37,'OMT-TMS-SEED-0005','杨磊','13712932583','zh',2,'义乌百灵贸易','GD','CUSTOMS_DELAY','APPROVED',1,1,2,'[{\"sku\":\"SKU-BL-002\",\"product_id\":5,\"qty\":2,\"unit_weight_kg\":0.3,\"unit_volume_l\":1.1,\"unit_declared_value\":60,\"currency\":\"CNY\"}]',120.00,'CNY',65.00,'CNY','2026-09-03 09:03:15','BREACHED','WB-RR-SEED-0005','广东省广州市天河区华穗路88号','广州','510623','2026-08-16 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(38,'OMT-TMS-SEED-0006','黄敏','13613011842','zh',3,'广州启航电子','SC','LAST_MILE','APPROVED',4,2,2,'[{\"sku\":\"SKU-QH-001\",\"product_id\":7,\"qty\":1,\"unit_weight_kg\":0.8,\"unit_volume_l\":3.5,\"unit_declared_value\":150,\"currency\":\"CNY\"}]',150.00,'CNY',80.00,'CNY','2026-09-08 09:03:15','NORMAL','WB-BR-SEED-0006','四川省成都市武侯区天府大道北段1700号','成都','610041','2026-08-25 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(39,'OMT-TMS-SEED-0007','周强','15013091101','zh',3,'广州启航电子','JS','DELIVERED','APPROVED',5,2,2,'[{\"sku\":\"SKU-QH-002\",\"product_id\":8,\"qty\":1,\"unit_weight_kg\":0.35,\"unit_volume_l\":1.2,\"unit_declared_value\":210,\"currency\":\"CNY\"}]',210.00,'CNY',32.20,'CNY','2026-09-02 09:03:15','NORMAL','WB-DE-SEED-0007','江苏省南京市建邺区江东中路369号','南京','210019','2026-08-29 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(40,'OMT-TMS-SEED-0008','吴婷','13813170360','zh',4,'杭州云图科技','ZJ','LOST','APPROVED',3,2,1,'[{\"sku\":\"SKU-YT-001\",\"product_id\":9,\"qty\":1,\"unit_weight_kg\":1.8,\"unit_volume_l\":8.5,\"unit_declared_value\":420,\"currency\":\"CNY\"}]',420.00,'CNY',171.00,'CNY',NULL,'BREACHED','WB-US-SEED-0008','浙江省杭州市滨江区江南大道588号','杭州','310051','2026-08-21 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL),(41,'OMT-TMS-SEED-0009','徐峰','13913249619','zh',4,'杭州云图科技','GD','IN_TRANSIT','APPROVED',6,3,1,'[{\"sku\":\"SKU-YT-002\",\"product_id\":10,\"qty\":1,\"unit_weight_kg\":3.2,\"unit_volume_l\":9,\"unit_declared_value\":680,\"currency\":\"CNY\"}]',680.00,'CNY',54.00,'CNY','2026-10-05 09:03:15','BREACHED','WB-SE-SEED-0009','广东省广州市天河区华穗路88号','广州','510623','2026-08-31 09:03:15','2026-09-05 09:04:29',0,NULL,NULL,NULL,NULL),(42,'OMT-TMS-SEED-0010','孙丽','15813328878','zh',2,'义乌百灵贸易','GD','DELIVERY_FAILED','APPROVED',1,1,2,'[{\"sku\":\"SKU-BL-001\",\"product_id\":4,\"qty\":2,\"unit_weight_kg\":0.6,\"unit_volume_l\":2.2,\"unit_declared_value\":120,\"currency\":\"CNY\"}]',240.00,'CNY',65.00,'CNY','2026-09-04 09:03:15','BREACHED','WB-RR-SEED-0010','广东省广州市天河区华穗路88号','广州','510623','2026-08-24 09:03:15','2026-09-05 09:03:18',0,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `logistics_order` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `logistics_track`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `logistics_track` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `node` varchar(32) NOT NULL COMMENT '节点（LogisticsNode.codeEn）',
  `raw_status` varchar(64) DEFAULT NULL COMMENT '物流商原始状态码',
  `raw_desc` varchar(256) DEFAULT NULL COMMENT '物流商原始描述',
  `location` varchar(128) DEFAULT NULL COMMENT '位置',
  `carrier_code` varchar(32) DEFAULT NULL COMMENT '承运商编码(预留真实物流商轨迹)',
  `track_time` datetime NOT NULL COMMENT '轨迹时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_node` (`node`),
  KEY `idx_carrier_code` (`carrier_code`)
) ENGINE=InnoDB AUTO_INCREMENT=200037 DEFAULT CHARSET=utf8mb4 COMMENT='物流轨迹表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `logistics_track` WRITE;
/*!40000 ALTER TABLE `logistics_track` DISABLE KEYS */;
INSERT INTO `logistics_track` VALUES (200001,'OMT-SEED-0001','CREATED','EXP-0010','包裹已下单，等待揽收','深圳仓',NULL,'2026-08-28 11:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200002,'OMT-SEED-0001','WAREHOUSE_OUT','EXP-0020','包裹已从仓库出库','深圳仓',NULL,'2026-08-28 18:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200003,'OMT-SEED-0001','DOMESTIC_PICKED','EXP-0030','快递员已揽收','深圳·揽收',NULL,'2026-08-29 01:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200004,'OMT-SEED-0001','EXPORT_CUSTOMS','EXP-0040','包裹已到达中转分拨中心，等待发运','广州分拨中心',NULL,'2026-08-29 17:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200005,'OMT-SEED-0001','IN_TRANSIT','EXP-0050','包裹正在干线运输途中','干线运输中',NULL,'2026-08-30 23:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200006,'OMT-SEED-0001','IMPORT_CUSTOMS','EXP-0060','包裹已到达目的地分拨中心，正在分拣','目的地分拨中心',NULL,'2026-09-02 01:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200007,'OMT-SEED-0001','LAST_MILE','EXP-0070','包裹已进入末端派送','目的地·末端派送',NULL,'2026-09-04 03:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200008,'OMT-SEED-0001','DELIVERED','EXP-0080','包裹已签收','目的地',NULL,'2026-09-04 21:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200009,'OMT-SEED-0002','CREATED','EXP-0010','包裹已下单，等待揽收','深圳仓',NULL,'2026-08-29 09:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200010,'OMT-SEED-0002','WAREHOUSE_OUT','EXP-0020','包裹已从仓库出库','深圳仓',NULL,'2026-08-29 16:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200011,'OMT-SEED-0002','IN_TRANSIT','EXP-0050','包裹正在干线运输途中','干线运输中',NULL,'2026-08-31 09:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200012,'OMT-SEED-0002','IMPORT_CUSTOMS','EXP-0060','包裹已到达目的地分拨中心，正在分拣','目的地分拨中心',NULL,'2026-09-02 21:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200013,'OMT-SEED-0002','DELIVERED','EXP-0080','包裹已签收','目的地',NULL,'2026-09-04 13:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200014,'OMT-SEED-0019','CREATED','EXP-0010','包裹已下单，等待揽收','深圳仓',NULL,'2026-08-28 11:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200015,'OMT-SEED-0019','EXPORT_CUSTOMS','EXP-0040','包裹已到达中转分拨中心，等待发运','广州分拨中心',NULL,'2026-08-30 03:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200016,'OMT-SEED-0019','IMPORT_CUSTOMS','EXP-0060','包裹已到达目的地分拨中心，正在分拣','目的地分拨中心',NULL,'2026-09-01 15:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200017,'OMT-SEED-0019','DELIVERED','EXP-0080','包裹已签收','目的地',NULL,'2026-09-03 17:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200018,'OMT-SEED-0004','CREATED','EXP-0010','包裹已下单，等待揽收','深圳仓',NULL,'2026-09-01 09:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200019,'OMT-SEED-0004','IMPORT_CUSTOMS','EXP-0060','包裹已到达目的地分拨中心，正在分拣','目的地分拨中心',NULL,'2026-09-04 03:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200020,'OMT-SEED-0004','LAST_MILE','EXP-0070','包裹已进入末端派送','目的地·末端派送',NULL,'2026-09-05 03:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200021,'OMT-SEED-0005','CREATED','EXP-0010','包裹已下单，等待揽收','深圳仓',NULL,'2026-08-31 09:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200022,'OMT-SEED-0005','IMPORT_CUSTOMS','EXP-0060','包裹已到达目的地分拨中心，正在分拣','目的地分拨中心',NULL,'2026-09-02 21:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200023,'OMT-SEED-0008','CREATED','EXP-0010','包裹已下单，等待揽收','深圳仓',NULL,'2026-09-02 09:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200024,'OMT-SEED-0008','IN_TRANSIT','EXP-0050','包裹正在干线运输途中','干线运输中',NULL,'2026-09-04 21:03:15','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200025,'OMT-TMS-SEED-0001','WAREHOUSE_OUT','EXP-0020','包裹已从仓库出库','深圳仓','AIRGO','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200026,'OMT-TMS-SEED-0001','DELIVERED','EXP-0080','包裹已签收','目的地','AIRGO','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200027,'OMT-TMS-SEED-0002','WAREHOUSE_OUT','EXP-0020','包裹已从仓库出库','深圳仓','AIRGO','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200028,'OMT-TMS-SEED-0002','IN_TRANSIT','EXP-0050','包裹正在干线运输途中','干线运输中','AIRGO','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200029,'OMT-TMS-SEED-0003','WAREHOUSE_OUT','EXP-0020','包裹已从仓库出库','深圳仓','AIRGO','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200030,'OMT-TMS-SEED-0004','IMPORT_CUSTOMS','EXP-0060','包裹已到达目的地分拨中心，正在分拣','目的地分拨中心','CARGOWAY','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200031,'OMT-TMS-SEED-0005','CUSTOMS_DELAY','CUS-1102','包裹在中转环节延误，正在处理','目的地分拨中心','CARGOWAY','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200032,'OMT-TMS-SEED-0006','LAST_MILE','EXP-0070','包裹已进入末端派送','目的地·末端派送','AIRGO','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200033,'OMT-TMS-SEED-0007','DELIVERED','EXP-0080','包裹已签收','目的地','AIRGO','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200034,'OMT-TMS-SEED-0008','LOST','EXP-0051','包裹在运输途中丢失，正在核查','干线运输中','AIRGO','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200035,'OMT-TMS-SEED-0009','IN_TRANSIT','EXP-0050','包裹正在干线运输途中','干线运输中','SEAGO','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0),(200036,'OMT-TMS-SEED-0010','DELIVERY_FAILED','EXP-0071','派送失败，将重新派送','目的地·末端派送','CARGOWAY','0000-00-00 00:00:00','2026-09-05 09:03:15','2026-09-05 09:03:18',0);
/*!40000 ALTER TABLE `logistics_track` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `merchant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `merchant` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_code` varchar(32) NOT NULL COMMENT '商家编码',
  `merchant_name` varchar(128) NOT NULL COMMENT '商家名称',
  `user_id` bigint(20) DEFAULT NULL COMMENT '关联sentinel_user.id(登录账号)',
  `contact_name` varchar(64) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(32) DEFAULT NULL COMMENT '联系电话',
  `contact_email` varchar(128) DEFAULT NULL COMMENT '联系邮箱',
  `country` varchar(32) DEFAULT 'CN' COMMENT '所在国家',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：1启用 0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_code` (`merchant_code`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='商家(卖家)主数据';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `merchant` WRITE;
/*!40000 ALTER TABLE `merchant` DISABLE KEYS */;
INSERT INTO `merchant` VALUES (1,'MCH-0001','深圳蓝鲸科技',4,'王海','13800138001','wh@lanjing.cn','CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(2,'MCH-0002','义乌百灵贸易',NULL,'李慧','13800138002','lihui@bailing.cn','CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(3,'MCH-0003','广州启航电子',NULL,'陈航','13800138003','chenhang@qihang.cn','CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(4,'MCH-0004','杭州云图科技',NULL,'赵琳','13800138004','zhaolin@yuntu.cn','CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0);
/*!40000 ALTER TABLE `merchant` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `notification_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notification_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `node` varchar(32) NOT NULL COMMENT '触发节点',
  `role` varchar(16) NOT NULL COMMENT '接收角色：buyer/merchant/customer_service',
  `channel` varchar(16) NOT NULL COMMENT '渠道：push/sms/email/feishu',
  `content` text COMMENT '通知内容',
  `language` varchar(10) DEFAULT 'zh' COMMENT '语言',
  `status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SENT/FAILED',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路ID',
  `agent_call_log_id` bigint(20) DEFAULT NULL COMMENT '关联 Agent 调用日志',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=300018 DEFAULT CHARSET=utf8mb4 COMMENT='通知记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `notification_record` WRITE;
/*!40000 ALTER TABLE `notification_record` DISABLE KEYS */;
INSERT INTO `notification_record` VALUES (300001,'OMT-SEED-0001','IMPORT_CUSTOMS','buyer','sms','您的包裹已到达目的地分拨中心，正在分拣。','zh','SENT','SEED-0001',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300002,'OMT-SEED-0001','DELIVERED','buyer','push','您的包裹已签收，感谢您的信任与支持！','zh','SENT','SEED-0001',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300003,'OMT-SEED-0002','IMPORT_CUSTOMS','buyer','sms','您的包裹已到达目的地分拨中心，正在分拣。','zh','SENT','SEED-0002',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300004,'OMT-SEED-0019','IMPORT_CUSTOMS','buyer','push','您的包裹已到达目的地分拨中心，正在分拣。','zh','SENT','SEED-0019',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300005,'OMT-SEED-0019','DELIVERED','buyer','email','您的包裹已签收，感谢您的信任与支持！','zh','SENT','SEED-0019',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300006,'OMT-SEED-0004','LAST_MILE','buyer','push','您的包裹已进入末端派送，快递员将很快与您联系。','zh','SENT','SEED-0004',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300007,'OMT-SEED-0005','IMPORT_CUSTOMS','buyer','sms','您的包裹已到达目的地分拨中心，正在分拣。','zh','PENDING','SEED-0005',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300008,'OMT-SEED-0007','CUSTOMS_DELAY','buyer','sms','您的包裹在中转环节延误，正在处理，请耐心等待。','zh','SENT','SEED-0007',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300009,'OMT-SEED-0007','CUSTOMS_DELAY','merchant','feishu','您的包裹在中转环节延误，正在处理，请耐心等待。','zh','SENT','SEED-0007',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300010,'OMT-SEED-0010','DELIVERY_FAILED','buyer','push','您的包裹派送失败，将重新派送，请保持电话畅通。','zh','SENT','SEED-0010',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300011,'OMT-SEED-0015','LOST','buyer','sms','您的包裹在运输途中丢失，正在核查处理，请耐心等待。','zh','SENT','SEED-0015',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300012,'OMT-SEED-0016','RETURNED','merchant','feishu','您的包裹已退回发件仓。','zh','SENT','SEED-0016',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300013,'OMT-SEED-0022','IMPORT_CUSTOMS','buyer','sms','您的包裹已到达目的地分拨中心，正在分拣。','zh','SENT','SEED-0022',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300014,'OMT-SEED-0025','IMPORT_CUSTOMS','buyer','push','您的包裹已到达目的地分拨中心，正在分拣。','zh','SENT','SEED-0025',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300015,'OMT-SEED-0028','IMPORT_CUSTOMS','buyer','sms','您的包裹已到达目的地分拨中心，正在分拣。','zh','SENT','SEED-0028',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300016,'OMT-SEED-0003','DELIVERED','buyer','push','您的包裹已签收，感谢您的信任与支持！','zh','SENT','SEED-0003',NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(300017,'OMT-TMS-SEED-0010','DELIVERY_FAILED','buyer','push','物流派送未成功，订单OMT-TMS-SEED-0010的商品已暂存网点，稍后将重新安排配送，请留意后续通知。','zh','PENDING','OMT-TMS-SEED-0010|DELIVERY_FAILED|zh|buyer',NULL,'2026-09-05 09:04:19','2026-09-05 09:04:19',0);
/*!40000 ALTER TABLE `notification_record` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `operation_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operator` varchar(64) NOT NULL COMMENT '操作人(username/用户名)',
  `operator_role` varchar(32) DEFAULT NULL COMMENT '操作人角色',
  `module` varchar(32) NOT NULL COMMENT '业务模块:order/waybill/workorder/bill/sla/notify',
  `action` varchar(64) NOT NULL COMMENT '动作:CREATE/GENERATE/ADVANCE/ANOMALY/CLAIM/SUBMIT/VERIFY/SETTLE/REJECT/DIAGNOSE/NOTIFY',
  `target_no` varchar(64) DEFAULT NULL COMMENT '业务对象单号(订单/运单/账单号)',
  `detail` varchar(512) DEFAULT NULL COMMENT '操作详情(JSON或描述)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_module` (`module`),
  KEY `idx_target` (`target_no`),
  KEY `idx_operator` (`operator`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `operation_log` WRITE;
/*!40000 ALTER TABLE `operation_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `operation_log` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id` bigint(20) NOT NULL COMMENT '商家ID',
  `sku` varchar(64) NOT NULL COMMENT 'SKU编码',
  `name` varchar(256) NOT NULL COMMENT '商品名称',
  `hs_code` varchar(32) DEFAULT NULL COMMENT '海关HS编码',
  `declared_value` decimal(12,2) DEFAULT '0.00' COMMENT '单件申报价值',
  `currency` varchar(8) DEFAULT 'CNY' COMMENT '申报币种',
  `weight_kg` decimal(10,3) NOT NULL COMMENT '单件实重kg',
  `volume_l` decimal(10,3) DEFAULT '0.000' COMMENT '单件体积L',
  `origin_country` varchar(8) DEFAULT 'CN' COMMENT '原产国',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：1启用 0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_sku` (`merchant_id`,`sku`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU(HS编码/申报/重量)';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `product` WRITE;
/*!40000 ALTER TABLE `product` DISABLE KEYS */;
INSERT INTO `product` VALUES (1,1,'SKU-LJ-001','智能手表','9102.12',320.00,'CNY',0.450,0.800,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(2,1,'SKU-LJ-002','蓝牙耳机','8518.30',180.00,'CNY',0.280,0.500,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(3,1,'SKU-LJ-003','USB数据线3条装','8544.42',45.00,'CNY',0.150,0.300,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(4,2,'SKU-BL-001','针织毛衣','6110.30',120.00,'CNY',0.600,2.200,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(5,2,'SKU-BL-002','围巾','6117.10',60.00,'CNY',0.300,1.100,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(6,2,'SKU-BL-003','收纳箱','3924.90',85.00,'CNY',1.200,6.000,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(7,3,'SKU-QH-001','LED台灯','9405.40',150.00,'CNY',0.800,3.500,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(8,3,'SKU-QH-002','无线充电器','8504.40',210.00,'CNY',0.350,1.200,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(9,4,'SKU-YT-001','便携吸尘器','8508.11',420.00,'CNY',1.800,8.500,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(10,4,'SKU-YT-002','智能门锁','8301.70',680.00,'CNY',3.200,9.000,'CN',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0);
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
  `action` varchar(64) NOT NULL COMMENT 'NOTIFY通知/CREATE_WORKORDER建工单',
  `action_config` varchar(255) DEFAULT NULL,
  `enabled` tinyint(4) DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(4) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='风险预警自动处置规则';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `risk_rule` WRITE;
/*!40000 ALTER TABLE `risk_rule` DISABLE KEYS */;
INSERT INTO `risk_rule` VALUES (1,'SLA 违约自动建工单','SLA','BREACHED','CREATE_WORKORDER','{\"level\":\"HIGH\"}',1,'2026-09-05 09:03:13','2026-09-05 09:03:13',0),(2,'SLA 预警自动通知','SLA','RISK','NOTIFY','{\"channel\":\"email\"}',1,'2026-09-05 09:03:13','2026-09-05 09:03:13',0);
/*!40000 ALTER TABLE `risk_rule` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `warehouse`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `warehouse` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `warehouse_code` varchar(32) NOT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(128) NOT NULL COMMENT '仓库名称',
  `country` varchar(8) DEFAULT 'CN' COMMENT '所在国家',
  `city` varchar(64) DEFAULT NULL COMMENT '城市',
  `address` varchar(256) DEFAULT NULL COMMENT '地址',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：1启用 0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_warehouse_code` (`warehouse_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='发货仓库';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `warehouse` WRITE;
/*!40000 ALTER TABLE `warehouse` DISABLE KEYS */;
INSERT INTO `warehouse` VALUES (1,'WH-SZ','深圳仓','CN','深圳','宝安区福永街道国际物流园A区',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0),(2,'WH-SH','上海仓','CN','上海','青浦区华新镇仓储基地B栋',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0);
/*!40000 ALTER TABLE `warehouse` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `waybill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `waybill` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `waybill_no` varchar(64) NOT NULL COMMENT '运单号',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `merchant_id` bigint(20) DEFAULT NULL COMMENT '商家ID',
  `channel_id` bigint(20) NOT NULL COMMENT '渠道ID',
  `carrier_id` bigint(20) DEFAULT NULL COMMENT '承运商ID',
  `tracking_no` varchar(64) NOT NULL COMMENT '跟踪号',
  `carrier_code` varchar(32) DEFAULT NULL COMMENT '承运商编码',
  `weight_kg` decimal(10,3) DEFAULT NULL COMMENT '实重kg',
  `volume_l` decimal(10,3) DEFAULT NULL COMMENT '体积L',
  `billable_weight_kg` decimal(10,3) DEFAULT NULL COMMENT '计费重kg',
  `declared_value` decimal(12,2) DEFAULT '0.00' COMMENT '申报价值',
  `declared_currency` varchar(8) DEFAULT 'CNY' COMMENT '申报币种',
  `freight_cost` decimal(12,2) NOT NULL COMMENT '出库时运费快照',
  `freight_currency` varchar(8) DEFAULT 'CNY' COMMENT '运费币种',
  `zone` varchar(16) DEFAULT NULL COMMENT '计费区域',
  `promise_eta` datetime DEFAULT NULL COMMENT '承诺妥投ETA',
  `actual_delivered_at` datetime DEFAULT NULL COMMENT '实际妥投时间',
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态:ACTIVE/DELIVERED/CANCELED',
  `billed` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否已入账单',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `items_json` text COMMENT '运单商品明细（分批出库子集）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_waybill_no` (`waybill_no`),
  UNIQUE KEY `uk_tracking_no` (`tracking_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_channel_id` (`channel_id`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COMMENT='运单(出库生成,运费快照)';
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
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `type` varchar(32) NOT NULL COMMENT '异常类型：customs_delay/lost/returned/delivery_failed/sla_breach',
  `level` varchar(8) NOT NULL DEFAULT 'P1' COMMENT '级别：P0/P1/P2',
  `description` text COMMENT '描述',
  `agent_diagnosis` text COMMENT 'Agent 诊断结果（JSON）',
  `sop` text COMMENT '处理 SOP',
  `status` varchar(16) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN/PROCESSING/RESOLVED/CLOSED/PUSHED',
  `liability` varchar(16) DEFAULT NULL COMMENT '责任方:merchant/carrier/platform',
  `claim_amount` decimal(12,2) DEFAULT '0.00' COMMENT '索赔金额',
  `compensation_amount` decimal(12,2) DEFAULT '0.00' COMMENT '理赔金额',
  `sla_breach` tinyint(4) DEFAULT '0' COMMENT '是否SLA违约',
  `currency` varchar(8) DEFAULT 'CNY' COMMENT '金额币种',
  `resolution` varchar(512) DEFAULT NULL COMMENT '处理结果',
  `resolved_at` datetime DEFAULT NULL COMMENT '解决时间',
  `claim_status` varchar(16) NOT NULL DEFAULT 'NONE' COMMENT '理赔状态:NONE/SUBMITTED/APPROVED/PAID/REJECTED',
  `claim_submitted_at` datetime DEFAULT NULL COMMENT '索赔提交时间',
  `claim_approved_at` datetime DEFAULT NULL COMMENT '理赔审批时间',
  `claim_paid_at` datetime DEFAULT NULL COMMENT '赔付时间',
  `claim_reject_reason` varchar(255) DEFAULT NULL COMMENT '驳回原因',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_status` (`status`),
  KEY `idx_level` (`level`),
  KEY `idx_claim_status` (`claim_status`)
) ENGINE=InnoDB AUTO_INCREMENT=400012 DEFAULT CHARSET=utf8mb4 COMMENT='异常工单表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `workorder` WRITE;
/*!40000 ALTER TABLE `workorder` DISABLE KEYS */;
INSERT INTO `workorder` VALUES (400001,'OMT-SEED-0007','customs_delay','P1','订单在物流环节发生中转延误异常（节点 CUSTOMS_DELAY）','{\"type\":\"customs_delay\",\"level\":\"P1\",\"sop\":\"1.联系物流商核实 2.通知买家预计延误时间\",\"reason\":\"海关抽检，包裹在目的国海关停留\"}','1.联系物流商核实 2.通知买家预计延误时间 3.超过72h建议补发安抚通知','OPEN','carrier',0.00,0.00,1,'CNY',NULL,NULL,'NONE',NULL,NULL,NULL,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(400002,'OMT-SEED-0010','delivery_failed','P2','订单在物流环节发生派送失败异常（节点 DELIVERY_FAILED）','{\"type\":\"delivery_failed\",\"level\":\"P2\",\"sop\":\"1.联系买家预约派送时间 2.重新安排派送\",\"reason\":\"买家不在家，派送失败\"}','1.联系买家预约派送时间 2.重新安排派送','PROCESSING','merchant',120.00,0.00,0,'CNY','商家提供错误地址，改派中',NULL,'SUBMITTED','2026-09-05 09:03:15',NULL,NULL,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:16',0),(400003,'OMT-SEED-0015','lost','P0','订单在物流环节发生丢件异常（节点 LOST）','{\"type\":\"lost\",\"level\":\"P0\",\"sop\":\"1.联系物流商核实 2.创建丢件工单 3.建议退款\",\"reason\":\"国际运输途中丢件\"}','1.联系物流商核实 2.创建丢件工单 3.建议退款','OPEN','carrier',420.00,420.00,1,'CNY','丢失理赔已赔付','2026-09-05 09:03:15','SUBMITTED','2026-09-05 09:03:15',NULL,NULL,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:16',0),(400004,'OMT-SEED-0016','returned','P1','订单在物流环节发生退回异常（节点 RETURNED）','{\"type\":\"returned\",\"level\":\"P1\",\"sop\":\"1.核实退回原因 2.通知买家 3.跟进退款\",\"reason\":\"包裹退回发件地\"}','1.核实退回原因 2.通知买家 3.跟进退款','CLOSED','platform',150.00,50.00,1,'CNY','中转延误平台补偿',NULL,'SUBMITTED','2026-09-05 09:03:15',NULL,NULL,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(400005,'OMT-TMS-SEED-0005','sla_breach','P1','SLA 违约：中转滞留超过承诺时效',NULL,NULL,'OPEN',NULL,0.00,0.00,1,'CNY',NULL,NULL,'NONE',NULL,NULL,NULL,NULL,'2026-09-05 09:03:15','2026-09-05 09:03:18',0),(400006,'OMT-TMS-SEED-0002','lost','P0','整件包裹在干线运输中丢失，买家要求按货值全额索赔',NULL,NULL,'PROCESSING','carrier',640.00,0.00,0,'CNY',NULL,NULL,'SUBMITTED','2026-09-04 09:03:16',NULL,NULL,NULL,'2026-09-05 09:03:16','2026-09-05 09:03:16',0),(400007,'OMT-TMS-SEED-0008','lost','P1','运单在转运后物流记录中断，疑似丢失，进入责任认定',NULL,NULL,'PROCESSING','carrier',420.00,0.00,0,'CNY',NULL,NULL,'SUBMITTED','2026-09-05 03:03:16',NULL,NULL,NULL,'2026-09-05 09:03:16','2026-09-05 09:03:16',0),(400008,'OMT-TMS-SEED-0004','customs_delay','P1','中转延误超承诺 3 天，买家索赔时效损失',NULL,NULL,'RESOLVED','carrier',200.00,120.00,0,'CNY','承运时效延误，核定赔付货值 60%',NULL,'APPROVED','2026-09-01 09:03:16','2026-09-03 09:03:16',NULL,NULL,'2026-09-05 09:03:16','2026-09-05 09:03:16',0),(400009,'OMT-TMS-SEED-0005','customs_delay','P2','延迟系商家晚发货导致，非承运运输时效问题，驳回索赔',NULL,NULL,'RESOLVED','merchant',200.00,0.00,0,'CNY',NULL,NULL,'REJECTED','2026-08-31 09:03:16','2026-09-01 09:03:16',NULL,'发货延迟属商家责任，驳回对承运商的理赔','2026-09-05 09:03:16','2026-09-05 09:03:16',0),(400010,'OMT-TMS-SEED-0010','delivery_failed','P1','订单在物流环节发生delivery_failed异常（节点 DELIVERY_FAILED）','{\"level\":\"P1\",\"sop\":\"1. 立即联系承运商核实失败原因（如收件人拒收、地址错误、电话不通、无人签收等）；2. 核对订单收件信息准确性，确认是否需更新或补发；3. 若属可重派场景，48小时内安排重新配送；4. 若属拒收或客户主动取消，触发退货流程并同步ERP系统；5. 向客户发送致歉及解决方案短信/站内信；6. 工单闭环前需上传承运商反馈截图及处理结果说明。\",\"type\":\"delivery_failed\"}','1. 立即联系承运商核实失败原因（如收件人拒收、地址错误、电话不通、无人签收等）；2. 核对订单收件信息准确性，确认是否需更新或补发；3. 若属可重派场景，48小时内安排重新配送；4. 若属拒收或客户主动取消，触发退货流程并同步ERP系统；5. 向客户发送致歉及解决方案短信/站内信；6. 工单闭环前需上传承运商反馈截图及处理结果说明。','OPEN',NULL,0.00,0.00,0,'CNY',NULL,NULL,'NONE',NULL,NULL,NULL,NULL,'2026-09-05 09:04:24','2026-09-05 09:04:24',0),(400011,'OMT-TMS-SEED-0009','sla_breach','P1','SLA 违约：订单超承诺时效（渠道承诺 4 天，已滞留 5 天）',NULL,NULL,'OPEN',NULL,0.00,0.00,0,'CNY',NULL,NULL,'NONE',NULL,NULL,NULL,NULL,'2026-09-05 09:04:29','2026-09-05 09:04:29',0);
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

