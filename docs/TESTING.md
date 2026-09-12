# Sentinel 测试文档

> 测试覆盖服务边界、异步链路、降级行为和最终业务状态。

## 1. 测试分层

| 层级 | 工具 | 关注点 | 是否需要完整基础设施 |
|---|---|---|---|
| 单元测试 | JUnit 5、Mockito | 状态机、Agent 降级、发送编排 | 否 |
| 前端单元测试 | Vitest、Pinia | 登录态、角色、localStorage/sessionStorage | 否 |
| 服务打包测试 | Maven | 共享模块与四个服务能否编译打包 | 否 |
| 端到端 Smoke | Bash、curl、MySQL client | 经 Gateway 登录、触发通知、三库落行 | 是 |
| 故障注入 | Docker + 压测客户端 | Agent 停机、模板降级、链路不中断 | 是 |
| 性能基准 | JDK HttpClient 压测工具 | QPS、P50/P95/P99、Kafka lag | 是 |

## 2. 后端单元测试

首次运行先安装父 POM：

```bash
cd microservices
mvn -B -ntp -N install
```

运行微服务核心测试：

```bash
mvn -B -ntp -pl sentinel-service-api-impl,sentinel-logistics,sentinel-agent -am test
```

当前基线：

| 模块 | 测试数 | 覆盖内容 |
|---|---:|---|
| `sentinel-service-api-impl` | 2 | 发送服务编排 |
| `sentinel-logistics` | 12 | 物流节点、状态迁移、Mock 轨迹、路由脚本 |
| `sentinel-agent` | 5 | 诊断、文案、客服、工单 Agent 降级 |
| 合计 | 19 | 0 failures，0 errors |

运行全部模块：

```bash
mvn -B -ntp test
```

构建四服务 jar：

```bash
mvn -B -ntp \
  -pl services/auth-service,services/msg-service,services/logistics-service,services/agent-service,gateway \
  -am package -DskipTests
```

## 3. 前端单元测试

```bash
cd microservices/frontend-vue
npm ci
npm test
npm run build
```

当前覆盖 4 项：

- `setAuth` 写入 token / username / role / nickname。
- 未传角色时保持向后兼容。
- `logout` 清理 localStorage 会话键。
- `remember=false` 只写 sessionStorage，刷新后仍可恢复。

## 4. 端到端 Smoke

启动完整环境：

```bash
cd microservices
bash infra/tools/up.sh
```

执行验收：

```bash
bash infra/tools/smoke_closed_loop.sh
```

CI 或无宿主机 MySQL 客户端时：

```bash
SMOKE_DOCKER=1 bash infra/tools/smoke_closed_loop.sh
```

Smoke 实际执行：

1. 经 Gateway 登录 `admin / Admin@123` 获取 token。
2. 从 `sentinel_logistics` 选择无买家通知、手机号为数字格式的订单。
3. 调用通知接口，最多重试 3 次以容忍容器重启后的消费就绪竞态。
4. 断言：
   - `notification_record.status = SENT`
   - `agent_call_log.status = success 或 degraded`
   - `sms_record.status = 10`
5. 全部满足输出 `SMOKE PASS`，否则退出码非 0。

## 5. 故障与降级测试

### 模型失败降级

设置空 `DASHSCOPE_API_KEY`，触发通知：

- `agent_call_log.status = degraded`
- `notification_record.status = SENT`
- 链路使用默认模板继续执行

### Agent 服务停机

在 Agent 服务不可用时触发通知，验证：

- 请求仍成功受理。
- 通知和短信记录仍然完成。
- 失败只影响模型增强能力，不阻塞业务闭环。

对应实测结果见 [`../microservices/docs/BENCHMARKS.md`](../microservices/docs/BENCHMARKS.md)。

### 鉴权边界

- 未携带 token 经 Gateway 访问：预期 `401`。
- 绕过 Gateway 直连服务且缺少身份头：预期 `403`。

## 6. CI 映射

当前仓库级 CI 中的微服务测试命令：

```bash
mvn -B -ntp -N install
mvn -B -ntp -pl sentinel-logistics,sentinel-agent,sentinel-service-api-impl -am test
```

微服务项目内保留独立 CI 模板：

```text
microservices/.github/workflows/ci.yml
```

该模板适用于将 `microservices/` 独立成仓库后的完整流程：

```text
build 全部服务 jar
→ 构建镜像
→ 启动完整 Compose
→ 等待 Gateway
→ 执行 SMOKE_DOCKER=1 smoke
→ 拆除容器
```

## 7. 测试数据与隔离

- Auth / Msg / Logistics / Agent 使用四个独立 MySQL 数据库。
- 宿主机调试端口为 `33061` 至 `33064`。
- Redis 使用 `6381`，Kafka 使用 `29092`。
- Smoke 会动态选择订单并生成随机节点，避免依赖固定测试数据。
- 清空测试数据卷：

```bash
cd microservices
bash infra/tools/down.sh -v
```

## 8. 新增测试约定

- Java 测试类以 `Test` 结尾，放在对应模块的 `src/test/java`。
- 单元测试不得依赖真实模型、真实短信或外部网络。
- 外部依赖使用 Mockito 或本地 Stub。
- 前端测试使用 `*.spec.js`，运行在 happy-dom 环境。
- Smoke 脚本必须可重复执行，并通过重试容忍容器启动竞态。
- 测试只声明实际覆盖内容，不把本地 smoke 说成生产可用性验证。

## 9. 已知测试边界

- 未覆盖网络分区、多实例租约锁、跨机房容灾。
- 未验证真实短信运营商回执。
- 未执行长时间稳定性和生产级容量测试。
- DashScope 质量评估仅覆盖小规模离线样本。