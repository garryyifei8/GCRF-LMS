# GCRF Library Management System - Performance Testing

Performance testing suite using k6 for load, stress, spike, and soak testing.

## Overview

This directory contains k6 performance test scripts for the GCRF Library Management System. The tests cover various scenarios to validate system performance, identify bottlenecks, and establish performance baselines.

## Test Types

| Test Type | Description | VUs | Duration |
|-----------|-------------|-----|----------|
| **Load Test** | Basic load test simulating normal traffic | 100 | ~8 min |
| **Stress Test** | Find system breaking point | Up to 500 | ~31 min |
| **Spike Test** | Test sudden traffic bursts | Up to 1000 | ~14 min |
| **Soak Test** | Long-term stability test | 50 | 1 hour |

## Quick Start

### Prerequisites

1. **Install k6**:
   ```bash
   # macOS
   brew install k6

   # Linux (Debian/Ubuntu)
   sudo gpg -k
   sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
     --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
   echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | \
     sudo tee /etc/apt/sources.list.d/k6.list
   sudo apt-get update && sudo apt-get install k6

   # Docker
   docker pull grafana/k6
   ```

2. **Ensure services are running**:
   ```bash
   # Start the GCRF services
   cd deployment
   docker-compose -f docker-compose.infrastructure.yml up -d
   docker-compose -f docker-compose.services.yml up -d
   ```

### Running Tests

#### Using the Script Runner (Recommended)

```bash
# Run load test
./deployment/scripts/run-performance-test.sh load

# Run stress test against custom URL
./deployment/scripts/run-performance-test.sh stress -u https://api.example.com

# Run load test with InfluxDB output and HTML report
./deployment/scripts/run-performance-test.sh load -i http://localhost:8086/k6 -r
```

#### Using k6 Directly

```bash
cd deployment/performance

# Basic load test
k6 run load-test.js

# With custom base URL
K6_BASE_URL=http://localhost:8080 k6 run load-test.js

# With InfluxDB output
k6 run --out influxdb=http://localhost:8086/k6 load-test.js

# With JSON output
k6 run --out json=results.json load-test.js
```

#### Using Docker Compose

```bash
cd deployment

# Start InfluxDB
docker-compose -f docker-compose.k6.yml up -d influxdb

# Run load test
docker-compose -f docker-compose.k6.yml --profile load-test up k6-load

# Run stress test
docker-compose -f docker-compose.k6.yml --profile stress-test up k6-stress

# Run spike test
docker-compose -f docker-compose.k6.yml --profile spike-test up k6-spike

# Run soak test
docker-compose -f docker-compose.k6.yml --profile soak-test up k6-soak
```

## Test Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `K6_BASE_URL` | Base URL of the API gateway | `http://localhost:8080` |
| `K6_ADMIN_USER` | Admin username | `admin` |
| `K6_ADMIN_PASS` | Admin password | `admin123` |
| `K6_LIBRARIAN_USER` | Librarian username | `librarian` |
| `K6_LIBRARIAN_PASS` | Librarian password | `librarian123` |
| `K6_READER_USER` | Reader username | `reader` |
| `K6_READER_PASS` | Reader password | `reader123` |
| `K6_ENVIRONMENT` | Environment tag | `dev` |
| `K6_SOAK_DURATION` | Soak test duration (minutes) | `60` |

### Test Scenarios

Each test covers the following API endpoints:

- **Authentication**: `POST /api/v1/auth/login`
- **Book Operations**:
  - `GET /api/v1/books` (paginated list)
  - `GET /api/v1/books/{id}` (detail view)
  - `GET /api/v1/books/search` (search with filters)
- **Circulation**:
  - `POST /api/v1/circulation/borrow`
  - `POST /api/v1/circulation/return`
- **Reader Operations**:
  - `GET /api/v1/readers` (paginated list)
  - `GET /api/v1/readers/{id}` (detail view)

## Performance Thresholds

### SLO Targets

| Metric | Target |
|--------|--------|
| P95 Response Time | < 500ms |
| P99 Response Time | < 1000ms |
| Error Rate | < 1% |
| Throughput | 500 TPS |

### Per-Endpoint Targets

| Endpoint | P95 Target |
|----------|------------|
| Login | < 500ms |
| Book List | < 500ms |
| Book Detail | < 300ms |
| Book Search | < 800ms |
| Borrow/Return | < 1000ms |
| Reader List | < 500ms |

## Monitoring Results

### Grafana Dashboard

1. Start the monitoring stack:
   ```bash
   docker-compose -f docker-compose.monitoring.yml up -d
   docker-compose -f docker-compose.k6.yml up -d influxdb
   ```

2. Access Grafana at `http://localhost:3000` (admin/admin)

3. Navigate to **GCRF Performance Testing** dashboard

### Results Analysis

Test results are saved to:

- `deployment/performance/results/` - Test output directory
- `results/<test-type>_<timestamp>/` - Per-test results
  - `console.log` - Console output
  - `results.json` - Detailed metrics
  - `summary.json` - Summary statistics
  - `report.html` - HTML report (if `-r` flag used)

## File Structure

```
deployment/performance/
|-- config.js          # Central configuration
|-- helpers.js         # Common helper functions
|-- load-test.js       # Load test (100 VUs, 5 min)
|-- stress-test.js     # Stress test (up to 500 VUs)
|-- spike-test.js      # Spike test (up to 1000 VUs)
|-- soak-test.js       # Soak test (50 VUs, 1 hour)
|-- README.md          # This file
|-- results/           # Test results directory
```

## Customization

### Modifying Test Parameters

Edit the `options` object in each test file:

```javascript
export const options = {
    stages: [
        { duration: '1m', target: 50 },   // Modify VUs and duration
        { duration: '5m', target: 100 },
        { duration: '1m', target: 0 }
    ],
    thresholds: {
        'http_req_duration': ['p(95)<500'],  // Modify thresholds
        'http_req_failed': ['rate<0.01']
    }
};
```

### Adding Custom Scenarios

1. Create a new test file (e.g., `custom-test.js`)
2. Import helpers: `import { login, queryBooks } from './helpers.js'`
3. Define test options and scenarios
4. Run: `k6 run custom-test.js`

### Adding Custom Metrics

```javascript
import { Trend, Counter } from 'k6/metrics';

const customDuration = new Trend('custom_duration', true);
const customCounter = new Counter('custom_counter');

// In your test function:
const start = Date.now();
// ... operation
customDuration.add(Date.now() - start);
customCounter.add(1);
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Performance Tests
on:
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM
  workflow_dispatch:

jobs:
  performance-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Install k6
        run: |
          sudo gpg -k
          sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
            --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
          echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | \
            sudo tee /etc/apt/sources.list.d/k6.list
          sudo apt-get update && sudo apt-get install k6

      - name: Run Load Test
        run: |
          cd deployment/performance
          k6 run --summary-export=summary.json load-test.js
        env:
          K6_BASE_URL: ${{ secrets.API_URL }}

      - name: Check Thresholds
        run: |
          # Fail if P95 > 500ms or error rate > 1%
          jq -e '.metrics.http_req_duration.values["p(95)"] < 500' summary.json
          jq -e '.metrics.http_req_failed.values.rate < 0.01' summary.json

      - name: Upload Results
        uses: actions/upload-artifact@v4
        with:
          name: performance-results
          path: deployment/performance/summary.json
```

## Troubleshooting

### Common Issues

1. **Connection refused**
   - Ensure services are running
   - Check firewall settings
   - Verify BASE_URL is correct

2. **Authentication failures**
   - Verify test credentials exist
   - Check JWT token expiration
   - Review auth service logs

3. **High error rates**
   - Check service health
   - Review application logs
   - Monitor resource utilization

4. **InfluxDB not receiving data**
   - Verify InfluxDB is running
   - Check network connectivity
   - Confirm database `k6` exists

### Debug Mode

```bash
# Run with verbose output
k6 run --http-debug=full load-test.js

# Run with console output
k6 run --console-output=console.log load-test.js
```

## Related Documentation

- [Performance Baseline](/doc/performance/performance-baseline.md)
- [Grafana Dashboard](/deployment/monitoring/grafana/dashboards/performance-dashboard.json)
- [k6 Documentation](https://k6.io/docs/)

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-12-01 | Initial performance testing suite |
