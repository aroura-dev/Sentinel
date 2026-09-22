# Sentinel

**Proactive logistics exception detection with AI-driven closed-loop resolution.**

[![Java](https://img.shields.io/badge/Java-17%20%2F%2021-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2021-6DB33F)](https://spring.io/projects/spring-cloud)
[![Kafka](https://img.shields.io/badge/Kafka-2.6-231F20?logo=apachekafka)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-6-DC382D?logo=redis&logoColor=white)](https://redis.io/)

---

## What It Is

Parcels stall — at a hub, on a line-haul leg, at the last mile. In most networks nobody finds out until the customer complains, by which point the cost is already sunk: an inbound support ticket, a negotiated compensation, a merchant who trusts the platform a little less.

Sentinel turns tracking data into **timely, classified, acted-upon exceptions**. It watches in-flight shipments for dwell-time and SLA deviations, classifies what went wrong and who is liable, raises the work order, and notifies the right party in their own language — automatically.

The platform is built as a **microservice system with strict domain isolation**: five independently deployable services, each owning its own database, communicating only through thin REST contracts and Kafka events. See [BUSINESS.md](docs/BUSINESS.md) for the business model and [ARCHITECTURE.md](docs/ARCHITECTURE.md) for the system design.

---

## Highlights

- **AI-augmented, not AI-dependent.** Content is composed per recipient and language by a language model — but when the model is unavailable, notifications still go out using curated fallback copy, and the degradation is recorded. *(→ [ARCHITECTURE §10.4](docs/ARCHITECTURE.md))*
- **Every service owns its data.** Four reserved databases, one per service, with cross-domain reads resolved by denormalised columns, REST, or events — and cross-database joins prohibited outright. *(→ [ARCHITECTURE §5](docs/ARCHITECTURE.md))*
- **Merchant isolation enforced in the query layer**, not the UI. A merchant cannot see another merchant's orders no matter how the data is requested. *(→ [BUSINESS §3](docs/BUSINESS.md))*
- **Strangler gateway.** Migrated interface prefixes route to their owning microservice; everything else falls back to the legacy backend, so migration proceeds without breaking the UI. *(→ [ARCHITECTURE §14](docs/ARCHITECTURE.md))*
- **The closed loop is verified end to end.** A smoke script asserts the full path and the rows it must produce across three databases — and CI runs it on every change.
- **Failure behaviour is designed, not accidental.** The AI service can be killed mid-flight without breaking notification delivery. *(→ [BENCHMARKS](docs/BENCHMARKS.md))*

---

## Architecture at a Glance

```
Client ──▶ Gateway :8080 ──┬──▶ auth-service      :8081   sessions, credentials
   (single entry point)    ├──▶ logistics-service :8083   orders, exception orchestration
                           ├──▶ agent-service     :8084   AI content generation
                           └──▶ (fallback) legacy backend  unmigrated interfaces

        logistics ──▶ msg-service :8082 ──▶ Kafka ──▶ channel dispatch
```

| Service | Port | Owns | Role |
|---|---:|---|---|
| **gateway** | 8080 | — | Session validation, identity propagation, routing |
| **auth-service** | 8081 | `sentinel_auth` | Login, sessions, verification codes, roles |
| **msg-service** | 8082 | `sentinel_msg` | Message pipeline, deduplication, channel delivery |
| **logistics-service** | 8083 | `sentinel_logistics` | Orders, tracking, work orders, notification orchestration |
| **agent-service** | 8084 | `sentinel_agent` | LLM content generation, exception knowledge base |

The full service catalog, data ownership model, and security design are in [ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## Quick Start

**Prerequisites**

| Requirement | Notes |
|---|---|
| JDK 17 or 21 | Built and verified on 21 |
| Maven 3.8+ | Or point `MAVEN_SETTINGS` at a custom `settings.xml` |
| Docker Desktop | With Compose v2 |

**One command**

```bash
bash infra/tools/up.sh     # build jars → build images → start the full stack → wait for gateway → run acceptance
bash infra/tools/down.sh   # stop the stack (volumes preserved; add -v to remove them)
```

**What success looks like**

```
== 5/5 等网关并验收 ==
gateway ready ~25s
✅ SMOKE PASS：登录+通知闭环三库落行
```

The acceptance script logs in through the gateway, triggers a notification, and asserts that the expected rows landed across three separate databases. It passing means the whole loop works — not merely that the services started.

**<details><summary>Manual steps (equivalent, for inspection)</summary>**

```bash
# 1) Build all service jars
mvn -N install -q
mvn -pl services/auth-service,services/msg-service,services/logistics-service,services/agent-service,gateway \
    -am package -DskipTests

# 2) Stage artifacts where the Dockerfiles expect them
mkdir -p dist
cp services/{auth,msg,logistics,agent}-service/target/*.jar dist/
cp gateway/target/sentinel-gateway.jar dist/

# 3) Build images and start the stack
cd infra/docker
docker compose -f compose.infra.yml build
docker compose -f compose.infra.yml up -d

# 4) Inspect
docker compose -f compose.infra.yml ps
```

</details>

---

## Try It

```bash
# Log in through the gateway
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -d "username=张伟&password=Admin@123" | grep -oE '"token":"[a-f0-9]+"' | cut -d'"' -f4)

# Trigger a notification; logistics orchestrates agent → msg
curl -s -X POST "http://localhost:8080/api/logistics/notify/send?orderNo=<ORDER>&node=IN_TRANSIT&role=buyer&channel=sms" \
  -H "Authorization: Bearer $TOKEN"
```

**Negative cases** — both should be rejected:

```bash
curl -i "http://localhost:8080/api/logistics/orders"                      # no token → 401
curl -i "http://localhost:8083/api/logistics/orders"                      # bypassing the gateway → no identity → 403
```

**Demo accounts**

| Username | Password | Role |
|---|---|---|
| `张伟` | `Admin@123` | ADMIN — default account |
| `刘洋` | `Operator@123` | OPERATOR |
| `王芳` | `Cs@123` | CUSTOMER_SERVICE |
| `陈浩` | `Merchant@123` | MERCHANT — sees only their own orders |
| `赵敏` | `Finance@123` | FINANCE |

---

## Verified Results

Measured locally under Docker on a development workstation. **These are not production figures** — they demonstrate that the loop holds under load and under partial failure, not a capacity claim.

| Scenario | Result |
|---|---|
| Closed loop under concurrency | All requests succeeded; Kafka consumer lag returned to zero |
| Notification completion | Every notification reached `SENT` |
| Delivery records | Every message recorded as delivered |
| **AI service stopped mid-flight** | All requests succeeded via degradation; the closed loop still completed |
| Unit tests | All passing |

Full methodology, raw figures, and reproduction commands: **[docs/BENCHMARKS.md](docs/BENCHMARKS.md)**.

---

## Frontend Entry Points

| Entry point | Port | Description |
|---|---:|---|
| Minimal demo console | 5175 | Logs in, lists merchant-scoped orders, triggers the closed loop. Exercises **only the microservice slice** — no legacy dependency |
| Full product UI | 5173 | The complete interface. Requires the legacy backend for unmigrated interfaces (see below) |

To run the full UI:

```bash
bash infra/tools/up_legacy.sh              # legacy backend, shares the session store with auth-service
cd frontend-vue && npm ci && npm run dev   # proxies /api through the gateway
```

---

## Repository Layout

```
sentinel-common / support / service-api         Shared libraries
sentinel-handler                                Message pipeline and channel handlers
sentinel-ms-web                                 Shared web layer: identity filter, role interceptor, VO
sentinel-logistics / sentinel-agent           Business domain libraries
services/{auth,msg,logistics,agent}-service   The four independently deployable services
gateway                                       Entry point: authentication and routing
infra/docker/compose.infra.yml                Full infrastructure topology
infra/mysql/<service>/init/*.sql              Per-service database initialisation
infra/tools/smoke_closed_loop.sh              End-to-end acceptance
frontend/ · frontend-vue/                     Demo console · full product UI
docs/                                         Business, architecture, and benchmark documentation
```

---

## Technology Stack

| Layer | Choice |
|---|---|
| Language / runtime | Java 17 baseline, verified on 21 |
| Framework | Spring Boot 2.7, Spring Cloud 2021 |
| Persistence | MySQL 5.7 — one dedicated instance per service |
| Session / dedup store | Redis 6 |
| Messaging | Kafka 2.6 |
| AI | LangChain4j with DashScope (Qwen) |
| Frontend | Vue 3 |
| Packaging | Docker Compose |

> **Note on the baseline.** Spring Boot 2.7 predates the `jakarta.*` namespace and several components are past upstream support. This is a reference implementation, not a production-readiness claim; the upgrade path is documented in [ARCHITECTURE §16](docs/ARCHITECTURE.md).

---

## Testing

```bash
mvn -pl sentinel-logistics,sentinel-agent,sentinel-service-api-impl -am test
```

The end-to-end acceptance script is the more meaningful check — it validates the business contract rather than individual units:

```bash
bash infra/tools/smoke_closed_loop.sh
```

---

## Documentation

| Document | Audience | Covers |
|---|---|---|
| [docs/BUSINESS.md](docs/BUSINESS.md) | Product, operations, stakeholders | The problem, roles, domain model, lifecycle, exception handling, AI's business role |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Engineers | Services, data ownership, security model, contracts, failure modes, deployment |
| [docs/BENCHMARKS.md](docs/BENCHMARKS.md) | Anyone evaluating behaviour | Raw measurements and reproduction steps — the single source of every performance figure |
| [DESIGN.md](DESIGN.md) | Contributors | Decision records and their rationale *(Chinese)* |

---

## Project Status

Capabilities are labelled by how far they have been carried, so nothing is overstated:

| Status | Meaning | Examples |
|---|---|---|
| **Shipped** | Implemented and reachable through the platform's interfaces | Notification closed loop, gateway authentication, merchant data isolation, AI degradation, message deduplication |
| **Implemented, not yet exposed** | Present in the codebase and unit-tested, but not reachable via an endpoint | Five of the six AI capabilities — exception diagnosis, work-order triage, intent routing, channel recommendation, delivery estimation |
| **Served by the platform** | Available in the product, provided by a separate backend while migration completes | Full order management, billing, inventory, returns |
| **Planned** | Not implemented | Distributed tracing and metrics, service registry, container orchestration, event-sourced closed loop |

**Known limitations** — the most significant being that failed notification dispatch has no automatic retry, and there is no distributed tracing or metrics export. Both are documented rather than hidden: see [ARCHITECTURE §17](docs/ARCHITECTURE.md).

---

## Credits

The message delivery engine and several domain libraries derive from **[austin](https://github.com/ZhongFuCheng3y/austin)**, an open-source notification platform. This project adapts them into a decomposed, domain-isolated architecture with an added AI layer and logistics domain model.

**License: TBD.** No license file is currently included in this repository, which means the code is not yet formally licensed for reuse and redistribution. This should be resolved before any external distribution, particularly given the upstream derivation noted above.
