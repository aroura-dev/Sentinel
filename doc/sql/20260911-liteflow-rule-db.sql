-- P1: LiteFlow SQL Rule-DB（与 liteflow-rule-db-sql 2.16.1 表结构一致）
-- 应用开启 sentinel.flow.rule-db.auto-init-table 时无需手工执行；
-- 生产环境关闭自动建表时，可先执行本脚本。默认链由应用启动时自动发布。

CREATE TABLE IF NOT EXISTS `lf_chain` (
  `application_name` VARCHAR(64) NOT NULL,
  `chain_id` VARCHAR(128) NOT NULL,
  `namespace` VARCHAR(64) DEFAULT NULL,
  `el_data` TEXT NOT NULL,
  `route_data` TEXT DEFAULT NULL,
  `version` BIGINT NOT NULL DEFAULT 1,
  `content_md5` CHAR(32) NOT NULL,
  `enable` TINYINT NOT NULL DEFAULT 1,
  `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`application_name`, `chain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LiteFlow 链规则';

CREATE TABLE IF NOT EXISTS `lf_script` (
  `application_name` VARCHAR(64) NOT NULL,
  `node_id` VARCHAR(128) NOT NULL,
  `script_name` VARCHAR(128) DEFAULT NULL,
  `script_type` VARCHAR(32) NOT NULL,
  `script_language` VARCHAR(32) DEFAULT NULL,
  `script_data` TEXT NOT NULL,
  `version` BIGINT NOT NULL DEFAULT 1,
  `content_md5` CHAR(32) NOT NULL,
  `enable` TINYINT NOT NULL DEFAULT 1,
  `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`application_name`, `node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LiteFlow 脚本规则';

CREATE TABLE IF NOT EXISTS `lf_change_log` (
  `seq` BIGINT NOT NULL AUTO_INCREMENT,
  `application_name` VARCHAR(64) NOT NULL,
  `target_type` VARCHAR(16) NOT NULL,
  `target_id` VARCHAR(128) NOT NULL,
  `op` VARCHAR(16) NOT NULL,
  `version` BIGINT NOT NULL,
  `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`seq`),
  KEY `idx_app_seq` (`application_name`, `seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LiteFlow 规则变更日志';

CREATE TABLE IF NOT EXISTS `lf_change_lock` (
  `lock_id` TINYINT NOT NULL,
  PRIMARY KEY (`lock_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LiteFlow 规则发布顺序锁';

INSERT IGNORE INTO `lf_change_lock` (`lock_id`) VALUES (1);
