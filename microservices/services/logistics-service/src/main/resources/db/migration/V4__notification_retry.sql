
-- 通知补偿所需的重试记账字段。
--
-- 背景：原本 msg-service 不可达时 notification_record 直接置 FAILED 就结束了，
-- 没有任何重投；进程若在 insert(PENDING) 与 dispatch 之间崩溃，记录则永久停在 PENDING。
-- 两者都需要一个定时补偿任务来收口，而重试必须有次数上限与退避，
-- 否则一个持续失败的通知会被无限重投。
--
-- retry_count    已重试次数，达到上限后不再重投，停在 FAILED 等人工介入
-- last_error     最近一次失败原因，便于排查（也让"为什么没发出去"可回答）
-- next_retry_at  下次可重试时间，构成指数退避
ALTER TABLE notification_record
  ADD COLUMN retry_count   INT          NOT NULL DEFAULT 0   COMMENT '已重试次数（补偿任务用）',
  ADD COLUMN last_error    VARCHAR(255) DEFAULT NULL         COMMENT '最近一次失败原因',
  ADD COLUMN next_retry_at DATETIME     DEFAULT NULL         COMMENT '下次可重试时间（退避）';

-- 补偿任务的扫描条件是 status + next_retry_at，建覆盖索引避免每次 tick 全表扫。
CREATE INDEX idx_retry ON notification_record (status, next_retry_at);

-- 说明：本迁移只加列加索引，不改写已有数据。
-- 历史遗留的 FAILED 行 retry_count=0 / next_retry_at=NULL，补偿任务会立即接管重投。
