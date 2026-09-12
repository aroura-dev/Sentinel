-- sentinel-ms : sentinel_agent（由 模块化单体版 库快照导出生成，勿手改）
CREATE DATABASE IF NOT EXISTS sentinel_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE sentinel_agent;
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
DROP TABLE IF EXISTS `agent_call_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `agent_call_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `agent_name` varchar(32) NOT NULL COMMENT 'Agent åç§°',
  `input` text COMMENT 'è¾“å…¥å‚æ•°ï¼ˆJSONï¼‰',
  `output` text COMMENT 'è¾“å‡ºç»“æžœï¼ˆJSONï¼‰',
  `tools_called` varchar(256) DEFAULT NULL COMMENT 'è°ƒç”¨çš„ Tool åˆ—è¡¨ï¼ˆé€—å·åˆ†éš”ï¼‰',
  `token_usage` int(11) DEFAULT NULL COMMENT 'Token æ¶ˆè€—',
  `latency_ms` int(11) DEFAULT NULL COMMENT 'è€—æ—¶ï¼ˆæ¯«ç§’ï¼‰',
  `status` varchar(16) NOT NULL DEFAULT 'success' COMMENT 'çŠ¶æ€ï¼šsuccess/failed/timeout/degraded',
  `trace_id` varchar(64) DEFAULT NULL COMMENT 'é“¾è·¯ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_agent_name` (`agent_name`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=500018 DEFAULT CHARSET=utf8mb4 COMMENT='Agent è°ƒç”¨æ—¥å¿—è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `agent_call_log` WRITE;
/*!40000 ALTER TABLE `agent_call_log` DISABLE KEYS */;
INSERT INTO `agent_call_log` (`id`,`agent_name`,`input`,`output`,`tools_called`,`token_usage`,`latency_ms`,`status`,`trace_id`,`created_at`,`updated_at`) VALUES (500001,'ContentGenAgent','{\"node\":\"IMPORT_CUSTOMS\",\"language\":\"zh\"}','æ‚¨çš„ç‰©æµçŠ¶æ€å·²æ›´æ–°ï¼Œè¯·å…³æ³¨æœ€æ–°è¿›å±•ã€‚','queryTemplate',320,1200,'success','SEED-0001','2026-08-29 09:03:15','2026-09-05 09:03:18'),(500002,'ContentGenAgent','{\"node\":\"DELIVERED\",\"language\":\"zh\"}','æ‚¨çš„ç‰©æµçŠ¶æ€å·²æ›´æ–°ï¼Œè¯·å…³æ³¨æœ€æ–°è¿›å±•ã€‚','queryTemplate',280,900,'success','SEED-0001','2026-08-29 09:03:15','2026-09-05 09:03:18'),(500003,'ContentGenAgent','{\"node\":\"IMPORT_CUSTOMS\",\"language\":\"zh\"}','æ‚¨çš„ç‰©æµçŠ¶æ€å·²æ›´æ–°ï¼Œè¯·å…³æ³¨æœ€æ–°è¿›å±•ã€‚','queryTemplate',300,1100,'success','SEED-0019','2026-08-30 09:03:15','2026-09-05 09:03:18'),(500004,'AnomalyDiagnoseAgent','{\"type\":\"customs_delay\",\"statusCode\":\"CUS-1102\"}','{\"reason\":\"æµ·å…³æŠ½æ£€ï¼ŒåŒ…è£¹åœ¨ç›®çš„å›½æµ·å…³åœç•™\",\"priority\":\"P1\"}','querySimilarAnomaly,decodeStatusCode',680,3800,'success','SEED-0007','2026-08-31 09:03:15','2026-09-05 09:03:15'),(500005,'WorkorderAgent','{\"anomalyDesc\":\"è®¢å•åœ¨ç‰©æµçŽ¯èŠ‚å‘ç”Ÿæ¸…å…³å»¶è¯¯å¼‚å¸¸\"}','{\"type\":\"customs_delay\",\"level\":\"P1\",\"sop\":\"1.è”ç³»ç‰©æµå•†æ ¸å®ž...\"}','classifyAnomaly,querySOP',720,4100,'success','SEED-0007','2026-08-31 09:03:15','2026-09-05 09:03:15'),(500006,'ContentGenAgent','{\"node\":\"CUSTOMS_DELAY\",\"language\":\"zh\"}','æ‚¨çš„ç‰©æµçŠ¶æ€å·²æ›´æ–°ï¼Œè¯·å…³æ³¨æœ€æ–°è¿›å±•ã€‚','queryTemplate',310,1000,'success','SEED-0007','2026-08-31 09:03:15','2026-09-05 09:03:18'),(500007,'ChannelRouteAgent','{\"priority\":\"P1\",\"buyerId\":\"buyer_007\"}','{\"primary\":\"sms\",\"backup\":\"push\",\"reason\":\"SMS é…é¢å……è¶³\"}','queryChannelHealth,queryUserPreference',540,2900,'success','SEED-0007','2026-08-31 09:03:15','2026-09-05 09:03:15'),(500008,'AnomalyDiagnoseAgent','{\"type\":\"lost\",\"statusCode\":\"EXP-0051\"}','{\"reason\":\"å›½é™…è¿è¾“é€”ä¸­ä¸¢ä»¶\",\"priority\":\"P0\"}','querySimilarAnomaly',700,3600,'success','SEED-0015','2026-09-02 09:03:15','2026-09-05 09:03:15'),(500009,'CsRouteAgent','{\"message\":\"æˆ‘çš„åŒ…è£¹åˆ°å“ªäº†ï¼Ÿ\",\"buyerId\":\"buyer_005\"}','{\"intent\":\"query_track\",\"route\":\"auto\",\"reply\":\"è¯·æä¾›æ‚¨çš„è®¢å•å·...\"}','queryTrack',430,2400,'success','SEED-CS-1','2026-09-03 09:03:15','2026-09-05 09:03:15'),(500010,'ContentGenAgent','{\"node\":\"IMPORT_CUSTOMS\",\"language\":\"zh\"}','æ‚¨çš„ç‰©æµçŠ¶æ€å·²æ›´æ–°ï¼Œè¯·å…³æ³¨æœ€æ–°è¿›å±•ã€‚','queryTemplate',290,950,'success','SEED-0022','2026-09-03 09:03:15','2026-09-05 09:03:18'),(500011,'UnsubscribePredictAgent','{\"buyerId\":\"buyer_010\",\"node\":\"DELIVERY_FAILED\"}','{\"probability\":0.31,\"advice\":\"reduce\"}','queryUserNotifyHistory',510,2700,'success','SEED-0010','2026-09-04 09:03:15','2026-09-05 09:03:15'),(500012,'ContentGenAgent','{\"node\":\"DELIVERY_FAILED\",\"language\":\"zh\"}','æ‚¨çš„ç‰©æµçŠ¶æ€å·²æ›´æ–°ï¼Œè¯·å…³æ³¨æœ€æ–°è¿›å±•ã€‚','queryTemplate',300,1050,'degraded','SEED-0010','2026-09-04 09:03:15','2026-09-05 09:03:18'),(500013,'AnomalyDiagnoseAgent','{\"type\":\"delivery_failed\",\"statusCode\":\"EXP-0071\"}','{\"reason\":\"ä¹°å®¶ä¸åœ¨å®¶\",\"priority\":\"P2\"}','querySimilarAnomaly',660,3400,'success','SEED-0010','2026-09-04 09:03:15','2026-09-05 09:03:15'),(500014,'CsRouteAgent','{\"message\":\"åŒ…è£¹ä¸€ç›´æ²¡åˆ°ï¼Œæˆ‘è¦æŠ•è¯‰\",\"buyerId\":\"buyer_010\"}','{\"intent\":\"complaint\",\"route\":\"human\",\"reply\":\"å·²ä¸ºæ‚¨è½¬æŽ¥äººå·¥å®¢æœ\"}','transferHuman',390,2100,'success','SEED-CS-2','2026-09-05 03:03:15','2026-09-05 09:03:15'),(500015,'AnomalyDiagnoseAgent','{\"anomalyType\":\"delivery_failed\",\"statusCode\":\"EXP-0071\",\"orderInfo\":\"{\\\"id\\\":42,\\\"order_no\\\":\\\"OMT-TMS-SEED-0010\\\",\\\"buyer_id\\\":\\\"å­™ä¸½\\\",\\\"buyer_phone\\\":\\\"15813328878\\\",\\\"buyer_language\\\":\\\"zh\\\",\\\"merchant_id\\\":2,\\\"merchant_name\\\":\\\"ä¹‰ä¹Œç™¾çµè´¸æ˜“\\\",\\\"destination_country\\\":\\\"GD\\\",\\\"current_node\\\":\\\"DELIVERY_FAILED\\\",\\\"review_status\\\":\\\"APPROVED\\\",\\\"channel_id\\\":1,\\\"carrier_id\\\":1,\\\"warehouse_id\\\":2,\\\"items_json\\\":\\\"[{\\\\\\\"sku\\\\\\\":\\\\\\\"SKU-BL-001\\\\\\\",\\\\\\\"product_id\\\\\\\":4,\\\\\\\"qty\\\\\\\":2,\\\\\\\"unit_weight_kg\\\\\\\":0.6,\\\\\\\"unit_volume_l\\\\\\\":2.2,\\\\\\\"unit_declared_value\\\\\\\":120,\\\\\\\"currency\\\\\\\":\\\\\\\"CNY\\\\\\\"}]\\\",\\\"declared_value\\\":240.00,\\\"declared_currency\\\":\\\"CNY\\\",\\\"freight_cost\\\":65.00,\\\"freight_currency\\\":\\\"CNY\\\",\\\"promise_eta\\\":\\\"2026-09-04 09:03:15\\\",\\\"sla_status\\\":\\\"BREACHED\\\",\\\"waybill_no\\\":\\\"WB-RR-SEED-0010\\\",\\\"buyer_address\\\":\\\"å¹¿ä¸œçœå¹¿å·žå¸‚å¤©æ²³åŒºåŽç©—è·¯88å·\\\",\\\"buyer_city\\\":\\\"å¹¿å·ž\\\",\\\"buyer_postal\\\":\\\"510623\\\",\\\"created_at\\\":\\\"2026-08-24 09:03:15\\\",\\\"updated_at\\\":\\\"2026-09-05 09:03:18\\\",\\\"is_deleted\\\":0}\"}','{\"priority\":\"P1\",\"reason\":\"本地派送失败（买家不在家）\",\"suggestion\":\"1.联系买家预约派送时间 2.重新安排派送\"}','queryKnowledge',1673,2818,'success','OMT-TMS-SEED-0010|DELIVERY_FAILED|zh|buyer','2026-09-05 09:04:18','2026-09-05 09:04:18'),(500016,'ContentGenAgent','{\"node\":\"DELIVERY_FAILED\",\"language\":\"zh\",\"productInfo\":\"商品信息\",\"orderNo\":\"OMT-TMS-SEED-0010\"}','\"物流派送未成功，订单OMT-TMS-SEED-0010的商品已暂存网点，稍后将重新安排配送，请留意后续通知。\"',NULL,342,1138,'success','OMT-TMS-SEED-0010|DELIVERY_FAILED|zh|buyer','2026-09-05 09:04:19','2026-09-05 09:04:19'),(500017,'WorkorderAgent','{\"anomalyDesc\":\"订单在物流环节发生delivery_failed异常（节点 DELIVERY_FAILED）\",\"orderInfo\":\"OMT-TMS-SEED-0010\"}','{\"level\":\"P1\",\"sop\":\"1. 立即联系承运商核实失败原因（如收件人拒收、地址错误、电话不通、无人签收等）；2. 核对订单收件信息准确性，确认是否需更新或补发；3. 若属可重派场景，48小时内安排重新配送；4. 若属拒收或客户主动取消，触发退货流程并同步ERP系统；5. 向客户发送致歉及解决方案短信/站内信；6. 工单闭环前需上传承运商反馈截图及处理结果说明。\",\"type\":\"delivery_failed\"}','getExistingWorkorder,queryKnowledge',1137,5235,'success','OMT-TMS-SEED-0010|DELIVERY_FAILED|zh|buyer','2026-09-05 09:04:24','2026-09-05 09:04:24');
/*!40000 ALTER TABLE `agent_call_log` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `anomaly_knowledge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `anomaly_knowledge` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `status_code` varchar(64) NOT NULL COMMENT 'ç‰©æµå•†çŠ¶æ€ç ',
  `type` varchar(32) NOT NULL COMMENT 'å¼‚å¸¸ç±»åž‹',
  `description` text NOT NULL COMMENT 'ä¸­æ–‡æè¿°',
  `avg_duration_hours` int(11) DEFAULT NULL COMMENT 'å¹³å‡å¤„ç†æ—¶é•¿ï¼ˆå°æ—¶ï¼‰',
  `suggestion` text COMMENT 'å¤„ç†å»ºè®®',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_status_code` (`status_code`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COMMENT='å¼‚å¸¸çŸ¥è¯†åº“è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `anomaly_knowledge` WRITE;
/*!40000 ALTER TABLE `anomaly_knowledge` DISABLE KEYS */;
INSERT INTO `anomaly_knowledge` VALUES (1,'CUS-1102','customs_delay','ä¸­è½¬åˆ†æ‹¨çŽ¯èŠ‚æ»žç•™ï¼ŒåŒ…è£¹åœç•™æ—¶é—´è¾ƒé•¿',48,'è”ç³»æ‰¿è¿å•†åŠ å¿«å¤„ç†','2026-09-05 09:03:12','2026-09-05 09:03:18',0),(2,'CUS-1105','customs_delay','ä¸­è½¬ä¿¡æ¯ç¼ºå¤±ï¼Œéœ€è¡¥å……é¢å•ä¿¡æ¯',72,'è¡¥å……é¢å•/åœ°å€ä¿¡æ¯','2026-09-05 09:03:12','2026-09-05 09:03:18',0),(3,'EXP-0051','lost','å¹²çº¿è¿è¾“é€”ä¸­ä¸¢ä»¶',0,'è”ç³»æ‰¿è¿å•†æ ¸æŸ¥','2026-09-05 09:03:12','2026-09-05 09:03:18',0),(4,'EXP-0062','returned','åŒ…è£¹é€€å›žå‘ä»¶åœ°',0,'1.æ ¸å®žé€€å›žåŽŸå›  2.é€šçŸ¥ä¹°å®¶ 3.è·Ÿè¿›é€€æ¬¾','2026-09-05 09:03:12','2026-09-05 09:03:12',0),(5,'EXP-0071','delivery_failed','æœ¬åœ°æ´¾é€å¤±è´¥ï¼ˆä¹°å®¶ä¸åœ¨å®¶ï¼‰',24,'1.è”ç³»ä¹°å®¶é¢„çº¦æ´¾é€æ—¶é—´ 2.é‡æ–°å®‰æŽ’æ´¾é€','2026-09-05 09:03:12','2026-09-05 09:03:12',0),(6,'EXP-0010','normal','ä¸‹å•ï¼ŒåŒ…è£¹åˆ›å»º',0,'æ— éœ€å¤„ç†','2026-09-05 09:03:14','2026-09-05 09:03:14',0),(7,'EXP-0020','normal','ä»“åº“å‡ºåº“',0,'æ— éœ€å¤„ç†','2026-09-05 09:03:14','2026-09-05 09:03:14',0),(8,'EXP-0030','normal','å›½å†…æ½æ”¶æˆåŠŸ',0,'æ— éœ€å¤„ç†','2026-09-05 09:03:14','2026-09-05 09:03:14',0),(9,'EXP-0040','normal','ä¸­è½¬åˆ†æ‹¨å®Œæˆ',0,'ç­‰å¾…å‘è¿','2026-09-05 09:03:14','2026-09-05 09:03:18',0),(10,'EXP-0050','normal','å¹²çº¿è¿è¾“é€”ä¸­',0,'ç­‰å¾…åˆ°è¾¾','2026-09-05 09:03:14','2026-09-05 09:03:18',0),(11,'EXP-0060','normal','åˆ°è¾¾åˆ†æ‹¨ä¸­',0,'ç­‰å¾…åˆ†æ‹£','2026-09-05 09:03:14','2026-09-05 09:03:18',0),(12,'EXP-0070','normal','æœ¬åœ°æ´¾é€ä¸­',0,'æ— éœ€å¤„ç†','2026-09-05 09:03:14','2026-09-05 09:03:14',0),(13,'EXP-0080','normal','å·²ç­¾æ”¶',0,'æ— éœ€å¤„ç†','2026-09-05 09:03:14','2026-09-05 09:03:14',0),(14,'CUS-1103','customs_delay','ä¸­è½¬åˆ†æ‹¨æŸ¥éªŒï¼ˆXå…‰/å¼€ç®±ï¼‰ï¼Œæ”¾è¡Œå»¶è¿Ÿ',60,'é…åˆè¡¥å……ä¿¡æ¯','2026-09-05 09:03:14','2026-09-05 09:03:18',0),(15,'CUS-1104','customs_delay','ä¸­è½¬ç½‘ç»œè°ƒæ•´ï¼Œè½¬è¿å‘¨æœŸæ‹‰é•¿',96,'å»ºè®®å…³æ³¨æœ€æ–°æ—¶æ•ˆ','2026-09-05 09:03:14','2026-09-05 09:03:18',0),(16,'EXP-0052','lost','è¿è¾“é€”ä¸­åŒ…è£¹ç ´æŸ/éƒ¨åˆ†ä¸¢å¤±',24,'1.æ ¸å®žç ´æŸæƒ…å†µ 2.åå•†éƒ¨åˆ†é€€æ¬¾æˆ–è¡¥å‘ 3.é€šçŸ¥ä¹°å®¶','2026-09-05 09:03:14','2026-09-05 09:03:14',0),(17,'EXP-0063','returned','æ”¶ä»¶äººæ‹’æ”¶ï¼ŒåŒ…è£¹é€€å›ž',24,'1.è”ç³»ä¹°å®¶ç¡®è®¤åŽŸå›  2.ç¡®è®¤æ˜¯å¦é‡å‘ 3.è·Ÿè¿›é€€å›žç‰©æµ','2026-09-05 09:03:14','2026-09-05 09:03:14',0),(18,'EXP-0072','delivery_failed','åœ°å€é”™è¯¯ï¼Œæ— æ³•æ´¾é€',24,'1.è”ç³»ä¹°å®¶æ ¸å¯¹åœ°å€ 2.æ›´æ–°æ´¾é€åœ°å€ 3.é‡æ–°æ´¾é€','2026-09-05 09:03:14','2026-09-05 09:03:14',0),(19,'EXP-0073','delivery_failed','è¶…æ—¶æœªå–ä»¶ï¼ŒåŒ…è£¹é€€å›žç½‘ç‚¹',24,'1.æé†’ä¹°å®¶å–ä»¶ 2.ç¡®è®¤æ˜¯å¦æ”¹æ´¾ 3.è·Ÿè¿›','2026-09-05 09:03:14','2026-09-05 09:03:14',0);
/*!40000 ALTER TABLE `anomaly_knowledge` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

