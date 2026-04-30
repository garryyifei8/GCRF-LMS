# GCRF Library Management System - Alerting Guide

**Version:** 1.0.0
**Last Updated:** 2025-12-01

## Overview

This document describes the AlertManager-based alerting system for the GCRF Library Management System. The alerting system provides:

- Real-time monitoring alerts for services, infrastructure, and business metrics
- Multi-channel notification (email, Slack, PagerDuty, webhooks)
- Intelligent alert routing and grouping
- Alert silencing and inhibition rules
- Beautiful notification templates

## Architecture

```
+---------------+     +----------------+     +------------------+
|  Prometheus   | --> |  AlertManager  | --> |  Notifications   |
|  (metrics +   |     |  (routing +    |     |  - Email         |
|   rules)      |     |   grouping)    |     |  - Slack         |
+---------------+     +----------------+     |  - PagerDuty     |
                                             |  - Webhooks      |
                                             +------------------+
```

## File Structure

```
deployment/
├── alertmanager/
│   ├── alertmanager.yml          # Main AlertManager configuration
│   └── templates/
│       ├── email.tmpl            # Email notification templates
│       └── webhook.tmpl          # Webhook/Slack templates
├── monitoring/
│   └── prometheus/
│       ├── prometheus.yml        # Prometheus config with AlertManager
│       ├── alerts/               # Legacy alert rules
│       │   ├── infrastructure-alerts.yml
│       │   └── service-alerts.yml
│       └── rules/                # New organized alert rules
│           ├── service-alerts.yml
│           ├── infrastructure-alerts.yml
│           └── business-alerts.yml
└── scripts/
    └── test-alerts.sh            # Alert testing script
```

## Alert Categories

### 1. Service Alerts (`rules/service-alerts.yml`)

| Alert                   | Severity | Threshold                  | Description                  |
| ----------------------- | -------- | -------------------------- | ---------------------------- |
| ServiceDown             | critical | up == 0 for 1m             | Service is unreachable       |
| ServiceUnhealthy        | warning  | health check failed for 2m | Service health check failing |
| HighResponseTimeP95     | warning  | P95 > 500ms for 5m         | Response time too high       |
| CriticalResponseTimeP99 | critical | P99 > 1s for 3m            | Critical response time       |
| HighErrorRate5xx        | warning  | 5xx rate > 1% for 5m       | Server error rate high       |
| CriticalErrorRate5xx    | critical | 5xx rate > 5% for 2m       | Critical error rate          |
| DatabasePoolHighUsage   | warning  | pool > 80% for 5m          | Connection pool filling up   |
| DatabasePoolExhausted   | critical | pool > 95% for 2m          | Connection pool exhausted    |

### 2. Infrastructure Alerts (`rules/infrastructure-alerts.yml`)

| Alert               | Severity | Threshold            | Description              |
| ------------------- | -------- | -------------------- | ------------------------ |
| HighCPUUsage        | warning  | CPU > 80% for 5m     | High CPU utilization     |
| CriticalCPUUsage    | critical | CPU > 90% for 3m     | Critical CPU utilization |
| HighMemoryUsage     | warning  | Memory > 85% for 5m  | High memory usage        |
| CriticalMemoryUsage | critical | Memory > 95% for 2m  | Critical memory usage    |
| HighDiskUsage       | warning  | Disk > 80% for 10m   | Disk space running low   |
| CriticalDiskUsage   | critical | Disk > 90% for 5m    | Critical disk space      |
| PostgreSQLDown      | critical | pg_up == 0 for 1m    | Database unreachable     |
| RedisDown           | critical | redis_up == 0 for 1m | Cache unreachable        |

### 3. Business Alerts (`rules/business-alerts.yml`)

| Alert                        | Severity | Threshold                  | Description             |
| ---------------------------- | -------- | -------------------------- | ----------------------- |
| HighBorrowFailureRate        | warning  | > 10% failure for 10m      | Book borrowing issues   |
| CriticalBorrowFailureRate    | critical | > 25% server errors for 5m | Major borrowing outage  |
| HighReturnFailureRate        | warning  | > 10% failure for 10m      | Book return issues      |
| ReservationProcessingBacklog | warning  | > 100 pending for 2h       | Reservation backlog     |
| HighLoginFailureRate         | warning  | > 50% failed for 10m       | Possible security issue |

## Alert Routing

### Severity-Based Routing

```yaml
routes:
  - match:
      severity: critical
    receiver: "critical-alerts" # Email + PagerDuty + Slack
    group_wait: 10s
    repeat_interval: 1h

  - match:
      severity: warning
    receiver: "warning-alerts" # Email + Slack
    group_wait: 1m
    repeat_interval: 4h

  - match:
      severity: info
    receiver: "info-alerts" # Email digest
    group_wait: 10m
    repeat_interval: 24h
```

### Team-Based Routing

```yaml
routes:
  - match:
      category: database
    receiver: "dba-team"

  - match:
      category: security
    receiver: "security-alerts"

  - match:
      category: business
    receiver: "business-alerts"
```

## Inhibition Rules

Inhibition rules prevent alert storms by suppressing related alerts:

1. **Severity Cascading**: Critical alerts inhibit warnings of same type
2. **Service Down**: ServiceDown inhibits all other alerts for that service
3. **Database Down**: PostgreSQLDown inhibits all database-related alerts
4. **Resource Overload**: Critical resource alerts inhibit performance alerts

## Notification Channels

### Email Configuration

```yaml
global:
  smtp_smarthost: "smtp.example.com:587"
  smtp_from: "alertmanager@gcrf-library.com"
  smtp_auth_username: "alertmanager@gcrf-library.com"
  smtp_auth_password: "${SMTP_PASSWORD}"
```

### Slack Configuration

```yaml
global:
  slack_api_url: "${SLACK_WEBHOOK_URL}"

receivers:
  - name: "critical-alerts"
    slack_configs:
      - channel: "#gcrf-alerts-critical"
        send_resolved: true
```

### PagerDuty Configuration

```yaml
receivers:
  - name: "pagerduty-critical"
    pagerduty_configs:
      - service_key: "${PAGERDUTY_SERVICE_KEY}"
        send_resolved: true
        severity: critical
```

### Webhook Configuration

```yaml
receivers:
  - name: "default-receiver"
    webhook_configs:
      - url: "http://notification-service:8086/api/v1/alerts/webhook"
        send_resolved: true
```

## Environment Variables

Create a `.env` file in `deployment/` with these variables:

```bash
# SMTP Configuration
SMTP_PASSWORD=your_smtp_password

# Slack Configuration
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/xxx/yyy/zzz

# PagerDuty Configuration
PAGERDUTY_SERVICE_KEY=your_pagerduty_key

# Webhook Authentication
WEBHOOK_TOKEN=your_secure_token
```

## Operations Guide

### Starting the Stack

```bash
cd deployment
docker-compose -f docker-compose.monitoring.yml up -d
```

### Accessing AlertManager

- **UI**: http://localhost:9093
- **API**: http://localhost:9093/api/v2/

### Testing Alerts

Use the test script:

```bash
# Send a test alert
./scripts/test-alerts.sh send-test warning TestAlert test-service

# Send batch of test alerts
./scripts/test-alerts.sh send-batch

# Validate alert rules
./scripts/test-alerts.sh validate-rules

# Check AlertManager config
./scripts/test-alerts.sh check-config

# List current alerts
./scripts/test-alerts.sh list-alerts
```

### Creating Silences

Via UI:

1. Go to http://localhost:9093/#/silences
2. Click "New Silence"
3. Add matchers (e.g., alertname=TestAlert)
4. Set duration and comment

Via API:

```bash
./scripts/test-alerts.sh silence-create TestAlert 2h
```

### Reloading Configuration

```bash
# Reload Prometheus rules
curl -X POST http://localhost:9090/-/reload

# Reload AlertManager config
curl -X POST http://localhost:9093/-/reload
```

## Custom Metrics for Business Alerts

To enable business alerts, services should expose these custom metrics:

```java
// Example: Micrometer metrics in Spring Boot

@Component
public class CirculationMetrics {
    private final Counter borrowSuccess;
    private final Counter borrowFailure;
    private final Gauge reservationsPending;

    public CirculationMetrics(MeterRegistry registry) {
        this.borrowSuccess = Counter.builder("gcrf_borrow_total")
            .tag("status", "success")
            .register(registry);
        this.borrowFailure = Counter.builder("gcrf_borrow_total")
            .tag("status", "failure")
            .register(registry);
        this.reservationsPending = Gauge.builder("gcrf_reservations_pending",
            () -> reservationService.countPending())
            .register(registry);
    }
}
```

## Troubleshooting

### Alert Not Firing

1. Check if metric exists in Prometheus:

   ```
   http://localhost:9090/graph?g0.expr=your_metric
   ```

2. Check rule evaluation:

   ```
   http://localhost:9090/api/v1/rules?type=alert
   ```

3. Check AlertManager received it:
   ```
   http://localhost:9093/api/v2/alerts
   ```

### Notification Not Received

1. Check AlertManager logs:

   ```bash
   docker logs gcrf-alertmanager
   ```

2. Verify receiver configuration:

   ```bash
   ./scripts/test-alerts.sh receivers
   ```

3. Check for silences:
   ```bash
   ./scripts/test-alerts.sh silence-list
   ```

### Too Many Alerts

1. Increase `for` duration in rules
2. Add/adjust inhibition rules
3. Create appropriate silences during maintenance

## Best Practices

1. **Alert on Symptoms, Not Causes**: Alert on user-facing impacts
2. **Reduce Noise**: Group related alerts, use inhibitions
3. **Include Runbooks**: Every alert should have a runbook_url
4. **Test Regularly**: Use test-alerts.sh to verify setup
5. **Review and Tune**: Regularly review alert frequency and adjust thresholds
6. **Document Silences**: Always add comments to silences

## Related Documentation

- [Prometheus Alerting Rules](https://prometheus.io/docs/prometheus/latest/configuration/alerting_rules/)
- [AlertManager Configuration](https://prometheus.io/docs/alerting/latest/configuration/)
- [Grafana Alerting](https://grafana.com/docs/grafana/latest/alerting/)
