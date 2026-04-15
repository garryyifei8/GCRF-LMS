# P3: 部署生产化与端到端验证

**日期：** 2026-04-15
**状态：** Approved
**优先级：** P3（生产上线最后一公里）
**背景：** 已有完整 docker-compose（infra/services/monitoring/observability）、SSL/Certbot、k6 性能、安全扫描 CI、54 个部署脚本。但 7/11 服务无 Dockerfile，`docker-compose.services.yml` 仅注册 3 服务，CI 缺少 mock 校验和 docker build smoke。本期闭环。

---

## 目标

1. 补齐 7 个 missing service Dockerfiles（统一模板）
2. `docker-compose.services.yml` 注册全部 11 个服务（含 web-admin）
3. CI 增加 `mock:check` + docker build smoke job（matrix）
4. 端到端 smoke 脚本 `deploy-and-smoke.sh`
5. 部署 runbook（前置依赖、启动顺序、故障排查、备份恢复）

最终：**git clone → 一条命令 → 全栈起 → 健康检查通过**。

---

## Dockerfile 统一模板

参考 `gateway-service/Dockerfile.optimized`，所有后端服务用同一模板（multi-stage build + JRE-only runtime）：

```dockerfile
# === Build stage ===
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy parent pom and common modules first (layer cache friendly)
COPY pom.xml .
COPY common/ common/

# Copy this service's pom to download deps
COPY <service-name>/pom.xml <service-name>/

# Pre-download dependencies (cached when source unchanged)
RUN mvn -pl <service-name> -am dependency:go-offline -DskipTests -B

# Copy source and build
COPY <service-name>/src <service-name>/src
RUN mvn -pl <service-name> -am clean package -DskipTests -B

# === Runtime stage ===
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder /build/<service-name>/target/*.jar app.jar

EXPOSE <port>

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:<port>/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

### 7 个待创建 Dockerfile

| Service              | Port |
| -------------------- | ---- |
| circulation-service  | 8083 |
| reader-service       | 8084 |
| system-service       | 8085 |
| notification-service | 8086 |
| analytics-service    | 8087 |
| chat-service         | 8088 |
| recommend-service    | 8089 |

实际端口以各自 `application.yml` 为准（agent 实施前必须 grep 验证）。

---

## docker-compose.services.yml 完整化

为每个服务添加 service block，注册到统一 `gcrf-network`：

```yaml
services:
  gateway-service:
    image: gcrf-library/gateway-service:latest
    build:
      context: ../backend
      dockerfile: gateway-service/Dockerfile
    container_name: gcrf-gateway
    ports: ["8080:8080"]
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - NACOS_HOST=nacos
      - NACOS_PORT=8848
    depends_on:
      nacos: { condition: service_healthy }
      redis: { condition: service_healthy }
    networks: [gcrf-network]
    restart: unless-stopped
    healthcheck:
      test:
        [
          "CMD",
          "wget",
          "--quiet",
          "--tries=1",
          "--spider",
          "http://localhost:8080/actuator/health",
        ]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 60s

  # ... repeat for all 11 services with appropriate depends_on per service category
```

**约定：**

- gateway 依赖 nacos + redis
- 业务服务依赖 nacos + redis + postgres + rabbitmq（按需）
- web-admin 依赖 gateway
- 所有服务挂同一 network

---

## CI 增强（`.github/workflows/ci.yml`）

### 修改：`lint-frontend`

```yaml
lint-frontend:
  name: Lint Frontend
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-node@v4
      with:
        node-version: 20
        cache: npm
        cache-dependency-path: web-admin/package-lock.json
    - run: cd web-admin && npm ci
    - name: ESLint
      run: cd web-admin && npm run lint
    - name: Prettier check
      run: cd web-admin && npx prettier --check "src/**/*.{vue,js,css,json}"
    - name: MSW Mock Coverage
      run: cd web-admin && npm run mock:check
```

### 新增：`docker-build-smoke`

```yaml
docker-build-smoke:
  name: Docker Build Smoke
  runs-on: ubuntu-latest
  strategy:
    fail-fast: false
    matrix:
      service:
        - gateway-service
        - auth-service
        - book-service
        - circulation-service
        - reader-service
        - system-service
        - notification-service
        - analytics-service
        - chat-service
        - recommend-service
  steps:
    - uses: actions/checkout@v4
    - uses: docker/setup-buildx-action@v3
    - name: Build ${{ matrix.service }}
      uses: docker/build-push-action@v5
      with:
        context: backend
        file: backend/${{ matrix.service }}/Dockerfile
        push: false
        tags: gcrf-library/${{ matrix.service }}:ci
        cache-from: type=gha,scope=${{ matrix.service }}
        cache-to: type=gha,mode=max,scope=${{ matrix.service }}
```

不 push、不存 artifact，仅验证 Dockerfile 可 build。

---

## End-to-End Smoke 脚本

**新建：** `deployment/scripts/deploy-and-smoke.sh`

```bash
#!/usr/bin/env bash
# Bring up full stack, wait for health, smoke-test all /actuator/health
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

log() { echo -e "\033[36m[$(date +%H:%M:%S)]\033[0m $*"; }
ok()  { echo -e "  \033[32m✓\033[0m $*"; }
err() { echo -e "  \033[31m✗\033[0m $*"; }

log "Phase 1: Infrastructure"
docker compose -f docker-compose.infrastructure.yml up -d
log "Waiting 30s for infra warm-up..."
sleep 30
docker compose -f docker-compose.infrastructure.yml ps

log "Phase 2: Build + start services"
docker compose -f docker-compose.services.yml up -d --build

log "Phase 3: Wait for service healthy"
SERVICES=(
  "gateway:8080"
  "auth:8081"
  "book:8082"
  "circulation:8083"
  "reader:8084"
  "system:8085"
  "notification:8086"
  "analytics:8087"
  "chat:8088"
  "recommend:8089"
)
TIMEOUT_PER_SERVICE=180
FAILED=()

for entry in "${SERVICES[@]}"; do
  IFS=: read -r name port <<< "$entry"
  printf "  Waiting %-16s (port %s)..." "$name" "$port"
  for i in $(seq 1 "$TIMEOUT_PER_SERVICE"); do
    if curl -sf "http://localhost:${port}/actuator/health" > /dev/null 2>&1; then
      printf " \033[32m✓\033[0m %ds\n" "$i"
      break
    fi
    sleep 1
    if [ "$i" -eq "$TIMEOUT_PER_SERVICE" ]; then
      printf " \033[31m✗\033[0m timeout\n"
      FAILED+=("$name")
    fi
  done
done

log "Phase 4: Final report"
if [ ${#FAILED[@]} -gt 0 ]; then
  err "Services failed health check: ${FAILED[*]}"
  for name in "${FAILED[@]}"; do
    log "Logs for ${name}-service:"
    docker compose -f docker-compose.services.yml logs --tail=50 "${name}-service" || true
  done
  exit 1
fi

ok "All ${#SERVICES[@]} services healthy."
exit 0
```

权限：`chmod +x`。可本地跑、可 nightly CI 跑。

---

## 部署 Runbook

**新建：** `docs/deployment/RUNBOOK.md`

包含：

1. **前置依赖**
   - Docker Engine 24+, Compose v2
   - 16 GB RAM, 30 GB disk
   - Java 21（仅本地构建用）
2. **环境变量**
   - 复制 `deployment/.env.infrastructure.example` → `deployment/.env`
   - 必填：DB 密码、JWT secret、MinIO 密钥
3. **首次启动顺序**
   - Step 1: `docker compose -f docker-compose.infrastructure.yml up -d` + 等待 30s
   - Step 2: 导入 Nacos 初始 config（`scripts/import-nacos-config.sh`）
   - Step 3: 执行 Flyway 迁移（首次自动跑）
   - Step 4: `docker compose -f docker-compose.services.yml up -d --build`
   - Step 5: 启动监控 `docker compose -f docker-compose.monitoring.yml up -d`（可选）
4. **健康检查命令**
   - `./scripts/deploy-and-smoke.sh`
   - `curl http://localhost:8080/actuator/health`
5. **常见故障**
   - Nacos 连不上：检查 8848 + 9848 端口
   - PG schema 缺失：手动跑 Flyway repair
   - 端口占用：`lsof -i :<port>`
   - 内存不足：减少并发服务数（`docker compose up gateway-service auth-service`）
6. **备份/恢复**
   - PG: `pg_dump -h localhost -U postgres > backup.sql`
   - MinIO: `mc cp --recursive minio/library-files ./backup/`
   - Nacos: 导出 config 到 zip
7. **升级流程**
   - rolling restart：`docker compose up -d --build <service>`
   - 验证：`curl /actuator/health`

---

## 实现风险与缓解

| 风险                                           | 缓解                                                                                              |
| ---------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| 7 个新 Dockerfile 与现有 3 个 optimized 不一致 | 用同一模板，gateway/auth/book 后续可统一（不在本期范围）                                          |
| 11 服务起栈耗资源                              | runbook 写明 16GB+ 要求；CI smoke 用 matrix 单服务 build                                          |
| `actuator/health` 未暴露                       | T6 校验所有 application.yml 含 `management.endpoints.web.exposure.include=health,info,prometheus` |
| smoke 脚本 timeout 过短                        | 默认 180s/服务，可参数化                                                                          |
| 端口实际值与设计不符                           | T1 实施前必须 grep `application.yml` 确认                                                         |
| 缺失 Nacos 初始 config                         | runbook + Nacos 导入脚本（可能已存在）                                                            |

---

## 验证

```bash
# 本地（如有 Docker）
cd deployment
./scripts/deploy-and-smoke.sh
# 预期：全部 ✓ healthy

# CI
git push → docker-build-smoke matrix 10 个服务全绿
            lint-frontend 包含 mock:check
```

---

## 文件修改清单

### 新建（10 个）

| 文件                                               | 用途                   |
| -------------------------------------------------- | ---------------------- |
| `backend/circulation-service/Dockerfile`           | 服务镜像               |
| `backend/reader-service/Dockerfile`                | 服务镜像               |
| `backend/system-service/Dockerfile`                | 服务镜像               |
| `backend/notification-service/Dockerfile`          | 服务镜像               |
| `backend/analytics-service/Dockerfile`             | 服务镜像               |
| `backend/chat-service/Dockerfile`                  | 服务镜像               |
| `backend/recommend-service/Dockerfile`             | 服务镜像               |
| `deployment/scripts/deploy-and-smoke.sh`           | 端到端 smoke 脚本      |
| `docs/deployment/RUNBOOK.md`                       | 部署手册               |
| `.github/workflows/docker-build.yml` 或合入 ci.yml | docker-build-smoke job |

### 修改（≥ 3 个）

| 文件                                                       | 变更                                                     |
| ---------------------------------------------------------- | -------------------------------------------------------- |
| `deployment/docker-compose.services.yml`                   | 注册 7 个新服务 + 补 web-admin                           |
| `.github/workflows/ci.yml`                                 | lint-frontend 加 mock:check；可能合入 docker-build-smoke |
| `backend/<service>/src/main/resources/application.yml` × 7 | 必要时补 actuator endpoint 暴露配置                      |

---

## 执行策略

### Phase 1（4 agents 并行 — 基础设施）

- **T1**：7 个 Dockerfile 创建（一个 agent 一次完成全部，确保模板一致）
- **T2**：`docker-compose.services.yml` 完整化
- **T3**：`deploy-and-smoke.sh` 脚本
- **T4**：`docs/deployment/RUNBOOK.md`

### Phase 2（2 agents 并行）

- **T5**：CI `ci.yml` 加 mock:check + docker-build-smoke job
- **T6**：校验所有 application.yml actuator 配置（缺则补）

### Phase 3（验证）

- **T7**：本地 `deploy-and-smoke.sh` 端到端验证（如 Docker 资源足够，否则依赖 CI）

T1-T6 全部完成后再进入 T7。
