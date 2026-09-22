# Sentinel

**物流异常主动感知与 AI 自动闭环处置平台**

[![Java](https://img.shields.io/badge/Java-8_source_%7C_JDK_21_build-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-2.7.18-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2021.0.8-6DB33F)](https://spring.io/projects/spring-cloud)
[![Kafka](https://img.shields.io/badge/Kafka-2.6-231F20?logo=apachekafka)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-6-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![MySQL](https://img.shields.io/badge/MySQL-5.7-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)

系统按鉴权、物流、消息和 Agent 四个业务域拆分独立服务，每个服务拥有独立 MySQL 数据库。Gateway 统一入口，服务之间通过 REST 与 Kafka 通信。

## 架构

```text
Browser / API Client
        │
        ▼
Gateway :8080
  │ token 校验 + X-User 身份头注入 + 路由
  ├── Auth Service :8081        sentinel_auth
  ├── Logistics Service :8083  sentinel_logistics
  │       ├── Agent Service :8084   sentinel_agent
  │       └── Msg Service :8082     sentinel_msg
  │                 └── Kafka :29092
  │                         └── SMS Stub :18999
  └── Redis :6381               会话与去重
```

| 服务 | 端口 | 数据库 | 职责 |
|---|---:|---|---|
| `gateway` | 8080 | — | 统一入口、token 校验、身份头注入、路由 |
| `auth-service` | 8081 | `sentinel_auth` | 注册、登录、登出、当前用户 |
| `msg-service` | 8082 | `sentinel_msg` | 消息发送、模板、渠道账号、Kafka 投递 |
| `logistics-service` | 8083 | `sentinel_logistics` | 订单、轨迹、状态机、工单、通知状态 |
| `agent-service` | 8084 | `sentinel_agent` | AI 诊断、文案生成、调用审计 |

完整设计见 [`DESIGN.md`](DESIGN.md)，系统架构见 [`../docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md)。

## 业务闭环

```text
logistics 触发通知
  → agent 生成文案并写 agent_call_log
  → notification_record PENDING
  → msg /send
  → Kafka
  → SMS Stub
  → sms_record SENT
  → 回写 notification_record SENT
```

一次触发会在 `sentinel_logistics`、`sentinel_agent`、`sentinel_msg` 三个数据库分别留下记录，通过 `trace_id` 关联。未配置模型 Key 时，Agent 记录 `degraded` 并返回默认文案，业务闭环继续执行。

## 一键启动

```bash
bash infra/tools/up.sh
```

脚本会构建共享模块和服务 jar、构建镜像、启动完整容器环境，并执行端到端 smoke 验收。

停止环境：

```bash
bash infra/tools/down.sh
```

删除数据卷：

```bash
bash infra/tools/down.sh -v
```

### 端口暴露面

默认起的是**安全形态**：只发布 `8080`（网关）与 `5175`（演示台），业务服务与数据库一律不发布到宿主机。

这不是为了好看 —— 下游服务**无条件信任** `X-User-Name` / `X-User-Role` 头。一旦 `8081-8084` 对宿主机可达，任何人伪造这两个头直连业务服务就能完全绕过网关鉴权；`agent-service` 的 `/internal/**` 与 `msg-service` 的 `/send` 更是连 `/api` 前缀都没有，不在任何鉴权拦截范围内。

本地调试需要直连这些端口（宿主 mysql 客户端、直接调 8081-8084）时：

```bash
DEV_PORTS=1 bash infra/tools/up.sh
```

叠加的 `compose.dev.yml` 会把调试端口加回来，但**只绑定 `127.0.0.1`** —— 同一局域网内的其他机器依然连不上。也可以手动叠加：

```bash
cd infra/docker
docker compose -f compose.infra.yml -f compose.dev.yml up -d
```

> 生产部署只应暴露网关端口；如需对外提供服务，应在网关前加 TLS 终结与网络隔离。

## Smoke 验收

```bash
bash infra/tools/smoke_closed_loop.sh
```

无宿主机 MySQL 客户端时：

```bash
SMOKE_DOCKER=1 bash infra/tools/smoke_closed_loop.sh
```

手工触发闭环：

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -d "username=张伟&password=Admin@123" \
  | grep -oE '"token":"[a-f0-9]+"' | cut -d'"' -f4)

curl -s -X POST \
  "http://localhost:8080/api/logistics/notify/send?orderNo=<订单号>&node=IN_TRANSIT&role=buyer&channel=sms" \
  -H "Authorization: Bearer $TOKEN"
```

默认账号：`张伟 / Admin@123`。

## 前端

| 入口 | 说明 |
|---|---|
| http://localhost:5175 | 管理控制台，经 Gateway 调用后端接口 |

## 测试

```text
sentinel-service-api-impl: 2/2
sentinel-logistics: 12/12
sentinel-agent: 5/5
合计: 19/19
```

完整测试说明见 [`../docs/TESTING.md`](../docs/TESTING.md)。

## 目录

```text
common / support / service-api / handler            共享组件
sentinel-logistics / sentinel-agent                 业务域模块
shared-web                                          身份头过滤与角色校验
services/{auth,msg,logistics,agent}-service         四个独立服务
gateway                                             统一入口
infra/docker/compose.infra.yml                      完整容器编排
infra/mysql/<svc>/init/*.sql                        各服务分库初始化
infra/tools/smoke_closed_loop.sh                    端到端验收脚本
```

## 已知边界

- 本地压测结果不代表生产容量。
- 未覆盖网络分区、多实例租约锁、真实短信回执和长时间稳定性。
- 服务契约、发布治理和分布式追踪平台仍可继续完善。