# Phase 2: Docker Compose Orchestration - Detailed Task Plan

**Phase**: Stage 15 - Phase 2
**Status**: In Progress
**Started**: 2025-11-01
**Estimated Duration**: 3-4 hours
**Objective**: Create production-ready Docker Compose orchestration for entire GCRF stack

---

## Overview

Phase 2 focuses on orchestrating all infrastructure and services using Docker Compose. This builds on Phase 1's environment configuration to create a deployable stack that can be launched with a single command.

**Key Goals**:
1. Unified orchestration for all infrastructure (PostgreSQL, Redis, Nacos, RabbitMQ, MinIO)
2. Service dependency management and health checks
3. Network isolation and security
4. Volume management for data persistence
5. Production and development environment separation

---

## Task Breakdown (8 Tasks)

### Task 1: Create Master Infrastructure Compose File
**Agent**: `cloud-architect` + `database-architect`
**Priority**: P0 (Critical - Blocking)
**Estimated Time**: 45 minutes
**Status**: Pending

**Deliverable**: `deployment/docker-compose.infrastructure.yml`

**Requirements**:
1. **PostgreSQL Cluster** (Primary + 2 Replicas):
   - Primary: `gcrf-postgres-primary` (port 5432)
   - Replica 1: `gcrf-postgres-replica-1` (port 5433)
   - Replica 2: `gcrf-postgres-replica-2` (port 5434)
   - Streaming replication configured
   - Health checks via pg_isready
   - Named volumes for persistence

2. **Redis Cluster** (Master + 2 Slaves + 3 Sentinels):
   - Master: `gcrf-redis-master` (port 6379)
   - Slave 1: `gcrf-redis-slave-1` (port 6380)
   - Slave 2: `gcrf-redis-slave-2` (port 6382)
   - Sentinels: ports 26379, 26380, 26381
   - Health checks via redis-cli ping
   - AOF persistence enabled

3. **Nacos** (Standalone):
   - Port: 8848 (HTTP), 9848 (gRPC)
   - MySQL 8.0 backend for config storage
   - Health check: HTTP /nacos/actuator/health
   - Environment variables for authentication

4. **RabbitMQ** (Standalone):
   - Ports: 5672 (AMQP), 15672 (Management UI)
   - Management plugin enabled
   - Health check: rabbitmqctl status
   - Named volume for data

5. **MinIO** (Standalone):
   - Ports: 9000 (API), 9001 (Console)
   - Health check: /minio/health/live
   - Named volume for object storage

**Network Configuration**:
- Network: `gcrf-infrastructure-network` (bridge)
- Internal DNS resolution enabled
- No external exposure except management ports

**Environment Variables**:
- Load from `.env` file
- All secrets externalized (DB passwords, Redis password, Nacos auth)
- Use Phase 1 validation script compatible format

**Health Checks**:
- All services must have health checks
- Retry intervals: 30s
- Start period: 60s for databases, 30s for others
- Failure threshold: 3 retries

**Example Structure**:
```yaml
version: '3.8'

services:
  postgres-primary:
    image: postgres:15-alpine
    container_name: gcrf-postgres-primary
    environment:
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres-primary-data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - gcrf-infrastructure-network

networks:
  gcrf-infrastructure-network:
    driver: bridge
    name: gcrf-infrastructure-network

volumes:
  postgres-primary-data:
    name: gcrf-postgres-primary-data
```

**Success Criteria**:
- [ ] All 5 infrastructure services defined
- [ ] Health checks pass within 5 minutes
- [ ] Services can communicate via internal DNS
- [ ] Data persists across container restarts
- [ ] No hardcoded secrets

---

### Task 2: Create Services Compose File
**Agent**: `kubernetes-architect` + `backend-architect`
**Priority**: P0 (Critical - Blocking)
**Estimated Time**: 30 minutes
**Status**: Pending

**Deliverable**: `deployment/docker-compose.services.yml`

**Requirements**:
1. **Gateway Service**:
   - Image: `gcrf-library/gateway-service:latest`
   - Port: 8080 (external)
   - Depends on: nacos, redis, auth-service
   - Health check: /actuator/health
   - Environment: Uses application-prod.yml from Phase 1
   - Restart policy: unless-stopped

2. **Auth Service**:
   - Image: `gcrf-library/auth-service:latest`
   - Port: 8081 (internal only)
   - Depends on: postgres-primary, redis, nacos
   - Health check: /actuator/health
   - Environment: Uses application-prod.yml from Phase 1
   - Restart policy: unless-stopped

3. **Web Admin** (Future - Phase 3):
   - Image: `gcrf-library/web-admin:latest`
   - Port: 80 (Nginx)
   - Depends on: gateway-service
   - Health check: HTTP 200 on /
   - Restart policy: unless-stopped

**Dependency Management**:
```yaml
depends_on:
  postgres-primary:
    condition: service_healthy
  redis-master:
    condition: service_healthy
  nacos:
    condition: service_healthy
```

**Network Configuration**:
- Backend services: `gcrf-backend-network` (connects to infrastructure)
- Gateway: Both `gcrf-backend-network` and `gcrf-frontend-network`

**Environment Variables**:
- Use `env_file: ../.env` for consistency
- Override specific variables with `environment:` section
- Reference Phase 1 ENVIRONMENT_VARIABLES.md

**Logging**:
- JSON format for structured logging
- Volume mount to host: `./logs:/app/logs`
- Log rotation configured

**Resource Limits** (Production):
```yaml
deploy:
  resources:
    limits:
      cpus: '1.0'
      memory: 1G
    reservations:
      cpus: '0.5'
      memory: 512M
```

**Success Criteria**:
- [ ] Gateway and Auth services defined
- [ ] Correct dependency order
- [ ] Health checks functional
- [ ] Services can discover each other via Nacos
- [ ] Logs accessible on host

---

### Task 3: Create Network Security Configuration
**Agent**: `network-engineer` + `security-auditor`
**Priority**: P1 (High)
**Estimated Time**: 30 minutes
**Status**: Pending

**Deliverable**: Network definitions in compose files + documentation

**Requirements**:

1. **Network Topology**:
```
┌─────────────────────────────────────────┐
│     gcrf-frontend-network (DMZ)         │
│  ┌──────────┐         ┌──────────┐     │
│  │ web-admin│─────────│ gateway  │     │
│  └──────────┘         └────┬─────┘     │
└────────────────────────────┼───────────┘
                             │
┌────────────────────────────┼───────────┐
│     gcrf-backend-network (Internal)    │
│                      ┌─────┴──────┐    │
│  ┌──────────┐        │   auth     │    │
│  │ book     │        └────────────┘    │
│  └──────────┘              │           │
└────────────────────────────┼───────────┘
                             │
┌────────────────────────────┼───────────┐
│  gcrf-infrastructure-network (Private) │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ │
│  │ PG   │ │ Redis│ │ Nacos│ │ MQ   │ │
│  └──────┘ └──────┘ └──────┘ └──────┘ │
└────────────────────────────────────────┘
```

2. **Network Definitions**:
```yaml
networks:
  gcrf-frontend-network:
    driver: bridge
    name: gcrf-frontend-network
    internal: false  # Exposed to host

  gcrf-backend-network:
    driver: bridge
    name: gcrf-backend-network
    internal: false  # Can access infrastructure

  gcrf-infrastructure-network:
    driver: bridge
    name: gcrf-infrastructure-network
    internal: true   # Isolated, no external access
```

3. **Service Network Assignment**:
- Frontend: `gcrf-frontend-network` only
- Gateway: `gcrf-frontend-network` + `gcrf-backend-network`
- Backend services: `gcrf-backend-network` + `gcrf-infrastructure-network`
- Infrastructure: `gcrf-infrastructure-network` only

4. **Port Exposure Rules**:
- **Public** (0.0.0.0): 80 (web-admin), 8080 (gateway)
- **Internal** (127.0.0.1): 8848 (Nacos UI), 15672 (RabbitMQ UI), 9001 (MinIO Console)
- **No Exposure**: Database ports, Redis ports, backend service ports

5. **Firewall Rules** (Documentation):
```bash
# Allow only gateway to accept external traffic
iptables -A INPUT -p tcp --dport 8080 -j ACCEPT

# Block direct access to backend services
iptables -A INPUT -p tcp --dport 8081:8099 -j DROP

# Allow internal network communication
iptables -A INPUT -i docker0 -j ACCEPT
```

**Security Best Practices**:
- Principle of least privilege
- No root user in containers
- Read-only root filesystems where possible
- Drop unnecessary capabilities
- Use security_opt for AppArmor/SELinux

**Success Criteria**:
- [ ] 3 networks defined with correct isolation
- [ ] Services can only communicate with intended targets
- [ ] External access limited to web-admin and gateway
- [ ] Documentation includes firewall rules
- [ ] Security best practices documented

---

### Task 4: Configure Volume Management & Backup Strategy
**Agent**: `devops-troubleshooter` + `database-admin`
**Priority**: P1 (High)
**Estimated Time**: 30 minutes
**Status**: Pending

**Deliverable**: Volume definitions + backup script

**Requirements**:

1. **Named Volumes** (Persistent data):
```yaml
volumes:
  # PostgreSQL
  gcrf-postgres-primary-data:
    name: gcrf-postgres-primary-data
    driver: local
    driver_opts:
      type: none
      device: /data/gcrf/postgres/primary
      o: bind

  gcrf-postgres-replica-1-data:
    name: gcrf-postgres-replica-1-data

  gcrf-postgres-replica-2-data:
    name: gcrf-postgres-replica-2-data

  # Redis
  gcrf-redis-master-data:
    name: gcrf-redis-master-data

  gcrf-redis-slave-1-data:
    name: gcrf-redis-slave-1-data

  gcrf-redis-slave-2-data:
    name: gcrf-redis-slave-2-data

  # Nacos
  gcrf-nacos-data:
    name: gcrf-nacos-data

  # RabbitMQ
  gcrf-rabbitmq-data:
    name: gcrf-rabbitmq-data

  # MinIO
  gcrf-minio-data:
    name: gcrf-minio-data
```

2. **Bind Mounts** (Logs and configs):
```yaml
services:
  gateway-service:
    volumes:
      - ./logs/gateway:/app/logs
      - ./config/gateway/application-prod.yml:/app/config/application.yml:ro
```

3. **Backup Script**: `deployment/scripts/backup-volumes.sh`
```bash
#!/bin/bash
# Backup all Docker volumes to compressed archives

BACKUP_DIR="/data/backups/$(date +%Y%m%d)"
mkdir -p "$BACKUP_DIR"

# Backup PostgreSQL
docker exec gcrf-postgres-primary pg_dumpall -U postgres | gzip > "$BACKUP_DIR/postgres-all.sql.gz"

# Backup Redis
docker exec gcrf-redis-master redis-cli --rdb /data/dump.rdb SAVE
docker cp gcrf-redis-master:/data/dump.rdb "$BACKUP_DIR/redis-dump.rdb"

# Backup volumes as tar archives
for volume in postgres-primary-data redis-master-data nacos-data; do
  docker run --rm -v gcrf-${volume}:/data -v "$BACKUP_DIR":/backup \
    alpine tar czf /backup/${volume}.tar.gz -C /data .
done
```

4. **Restore Script**: `deployment/scripts/restore-volumes.sh`

5. **Volume Cleanup Script**: `deployment/scripts/cleanup-volumes.sh`

**Backup Strategy**:
- Daily incremental backups at 2 AM
- Weekly full backups on Sunday
- 30-day retention policy
- Off-site backup to S3/MinIO

**Success Criteria**:
- [ ] All volumes defined with correct drivers
- [ ] Backup script creates compressed archives
- [ ] Restore script can recover from backups
- [ ] Backup automation documented
- [ ] Retention policy implemented

---

### Task 5: Implement Health Checks & Startup Orchestration
**Agent**: `incident-responder` + `observability-engineer`
**Priority**: P0 (Critical)
**Estimated Time**: 30 minutes
**Status**: Pending

**Deliverable**: Health check configurations + startup script

**Requirements**:

1. **Health Check Patterns**:

**HTTP Health Checks** (Spring Boot services):
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

**Database Health Checks**:
```yaml
# PostgreSQL
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U postgres -h localhost"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 60s

# Redis
healthcheck:
  test: ["CMD", "redis-cli", "ping"]
  interval: 10s
  timeout: 3s
  retries: 3
  start_period: 30s
```

**TCP Health Checks** (Nacos, RabbitMQ):
```yaml
healthcheck:
  test: ["CMD-SHELL", "nc -z localhost 8848 || exit 1"]
  interval: 30s
  timeout: 5s
  retries: 3
  start_period: 90s
```

2. **Dependency Management**:
```yaml
services:
  auth-service:
    depends_on:
      postgres-primary:
        condition: service_healthy
      redis-master:
        condition: service_healthy
      nacos:
        condition: service_healthy
```

3. **Startup Orchestration Script**: `deployment/scripts/start-stack.sh`
```bash
#!/bin/bash
# Orchestrated startup with health monitoring

set -e

echo "Starting GCRF infrastructure..."
docker-compose -f docker-compose.infrastructure.yml up -d

echo "Waiting for infrastructure to be healthy..."
./wait-for-healthy.sh postgres-primary redis-master nacos rabbitmq minio

echo "Starting backend services..."
docker-compose -f docker-compose.services.yml up -d

echo "Waiting for services to be healthy..."
./wait-for-healthy.sh auth-service gateway-service

echo "✅ All services are healthy and ready!"
docker-compose ps
```

4. **Health Monitoring Script**: `deployment/scripts/wait-for-healthy.sh`
```bash
#!/bin/bash
# Wait for Docker services to be healthy

MAX_WAIT=300  # 5 minutes
INTERVAL=5

for service in "$@"; do
  elapsed=0
  while [ $elapsed -lt $MAX_WAIT ]; do
    status=$(docker inspect --format='{{.State.Health.Status}}' "$service" 2>/dev/null || echo "not_found")

    if [ "$status" = "healthy" ]; then
      echo "✓ $service is healthy"
      break
    fi

    echo "⏳ Waiting for $service... ($elapsed/$MAX_WAIT seconds)"
    sleep $INTERVAL
    elapsed=$((elapsed + INTERVAL))
  done

  if [ $elapsed -ge $MAX_WAIT ]; then
    echo "✗ $service failed to become healthy"
    exit 1
  fi
done
```

5. **Graceful Shutdown Script**: `deployment/scripts/stop-stack.sh`
```bash
#!/bin/bash
# Graceful shutdown with data flush

echo "Stopping services (gracefully)..."
docker-compose -f docker-compose.services.yml down --timeout 30

echo "Flushing Redis data..."
docker exec gcrf-redis-master redis-cli SAVE

echo "Stopping infrastructure..."
docker-compose -f docker-compose.infrastructure.yml down --timeout 60

echo "✅ All services stopped gracefully"
```

**Success Criteria**:
- [ ] All services have working health checks
- [ ] Startup script respects dependencies
- [ ] Health monitoring detects failures
- [ ] Graceful shutdown prevents data loss
- [ ] Startup completes within 5 minutes

---

### Task 6: Create Development Override Configuration
**Agent**: `dx-optimizer`
**Priority**: P2 (Medium)
**Estimated Time**: 20 minutes
**Status**: Pending

**Deliverable**: `deployment/docker-compose.override.yml`

**Requirements**:

1. **Purpose**: Developer-friendly overrides for local development

2. **Key Differences from Production**:
```yaml
version: '3.8'

services:
  # Simplified PostgreSQL (no replication)
  postgres-primary:
    ports:
      - "5432:5432"  # Expose for local access
    environment:
      POSTGRES_PASSWORD: gcrf_secure_2024  # Simple dev password
    command: postgres -c log_statement=all  # Verbose logging

  # Simplified Redis (no cluster)
  redis-master:
    ports:
      - "6379:6379"
    command: redis-server --loglevel debug --save 60 1

  # Nacos with local credentials
  nacos:
    ports:
      - "8848:8848"
      - "9848:9848"
    environment:
      MODE: standalone
      SPRING_DATASOURCE_PLATFORM: embedded  # No external MySQL

  # Services with debug mode
  gateway-service:
    ports:
      - "8080:8080"
      - "5005:5005"  # Debug port
    environment:
      SPRING_PROFILES_ACTIVE: dev
      JAVA_TOOL_OPTIONS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
    volumes:
      - ./backend/gateway-service/target:/app:ro  # Hot reload

  auth-service:
    ports:
      - "8081:8081"
      - "5006:5006"  # Debug port
    environment:
      SPRING_PROFILES_ACTIVE: dev
      JAVA_TOOL_OPTIONS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006"
```

3. **Features**:
- All management ports exposed (Nacos UI, RabbitMQ UI, MinIO Console)
- Debug ports enabled for remote debugging
- Verbose logging for troubleshooting
- Hot reload support with volume mounts
- Simplified passwords (documented as dev-only)
- No resource limits

4. **Usage**:
```bash
# Development mode (auto-loads docker-compose.override.yml)
docker-compose up -d

# Production mode (explicit file)
docker-compose -f docker-compose.infrastructure.yml -f docker-compose.services.yml up -d
```

**Success Criteria**:
- [ ] Override file simplifies local development
- [ ] All ports accessible from host
- [ ] Debug connections work in IDEs
- [ ] Hot reload functional
- [ ] Documentation explains dev vs prod differences

---

### Task 7: Implement Service Discovery Integration
**Agent**: `kubernetes-architect` + `backend-architect`
**Priority**: P1 (High)
**Estimated Time**: 30 minutes
**Status**: Pending

**Deliverable**: Nacos integration documentation + configuration

**Requirements**:

1. **Nacos Service Registration**:
```yaml
# In each service's application-prod.yml (already done in Phase 1)
spring:
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:nacos:8848}
        namespace: ${NACOS_NAMESPACE:prod}
        group: LIBRARY_GROUP
        username: ${NACOS_USERNAME}
        password: ${NACOS_PASSWORD}
        register-enabled: true
```

2. **Docker Compose Nacos Configuration**:
```yaml
services:
  nacos:
    image: nacos/nacos-server:v2.3.0
    container_name: gcrf-nacos
    environment:
      MODE: standalone
      SPRING_DATASOURCE_PLATFORM: mysql
      MYSQL_SERVICE_HOST: nacos-mysql
      MYSQL_SERVICE_DB_NAME: nacos_config
      MYSQL_SERVICE_USER: nacos
      MYSQL_SERVICE_PASSWORD: ${NACOS_MYSQL_PASSWORD}
      NACOS_AUTH_ENABLE: true
      NACOS_AUTH_TOKEN: ${NACOS_AUTH_TOKEN}
      NACOS_AUTH_IDENTITY_KEY: ${NACOS_USERNAME}
      NACOS_AUTH_IDENTITY_VALUE: ${NACOS_PASSWORD}
    depends_on:
      nacos-mysql:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8848/nacos/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 60s

  nacos-mysql:
    image: mysql:8.0
    container_name: gcrf-nacos-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${NACOS_MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: nacos_config
      MYSQL_USER: nacos
      MYSQL_PASSWORD: ${NACOS_MYSQL_PASSWORD}
    volumes:
      - nacos-mysql-data:/var/lib/mysql
      - ./nacos-init.sql:/docker-entrypoint-initdb.d/nacos-init.sql:ro
```

3. **Service Discovery Testing**:
```bash
# Script: test-service-discovery.sh
#!/bin/bash

echo "Testing Nacos service registration..."

TOKEN=$(curl -s -X POST "http://localhost:8848/nacos/v1/auth/login" \
  -d "username=${NACOS_USERNAME}&password=${NACOS_PASSWORD}" | jq -r '.accessToken')

# Check gateway registration
curl -s "http://localhost:8848/nacos/v1/ns/instance/list?accessToken=$TOKEN&serviceName=gateway-service" | jq .

# Check auth service registration
curl -s "http://localhost:8848/nacos/v1/ns/instance/list?accessToken=$TOKEN&serviceName=auth-service" | jq .
```

4. **Nacos Configuration Management**:
- Document how to push shared configs to Nacos
- Document config refresh strategies
- Provide examples for `@RefreshScope`

**Success Criteria**:
- [ ] Nacos starts with MySQL backend
- [ ] Services auto-register on startup
- [ ] Gateway can discover auth-service via Nacos
- [ ] Configuration management documented
- [ ] Testing script validates registration

---

### Task 8: Create Comprehensive Docker Compose Documentation
**Agent**: `docs-architect`
**Priority**: P2 (Medium)
**Estimated Time**: 30 minutes
**Status**: Pending

**Deliverable**: `deployment/docs/DOCKER_COMPOSE_GUIDE.md`

**Requirements**:

1. **Quick Start Guide**:
```markdown
## Quick Start

### Prerequisites
- Docker 24.0+
- Docker Compose 2.20+
- 8GB RAM minimum
- 50GB disk space

### Start the Stack
```bash
# Load environment variables
source .env

# Validate configuration
./deployment/scripts/validate-config.sh prod

# Start infrastructure
docker-compose -f deployment/docker-compose.infrastructure.yml up -d

# Wait for health checks
./deployment/scripts/wait-for-healthy.sh postgres-primary redis-master nacos

# Start services
docker-compose -f deployment/docker-compose.services.yml up -d

# Verify all services
docker-compose ps
```

2. **Architecture Diagram**:
- Service topology
- Network diagram
- Volume mappings
- Port mappings

3. **Common Operations**:
- Starting/stopping services
- Viewing logs (`docker-compose logs -f service-name`)
- Executing commands in containers
- Accessing management UIs
- Scaling services

4. **Troubleshooting Section**:
```markdown
## Troubleshooting

### Service Won't Start
1. Check health status: `docker ps -a`
2. View logs: `docker-compose logs service-name`
3. Check dependencies: `docker inspect service-name`
4. Verify environment variables: `./validate-config.sh prod`

### Port Already in Use
```bash
# Find process using port
lsof -i :8080

# Stop conflicting service
docker stop $(docker ps -q --filter "expose=8080")
```

### Database Connection Failed
1. Check PostgreSQL health: `docker exec gcrf-postgres-primary pg_isready`
2. Test connection: `docker exec -it gcrf-postgres-primary psql -U postgres`
3. Verify credentials in .env file
```

5. **Backup and Recovery**:
- How to backup volumes
- How to restore from backup
- Disaster recovery procedures

6. **Performance Tuning**:
- Resource limits configuration
- Connection pool tuning
- Cache optimization

7. **Security Checklist**:
- [ ] All default passwords changed
- [ ] Firewall rules configured
- [ ] SSL/TLS enabled
- [ ] Secrets in environment variables (not compose files)
- [ ] Non-root users in containers

**Success Criteria**:
- [ ] Complete quick start guide
- [ ] Architecture diagrams included
- [ ] Common operations documented
- [ ] Troubleshooting covers main issues
- [ ] Security checklist provided

---

## Task Execution Order

### Parallel Track A (Infrastructure)
1. **Task 1** → **Task 4** → **Task 7**
   - Infrastructure setup → Volume management → Service discovery

### Parallel Track B (Services & Security)
2. **Task 2** → **Task 3** → **Task 5**
   - Services definition → Network security → Health checks

### Final Tasks
3. **Task 6** (Dev overrides) - Can start anytime after Task 1
4. **Task 8** (Documentation) - Can start anytime, complete last

**Critical Path**: Task 1 → Task 2 → Task 5
**Estimated Total Time**: 3-4 hours

---

## Agent Assignment Summary

| Task | Agent(s) | Duration | Dependencies |
|------|----------|----------|--------------|
| 1. Infrastructure Compose | `cloud-architect` + `database-architect` | 45 min | None (Start first) |
| 2. Services Compose | `kubernetes-architect` + `backend-architect` | 30 min | Task 1 |
| 3. Network Security | `network-engineer` + `security-auditor` | 30 min | Task 2 |
| 4. Volume Management | `devops-troubleshooter` + `database-admin` | 30 min | Task 1 |
| 5. Health Checks | `incident-responder` + `observability-engineer` | 30 min | Task 2 |
| 6. Dev Overrides | `dx-optimizer` | 20 min | Task 1 |
| 7. Service Discovery | `kubernetes-architect` + `backend-architect` | 30 min | Task 1 |
| 8. Documentation | `docs-architect` | 30 min | All tasks |

---

## Success Criteria (Phase 2 Complete)

- [  ] `docker-compose up -d` starts entire stack successfully
- [  ] All services pass health checks within 5 minutes
- [  ] Services can communicate via Docker networks
- [  ] Data persists across container restarts
- [  ] Graceful shutdown without data loss
- [  ] Configuration validation passes
- [  ] Service discovery functional (Nacos)
- [  ] Logs accessible and structured
- [  ] Development overrides work for local dev
- [  ] Comprehensive documentation complete

---

## Deliverables Checklist

**Configuration Files**:
- [  ] `deployment/docker-compose.infrastructure.yml`
- [  ] `deployment/docker-compose.services.yml`
- [  ] `deployment/docker-compose.override.yml` (dev)
- [  ] `.dockerignore` files

**Scripts**:
- [  ] `deployment/scripts/start-stack.sh`
- [  ] `deployment/scripts/stop-stack.sh`
- [  ] `deployment/scripts/wait-for-healthy.sh`
- [  ] `deployment/scripts/backup-volumes.sh`
- [  ] `deployment/scripts/restore-volumes.sh`
- [  ] `deployment/scripts/test-service-discovery.sh`

**Documentation**:
- [  ] `deployment/docs/DOCKER_COMPOSE_GUIDE.md`
- [  ] Network architecture diagram
- [  ] Volume mapping documentation
- [  ] Troubleshooting guide

---

## Next Actions

**User Choice**: Which approach to execute Phase 2?

**Option A**: Execute all 8 tasks systematically (recommended, 3-4 hours)
**Option B**: Execute only critical path tasks 1, 2, 5 first (1.5 hours)
**Option C**: Let me know specific priorities or ask questions

---

**Created**: 2025-11-01
**Status**: Ready for execution
**Depends On**: Phase 1 (✅ Complete)
