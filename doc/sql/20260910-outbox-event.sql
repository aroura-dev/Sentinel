-- P0-2: 发件箱事件表 + 消费幂等表
CREATE TABLE IF NOT EXISTS `outbox_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `event_id` VARCHAR(64) NOT NULL COMMENT '事件唯一ID（消费幂等键）',
    `event_type` VARCHAR(64) NOT NULL COMMENT '事件类型',
    `aggregate_type` VARCHAR(32) NOT NULL DEFAULT 'workorder' COMMENT '聚合类型',
    `aggregate_id` VARCHAR(64) NOT NULL COMMENT '聚合ID（订单号/工单ID）',
    `payload` TEXT NOT NULL COMMENT '事件负载(JSON)',
    `status` VARCHAR(16) NOT NULL DEFAULT 'NEW' COMMENT 'NEW/SENT/DEAD',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    `next_retry_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次重试时间',
    `last_error` VARCHAR(512) DEFAULT NULL COMMENT '最近一次错误',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    KEY `idx_status_retry` (`status`, `next_retry_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发件箱事件表';

CREATE TABLE IF NOT EXISTS `consumed_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `event_id` VARCHAR(64) NOT NULL COMMENT '事件唯一ID',
    `consumer` VARCHAR(64) NOT NULL COMMENT '消费者标识',
    `consumed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消费时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_consumer` (`event_id`, `consumer`),
    KEY `idx_consumed_at` (`consumed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费幂等表';