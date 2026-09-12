# Sentinel 工程证据映射

> 本文用于把项目描述映射到公开代码、测试和 CI 证据。项目公开仓库采用单次初始提交，不提供逐步开发提交历史。

## 1. 业务接口交付

| 业务模块 | 公开入口 | 已实现能力 |
|---|---|---|
| 物流订单 | `austin-web/.../LogisticsController.java` | 订单查询、轨迹查询、节点推进管理 |
| 工单流转 | `austin-web/.../SentinelWorkorderController.java` | 分页、详情、创建、诊断、状态更新、推送、索赔 |
| 通知触达 | `austin-web/.../SentinelNotificationController.java` | 状态统计、分页、详情、发送、重试、状态回写 |
| AgentOps | `austin-web/.../SentinelAgentOpsController.java` | 编排启动、任务列表、链路查询、写步骤审批和拒绝 |

控制器只负责参数和响应，业务规则位于对应 Service，避免把业务逻辑堆在接口层。

## 2. 事务与一致性

- `WorkorderService#updateStatus` 使用 `@Transactional(rollbackFor = Exception.class)`。
- 工单状态迁移由状态机校验，非法迁移直接拒绝。
- 状态更新使用版本号做乐观锁，并发修改时返回冲突而不是覆盖数据。
- 工单状态、审计日志和 Outbox 事件在同一事务边界内写入。
- 重复请求通过唯一业务键和状态幂等判断控制，降低重复通知、重复落库风险。

## 3. 稳定性与可观测性

- 消息按业务、撤回和轨迹拆分消费者组。
- 不同渠道使用独立动态线程池，队列容量和拒绝策略可配置。
- Agent 调用统一记录 traceId、工具、Token、耗时和状态。
- 模型或下游异常时回退模板、转人工或保留人工审批，不阻断主业务链路。

## 4. 测试与 CI

- CI 配置：`.github/workflows/ci.yml`
- Workflow 包含后端测试、前端测试与构建、微服务核心测试。
- 测试覆盖状态机、发送参数校验、幂等控制、异常降级和链路执行。
- 本地运行命令：

```bash
mvn -B -pl austin-web -am test
cd microservices
mvn -B -N install
mvn -B -pl sentinel-logistics,sentinel-agent,austin-service-api-impl -am test
```

## 5. 数据来源

- 压测脚本：`microservices/infra/tools/bench/ClosedLoopLoadTest.java`
- 压测订单：`microservices/infra/tools/seed_benchmark_orders.sql`
- SQL 基准：`microservices/infra/tools/benchmark_outbox_index_setup.sql`
- Redis Lua：`microservices/services/msg-service/src/main/resources/limit.lua`

所有性能数据均为本地 Docker 或隔离基准表结果，不代表生产环境。