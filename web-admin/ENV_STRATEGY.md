# Environment Variable Injection Strategy for GCRF Library Management System

## 1. Environment Variable Naming Conventions

### Hierarchical Structure
```
{SERVICE}_{RESOURCE}_{PROPERTY}
```

### Naming Rules
- **UPPER_SNAKE_CASE** for all variables
- **Service Prefix**: GATEWAY_, AUTH_, BOOK_, etc.
- **Resource Groups**: DB_, REDIS_, NACOS_, JWT_, etc.
- **Common Variables**: No service prefix (e.g., `SPRING_PROFILES_ACTIVE`)

### Examples
```bash
# Service-specific
GATEWAY_SERVER_PORT=8080
AUTH_DB_HOST=postgres-primary.gcrf.local

# Shared resources
NACOS_SERVER_ADDR=nacos.gcrf.local:8848
REDIS_SENTINEL_NODES=sentinel1:26379,sentinel2:26379

# Common/Global
SPRING_PROFILES_ACTIVE=prod
TZ=Asia/Shanghai
```

## 2. Variable Categories

### Required vs Optional

**Required (fail-fast on missing):**
- Database credentials
- Service discovery (Nacos)
- JWT secrets
- Service ports

**Optional (with defaults):**
- Pool sizes
- Timeouts
- Log levels
- Cache TTLs

### Sensitive vs Non-Sensitive

**Sensitive (use Docker secrets/K8s Secrets):**
- Passwords
- JWT secrets
- API keys
- Database credentials

**Non-Sensitive (environment variables):**
- Hostnames
- Ports
- Pool configurations
- Feature flags

## 3. Environment File Structure

### File Hierarchy
```
deployment/
├── .env.example      # Template with descriptions
├── .env.dev         # Development values
├── .env.staging     # Staging values
├── .env.prod        # Production values (gitignored)
├── .env.local       # Local overrides (gitignored)
└── validate-env.sh  # Validation script
```

### Loading Priority (highest to lowest)
1. `.env.local` (developer overrides)
2. `.env.{ENVIRONMENT}` (environment-specific)
3. `.env` (base configuration)
4. Application defaults

## 4. Spring Boot Integration Pattern

### application.yml Structure
```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

---
spring:
  config:
    activate:
      on-profile: prod
    import:
      - optional:file:./config/application-prod.yml
      - optional:configserver:${CONFIG_SERVER_URL:}
```

### Variable Binding Pattern
```yaml
# With default values
server:
  port: ${SERVER_PORT:8080}

# Required (no default)
database:
  url: ${DATABASE_URL}

# Complex with fallback chain
redis:
  host: ${REDIS_HOST:${REDIS_SENTINEL_MASTER:localhost}}
```

## 5. Docker Compose Integration

### docker-compose.yml
```yaml
services:
  gateway-service:
    env_file:
      - .env
      - .env.${ENVIRONMENT:-dev}
    environment:
      - GATEWAY_SERVER_PORT=${GATEWAY_SERVER_PORT:-8080}
    secrets:
      - jwt_secret
      - db_password

secrets:
  jwt_secret:
    file: ./secrets/jwt_secret.txt
  db_password:
    file: ./secrets/db_password.txt
```

## 6. Multi-Environment Support

### Environment Switching Script
```bash
#!/bin/bash
# switch-env.sh
ENVIRONMENT=${1:-dev}
ln -sf .env.${ENVIRONMENT} .env.active
export $(cat .env.active | grep -v '^#' | xargs)
echo "Switched to ${ENVIRONMENT} environment"
```

## 7. Validation Strategy

### Startup Validation
- Check required variables exist
- Validate format/patterns
- Test connectivity to external services
- Fail fast with clear error messages

### Health Check Integration
- Expose configuration status endpoint
- Monitor configuration drift
- Alert on missing/invalid configurations

## 8. Security Best Practices

### Secret Management
1. **Never commit secrets** to version control
2. **Use Docker secrets** for sensitive data in Docker Compose
3. **Use K8s Secrets** for Kubernetes deployments
4. **Rotate secrets** regularly with zero-downtime
5. **Encrypt at rest** using tools like HashiCorp Vault

### Access Control
- Limit environment variable access by service account
- Use read-only mounts for configuration files
- Audit configuration access and changes

## 9. Migration to Kubernetes

### ConfigMap Strategy
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: gateway-config
data:
  SERVER_PORT: "8080"
  NACOS_SERVER_ADDR: "nacos-service:8848"
```

### Secret Strategy
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: auth-secrets
type: Opaque
data:
  JWT_SECRET: <base64-encoded>
  DB_PASSWORD: <base64-encoded>
```

## 10. Documentation Requirements

Each environment variable must be documented with:
- **Name**: Full variable name
- **Description**: What it controls
- **Required**: Yes/No
- **Default**: Default value if optional
- **Example**: Valid example value
- **Format**: Expected format/pattern
- **Service**: Which service(s) use it