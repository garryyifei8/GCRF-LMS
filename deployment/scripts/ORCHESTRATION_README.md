# GCRF Stack Orchestration Scripts

## Overview

This directory contains production-ready orchestration scripts for managing the GCRF Library Management System stack. These scripts provide automated startup, shutdown, health monitoring, and maintenance operations with comprehensive error handling and logging.

## Quick Start

```bash
# Start the complete stack
./start-stack.sh

# Check system health
./health-check-all.sh

# Stop the stack gracefully
./stop-stack.sh

# Restart a specific service
./restart-service.sh gateway-service
```

## Scripts Overview

| Script | Purpose | Key Features |
|--------|---------|--------------|
| `start-stack.sh` | Orchestrated startup | Health monitoring, dependency management, startup report |
| `stop-stack.sh` | Graceful shutdown | Data persistence, configurable timeouts, volume management |
| `wait-for-healthy.sh` | Health check utility | Container health monitoring, configurable timeouts, colored output |
| `restart-service.sh` | Service restart | Rolling restart, zero-downtime options, health verification |
| `health-check-all.sh` | System health check | Comprehensive monitoring, alerts, resource tracking |

## Detailed Script Documentation

### 1. Stack Startup (`start-stack.sh`)

Orchestrates the complete stack startup with proper dependency management and health verification.

#### Usage

```bash
./start-stack.sh [options]

Options:
  --skip-infrastructure  Skip infrastructure startup (assume already running)
  --skip-health-check    Skip health check validation
  --timeout <seconds>    Override health check timeout (default: 300)
  --verbose              Enable verbose output
  --help                 Display help message
```

#### Examples

```bash
# Standard startup
./start-stack.sh

# Quick startup without health checks (development)
./start-stack.sh --skip-health-check

# Start only services (infrastructure already running)
./start-stack.sh --skip-infrastructure

# Verbose mode with custom timeout
./start-stack.sh --verbose --timeout 600
```

#### Startup Sequence

1. **Prerequisites Check**
   - Docker/Docker Compose installation
   - Docker daemon status
   - Compose file existence
   - Available disk space (10GB minimum)

2. **Infrastructure Startup**
   - PostgreSQL (primary database)
   - Redis (cache and session store)
   - Nacos (service discovery)
   - RabbitMQ (message queue)
   - MinIO (object storage)

3. **Health Monitoring**
   - Wait for infrastructure health checks
   - Verify service readiness
   - Check network connectivity

4. **Application Services**
   - Gateway Service (API gateway)
   - Auth Service (authentication)
   - Additional microservices

5. **Final Verification**
   - All services healthy
   - Endpoints accessible
   - Resource usage report

#### Exit Codes

- `0` - Success
- `1` - General error
- `2` - Infrastructure startup failed
- `3` - Service startup failed
- `4` - Health check failed
- `5` - Prerequisites check failed

### 2. Stack Shutdown (`stop-stack.sh`)

Gracefully shuts down the stack with data persistence and cleanup options.

#### Usage

```bash
./stop-stack.sh [options]

Options:
  --force                Force immediate shutdown (no grace period)
  --remove-volumes       Remove all data volumes (DESTRUCTIVE!)
  --skip-backup          Skip data backup before shutdown
  --services-only        Only stop application services
  --infrastructure-only  Only stop infrastructure
  --timeout <seconds>    Override grace period (default: 30/60)
  --verbose              Enable verbose output
  --help                 Display help message
```

#### Examples

```bash
# Graceful shutdown with data persistence
./stop-stack.sh

# Force immediate shutdown
./stop-stack.sh --force

# Stop and remove all data (CAUTION!)
./stop-stack.sh --remove-volumes

# Stop only services (keep infrastructure running)
./stop-stack.sh --services-only

# Quick shutdown without backup
./stop-stack.sh --skip-backup
```

#### Shutdown Sequence

1. **Data Backup** (optional)
   - Create volume backups
   - Export configurations
   - Save critical state

2. **Data Persistence**
   - Redis BGSAVE
   - PostgreSQL CHECKPOINT
   - RabbitMQ sync

3. **Service Shutdown**
   - 30-second grace period
   - Graceful connection draining
   - State preservation

4. **Infrastructure Shutdown**
   - 60-second grace period
   - Data flush completion
   - Clean resource release

5. **Verification**
   - All containers stopped
   - Resources cleaned up
   - Volumes preserved/removed

#### Exit Codes

- `0` - Success
- `1` - General error
- `2` - Backup failed
- `3` - Service shutdown failed
- `4` - Infrastructure shutdown failed

### 3. Health Check Utility (`wait-for-healthy.sh`)

Waits for Docker containers to become healthy with detailed status reporting.

#### Usage

```bash
./wait-for-healthy.sh <container1> [container2] [...]

Environment Variables:
  MAX_WAIT=300        Maximum wait time in seconds
  POLL_INTERVAL=5     Poll interval in seconds
  VERBOSE=false       Enable verbose output
  NO_COLOR=false      Disable colored output
```

#### Examples

```bash
# Wait for single container
./wait-for-healthy.sh gcrf-postgres-primary

# Wait for multiple containers
./wait-for-healthy.sh gcrf-nacos gcrf-redis-master gcrf-rabbitmq

# Custom timeout
MAX_WAIT=600 ./wait-for-healthy.sh gcrf-gateway-service

# Verbose mode
VERBOSE=true ./wait-for-healthy.sh gcrf-auth-service
```

#### Features

- **Smart Health Detection**: Automatically detects if container has health check
- **Detailed Status**: Shows current state, health status, and logs on failure
- **Colored Output**: Visual indicators (✓ healthy, ✗ failed, ⏳ waiting)
- **Timeout Handling**: Configurable timeout with detailed failure reporting
- **Spinner Animation**: Visual progress indicator during wait

#### Exit Codes

- `0` - All services healthy
- `1` - Service not found
- `2` - Service unhealthy
- `3` - Timeout reached
- `4` - Invalid arguments

### 4. Service Restart (`restart-service.sh`)

Restarts individual services with multiple strategies and health verification.

#### Usage

```bash
./restart-service.sh <service-name> [options]

Options:
  --rolling              Enable rolling restart (zero-downtime)
  --force                Force restart without health checks
  --timeout <seconds>    Health check timeout (default: 120)
  --scale <number>       Scale during rolling restart (default: 2)
  --verbose              Enable verbose output
  --help                 Display help message
```

#### Examples

```bash
# Standard restart
./restart-service.sh gateway-service

# Rolling restart (zero-downtime)
./restart-service.sh auth-service --rolling

# Force restart
./restart-service.sh redis --force

# Rolling with custom scale
./restart-service.sh gateway --rolling --scale 3
```

#### Supported Services

Short names and full container names are both supported:

- `postgres` or `gcrf-postgres-primary`
- `redis` or `gcrf-redis-master`
- `nacos` or `gcrf-nacos`
- `rabbitmq` or `gcrf-rabbitmq`
- `minio` or `gcrf-minio`
- `gateway` or `gcrf-gateway-service`
- `auth` or `gcrf-auth-service`

#### Restart Strategies

1. **Standard Restart**
   - Stop container (30s grace)
   - Start container
   - Verify health

2. **Rolling Restart** (--rolling)
   - Scale up to N instances
   - Wait for new instances
   - Remove old instance
   - Scale back to 1

3. **Force Restart** (--force)
   - Kill container immediately
   - Remove container
   - Recreate and start

#### Exit Codes

- `0` - Success
- `1` - Invalid arguments
- `2` - Service not found
- `3` - Restart failed
- `4` - Health check failed
- `5` - Rollback failed

### 5. System Health Check (`health-check-all.sh`)

Comprehensive health monitoring for the entire stack.

#### Usage

```bash
./health-check-all.sh [options]

Options:
  --json                 Output results in JSON format
  --alerts-only          Only show unhealthy services
  --detailed             Include detailed diagnostics
  --watch <seconds>      Continuous monitoring mode
  --threshold <percent>  Resource alert threshold (default: 80)
  --output <file>        Save report to file
  --slack-webhook <url>  Send alerts to Slack
  --email <address>      Send alerts via email
  --verbose              Enable verbose output
  --help                 Display help message
```

#### Examples

```bash
# Basic health check
./health-check-all.sh

# Continuous monitoring (refresh every 10s)
./health-check-all.sh --watch 10

# JSON output for automation
./health-check-all.sh --json > health.json

# Alerts only with Slack notification
./health-check-all.sh --alerts-only \
  --slack-webhook "https://hooks.slack.com/..."

# Detailed diagnostics with file output
./health-check-all.sh --detailed --output health-report.txt

# Resource monitoring with custom threshold
./health-check-all.sh --threshold 90
```

#### Health Check Components

1. **Container Status**
   - Running state
   - Health check status
   - Uptime
   - Resource usage

2. **Service Endpoints**
   - API Gateway (port 8080)
   - Auth Service (port 8081)
   - Nacos Console (port 8848)
   - RabbitMQ Management (port 15672)
   - MinIO Console (port 9001)

3. **Resource Usage**
   - System CPU/Memory/Disk
   - Container CPU/Memory
   - Network I/O
   - Threshold alerts

4. **Network Connectivity**
   - Docker network status
   - Inter-service communication
   - Service discovery verification

#### Output Formats

**Standard Output**:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Container Status
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ gcrf-postgres-primary
  State: running | Health: healthy
  Resources: CPU: 2.5% | Mem: 256MB (10%) | Uptime: 2h 15m
```

**JSON Output**:
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "summary": {
    "total_services": 8,
    "healthy": 7,
    "unhealthy": 1,
    "warning": 0
  },
  "services": [...],
  "status": "unhealthy"
}
```

#### Alert Integration

**Slack Webhook**:
```bash
export SLACK_WEBHOOK="https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX"
./health-check-all.sh --slack-webhook "$SLACK_WEBHOOK"
```

**Email Alerts** (requires mail configured):
```bash
./health-check-all.sh --email "ops@example.com"
```

#### Exit Codes

- `0` - All services healthy
- `1` - Some services unhealthy
- `2` - Critical services down
- `3` - Resource thresholds exceeded
- `4` - Network connectivity issues

## Common Use Cases

### Development Workflow

```bash
# Morning startup
./start-stack.sh

# Check status during development
./health-check-all.sh

# Restart service after code changes
./restart-service.sh auth-service

# End of day shutdown
./stop-stack.sh
```

### Production Deployment

```bash
# Initial deployment
./start-stack.sh --verbose

# Continuous monitoring
./health-check-all.sh --watch 30 --slack-webhook "$WEBHOOK"

# Zero-downtime service update
./restart-service.sh gateway-service --rolling

# Maintenance window
./stop-stack.sh --force --remove-volumes  # Full cleanup
```

### Troubleshooting

```bash
# Detailed health check
./health-check-all.sh --detailed --verbose

# Force restart problematic service
./restart-service.sh nacos --force

# Check specific containers
./wait-for-healthy.sh gcrf-postgres-primary gcrf-redis-master

# Emergency shutdown
./stop-stack.sh --force
```

## Best Practices

### 1. Startup Best Practices

- Always check prerequisites before production deployment
- Use `--verbose` for initial deployments
- Monitor startup logs in `deployment/logs/`
- Verify all endpoints after startup

### 2. Shutdown Best Practices

- Always use graceful shutdown in production
- Create backups before using `--remove-volumes`
- Allow sufficient timeout for data persistence
- Verify no active connections before shutdown

### 3. Health Monitoring

- Set up continuous monitoring in production
- Configure alerts for critical services
- Monitor resource usage trends
- Keep health check logs for analysis

### 4. Service Restart

- Use rolling restart for zero-downtime updates
- Test restart procedures in staging first
- Monitor health after restart
- Keep restart logs for troubleshooting

## Logging

All scripts generate detailed logs in `deployment/logs/`:

```
deployment/logs/
├── startup_20240115_103000.log      # Startup logs
├── shutdown_20240115_180000.log     # Shutdown logs
├── health_20240115_120000.json      # Health check reports
└── restart_gateway_20240115.log     # Service restart logs
```

## Environment Variables

### Global Settings

```bash
# Disable colored output
export NO_COLOR=true

# Enable verbose mode globally
export VERBOSE=true

# Set default timeouts
export MAX_WAIT=600
export POLL_INTERVAL=10
```

### Service-Specific

```bash
# Redis password (for data persistence)
export REDIS_PASSWORD=admin123

# Database credentials
export POSTGRES_PASSWORD=admin123

# Health check endpoints
export GATEWAY_HEALTH_URL=http://localhost:8080/actuator/health
```

## Troubleshooting

### Common Issues

#### 1. Container Won't Start

```bash
# Check logs
docker logs gcrf-gateway-service

# Force restart
./restart-service.sh gateway --force

# Verify configuration
docker-compose -f docker-compose.services.yml config
```

#### 2. Health Check Timeout

```bash
# Increase timeout
MAX_WAIT=600 ./wait-for-healthy.sh gcrf-nacos

# Skip health check (development only)
./start-stack.sh --skip-health-check
```

#### 3. Resource Issues

```bash
# Check resource usage
./health-check-all.sh --detailed

# Clean up unused resources
docker system prune -a
```

#### 4. Network Problems

```bash
# Verify network
docker network ls
docker network inspect gcrf_network

# Recreate network
docker-compose -f docker-compose.infrastructure.yml down
docker-compose -f docker-compose.infrastructure.yml up -d
```

## Integration with CI/CD

### GitHub Actions Example

```yaml
- name: Start GCRF Stack
  run: |
    cd deployment/scripts
    ./start-stack.sh --timeout 600

- name: Health Check
  run: |
    cd deployment/scripts
    ./health-check-all.sh --json > health.json

- name: Shutdown
  if: always()
  run: |
    cd deployment/scripts
    ./stop-stack.sh --skip-backup
```

### Jenkins Pipeline

```groovy
stage('Deploy') {
    steps {
        sh '''
            cd deployment/scripts
            ./start-stack.sh --verbose
            ./health-check-all.sh --alerts-only
        '''
    }
}
```

## Support and Maintenance

### Regular Maintenance

1. **Daily**: Check health status
2. **Weekly**: Review resource usage trends
3. **Monthly**: Update container images
4. **Quarterly**: Full stack restart and cleanup

### Monitoring Checklist

- [ ] All containers running
- [ ] Health checks passing
- [ ] Resource usage < 80%
- [ ] Network connectivity verified
- [ ] Endpoints accessible
- [ ] Logs reviewed for errors

## Related Documentation

- [Docker Compose Files](../README.md)
- [Volume Management](VOLUME_MANAGEMENT.md)
- [Service Discovery](SERVICE_DISCOVERY_README.md)
- [Backup and Restore](../README.md#backup-and-restore)

---

**Last Updated**: 2024-01-15
**Version**: 1.0.0
**Phase**: Production Ready