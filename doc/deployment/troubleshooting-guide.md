# GCRF Library Management System - Troubleshooting Guide

**Project**: 国创睿峰智能图书馆管理系统 (GCRF Intelligent Library Management System)
**Version**: 1.0.0
**Last Updated**: 2025-12-01
**Document Status**: Production Ready

---

## Table of Contents

1. [Quick Diagnostic Commands](#quick-diagnostic-commands)
2. [Service Startup Issues](#service-startup-issues)
3. [Database Connection Problems](#database-connection-problems)
4. [Authentication and Authorization Issues](#authentication-and-authorization-issues)
5. [Performance Problems](#performance-problems)
6. [Network and Connectivity Issues](#network-and-connectivity-issues)
7. [Data Inconsistency Issues](#data-inconsistency-issues)
8. [Container and Docker Issues](#container-and-docker-issues)
9. [Monitoring and Logging Issues](#monitoring-and-logging-issues)
10. [Common Error Messages](#common-error-messages)

---

## Quick Diagnostic Commands

### System Health Check

```bash
#!/bin/bash
# Quick diagnostic script

echo "=== System Resources ==="
echo "CPU: $(top -bn1 | grep "Cpu(s)" | awk '{print $2}')"
echo "Memory: $(free -h | grep Mem | awk '{print $3 "/" $2}')"
echo "Disk: $(df -h / | tail -1 | awk '{print $5 " used"}')"

echo -e "\n=== Docker Services ==="
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo -e "\n=== Service Health ==="
for port in 8080 8081 8082 8083 8084 8085 8086; do
    if curl -sf http://localhost:${port}/actuator/health > /dev/null 2>&1; then
        echo "✓ Port ${port}: UP"
    else
        echo "✗ Port ${port}: DOWN"
    fi
done

echo -e "\n=== Recent Errors ==="
echo "Application errors (last hour):"
find /logs/services -name "*.log" -mmin -60 -exec grep -i "ERROR" {} + | wc -l

echo -e "\n=== Network Connectivity ==="
ping -c 1 8.8.8.8 > /dev/null 2>&1 && echo "✓ Internet: OK" || echo "✗ Internet: FAILED"
curl -sf http://localhost:8848/nacos/v1/console/health/readiness > /dev/null && echo "✓ Nacos: OK" || echo "✗ Nacos: FAILED"
```

### Service-Specific Diagnostics

```bash
# Check specific service
SERVICE_NAME="gcrf-gateway-service"

echo "Service: $SERVICE_NAME"
echo "===================="

# Container status
docker inspect $SERVICE_NAME --format='{{.State.Status}}'

# Resource usage
docker stats $SERVICE_NAME --no-stream

# Recent logs
docker logs --tail 50 $SERVICE_NAME

# Health check
docker exec $SERVICE_NAME curl -sf http://localhost:8080/actuator/health || echo "Health check failed"
```

---

## Service Startup Issues

### Problem: Service Fails to Start

**Symptoms**:
- Container exits immediately after start
- Service shows "Exited" status in `docker ps -a`
- Error messages in logs

**Diagnostic Steps**:

1. **Check container logs**:
```bash
docker logs gcrf-auth-service
docker logs --tail 100 gcrf-auth-service
```

2. **Check resource constraints**:
```bash
# Check if out of memory
docker inspect gcrf-auth-service | grep -i memory

# Check disk space
df -h
```

3. **Verify configuration**:
```bash
# Check environment variables
docker exec gcrf-auth-service env | grep -E "DB_|REDIS_|NACOS_"

# Verify application.yml is present
docker exec gcrf-auth-service ls -la /app/config/
```

**Common Causes and Solutions**:

#### Cause 1: Missing or Incorrect Environment Variables

**Error Message**:
```
Error creating bean with name 'dataSource'
Could not resolve placeholder 'DB_PASSWORD'
```

**Solution**:
```bash
# Check .env file exists
cat /opt/gcrf-library/deployment/.env | grep DB_PASSWORD

# Restart service
docker-compose -f docker-compose.services.yml restart gcrf-auth-service
```

#### Cause 2: Database Not Ready

**Error Message**:
```
Connection refused: connect
org.postgresql.util.PSQLException: Connection to postgres-primary:5432 refused
```

**Solution**:
```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Check PostgreSQL health
docker exec postgres-primary pg_isready -U postgres

# If PostgreSQL is down, start it first
docker-compose -f docker-compose.infrastructure.yml up -d postgres-primary

# Wait 30 seconds, then restart application
sleep 30
docker-compose -f docker-compose.services.yml restart gcrf-auth-service
```

#### Cause 3: Port Already in Use

**Error Message**:
```
java.net.BindException: Address already in use
```

**Solution**:
```bash
# Find what's using the port
sudo lsof -i :8081
sudo netstat -tlnp | grep 8081

# Kill the process using the port
sudo kill -9 <PID>

# Or change port in docker-compose.yml and restart
```

#### Cause 4: Java Version Mismatch

**Error Message**:
```
Unsupported class file major version
java.lang.UnsupportedClassVersionError
```

**Solution**:
```bash
# Check Java version in container
docker exec gcrf-auth-service java -version

# Should be Java 21. If not, rebuild image with correct base image
# FROM eclipse-temurin:21-jre-jammy
```

### Problem: Service Starts but Doesn't Register with Nacos

**Symptoms**:
- Service running but not visible in Nacos console
- Gateway can't route to service
- Service health check passes locally

**Diagnostic Steps**:

1. **Check Nacos connectivity from service**:
```bash
docker exec gcrf-auth-service curl -sf http://nacos:8848/nacos/v1/console/health/readiness
```

2. **Check service logs for registration errors**:
```bash
docker logs gcrf-auth-service | grep -i "nacos\|register\|discovery"
```

3. **Verify Nacos configuration**:
```bash
# Check Nacos environment variables
docker exec gcrf-auth-service env | grep NACOS

# Check application.yml
docker exec gcrf-auth-service cat /app/config/application.yml | grep -A 10 nacos
```

**Solutions**:

1. **Incorrect Nacos address**:
```bash
# Should be: nacos:8848 (container name, not localhost)
# Fix in .env or application.yml and restart
```

2. **Nacos authentication failure**:
```bash
# Check Nacos credentials
# Login to Nacos console: http://localhost:8848/nacos
# Username/password should match NACOS_USERNAME and NACOS_PASSWORD in .env
```

3. **Network connectivity**:
```bash
# Check service is on correct network
docker inspect gcrf-auth-service | grep -A 5 Networks

# Should be on gcrf-backend-network
# If not, recreate service
docker-compose -f docker-compose.services.yml up -d --force-recreate gcrf-auth-service
```

---

## Database Connection Problems

### Problem: Cannot Connect to PostgreSQL

**Symptoms**:
- "Connection refused" errors
- "Connection timeout" errors
- Service fails to start with database errors

**Diagnostic Steps**:

1. **Check PostgreSQL is running**:
```bash
docker ps | grep postgres
docker exec postgres-primary pg_isready -U postgres
```

2. **Check connection from application**:
```bash
docker exec gcrf-auth-service psql -h postgres-primary -U auth_user -d gcrf_auth -c "SELECT 1"
```

3. **Check network connectivity**:
```bash
docker exec gcrf-auth-service ping postgres-primary
docker exec gcrf-auth-service telnet postgres-primary 5432
```

**Solutions**:

#### Solution 1: PostgreSQL Container Not Running

```bash
# Start PostgreSQL
docker-compose -f docker-compose.infrastructure.yml up -d postgres-primary

# Check logs for startup issues
docker logs postgres-primary

# If data corruption, restore from backup
```

#### Solution 2: Incorrect Credentials

```bash
# Test credentials
docker exec postgres-primary psql -U auth_user -d gcrf_auth -c "SELECT 1"

# If authentication fails, reset password
docker exec -it postgres-primary psql -U postgres <<EOF
ALTER USER auth_user WITH PASSWORD 'new_password';
EOF

# Update .env file with new password
# Restart service
```

#### Solution 3: Connection Pool Exhausted

**Error Message**:
```
HikariPool - Connection is not available, request timed out after 30000ms
```

**Solution**:
```bash
# Check active connections
docker exec postgres-primary psql -U postgres -c \
    "SELECT count(*) FROM pg_stat_activity WHERE datname='gcrf_auth';"

# Kill long-running queries
docker exec postgres-primary psql -U postgres -c \
    "SELECT pg_terminate_backend(pid) FROM pg_stat_activity
     WHERE datname='gcrf_auth' AND state='idle in transaction'
     AND query_start < NOW() - INTERVAL '10 minutes';"

# Increase connection pool size in application.yml
# spring.datasource.hikari.maximum-pool-size: 30
```

#### Solution 4: Database Locks

**Error Message**:
```
ERROR: deadlock detected
```

**Solution**:
```bash
# Find locked queries
docker exec postgres-primary psql -U postgres -d gcrf_auth -c \
    "SELECT pid, query, state, wait_event_type, wait_event
     FROM pg_stat_activity
     WHERE wait_event_type IS NOT NULL;"

# Kill blocking query
docker exec postgres-primary psql -U postgres -c \
    "SELECT pg_terminate_backend(<PID>);"
```

### Problem: Slow Database Queries

**Symptoms**:
- API requests timing out
- High database CPU usage
- Slow response times

**Diagnostic Steps**:

1. **Find slow queries**:
```sql
-- Enable pg_stat_statements if not already
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Find slowest queries
SELECT
    calls,
    mean_exec_time,
    max_exec_time,
    query
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 20;
```

2. **Check for missing indexes**:
```sql
SELECT
    schemaname,
    tablename,
    attname,
    n_distinct,
    correlation
FROM pg_stats
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY n_distinct DESC;
```

3. **Check table bloat**:
```sql
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

**Solutions**:

1. **Add missing indexes**:
```sql
-- Example: Add index on frequently queried column
CREATE INDEX idx_books_category_id ON books(category_id);
CREATE INDEX idx_circulation_reader_id ON circulation_records(reader_id);
CREATE INDEX idx_circulation_status ON circulation_records(status);
```

2. **Analyze and vacuum tables**:
```bash
docker exec postgres-primary psql -U postgres -d gcrf_book -c "VACUUM ANALYZE books;"
```

3. **Increase PostgreSQL work_mem**:
```bash
# Edit postgresql.conf
docker exec -it postgres-primary bash -c \
    "echo 'work_mem = 16MB' >> /var/lib/postgresql/data/postgresql.conf"

# Restart PostgreSQL
docker-compose -f docker-compose.infrastructure.yml restart postgres-primary
```

---

## Authentication and Authorization Issues

### Problem: JWT Token Invalid or Expired

**Symptoms**:
- 401 Unauthorized errors
- "Token expired" messages
- Users logged out unexpectedly

**Diagnostic Steps**:

1. **Verify JWT configuration**:
```bash
# Check JWT secret is set
docker exec gcrf-auth-service env | grep JWT_SECRET

# Check token expiration settings
docker exec gcrf-auth-service env | grep JWT_EXPIRATION
```

2. **Decode and inspect JWT token**:
```bash
# Use jwt.io or decode manually
echo "<token>" | cut -d'.' -f2 | base64 -d | jq
```

3. **Check system time**:
```bash
# JWT validation depends on accurate time
date
docker exec gcrf-auth-service date

# Time should be synchronized across all containers
```

**Solutions**:

#### Solution 1: Token Expired

```bash
# User needs to log in again to get new token
# Or implement refresh token mechanism

# Check token expiration setting
# Default: 86400 seconds (24 hours)
# Increase if needed in .env:
JWT_EXPIRATION=172800  # 48 hours
```

#### Solution 2: JWT Secret Mismatch

**Error Message**:
```
JWT signature does not match locally computed signature
```

**Solution**:
```bash
# Ensure all services use same JWT_SECRET
# Check .env file
grep JWT_SECRET /opt/gcrf-library/deployment/.env

# If secret was changed, all existing tokens are invalid
# Users need to log in again

# Restart all services to pick up new secret
docker-compose -f docker-compose.services.yml restart
```

#### Solution 3: Clock Skew Between Containers

**Solution**:
```bash
# Synchronize time across host and containers
sudo timedatectl set-ntp true
sudo systemctl restart chronyd

# Restart services
docker-compose -f docker-compose.services.yml restart
```

### Problem: User Cannot Access Resource (403 Forbidden)

**Symptoms**:
- User authenticated but gets 403 errors
- Permission denied messages

**Diagnostic Steps**:

1. **Check user roles and permissions**:
```sql
docker exec postgres-primary psql -U postgres -d gcrf_auth -c \
    "SELECT id, username, role, status FROM users WHERE username='<username>';"
```

2. **Check Gateway routing rules**:
```bash
# View Gateway routes
curl http://localhost:8848/nacos/v1/cs/configs?dataId=gateway-service.yml&group=GCRF_GROUP
```

3. **Check service logs for authorization errors**:
```bash
docker logs gcrf-gateway-service | grep -i "403\|forbidden\|denied"
```

**Solutions**:

1. **Update user role**:
```sql
docker exec postgres-primary psql -U postgres -d gcrf_auth -c \
    "UPDATE users SET role='ADMIN' WHERE username='<username>';"
```

2. **Check RBAC configuration in code**:
```java
// Verify @PreAuthorize annotations match user's role
@PreAuthorize("hasRole('ADMIN')")
```

---

## Performance Problems

### Problem: High CPU Usage

**Symptoms**:
- System sluggish
- High load average
- Slow API responses

**Diagnostic Steps**:

1. **Identify high CPU containers**:
```bash
docker stats --no-stream | sort -k3 -rh
```

2. **Check for infinite loops or runaway processes**:
```bash
# Get thread dump from Java service
docker exec gcrf-book-service jstack 1 > thread-dump.txt

# Look for threads in RUNNABLE state with high CPU
```

3. **Check for database query issues**:
```bash
# See "Slow Database Queries" section above
```

**Solutions**:

1. **Increase CPU limits**:
```yaml
# Edit docker-compose.services.yml
deploy:
  resources:
    limits:
      cpus: '2.0'  # Increased from 1.0
```

2. **Scale horizontally**:
```bash
docker-compose -f docker-compose.services.yml up -d --scale gcrf-book-service=2
```

3. **Optimize code**:
- Review slow endpoints
- Add caching for frequently accessed data
- Optimize database queries

### Problem: High Memory Usage

**Symptoms**:
- Out of memory errors
- Container restarts
- System swapping

**Diagnostic Steps**:

1. **Check memory usage**:
```bash
free -h
docker stats --no-stream | sort -k4 -rh
```

2. **Get heap dump from Java service**:
```bash
docker exec gcrf-book-service jmap -dump:live,format=b,file=/tmp/heap-dump.hprof 1
docker cp gcrf-book-service:/tmp/heap-dump.hprof ./

# Analyze with VisualVM or Eclipse MAT
```

3. **Check for memory leaks**:
```bash
# Monitor GC activity
docker exec gcrf-book-service jstat -gc 1 1000
```

**Solutions**:

1. **Increase heap size**:
```yaml
# Edit docker-compose.services.yml
environment:
  JAVA_OPTS: "-Xms1g -Xmx2g"  # Increased from 512m-1g
```

2. **Tune GC**:
```yaml
JAVA_OPTS: "-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+ParallelRefProcEnabled"
```

3. **Fix memory leaks**:
- Review code for unclosed connections
- Check for large object caching
- Implement proper cleanup in finally blocks

### Problem: Slow API Response Times

**Symptoms**:
- API requests taking >1 second
- Timeout errors
- Poor user experience

**Diagnostic Steps**:

1. **Check Grafana metrics**:
- Response time percentiles (p95, p99)
- Request rate
- Error rate

2. **Identify slow endpoints**:
```bash
# Analyze Nginx access logs
awk '$NF > 1.0 {print $7, $NF}' /var/log/nginx/gcrf-library-access.log | \
    sort -k2 -rn | head -20
```

3. **Profile application**:
```bash
# Enable Spring Boot Actuator profiling
curl http://localhost:8082/actuator/metrics/http.server.requests
```

**Solutions**:

1. **Add caching**:
```java
@Cacheable("books")
public Book findById(Long id) {
    return bookRepository.findById(id);
}
```

2. **Optimize database queries**:
- Add indexes
- Use pagination
- Avoid N+1 queries (use JOIN FETCH)

3. **Implement async processing**:
```java
@Async
public CompletableFuture<List<Book>> findBooksAsync() {
    return CompletableFuture.completedFuture(bookRepository.findAll());
}
```

---

## Network and Connectivity Issues

### Problem: Services Cannot Communicate

**Symptoms**:
- "Connection refused" between services
- Services isolated from each other
- Feign client errors

**Diagnostic Steps**:

1. **Check Docker networks**:
```bash
docker network ls
docker network inspect gcrf-backend-network
```

2. **Test connectivity between containers**:
```bash
docker exec gcrf-gateway-service ping gcrf-auth-service
docker exec gcrf-gateway-service curl http://gcrf-auth-service:8081/actuator/health
```

3. **Check DNS resolution**:
```bash
docker exec gcrf-gateway-service nslookup gcrf-auth-service
```

**Solutions**:

1. **Ensure containers on same network**:
```bash
# Check network membership
docker inspect gcrf-gateway-service | grep -A 10 Networks

# Reconnect to network if needed
docker network connect gcrf-backend-network gcrf-gateway-service
```

2. **Use container names, not localhost**:
```yaml
# Correct:
url: http://auth-service:8081

# Incorrect:
url: http://localhost:8081
```

3. **Check firewall rules**:
```bash
# Ensure Docker networks are not blocked
sudo iptables -L -n | grep docker
```

### Problem: Cannot Access System from External Network

**Symptoms**:
- Cannot reach https://library.gcrf.com
- Connection timeout from outside
- Works on localhost but not remotely

**Diagnostic Steps**:

1. **Check Nginx is running**:
```bash
sudo systemctl status nginx
sudo nginx -t
```

2. **Check firewall**:
```bash
# Ubuntu
sudo ufw status

# CentOS
sudo firewall-cmd --list-all
```

3. **Check port bindings**:
```bash
sudo netstat -tlnp | grep -E "80|443"
```

**Solutions**:

1. **Open firewall ports**:
```bash
# Ubuntu
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# CentOS
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

2. **Verify Nginx configuration**:
```bash
sudo nginx -t
sudo systemctl restart nginx
```

3. **Check DNS resolution**:
```bash
nslookup library.gcrf.com
# Should return your server's public IP
```

---

## Data Inconsistency Issues

### Problem: Data Missing or Incorrect

**Symptoms**:
- Records not showing up in UI
- Inconsistent counts between services
- Duplicate records

**Diagnostic Steps**:

1. **Check database directly**:
```sql
docker exec postgres-primary psql -U postgres -d gcrf_book -c \
    "SELECT count(*) FROM books;"

docker exec postgres-primary psql -U postgres -d gcrf_book -c \
    "SELECT * FROM books ORDER BY created_at DESC LIMIT 10;"
```

2. **Check Redis cache**:
```bash
docker exec redis-master redis-cli -a ${REDIS_PASSWORD} KEYS "books:*"
docker exec redis-master redis-cli -a ${REDIS_PASSWORD} GET "books:123"
```

3. **Check for transaction failures**:
```bash
docker logs gcrf-book-service | grep -i "rollback\|transaction"
```

**Solutions**:

1. **Clear cache**:
```bash
# Clear specific cache
docker exec redis-master redis-cli -a ${REDIS_PASSWORD} DEL "books:*"

# Clear all cache (use with caution!)
docker exec redis-master redis-cli -a ${REDIS_PASSWORD} FLUSHDB
```

2. **Refresh data from database**:
```bash
# Restart service to reload caches
docker-compose -f docker-compose.services.yml restart gcrf-book-service
```

3. **Fix data inconsistency**:
```sql
-- Example: Update incorrect counts
UPDATE book_categories
SET book_count = (
    SELECT count(*)
    FROM books
    WHERE category_id = book_categories.id
);
```

---

## Container and Docker Issues

### Problem: Docker Daemon Not Responding

**Symptoms**:
- `docker ps` hangs
- Cannot start/stop containers
- Docker commands timeout

**Solutions**:

1. **Restart Docker daemon**:
```bash
sudo systemctl restart docker

# Wait for Docker to be ready
sleep 10
docker ps
```

2. **Check Docker disk space**:
```bash
# Check Docker data directory
df -h /var/lib/docker

# Clean up unused resources
docker system prune -a --volumes
```

3. **Check Docker logs**:
```bash
sudo journalctl -u docker -n 100
```

### Problem: Container Keeps Restarting

**Symptoms**:
- Container status shows "Restarting"
- Service unavailable
- Logs show crash/exit

**Diagnostic Steps**:

1. **Check container logs**:
```bash
docker logs --tail 100 gcrf-auth-service
```

2. **Check exit code**:
```bash
docker inspect gcrf-auth-service --format='{{.State.ExitCode}}'
# Exit code 137 = out of memory
# Exit code 1 = application error
```

3. **Check resource limits**:
```bash
docker stats --no-stream gcrf-auth-service
```

**Solutions**:

1. **Fix application error** (see logs)

2. **Increase memory limit**:
```yaml
deploy:
  resources:
    limits:
      memory: 2G  # Increased
```

3. **Disable restart policy temporarily**:
```bash
docker update --restart=no gcrf-auth-service
# Debug the issue, then re-enable
docker update --restart=unless-stopped gcrf-auth-service
```

---

## Monitoring and Logging Issues

### Problem: Metrics Not Showing in Grafana

**Symptoms**:
- Empty dashboards
- "No data" messages
- Prometheus not scraping metrics

**Diagnostic Steps**:

1. **Check Prometheus targets**:
```bash
curl http://localhost:9090/api/v1/targets
# Look for targets with state "down"
```

2. **Check service actuator endpoints**:
```bash
curl http://localhost:8081/actuator/prometheus
# Should return metrics in Prometheus format
```

3. **Check Grafana data source**:
- Login to Grafana
- Configuration > Data Sources
- Test connection to Prometheus

**Solutions**:

1. **Fix Prometheus configuration**:
```yaml
# Ensure correct job configuration in prometheus.yml
scrape_configs:
  - job_name: 'auth-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['gcrf-auth-service:8081']
```

2. **Restart Prometheus**:
```bash
docker-compose -f docker-compose.monitoring.yml restart prometheus
```

3. **Enable actuator in application**:
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

### Problem: Logs Not Being Collected

**Symptoms**:
- Empty log files
- Logs not in Loki
- Cannot find recent logs

**Solutions**:

1. **Check log directories exist**:
```bash
ls -la /logs/services/gateway/
# Create if missing
mkdir -p /logs/services/gateway
```

2. **Check log configuration**:
```yaml
# application.yml
logging:
  file:
    name: /logs/services/auth/application.log
```

3. **Check Promtail is running**:
```bash
docker ps | grep promtail
docker logs promtail
```

---

## Common Error Messages

### Error: "java.lang.OutOfMemoryError: Java heap space"

**Cause**: JVM heap exhausted

**Solution**:
```bash
# Increase heap size
# Edit docker-compose.services.yml
environment:
  JAVA_OPTS: "-Xms1g -Xmx2g"

# Restart service
docker-compose -f docker-compose.services.yml restart gcrf-book-service
```

### Error: "Connection pool exhausted"

**Cause**: Too many database connections

**Solution**:
```yaml
# Increase pool size in application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30  # Increased from 20
```

### Error: "com.alibaba.nacos.api.exception.NacosException: No available server"

**Cause**: Cannot connect to Nacos

**Solution**:
```bash
# Check Nacos is running
docker ps | grep nacos

# Check Nacos address
docker exec gcrf-auth-service env | grep NACOS_SERVER_ADDR
# Should be: nacos:8848

# Restart Nacos if needed
docker-compose -f docker-compose.infrastructure.yml restart nacos
```

### Error: "Feign.RetryableException: Connection refused"

**Cause**: Target service not available

**Solution**:
```bash
# Check target service is running
docker ps | grep <service-name>

# Check service registered in Nacos
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=<service-name>

# Restart target service
docker-compose -f docker-compose.services.yml restart <service-name>
```

---

## Getting Help

### Information to Collect Before Escalating

1. **System Information**:
```bash
uname -a
docker --version
docker-compose --version
```

2. **Service Status**:
```bash
docker ps -a
docker-compose -f docker-compose.services.yml ps
```

3. **Recent Logs**:
```bash
docker logs --tail 200 <failing-service> > service-logs.txt
```

4. **Resource Usage**:
```bash
docker stats --no-stream > docker-stats.txt
free -h
df -h
```

5. **Configuration**:
```bash
# Sanitize sensitive information first!
docker-compose -f docker-compose.services.yml config > docker-config.txt
```

### Support Channels

- **Internal Ops Team**: ops@gcrf-library.com
- **System Administrator**: admin@gcrf-library.com
- **Emergency Hotline**: xxx-xxxx-xxxx

---

**Document Version**: 1.0.0
**Last Updated**: 2025-12-01
**Next Review Date**: 2026-03-01
