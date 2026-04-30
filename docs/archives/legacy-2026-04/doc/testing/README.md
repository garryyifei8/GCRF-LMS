# GCRF Library Management System - Testing Framework

**Version**: 1.0
**Last Updated**: 2025-12-01
**Status**: Production Ready

---

## Overview

This directory contains comprehensive testing resources for the GCRF Library Management System, including:

- **E2E Test Scripts**: Automated end-to-end testing of all services
- **API Smoke Tests**: Quick validation of API endpoints
- **Frontend E2E Tests**: Browser-based UI testing with Playwright
- **Test Data Management**: Scripts to prepare and manage test data
- **Test Documentation**: Complete test case specifications

---

## Quick Start

### Prerequisites

```bash
# System requirements
- Docker & Docker Compose (for running services)
- Node.js 18+ (for frontend tests)
- PostgreSQL 15+ (for test database)
- Bash 4.0+ (for shell scripts)
- curl (for API testing)

# Install Playwright (for frontend E2E tests)
cd /path/to/web-admin
npm install
npx playwright install
```

### Running Tests

#### 1. Complete E2E Test Suite

```bash
# Start all services first
cd deployment
docker-compose up -d

# Wait for services to be healthy
./scripts/wait-for-healthy.sh

# Prepare test data
./scripts/prepare-test-data.sh --clean --full

# Run E2E tests
./scripts/e2e-test.sh --full --report
```

#### 2. Quick Smoke Tests (5 minutes)

```bash
# Quick validation of all services
./scripts/api-smoke-test.sh --verbose

# Or run quick E2E tests
./scripts/e2e-test.sh --quick
```

#### 3. Frontend E2E Tests

```bash
cd web-admin

# Run all tests
npm run test:e2e

# Run with UI mode (interactive)
npm run test:e2e:ui

# Run specific test file
npx playwright test e2e/auth.spec.ts

# Debug mode
npm run test:e2e:debug
```

---

## Test Scripts Reference

### 1. E2E Test Script (`deployment/scripts/e2e-test.sh`)

**Purpose**: Comprehensive end-to-end testing of all business flows

**Usage**:

```bash
./e2e-test.sh [options]

Options:
  --quick       Run quick smoke tests (10-15 min)
  --full        Run full E2E test suite (30-60 min)
  --load        Include load testing
  --report      Generate HTML test report
  --env <env>   Environment (dev|prod)
```

**Test Coverage**:

- Service health checks
- Authentication flow (admin, reader, invalid)
- Book management (CRUD operations)
- Reader management (registration, profile)
- Circulation flow (borrow, return, renew)
- Reservation flow (create, cancel, fulfill)
- Statistics and reports

**Output**:

- HTML Report: `deployment/test-reports/e2e-report-*.html`
- JSON Report: `deployment/test-reports/e2e-results-*.json`
- Log File: `deployment/test-reports/e2e-test-*.log`

**Examples**:

```bash
# Development environment
./e2e-test.sh --full --report

# Production validation
./e2e-test.sh --env prod --quick

# With load testing
./e2e-test.sh --full --load
```

---

### 2. API Smoke Test Script (`deployment/scripts/api-smoke-test.sh`)

**Purpose**: Fast validation of all API endpoints using curl

**Usage**:

```bash
./api-smoke-test.sh [options]

Options:
  --gateway <url>   Gateway URL (default: http://localhost:8080)
  --verbose         Show detailed output
  --json            Output results as JSON
```

**Test Coverage**:

- Gateway health check
- Service health checks (auth, book, reader, circulation)
- Authentication (login, token validation, logout)
- Book operations (list, search, create, details)
- Reader operations (register, login, profile)
- Circulation operations (borrow, return, records)
- Authorization checks

**Output**:

- Console summary with pass/fail counts
- JSON output (with --json flag)

**Examples**:

```bash
# Quick smoke test
./api-smoke-test.sh

# Verbose mode with details
./api-smoke-test.sh --verbose

# Test production environment
./api-smoke-test.sh --gateway https://api.gcrf.com --json > results.json
```

---

### 3. Test Data Preparation Script (`deployment/scripts/prepare-test-data.sh`)

**Purpose**: Prepare comprehensive test data for all testing scenarios

**Usage**:

```bash
./prepare-test-data.sh [options]

Options:
  --clean       Clean existing test data first
  --minimal     Import minimal dataset (3 readers, 10 books)
  --full        Import full dataset (10 readers, 50 books)
  --gateway     Gateway URL
```

**What It Creates**:

- **Test Users**:
  - `admin` / `admin123` (existing admin)
  - `reader001` / `reader123` (test reader)
  - `maxborrow` / `Test123456` (reader with max borrows)
  - `testuser_*` / `Test123456` (10 additional readers)

- **Sample Books**: 10-50 books across different categories
- **Borrow Records**: 5+ active borrow scenarios
- **Overdue Records**: 3+ overdue scenarios (for fine testing)
- **Reservations**: 3+ reservation scenarios
- **Categories**: 5 main book categories

**Database Impact**:

- Inserts data into: `reader_service`, `book_service`, `circulation_service`
- Can clean previous test data with `--clean`

**Examples**:

```bash
# Fresh start with full dataset
./prepare-test-data.sh --clean --full

# Quick minimal dataset
./prepare-test-data.sh --minimal

# Production environment (careful!)
./prepare-test-data.sh --gateway https://prod.gcrf.com --minimal
```

---

## Frontend E2E Tests (Playwright)

### Configuration

**File**: `web-admin/playwright.config.ts`

**Key Settings**:

- Base URL: `http://localhost:3011`
- Browsers: Chromium, Firefox, WebKit
- Mobile: Pixel 5, iPhone 12
- Reporters: HTML, JSON, JUnit
- Screenshots: On failure
- Video: On failure
- Traces: On first retry

### Test Files

#### `e2e/auth.spec.ts` - Authentication Tests

- Login page display
- Admin/reader login
- Invalid credentials
- Password visibility toggle
- Session persistence
- Logout functionality
- Unauthorized access protection

#### `e2e/books.spec.ts` - Book Management Tests

- Books list display
- Search and filter
- Create new book
- Edit book details
- Delete book
- Pagination
- Category management

#### `e2e/circulation.spec.ts` - Circulation Tests

- Borrow book workflow
- Return book workflow
- Renew book
- Borrow limit validation
- Overdue fines
- View borrow records
- Fine management
- Reservation management

### Running Frontend Tests

```bash
cd web-admin

# Install dependencies (first time)
npm install
npx playwright install

# Run all tests
npm run test:e2e

# Run specific test file
npx playwright test e2e/auth.spec.ts

# Run specific test by name
npx playwright test -g "should login successfully"

# Interactive UI mode
npm run test:e2e:ui

# Debug mode (step through tests)
npm run test:e2e:debug

# Run on specific browser
npx playwright test --project=chromium
npx playwright test --project=firefox
npx playwright test --project=webkit

# View last test report
npm run test:e2e:report
```

### Test Results

Reports are generated in:

- HTML Report: `web-admin/playwright-report/index.html`
- JSON Results: `web-admin/test-results/results.json`
- JUnit XML: `web-admin/test-results/junit.xml` (for CI integration)

---

## Test Case Documentation

### File: `doc/testing/e2e-test-cases.md`

Comprehensive test case specifications including:

1. **Core Business Flows**
   - Authentication (TC-AUTH-001 to TC-AUTH-006)
   - Book Management (TC-BOOK-001 to TC-BOOK-008)
   - Reader Management (TC-READER-001 to TC-READER-007)
   - Circulation (TC-CIRC-001 to TC-CIRC-007)
   - Reservations (TC-RESV-001 to TC-RESV-005)
   - Statistics (TC-STAT-001 to TC-STAT-004)

2. **Integration Tests**
   - Cross-service authentication
   - Transaction consistency
   - Service failure handling

3. **Frontend UI Tests**
   - Login page (TC-UI-001)
   - Book management (TC-UI-002)
   - Circulation flow (TC-UI-003)
   - Search functionality (TC-UI-004)

4. **Performance Tests**
   - Concurrent login load test (TC-PERF-001)
   - Search under load (TC-PERF-002)
   - Concurrent borrow operations (TC-PERF-003)

5. **Security Tests**
   - SQL injection prevention (TC-SEC-001)
   - XSS prevention (TC-SEC-002)
   - Unauthorized access (TC-SEC-003)

6. **Data Integrity Tests**
   - Transaction rollback (TC-DATA-001)
   - Concurrent update handling (TC-DATA-002)

Each test case includes:

- Level (L1-L4: Smoke, Functional, Integration, Performance)
- Priority (Critical, High, Medium, Low)
- Preconditions
- Detailed test steps
- Expected results
- Acceptance criteria

---

## Continuous Integration

### GitLab CI Example

```yaml
# .gitlab-ci.yml
stages:
  - test

e2e_tests:
  stage: test
  image: mcr.microsoft.com/playwright:v1.40.0
  services:
    - postgres:15
    - redis:7
  script:
    # Start services
    - docker-compose up -d
    - ./deployment/scripts/wait-for-healthy.sh

    # Prepare test data
    - ./deployment/scripts/prepare-test-data.sh --clean --minimal

    # Run API tests
    - ./deployment/scripts/api-smoke-test.sh --json > api-results.json

    # Run E2E tests
    - ./deployment/scripts/e2e-test.sh --full --report

    # Run frontend tests
    - cd web-admin
    - npm install
    - npm run test:e2e

  artifacts:
    reports:
      junit:
        - deployment/test-reports/junit-*.xml
        - web-admin/test-results/junit.xml
    paths:
      - deployment/test-reports/
      - web-admin/playwright-report/
      - api-results.json
    expire_in: 30 days

  only:
    - merge_requests
    - main
```

### GitHub Actions Example

```yaml
# .github/workflows/e2e-tests.yml
name: E2E Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: gcrf_secure_2024
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      redis:
        image: redis:7
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: "18"

      - name: Install Playwright
        run: |
          cd web-admin
          npm install
          npx playwright install --with-deps

      - name: Start Services
        run: |
          docker-compose up -d
          ./deployment/scripts/wait-for-healthy.sh

      - name: Prepare Test Data
        run: ./deployment/scripts/prepare-test-data.sh --clean --minimal

      - name: Run API Smoke Tests
        run: ./deployment/scripts/api-smoke-test.sh --verbose

      - name: Run E2E Tests
        run: ./deployment/scripts/e2e-test.sh --full --report

      - name: Run Frontend E2E Tests
        run: |
          cd web-admin
          npm run test:e2e

      - name: Upload Test Reports
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-reports
          path: |
            deployment/test-reports/
            web-admin/playwright-report/
          retention-days: 30
```

---

## Best Practices

### 1. Test Isolation

- Each test should be independent
- Use `--clean` flag to reset test data
- Tests should not depend on execution order

### 2. Test Data Management

- Use `prepare-test-data.sh` before test runs
- Create specific test accounts (don't reuse production data)
- Clean up test data after runs

### 3. Test Stability

- Use explicit waits (not sleep)
- Handle async operations properly
- Implement retry logic for flaky tests

### 4. Performance

- Run quick smoke tests frequently
- Run full E2E tests nightly
- Run load tests weekly

### 5. Debugging Failed Tests

```bash
# For shell scripts - run with verbose
./e2e-test.sh --full --verbose 2>&1 | tee debug.log

# For Playwright - use debug mode
cd web-admin
npm run test:e2e:debug

# View trace of failed test
npx playwright show-trace test-results/path-to-trace.zip
```

---

## Troubleshooting

### Issue: Services not healthy

**Solution**:

```bash
# Check service status
docker-compose ps

# View service logs
docker-compose logs gateway-service
docker-compose logs auth-service

# Restart services
docker-compose restart

# Wait for health
./deployment/scripts/wait-for-healthy.sh
```

### Issue: Test data creation fails

**Solution**:

```bash
# Check database connectivity
psql -h localhost -U postgres -d reader_service -c "SELECT 1;"

# Check admin login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Clean and retry
./prepare-test-data.sh --clean --minimal
```

### Issue: Frontend tests fail to start

**Solution**:

```bash
cd web-admin

# Reinstall dependencies
rm -rf node_modules package-lock.json
npm install

# Reinstall Playwright browsers
npx playwright install --with-deps

# Check if dev server starts
npm run dev
# Open http://localhost:3011 in browser
```

### Issue: Flaky tests (random failures)

**Solution**:

```bash
# Increase timeouts in playwright.config.ts
timeout: 90 * 1000  # Increase to 90 seconds

# Run specific test multiple times
npx playwright test e2e/auth.spec.ts --repeat-each=5

# Enable retry on failure
retries: 2  # in playwright.config.ts
```

---

## Test Maintenance

### Adding New Test Cases

1. **Backend API Tests**: Add to `e2e-test.sh`

   ```bash
   test_new_feature() {
       log_info "====== Phase X: New Feature Tests ======"
       # Add test logic
   }
   ```

2. **Frontend Tests**: Create new spec file

   ```bash
   touch web-admin/e2e/new-feature.spec.ts
   # Follow existing test patterns
   ```

3. **Update Documentation**: Add to `e2e-test-cases.md`

### Updating Test Data

Edit `prepare-test-data.sh` to add/modify:

- User accounts
- Sample books
- Test scenarios

### Performance Benchmarking

Track test execution times:

```bash
# Baseline measurement
time ./e2e-test.sh --full > baseline.log

# Compare after changes
time ./e2e-test.sh --full > updated.log

# Analyze differences
diff baseline.log updated.log
```

---

## Support & Contact

**Team**: QA & DevOps Team
**Slack**: #gcrf-testing
**Email**: qa@gcrf.com

**Documentation Updates**: Submit PR to update test documentation
**Bug Reports**: File issue with `testing` label
**Feature Requests**: Discuss in #gcrf-testing channel

---

## References

- [Playwright Documentation](https://playwright.dev/docs/intro)
- [PostgreSQL Testing Best Practices](https://www.postgresql.org/docs/current/regress.html)
- [API Testing with curl](https://everything.curl.dev/)
- [Shell Script Best Practices](https://google.github.io/styleguide/shellguide.html)

---

**Last Updated**: 2025-12-01
**Next Review**: 2026-01-01
