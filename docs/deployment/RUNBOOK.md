# GCRF Library Management System - Deployment Runbook

**Version**: 1.0.0  
**Last Updated**: 2026-04-15  
**Audience**: DevOps Engineers, SREs, Platform Engineers

---

## Table of Contents

1. [前置依赖](#前置依赖)
2. [环境变量配置](#环境变量配置)
3. [首次启动顺序](#首次启动顺序)
4. [健康检查](#健康检查)
5. [常见故障排查](#常见故障排查)
6. [备份与恢复](#备份与恢复)
7. [升级流程](#升级流程)
8. [监控 Dashboard 入口](#监控-dashboard-入口)
9. [关闭与清理](#关闭与清理)
10. [性能优化](#性能优化)
11. [安全加固](#安全加固)

---

## 前置依赖

### 系统要求

| 项目 | 最低版本 | 建议版本 | 备注 |
|------|---------|---------|------|
| Docker Engine | 20.10 | 24.0+ | `docker --version` |
| Docker Compose | v1.29 | v2.20+ | `docker compose version` |
| 内存 (RAM) | 8 GB | 16 GB | 推荐 32 GB 生产环境 |
| 磁盘空间 | 20 GB | 50+ GB | 用于数据 + 日志 |
| 操作系统 | - | Ubuntu 20.04 LTS / CentOS 8 / macOS 12+ | - |

### 必需工具

```bash
# 检查 Docker
docker --version          # >= 20.10
docker compose version    # >= v2.0

# 可选：性能分析工具
curl --version           # 用于健康检查
jq --version            # JSON 处理
postgresql-client       # PG 客户端（备份/恢复用）
```

### 网络要求

- **开放端口**:
  - 8080: API Gateway (外部访问)
  - 3011: Web Admin UI (前端)
  - 8848: Nacos Console (内部管理)
  - 5432: PostgreSQL (仅 localhost 或专网)
  - 6379: Redis (仅 localhost 或专网)
  - 9000: MinIO (仅内网)
  - 9090: Prometheus (仅内网)
  - 3000: Grafana (仅内网)

- **DNS**: 若需要虚拟主机 (MinIO 等)，配置 `/etc/hosts`:
  ```
  127.0.0.1 minio.gcrf.local
  127.0.0.1 nacos.gcrf.local
  ```

---

## 环境变量配置

### 第 1 步：复制环境文件

```bash
cd /path/to/GCRF_LibraryManagementSystem/deployment
cp .env.infrastructure.example .env.infrastructure
cp ../.env.prod.example .env
```

### 第 2 步：编辑 `.env.infrastructure` - 基础设施配置

**必填项**（需要强随机密码）：

```bash
# PostgreSQL
DB_PASSWORD=REPLACE_WITH_STRONG_PASSWORD       # 数据库密码
DB_APP_PASSWORD=REPLACE_WITH_APP_PASSWORD      # 应用用户密码
POSTGRES_REPLICATION_PASSWORD=REPLACE_WITH_REPLICATION_PASSWORD

# Redis
REDIS_PASSWORD=REPLACE_WITH_STRONG_PASSWORD

# Nacos (自带 MySQL)
NACOS_MYSQL_ROOT_PASSWORD=REPLACE_WITH_ROOT_PASSWORD
NACOS_MYSQL_PASSWORD=REPLACE_WITH_NACOS_PASSWORD
NACOS_AUTH_TOKEN=$(openssl rand -hex 32)       # 自动生成

# RabbitMQ
RABBITMQ_PASSWORD=REPLACE_WITH_STRONG_PASSWORD
RABBITMQ_ERLANG_COOKIE=$(openssl rand -hex 20)  # 自动生成

# MinIO
MINIO_ROOT_PASSWORD=REPLACE_WITH_STRONG_PASSWORD  # 最少 8 字符

# Monitoring
GRAFANA_ADMIN_PASSWORD=REPLACE_WITH_GRAFANA_PASSWORD

# 备份配置
BACKUP_PATH=/data/backups/gcrf
BACKUP_RETENTION_DAYS=30
```

### 第 3 步：编辑 `.env` - 应用配置

**必填项**：

```bash
# JWT 安全
JWT_SECRET=$(openssl rand -base64 32)          # 64 字符 base64
JWT_EXPIRATION=86400                            # 24 小时（秒）

# Nacos 服务发现
NACOS_SERVER_ADDR=nacos:8848                   # Docker 网络内
NACOS_NAMESPACE=gcrf-prod                      # 命名空间
NACOS_USERNAME=nacos
NACOS_PASSWORD=${NACOS_MYSQL_PASSWORD}         # 与基础设施 .env 保持一致

# 数据库
DB_HOST=postgres-primary
DB_PORT=5432
DB_NAME=gcrf_library
DB_USERNAME=gcrf_app
DB_PASSWORD=${DB_APP_PASSWORD}

# Redis
REDIS_HOST=redis-master
REDIS_PORT=6379
REDIS_PASSWORD=${REDIS_PASSWORD}

# RabbitMQ
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=gcrf_rabbitmq_admin
RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}

# MinIO
MINIO_ENDPOINT=minio:9000
MINIO_BUCKET=library-files
MINIO_ACCESS_KEY=gcrf_minio_admin
MINIO_SECRET_KEY=${MINIO_ROOT_PASSWORD}

# 日志级别
LOGGING_LEVEL=INFO
```

### 第 4 步：文件权限

```bash
# 仅所有者可读写（包含敏感信息）
chmod 600 .env .env.infrastructure

# 不要提交到版本控制
echo ".env .env.infrastructure" >> .gitignore
```

### 生成强密码的方法

```bash
# 方法 1：openssl（推荐）
openssl rand -base64 32    # 32 字节 base64 字符串
openssl rand -hex 32       # 64 字符十六进制

# 方法 2：Python
python3 -c "import secrets; print(secrets.token_hex(32))"

# 方法 3：dd
dd if=/dev/urandom bs=1 count=32 2>/dev/null | base64
```

---

## 首次启动顺序

### Step 1: 验证环境

```bash
cd deployment

# 检查 Docker
docker --version
docker compose version

# 验证 .env 文件存在
ls -l .env .env.infrastructure
echo "Checking variables..."
grep -c "REPLACE_WITH" .env .env.infrastructure && echo "ERROR: 未替换占位符！" && exit 1
echo "✓ 环境变量已配置"
```

### Step 2: 启动基础设施

```bash
# 启动 PostgreSQL, Redis, Nacos, RabbitMQ, MinIO
docker compose -f docker-compose.infrastructure.yml up -d

# 等待服务就绪（首次启动 Nacos MySQL 可能需要 2-3 分钟）
log() { echo "[$(date +%H:%M:%S)] $*"; }
log "等待基础设施服务启动..."
sleep 30

# 检查状态：所有服务应为 "healthy" 或 "running"
docker compose -f docker-compose.infrastructure.yml ps

log "验证关键服务..."

# PostgreSQL: 测试连接
docker exec gcrf-postgres-primary pg_isready -U postgres && log "✓ PostgreSQL 就绪"

# Redis: 检查响应
docker exec gcrf-redis-master redis-cli -a "${REDIS_PASSWORD}" ping | grep -q PONG && log "✓ Redis 就绪"

# Nacos: 检查 UI
curl -s http://localhost:8848/nacos/ | head -1 && log "✓ Nacos 就绪"

# RabbitMQ: 检查管理端
curl -s http://guest:guest@localhost:15672/api/vhosts | jq . && log "✓ RabbitMQ 就绪"

# MinIO: 检查健康
docker exec gcrf-minio mc admin info local 2>/dev/null && log "✓ MinIO 就绪"
```

**常见问题**：
- **Nacos 超时**: 首次初始化需要 2-3 分钟，耐心等待。检查日志：`docker logs gcrf-nacos`
- **内存不足 OOM**: 减少容器内存限制，或限制并发容器启动

### Step 3: 导入 Nacos 配置

```bash
# 使用现成脚本（如果存在）
cd scripts
./push-nacos-configs.sh

# 或者手动导入：
# 打开浏览器 → http://localhost:8848/nacos
# 用户: nacos, 密码: <NACOS_MYSQL_PASSWORD>
# 配置管理 → 导入配置 → 选择 JSON/YAML 文件
```

**验证配置已导入**:
```bash
curl -s "http://nacos:8848/nacos/v1/cs/configs" \
  -d "dataId=application.yml&group=DEFAULT_GROUP&tenant=&search=accurate" \
  -H "Content-Type: application/x-www-form-urlencoded" | jq .
```

### Step 4: 启动后端服务 + 前端

```bash
# 构建并启动所有微服务
docker compose -f docker-compose.services.yml up -d --build

log "等待服务启动..."
sleep 20

# 查看状态
docker compose -f docker-compose.services.yml ps

# 检查关键服务日志（如有错误）
docker logs gcrf-gateway-service
docker logs gcrf-auth-service
docker logs gcrf-book-service
```

**预期输出**：所有服务 `STATUS=Up (healthy)`

### Step 5: (可选) 启动监控栈

```bash
# Prometheus + Grafana + AlertManager
docker compose -f docker-compose.monitoring.yml up -d

log "监控栈已启动"
# Grafana: http://localhost:3000 (admin/admin)
# Prometheus: http://localhost:9090
```

### Step 6: 完整健康检查

```bash
# 一键检查（推荐）
./scripts/deploy-and-smoke.sh

# 或分步检查
log "=== 网关健康检查 ==="
curl -s http://localhost:8080/actuator/health | jq .

log "=== 认证服务 ==="
curl -s http://localhost:8081/actuator/health | jq .

log "=== 图书服务 ==="
curl -s http://localhost:8082/actuator/health | jq .

log "=== 流通服务 ==="
curl -s http://localhost:8083/actuator/health | jq .

log "=== 读者服务 ==="
curl -s http://localhost:8084/actuator/health | jq .

log "=== 前端 ==="
curl -s http://localhost:3011 | head -1
```

**全绿✓ 表示成功启动！**

---

## 健康检查

### 自动化检查

```bash
cd deployment

# 完整端到端烟雾测试（推荐）
./scripts/deploy-and-smoke.sh

# 仅基础设施检查
./scripts/health-check.sh infrastructure

# 仅服务检查
./scripts/health-check.sh services

# 全堆栈检查
./scripts/health-check-all.sh
```

### 手动检查清单

| 组件 | 命令 | 预期 |
|------|------|------|
| PostgreSQL | `docker logs gcrf-postgres-primary \| grep "ready to accept"` | ✓ 含 "ready" 消息 |
| Redis | `docker exec gcrf-redis-master redis-cli ping` | `PONG` |
| Nacos | `curl http://localhost:8848/nacos/\` | HTTP 200 |
| RabbitMQ | `curl http://guest:guest@localhost:15672/api/vhosts` | JSON 数组 |
| MinIO | `docker exec gcrf-minio mc admin info local` | 显示集群信息 |
| Gateway | `curl http://localhost:8080/actuator/health` | `{"status":"UP"}` |
| Auth Service | `curl http://localhost:8081/actuator/health` | `{"status":"UP"}` |
| PostgreSQL 连接池 | `curl http://localhost:8080/actuator/db` | 显示连接数 |

### 交互式监控

```bash
# 实时日志尾部
docker compose -f docker-compose.services.yml logs -f --tail=50

# 单个服务日志
docker logs -f gcrf-gateway-service

# 容器统计（CPU/内存）
docker stats --no-stream
```

---

## 常见故障排查

### 问题 1: 服务启动后 health 一直 DOWN

**症状**:
```json
{
  "status": "DOWN",
  "components": {
    "db": { "status": "DOWN", "reason": "Connection timeout" }
  }
}
```

**根本原因**:
- Nacos 未就绪，服务无法获取配置
- 数据库连接超时
- 网络隔离

**解决步骤**:
```bash
# 1. 检查 Nacos 状态
docker logs gcrf-nacos | tail -50

# 2. 等待 Nacos 就绪（可能需要 2-3 分钟）
docker exec gcrf-nacos curl -s http://localhost:8848/nacos/v1/auth/login \
  -d "username=nacos&password=<NACOS_MYSQL_PASSWORD>" | jq .

# 3. 检查网络连接
docker network ls | grep gcrf
docker network inspect gcrf-backend-network  # 验证服务在同一网络

# 4. 查看服务日志（DEBUG 级别）
docker exec gcrf-gateway-service env | grep -i nacos
docker logs gcrf-gateway-service | grep -i "nacos\|discovery"

# 5. 重启依赖服务
docker compose -f docker-compose.services.yml up -d --no-deps --build <service>
```

---

### 问题 2: PostgreSQL schema 缺失 / Flyway 错误

**症状**:
```
ERROR: relation "reader" does not exist
Caused by: org.flywaydb.core.api.FlywayException: Validate failed
```

**根本原因**: Flyway 迁移失败或未运行

**解决步骤**:
```bash
# 1. 检查 Flyway 迁移日志
docker logs gcrf-reader-service | grep -i flyway

# 2. 登录 PostgreSQL 检查当前 schema
docker exec -it gcrf-postgres-primary psql -U postgres -d gcrf_reader -c "\dt"

# 3. 查看迁移历史
docker exec -it gcrf-postgres-primary psql -U postgres -d gcrf_reader \
  -c "SELECT * FROM flyway_schema_history;"

# 4. 手动修复（需要管理员介入）
# 选项 A：清除并重新初始化
docker exec -it gcrf-postgres-primary psql -U postgres -d gcrf_reader \
  -c "DROP TABLE IF EXISTS flyway_schema_history CASCADE;"

# 选项 B：跳过失败的迁移
docker exec -it gcrf-postgres-primary psql -U postgres -d gcrf_reader \
  -c "UPDATE flyway_schema_history SET success=true WHERE script LIKE 'V%' LIMIT 1;"

# 5. 重启应用
docker compose -f docker-compose.services.yml up -d --no-deps --build <service>

# 6. 验证
curl -s http://localhost:<port>/actuator/health | jq .
```

---

### 问题 3: 端口被占用

**症状**:
```
ERROR: for gcrf-gateway-service Cannot start service: Bind for 0.0.0.0:8080 failed
```

**排查**:
```bash
# 查看占用的进程
lsof -i :8080           # macOS/Linux
netstat -ano | findstr :8080  # Windows

# 杀死进程
kill -9 <PID>
# 或改变 compose 端口映射：
#   ports:
#     - "8081:8080"
```

---

### 问题 4: 内存不足 (OOM)

**症状**: 容器异常退出，日志含 "Out of Memory" 或 "Cannot allocate memory"

**快速修复**:
```bash
# 只启动部分服务
docker compose -f docker-compose.services.yml up -d gcrf-gateway-service

# 或降低 JVM 堆大小
# 编辑 docker-compose.services.yml:
# JAVA_OPTS: -Xms256m -Xmx512m   # 从 512m/1024m 改为更小
```

**永久方案**:
- 购买更多 RAM
- 改为 Kubernetes 部署（自动弹性扩容）

---

### 问题 5: 镜像构建慢 / Maven 依赖下载超时

**症状**: `docker compose up --build` 卡在 Maven 下载，超时失败

**原因**: 默认 Maven Central 仓库网络慢

**快速修复**:
```bash
# 配置阿里云镜像（中国用户推荐）
cat > backend/.mvn/settings.xml <<'EOF'
<settings>
  <mirrors>
    <mirror>
      <id>alimaven</id>
      <name>Aliyun Maven Mirror</name>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
EOF
```

**完整缓存优化**:
```bash
# 或使用多阶段 Dockerfile + BuildKit 缓存
DOCKER_BUILDKIT=1 docker build \
  --cache-from=gcrf-library/gateway-service:latest \
  -t gcrf-library/gateway-service:latest \
  backend/gateway-service/

# 或预先拉取依赖
docker run --rm \
  -v ~/.m2:/root/.m2 \
  -v $(pwd)/backend:/app \
  maven:3.9 \
  mvn -f /app/pom.xml dependency:resolve
```

---

### 问题 6: JWT 认证失败 (401 Unauthorized)

**症状**: 所有 API 请求返回 `401: Unauthorized`，Gateway 日志含 "Invalid token"

**排查**:
```bash
# 1. 检查 JWT_SECRET 配置一致性
docker exec gcrf-gateway-service env | grep JWT_SECRET
docker exec gcrf-auth-service env | grep JWT_SECRET
# 两者应完全相同

# 2. 检查 JWT 过期配置
docker exec gcrf-gateway-service env | grep JWT_EXPIRATION

# 3. 查看 Gateway 日志
docker logs gcrf-gateway-service | grep -i "jwt\|token\|authentication"

# 4. 测试 token 生成（从认证服务）
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# 5. 使用有效 token 重试
curl http://localhost:8080/api/v1/books \
  -H "Authorization: Bearer <token_from_step_4>"
```

**恢复步骤**:
```bash
# 如果 JWT_SECRET 值不正确，重新配置：
# 编辑 .env，生成新密钥
JWT_SECRET=$(openssl rand -base64 32)

# 重新启动服务
docker compose -f docker-compose.services.yml up -d --no-deps --build gcrf-gateway-service gcrf-auth-service
```

---

### 问题 7: MinIO 文件上传失败

**症状**: 上传返回 `403 Forbidden` 或 `Connection refused`

**排查**:
```bash
# 1. 检查 MinIO 状态
docker exec gcrf-minio mc admin info local

# 2. 验证访问密钥
docker compose -f docker-compose.infrastructure.yml ps gcrf-minio
docker logs gcrf-minio | tail -20

# 3. 连接 MinIO 并列出桶
docker exec gcrf-minio mc ls local/

# 4. 创建必要的桶（如果不存在）
docker exec gcrf-minio mc mb local/library-files

# 5. 检查应用配置
docker exec gcrf-book-service env | grep MINIO

# 6. 查看应用日志
docker logs gcrf-book-service | grep -i "minio\|s3"
```

---

## 备份与恢复

### PostgreSQL 备份

**日常备份（推荐脚本）**:
```bash
cd deployment/scripts
./backup-database.sh
```

**手动备份**:
```bash
# 全库备份
docker exec gcrf-postgres-primary pg_dumpall -U postgres > \
  backup-all-$(date +%Y%m%d-%H%M%S).sql

# 单库备份
docker exec gcrf-postgres-primary pg_dump -U postgres gcrf_reader > \
  backup-gcrf_reader-$(date +%Y%m%d).sql

# 大文件压缩备份（推荐）
docker exec gcrf-postgres-primary pg_dumpall -U postgres | gzip > \
  backup-all-$(date +%Y%m%d).sql.gz
```

**恢复**:
```bash
# 从备份恢复全部数据库
gunzip -c backup-all-20260415.sql.gz | \
  docker exec -i gcrf-postgres-primary psql -U postgres

# 或恢复单库（需先创建数据库）
docker exec gcrf-postgres-primary psql -U postgres \
  -c "CREATE DATABASE gcrf_reader;"
gunzip -c backup-gcrf_reader-20260415.sql.gz | \
  docker exec -i gcrf-postgres-primary psql -U postgres gcrf_reader
```

**验证恢复**:
```bash
docker exec gcrf-postgres-primary psql -U postgres gcrf_reader \
  -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public';"
```

---

### Redis 备份与恢复

**备份 RDB 快照**:
```bash
# Redis 默认定期生成 RDB（dump.rdb）
docker exec gcrf-redis-master redis-cli -a "${REDIS_PASSWORD}" BGSAVE

# 复制到宿主机
docker cp gcrf-redis-master:/data/dump.rdb ./backup-redis-$(date +%Y%m%d).rdb
```

**恢复**:
```bash
# 停止 Redis
docker compose -f docker-compose.infrastructure.yml stop gcrf-redis-master

# 恢复文件
docker cp ./backup-redis-20260415.rdb gcrf-redis-master:/data/dump.rdb

# 启动
docker compose -f docker-compose.infrastructure.yml up -d gcrf-redis-master

# 验证
docker exec gcrf-redis-master redis-cli -a "${REDIS_PASSWORD}" DBSIZE
```

---

### MinIO 文件备份

**安装 MC 客户端**:
```bash
# macOS
brew install minio-mc

# Linux
curl https://dl.min.io/client/mc/release/linux-amd64/mc -o mc
chmod +x mc
sudo mv mc /usr/local/bin/

# Windows
# 下载: https://dl.min.io/client/mc/release/windows-amd64/mc.exe
```

**配置别名**:
```bash
mc alias set local http://localhost:9000 \
  gcrf_minio_admin ${MINIO_ROOT_PASSWORD}
```

**备份所有文件**:
```bash
# 递归备份桶
mc cp --recursive local/library-files ./backup/library-files-$(date +%Y%m%d)/

# 带压缩
tar -czf backup-minio-$(date +%Y%m%d).tar.gz ./backup/library-files/
```

**恢复**:
```bash
# 恢复文件到 MinIO
mc cp --recursive ./backup/library-files-20260415/ local/library-files/
```

---

### Nacos 配置备份

**导出配置**:
```bash
# Web UI 方式（推荐）:
# 登录 http://localhost:8848/nacos
# 配置管理 → 右上角"导出" → 选择命名空间 → 下载 ZIP

# CLI 方式:
# 需要在 Nacos 目录执行
docker exec gcrf-nacos /bin/bash -c \
  'cd /home/nacos && ./bin/export-config.sh'
```

**恢复配置**:
```bash
# Web UI: 配置管理 → 导入 → 选择 ZIP 文件

# 或手动导入 YAML
curl -X POST "http://localhost:8848/nacos/v1/cs/configs" \
  -d "dataId=application.yml&group=DEFAULT_GROUP&content=$(cat config.yml | jq -Rs .)" \
  -H "Content-Type: application/x-www-form-urlencoded"
```

---

## 升级流程

### 滚动更新 (Rolling Update)

**升级单个服务**（零停机）:
```bash
cd deployment

# Step 1: 构建新镜像
DOCKER_BUILDKIT=1 docker build \
  -t gcrf-library/book-service:v1.0.1 \
  backend/book-service/

# Step 2: 更新 docker-compose.services.yml 镜像标签
sed -i 's/gcrf-library\/book-service:latest/gcrf-library\/book-service:v1.0.1/' \
  docker-compose.services.yml

# Step 3: 执行滚动更新
docker compose -f docker-compose.services.yml up -d --no-deps gcrf-book-service

# Step 4: 等待容器就绪
sleep 10
docker compose -f docker-compose.services.yml ps gcrf-book-service

# Step 5: 验证健康
curl -s http://localhost:8082/actuator/health | jq .

# Step 6: 验证功能（烟雾测试）
./scripts/api-smoke-test.sh book
```

**预期结果**: 无请求丢失，用户无感知

---

### 全栈升级 (Full Stack Update)

**更新所有服务**:
```bash
# Step 1: 拉取最新代码
git pull origin main

# Step 2: 重新构建所有镜像
docker compose -f docker-compose.services.yml build --no-cache

# Step 3: 启动新容器（旧容器 graceful shutdown）
docker compose -f docker-compose.services.yml up -d --build

# Step 4: 等待所有服务健康
./scripts/wait-for-healthy.sh 120  # 等待 2 分钟

# Step 5: 端到端测试
./scripts/deploy-and-smoke.sh

# Step 6: 验证无报错
docker compose -f docker-compose.services.yml logs --tail=100 | grep -i error
```

**回滚计划**:
```bash
# 若升级失败，快速回滚
git checkout HEAD~1
docker compose -f docker-compose.services.yml up -d --build
./scripts/deploy-and-smoke.sh
```

---

### 数据库版本升级

**PostgreSQL 大版本升级** (如 14 → 15):
```bash
# 高风险！需要完整备份

# Step 1: 完整备份
docker exec gcrf-postgres-primary pg_dumpall -U postgres | gzip > \
  backup-before-upgrade-$(date +%Y%m%d).sql.gz

# Step 2: 停止应用
docker compose -f docker-compose.services.yml down

# Step 3: 停止旧数据库，备份卷
docker compose -f docker-compose.infrastructure.yml down
docker volume create postgres-backup
docker run --rm -v postgres-primary-data:/source -v postgres-backup:/dest \
  busybox cp -r /source/* /dest/

# Step 4: 删除旧 PostgreSQL 卷
docker volume rm postgres-primary-data

# Step 5: 编辑 docker-compose.infrastructure.yml
# image: postgres:15-alpine   # 升级版本

# Step 6: 启动新版本并恢复数据
docker compose -f docker-compose.infrastructure.yml up -d postgres-primary
sleep 60  # 初始化

gunzip -c backup-before-upgrade-20260415.sql.gz | \
  docker exec -i gcrf-postgres-primary psql -U postgres

# Step 7: 验证
docker exec gcrf-postgres-primary psql -U postgres -c "SELECT version();"

# Step 8: 启动应用
docker compose -f docker-compose.services.yml up -d
./scripts/deploy-and-smoke.sh
```

---

## 监控 Dashboard 入口

| 系统 | URL | 默认账户 | 用途 |
|------|-----|---------|------|
| **Grafana** | http://localhost:3000 | admin / admin | 可视化仪表板、告警 |
| **Prometheus** | http://localhost:9090 | - | 时间序列数据库、PromQL 查询 |
| **Nacos Console** | http://localhost:8848/nacos | nacos / `<NACOS_MYSQL_PASSWORD>` | 服务注册、配置管理 |
| **RabbitMQ** | http://localhost:15672 | guest / guest | 消息队列管理 |
| **MinIO Console** | http://localhost:9001 | `<MINIO_ROOT_USER>` / `<MINIO_ROOT_PASSWORD>` | 对象存储管理 |
| **Web Admin** | http://localhost:3011 | 见初始化数据 | 业务管理后台 |
| **API Gateway** | http://localhost:8080 | - | REST API 端点 |

### Grafana 快速开始

```bash
# 1. 登录 http://localhost:3000
# 用户: admin, 密码: admin（第一次需改密)

# 2. 添加 Prometheus 数据源
# Settings → Data Sources → Add Prometheus
# URL: http://prometheus:9090
# Save & Test

# 3. 导入仪表板
# Dashboards → Import
# ID: 1860 (Node Exporter)
# ID: 3662 (Prometheus)

# 4. 设置告警通知
# Alerting → Notification Channels
# Slack, Email 等
```

### Prometheus 常见查询

```promql
# 服务可用性
up{job="gateway"}

# CPU 使用率
rate(container_cpu_usage_seconds_total[5m])

# 内存使用 (MB)
container_memory_usage_bytes / 1024 / 1024

# HTTP 请求率
rate(http_requests_total[1m])

# HTTP 错误率
rate(http_requests_total{status=~"5.."}[5m])

# 数据库连接数
hikaricp_connections_active
```

---

## 关闭与清理

### 正常关闭

```bash
cd deployment

# Step 1: 关闭服务（graceful shutdown，120 秒超时）
docker compose -f docker-compose.services.yml down --timeout=120

# Step 2: 关闭基础设施
docker compose -f docker-compose.infrastructure.yml down

# Step 3: (可选) 关闭监控
docker compose -f docker-compose.monitoring.yml down

# 验证所有容器已停止
docker ps
```

### 清理卷数据（危险！数据丢失）

```bash
# 仅在完全重置时执行
docker compose -f docker-compose.infrastructure.yml down -v
docker compose -f docker-compose.services.yml down -v
docker compose -f docker-compose.monitoring.yml down -v

# 删除所有 gcrf 卷
docker volume ls | grep gcrf | awk '{print $2}' | xargs docker volume rm

# 确认
docker volume ls | grep gcrf  # 应该无输出
```

### 清理镜像（可选）

```bash
# 删除所有 gcrf 镜像
docker rmi $(docker images 'gcrf-library/*' -q)

# 清理未使用的镜像/卷（安全）
docker image prune -a
docker volume prune
```

---

## 性能优化

### JVM 调优

**编辑** `docker-compose.services.yml`:
```yaml
environment:
  JAVA_OPTS: >
    -Xms512m
    -Xmx2048m
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=100
    -XX:+ParallelRefProcEnabled
    -XX:+UnlockDiagnosticVMOptions
    -XX:G1NewCollectionHeuristicPercent=35
    -XX:InitiatingHeapOccupancyPercent=35
    -Djava.security.egd=file:/dev/./urandom
```

### PostgreSQL 优化

```sql
-- 连接池优化
ALTER SYSTEM SET max_connections = 1000;
ALTER SYSTEM SET shared_buffers = '4GB';
ALTER SYSTEM SET effective_cache_size = '12GB';
ALTER SYSTEM SET work_mem = '16MB';

-- 应用修改
SELECT pg_reload_conf();

-- 验证
SHOW max_connections;
```

### Redis 优化

**编辑** `docker-compose.infrastructure.yml`:
```yaml
redis-master:
  command: >
    redis-server
    --maxmemory 2gb
    --maxmemory-policy allkeys-lru
    --save 300 10
    --tcp-backlog 511
    --tcp-keepalive 300
```

### 容器资源限制

```yaml
services:
  gcrf-gateway-service:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

---

## 安全加固

### 密钥轮换

**每 90 天执行一次**:
```bash
# Step 1: 生成新密钥
NEW_JWT_SECRET=$(openssl rand -base64 32)
NEW_DB_PASSWORD=$(openssl rand -base64 32)

# Step 2: 备份当前值
grep JWT_SECRET .env > keys-backup-$(date +%Y%m%d).txt

# Step 3: 更新 .env
sed -i "s/JWT_SECRET=.*/JWT_SECRET=${NEW_JWT_SECRET}/" .env

# Step 4: 更新数据库用户密码
docker exec gcrf-postgres-primary psql -U postgres \
  -c "ALTER USER gcrf_app WITH PASSWORD '${NEW_DB_PASSWORD}';"

# Step 5: 更新 .env
sed -i "s/DB_PASSWORD=.*/DB_PASSWORD=${NEW_DB_PASSWORD}/" .env

# Step 6: 重启所有服务
docker compose -f docker-compose.services.yml up -d --no-deps --build

# Step 7: 验证
./scripts/deploy-and-smoke.sh

# Step 8: 清理备份（保存到安全位置）
rm keys-backup-*.txt
```

### 网络隔离

```bash
# PostgreSQL 仅允许本地 / 专网
# docker-compose.infrastructure.yml:
# ports:
#   - "127.0.0.1:5432:5432"    # 仅 localhost
#   - "10.0.0.0/8:5432:5432"   # 仅内网

# 验证端口绑定
lsof -i :5432 | grep LISTEN
```

### 日志审计

```bash
# 启用 PostgreSQL 查询日志
docker exec gcrf-postgres-primary psql -U postgres \
  -c "ALTER SYSTEM SET log_statement = 'all';"

docker exec gcrf-postgres-primary psql -U postgres \
  -c "SELECT pg_reload_conf();"

# 查看日志
docker logs gcrf-postgres-primary | grep LOG

# 设置日志轮转
# docker-compose.infrastructure.yml:
# logging:
#   driver: "json-file"
#   options:
#     max-size: "10m"
#     max-file: "10"
```

### 定期安全扫描

```bash
# 使用 Trivy 扫描镜像
./scripts/security-scan.sh gateway

# 漏洞报告输出
./scripts/security-scan.sh gateway --report json > vuln-report-$(date +%Y%m%d).json
```

---

## 应急恢复

### 完全数据中心故障

**假设场景**: 所有物理设备损毁，仅有离线备份

```bash
# Step 1: 在新硬件上安装 Docker
curl -fsSL https://get.docker.com | sh

# Step 2: 恢复备份文件
# 从备份存储还原 PostgreSQL dump
gunzip -c backup-all-20260415.sql.gz | \
  docker exec -i gcrf-postgres-primary psql -U postgres

# Step 3: 恢复 MinIO 数据
mc cp --recursive backup/library-files-20260415/ local/library-files/

# Step 4: 恢复 Nacos 配置
# 手动导入 configs-20260415.zip

# Step 5: 启动应用栈
docker compose -f docker-compose.infrastructure.yml up -d
docker compose -f docker-compose.services.yml up -d
docker compose -f docker-compose.monitoring.yml up -d

# Step 6: 验证数据完整性
./scripts/deploy-and-smoke.sh
docker exec gcrf-postgres-primary psql -U postgres gcrf_reader \
  -c "SELECT COUNT(*) FROM reader;"  # 应与灾难前一致
```

**恢复时间目标 (RTO)**: 2-4 小时  
**恢复点目标 (RPO)**: 最多丢失 1 天数据（取决于备份频率）

---

## 支持与反馈

### 常见资源

- **项目文档**: `/docs/architecture/architect.md` (authoritative)
- **故障检查清单**: 本文件的"故障排查"章节
- **脚本帮助**: `./scripts/<script-name>.sh --help`
- **Docker Compose 官方文档**: https://docs.docker.com/compose/

### 报告问题

```bash
# 收集诊断信息
mkdir -p diagnostics-$(date +%Y%m%d)
docker compose -f docker-compose.services.yml logs > diagnostics/services.log
docker compose -f docker-compose.infrastructure.yml logs > diagnostics/infra.log
docker stats --no-stream > diagnostics/stats.txt
docker ps -a --no-trunc > diagnostics/containers.txt
uname -a > diagnostics/system.txt

# 打包上传
tar -czf diagnostics-$(date +%Y%m%d).tar.gz diagnostics/

# 联系 DevOps 团队，附上此文件
```

---

**Last Revised**: 2026-04-15  
**Document Version**: 1.0.0  
**Maintainer**: DevOps Team
