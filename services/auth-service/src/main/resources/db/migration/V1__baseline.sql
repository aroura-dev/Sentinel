-- sentinel-ms : sentinel_auth（由 模块化单体版 库快照导出生成，勿手改）
CREATE DATABASE IF NOT EXISTS sentinel_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
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
DROP TABLE IF EXISTS `sentinel_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sentinel_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(64) NOT NULL COMMENT '登录名',
  `password` varchar(100) NOT NULL COMMENT 'bcrypt 哈希',
  `nickname` varchar(64) DEFAULT NULL COMMENT '显示名',
  `role` varchar(24) NOT NULL COMMENT '角色：ADMIN/OPERATOR/CUSTOMER_SERVICE/MERCHANT/FINANCE',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：1启用 0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COMMENT='Sentinel 用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `sentinel_user` DISABLE KEYS */;
INSERT INTO `sentinel_user` VALUES (1,'张伟','$2b$10$zMgQzt/OgPHd7qZHdqb.LOo8yvm9ePV7nlNlvN1EWGwaZxvA.RkHu','张伟','ADMIN',1,'2026-09-05 09:03:14','2026-09-05 09:03:14',0),(2,'刘洋','$2b$10$bEjPnY/h/XVMiaxzMCIoAe2hHyYeRzlkAdHv9FM2neZzN2lXa8Yu2','刘洋','OPERATOR',1,'2026-09-05 09:03:14','2026-09-05 09:03:14',0),(3,'王芳','$2b$10$Zs8/cCH2E6BBlmqKEcJ1vOm6Vkx8Kk89ufWSlzcY2U12vPE47V0XS','王芳','CUSTOMER_SERVICE',1,'2026-09-05 09:03:14','2026-09-05 09:03:14',0),(4,'陈浩','$2b$10$EFnvExNqe0eMYHay4Ym1RuS9xwcwhCBSd73OJJARy6bzbPJjJrENC','陈浩','MERCHANT',1,'2026-09-05 09:03:14','2026-09-05 09:03:14',0),(5,'赵敏','$2a$10$6kLzPvqf.Ev8jVD.HpYW8ejTSA3Rp3eECIracYmJH8uK1eORQH46K','赵敏','FINANCE',1,'2026-09-05 09:03:15','2026-09-05 09:03:15',0);
/*!40000 ALTER TABLE `sentinel_user` ENABLE KEYS */;
DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(24) NOT NULL COMMENT '角色编码',
  `name` varchar(50) NOT NULL COMMENT '角色名',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '1启用 0停用',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `role` DISABLE KEYS */;
INSERT INTO `role` VALUES (1,'ADMIN','管理员','平台全部权限',1,0,'2026-09-05 09:03:14','2026-09-05 09:03:14'),(2,'OPERATOR','运营','订单履约/运单/售后/运营日常',1,0,'2026-09-05 09:03:14','2026-09-05 09:03:14'),(3,'CUSTOMER_SERVICE','王芳','售后/工单/物流跟踪/智能客服',1,0,'2026-09-05 09:03:14','2026-09-05 09:03:14'),(4,'MERCHANT','陈浩','下单/查单/自己商品',1,0,'2026-09-05 09:03:14','2026-09-05 09:03:14'),(5,'FINANCE','赵敏','账单/对账/报价',1,0,'2026-09-05 09:03:14','2026-09-05 09:03:14');
/*!40000 ALTER TABLE `role` ENABLE KEYS */;
DROP TABLE IF EXISTS `role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `role_menu` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_code` varchar(24) NOT NULL COMMENT '角色编码',
  `menu_path` varchar(100) NOT NULL COMMENT '可访问菜单路径(对应前端路由 path)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_code`,`menu_path`)
) ENGINE=InnoDB AUTO_INCREMENT=104 DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单授权表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40000 ALTER TABLE `role_menu` DISABLE KEYS */;
INSERT INTO `role_menu` VALUES (1,'ADMIN','/workbench','2026-09-05 09:03:14'),(2,'ADMIN','/dashboard','2026-09-05 09:03:14'),(3,'ADMIN','/tms/seller-dashboard','2026-09-05 09:03:14'),(4,'ADMIN','/tms/fulfillment','2026-09-05 09:03:14'),(5,'ADMIN','/tms/order','2026-09-05 09:03:14'),(6,'ADMIN','/tms/order-review','2026-09-05 09:03:14'),(7,'ADMIN','/tms/order-merge','2026-09-05 09:03:14'),(8,'ADMIN','/tms/waybill','2026-09-05 09:03:14'),(9,'ADMIN','/logistics/track','2026-09-05 09:03:14'),(10,'ADMIN','/tms/sign-back','2026-09-05 09:03:14'),(11,'ADMIN','/tms/after-sale','2026-09-05 09:03:14'),(12,'ADMIN','/workorder','2026-09-05 09:03:14'),(13,'ADMIN','/tms/claim','2026-09-05 09:03:14'),(14,'ADMIN','/tms/bill','2026-09-05 09:03:14'),(15,'ADMIN','/tms/rate','2026-09-05 09:03:14'),(16,'ADMIN','/tms/reconcile','2026-09-05 09:03:14'),(17,'ADMIN','/tms/inventory','2026-09-05 09:03:14'),(18,'ADMIN','/tms/stock-in-out','2026-09-05 09:03:14'),(19,'ADMIN','/tms/stocktake','2026-09-05 09:03:14'),(20,'ADMIN','/tms/risk-alert','2026-09-05 09:03:14'),(21,'ADMIN','/notification/list','2026-09-05 09:03:14'),(22,'ADMIN','/tms/sla-analysis','2026-09-05 09:03:14'),(23,'ADMIN','/tms/carrier-kpi','2026-09-05 09:03:14'),(24,'ADMIN','/tms/merchant','2026-09-05 09:03:14'),(25,'ADMIN','/tms/carrier','2026-09-05 09:03:14'),(26,'ADMIN','/tms/channel','2026-09-05 09:03:14'),(27,'ADMIN','/tms/warehouse','2026-09-05 09:03:14'),(28,'ADMIN','/tms/product','2026-09-05 09:03:14'),(29,'ADMIN','/sys/user','2026-09-05 09:03:14'),(30,'ADMIN','/sys/role','2026-09-05 09:03:14'),(31,'ADMIN','/audit','2026-09-05 09:03:14'),(32,'ADMIN','/tms/api-console','2026-09-05 09:03:14'),(33,'ADMIN','/ai/chat','2026-09-05 09:03:14'),(34,'ADMIN','/ai/analytics','2026-09-05 09:03:14'),(35,'ADMIN','/agent/cs','2026-09-05 09:03:14'),(36,'ADMIN','/config/knowledge','2026-09-05 09:03:14'),(37,'ADMIN','/agent/log','2026-09-05 09:03:14'),(38,'ADMIN','/agent/trace','2026-09-05 09:03:14'),(39,'OPERATOR','/workbench','2026-09-05 09:03:14'),(40,'OPERATOR','/dashboard','2026-09-05 09:03:14'),(41,'OPERATOR','/tms/seller-dashboard','2026-09-05 09:03:14'),(42,'OPERATOR','/tms/fulfillment','2026-09-05 09:03:14'),(43,'OPERATOR','/tms/order','2026-09-05 09:03:14'),(44,'OPERATOR','/tms/order-review','2026-09-05 09:03:14'),(45,'OPERATOR','/tms/order-merge','2026-09-05 09:03:14'),(46,'OPERATOR','/tms/waybill','2026-09-05 09:03:14'),(47,'OPERATOR','/logistics/track','2026-09-05 09:03:14'),(48,'OPERATOR','/tms/sign-back','2026-09-05 09:03:14'),(49,'OPERATOR','/tms/after-sale','2026-09-05 09:03:14'),(50,'OPERATOR','/workorder','2026-09-05 09:03:14'),(51,'OPERATOR','/tms/claim','2026-09-05 09:03:14'),(52,'OPERATOR','/tms/rate','2026-09-05 09:03:14'),(53,'OPERATOR','/tms/inventory','2026-09-05 09:03:14'),(54,'OPERATOR','/tms/stock-in-out','2026-09-05 09:03:14'),(55,'OPERATOR','/tms/stocktake','2026-09-05 09:03:14'),(56,'OPERATOR','/tms/risk-alert','2026-09-05 09:03:14'),(57,'OPERATOR','/notification/list','2026-09-05 09:03:14'),(58,'OPERATOR','/tms/sla-analysis','2026-09-05 09:03:14'),(59,'OPERATOR','/tms/carrier-kpi','2026-09-05 09:03:14'),(60,'OPERATOR','/tms/merchant','2026-09-05 09:03:14'),(61,'OPERATOR','/tms/carrier','2026-09-05 09:03:14'),(62,'OPERATOR','/tms/channel','2026-09-05 09:03:14'),(63,'OPERATOR','/tms/warehouse','2026-09-05 09:03:14'),(64,'OPERATOR','/tms/product','2026-09-05 09:03:14'),(65,'OPERATOR','/tms/api-console','2026-09-05 09:03:14'),(66,'OPERATOR','/ai/chat','2026-09-05 09:03:14'),(67,'OPERATOR','/ai/analytics','2026-09-05 09:03:14'),(68,'CUSTOMER_SERVICE','/workbench','2026-09-05 09:03:14'),(69,'CUSTOMER_SERVICE','/logistics/track','2026-09-05 09:03:14'),(70,'CUSTOMER_SERVICE','/tms/sign-back','2026-09-05 09:03:14'),(71,'CUSTOMER_SERVICE','/tms/after-sale','2026-09-05 09:03:14'),(72,'CUSTOMER_SERVICE','/workorder','2026-09-05 09:03:14'),(73,'CUSTOMER_SERVICE','/tms/claim','2026-09-05 09:03:14'),(74,'CUSTOMER_SERVICE','/tms/inventory','2026-09-05 09:03:14'),(75,'CUSTOMER_SERVICE','/tms/risk-alert','2026-09-05 09:03:14'),(76,'CUSTOMER_SERVICE','/notification/list','2026-09-05 09:03:14'),(77,'CUSTOMER_SERVICE','/ai/chat','2026-09-05 09:03:14'),(78,'CUSTOMER_SERVICE','/agent/cs','2026-09-05 09:03:14'),(79,'MERCHANT','/workbench','2026-09-05 09:03:14'),(80,'MERCHANT','/tms/seller-dashboard','2026-09-05 09:03:14'),(81,'MERCHANT','/tms/fulfillment','2026-09-05 09:03:14'),(82,'MERCHANT','/tms/order','2026-09-05 09:03:14'),(83,'MERCHANT','/tms/waybill','2026-09-05 09:03:14'),(84,'MERCHANT','/logistics/track','2026-09-05 09:03:14'),(85,'MERCHANT','/tms/after-sale','2026-09-05 09:03:14'),(86,'MERCHANT','/workorder','2026-09-05 09:03:14'),(87,'MERCHANT','/tms/inventory','2026-09-05 09:03:14'),(88,'MERCHANT','/tms/risk-alert','2026-09-05 09:03:14'),(89,'MERCHANT','/notification/list','2026-09-05 09:03:14'),(90,'MERCHANT','/tms/product','2026-09-05 09:03:14'),(91,'FINANCE','/workbench','2026-09-05 09:03:14'),(92,'FINANCE','/dashboard','2026-09-05 09:03:14'),(93,'FINANCE','/tms/order','2026-09-05 09:03:14'),(94,'FINANCE','/tms/waybill','2026-09-05 09:03:14'),(95,'FINANCE','/logistics/track','2026-09-05 09:03:14'),(96,'FINANCE','/tms/after-sale','2026-09-05 09:03:14'),(97,'FINANCE','/workorder','2026-09-05 09:03:14'),(98,'FINANCE','/tms/bill','2026-09-05 09:03:14'),(99,'FINANCE','/tms/rate','2026-09-05 09:03:14'),(100,'FINANCE','/tms/reconcile','2026-09-05 09:03:14'),(101,'FINANCE','/tms/inventory','2026-09-05 09:03:14'),(102,'FINANCE','/tms/risk-alert','2026-09-05 09:03:14'),(103,'FINANCE','/notification/list','2026-09-05 09:03:14');
/*!40000 ALTER TABLE `role_menu` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


-- Java AgentOps 页面授权（幂等追加）
INSERT IGNORE INTO role_menu (role_code, menu_path) VALUES
('ADMIN', '/agent/ops'),
('OPERATOR', '/agent/ops');
