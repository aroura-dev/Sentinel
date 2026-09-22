-- sentinel-ms : sentinel_agent（由 模块化单体版 库快照导出生成，勿手改）
CREATE DATABASE IF NOT EXISTS sentinel_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
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
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `agent_name` varchar(32) NOT NULL COMMENT 'Agent 名称',
  `input` text COMMENT '输入参数（JSON）',
  `output` text COMMENT '输出结果（JSON）',
  `tools_called` varchar(256) DEFAULT NULL COMMENT '调用的 Tool 列表（逗号分隔）',
  `token_usage` int(11) DEFAULT NULL COMMENT 'Token 消耗',
  `latency_ms` int(11) DEFAULT NULL COMMENT '耗时（毫秒）',
  `status` varchar(16) NOT NULL DEFAULT 'success' COMMENT '状态：success/failed/timeout/degraded',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_agent_name` (`agent_name`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=500018 DEFAULT CHARSET=utf8mb4 COMMENT='Agent 调用日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `agent_call_log` DISABLE KEYS */;
INSERT INTO `agent_call_log` (`id`,`agent_name`,`input`,`output`,`tools_called`,`token_usage`,`latency_ms`,`status`,`trace_id`,`created_at`,`updated_at`) VALUES (500001,'ContentGenAgent','{\"node\":\"IMPORT_CUSTOMS\",\"language\":\"zh\"}','您的物流状态已更新，请关注最新进展。','queryTemplate',320,1200,'success','SEED-0001','2026-08-29 09:03:15','2026-09-05 09:03:18'),(500002,'ContentGenAgent','{\"node\":\"DELIVERED\",\"language\":\"zh\"}','您的物流状态已更新，请关注最新进展。','queryTemplate',280,900,'success','SEED-0001','2026-08-29 09:03:15','2026-09-05 09:03:18'),(500003,'ContentGenAgent','{\"node\":\"IMPORT_CUSTOMS\",\"language\":\"zh\"}','您的物流状态已更新，请关注最新进展。','queryTemplate',300,1100,'success','SEED-0019','2026-08-30 09:03:15','2026-09-05 09:03:18'),(500004,'AnomalyDiagnoseAgent','{\"type\":\"customs_delay\",\"statusCode\":\"CUS-1102\"}','{\"reason\":\"海关抽检，包裹在目的国海关停留\",\"priority\":\"P1\"}','querySimilarAnomaly,decodeStatusCode',680,3800,'success','SEED-0007','2026-08-31 09:03:15','2026-09-05 09:03:15'),(500005,'WorkorderAgent','{\"anomalyDesc\":\"订单在物流环节发生清关延误异常\"}','{\"type\":\"customs_delay\",\"level\":\"P1\",\"sop\":\"1.联系物流商核实...\"}','classifyAnomaly,querySOP',720,4100,'success','SEED-0007','2026-08-31 09:03:15','2026-09-05 09:03:15'),(500006,'ContentGenAgent','{\"node\":\"CUSTOMS_DELAY\",\"language\":\"zh\"}','您的物流状态已更新，请关注最新进展。','queryTemplate',310,1000,'success','SEED-0007','2026-08-31 09:03:15','2026-09-05 09:03:18'),(500007,'ChannelRouteAgent','{\"priority\":\"P1\",\"buyerId\":\"buyer_007\"}','{\"primary\":\"sms\",\"backup\":\"push\",\"reason\":\"SMS 配额充足\"}','queryChannelHealth,queryUserPreference',540,2900,'success','SEED-0007','2026-08-31 09:03:15','2026-09-05 09:03:15'),(500008,'AnomalyDiagnoseAgent','{\"type\":\"lost\",\"statusCode\":\"EXP-0051\"}','{\"reason\":\"国际运输途中丢件\",\"priority\":\"P0\"}','querySimilarAnomaly',700,3600,'success','SEED-0015','2026-09-02 09:03:15','2026-09-05 09:03:15'),(500009,'CsRouteAgent','{\"message\":\"我的包裹到哪了？\",\"buyerId\":\"buyer_005\"}','{\"intent\":\"query_track\",\"route\":\"auto\",\"reply\":\"请提供您的订单号...\"}','queryTrack',430,2400,'success','SEED-CS-1','2026-09-03 09:03:15','2026-09-05 09:03:15'),(500010,'ContentGenAgent','{\"node\":\"IMPORT_CUSTOMS\",\"language\":\"zh\"}','您的物流状态已更新，请关注最新进展。','queryTemplate',290,950,'success','SEED-0022','2026-09-03 09:03:15','2026-09-05 09:03:18'),(500011,'UnsubscribePredictAgent','{\"buyerId\":\"buyer_010\",\"node\":\"DELIVERY_FAILED\"}','{\"probability\":0.31,\"advice\":\"reduce\"}','queryUserNotifyHistory',510,2700,'success','SEED-0010','2026-09-04 09:03:15','2026-09-05 09:03:15'),(500012,'ContentGenAgent','{\"node\":\"DELIVERY_FAILED\",\"language\":\"zh\"}','您的物流状态已更新，请关注最新进展。','queryTemplate',300,1050,'degraded','SEED-0010','2026-09-04 09:03:15','2026-09-05 09:03:18'),(500013,'AnomalyDiagnoseAgent','{\"type\":\"delivery_failed\",\"statusCode\":\"EXP-0071\"}','{\"reason\":\"买家不在家\",\"priority\":\"P2\"}','querySimilarAnomaly',660,3400,'success','SEED-0010','2026-09-04 09:03:15','2026-09-05 09:03:15'),(500014,'CsRouteAgent','{\"message\":\"包裹一直没到，我要投诉\",\"buyerId\":\"buyer_010\"}','{\"intent\":\"complaint\",\"route\":\"human\",\"reply\":\"已为您转接人工客服\"}','transferHuman',390,2100,'success','SEED-CS-2','2026-09-05 03:03:15','2026-09-05 09:03:15'),(500015,'AnomalyDiagnoseAgent','{\"anomalyType\":\"delivery_failed\",\"statusCode\":\"EXP-0071\",\"orderInfo\":\"{\\\"id\\\":42,\\\"order_no\\\":\\\"OMT-TMS-SEED-0010\\\",\\\"buyer_id\\\":\\\"孙丽\\\",\\\"buyer_phone\\\":\\\"15813328878\\\",\\\"buyer_language\\\":\\\"zh\\\",\\\"merchant_id\\\":2,\\\"merchant_name\\\":\\\"义乌百灵贸易\\\",\\\"destination_country\\\":\\\"GD\\\",\\\"current_node\\\":\\\"DELIVERY_FAILED\\\",\\\"review_status\\\":\\\"APPROVED\\\",\\\"channel_id\\\":1,\\\"carrier_id\\\":1,\\\"warehouse_id\\\":2,\\\"items_json\\\":\\\"[{\\\\\\\"sku\\\\\\\":\\\\\\\"SKU-BL-001\\\\\\\",\\\\\\\"product_id\\\\\\\":4,\\\\\\\"qty\\\\\\\":2,\\\\\\\"unit_weight_kg\\\\\\\":0.6,\\\\\\\"unit_volume_l\\\\\\\":2.2,\\\\\\\"unit_declared_value\\\\\\\":120,\\\\\\\"currency\\\\\\\":\\\\\\\"CNY\\\\\\\"}]\\\",\\\"declared_value\\\":240.00,\\\"declared_currency\\\":\\\"CNY\\\",\\\"freight_cost\\\":65.00,\\\"freight_currency\\\":\\\"CNY\\\",\\\"promise_eta\\\":\\\"2026-09-04 09:03:15\\\",\\\"sla_status\\\":\\\"BREACHED\\\",\\\"waybill_no\\\":\\\"WB-RR-SEED-0010\\\",\\\"buyer_address\\\":\\\"广东省广州市天河区华穗路88号\\\",\\\"buyer_city\\\":\\\"广州\\\",\\\"buyer_postal\\\":\\\"510623\\\",\\\"created_at\\\":\\\"2026-08-24 09:03:15\\\",\\\"updated_at\\\":\\\"2026-09-05 09:03:18\\\",\\\"is_deleted\\\":0}\"}','{\"priority\":\"P1\",\"reason\":\"本地派送失败（买家不在家）\",\"suggestion\":\"1.联系买家预约派送时间 2.重新安排派送\"}','queryKnowledge',1673,2818,'success','OMT-TMS-SEED-0010|DELIVERY_FAILED|zh|buyer','2026-09-05 09:04:18','2026-09-05 09:04:18'),(500016,'ContentGenAgent','{\"node\":\"DELIVERY_FAILED\",\"language\":\"zh\",\"productInfo\":\"商品信息\",\"orderNo\":\"OMT-TMS-SEED-0010\"}','\"物流派送未成功，订单OMT-TMS-SEED-0010的商品已暂存网点，稍后将重新安排配送，请留意后续通知。\"',NULL,342,1138,'success','OMT-TMS-SEED-0010|DELIVERY_FAILED|zh|buyer','2026-09-05 09:04:19','2026-09-05 09:04:19'),(500017,'WorkorderAgent','{\"anomalyDesc\":\"订单在物流环节发生delivery_failed异常（节点 DELIVERY_FAILED）\",\"orderInfo\":\"OMT-TMS-SEED-0010\"}','{\"level\":\"P1\",\"sop\":\"1. 立即联系承运商核实失败原因（如收件人拒收、地址错误、电话不通、无人签收等）；2. 核对订单收件信息准确性，确认是否需更新或补发；3. 若属可重派场景，48小时内安排重新配送；4. 若属拒收或客户主动取消，触发退货流程并同步ERP系统；5. 向客户发送致歉及解决方案短信/站内信；6. 工单闭环前需上传承运商反馈截图及处理结果说明。\",\"type\":\"delivery_failed\"}','getExistingWorkorder,queryKnowledge',1137,5235,'success','OMT-TMS-SEED-0010|DELIVERY_FAILED|zh|buyer','2026-09-05 09:04:24','2026-09-05 09:04:24');
/*!40000 ALTER TABLE `agent_call_log` ENABLE KEYS */;
DROP TABLE IF EXISTS `anomaly_knowledge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `anomaly_knowledge` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `status_code` varchar(64) NOT NULL COMMENT '物流商状态码',
  `type` varchar(32) NOT NULL COMMENT '异常类型',
  `description` text NOT NULL COMMENT '中文描述',
  `avg_duration_hours` int(11) DEFAULT NULL COMMENT '平均处理时长（小时）',
  `suggestion` text COMMENT '处理建议',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_status_code` (`status_code`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COMMENT='异常知识库表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `anomaly_knowledge` DISABLE KEYS */;
INSERT INTO `anomaly_knowledge` VALUES (1,'CUS-1102','customs_delay','中转分拨环节滞留，包裹停留时间较长',48,'联系承运商加快处理','2026-09-05 09:03:12','2026-09-05 09:03:18',0),(2,'CUS-1105','customs_delay','中转信息缺失，需补充面单信息',72,'补充面单/地址信息','2026-09-05 09:03:12','2026-09-05 09:03:18',0),(3,'EXP-0051','lost','干线运输途中丢件',0,'联系承运商核查','2026-09-05 09:03:12','2026-09-05 09:03:18',0),(4,'EXP-0062','returned','包裹退回发件地',0,'1.核实退回原因 2.通知买家 3.跟进退款','2026-09-05 09:03:12','2026-09-05 09:03:12',0),(5,'EXP-0071','delivery_failed','本地派送失败（买家不在家）',24,'1.联系买家预约派送时间 2.重新安排派送','2026-09-05 09:03:12','2026-09-05 09:03:12',0),(6,'EXP-0010','normal','下单，包裹创建',0,'无需处理','2026-09-05 09:03:14','2026-09-05 09:03:14',0),(7,'EXP-0020','normal','仓库出库',0,'无需处理','2026-09-05 09:03:14','2026-09-05 09:03:14',0),(8,'EXP-0030','normal','国内揽收成功',0,'无需处理','2026-09-05 09:03:14','2026-09-05 09:03:14',0),(9,'EXP-0040','normal','中转分拨完成',0,'等待发运','2026-09-05 09:03:14','2026-09-05 09:03:18',0),(10,'EXP-0050','normal','干线运输途中',0,'等待到达','2026-09-05 09:03:14','2026-09-05 09:03:18',0),(11,'EXP-0060','normal','到达分拨中',0,'等待分拣','2026-09-05 09:03:14','2026-09-05 09:03:18',0),(12,'EXP-0070','normal','本地派送中',0,'无需处理','2026-09-05 09:03:14','2026-09-05 09:03:14',0),(13,'EXP-0080','normal','已签收',0,'无需处理','2026-09-05 09:03:14','2026-09-05 09:03:14',0),(14,'CUS-1103','customs_delay','中转分拨查验（X光/开箱），放行延迟',60,'配合补充信息','2026-09-05 09:03:14','2026-09-05 09:03:18',0),(15,'CUS-1104','customs_delay','中转网络调整，转运周期拉长',96,'建议关注最新时效','2026-09-05 09:03:14','2026-09-05 09:03:18',0),(16,'EXP-0052','lost','运输途中包裹破损/部分丢失',24,'1.核实破损情况 2.协商部分退款或补发 3.通知买家','2026-09-05 09:03:14','2026-09-05 09:03:14',0),(17,'EXP-0063','returned','收件人拒收，包裹退回',24,'1.联系买家确认原因 2.确认是否重发 3.跟进退回物流','2026-09-05 09:03:14','2026-09-05 09:03:14',0),(18,'EXP-0072','delivery_failed','地址错误，无法派送',24,'1.联系买家核对地址 2.更新派送地址 3.重新派送','2026-09-05 09:03:14','2026-09-05 09:03:14',0),(19,'EXP-0073','delivery_failed','超时未取件，包裹退回网点',24,'1.提醒买家取件 2.确认是否改派 3.跟进','2026-09-05 09:03:14','2026-09-05 09:03:14',0);
/*!40000 ALTER TABLE `anomaly_knowledge` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

