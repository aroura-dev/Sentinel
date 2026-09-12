# Sentinel 部署指南

## 1. 环境要求

| 组件 | 要求 |
|---|---|
| Docker | Docker Desktop 或 Docker Engine，支持 Compose v2 |
| Java | JDK 21 |
| Maven | 3.9+ |
| 可选 | 自定义 Maven settings：`MAVEN_SETTINGS=/path/to/settings.xml` |
| 可选 | `DASHSCOPE_API_KEY`，未配置时 Agent 自动降级 |

检查环境：

```bash
docker version
docker compose version
java -version
mvn -version
```

## 2. 一键启动

```bash
cd microservices
bash infra/tools/up.sh
```

脚本自动完成：

1. 构建共享模块和四个服务的 jar。
2. 构建后端和前端镜像。
3. 启动 4 个 MySQL、Redis、Kafka、SMS Stub 和业务服务。
4. 等待 Gateway 可用。
5. 执行端到端 smoke 验收。

停止环境：

```bash
cd microservices
bash infra/tools/down.sh
```

删除数据卷：

```bash
cd microservices
bash infra/tools/down.sh -v
```

## 3. 服务与端口

| 服务 | 地址 | 说明 |
|---|---|---|
| Gateway | http://localhost:8080 | 统一入口、token 校验、路由 |
| Auth Service | http://localhost:8081 | 注册、登录、登出、当前用户 |
| Msg Service | http://localhost:8082 | 消息发送与 Kafka 投递 |
| Logistics Service | http://localhost:8083 | 订单、轨迹、工单、通知闭环 |
| Agent Service | http://localhost:8084 | AI 诊断、文案、调用审计 |
| 微服务演示台 | http://localhost:5175 | 经 Gateway 访问后端 |
| SMS Stub | http://localhost:18999 | 本地短信渠道桩 |
| Auth MySQL | `localhost:33061` | `sentinel_auth` |
| Msg MySQL | `localhost:33062` | `sentinel_msg` |
| Logistics MySQL | `localhost:33063` | `sentinel_logistics` |
| Agent MySQL | `localhost:33064` | `sentinel_agent` |
| Redis | `localhost:6381` | 登录会话与去重 |
| Kafka | `localhost:29092` | 异步消息管道 |

## 4. 手动启动

```bash
cd microservices

# 安装共享父 POM
mvn -s "${MAVEN_SETTINGS:-}" -N install

# 构建服务
mvn -s "${MAVEN_SETTINGS:-}" \
  -pl services/auth-service,services/msg-service,services/logistics-service,services/agent-service,gateway \
  -am package -DskipTests

# 准备镜像上下文
mkdir -p dist
cp services/{auth,msg,logistics,agent}-service/target/*.jar dist/
cp gateway/target/sentinel-gateway.jar dist/

# 启动完整基础设施和应用
cd infra/docker
docker compose -f compose.infra.yml up -d --build
```

## 5. 验证闭环

```bash
cd microservices
bash infra/tools/smoke_closed_loop.sh
```

预期结果：

- Gateway 登录成功。
- Logistics 接收通知请求。
- Agent 生成文案；没有 Key 时记录 `degraded` 并返回默认文案。
- Msg 写入 Kafka，SMS Stub 返回成功。
- 三个业务库分别留下记录，通知状态最终为 `SENT`。

手工验证：

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -d "username=admin&password=Admin@123" \
  | grep -oE '"token":"[a-f0-9]+"' | cut -d'"' -f4)

curl -s -X POST \
  "http://localhost:8080/api/logistics/notify/send?orderNo=<订单号>&node=IN_TRANSIT&role=buyer&channel=sms" \
  -H "Authorization: Bearer $TOKEN"
```

## 6. 日志与排查

```bash
cd microservices/infra/docker

docker compose -f compose.infra.yml ps
docker compose -f compose.infra.yml logs -f gateway
docker compose -f compose.infra.yml logs -f auth-service
docker compose -f compose.infra.yml logs -f logistics-service
docker compose -f compose.infra.yml logs -f agent-service
docker compose -f compose.infra.yml logs -f msg-service
```

常见问题：

- **401**：Gateway 未收到有效 token，检查登录响应和 `Authorization` 请求头。
- **403**：服务直连时缺少 Gateway 注入的 `X-User-Name` / `X-User-Role`。
- **Agent degraded**：未配置 `DASHSCOPE_API_KEY`，属于预期降级。
- **MySQL 端口冲突**：修改 `infra/docker/compose.infra.yml`，并同步修改服务配置。
- **Kafka 未就绪**：等待容器健康后重试，或查看 `compose.infra.yml` 中 Kafka 日志。

## 7. 数据库初始化

各服务分库脚本位于：

```text
microservices/infra/mysql/auth/init/
microservices/infra/mysql/msg/init/
microservices/infra/mysql/logistics/init/
microservices/infra/mysql/agent/init/
```

MySQL 容器只在数据卷为空时执行 init 脚本。需要重新初始化时使用：

```bash
cd microservices
bash infra/tools/down.sh -v
bash infra/tools/up.sh
```