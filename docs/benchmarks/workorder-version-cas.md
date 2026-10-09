# 工单 version CAS 并发验证

## 环境

- 日期：2026-10-09
- MySQL 5.7.44（Docker `sentinel-mysql`）
- 数据库：`sentinel`
- 表：`workorder`

## 验证目的

验证工单状态更新同时校验 `status` 和 `version` 时，同一版本的并发请求只有一个能够更新成功。

```sql
UPDATE workorder
SET status = ?, version = version + 1, updated_at = CURRENT_TIMESTAMP
WHERE id = ? AND status = ? AND version = ? AND is_deleted = 0;
```

## 测试过程

创建一条测试工单，初始状态为 `OPEN`，版本为 `0`。两个独立连接同时读取相同状态和版本，并通过 `SELECT SLEEP(0.2)` 制造并发窗口，然后执行相同更新。

## 实测结果

| 连接 | 影响行数 | 读取版本 |
|---|---:|---:|
| Worker 1 | 1 | 0 |
| Worker 2 | 0 | 0 |

最终数据库状态：

| 字段 | 值 |
|---|---|
| status | PROCESSING |
| version | 1 |

测试完成后已删除测试工单。

## 结论

两个并发请求携带相同版本时，仅一个请求更新成功，另一个请求影响行数为 0，说明 `status + version` 双重 CAS 能阻止同一版本的并发覆盖。
