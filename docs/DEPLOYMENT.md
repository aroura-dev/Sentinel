# Sentinel 部署指南

## 1. 推荐部署方式

推荐直接使用仓库根目录的 `docker-compose.sentinel.yml`。该方式不依赖宿主机 JDK、Maven 或 Node.js，后端和前端均在 Docker 构建阶段完成。

只需安装：

- Docker Desktop 或 Docker Engine
- Docker Compose v2

## 2. 全新克隆部署

在任意目录执行：

```bash
git clone https://github.com/aroura-dev/Sentinel.git
cd Sentinel
docker compose -f docker-compose.sentinel.yml up -d --build
```

首次启动会自动：

1. 使用 Maven 容器编译 `sentinel-web` 及其依赖模块。
2. 构建 Vue 前端生产镜像。
3. 创建 MySQL 数据库并执行 `doc/docker/mysql/init-sentinel.sh`。
4. 初始化用户、角色、订单、库存、工单、账单、API 密钥和演示业务数据。
5. 启动 MySQL、Redis、后端和前端。

访问：

| 服务 | 地址 |
|---|---|
| 管理端 | http://localhost:5173 |
| 后端 API | http://localhost:8080 |
| MySQL | localhost:3307 |
| Redis | localhost:6379 |

默认账号：`张伟 / Admin@123`。

## 3. 环境变量

默认配置无需 `.env` 即可启动。需要自定义时可执行：

```bash
cp .env.example .env
```

主要变量：

```env
SENTINEL_DATABASE_PASSWORD=root123_A
SENTINEL_REDIS_PASSWORD=sentinel
SENTINEL_FRONTEND_HOST_PORT=5173
SENTINEL_BACKEND_HOST_PORT=8080
SENTINEL_MYSQL_HOST_PORT=3307
SENTINEL_REDIS_HOST_PORT=6379
DASHSCOPE_API_KEY=
```

未配置 `DASHSCOPE_API_KEY` 时，Agent 自动使用模板降级，不影响页面和其他业务功能。

## 4. 启动、停止与重置

停止并保留数据：

```bash
docker compose -f docker-compose.sentinel.yml down
```

重新启动：

```bash
docker compose -f docker-compose.sentinel.yml up -d
```

查看状态和日志：

```bash
docker compose -f docker-compose.sentinel.yml ps
docker compose -f docker-compose.sentinel.yml logs -f backend
docker compose -f docker-compose.sentinel.yml logs -f frontend
```

清空数据库并重新初始化：

```bash
docker compose -f docker-compose.sentinel.yml down -v
docker compose -f docker-compose.sentinel.yml up -d --build
```

## 5. 数据库初始化

初始化入口：

```text
doc/docker/mysql/init-sentinel.sh
```

实际 SQL 位于：

```text
doc/sql/
```

MySQL 官方镜像只在数据卷为空时执行初始化。修改初始化脚本后，需要执行 `down -v` 清空旧数据卷，再重新启动。

当前初始化数据包括：

- 5 个中文账号及联系方式
- 用户头像
- 4 个商家、10 个商品、2 个仓库
- 42 个订单、42 个运单、69 条物流轨迹
- 17 个工单
- 库存台账、出入库流水
- 售后、账单、通知和开放 API 示例
- 客服会话由后端启动时自动注入

## 6. 端口冲突

以下默认端口可能与其他项目冲突：

- `5173`
- `8080`
- `3307`
- `6379`

在 `.env` 中修改宿主机端口：

```env
SENTINEL_FRONTEND_HOST_PORT=15173
SENTINEL_BACKEND_HOST_PORT=18080
SENTINEL_MYSQL_HOST_PORT=13307
SENTINEL_REDIS_HOST_PORT=16379
```

容器之间的通信端口保持不变，前端仍通过 `backend:8080` 访问后端。

## 7. 常见问题

### 后端一直 unhealthy

检查后端日志：

```bash
docker compose -f docker-compose.sentinel.yml logs --tail 300 backend
```

常见原因是 MySQL 初始化未完成或数据卷使用了旧的错误结构。可执行 `down -v` 后重新初始化。

### 登录账号无效

使用：

```text
用户名：张伟
密码：Admin@123
```

### 图片或页面数据为空

确认前端镜像已重新构建：

```bash
docker compose -f docker-compose.sentinel.yml up -d --build frontend
```

### Docker 端口已分配

修改 `.env` 中的宿主机端口，或停止占用相同端口的其他项目。

## 8. 微服务版本

`microservices/` 目录保留了四服务拆分版本，适合研究数据库分库、Gateway、Kafka 和 Outbox 链路。该版本依赖更多基础设施，不是普通演示的首选部署方式。

相关脚本：

```text
microservices/infra/tools/up.sh
microservices/infra/tools/down.sh
microservices/infra/tools/smoke_closed_loop.sh
```