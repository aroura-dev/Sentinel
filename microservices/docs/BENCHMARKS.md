# Sentinel-MS 实测结果

> 所有数据均为本机 Docker 环境实测，不代表线上生产指标。

## 压测环境

- Windows 11 + Docker Desktop
- JDK 21.0.8
- Spring Boot 2.7.18
- MySQL 5.7.44
- Redis 6
- Kafka 2.6
- JDK 21 虚拟线程 HttpClient 压测客户端

## 高并发闭环

- 请求数：900
- 并发数：200
- HTTP 成功：900/900
- QPS：74.35
- P50：2.11s
- P95：4.87s
- P99：6.20s
- 最终通知：1000/1000 SENT
- 最终短信：1000/1000 成功
- Kafka consumer lag：0

复现工具：`infra/tools/bench/ClosedLoopLoadTest.java`。

## Redis Lua 去重修复

修复前 `limit.lua` 直接返回 1，导致所有消息被误判为重复。

修复后使用 ZSet + Lua：

- 首次写入返回 0，放行；
- 同一窗口第二次写入返回 1，拦截；
- 新 key 首次写入返回 0；
- 窗口过期后写入返回 0。

## 故障隔离

停止 `agent-service` 后：

- 100/100 HTTP 请求成功；
- 100/100 通知最终 SENT；
- 100/100 短信成功。

## 测试

```text
service-api-impl: 2/2
sentinel-logistics: 12/12
sentinel-agent: 5/5
合计: 19/19
```

## 复现步骤

```bash
mvn -s "$MAVEN_SETTINGS" test
bash infra/tools/up.sh
```
