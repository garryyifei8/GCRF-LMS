# Service Discovery Scripts

This directory contains scripts for managing Nacos service discovery and configuration management.

## Available Scripts

### 1. test-service-discovery.sh
Tests Nacos service discovery and configuration management functionality.

**Usage:**
```bash
./test-service-discovery.sh [service-name]
```

**Features:**
- Tests Nacos accessibility and authentication
- Lists all registered services with health status
- Checks configuration management
- Tests service discovery for specific services
- Generates detailed test reports

**Examples:**
```bash
# Test with default service (gateway-service)
./test-service-discovery.sh

# Test specific service
./test-service-discovery.sh auth-service

# Show help
./test-service-discovery.sh --help
```

**Output:**
- Console output with colored status indicators
- Detailed report file: `deployment/service-discovery-report-YYYYMMDD-HHMMSS.txt`

---

### 2. push-nacos-configs.sh
Manages shared configurations in Nacos for all environments.

**Usage:**
```bash
./push-nacos-configs.sh [action] [environment]
```

**Actions:**
- `create` - Create configuration files locally
- `push` - Push configurations to Nacos (default)
- `verify` - Verify configurations in Nacos
- `backup` - Backup existing configurations

**Environments:**
- `dev` - Development environment
- `staging` - Staging environment
- `prod` - Production environment
- `public` - Public namespace (default)

**Examples:**
```bash
# Create configuration files locally
./push-nacos-configs.sh create

# Push to development environment
./push-nacos-configs.sh push dev

# Push to production environment
./push-nacos-configs.sh push prod

# Verify existing configurations
./push-nacos-configs.sh verify

# Backup configurations
./push-nacos-configs.sh backup
```

**Configuration Files Created:**
- `application-shared.yml` - Common application settings
- `jwt-shared.yml` - JWT authentication configuration
- `redis-shared.yml` - Redis cache configuration
- `database-shared.yml` - Database connection pool settings
- `rabbitmq-shared.yml` - RabbitMQ messaging configuration
- `security-shared.yml` - Security and CORS settings

---

## Environment Variables

Both scripts use the following environment variables (can be set in `.env.infrastructure`):

```bash
# Nacos Connection
NACOS_SERVER_ADDR=localhost:8848
NACOS_USERNAME=nacos
NACOS_PASSWORD=nacos
NACOS_NAMESPACE=prod
NACOS_GROUP=DEFAULT_GROUP

# Additional settings for push-nacos-configs.sh
DRY_RUN=false  # Set to 'true' for dry run mode
```

---

## Workflow Examples

### Initial Setup
```bash
# 1. Start infrastructure
./start-infrastructure.sh

# 2. Create configuration files
./push-nacos-configs.sh create

# 3. Review and modify configurations if needed
vi ../nacos/configs/jwt-shared.yml

# 4. Push configurations to development
./push-nacos-configs.sh push dev

# 5. Test service discovery
./test-service-discovery.sh
```

### Production Deployment
```bash
# 1. Backup existing configurations
./push-nacos-configs.sh backup

# 2. Push configurations to production
./push-nacos-configs.sh push prod

# 3. Verify configurations
./push-nacos-configs.sh verify

# 4. Test service discovery
./test-service-discovery.sh gateway-service
```

### Troubleshooting Service Registration
```bash
# 1. Test Nacos connectivity
./test-service-discovery.sh

# 2. Check specific service
./test-service-discovery.sh auth-service

# 3. Review generated report
cat ../service-discovery-report-*.txt

# 4. Check Nacos dashboard
open http://localhost:8848/nacos/
```

---

## Integration with Services

### Service Configuration
Each service should include Nacos discovery configuration:

```yaml
# application.yml
spring:
  application:
    name: service-name
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
        username: ${NACOS_USERNAME:nacos}
        password: ${NACOS_PASSWORD:nacos}
        namespace: ${NACOS_NAMESPACE:}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
```

### Using Shared Configurations
Services can import shared configurations:

```yaml
# bootstrap.yml
spring:
  cloud:
    nacos:
      config:
        server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
        username: ${NACOS_USERNAME:nacos}
        password: ${NACOS_PASSWORD:nacos}
        namespace: ${NACOS_NAMESPACE:}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
        shared-configs:
          - data-id: application-shared.yml
            group: DEFAULT_GROUP
            refresh: true
          - data-id: jwt-shared.yml
            group: DEFAULT_GROUP
            refresh: true
```

---

## Monitoring

### Nacos Dashboard
Access the Nacos dashboard to monitor services and configurations:
- URL: http://localhost:8848/nacos/
- Username: nacos
- Password: nacos

### Service List
View all registered services:
1. Navigate to "Service Management" → "Service List"
2. Select namespace (dev/staging/prod)
3. View service instances and health status

### Configuration Management
Manage configurations:
1. Navigate to "Configuration Management" → "Configurations"
2. Select namespace
3. View/Edit/Clone configurations

---

## Troubleshooting

### Service Not Registering
1. Check Nacos is running: `docker ps | grep nacos`
2. Test connectivity: `./test-service-discovery.sh`
3. Verify service configuration (application.yml)
4. Check service logs for errors

### Configuration Not Loading
1. Verify configuration exists: `./push-nacos-configs.sh verify`
2. Check namespace and group settings
3. Ensure bootstrap.yml is configured correctly
4. Check service logs for configuration errors

### Authentication Issues
1. Verify credentials in `.env.infrastructure`
2. Check Nacos authentication is enabled
3. Try without authentication (for local development)

---

## Related Documentation
- [Nacos Configuration Guide](../docs/NACOS_CONFIGURATION.md)
- [Infrastructure README](../INFRASTRUCTURE_README.md)
- [Service Deployment Guide](../docs/SERVICE_DEPLOYMENT.md)