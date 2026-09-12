# sentinel-ms

**Sentinel · 物流异常主动感知与 AI 自动闭环处置平台（微服务版）**

[![Java](https://img.shields.io/badge/Java-17%2F21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2021-6DB33F)](https://spring.io/projects/spring-cloud)
[![Kafka](https://img.shields.io/badge/Kafka-2.6-231F20?logo=apachekafka)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-6-DC382D?logo=redis&logoColor=white)](https://redis.io/)

由同仓库的单体版演进而来：同一套业务代码按域拆成可独立部署的进程，每个服务独享一个 MySQL 库，服务间用 **REST / Kafka 事件** 通信；AI 文案与消息触达都已从「进程内调用」改为「跨服务调用」，闭环贯穿 3 个物理库。

技术基线：**Spring Boot 2.7.18 + Spring Cloud 2021.0.x**（javax 兼容、可跑 JDK21）、LangChain4j + DashScope(Qwen)。

> 设计决策/代码规则（小·高内聚·低耦合）见 **`DESIGN.md`**；持续集成见 **`.github/workflows/ci.yml`**（build→镜像→起栈→smoke→拆栈）。

## 架构总览

```
浏览器 / 客户端
   │  (统一入口 :8080)
┌──┴─────────────── gateway ────────────────┐
│  Redis 会话校验 → 注入 X-User-Name/Role → 路由   │
└──┬──────────┬──────────┬──────────┬───────┘
 auth-service  msg-service logistics-svc agent-service
  :8081          :8082       :8083       :8084
 sentinel_auth    sentinel_msg  sentinel_logistics sentinel_agent
 (3 表)         (4 表)       (19 表)         (2 表)
   各服务独立 MySQL 容器(宿主 33061~33064)
   redis :6381(会话/去重) · kafka :29092(消息管道) · SMS stub :18999
```

| 服务 | 端口 | 库 | 说明 |
|---|---|---|---|
| **gateway** | 8080 | — | Spring Cloud Gateway：认证(token→X-User 头)+路由 |
| **auth-service** | 8081 | sentinel_auth | 登录/登出/me，Redis 会话 `sentinel:token:` |
| **msg-service** | 8082 | sentinel_msg | 消息发送引擎：/send → Kafka → SmsHandler → sms_record |
| **logistics-service** | 8083 | sentinel_logistics | 订单/通知闭环编排；调 agent(msg) 生成(发送) |
| **agent-service** | 8084 | sentinel_agent | ContentGenAgent 生成文案 + agent_call_log |

业务闭环（验收主线）：
`logistics 触发通知 → agent 生成文案(写 agent_call_log) → notification_record(PENDING) → msg /send → Kafka → sms_record(SENT) → 回写 notification_record(SENT)`
一次触发在 **sentinel_logistics / sentinel_agent / sentinel_msg 三库各落一行**，凭 `trace_id = orderNo|node|lang|role` 可关联。无 DashScope Key 时 agent 自动降级默认文案并记 `degraded`，闭环不中断。

## 实测数据

> 本机 Docker 本地压测，不代表生产环境。

| 场景 | 结果 |
|---|---:|
| 网关高并发闭环 | 200 并发、900 请求，HTTP 900/900 成功 |
| 峰值 QPS | 74.35 |
| 端到端 P95 | 4.87s |
| 最终闭环 | 通知 1000/1000 SENT，短信 1000/1000 成功 |
| Kafka consumer lag | 0 |
| Agent 服务停机 | 100/100 请求成功降级，通知和短信均 100/100 完成 |
| 核心后端测试 | 19/19 通过 |

完整命令与原始结果见 [Benchmarks](docs/BENCHMARKS.md)。

## 快速启动（推荐：一条命令）

```bash
bash infra/tools/up.sh          # 构建 jar → 镜像 → 起整套(13 容器) → 等网关 → smoke 验收
bash infra/tools/down.sh        # 拆栈(保留数据卷)；加 -v 连卷一起删
```

下面是等价的手动步骤（供细看）：

前置：JDK21、Maven、Docker Desktop。需要自定义 Maven settings 时设置 `MAVEN_SETTINGS=/path/to/settings.xml`。

```bash
# 1) 构建全部服务 jar
mvn -s <settings> -N install -q
mvn -s <settings> -pl services/auth-service,services/msg-service,services/logistics-service,services/agent-service,gateway -am package -DskipTests
#    产物按 Dockerfile 约定放入 dist/
mkdir -p dist && cp services/{auth,msg,logistics,agent}-service/target/*.jar dist/ && cp gateway/target/sentinel-gateway.jar dist/

# 2) 构建镜像并一键起整套（4×MySQL + redis + kafka + sms-stub + 5 服务）
cd infra/docker
docker compose -f compose.infra.yml build
docker compose -f compose.infra.yml up -d

# 3) 查看
docker compose -f compose.infra.yml ps      # 13 个容器 healthy/up
```

容器内服务间按容器名互连（`mysql-auth/redis/kafka/sms-stub/auth-service/...`），宿主仅暴露 8080(gateway) 与各库调试端口。

> 备选「宿主 jar 联调」：各服务 `application.yml` 默认连宿主 127.0.0.1 的 33061~33064 / redis 6381 / kafka 29092 / 服务地址 127.0.0.1:808x，可 `java -jar` 逐起（与容器化二选一，勿同起占用 8080-8084）。

## 验收

```bash
bash infra/tools/smoke_closed_loop.sh    # 经网关登录→触发通知→断言三库落行 → SMOKE PASS
```

手工：
```bash
# 登录（经网关）
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -d "username=admin&password=Admin@123" | grep -oE '"token":"[a-f0-9]+"' | cut -d'"' -f4)
# 触发通知（logistics 编排 → agent → msg）
curl -s -X POST "http://localhost:8080/api/logistics/notify/send?orderNo=<订单号>&node=IN_TRANSIT&role=buyer&channel=sms" -H "Authorization: Bearer $TOKEN"
# 无 token → 401；直连服务无身份头 → 403
```
多角色账号：`admin/Admin@123`、`operator/Operator@123`、`cs/Cs@123`、`merchant/Merchant@123`、`finance/Finance@123`。

## 浏览器(前后端)

| 入口 | 说明 |
|---|---|
| http://localhost:5173 | **原版完整 Vue UI**（登录→看板→工单/TMS/通知…），经网关：已迁移片走微服务、其余接口回源 legacy 后端 |
| http://localhost:5175 | 极简演示台（登录 / MERCHANT 隔离订单 / 触发闭环，纯微服务切片，不依赖 legacy） |

原版 UI 需先把 legacy 后端起来（strangler 回源依赖）：
```bash
bash infra/tools/up_legacy.sh        # :8090 单体(全量接口, 与 auth 共享 Redis token)
cd frontend-vue && npm ci && npm run dev   # :5173(代理 /api -> 网关 :8080)
```
> 设计说明：网关是 **strangler**——具体前缀已迁微服务(如 `/api/logistics/orders`, `/api/auth/login`)优先，其余 `/api/**` 回源 legacy。迁移进度 = 把一个域的前缀从 legacy 切到对应微服务。详见 `DESIGN.md §7`。

## 目录

```
common / support / service-api / handler           共享组件
sentinel-logistics / sentinel-agent              业务域模块
shared-web                                        共享 Web 层：身份头过滤与角色校验
services/{auth,msg,logistics,agent}-service        4 个独立可执行服务
gateway                                            统一网关
infra/docker/compose.infra.yml                     基础设施编排(4 MySQL/redis/kafka)
infra/mysql/<svc>/init/*.sql                       各服务分库 initdb（由 模块化单体版 快照导出）
infra/tools/smoke_closed_loop.sh                   端到端验收脚本
```

## 已提交里程碑
- `4f5e40d` 骨架 + 分库 + auth/msg 两个服务跑通
- `785f991` logistics/agent，四服务三库闭环
- `a27e7c2` 网关认证+路由，端到端验收通过

## 后续路线
阶段2：TMS 全量控制器收编 + MERCHANT 行级隔离验收；阶段3：闭环事件化；阶段4：Nacos/SkyWalking/CI/k8s、全容器化 compose、对照源 oracle 回放。
