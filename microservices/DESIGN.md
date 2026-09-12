# Sentinel 设计说明

> 本文记录服务拆分、数据归属、扩展方式和可靠性约束，作为代码实现的设计基线。

## 1. 设计原则

- **领域边界清晰**：鉴权、物流、消息和 Agent 各司其职，不把不同业务域塞进同一服务。
- **数据归属明确**：每个服务只写自己的 MySQL 数据库，不跨库直接查询。
- **契约优先**：服务之间通过 REST 或 Kafka 事件通信，调用方只依赖接口，不依赖对方实现。
- **可靠性可验证**：所有关键链路必须能通过 smoke 或自动化测试复现。
- **故障可降级**：外部模型、渠道和下游服务故障时，业务主链路仍需给出确定结果。

## 2. 服务与数据边界

| 服务 | 端口 | 独立数据库 | 职责 |
|---|---:|---|---|
| `gateway` | 8080 | — | 统一路由、token 校验、身份头注入 |
| `auth-service` | 8081 | `sentinel_auth` | 用户、角色、会话 |
| `msg-service` | 8082 | `sentinel_msg` | 消息发送、模板、渠道记录 |
| `logistics-service` | 8083 | `sentinel_logistics` | 订单、轨迹、状态机、工单、通知 |
| `agent-service` | 8084 | `sentinel_agent` | AI 诊断、文案、调用日志 |

共享模块按职责复用：

- `common`：领域模型、枚举、Pipeline 抽象；
- `support`：MQ、Redis、配置和通用工具；
- `handler`：消费、去重、限流和渠道发送；
- `service-api`：发送与撤回领域协议；
- `shared-web`：身份头过滤、角色校验和共享 Web 配置。

## 3. 扩展新服务

新增业务域时按以下顺序实现：

1. 定义服务边界和独立数据库，不直接复用其他服务的数据库对象。
2. 复用对应领域模块，`pom.xml` 只引入必要依赖。
3. 启动类放在独立包中，显式配置组件扫描、实体扫描和 Repository 扫描。
4. 跨服务调用使用薄客户端或 Kafka 事件，并为超时、重试和降级设置明确策略。
5. 在 `infra/tools/smoke_closed_loop.sh` 或对应验收脚本中增加断言。

## 4. 一致性约束

- **业务写入**：先写所属服务数据库，再通过 Outbox 记录待发布事件。
- **事件投递**：Dispatcher 负责将 Outbox 事件发布到 Kafka，并支持重试。
- **消费幂等**：消费端使用事件唯一键去重，重复投递不重复执行副作用。
- **缓存一致性**：Redis 只保存会话、去重窗口和可重建缓存，业务事实以 MySQL 为准。
- **跨服务关联**：使用业务 ID 和 `traceId` 关联日志，不跨库 `JOIN`。

## 5. 验收矩阵

| 验收项 | 验证方式 |
|---|---|
| Gateway 登录与身份传播 | 经 Gateway 登录并访问受保护接口 |
| 通知闭环 | 触发通知，检查 Logistics、Agent、Msg 三个库 |
| 降级能力 | 不配置模型 Key 或停止 Agent 服务后重复触发 |
| 鉴权边界 | 无 token 预期 401，绕过 Gateway 直连预期 403 |
| 容器化启动 | `infra/tools/up.sh` 启动并执行 smoke |
| 核心测试 | `mvn -B -ntp -pl sentinel-logistics,sentinel-agent,sentinel-service-api-impl -am test` |

## 6. Roadmap

1. 完善服务契约和接口兼容性检查。
2. 扩展订单、物流、售后和财务域的闭环覆盖。
3. 接入 Nacos、SkyWalking 和 Kubernetes 等可选基础设施。
4. 增加更多故障注入和长时稳定性测试。