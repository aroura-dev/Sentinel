# Nginx 反代上游连接池基准

## 环境

- 被测链路：`docker-compose.sentinel.yml` 全栈，压前端 nginx `:5173`，`/api/` 反代到 backend `:8080`
- 宿主：i5-13500H，16 逻辑核，15.6GB；Docker Desktop WSL2 VM 16 核 / 8GB
- 后端：JVM 堆约 2.04GB；Tomcat 默认 `max-threads=200`；Hikari `maximum-pool-size=30`
- 数据库为演示数据（`workorder` 17 行、`notification_record` 20 行、`logistics_order` 42 行），SQL 未构成压力
- JMeter 5.6.3；并发阶梯每档 60s（含 10s 爬坡）；每虚拟用户登录一次后循环读三个接口
- 断言 HTTP 200 **且**响应体含 `"status":"0"`——该应用业务失败同样返回 200，只断言状态码会漏报

## 场景

- `GET /api/dashboard/channel-distribution`
- `GET /api/workorder/list?page=1&perPage=10`
- `GET /api/notification/list?page=1&perPage=10`

## 优化前

```nginx
location /api/ {
    proxy_pass http://backend:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

nginx 对 `proxy_pass` 默认使用 HTTP/1.0 且不回传 keep-alive，等于**每个 API 请求都新建一条到后端的 TCP 连接**。

| 并发 | req/s | avg | p95 | p99 | 错误率 |
|---|---|---|---|---|---|
| 25 | 1700 | 13.4ms | 33ms | 46ms | 0% |
| 50 | 1739 | 26.3ms | 61ms | 85ms | 0% |
| 100 | 1914 | 48.0ms | 105ms | 148ms | 0% |
| 200 | 1976 | 93.9ms | 207ms | 303ms | 0% |

- 吞吐自 25 并发起停在 1700–1980 req/s，延迟却随并发线性上涨（13ms → 94ms）
- 同并发直连 `backend:8080` 对照：100 并发 3584 req/s、avg 25ms

即这一跳砍掉约 47% 吞吐、翻倍延迟。

## 优化后

```nginx
upstream sentinel_backend {
    server backend:8080;
    keepalive 64;
    keepalive_requests 1000;
    keepalive_timeout 60s;
}

location /api/ {
    proxy_pass http://sentinel_backend;
    proxy_http_version 1.1;
    proxy_set_header Connection "";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_next_upstream error timeout http_502;
    proxy_next_upstream_tries 2;
}
```

| 并发 | req/s | avg | p95 | p99 | 错误率 |
|---|---|---|---|---|---|
| 50 | 3021 | 15.1ms | 26ms | 42ms | 0% |
| 100 | 3282 | 28.1ms | 47ms | 79ms | 0% |
| 200 | 3321 | 56.0ms | 93ms | 167ms | 0% |
| 300 | 4242 | 65.2ms | 123ms | 178ms | 0% |
| 500 | 3173 | 141.2ms | 244ms | 368ms | 0% |

- 100 并发：1914 → 3282 req/s（+72%），avg 48.0 → 28.1ms（-41%），已基本追平直连后端的 3584 req/s
- 从零重建镜像后复测 100 并发：3901 req/s、avg 22ms、错误率 0%
- 拐点在 300 并发附近（约 4200 req/s）；500 并发吞吐回落、延迟飙至 141ms，但仍无错误

## 踩坑：只加那两行会更糟

先尝试只补 `proxy_http_version 1.1` + `proxy_set_header Connection ""` 而不定义 `upstream` 块：

- 100 并发：1259 req/s，avg 72ms，**错误率 62.79%**
- nginx 错误日志：`connect() to 172.20.0.5:8080 failed (99: Address not available)`
- 原因是 nginx 仍不池化上游连接，反而制造连接风暴，耗尽本地可用端口

**必须同时定义显式 `upstream` 块才能真正复用长连接。** 这一点已写进 `nginx.conf` 注释，避免后人照抄那两行。

## 结论

在本地 compose 全栈上，启用上游连接池后 100 并发吞吐提升 72%、平均延迟下降 41%，并消除了反代相对于直连后端的绝大部分开销。

两点限制需要注意：

- **约 4200 req/s 是整机上限，不是应用上限。** 稳态期间宿主 CPU 达 90–100%，但 backend 容器仅占 616%（16 核中的约 6 核），其余被压测器自身与 Docker VM 开销占用。压测器与被测系统同机竞争 CPU，故实测值应视为容量下限；要测应用真实容量需将压测器部署到另一台机器。
- **未覆盖写路径与真实数据量。** 本次仅压只读接口，且库为演示数据，SQL 未被压到。后端侧 `max-threads=200`、`maximum-pool-size=30` 两个上限本次未触及。

## 复现

压测工装见 `docs/benchmarks/loadtest/`：

```bash
# 阶梯压测：并发 1/5/10/25/50/100/200，每档 60s
PORT=5173 DUR=60 RAMP=10 LEVELS="1 5 10 25 50 100 200" bash run-ramp.sh
# 汇总（读 ramp-N.jtl 与 stats-N.csv）
bash report.sh ramp "1 5 10 25 50 100 200"
```

原始 JTL 与容器资源采样文件体积较大（合计约 450MB）且可由上述脚本重新生成，故未纳入版本库。
