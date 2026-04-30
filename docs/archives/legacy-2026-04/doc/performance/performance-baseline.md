# GCRF Library Management System - Performance Baseline

**Document Version**: 1.0.0
**Last Updated**: 2025-12-01
**Status**: Active

---

## Table of Contents

1. [Overview](#overview)
2. [Performance Objectives](#performance-objectives)
3. [Service Level Indicators (SLIs)](#service-level-indicators-slis)
4. [Service Level Objectives (SLOs)](#service-level-objectives-slos)
5. [Performance Metrics](#performance-metrics)
6. [Baseline Measurements](#baseline-measurements)
7. [Resource Utilization Limits](#resource-utilization-limits)
8. [Test Scenarios](#test-scenarios)
9. [Monitoring & Alerting](#monitoring--alerting)
10. [Performance Budgets](#performance-budgets)
11. [Regression Detection](#regression-detection)

---

## Overview

This document defines the performance baseline and targets for the GCRF Library Management System. It establishes measurable performance criteria that must be maintained across all releases and deployments.

### System Architecture Overview

| Component           | Technology                 | Role                           |
| ------------------- | -------------------------- | ------------------------------ |
| API Gateway         | Spring Cloud Gateway       | Request routing, rate limiting |
| Auth Service        | Spring Security + JWT      | Authentication & authorization |
| Book Service        | Spring Boot + MyBatis Plus | Book catalog management        |
| Circulation Service | Spring Boot                | Borrow/return operations       |
| Reader Service      | Spring Boot                | Reader management              |
| Database            | PostgreSQL 15+             | Primary data store             |
| Cache               | Redis 7.x                  | Session & data caching         |
| Message Queue       | RabbitMQ 3.12.x            | Async processing               |

---

## Performance Objectives

### Primary Objectives

1. **Response Time**: Ensure fast, consistent response times for all user interactions
2. **Throughput**: Handle expected peak load with headroom for growth
3. **Reliability**: Maintain high availability with minimal errors
4. **Scalability**: Support horizontal scaling for increased demand
5. **Stability**: Maintain consistent performance over extended periods

### Target Capacity

| Metric                | Target  | Notes                    |
| --------------------- | ------- | ------------------------ |
| Concurrent Users      | 500     | Normal operation         |
| Peak Concurrent Users | 1000    | During events/promotions |
| Requests per Second   | 500 TPS | Sustained throughput     |
| Daily Active Users    | 5,000   | Expected DAU             |
| Monthly Active Users  | 20,000  | Expected MAU             |

---

## Service Level Indicators (SLIs)

### Response Time SLIs

| SLI                         | Definition                    | Measurement       |
| --------------------------- | ----------------------------- | ----------------- |
| `http_request_duration_p50` | 50th percentile response time | All API endpoints |
| `http_request_duration_p90` | 90th percentile response time | All API endpoints |
| `http_request_duration_p95` | 95th percentile response time | All API endpoints |
| `http_request_duration_p99` | 99th percentile response time | All API endpoints |

### Availability SLIs

| SLI                    | Definition                           | Measurement        |
| ---------------------- | ------------------------------------ | ------------------ |
| `service_availability` | Successful requests / Total requests | All services       |
| `error_rate`           | Failed requests / Total requests     | HTTP 5xx responses |
| `health_check_success` | Successful health checks             | /actuator/health   |

### Throughput SLIs

| SLI                       | Definition                       | Measurement         |
| ------------------------- | -------------------------------- | ------------------- |
| `requests_per_second`     | HTTP requests per second         | Gateway ingress     |
| `transactions_per_second` | Business transactions per second | Circulation service |

---

## Service Level Objectives (SLOs)

### Response Time SLOs

| Endpoint Category           | P50     | P90     | P95 (Primary) | P99      |
| --------------------------- | ------- | ------- | ------------- | -------- |
| Authentication              | < 200ms | < 400ms | < 500ms       | < 1000ms |
| Book List/Search            | < 150ms | < 300ms | < 500ms       | < 800ms  |
| Book Detail                 | < 100ms | < 200ms | < 300ms       | < 500ms  |
| Circulation (Borrow/Return) | < 300ms | < 600ms | < 1000ms      | < 2000ms |
| Reader Queries              | < 150ms | < 300ms | < 500ms       | < 800ms  |
| Health Check                | < 50ms  | < 100ms | < 200ms       | < 500ms  |

### Availability SLOs

| Metric                  | Target   | Window  |
| ----------------------- | -------- | ------- |
| Service Availability    | >= 99.9% | Monthly |
| Error Rate (5xx)        | < 0.1%   | Daily   |
| Error Rate (4xx client) | < 5%     | Daily   |

### Throughput SLOs

| Metric        | Target | Conditions         |
| ------------- | ------ | ------------------ |
| Minimum TPS   | 500    | Normal load        |
| Peak TPS      | 1000   | During spike       |
| Sustained TPS | 300    | Soak test (1 hour) |

---

## Performance Metrics

### API Response Time Targets

```yaml
# Primary SLO: P95 < 500ms for all read operations

endpoints:
  # Authentication
  POST /api/v1/auth/login:
    p95_target: 500ms
    p99_target: 1000ms

  POST /api/v1/auth/logout:
    p95_target: 200ms
    p99_target: 500ms

  # Book Service
  GET /api/v1/books:
    p95_target: 500ms
    p99_target: 800ms
    notes: "Paginated list, max 50 items per page"

  GET /api/v1/books/{id}:
    p95_target: 300ms
    p99_target: 500ms

  GET /api/v1/books/search:
    p95_target: 800ms
    p99_target: 1500ms
    notes: "Full-text search with filters"

  # Circulation Service
  POST /api/v1/circulation/borrow:
    p95_target: 1000ms
    p99_target: 2000ms
    notes: "Transactional operation"

  POST /api/v1/circulation/return:
    p95_target: 1000ms
    p99_target: 2000ms
    notes: "Includes fine calculation"

  GET /api/v1/circulation/records:
    p95_target: 500ms
    p99_target: 800ms

  # Reader Service
  GET /api/v1/readers:
    p95_target: 500ms
    p99_target: 800ms

  GET /api/v1/readers/{id}:
    p95_target: 300ms
    p99_target: 500ms
```

### Error Rate Targets

| Error Type         | Target  | Alert Threshold |
| ------------------ | ------- | --------------- |
| HTTP 5xx           | < 0.1%  | > 0.5%          |
| HTTP 4xx           | < 5%    | > 10%           |
| Timeout            | < 0.05% | > 0.2%          |
| Connection Refused | 0%      | > 0%            |

### Throughput Targets

| Scenario    | TPS Target | Duration   |
| ----------- | ---------- | ---------- |
| Normal Load | 500 TPS    | Sustained  |
| Peak Load   | 800 TPS    | 15 minutes |
| Spike Load  | 1000 TPS   | 5 minutes  |
| Soak Test   | 300 TPS    | 1 hour     |

---

## Baseline Measurements

### Initial Baseline (To Be Measured)

Run the load test and record baseline metrics:

```bash
# Run baseline measurement
./deployment/scripts/run-performance-test.sh load

# Expected baseline results structure:
baseline:
  date: "YYYY-MM-DD"
  environment: "staging"
  load_test:
    vus: 100
    duration: "5m"
    results:
      http_req_duration_p50: TBD
      http_req_duration_p90: TBD
      http_req_duration_p95: TBD
      http_req_duration_p99: TBD
      http_req_failed: TBD
      requests_per_second: TBD
```

### Baseline Update Schedule

| Frequency   | Action                                |
| ----------- | ------------------------------------- |
| Weekly      | Automated baseline comparison         |
| Monthly     | Full baseline re-measurement          |
| Per Release | Pre-deployment performance validation |
| Quarterly   | Comprehensive performance review      |

---

## Resource Utilization Limits

### CPU Utilization

| Component            | Normal | Warning | Critical |
| -------------------- | ------ | ------- | -------- |
| API Gateway          | < 60%  | 60-80%  | > 80%    |
| Application Services | < 70%  | 70-85%  | > 85%    |
| PostgreSQL           | < 60%  | 60-75%  | > 75%    |
| Redis                | < 50%  | 50-70%  | > 70%    |

### Memory Utilization

| Component            | Normal | Warning | Critical |
| -------------------- | ------ | ------- | -------- |
| API Gateway          | < 70%  | 70-85%  | > 85%    |
| Application Services | < 75%  | 75-85%  | > 85%    |
| PostgreSQL           | < 80%  | 80-90%  | > 90%    |
| Redis                | < 70%  | 70-85%  | > 85%    |

### JVM Heap Settings

```yaml
services:
  gateway-service:
    heap_min: 512m
    heap_max: 1g
    gc: G1GC

  auth-service:
    heap_min: 256m
    heap_max: 512m
    gc: G1GC

  book-service:
    heap_min: 512m
    heap_max: 1g
    gc: G1GC

  circulation-service:
    heap_min: 512m
    heap_max: 1g
    gc: G1GC

  reader-service:
    heap_min: 256m
    heap_max: 512m
    gc: G1GC
```

### Database Connection Pools

| Service             | Min Connections | Max Connections |
| ------------------- | --------------- | --------------- |
| Gateway             | N/A             | N/A             |
| Auth Service        | 5               | 20              |
| Book Service        | 10              | 50              |
| Circulation Service | 10              | 50              |
| Reader Service      | 5               | 30              |

### Redis Connection Pools

| Service      | Max Active | Max Idle | Min Idle |
| ------------ | ---------- | -------- | -------- |
| All Services | 50         | 20       | 5        |

---

## Test Scenarios

### Load Test

| Parameter          | Value                        |
| ------------------ | ---------------------------- |
| Virtual Users      | 100                          |
| Ramp-up Time       | 2 minutes                    |
| Sustained Duration | 5 minutes                    |
| Ramp-down Time     | 1 minute                     |
| Pass Criteria      | P95 < 500ms, Error Rate < 1% |

### Stress Test

| Parameter     | Value                                             |
| ------------- | ------------------------------------------------- |
| Maximum VUs   | 500                                               |
| Ramp Pattern  | Step-wise (50 -> 100 -> 200 -> 300 -> 400 -> 500) |
| Step Duration | 3 minutes each                                    |
| Pass Criteria | P95 < 3s at 300 VUs, graceful degradation         |

### Spike Test

| Parameter      | Value                                   |
| -------------- | --------------------------------------- |
| Baseline VUs   | 50                                      |
| Spike VUs      | 1000                                    |
| Spike Duration | 3 minutes                               |
| Recovery Time  | < 2 minutes                             |
| Pass Criteria  | System recovers to baseline performance |

### Soak Test

| Parameter     | Value                            |
| ------------- | -------------------------------- |
| Virtual Users | 50                               |
| Duration      | 1 hour                           |
| Pass Criteria | No performance degradation > 20% |

---

## Monitoring & Alerting

### Key Metrics to Monitor

```yaml
metrics:
  # Response Time
  - name: http_request_duration_seconds
    type: histogram
    labels: [service, endpoint, method, status]

  # Throughput
  - name: http_requests_total
    type: counter
    labels: [service, endpoint, method, status]

  # Error Rate
  - name: http_request_errors_total
    type: counter
    labels: [service, endpoint, error_type]

  # Active Connections
  - name: active_connections
    type: gauge
    labels: [service]

  # Database Metrics
  - name: db_connection_pool_active
    type: gauge
    labels: [service, pool]

  # JVM Metrics
  - name: jvm_memory_used_bytes
    type: gauge
    labels: [service, area]
```

### Alert Rules

```yaml
alerts:
  # Response Time Alert
  - name: HighResponseTime
    condition: http_request_duration_p95 > 500ms
    duration: 5m
    severity: warning

  - name: CriticalResponseTime
    condition: http_request_duration_p95 > 1000ms
    duration: 2m
    severity: critical

  # Error Rate Alert
  - name: HighErrorRate
    condition: http_request_error_rate > 1%
    duration: 5m
    severity: warning

  - name: CriticalErrorRate
    condition: http_request_error_rate > 5%
    duration: 2m
    severity: critical

  # Resource Alerts
  - name: HighCPUUsage
    condition: cpu_usage > 80%
    duration: 10m
    severity: warning

  - name: HighMemoryUsage
    condition: memory_usage > 85%
    duration: 5m
    severity: warning
```

---

## Performance Budgets

### Page Load Budget (Frontend)

| Metric                         | Budget  |
| ------------------------------ | ------- |
| First Contentful Paint (FCP)   | < 1.5s  |
| Largest Contentful Paint (LCP) | < 2.5s  |
| First Input Delay (FID)        | < 100ms |
| Cumulative Layout Shift (CLS)  | < 0.1   |
| Time to Interactive (TTI)      | < 3.5s  |
| Total Blocking Time (TBT)      | < 200ms |

### API Response Budget

| Operation Type        | Budget |
| --------------------- | ------ |
| Simple Read           | 200ms  |
| Complex Read (Search) | 500ms  |
| Write Operation       | 500ms  |
| Transaction           | 1000ms |
| Batch Operation       | 2000ms |

### Resource Budget

| Resource             | Budget per Service |
| -------------------- | ------------------ |
| CPU                  | 2 cores            |
| Memory               | 2 GB               |
| Database Connections | 50                 |
| Redis Connections    | 50                 |

---

## Regression Detection

### Automated Checks

```yaml
regression_detection:
  # Compare against baseline
  baseline_comparison:
    enabled: true
    threshold: 20% # Alert if >20% worse than baseline

  # Compare against previous release
  release_comparison:
    enabled: true
    threshold: 10% # Alert if >10% worse than previous release

  # Statistical significance
  statistical_analysis:
    enabled: true
    confidence_level: 95%
    min_samples: 100
```

### CI/CD Integration

```yaml
# Pipeline performance gate
performance_gate:
  enabled: true
  test_type: load
  fail_on:
    - p95_response_time > 500ms
    - error_rate > 1%
    - throughput_degradation > 20%
  blocking: true # Block deployment on failure
```

### Regression Response Process

1. **Detection**: Automated alerting on regression
2. **Analysis**: Compare metrics with baseline
3. **Isolation**: Identify regression source
4. **Resolution**: Fix or rollback
5. **Verification**: Re-run performance tests
6. **Documentation**: Update baseline if intentional change

---

## Appendix

### Environment Specifications

#### Production Environment

| Component            | Specification                           |
| -------------------- | --------------------------------------- |
| API Gateway          | 2 instances, 2 CPU, 2GB RAM each        |
| Application Services | 2 instances each, 2 CPU, 2GB RAM        |
| PostgreSQL           | Primary + Read Replica, 4 CPU, 16GB RAM |
| Redis                | Sentinel cluster, 2 CPU, 4GB RAM        |

#### Staging Environment

| Component            | Specification                   |
| -------------------- | ------------------------------- |
| API Gateway          | 1 instance, 1 CPU, 1GB RAM      |
| Application Services | 1 instance each, 1 CPU, 1GB RAM |
| PostgreSQL           | Single instance, 2 CPU, 8GB RAM |
| Redis                | Single instance, 1 CPU, 2GB RAM |

### Related Documents

- [Architecture Documentation](/docs/architecture/architect.md)
- [Monitoring Setup Guide](/deployment/monitoring/README.md)
- [k6 Test Scripts](/deployment/performance/)
- [Grafana Dashboards](/deployment/monitoring/grafana/dashboards/)

### Version History

| Version | Date       | Changes                   |
| ------- | ---------- | ------------------------- |
| 1.0.0   | 2025-12-01 | Initial baseline document |

---

**Document Owner**: Performance Engineering Team
**Review Cycle**: Quarterly
**Next Review**: 2026-03-01
