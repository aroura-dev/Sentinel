USE sentinel_logistics;

DROP TABLE IF EXISTS benchmark_digits;
CREATE TABLE benchmark_digits (n TINYINT PRIMARY KEY);
INSERT INTO benchmark_digits VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);

INSERT IGNORE INTO logistics_order
  (order_no, buyer_id, buyer_phone, buyer_language, merchant_id, current_node, is_deleted)
SELECT
  CONCAT('BENCH20260911-', LPAD(a.n*100 + b.n*10 + c.n, 4, '0')),
  CONCAT('bench-user-', a.n*100 + b.n*10 + c.n),
  CONCAT('199', LPAD(a.n*100 + b.n*10 + c.n, 8, '0')),
  'zh',
  1,
  'IN_TRANSIT',
  0
FROM benchmark_digits a
CROSS JOIN benchmark_digits b
CROSS JOIN benchmark_digits c;

DROP TABLE benchmark_digits;
SELECT COUNT(*) AS benchmark_orders
FROM logistics_order
WHERE order_no LIKE 'BENCH20260911-%' AND is_deleted = 0;
