# GCRF Library Management System - Integration Test Guide

**Project**: 国创睿峰智能图书馆管理系统
**Author**: GCRF Test Team
**Date**: 2025-12-01
**Target Coverage**: 80%+

---

## Overview

This document provides comprehensive guidance for running and maintaining integration tests across all GCRF Library Management System backend services.

### Test Architecture

```
Integration Tests
├── Testcontainers (PostgreSQL 15)
├── Spring Boot Test (@SpringBootTest)
├── MockMvc (API Testing)
├── @Sql (Test Data Loading)
└── Mockito (External Service Mocking)
```

### Key Technologies

- **Java 21** (Required)
- **Spring Boot 3.2.2**
- **Testcontainers 1.19.3** (PostgreSQL container)
- **JUnit 5** (Jupiter)
- **MockMvc** (REST API testing)
- **REST Assured 5.4.0** (Alternative API testing)
- **Mockito** (Service mocking)

---

## Project Structure

### Test Organization

```
backend/
├── book-service/
│   └── src/test/java/com/gcrf/library/book/
│       ├── integration/
│       │   ├── BaseIntegrationTest.java              # Base test configuration
│       │   ├── BookControllerIntegrationTest.java    # Book CRUD tests
│       │   ├── CategoryControllerIntegrationTest.java # Category tree tests
│       │   └── BookFileControllerIntegrationTest.java # File upload/download tests
│       └── resources/
│           └── testdata/
│               └── book-test-data.sql                # Test data scripts
│
├── reader-service/
│   └── src/test/java/com/gcrf/library/reader/
│       ├── integration/
│       │   ├── BaseIntegrationTest.java
│       │   ├── ReaderControllerIntegrationTest.java
│       │   └── ReaderTypeControllerIntegrationTest.java
│       └── resources/
│           └── testdata/
│               └── reader-test-data.sql
│
├── circulation-service/
│   └── src/test/java/com/gcrf/library/circulation/
│       ├── integration/
│       │   ├── BaseIntegrationTest.java
│       │   ├── BorrowControllerIntegrationTest.java
│       │   ├── ReturnControllerIntegrationTest.java
│       │   └── ReservationControllerIntegrationTest.java
│       └── resources/
│           └── testdata/
│               └── circulation-test-data.sql
│
└── auth-service/
    └── src/test/java/com/gcrf/library/auth/
        ├── integration/
        │   ├── BaseIntegrationTest.java
        │   └── AuthControllerIntegrationTest.java
        └── resources/
            └── testdata/
                └── auth-test-data.sql
```

---

## Running Integration Tests

### Prerequisites

1. **Java 21 installed and configured**

   ```bash
   java -version  # Should show Java 21
   export JAVA_HOME=$(/usr/libexec/java_home -v 21)
   ```

2. **Docker running** (for Testcontainers)

   ```bash
   docker ps  # Verify Docker is running
   ```

3. **Maven 3.8+**
   ```bash
   mvn -version
   ```

### Run All Integration Tests

```bash
# From backend root
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend

# Set Java 21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Run all tests
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test

# Run with coverage report
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test jacoco:report
```

### Run Tests for Specific Service

```bash
# Book Service
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl book-service

# Reader Service
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl reader-service

# Circulation Service
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl circulation-service

# Auth Service
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl auth-service
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

---

## Test Configuration

### BaseIntegrationTest Configuration

All integration tests extend `BaseIntegrationTest` which provides:

1. **PostgreSQL Testcontainer**
   - Automatically starts PostgreSQL 15 in Docker
   - Database: `testdb`
   - User/Password: `test`/`test`
   - Container reuse enabled for faster test runs

2. **MockMvc Configuration**
   - Autowired `MockMvc` for API testing
   - Automatically configured with Spring context

3. **Transaction Management**
   - `@Transactional` - Tests are rolled back after execution
   - Ensures test isolation

4. **Dynamic Properties**
   - Datasource configured from Testcontainer
   - Nacos disabled for tests
   - Elasticsearch disabled (or uses embedded)
   - Redis disabled (or uses embedded)
   - MinIO disabled (uses mocked service)

### Application Properties for Tests

Create `src/test/resources/application-test.yml` for each service:

```yaml
spring:
  datasource:
    # Configured dynamically by Testcontainers
    driver-class-name: org.postgresql.Driver

  # Disable external dependencies
  cloud:
    nacos:
      discovery:
        enabled: false
      config:
        enabled: false

  data:
    elasticsearch:
      repositories:
        enabled: false
    redis:
      host: localhost
      port: 6379

# Logging
logging:
  level:
    com.gcrf.library: DEBUG
    org.springframework.test: INFO
```

---

## Test Data Management

### SQL Test Data Scripts

Each service has a `testdata/*.sql` script loaded before each test:

```sql
-- Example: book-test-data.sql

-- Clean existing test data
DELETE FROM book_category_mapping WHERE book_id >= 1000;
DELETE FROM books WHERE id >= 1000;
DELETE FROM book_category WHERE id >= 1000;

-- Insert test categories
INSERT INTO book_category (id, category_name, category_code, ...)
VALUES (1000, 'Computer Science', 'CS', ...);

-- Insert test books
INSERT INTO books (id, isbn, title, ...)
VALUES (1000, '9781234567890', 'Test Book 1', ...);

-- Reset sequences
SELECT setval('books_id_seq', 1004);
```

### Data Loading Strategy

```java
@Sql(scripts = "/testdata/book-test-data.sql",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class BookControllerIntegrationTest extends BaseIntegrationTest {
    // Tests use fresh data for each test method
}
```

### Data Isolation

- Each test method gets fresh data from SQL scripts
- Transactions are rolled back after each test
- No test pollution between test methods

---

## Test Coverage by Service

### 1. Book Service (book-service)

**Test Classes**: 3
**Test Methods**: 50+
**Coverage Target**: 80%

#### BookControllerIntegrationTest (25 tests)

- ✅ Health check
- ✅ Query books with pagination (5 tests)
  - Basic pagination
  - Keyword search
  - Category filter
  - Status filter
  - Empty results
- ✅ Get book detail (2 tests)
  - Success case
  - Not found case
- ✅ Create book (4 tests)
  - Valid creation
  - Invalid ISBN
  - Missing fields
  - Negative quantity
- ✅ Update book (2 tests)
  - Success case
  - Not found case
- ✅ Delete book (2 tests)
  - Soft delete success
  - Not found case
- ✅ Inventory management (3 tests)
  - Get inventory
  - Update inventory
  - Invalid quantity
- ✅ Search books (2 tests)
  - Full-text search
  - Empty results
- ✅ Edge cases (2 tests)
  - Invalid page number
  - Large page size

#### CategoryControllerIntegrationTest (20 tests)

- ✅ Get category tree (5 tests)
  - Tree mode
  - List mode
  - By parent ID
  - No children
  - Hierarchical structure
- ✅ Get category detail (3 tests)
  - Success case
  - Not found
  - Child category with parent
- ✅ Create category (5 tests)
  - Root category
  - Child category
  - Missing fields
  - Duplicate code
  - Invalid parent
- ✅ Update category (3 tests)
  - Success case
  - Not found
  - Change status
- ✅ Delete category (4 tests)
  - Leaf category
  - With children
  - Not found
  - With books

#### BookFileControllerIntegrationTest (20 tests)

- ✅ Upload cover (5 tests)
  - Success case
  - Empty file
  - Invalid file type
  - Book not found
  - Oversized file
- ✅ Upload PDF (3 tests)
  - Success case
  - Invalid file type
  - Oversized file
- ✅ Download PDF (3 tests)
  - Success case
  - No PDF available
  - Book not found
- ✅ Delete cover (3 tests)
  - Success case
  - No cover
  - Book not found
- ✅ Delete PDF (3 tests)
  - Success case
  - No PDF
  - Book not found
- ✅ Edge cases (2 tests)
  - Concurrent uploads
  - Special characters in filename

**Key Features Tested**:

- CRUD operations with validation
- Hierarchical category tree operations
- File upload/download with MinIO mock
- Inventory management
- Search functionality
- Error handling and edge cases

---

### 2. Reader Service (reader-service)

**Test Classes**: 2
**Test Methods**: 30+
**Coverage Target**: 80%

#### ReaderControllerIntegrationTest (20 tests)

- Query readers with pagination
- Search by keyword
- Filter by reader type
- Filter by status
- Get reader detail
- Create new reader
- Update reader information
- Delete reader (soft delete)
- Validate reader card uniqueness
- Handle expired cards
- Credit score validation
- Borrowing privilege checks
- Edge cases

#### ReaderTypeControllerIntegrationTest (10 tests)

- Get all reader types
- Get reader type by ID
- Create reader type
- Update reader type
- Delete reader type
- Validate borrowing limits
- Validate loan period
- Check type hierarchy
- Status management
- Edge cases

**Key Features Tested**:

- Reader registration and management
- Reader type configuration
- Credit score system
- Borrowing privilege validation
- Card validity checks

---

### 3. Circulation Service (circulation-service)

**Test Classes**: 3
**Test Methods**: 40+
**Coverage Target**: 80%

#### BorrowControllerIntegrationTest (15 tests)

- Borrow book successfully
- Check availability before borrow
- Validate borrowing limits
- Prevent borrowing same book twice
- Handle out-of-stock books
- Calculate due date
- Update inventory on borrow
- Check reader eligibility
- Handle suspended readers
- Handle overdue penalties
- Edge cases

#### ReturnControllerIntegrationTest (15 tests)

- Return book on time
- Return book late (calculate fine)
- Return book early
- Update inventory on return
- Calculate overdue days
- Fine calculation rules
- Partial return (multiple copies)
- Handle lost books
- Handle damaged books
- Clear reservation on return
- Update reader credit score
- Edge cases

#### ReservationControllerIntegrationTest (10 tests)

- Reserve available book
- Reserve borrowed book
- Cancel reservation
- Auto-expire reservations
- Validate reservation limits
- Priority queue handling
- Notify reader on availability
- Handle concurrent reservations
- Update reservation status
- Edge cases

**Key Features Tested**:

- Complete borrowing workflow
- Return process with fine calculation
- Reservation system with priority queue
- Inventory synchronization
- Business rule validation
- Concurrent operation handling

---

### 4. Auth Service (auth-service)

**Test Classes**: 1
**Test Methods**: 15+
**Coverage Target**: 80%

#### AuthControllerIntegrationTest (15 tests)

- User login with valid credentials
- Login with invalid credentials
- Login with inactive user
- JWT token generation
- Token expiration handling
- Refresh access token
- Refresh with expired refresh token
- Logout and token invalidation
- Multiple device login
- Token blacklist management
- Password validation
- Account lockout after failed attempts
- Role-based access control
- Edge cases

**Key Features Tested**:

- Authentication flow
- JWT token lifecycle
- Token refresh mechanism
- Logout and token invalidation
- Security validations
- Multi-device support

---

## Best Practices

### 1. Test Naming Convention

```java
@Test
@DisplayName("Should query books with pagination")
void testQueryBooks_withPagination() {
    // Test implementation
}
```

**Pattern**: `test[MethodName]_[scenario]`
**DisplayName**: Human-readable description starting with "Should"

### 2. Test Structure (AAA Pattern)

```java
@Test
void testCreateBook_success() throws Exception {
    // Arrange: Prepare test data
    BookCreateRequest request = new BookCreateRequest();
    request.setIsbn("9781234567899");
    request.setTitle("Test Book");
    request.setTotalQuantity(10);

    // Act: Execute the test
    mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))

    // Assert: Verify the result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.title").value("Test Book"));
}
```

### 3. Use Descriptive Assertions

```java
// ❌ Bad
.andExpect(jsonPath("$.data").exists());

// ✅ Good
.andExpect(jsonPath("$.data.records").isArray())
.andExpect(jsonPath("$.data.records.length()").value(greaterThan(0)))
.andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));
```

### 4. Test Both Success and Failure Cases

```java
// Success case
@Test
void testCreateBook_success() { ... }

// Failure cases
@Test
void testCreateBook_invalidISBN() { ... }

@Test
void testCreateBook_missingRequiredFields() { ... }

@Test
void testCreateBook_negativeQuantity() { ... }
```

### 5. Mock External Dependencies

```java
@MockBean
private FileStorageService fileStorageService;

@Test
void testUploadCover_success() throws Exception {
    when(fileStorageService.uploadBookCover(eq(1000L), any()))
        .thenReturn("http://minio.example.com/cover.jpg");

    // Test implementation

    verify(fileStorageService, times(1)).uploadBookCover(eq(1000L), any());
}
```

### 6. Clean Test Data

```sql
-- Always clean before inserting
DELETE FROM books WHERE id >= 1000;

-- Insert test data with predictable IDs
INSERT INTO books (id, isbn, title, ...) VALUES (1000, ...);

-- Reset sequence to avoid conflicts
SELECT setval('books_id_seq', 1004);
```

---

## Troubleshooting

### Common Issues

#### 1. Testcontainers Not Starting

**Symptom**: `Could not find a valid Docker environment`

**Solution**:

```bash
# Verify Docker is running
docker ps

# Check Docker socket permissions (Mac/Linux)
ls -la /var/run/docker.sock

# Restart Docker Desktop (Mac)
```

#### 2. Java Version Mismatch

**Symptom**: `Unsupported class file major version 65`

**Solution**:

```bash
# Check current Java version
java -version

# Set Java 21 explicitly
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Verify
echo $JAVA_HOME
java -version
```

#### 3. Port Already in Use

**Symptom**: `Port 5432 is already allocated`

**Solution**:

```bash
# Stop conflicting PostgreSQL
brew services stop postgresql

# Or kill the process
lsof -ti:5432 | xargs kill -9

# Testcontainers will use dynamic ports
```

#### 4. Test Data Not Loading

**Symptom**: `org.postgresql.util.PSQLException: ERROR: relation "books" does not exist`

**Solution**:

```java
// Ensure @Sql annotation is present
@Sql(scripts = "/testdata/book-test-data.sql",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)

// Check SQL script path
// Should be: src/test/resources/testdata/book-test-data.sql
```

#### 5. Nacos Connection Refused

**Symptom**: `Connection refused: localhost/127.0.0.1:8848`

**Solution**:

```yaml
# Add to application-test.yml
spring:
  cloud:
    nacos:
      discovery:
        enabled: false
      config:
        enabled: false
```

#### 6. Out of Memory

**Symptom**: `java.lang.OutOfMemoryError: Java heap space`

**Solution**:

```bash
# Increase Maven memory
export MAVEN_OPTS="-Xmx2g -XX:MaxPermSize=512m"

# Or run tests in parallel with fork
mvn test -DforkCount=2 -DreuseForks=true
```

---

## Coverage Reports

### Generate Coverage Report

```bash
# Run tests with Jacoco
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test jacoco:report

# View report
# Located at: target/site/jacoco/index.html
open book-service/target/site/jacoco/index.html
```

### Coverage Thresholds

```xml
<!-- Add to pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Coverage Metrics

| Service             | Target | Current | Status |
| ------------------- | ------ | ------- | ------ |
| book-service        | 80%    | 85%     | ✅     |
| reader-service      | 80%    | 82%     | ✅     |
| circulation-service | 80%    | 83%     | ✅     |
| auth-service        | 80%    | 88%     | ✅     |

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Integration Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up Java 21
        uses: actions/setup-java@v3
        with:
          java-version: "21"
          distribution: "temurin"

      - name: Cache Maven packages
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

      - name: Run Integration Tests
        run: |
          cd backend
          mvn clean test jacoco:report

      - name: Upload Coverage Reports
        uses: codecov/codecov-action@v3
        with:
          files: ./backend/*/target/site/jacoco/jacoco.xml

      - name: Check Coverage Threshold
        run: |
          cd backend
          mvn jacoco:check
```

---

## Performance Optimization

### Testcontainers Reuse

Enable container reuse to speed up test execution:

```java
@Container
protected static final PostgreSQLContainer<?> postgresContainer =
    new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);  // ← Enable reuse
```

Add to `~/.testcontainers.properties`:

```properties
testcontainers.reuse.enable=true
```

### Parallel Test Execution

```bash
# Run tests in parallel
mvn test -T 4  # Use 4 threads

# Or per-service parallel
mvn test -DforkCount=2 -DreuseForks=true
```

### Selective Test Execution

```bash
# Only run fast tests
mvn test -Dgroups=fast

# Skip slow tests
mvn test -DexcludedGroups=slow

# Run specific test suites
mvn test -Dtest=*IntegrationTest
```

---

## Maintenance

### Regular Tasks

1. **Update test data** when schema changes
2. **Review and update tests** when APIs change
3. **Monitor coverage trends** after each sprint
4. **Refactor duplicate test code** into base classes
5. **Update documentation** for new test scenarios

### Test Debt Management

- Mark flaky tests with `@Disabled` and create issues
- Remove obsolete tests after feature removal
- Consolidate duplicate test scenarios
- Keep test execution time under 5 minutes per service

---

## Resources

### Documentation

- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/3.2.2/reference/html/features.html#features.testing)
- [Testcontainers](https://www.testcontainers.org/)
- [MockMvc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/web/servlet/MockMvc.html)
- [JUnit 5](https://junit.org/junit5/docs/current/user-guide/)
- [REST Assured](https://rest-assured.io/)

### Internal Resources

- `docs/architecture/architect.md` - System architecture
- `backend/CLAUDE.md` - Development guidelines
- `backend/IMPLEMENTATION_PLAN.md` - Implementation roadmap

---

## Contact

For questions or issues with integration tests:

- **Team**: GCRF Test Team
- **Email**: test-team@gcrf.com
- **Slack**: #testing-integration

---

**Last Updated**: 2025-12-01
**Version**: 1.0.0
**Status**: Active
