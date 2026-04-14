# GCRF Library Management System - Loki Log Aggregation Setup Guide

**Version**: 1.0.0
**Last Updated**: 2025-12-01
**Author**: GCRF Observability Team

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Quick Start](#quick-start)
4. [Configuration](#configuration)
5. [Grafana Integration](#grafana-integration)
6. [LogQL Query Examples](#logql-query-examples)
7. [Spring Boot Integration](#spring-boot-integration)
8. [Troubleshooting](#troubleshooting)
9. [Production Considerations](#production-considerations)

---

## Overview

This guide describes the Loki log aggregation setup for the GCRF Library Management System. Loki provides centralized logging with the following benefits:

- **Cost-effective**: Index-free design, only labels are indexed
- **Scalable**: Horizontal scaling with S3/MinIO backend
- **Integration**: Native Grafana support for log visualization
- **Correlation**: Link logs with Prometheus metrics via labels

### Components

| Component | Version | Purpose |
|-----------|---------|---------|
| Loki | 2.9.3 | Log aggregation and storage |
| Promtail | 2.9.3 | Log collection agent |
| Grafana | 10.2.2 | Visualization and exploration |

---

## Architecture

```
+------------------+     +------------------+     +------------------+
|  Spring Boot     |     |  Spring Boot     |     |  Infrastructure  |
|  Services        |     |  Services        |     |  (PostgreSQL,    |
|  (JSON logs)     |     |  (JSON logs)     |     |   Redis, etc.)   |
+--------+---------+     +--------+---------+     +--------+---------+
         |                        |                        |
         v                        v                        v
    Docker Container Logs (json-file driver)
         |                        |                        |
         +------------------------+------------------------+
                                  |
                                  v
                    +-------------+--------------+
                    |         Promtail           |
                    |   (Log Collection Agent)   |
                    |  - Docker SD discovery     |
                    |  - JSON parsing            |
                    |  - Label extraction        |
                    +-------------+--------------+
                                  |
                                  v
                    +-------------+--------------+
                    |           Loki             |
                    |   (Log Aggregation)        |
                    |  - 30 day retention        |
                    |  - TSDB index              |
                    |  - Filesystem/S3 storage   |
                    +-------------+--------------+
                                  |
                                  v
                    +-------------+--------------+
                    |         Grafana            |
                    |   (Visualization)          |
                    |  - Logs Dashboard          |
                    |  - Explore mode            |
                    |  - Alerting                |
                    +---------------------------+
```

---

## Quick Start

### 1. Start the Observability Stack

```bash
cd deployment

# Start infrastructure first (if not running)
docker-compose -f docker-compose.infrastructure.yml up -d

# Start observability stack
docker-compose -f docker-compose.observability.yml up -d

# Verify services are healthy
docker-compose -f docker-compose.observability.yml ps
```

### 2. Access Points

| Service | URL | Credentials |
|---------|-----|-------------|
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | - |
| Loki (internal) | http://localhost:3100 | - |

### 3. View Logs in Grafana

1. Open Grafana at http://localhost:3000
2. Navigate to **Explore** (compass icon)
3. Select **Loki** datasource
4. Enter a query: `{tier="backend"}`
5. Click **Run query**

---

## Configuration

### Loki Configuration (`deployment/loki/loki-config.yml`)

Key settings:

```yaml
# Retention: 30 days
limits_config:
  retention_period: 720h

# Query limits
limits_config:
  max_entries_limit_per_query: 5000
  max_query_series: 500
  query_timeout: 5m

# Storage: Filesystem (development)
storage_config:
  filesystem:
    directory: /loki/chunks
```

### Promtail Configuration (`deployment/loki/promtail-config.yml`)

Key features:

1. **Docker Discovery**: Automatically discovers containers with `com.gcrf.service` label
2. **Label Extraction**: Extracts service_name, tier, component from Docker labels
3. **JSON Parsing**: Parses Spring Boot JSON logs
4. **Pipeline Stages**: Multi-stage processing for log enrichment

### Docker Labels Required

Services must have these Docker labels for log collection:

```yaml
labels:
  com.gcrf.service: "auth"           # Service name
  com.gcrf.tier: "backend"           # Tier (backend, frontend, infrastructure)
  com.gcrf.component: "authentication"  # Component description
  com.gcrf.version: "1.0.0"          # Version
```

---

## Grafana Integration

### Datasource Configuration

The Loki datasource is auto-provisioned at `deployment/monitoring/grafana/provisioning/datasources/loki.yml`:

```yaml
datasources:
  - name: Loki
    type: loki
    url: http://loki:3100
    isDefault: false
```

### Pre-built Dashboard

The logs dashboard is available at: **Dashboards > GCRF Library System > GCRF Logs Dashboard**

Features:
- Log volume by service (bar chart)
- Log level distribution (pie chart)
- Error rate trend (time series)
- Real-time log stream with filtering
- Error analysis table
- Trace correlation panel

### Dashboard Variables

| Variable | Description | Example |
|----------|-------------|---------|
| $service | Filter by service name | auth, gateway, book |
| $search | Full-text search | error, exception |
| $infra_component | Infrastructure component | postgresql, redis |
| $trace_id | Distributed trace ID | abc123def456 |

---

## LogQL Query Examples

### Basic Queries

```logql
# All backend service logs
{tier="backend"}

# Specific service logs
{service_name="auth"}

# Filter by log level
{tier="backend"} | json | level="error"

# Full-text search
{tier="backend"} |= "exception"

# Regex filter
{tier="backend"} |~ "(?i)error|exception"
```

### Advanced Queries

```logql
# Parse JSON and filter by user
{tier="backend"} | json | userId="user123"

# Trace correlation
{tier="backend"} | json | traceId="abc123def456"

# Count errors by service over 5m
sum by (service_name) (count_over_time({tier="backend"} |~ "error" [5m]))

# Error rate percentage
sum(rate({tier="backend"} |~ "error" [5m])) / sum(rate({tier="backend"} [5m])) * 100

# Top 10 error messages
topk(10, sum by (message) (count_over_time({tier="backend"} | json | level="error" [1h])))

# Slow requests (>3s)
{service_name="gateway"} | json | request_time > 3000

# Logs with stack traces
{tier="backend"} |= "stack_trace"
```

### Aggregation Examples

```logql
# Log volume per minute
sum(count_over_time({tier="backend"} [1m]))

# Log volume by level
sum by (level) (count_over_time({tier="backend"} | json [5m]))

# Unique users with errors
count(count by (userId) ({tier="backend"} | json | level="error"))
```

---

## Spring Boot Integration

### MDC Context Fields

The `MdcLoggingFilter` adds these fields to every log:

| Field | Description | Source |
|-------|-------------|--------|
| traceId | Distributed trace ID | X-Trace-Id header or generated |
| spanId | Span ID | X-Span-Id header or generated |
| requestId | Request ID | X-Request-Id header or traceId |
| userId | User ID | X-User-Id header |
| clientIp | Client IP address | Request headers |
| service | Service name | spring.application.name |
| requestPath | Request URI | Request |
| requestMethod | HTTP method | Request |

### Logging Configuration

Services should include the common logback configuration:

```xml
<!-- src/main/resources/logback-spring.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="logback-spring.xml"/>
</configuration>
```

Or use the provided configuration directly from `common-web`.

### Profile-based Logging

| Profile | Format | Output |
|---------|--------|--------|
| dev | Colored console | STDOUT |
| prod | JSON | STDOUT + File |
| docker | JSON | STDOUT (for Promtail) |
| test | Plain | STDOUT |

### Adding Custom MDC Fields

```java
import org.slf4j.MDC;

// In your service code
MDC.put("customField", "value");
try {
    // Your code
    log.info("Processing order");  // customField will be in the log
} finally {
    MDC.remove("customField");
}
```

---

## Troubleshooting

### Common Issues

#### 1. Logs Not Appearing in Loki

**Check Promtail is running:**
```bash
docker logs gcrf-promtail
```

**Verify Docker socket access:**
```bash
docker exec gcrf-promtail ls -la /var/run/docker.sock
```

**Check container labels:**
```bash
docker inspect gcrf-auth-service | grep -A 10 Labels
```

#### 2. JSON Parsing Errors

**Verify log format:**
```bash
docker logs gcrf-auth-service 2>&1 | head -5
```

Expected format:
```json
{"timestamp":"2025-12-01T10:00:00.000Z","level":"INFO","message":"..."}
```

#### 3. High Memory Usage

Reduce retention or enable compaction:
```yaml
# loki-config.yml
compactor:
  retention_enabled: true
  retention_delete_delay: 2h
```

#### 4. Query Timeouts

Increase limits:
```yaml
limits_config:
  query_timeout: 10m
  max_query_parallelism: 64
```

### Health Checks

```bash
# Loki health
curl http://localhost:3100/ready

# Promtail health
curl http://localhost:9080/ready

# Loki metrics
curl http://localhost:3100/metrics | grep loki_ingester
```

### Logs for Debugging

```bash
# Promtail logs
docker logs -f gcrf-promtail

# Loki logs
docker logs -f gcrf-loki

# Check Promtail targets
curl http://localhost:9080/targets
```

---

## Production Considerations

### Storage Backend

For production, use S3/MinIO storage:

```yaml
# loki-config.yml
storage_config:
  aws:
    s3: s3://minio:9000/loki-logs
    s3forcepathstyle: true
  boltdb_shipper:
    active_index_directory: /loki/index
    shared_store: s3
```

### High Availability

Deploy multiple Loki instances with shared storage:

```yaml
# Add to loki-config.yml
common:
  replication_factor: 3
  ring:
    kvstore:
      store: consul
```

### Resource Limits

Recommended production limits:

| Component | CPU | Memory |
|-----------|-----|--------|
| Loki | 2 cores | 4GB |
| Promtail | 0.5 core | 256MB |

### Retention Policy

Configure based on compliance requirements:

```yaml
limits_config:
  retention_period: 2160h  # 90 days for audit logs
```

### Alerting

Example alert rule for error spike:

```yaml
groups:
  - name: log-alerts
    rules:
      - alert: HighErrorRate
        expr: |
          sum(rate({tier="backend"} |~ "error" [5m])) > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: High error rate detected
```

---

## File Reference

| File | Purpose |
|------|---------|
| `deployment/loki/loki-config.yml` | Loki server configuration |
| `deployment/loki/promtail-config.yml` | Promtail agent configuration |
| `deployment/docker-compose.observability.yml` | Full observability stack |
| `deployment/monitoring/grafana/provisioning/datasources/loki.yml` | Grafana Loki datasource |
| `deployment/monitoring/grafana/dashboards/logs-dashboard.json` | Pre-built logs dashboard |
| `backend/common/common-web/src/main/resources/logback-spring.xml` | Logback configuration |
| `backend/common/common-web/src/main/java/com/gcrf/library/common/web/filter/MdcLoggingFilter.java` | MDC context filter |

---

## Support

For issues or questions:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review Loki documentation: https://grafana.com/docs/loki/latest/
3. Contact the GCRF DevOps team

---

**Document Revision History**

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0.0 | 2025-12-01 | GCRF Team | Initial release |
