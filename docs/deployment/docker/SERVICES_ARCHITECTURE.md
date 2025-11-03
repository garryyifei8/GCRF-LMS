# GCRF Library Management System - Services Architecture

## Overview
This document describes the microservices architecture deployed via Docker Compose.

## Service Definitions

### 1. Gateway Service (`gcrf-gateway-service`)
- **Purpose**: API Gateway and request routing
- **Port**: 8080 (exposed to host)
- **Dependencies**: Nacos, Redis, Auth Service
- **Networks**: Backend + Frontend (bridge between internal and external)
- **Key Features**:
  - Central entry point for all API requests
  - JWT token validation via Redis cache
  - Service discovery via Nacos
  - Request routing and load balancing
  - Rate limiting and circuit breaking

### 2. Auth Service (`gcrf-auth-service`)
- **Purpose**: Authentication and authorization
- **Port**: 8081 (internal only)
- **Dependencies**: PostgreSQL, Redis, Nacos
- **Networks**: Backend only
- **Key Features**:
  - User authentication (login/logout)
  - JWT token generation and validation
  - User session management in Redis
  - Password encryption with BCrypt
  - Role-based access control (RBAC)

### 3. Web Admin (Placeholder)
- **Purpose**: Administrative web interface
- **Port**: 80 (when deployed)
- **Dependencies**: Gateway Service
- **Networks**: Frontend only
- **Status**: To be implemented in Phase 3

## Network Architecture

```
Internet
    ↓
[Port 80] → Web Admin (Frontend Network)
    ↓
[Port 8080] → Gateway Service (Frontend + Backend Networks)
    ↓
Backend Network (gcrf-infrastructure-network)
    ├── Auth Service (8081)
    ├── PostgreSQL (5432)
    ├── Redis (6379)
    ├── Nacos (8848)
    ├── RabbitMQ (5672)
    └── MinIO (9000)
```

### Network Segmentation
1. **Frontend Network** (`172.29.0.0/16`):
   - Web Admin → Gateway communication
   - Exposed to host for web access

2. **Backend Network** (`172.28.0.0/16`):
   - Internal service communication
   - Infrastructure services
   - Not directly accessible from outside

## Resource Allocation

### Production Settings
Each service has defined resource limits:
- **CPU**: 1.0 cores max, 0.5 cores reserved
- **Memory**: 1GB max, 512MB reserved
- **JVM**: -Xms512m -Xmx1024m with G1GC

### Logging Configuration
- **Driver**: json-file
- **Rotation**: 10MB per file, max 3 files
- **Format**: Structured JSON for log aggregation

## Health Checks
All services implement health endpoints:
- **Endpoint**: `/actuator/health`
- **Interval**: 30 seconds
- **Timeout**: 10 seconds
- **Retries**: 3
- **Start Period**: 60 seconds

## Environment Variables
Services load configuration from:
1. Root `.env` file (primary)
2. Service-specific overrides (optional)
3. Mounted `application-prod.yml` files

## Deployment Order

### Phase 2 (Current)
1. Start infrastructure services:
   ```bash
   docker-compose -f docker-compose.infrastructure.yml up -d
   ```

2. Verify infrastructure health:
   ```bash
   docker-compose -f docker-compose.infrastructure.yml ps
   ```

3. Start application services:
   ```bash
   docker-compose -f docker-compose.services.yml up -d
   ```

### Phase 3 (Future)
- Build service images
- Deploy remaining microservices
- Enable Web Admin frontend

## Service Dependencies Chain

```
postgres-primary ─┐
redis-master ─────┼─→ nacos ─→ auth-service ─→ gateway-service ─→ web-admin
nacos-mysql ──────┘
```

## Monitoring and Maintenance

### View Logs
```bash
# Real-time logs for a service
docker-compose -f docker-compose.services.yml logs -f gcrf-gateway-service

# Check log files
tail -f ./logs/gateway/application.log
```

### Service Management
```bash
# Restart a service
docker-compose -f docker-compose.services.yml restart gcrf-auth-service

# Scale a service (if configured)
docker-compose -f docker-compose.services.yml up -d --scale gcrf-auth-service=2
```

### Health Status
```bash
# Check service health
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health  # Internal only
```

## Security Considerations

1. **Network Isolation**:
   - Auth service not exposed externally
   - Infrastructure ports bound to 127.0.0.1

2. **Secret Management**:
   - Secrets loaded from environment files
   - JWT secrets never hardcoded
   - Database passwords encrypted

3. **Resource Limits**:
   - Memory and CPU limits prevent resource exhaustion
   - JVM tuned for container environment

## Troubleshooting

### Service Won't Start
1. Check dependencies are healthy:
   ```bash
   docker-compose -f docker-compose.infrastructure.yml ps
   ```

2. Check logs:
   ```bash
   docker-compose -f docker-compose.services.yml logs gcrf-auth-service
   ```

3. Verify environment variables:
   ```bash
   docker-compose -f docker-compose.services.yml config
   ```

### Connection Issues
1. Verify network connectivity:
   ```bash
   docker network inspect gcrf-infrastructure-network
   docker network inspect gcrf-frontend-network
   ```

2. Test internal DNS:
   ```bash
   docker exec gcrf-gateway-service ping nacos
   docker exec gcrf-gateway-service ping redis-master
   ```

### Performance Issues
1. Check resource usage:
   ```bash
   docker stats
   ```

2. Review JVM metrics:
   ```bash
   curl http://localhost:8080/actuator/metrics/jvm.memory.used
   ```

## Next Steps (Phase 3)
1. Build service Docker images
2. Create remaining microservice configurations
3. Deploy Web Admin frontend
4. Implement service mesh (optional)
5. Add monitoring stack (Prometheus/Grafana)