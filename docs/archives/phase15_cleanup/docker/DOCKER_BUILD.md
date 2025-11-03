# Auth Service Docker Build Guide

## Overview
Production-ready Docker configuration for GCRF Library Management System Auth Service.

## Build Information
- **Service**: auth-service
- **Port**: 8081
- **Base Image**: eclipse-temurin:21-jre-alpine
- **Java Version**: 21
- **Expected Image Size**: < 200MB

## Key Features

### 1. Multi-Stage Build
- **Stage 1 (deps)**: Dependency caching for faster rebuilds
- **Stage 2 (builder)**: Application compilation
- **Stage 3 (runtime)**: Minimal production image

### 2. Database Optimizations
- HikariCP connection pool tuning
- PostgreSQL SSL mode enabled
- Redis connection pooling with Lettuce
- JVM options for network optimization

### 3. Security
- Non-root user (spring:spring)
- Minimal Alpine Linux base
- No unnecessary packages
- Secure JVM options

### 4. Health Monitoring
- Health check endpoint: `/actuator/health`
- 30-second intervals with 3 retries
- 60-second startup grace period

### 5. JVM Configuration
```bash
# Garbage Collection
-XX:+UseG1GC
-XX:MaxRAMPercentage=75.0

# Error Handling
-XX:+ExitOnOutOfMemoryError
-XX:+HeapDumpOnOutOfMemoryError

# Database Optimization
-Dcom.zaxxer.hikari.aliveBypassWindowMs=500
-Djava.net.preferIPv4Stack=true
```

## Build Commands

### Local Build
```bash
cd backend
docker build -f auth-service/Dockerfile -t gcrf-auth-service:latest .
```

### Production Build (Multi-platform)
```bash
cd backend
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -f auth-service/Dockerfile \
  -t gcrf-auth-service:1.0.0 \
  --push .
```

## Environment Variables
Required environment variables (set in docker-compose or K8s):

```yaml
# Database
DB_HOST: postgres-host
DB_PORT: 5432
DB_NAME: auth_service
DB_USERNAME: postgres
DB_PASSWORD: secure_password

# Redis
REDIS_HOST: redis-host
REDIS_PORT: 6379
REDIS_PASSWORD: redis_password

# Nacos
NACOS_SERVER_ADDR: nacos:8848
NACOS_USERNAME: nacos
NACOS_PASSWORD: nacos

# JWT
JWT_SECRET: production_secret
JWT_EXPIRATION: 86400000

# Spring Profile
SPRING_PROFILES_ACTIVE: prod
```

## Docker Compose Integration
Works with Phase 2's `docker-compose.services.yml`:

```yaml
auth-service:
  build:
    context: ./backend
    dockerfile: auth-service/Dockerfile
  ports:
    - "8081:8081"
  environment:
    - DB_HOST=postgres
    - REDIS_HOST=redis
    - NACOS_SERVER_ADDR=nacos:8848
  depends_on:
    postgres:
      condition: service_healthy
    redis:
      condition: service_healthy
    nacos:
      condition: service_healthy
```

## Verification

### 1. Build Test
```bash
cd backend
docker build -f auth-service/Dockerfile --target deps -t test:deps .
docker build -f auth-service/Dockerfile --target builder -t test:builder .
docker build -f auth-service/Dockerfile -t test:runtime .
```

### 2. Run Test
```bash
docker run --rm -p 8081:8081 \
  -e DB_HOST=localhost \
  -e REDIS_HOST=localhost \
  gcrf-auth-service:latest
```

### 3. Health Check
```bash
curl http://localhost:8081/actuator/health
```

## Troubleshooting

### Common Issues

1. **Build fails at dependency stage**
   - Ensure all POM files exist
   - Check Maven repository availability

2. **Runtime connection failures**
   - Verify environment variables are set
   - Check network connectivity to databases
   - Ensure dependent services are running

3. **Out of Memory errors**
   - Adjust `-XX:MaxRAMPercentage`
   - Increase container memory limits

## Performance Metrics
- Build time: < 5 minutes (with cache)
- Startup time: < 30 seconds
- Memory usage: ~256-512MB
- Connection pool: 20 max connections

## Maintenance
- Update base images monthly for security patches
- Review JVM options quarterly
- Monitor connection pool metrics in production

---
Last Updated: 2025-11-01