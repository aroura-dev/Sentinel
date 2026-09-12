# sentinel-ms 设计说明（收口文档）

> 本文档记录**关键决策与规则**，避免随代码膨胀而漂移。完整运维/启动见 `README.md`。

## 1. 目标与原则（用户的硬约束）

把单体 sentinel 演进为微服务产品的过程中，**代码量要小、高内聚、低耦合**。据此落地规则：

- **代码量小**：增量只写必要类；能复用共享模块绝不新造；验收/对拍用轻量脚本。
- **高内聚**：共享概念只放一处——
  - 鉴权/身份/`@RequireRole`/`CurrentUserVO` 等收敛在 **`austin-ms-web`**（X-User 头 Filter + 角色校验 + VO），各服务依赖它，不重复拷贝；
  - austin 引擎/物流域/AI 域分别复用复制来的库模块，不再散装。
- **低耦合**：
  - 服务间只经 **thin REST client / Kafka 事件**（例：`logistics` 只通过 `AgentClient`/`MsgClient` 调 agent/msg，不依赖其库）；
  - **每服务独享 MySQL**，禁止跨库 JOIN —— 跨域读一律「冗余列 / REST / 事件」（例：`merchant.owner_username` 替代 `JOIN sentinel_user`）；
  - **新业务域优先拆独立小微服务**，禁止往一个服务无限塞域导致“上帝服务”。

## 2. 服务与库

| 服务 | 端口 | 独享库 | 高内聚边界 |
|---|---|---|---|
| gateway | 8080 | — | 仅认证+路由，无业务 |
| auth-service | 8081 | sentinel_auth | 登录/会话/角色 |
| msg-service | 8082 | sentinel_msg | 消息发送引擎（生产+消费同进程，Kafka 管道） |
| logistics-service | 8083 | sentinel_logistics | 订单/轨迹/工单/通知编排 |
| agent-service | 8084 | sentinel_agent | AI 文案/调用日志 |

新域（财务结算、仓管等）应**新建 service**，套用“建壳 → 复用共享层 → 自带库 → thin REST”套路，而非并进上面某个服务。

## 3. 扩展新服务 / 新接口的最小套路（照抄）

1. 拷贝/引用对应 austin 域库模块；`pom` 只加必需依赖（web/jdbc/ms-web/mysql 等）。
2. 启动类放**独立包**（如 `com.java3y.<svc>`），配 `@ComponentScan` 指到要扫的 austin 包 + 用 `@EntityScan/@EnableJpaRepositories` 显式指 JPA 包；需要排除共享模块中“非本进程”装配时用 **REGEX 型 excludeFilters**（避免 class 引用触发加载）。
3. 跨进程能力 = thin `@Component` REST client + `@Value` 地址 + 降级兜底（例 `AgentClient`）。
4. 各自 `application.yml` 默认连宿主地址、可被 `SPRING_*` / 自定义 env 覆盖（容器化靠它）。
5. 冒烟：追加断言到 `infra/tools/smoke_closed_loop.sh`（支持 host / `SMOKE_DOCKER=1` 两模式）。

## 4. (e) oracle 对拍的结论（记录在案，勿当成缺陷重做）

同一订单 OMT-SEED-0001 + 同一节点对拍 单体(8090) vs 微服务(网关)：

| | notification_record | sms_record |
|---|---|---|
| 单体 | **PENDING（卡住）** | 无 —— 只认 `buyer_id`，该订单为空 → 永不派发 |
| 微服务 | **SENT** | `13910079259 / 成功` —— 增加 `buyer_phone` 回退 |

结论：逐字回放不等价，但**微服务修掉了单体“空买家不通知”的洞**，属健壮性改进而非回归。若需严格等价，仅当把单体侧数据也补齐 buyer_id 数字值后才能成立；当前以「微服务 ≥ 单体语义」为验收口径。

## 5. 已验收矩阵

- ✅ (a) 经网关登录　(b) 通知闭环三库落行　(c) 绕过网关 401/直连 403
- ✅ (d) MERCHANT 行级隔离（owner_username 收口：merchant=3 vs admin=42）
- ✅ 容器化一键起 + SMOKE PASS（host 与 docker 双模式）
- ⏳ 阶段2 TMS 全量收编 / 阶段3 事件化 / 阶段4 Nacos·SkyWalking·CI·k8s / 前端经网关

## 6. 路线（按“小/内聚/低耦合”排序）

1. CI 落地（本仓库 `.github/workflows/ci.yml`：build → 镜像 → 起栈 → smoke → down）
2. 需要新增业务域时：先拆**独立小微服务**示范（自带库表已就绪）
3. TMS 存量接口按域分批收编到对应小微服务，逐簇对拍
4. 基建增强与可观测作为可选项增量接入

## 7. 前后端整合（strangler 网关）

原版 Vue UI 依赖 ~150 个后端接口，无法在只有一条切片时全量工作。因此网关采用 **strangler**：

- 已迁移的**具体前缀**优先路由到对应微服务（当前：`/api/auth/*` → auth、`/api/logistics/orders|notify` → logistics）；
- 其余 `/api/**` **回源 legacy 单体后端**(:8090，保留全量接口)，保证原版 UI 每个页面有数据；
- auth 与 legacy 共享 Redis(:6381) 会话 → 网关发的 token 两边通用，无需改造单体鉴权。

**迁移一个域 = 两步**：把该域的接口按 §3 套路在微服务实现（路径与单体一致）→ 在网关把该前缀从 legacy 切到该微服务。UI 无需改动，自动切流。

