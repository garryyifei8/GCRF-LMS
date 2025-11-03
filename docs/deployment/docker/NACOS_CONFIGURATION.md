# Nacos Configuration Guide

## Table of Contents
1. [Service Discovery Architecture](#service-discovery-architecture)
2. [Service Registration](#service-registration)
3. [Configuration Management](#configuration-management)
4. [Namespace Strategy](#namespace-strategy)
5. [Dynamic Configuration Refresh](#dynamic-configuration-refresh)
6. [Monitoring & Health Checks](#monitoring--health-checks)
7. [Troubleshooting](#troubleshooting)
8. [Best Practices](#best-practices)

---

## Service Discovery Architecture

### Overview
The GCRF Library Management System uses Nacos as the central service registry and configuration management platform. This enables:
- Dynamic service discovery
- Centralized configuration management
- Health monitoring
- Load balancing

### Architecture Diagram
```
┌──────────────────────────────────────────────────────────┐
│                      Nacos Server                        │
│                    (localhost:8848)                      │
│  ┌─────────────────────────────────────────────────┐    │
│  │           Service Registry                       │    │
│  │  ┌────────────┐  ┌────────────┐  ┌───────────┐ │    │
│  │  │  Gateway   │  │   Auth     │  │   Book    │ │    │
│  │  │  Service   │  │  Service   │  │  Service  │ │    │
│  │  └────────────┘  └────────────┘  └───────────┘ │    │
│  │  ┌────────────┐  ┌────────────┐  ┌───────────┐ │    │
│  │  │  Reader    │  │Circulation │  │  System   │ │    │
│  │  │  Service   │  │  Service   │  │  Service  │ │    │
│  │  └────────────┘  └────────────┘  └───────────┘ │    │
│  └─────────────────────────────────────────────────┘    │
│                                                          │
│  ┌─────────────────────────────────────────────────┐    │
│  │         Configuration Management                 │    │
│  │  ┌─────────────────────────────────────────┐    │    │
│  │  │  Shared Configs:                        │    │    │
│  │  │  - application-shared.yml               │    │    │
│  │  │  - jwt-shared.yml                       │    │    │
│  │  │  - redis-shared.yml                     │    │    │
│  │  │  - database-shared.yml                  │    │    │
│  │  └─────────────────────────────────────────┘    │    │
│  └─────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────┘
                           ↑
         ┌─────────────────┼─────────────────┐
         │                 │                 │
    ┌─────────┐      ┌─────────┐      ┌─────────┐
    │Service A│      │Service B│      │Service C│
    │ (8081)  │      │ (8082)  │      │ (8083)  │
    └─────────┘      └─────────┘      └─────────┘
```

---

## Service Registration

### Automatic Registration
Services automatically register with Nacos on startup using Spring Cloud Alibaba:

```yaml
# application.yml
spring:
  application:
    name: service-name  # Service identifier in Nacos
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
        username: ${NACOS_USERNAME:nacos}
        password: ${NACOS_PASSWORD:nacos}
        namespace: ${NACOS_NAMESPACE:}  # Empty for public namespace
        group: ${NACOS_GROUP:DEFAULT_GROUP}
```

### Registration Process
1. **Service Startup**: Service starts and reads Nacos configuration
2. **Instance Registration**: Service registers itself with:
   - Service name
   - IP address and port
   - Metadata (version, region, etc.)
   - Health check endpoint
3. **Heartbeat**: Service sends periodic heartbeats (default: 5s)
4. **Health Monitoring**: Nacos monitors instance health
5. **Deregistration**: Service deregisters on shutdown

### Metadata Configuration
Services can include custom metadata for advanced routing:

```yaml
spring:
  cloud:
    nacos:
      discovery:
        metadata:
          version: 1.0.0
          region: us-east
          zone: zone-1
          protocol: http
```

---

## Configuration Management

### Shared Configuration Structure
Shared configurations reduce duplication and ensure consistency:

#### 1. application-shared.yml (All Services)
```yaml
# Common application settings
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
    default-property-inclusion: non_null

  # Common servlet settings
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

# Logging configuration
logging:
  level:
    com.gcrf.library: DEBUG
    org.springframework.cloud.gateway: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

#### 2. jwt-shared.yml (Authentication)
```yaml
jwt:
  secret: ${JWT_SECRET:gcrf-library-jwt-secret-2024-production}
  expiration: ${JWT_EXPIRATION:86400000}  # 24 hours in milliseconds
  refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000}  # 7 days
  header: Authorization
  prefix: "Bearer "
  issuer: gcrf-library-system
```

#### 3. redis-shared.yml (Caching)
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:gcrf_redis_2024}
      database: ${REDIS_DATABASE:0}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
```

#### 4. database-shared.yml (Database Connection Pool)
```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 30000
      pool-name: HikariPool
      max-lifetime: 1800000
      connection-timeout: 30000
      connection-test-query: SELECT 1
```

### Service-Specific Configuration
Each service can have its own configuration that extends or overrides shared configs:

```yaml
# gateway-service.yml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true

      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/v1/auth/**
```

### Configuration Priority
Configuration is loaded in the following order (later overrides earlier):
1. Built-in defaults
2. application.yml (in JAR)
3. application-{profile}.yml (in JAR)
4. Nacos shared configuration
5. Nacos service-specific configuration
6. Environment variables
7. Command-line arguments

---

## Namespace Strategy

### Environment Isolation
Use namespaces to isolate configurations across environments:

```
nacos/
├── namespaces/
│   ├── dev/              # Development environment
│   │   ├── configs/
│   │   └── services/
│   ├── staging/          # Staging environment
│   │   ├── configs/
│   │   └── services/
│   └── prod/             # Production environment
│       ├── configs/
│       └── services/
```

### Namespace Configuration
```yaml
# Development
NACOS_NAMESPACE=dev
NACOS_GROUP=LIBRARY_GROUP

# Staging
NACOS_NAMESPACE=staging
NACOS_GROUP=LIBRARY_GROUP

# Production
NACOS_NAMESPACE=prod
NACOS_GROUP=LIBRARY_GROUP
```

### Creating Namespaces
1. Access Nacos console: http://localhost:8848/nacos
2. Navigate to "Namespace" menu
3. Create namespace with:
   - Namespace ID: dev/staging/prod
   - Namespace Name: Development/Staging/Production
   - Description: Environment description

---

## Dynamic Configuration Refresh

### Using @RefreshScope
Enable dynamic configuration updates without service restart:

```java
@RestController
@RefreshScope  // Enable dynamic refresh
@RequestMapping("/api/v1/config")
public class ConfigController {

    @Value("${feature.enabled:false}")
    private boolean featureEnabled;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
            "featureEnabled", featureEnabled,
            "version", appVersion
        );
    }
}
```

### Configuration Listener
Listen for configuration changes programmatically:

```java
@Component
public class ConfigChangeListener {

    @NacosValue(value = "${app.config}", autoRefreshed = true)
    private String config;

    @NacosConfigListener(dataId = "application-shared.yml")
    public void onConfigChange(String newConfig) {
        log.info("Configuration updated: {}", newConfig);
        // Handle configuration change
    }
}
```

### Refresh Process
1. Update configuration in Nacos console
2. Nacos pushes changes to subscribed services
3. Spring Cloud refreshes @RefreshScope beans
4. New configuration takes effect immediately

---

## Monitoring & Health Checks

### Service Health Status
Monitor service health through Nacos console:

1. **Healthy**: Service is running and responding to health checks
2. **Unhealthy**: Service failed health checks but still registered
3. **Offline**: Service is not registered or has been deregistered

### Health Check Configuration
```yaml
spring:
  cloud:
    nacos:
      discovery:
        heart-beat-interval: 5000  # 5 seconds
        heart-beat-timeout: 15000  # 15 seconds
        ip-delete-timeout: 30000   # 30 seconds
```

### Custom Health Check
```java
@Component
public class CustomHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // Custom health check logic
        if (isHealthy()) {
            return Health.up()
                .withDetail("status", "Service is healthy")
                .build();
        }
        return Health.down()
            .withDetail("error", "Service unhealthy")
            .build();
    }
}
```

---

## Troubleshooting

### Common Issues and Solutions

#### 1. Service Not Registering
**Symptoms**: Service doesn't appear in Nacos console

**Possible Causes & Solutions**:
- **Nacos not accessible**: Check `server-addr` configuration
  ```bash
  curl http://localhost:8848/nacos/
  ```
- **Authentication failure**: Verify username/password
  ```yaml
  spring.cloud.nacos.discovery.username=nacos
  spring.cloud.nacos.discovery.password=nacos
  ```
- **Network issues**: Check firewall/security groups
- **Wrong namespace**: Verify namespace configuration

#### 2. Configuration Not Loading
**Symptoms**: Service uses default values instead of Nacos config

**Possible Causes & Solutions**:
- **Data ID mismatch**: Ensure correct naming
  ```
  Data ID: ${spring.application.name}-${profile}.yml
  ```
- **Group mismatch**: Check group configuration
- **Namespace issues**: Verify namespace in both service and Nacos
- **Format issues**: Validate YAML syntax

#### 3. Service Discovery Failure
**Symptoms**: Services can't discover each other

**Possible Causes & Solutions**:
- **Different namespaces**: Ensure services in same namespace
- **Group mismatch**: Services must use same group
- **Load balancer missing**: Add Spring Cloud LoadBalancer dependency
  ```xml
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-loadbalancer</artifactId>
  </dependency>
  ```

#### 4. Heartbeat Timeout
**Symptoms**: Service marked as unhealthy intermittently

**Solutions**:
- Increase heartbeat timeout:
  ```yaml
  spring.cloud.nacos.discovery.heart-beat-timeout=20000
  ```
- Check network latency
- Verify service resource availability

#### 5. Configuration Not Refreshing
**Symptoms**: @RefreshScope not working

**Solutions**:
- Add @RefreshScope annotation
- Enable configuration auto-refresh:
  ```yaml
  spring.cloud.nacos.config.refresh-enabled=true
  ```
- Check WebSocket connection for push updates

### Debug Logging
Enable detailed logging for troubleshooting:

```yaml
logging:
  level:
    com.alibaba.nacos: DEBUG
    com.alibaba.cloud: DEBUG
    org.springframework.cloud: DEBUG
```

---

## Best Practices

### 1. Configuration Management
- **Use shared configurations** for common settings
- **Version control** configuration changes
- **Document** configuration parameters
- **Validate** configurations before deployment
- **Use profiles** for environment-specific settings

### 2. Service Registration
- **Set appropriate timeouts** based on network conditions
- **Include metadata** for better service identification
- **Use consistent naming** conventions
- **Implement graceful shutdown** for clean deregistration

### 3. Security
- **Use authentication** in production
- **Encrypt sensitive data** in configurations
- **Use environment variables** for secrets
- **Implement RBAC** for configuration access
- **Audit configuration changes**

### 4. High Availability
- **Deploy Nacos cluster** for production
- **Use persistent storage** for configurations
- **Implement backup strategies**
- **Monitor Nacos health**
- **Plan for disaster recovery**

### 5. Performance
- **Cache configurations** locally
- **Use batch operations** for bulk updates
- **Optimize heartbeat intervals**
- **Monitor resource usage**
- **Implement circuit breakers**

### 6. Development Workflow
1. **Local Development**: Use embedded Nacos or Docker
2. **Integration Testing**: Use dedicated Nacos instance
3. **Staging**: Mirror production configuration
4. **Production**: Use Nacos cluster with monitoring

---

## Quick Reference

### Environment Variables
```bash
# Nacos Connection
NACOS_SERVER_ADDR=localhost:8848
NACOS_USERNAME=nacos
NACOS_PASSWORD=nacos
NACOS_NAMESPACE=prod
NACOS_GROUP=DEFAULT_GROUP

# Service Configuration
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
```

### Maven Dependencies
```xml
<!-- Spring Cloud Alibaba Nacos -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
```

### Useful Commands
```bash
# Test service discovery
./scripts/test-service-discovery.sh

# Push configurations
./scripts/push-nacos-configs.sh

# Check Nacos health
curl http://localhost:8848/nacos/v1/console/health/readiness

# List all services
curl http://localhost:8848/nacos/v1/ns/service/list

# Get service instances
curl "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=gateway-service"
```

---

## Additional Resources

- [Nacos Official Documentation](https://nacos.io/en-us/docs/what-is-nacos.html)
- [Spring Cloud Alibaba Documentation](https://spring-cloud-alibaba-group.github.io/github-pages/hoxton/en-us/index.html)
- [GCRF System Architecture](../INFRASTRUCTURE_README.md)
- [Service Deployment Guide](./SERVICE_DEPLOYMENT.md)