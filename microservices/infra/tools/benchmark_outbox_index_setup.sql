-- 本地 SQL 基准：20 万行 Outbox 扫描
-- 用途：对比无联合索引与 status + next_retry_at 联合索引
USE sentinel_logistics;

DROP TABLE IF EXISTS benchmark_outbox;
CREATE TABLE benchmark_outbox (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL,
  next_retry_at DATETIME NOT NULL,
  payload VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS benchmark_digits;
CREATE TABLE benchmark_digits (n TINYINT PRIMARY KEY);
INSERT INTO benchmark_digits VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);

INSERT INTO benchmark_outbox (event_id, status, next_retry_at, payload)
SELECT
  CONCAT('evt-', a.n, b.n, c.n, d.n, e.n, f.n),
  CASE WHEN MOD(a.n*100000 + b.n*10000 + c.n*1000 + d.n*100 + e.n*10 + f.n, 1000) = 0
       THEN 'NEW' ELSE 'SENT' END,
  DATE_SUB(NOW(), INTERVAL MOD(a.n*31 + b.n*17 + c.n*7 + d.n*3 + e.n*2 + f.n, 2592000) SECOND),
  CONCAT('payload-', a.n, b.n, c.n, d.n, e.n, f.n)
FROM benchmark_digits a
CROSS JOIN benchmark_digits b
CROSS JOIN benchmark_digits c
CROSS JOIN benchmark_digits d
CROSS JOIN benchmark_digits e
CROSS JOIN benchmark_digits f
LIMIT 200000;

DROP TABLE benchmark_digits;

UPDATE benchmark_outbox
SET status = CASE WHEN MOD(id, 1000) = 0 THEN 'NEW' ELSE 'SENT' END,
    next_retry_at = CASE WHEN MOD(id, 1000) = 0 THEN DATE_SUB(NOW(), INTERVAL 1 DAY) ELSE next_retry_at END;

ANALYZE TABLE benchmark_outbox;
SELECT COUNT(*) AS rows_total,
       SUM(status='NEW') AS rows_new,
       SUM(status='NEW' AND next_retry_at <= NOW()) AS rows_new_due
FROM benchmark_outbox;
