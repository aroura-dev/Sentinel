# Outbox SQL 基准

## 环境

- MySQL 5.7.44
- 20 万行隔离基准表
- 约 200 条到期 `NEW` 记录
- 连续运行 3 次，取中位数

## 优化前

```sql
SELECT id
FROM benchmark_outbox
WHERE status='NEW' AND next_retry_at <= NOW()
ORDER BY next_retry_at, id
LIMIT 100;
```

- 执行计划：`ALL + Using filesort`
- EXPLAIN 预估行数：199408
- 耗时范围：109–149ms
- 中位耗时：约 121ms

## 优化后

```sql
KEY idx_status_retry (status, next_retry_at, id)
```

- 执行计划：`range + Using index`
- EXPLAIN 预估行数：200
- 耗时范围：1.36–1.53ms
- 中位耗时：约 1.4ms

## 结论

在隔离基准数据上，预估扫描行数从 199408 降至 200，耗时从 109–149ms 降至 1.36–1.53ms，约提升一个数量级。该结果仅适用于本地隔离基准，不代表生产数据收益。

查询排序和索引定义已同步到：

- `sentinel-logistics/src/main/java/.../OutboxEventDao.java`
- `doc/sql/20260910-outbox-event.sql`

## 2026-10-09 复测

在 Docker MySQL 5.7.44 中重新生成 20 万行隔离基准表，连续运行 3 次并取中位数。

### 优化前

- 执行计划：`ALL + Using filesort`
- `EXPLAIN` 预估行数：199408
- 耗时：49.299ms、39.514ms、39.960ms
- 中位耗时：39.960ms

### 优化后

- 执行计划：`range + Using index`
- `EXPLAIN` 预估行数：200
- 耗时：1.3395ms、0.33725ms、0.64875ms
- 中位耗时：0.64875ms

### 复测结论

- 预估扫描行数减少：`(199408 - 200) / 199408 = 99.8997%`
- 中位查询耗时减少：`(39.960 - 0.64875) / 39.960 = 98.38%`

该结果来自本地隔离基准表，只说明该查询在该数据分布和索引顺序下能够稳定命中覆盖索引，不代表生产环境整体收益。
