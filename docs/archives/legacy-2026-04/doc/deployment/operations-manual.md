# GCRF Library Management System - Operations Manual

**Project**: 国创睿峰智能图书馆管理系统 (GCRF Intelligent Library Management System)
**Version**: 1.0.0
**Last Updated**: 2025-12-01
**Document Status**: Production Ready

---

## Table of Contents

1. [Daily Operations](#daily-operations)
2. [Service Management](#service-management)
3. [Log Management](#log-management)
4. [Backup and Recovery](#backup-and-recovery)
5. [Monitoring and Alerting](#monitoring-and-alerting)
6. [Performance Tuning](#performance-tuning)
7. [Security Operations](#security-operations)
8. [Scaling Operations](#scaling-operations)
9. [Maintenance Windows](#maintenance-windows)
10. [Emergency Procedures](#emergency-procedures)

---

## Daily Operations

### Morning Health Check Routine

**Time**: Every morning at 9:00 AM

```bash
#!/bin/bash
# /usr/local/bin/daily-health-check.sh

echo "======================================"
echo "Daily Health Check - $(date)"
echo "======================================"

# 1. Check system resources
echo -e "\n1. System Resources:"
echo "CPU Usage:"
top -bn1 | grep "Cpu(s)" | awk '{print "  " $2}'
echo "Memory Usage:"
free -h | grep Mem | awk '{print "  Used: " $3 " / Total: " $2}'
echo "Disk Usage:"
df -h | grep -E "^/dev/" | awk '{print "  " $6 ": " $5 " used"}'

# 2. Check Docker services
echo -e "\n2. Docker Services:"
docker ps --format "table {{.Names}}\t{{.Status}}" | grep -E "(Up|Exited|Restarting)"

# 3. Check infrastructure services
echo -e "\n3. Infrastructure Services:"
docker exec postgres-primary pg_isready -U postgres && echo "  ✓ PostgreSQL: OK" || echo "  ✗ PostgreSQL: FAILED"
docker exec redis-master redis-cli -a ${REDIS_PASSWORD} ping > /dev/null 2>&1 && echo "  ✓ Redis: OK" || echo "  ✗ Redis: FAILED"
curl -sf http://localhost:8848/nacos/v1/console/health/readiness > /dev/null && echo "  ✓ Nacos: OK" || echo "  ✗ Nacos: FAILED"
curl -sf http://localhost:15672/api/healthchecks/node -u admin:${RABBITMQ_PASSWORD} > /dev/null && echo "  ✓ RabbitMQ: OK" || echo "  ✗ RabbitMQ: FAILED"

# 4. Check application services
echo -e "\n4. Application Services:"
for port in 8080 8081 8082 8083 8084 8085 8086; do
    service_name=$(docker ps --format "{{.Names}}" | grep -E "service" | grep "${port}" || echo "unknown:${port}")
    if curl -sf http://localhost:${port}/actuator/health > /dev/null 2>&1; then
        echo "  ✓ ${service_name}: OK"
    else
        echo "  ✗ ${service_name}: FAILED"
    fi
done

# 5. Check disk space
echo -e "\n5. Disk Space Warnings:"
df -h | awk 'NR>1 {if(int($5) > 80) print "  WARNING: " $6 " is " $5 " full"}'

# 6. Check last night's backup
echo -e "\n6. Last Backup Status:"
if [ -f /backups/postgresql/latest.sql.gz ]; then
    backup_age=$(( ($(date +%s) - $(stat -f %m /backups/postgresql/latest.sql.gz)) / 3600 ))
    if [ $backup_age -lt 24 ]; then
        echo "  ✓ Backup completed within last 24 hours"
    else
        echo "  ✗ WARNING: Backup is ${backup_age} hours old"
    fi
else
    echo "  ✗ ERROR: No backup file found"
fi

# 7. Check error logs
echo -e "\n7. Recent Errors (last 24 hours):"
find /logs -name "*.log" -mtime -1 -exec grep -i "ERROR" {} + | wc -l | awk '{print "  Total errors: " $1}'

echo -e "\n======================================"
echo "Health Check Complete"
echo "======================================"
```

**Schedule with cron**:
```bash
# Add to crontab
0 9 * * * /usr/local/bin/daily-health-check.sh | tee -a /logs/health-checks/daily-$(date +\%Y\%m\%d).log
```

### End-of-Day Routine

**Time**: Every evening at 6:00 PM

```bash
#!/bin/bash
# /usr/local/bin/end-of-day-check.sh

echo "End of Day Check - $(date)"

# 1. Review today's alerts
echo "1. Today's Alerts:"
grep -h "ALERT" /logs/alertmanager/$(date +%Y-%m-%d)*.log 2>/dev/null | wc -l | awk '{print "  Total alerts: " $1}'

# 2. Review daily statistics
echo "2. Daily Statistics:"
echo "  Total API requests: $(grep -h "200\|201" /logs/nginx/gcrf-library-access.log | wc -l)"
echo "  Failed requests: $(grep -h "400\|401\|403\|404\|500\|502\|503" /logs/nginx/gcrf-library-access.log | wc -l)"

# 3. Check system updates available
echo "3. System Updates:"
if command -v apt > /dev/null; then
    apt list --upgradable 2>/dev/null | tail -n +2 | wc -l | awk '{print "  Updates available: " $1}'
elif command -v dnf > /dev/null; then
    dnf check-update --quiet | tail -n +2 | wc -l | awk '{print "  Updates available: " $1}'
fi

# 4. Generate summary report
echo "4. Generating daily report..."
# Send summary email to operations team
```

---

## Service Management

### Starting Services

**Start all services**:
```bash
cd /opt/gcrf-library/deployment

# Start infrastructure services first
docker-compose -f docker-compose.infrastructure.yml up -d

# Wait 30 seconds for infrastructure to be ready
sleep 30

# Start application services
docker-compose -f docker-compose.services.yml up -d

# Start monitoring services
docker-compose -f docker-compose.monitoring.yml up -d
```

**Start specific service**:
```bash
# Restart single service
docker-compose -f docker-compose.services.yml restart gcrf-gateway-service

# Start with logs
docker-compose -f docker-compose.services.yml up -d gcrf-auth-service
docker-compose -f docker-compose.services.yml logs -f gcrf-auth-service
```

### Stopping Services

**Graceful shutdown (recommended)**:
```bash
# Stop application services first
docker-compose -f docker-compose.services.yml down

# Stop monitoring services
docker-compose -f docker-compose.monitoring.yml down

# Stop infrastructure services last
docker-compose -f docker-compose.infrastructure.yml down
```

**Emergency stop**:
```bash
# Force stop all containers
docker stop $(docker ps -q)
```

### Restarting Services

**Restart application service**:
```bash
# Standard restart
docker-compose -f docker-compose.services.yml restart gcrf-book-service

# Restart with recreation (picks up config changes)
docker-compose -f docker-compose.services.yml up -d --force-recreate gcrf-book-service
```

**Rolling restart (zero downtime)**:
```bash
#!/bin/bash
# Restart service instances one by one

SERVICES=("gcrf-gateway-service" "gcrf-auth-service" "gcrf-book-service")

for service in "${SERVICES[@]}"; do
    echo "Restarting $service..."

    # Get number of replicas
    replicas=$(docker ps --filter "name=$service" --format "{{.Names}}" | wc -l)

    if [ $replicas -gt 1 ]; then
        # Rolling restart for multiple replicas
        for i in $(seq 1 $replicas); do
            container="${service}_${i}"
            echo "  Restarting instance $i..."
            docker restart $container
            sleep 30  # Wait for instance to be healthy
        done
    else
        # Single instance restart
        docker restart $service
        sleep 30
    fi

    echo "  $service restarted successfully"
done
```

### Scaling Services

**Scale up application service**:
```bash
# Scale gateway service to 3 instances
docker-compose -f docker-compose.services.yml up -d --scale gcrf-gateway-service=3

# Scale auth service to 2 instances
docker-compose -f docker-compose.services.yml up -d --scale gcrf-auth-service=2
```

**Scale down**:
```bash
# Scale back to 1 instance
docker-compose -f docker-compose.services.yml up -d --scale gcrf-gateway-service=1
```

### Viewing Service Status

**Check all services**:
```bash
# View running containers
docker ps

# View all containers (including stopped)
docker ps -a

# View service status with resource usage
docker stats

# View specific service
docker inspect gcrf-gateway-service
```

**Check service logs**:
```bash
# View live logs
docker logs -f gcrf-gateway-service

# View last 100 lines
docker logs --tail 100 gcrf-gateway-service

# View logs with timestamps
docker logs -t gcrf-gateway-service

# View logs from last hour
docker logs --since 1h gcrf-gateway-service
```

### Service Registration Check

**Check Nacos service registry**:
```bash
# List all registered services
curl http://localhost:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=20

# Check specific service instances
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=gateway-service

# Check service health
curl http://localhost:8848/nacos/v1/ns/instance?serviceName=auth-service&ip=172.18.0.5&port=8081
```

---

## Log Management

### Log Locations

| Component | Log Location |
|-----------|--------------|
| Application Services | `/logs/services/<service-name>/` |
| Nginx Access | `/var/log/nginx/gcrf-library-access.log` |
| Nginx Error | `/var/log/nginx/gcrf-library-error.log` |
| PostgreSQL | `/logs/postgresql/` |
| Redis | `/logs/redis/redis.log` |
| Docker | `/var/lib/docker/containers/` |
| System | `/var/log/syslog` (Ubuntu) or `/var/log/messages` (CentOS) |

### Viewing Logs

**Application logs**:
```bash
# View specific service logs
tail -f /logs/services/gateway/application.log

# View all services logs
tail -f /logs/services/*/application.log

# Search for errors
grep -r "ERROR" /logs/services/ | tail -50

# Search with date filter
grep "2025-12-01" /logs/services/auth/application.log | grep "ERROR"
```

**Nginx logs**:
```bash
# View access logs (real-time)
tail -f /var/log/nginx/gcrf-library-access.log

# Count requests by status code
awk '{print $9}' /var/log/nginx/gcrf-library-access.log | sort | uniq -c | sort -rn

# Find slow requests (>1s)
awk '$NF > 1.0' /var/log/nginx/gcrf-library-access.log

# Top 10 IPs by request count
awk '{print $1}' /var/log/nginx/gcrf-library-access.log | sort | uniq -c | sort -rn | head -10
```

**Docker logs**:
```bash
# View logs from all containers
docker-compose -f docker-compose.services.yml logs

# Follow logs from specific service
docker-compose -f docker-compose.services.yml logs -f gcrf-gateway-service

# View logs with timestamps
docker-compose -f docker-compose.services.yml logs -t gcrf-auth-service

# Search across all container logs
docker ps -q | xargs -I {} docker logs {} 2>&1 | grep "ERROR"
```

### Log Rotation

**Configure logrotate for Nginx**:
```bash
sudo tee /etc/logrotate.d/gcrf-library <<EOF
/var/log/nginx/gcrf-library-*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 0640 www-data adm
    sharedscripts
    postrotate
        if [ -f /var/run/nginx.pid ]; then
            kill -USR1 \$(cat /var/run/nginx.pid)
        fi
    endscript
}
EOF
```

**Application log rotation**:
```bash
sudo tee /etc/logrotate.d/gcrf-application <<EOF
/logs/services/*/application.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 0640 root root
    size 100M
}
EOF
```

**Test log rotation**:
```bash
sudo logrotate -d /etc/logrotate.d/gcrf-library
sudo logrotate -f /etc/logrotate.d/gcrf-library
```

### Log Analysis Scripts

**Error summary script**:
```bash
#!/bin/bash
# /usr/local/bin/analyze-errors.sh

echo "Error Analysis - Last 24 hours"
echo "================================"

# Application errors
echo -e "\n1. Application Errors by Service:"
for service in gateway auth book reader circulation system notification; do
    count=$(grep -r "ERROR" /logs/services/${service}/ --include="*.log" 2>/dev/null | wc -l)
    echo "  ${service}: ${count} errors"
done

# Most common errors
echo -e "\n2. Top 10 Most Common Errors:"
grep -rh "ERROR" /logs/services/ --include="*.log" 2>/dev/null | \
    awk -F'ERROR' '{print $2}' | \
    sort | uniq -c | sort -rn | head -10

# HTTP errors
echo -e "\n3. HTTP Errors:"
echo "  4xx errors: $(grep -c " 4[0-9][0-9] " /var/log/nginx/gcrf-library-access.log)"
echo "  5xx errors: $(grep -c " 5[0-9][0-9] " /var/log/nginx/gcrf-library-access.log)"

# Database errors
echo -e "\n4. Database Errors:"
docker exec postgres-primary psql -U postgres -c \
    "SELECT count(*) FROM pg_stat_database_conflicts WHERE datname='gcrf_book';"
```

---

## Backup and Recovery

### Database Backup

**Manual backup**:
```bash
#!/bin/bash
# /usr/local/bin/backup-database.sh

BACKUP_DIR="/backups/postgresql"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/backup_$DATE.sql"

echo "Starting database backup..."

# Full backup of all databases
docker exec postgres-primary pg_dumpall -U postgres > "$BACKUP_FILE"

# Compress backup
gzip "$BACKUP_FILE"

# Upload to remote storage (optional)
# aws s3 cp "$BACKUP_FILE.gz" s3://gcrf-backups/postgresql/

# Cleanup old backups (keep 30 days)
find $BACKUP_DIR -name "backup_*.sql.gz" -mtime +30 -delete

# Create 'latest' symlink
ln -sf "$BACKUP_FILE.gz" "$BACKUP_DIR/latest.sql.gz"

echo "Backup completed: $BACKUP_FILE.gz"
```

**Scheduled backup** (cron):
```bash
# Daily full backup at 2 AM
0 2 * * * /usr/local/bin/backup-database.sh >> /logs/backups/database-backup.log 2>&1

# Hourly incremental backup (WAL archiving)
0 * * * * /usr/local/bin/backup-wal-archives.sh
```

### Database Recovery

**Restore from backup**:
```bash
#!/bin/bash
# /usr/local/bin/restore-database.sh

BACKUP_FILE=$1

if [ -z "$BACKUP_FILE" ]; then
    echo "Usage: $0 <backup-file.sql.gz>"
    exit 1
fi

echo "WARNING: This will restore the database from backup."
echo "All current data will be lost!"
read -p "Continue? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Restore cancelled"
    exit 0
fi

# Stop application services
echo "Stopping application services..."
cd /opt/gcrf-library/deployment
docker-compose -f docker-compose.services.yml down

# Restore database
echo "Restoring database..."
gunzip -c "$BACKUP_FILE" | docker exec -i postgres-primary psql -U postgres

# Restart services
echo "Restarting services..."
docker-compose -f docker-compose.services.yml up -d

echo "Restore completed"
```

**Point-in-Time Recovery (PITR)**:
```bash
#!/bin/bash
# /usr/local/bin/pitr-restore.sh

TARGET_TIME=$1  # Format: "2025-12-01 14:30:00"

# Stop PostgreSQL
docker-compose -f docker-compose.infrastructure.yml stop postgres-primary

# Restore base backup
tar -xzf /backups/postgresql/base_backup_latest.tar.gz -C /data/postgresql/

# Create recovery.conf
cat > /data/postgresql/recovery.conf <<EOF
restore_command = 'cp /data/postgresql/archive/%f %p'
recovery_target_time = '${TARGET_TIME}'
recovery_target_action = 'promote'
EOF

# Start PostgreSQL (will replay WAL to target time)
docker-compose -f docker-compose.infrastructure.yml start postgres-primary

# Monitor recovery
docker logs -f postgres-primary
```

### MinIO Backup

**Backup MinIO buckets**:
```bash
#!/bin/bash
# /usr/local/bin/backup-minio.sh

BACKUP_DIR="/backups/minio/$(date +%Y%m%d)"
mkdir -p "$BACKUP_DIR"

# Set MinIO alias
mc alias set local http://localhost:9000 ${MINIO_ACCESS_KEY} ${MINIO_SECRET_KEY}

# Backup each bucket
for bucket in gcrf-books gcrf-avatars gcrf-documents; do
    echo "Backing up bucket: $bucket"
    mc mirror local/$bucket "$BACKUP_DIR/$bucket"
done

echo "MinIO backup completed"
```

### Configuration Backup

**Backup all configuration**:
```bash
#!/bin/bash
# /usr/local/bin/backup-config.sh

BACKUP_DIR="/backups/config/$(date +%Y%m%d)"
mkdir -p "$BACKUP_DIR"

# Backup Docker configurations
cp -r /opt/gcrf-library/deployment/*.yml "$BACKUP_DIR/"

# Backup Nginx configuration
cp -r /etc/nginx/sites-available "$BACKUP_DIR/nginx/"

# Backup environment file (encrypted)
openssl enc -aes-256-cbc -salt -in /opt/gcrf-library/deployment/.env \
    -out "$BACKUP_DIR/env.enc" -k "${BACKUP_ENCRYPTION_KEY}"

# Backup Nacos configuration
docker exec nacos-server curl -X GET \
    "http://localhost:8848/nacos/v1/cs/configs?dataId=*&group=GCRF_GROUP" \
    > "$BACKUP_DIR/nacos-config.json"

echo "Configuration backup completed"
```

---

## Monitoring and Alerting

### Grafana Dashboards

**Access Grafana**: http://localhost:3000 (or https://library.gcrf.com:3000)

**Key Dashboards**:
1. **System Overview**
   - CPU, Memory, Disk, Network usage
   - Service health status
   - Request rate and latency

2. **Application Metrics**
   - HTTP request rate
   - Response time percentiles (p50, p90, p99)
   - Error rate
   - JVM metrics (heap, GC)

3. **Database Metrics**
   - Connection pool usage
   - Query performance
   - Transaction rate
   - Table sizes

4. **Infrastructure Metrics**
   - Redis hit/miss ratio
   - RabbitMQ queue depth
   - MinIO storage usage
   - Elasticsearch indices health

### Prometheus Queries

**Useful queries**:
```promql
# Request rate per service
rate(http_server_requests_seconds_count[5m])

# 95th percentile response time
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# JVM heap usage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}

# Database connections
hikaricp_connections_active

# Redis memory usage
redis_memory_used_bytes / redis_memory_max_bytes
```

### Alert Rules

**Create alert rules** in Prometheus (`/opt/gcrf-library/deployment/prometheus/alerts.yml`):
```yaml
groups:
  - name: GCRF_Alerts
    interval: 30s
    rules:
      # High error rate
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 10
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Service {{ $labels.service }} has error rate of {{ $value }} req/s"

      # Service down
      - alert: ServiceDown
        expr: up{job=~".*-service"} == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Service is down"
          description: "{{ $labels.job }} has been down for more than 2 minutes"

      # High response time
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High response time"
          description: "95th percentile response time is {{ $value }}s"

      # Database connections exhausted
      - alert: DatabaseConnectionsHigh
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Database connection pool nearly exhausted"

      # Disk space low
      - alert: DiskSpaceLow
        expr: (node_filesystem_avail_bytes / node_filesystem_size_bytes) < 0.2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Disk space is running low"
          description: "Only {{ $value }}% free space remaining"
```

### Viewing Alerts

**Check active alerts**:
```bash
# Prometheus alerts
curl http://localhost:9090/api/v1/alerts

# AlertManager alerts
curl http://localhost:9093/api/v1/alerts
```

---

## Performance Tuning

### JVM Tuning

**Recommended JVM options for services**:
```bash
# For services with moderate load (1GB heap)
JAVA_OPTS="-Xms512m -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:+ParallelRefProcEnabled \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/logs/heap_dump.hprof \
  -Djava.security.egd=file:/dev/./urandom"

# For high-load services (2GB heap)
JAVA_OPTS="-Xms1g -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:+ParallelRefProcEnabled \
  -XX:+UnlockExperimentalVMOptions \
  -XX:G1NewSizePercent=30 \
  -XX:G1MaxNewSizePercent=40 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/logs/heap_dump.hprof"
```

### Database Performance Tuning

**Analyze slow queries**:
```sql
-- Enable pg_stat_statements
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Find slow queries
SELECT
    calls,
    mean_exec_time,
    max_exec_time,
    query
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 20;

-- Find queries with high I/O
SELECT
    query,
    shared_blks_hit,
    shared_blks_read,
    shared_blks_hit::float / (shared_blks_hit + shared_blks_read) AS hit_ratio
FROM pg_stat_statements
WHERE shared_blks_hit + shared_blks_read > 0
ORDER BY hit_ratio
LIMIT 20;
```

**Create missing indexes**:
```sql
-- Find missing indexes
SELECT
    schemaname,
    tablename,
    seq_scan,
    idx_scan,
    seq_scan / NULLIF(idx_scan, 0) AS scan_ratio
FROM pg_stat_user_tables
WHERE seq_scan > 1000 AND idx_scan < 100
ORDER BY scan_ratio DESC;
```

### Redis Performance Tuning

**Monitor Redis performance**:
```bash
# Check slow log
docker exec redis-master redis-cli -a ${REDIS_PASSWORD} slowlog get 10

# Check hit rate
docker exec redis-master redis-cli -a ${REDIS_PASSWORD} info stats | grep keyspace

# Check memory usage
docker exec redis-master redis-cli -a ${REDIS_PASSWORD} info memory

# Find largest keys
docker exec redis-master redis-cli -a ${REDIS_PASSWORD} --bigkeys
```

### Network Optimization

**TCP tuning** (`/etc/sysctl.conf`):
```bash
# Increase TCP buffer sizes
net.core.rmem_max = 16777216
net.core.wmem_max = 16777216
net.ipv4.tcp_rmem = 4096 87380 16777216
net.ipv4.tcp_wmem = 4096 65536 16777216

# Enable TCP window scaling
net.ipv4.tcp_window_scaling = 1

# Increase connection backlog
net.core.somaxconn = 32768
net.ipv4.tcp_max_syn_backlog = 8192

# Reuse TIME_WAIT connections
net.ipv4.tcp_tw_reuse = 1

# Apply changes
sudo sysctl -p
```

---

## Security Operations

### Security Audit

**Daily security checks**:
```bash
#!/bin/bash
# /usr/local/bin/security-audit.sh

echo "Security Audit - $(date)"
echo "======================================"

# 1. Check for failed login attempts
echo -e "\n1. Failed Login Attempts (last 24h):"
grep "Failed password" /var/log/auth.log | wc -l

# 2. Check open ports
echo -e "\n2. Open Ports:"
ss -tulpn | grep LISTEN

# 3. Check Docker security
echo -e "\n3. Docker Security:"
docker inspect --format='{{.Name}}: Privileged={{.HostConfig.Privileged}}' $(docker ps -q)

# 4. Check file permissions
echo -e "\n4. Sensitive File Permissions:"
ls -l /opt/gcrf-library/deployment/.env
ls -l /opt/gcrf-library/secrets/

# 5. Check for rootkits
echo -e "\n5. Rootkit Check:"
if command -v rkhunter > /dev/null; then
    rkhunter --check --skip-keypress --report-warnings-only
fi

# 6. Check SSL certificate expiration
echo -e "\n6. SSL Certificate:"
openssl x509 -in /etc/nginx/ssl/fullchain.pem -noout -enddate
```

### Update Services

**Update Docker images**:
```bash
#!/bin/bash
# /usr/local/bin/update-images.sh

echo "Checking for image updates..."

# Pull latest images
docker-compose -f docker-compose.infrastructure.yml pull
docker-compose -f docker-compose.services.yml pull
docker-compose -f docker-compose.monitoring.yml pull

# Recreate containers with new images
docker-compose -f docker-compose.services.yml up -d --force-recreate

echo "Update completed"
```

**System updates**:
```bash
# Ubuntu
sudo apt update && sudo apt upgrade -y
sudo apt autoremove -y

# CentOS
sudo dnf update -y
sudo dnf autoremove -y

# Reboot if kernel updated
sudo reboot
```

---

## Scaling Operations

### Vertical Scaling (Increase Resources)

**Increase container resources**:
```yaml
# Edit docker-compose.services.yml
services:
  gcrf-gateway-service:
    deploy:
      resources:
        limits:
          cpus: '2.0'      # Increased from 1.0
          memory: 2G       # Increased from 1G
        reservations:
          cpus: '1.0'      # Increased from 0.5
          memory: 1G       # Increased from 512M
```

**Apply changes**:
```bash
docker-compose -f docker-compose.services.yml up -d --force-recreate gcrf-gateway-service
```

### Horizontal Scaling (Add Instances)

**Scale services**:
```bash
# Scale gateway to 3 instances
docker-compose -f docker-compose.services.yml up -d --scale gcrf-gateway-service=3

# Verify scaling
docker ps | grep gateway
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=gateway-service
```

---

## Maintenance Windows

### Planned Maintenance Procedure

**Recommended maintenance window**: Sunday 2:00 AM - 4:00 AM

1. **Pre-maintenance** (T-24 hours):
   - Notify users via system announcement
   - Schedule maintenance alert in AlertManager
   - Create full system backup

2. **During maintenance**:
   - Put system in maintenance mode
   - Apply updates
   - Run database maintenance
   - Test functionality

3. **Post-maintenance**:
   - Verify all services operational
   - Monitor for issues (4 hours)
   - Clear maintenance alerts
   - Notify users system is back

**Maintenance script**:
```bash
#!/bin/bash
# /usr/local/bin/maintenance.sh

case "$1" in
    start)
        echo "Starting maintenance mode..."
        # Enable maintenance page in Nginx
        ln -sf /etc/nginx/maintenance.html /var/www/html/index.html
        # Stop application services
        docker-compose -f /opt/gcrf-library/deployment/docker-compose.services.yml down
        ;;
    end)
        echo "Ending maintenance mode..."
        # Start application services
        docker-compose -f /opt/gcrf-library/deployment/docker-compose.services.yml up -d
        # Remove maintenance page
        rm /var/www/html/index.html
        ;;
    *)
        echo "Usage: $0 {start|end}"
        exit 1
        ;;
esac
```

---

## Emergency Procedures

### Emergency Contacts

| Role | Name | Phone | Email | Escalation |
|------|------|-------|-------|------------|
| Primary On-Call | DevOps Team | xxx-xxxx-xxxx | ops@gcrf-library.com | 15 min |
| Secondary On-Call | System Admin | xxx-xxxx-xxxx | admin@gcrf-library.com | 30 min |
| Manager | IT Manager | xxx-xxxx-xxxx | manager@gcrf-library.com | 1 hour |

### Emergency Response

**System Down**:
1. Check service status: `docker ps`
2. Check logs: `docker-compose logs`
3. Restart services: `docker-compose up -d`
4. If failed, restore from backup
5. Escalate if not resolved in 15 minutes

**Data Breach**:
1. Immediately isolate affected systems
2. Notify security team
3. Preserve logs for forensics
4. Follow incident response plan
5. Notify users if PII affected

**Performance Degradation**:
1. Check system resources: `top`, `df -h`
2. Check service metrics in Grafana
3. Scale up services if needed
4. Optimize database queries
5. Add caching if appropriate

---

**Document Version**: 1.0.0
**Last Updated**: 2025-12-01
**Next Review Date**: 2026-03-01
