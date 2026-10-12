# Nacos：注册中心 + 配置中心

本项目的 Nacos 同时承担两职。容器定义见 `infra/docker/compose.infra.yml`，**不发布任何端口**——
控制台能改配置、下线实例，对宿主可达即等于交出集群控制权。本机要看控制台请叠加 dev 覆盖：

```bash
docker compose -f compose.infra.yml -f compose.dev.yml up -d
# http://127.0.0.1:8848/nacos   默认账号 nacos/nacos
```

## 一、服务注册发现

各服务在 `application.yml` 里注册，`spring.application.name` 即 serviceId：

```yaml
spring:
  cloud:
    nacos:
      server-addr: ${NACOS_SERVER_ADDR:nacos:8848}
      discovery:
        enabled: ${NACOS_DISCOVERY_ENABLED:true}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
```

- 网关路由 `uri: ${AUTH_URI:lb://auth-service}` 经发现解析实例，换 IP / 扩副本无需改配置。
- 服务间调用是 `@FeignClient(url = "${service.xxx-url:}")`：**url 留空即走发现**。
  把对应 env（如 `SERVICE_MSG_URL`）设回 `http://host:port` 即可退回静态直连。
- 全栈同一 compose bridge 网络，注册容器网卡 IP 即可互达，不需要设 `discovery.ip`。

查询已注册的实例：

```bash
docker exec sentinelms-nacos curl -s \
  "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=logistics-service"
```

## 二、配置中心

各服务通过 `spring.config.import` 拉取，**不需要 bootstrap.yml**（Boot 2.4+ 的推荐路径，
Spring Cloud Alibaba 新版已废弃 bootstrap）：

```yaml
spring:
  config:
    import:
      - optional:nacos:${spring.application.name}.yml   # 本服务私有
      - optional:nacos:sentinel-common.yml              # 跨服务共享
```

**红线：`optional:` 前缀必须保留。** 它保证 Nacos 不可用、或数据未播种时服务照常启动并回落到
本地 `application.yml`。SCA 另有 import-check——classpath 上有 `nacos-config` starter 却没写
`spring.config.import` 会直接启动失败。

### dataId 约定

| dataId | group | 用途 |
|---|---|---|
| `<spring.application.name>.yml` | `DEFAULT_GROUP` | 该服务私有覆盖 |
| `sentinel-common.yml` | `DEFAULT_GROUP` | 跨服务共享 |

键名用 **camelCase**（如 `deduplicationRule`），与 `ConfigServiceImpl` 的查询键一致；
写成大写下划线在 Nacos 里不会被宽松匹配到。

### 该放 / 不该放

- **放 Nacos**：可热调的业务 knob —— `notify.retry.*`、`notify.dedup-hours`、
  `notify.dispatch.template-id`、`sms.engine.template-id`、去重与流控规则。
- **留本地**：拓扑与连接信息（datasource / redis / kafka / `server.port` / `feign.client.config`），
  以及 `spring.cloud.nacos.server-addr` 本身（自举鸡生蛋）。
- **走 env 不要进 Nacos**：密钥（`SENTINEL_INTERNAL_SECRET`、DB/Redis 口令）——Nacos 默认不做静态加密。

### 播种

```bash
N() { docker exec sentinelms-nacos curl -s "$@"; }

# 共享配置
N -X POST "http://localhost:8848/nacos/v1/cs/configs" \
  -d "dataId=sentinel-common.yml" -d "group=DEFAULT_GROUP" \
  --data-urlencode "content=notify:
  dedup-hours: 24
"

# 某服务的私有覆盖
N -X POST "http://localhost:8848/nacos/v1/cs/configs" \
  -d "dataId=logistics-service.yml" -d "group=DEFAULT_GROUP" \
  --data-urlencode "content=notify:
  dispatch:
    template-id: 3
"
```

删除：

```bash
N -X DELETE "http://localhost:8848/nacos/v1/cs/configs?dataId=logistics-service.yml&group=DEFAULT_GROUP"
```

### 生效方式

**Nacos 推送**：改 Nacos 里的某个值 → Nacos 长轮询把变更推给**所有**实例 → Spring 发 `RefreshEvent`
→ `@RefreshScope` bean 重建并读到新值。**无需重启。** 日志形如：

```
[nacos.client.cachedata.internal.notifier] Refresh keys changed: [notify.dispatch.enabled]
```

`@RefreshScope` 只加在 `LogisticsNotifyService` 与 `NotificationRetryTask`（`notify.*` 可热调键的持有者）。
其他服务的运行期 knob 走 `ConfigServiceImpl.getProperty` 现读 `Environment`，本来就不需要重启。

### 为什么没有引入 Spring Cloud Bus

曾接过 `spring-cloud-starter-bus-kafka`（`/actuator/busrefresh` 手动触发全集群刷新），**实测后移除**，两条理由：

1. **对配置刷新是冗余的。** Nacos 长轮询本来就会把变更推给所有实例并触发 `@RefreshScope` 重建 ——
   实测日志可区分两种机制（线程名不同）：`nacos.client.cachedata.internal.notifier` 是 Nacos 推的，
   `KafkaConsumerDestination{springCloudBus}` + `o.s.cloud.bus.event.RefreshListener` 才是 Bus 送达的。
   留着只是为凑组件。
2. **引入了一个真实故障面。** Stream 的 Kafka binder 会通过 `KafkaBinderEnvironmentPostProcessor`
   把 `spring.kafka.producer/consumer.*` 的**默认值**改成 `ByteArray*`，而 msg-service 的业务管道两端
   都依赖 String（`KafkaSendMqServiceImpl` 发 String、`Receiver` 收 `ConsumerRecord<?, String>`），
   不显式钉死就会炸 `Can't convert value of class java.lang.String to class ByteArraySerializer`
   （实测直接把 smoke 打挂）。两套客户端栈共用 `spring.kafka.*` 命名空间，得在两侧分别钉死序列化器。

代价是少了一个"手动触发一次全集群刷新"的能力（Nacos 不能在无变更时被要求刷新）。
若日后确实需要，按上面两条把 starter 加回、并把序列化器两侧钉死即可。

### ⚠️ 三个实测行为边界（别踩）

1. **改值会热生效；仅"删除"键不会让 bean 回落到本地默认。** bean 会一直持有最后见到的值 ——
   实测删掉 dataId 并触发刷新后，`notify.dispatch.enabled` 仍是 `false`。要回落：写回一个值触发变更，或重启服务。
2. **`@Scheduled` 的 `fixedDelay`/`cron` 改不动。** 它在启动时一次性注册进 `ScheduledTaskRegistrar`，
   加 `@RefreshScope` 也无效（`notify.retry.scan-interval-ms` / `initial-delay-ms` 属于此类）。
3. **`spring.kafka.*` 被业务管道与 Bus 的 binder 共用**，两侧序列化器要求相反
   （业务发 String / Bus 发 byte[]）。msg-service 里已在 `spring.kafka.producer/consumer.*` 钉 String、
   在 `spring.cloud.stream.kafka.binder.configuration` 钉 ByteArray，改任一侧都要同步。

## 三、回退阀门

注册中心或配置中心出问题时的开关，都是纯配置、**不需要重建镜像**：

| 场景 | 处置 |
|---|---|
| 网关照常但发现不可用 | 给 gateway 设回 `AUTH_URI` / `LOGISTICS_URI` 静态地址 |
| 某服务不想注册 | `NACOS_DISCOVERY_ENABLED=false` |
| 服务间调用退回静态 | 设回 `SERVICE_AGENT_URL` / `SERVICE_MSG_URL` / `SERVICE_LOGISTICS_URL` / `SMS_ENGINE_MSG_URL` |
| Feign 熔断想关掉 | `feign.circuitbreaker.enabled: false`（auth / msg） |
| 网关路由熔断想关掉 | 删掉两条路由 `filters:` 里的 `CircuitBreaker` 块 |
