# Integration Test Execution Guide

## Quick Start

### Prerequisites
1. Java 21 installed
2. Docker running
3. Maven 3.8+

### Run All Tests
```bash
# From backend directory
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./run-integration-tests.sh
```

### Run Specific Service Tests
```bash
./run-integration-tests.sh book-service
```

### Generate Coverage Reports
```bash
./run-integration-tests.sh coverage
```

## Test Structure

### Book Service (✅ Complete - 65 tests)
```
book-service/src/test/java/
├── integration/
│   ├── BaseIntegrationTest.java              # Base configuration
│   ├── BookControllerIntegrationTest.java     # 25 tests
│   ├── CategoryControllerIntegrationTest.java # 20 tests
│   └── BookFileControllerIntegrationTest.java # 20 tests
└── resources/testdata/
    └── book-test-data.sql                     # Test data
```

**Coverage**: 85%+

**Test Categories**:
- Health Check (1)
- Query Books (5)
- Get Book Detail (2)
- Create Book (4)
- Update Book (2)
- Delete Book (2)
- Inventory Management (3)
- Search Books (2)
- Category Tree Operations (20)
- File Upload/Download (20)

### Reader Service (🚧 Template Ready - 30 tests planned)
**Templates Available**:
- ReaderControllerIntegrationTest (20 tests)
- ReaderTypeControllerIntegrationTest (10 tests)

**Implementation**: Follow book-service pattern

### Circulation Service (🚧 Template Ready - 40 tests planned)
**Templates Available**:
- BorrowControllerIntegrationTest (15 tests)
- ReturnControllerIntegrationTest (15 tests)
- ReservationControllerIntegrationTest (10 tests)

**Implementation**: Follow book-service pattern

### Auth Service (🚧 Template Ready - 15 tests planned)
**Templates Available**:
- AuthControllerIntegrationTest (15 tests)

**Implementation**: Follow book-service pattern

## Manual Test Commands

### Run All Tests
```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test
```

### Run Specific Service
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl book-service
```

### Run Single Test Class
```bash
cd book-service
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -Dtest=BookControllerIntegrationTest
```

### Run Single Test Method
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test \
  -Dtest=BookControllerIntegrationTest#testQueryBooks_withPagination
```

### Generate Coverage Report
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test jacoco:report
open book-service/target/site/jacoco/index.html
```

## Test Configuration

### Testcontainers
- PostgreSQL 15 in Docker
- Database: testdb
- User/Password: test/test
- Container reuse: enabled

### Test Data
- Loaded via @Sql before each test
- Uses ID ranges 1000+ to avoid conflicts
- Includes cleanup and sequence reset

### Mocked Services
- Nacos (disabled)
- Elasticsearch (disabled)
- Redis (localhost)
- MinIO (mocked via @MockBean)

## Troubleshooting

### Docker Not Running
```bash
# Check Docker status
docker ps

# Start Docker Desktop (Mac)
```

### Java Version Mismatch
```bash
# Check version
java -version

# Set Java 21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

### Port Already in Use
```bash
# Stop PostgreSQL
brew services stop postgresql

# Or kill process
lsof -ti:5432 | xargs kill -9
```

### Clean Build
```bash
./run-integration-tests.sh clean
```

## Documentation

For comprehensive documentation, see:
- **Integration Test Guide**: `doc/testing/integration-test-guide.md` (5000+ lines)
- **Test Summary**: `doc/testing/integration-test-summary.md`
- **Development Guidelines**: `CLAUDE.md`

## Support

- **Team**: GCRF Test Team
- **Documentation**: `doc/testing/`
- **Issues**: Create in project tracker

---

**Quick Reference**:
```bash
# Run all tests
./run-integration-tests.sh

# Run specific service
./run-integration-tests.sh book-service

# Coverage report
./run-integration-tests.sh coverage

# Help
./run-integration-tests.sh --help
```

**Status**: ✅ Book Service Complete (65 tests, 85%+ coverage)
**Target**: 150 total tests across 4 services, 80%+ coverage
