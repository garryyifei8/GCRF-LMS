# P2A: Testcontainers 基础设施 + 迁移现有 integration tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce Testcontainers as the standard integration test infrastructure, provide a shared `BaseIntegrationTest` base class, and migrate 17 existing integration tests to use it.

**Architecture:** Singleton PostgreSQL container started once per JVM (via static initializer). `BaseIntegrationTest` in `common-core` uses `@DynamicPropertySource` to inject datasource config. Tests use `@Transactional` for per-method rollback.

**Tech Stack:** Testcontainers 1.19.7, Spring Boot 3.2.2, JUnit 5, PostgreSQL 15-alpine

**Spec:** `docs/specs/2026-04-14-p2a-testcontainers-infrastructure-design.md`

**Prerequisite:** Docker must be running locally.

---

## File Map

### New Files

| File                                                                                             | Responsibility                                                                                  |
| ------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------- |
| `backend/common/common-core/src/main/java/com/gcrf/library/common/test/BaseIntegrationTest.java` | Abstract base class providing singleton Testcontainers PostgreSQL and dynamic datasource config |

### Modified Files

| File                                 | Change                                                                               |
| ------------------------------------ | ------------------------------------------------------------------------------------ |
| `backend/pom.xml`                    | Add testcontainers-bom to dependencyManagement                                       |
| `backend/common/common-core/pom.xml` | Add testcontainers, junit-jupiter, spring-boot-starter-test as optional compile deps |
| 17 existing integration test classes | `extends BaseIntegrationTest` + `@Transactional`                                     |

---

## Task 1: Add Testcontainers BOM to parent pom

**Files:**

- Modify: `backend/pom.xml`

- [ ] **Step 1: Locate the dependencyManagement section**

```bash
grep -n "<dependencyManagement>" backend/pom.xml
```

- [ ] **Step 2: Add Testcontainers BOM**

Inside `<dependencyManagement><dependencies>` in `backend/pom.xml`, add this block (before the closing `</dependencies>`):

```xml
            <!-- Testcontainers BOM -->
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>1.19.7</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
```

- [ ] **Step 3: Validate pom**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn validate -N
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/pom.xml
git commit -m "build(infra): add Testcontainers BOM to parent pom

- Version 1.19.7 managed via BOM
- Individual modules can declare testcontainers deps without version"
```

---

## Task 2: Add Testcontainers dependencies to common-core

**Files:**

- Modify: `backend/common/common-core/pom.xml`

- [ ] **Step 1: Add dependencies**

In `backend/common/common-core/pom.xml`, inside `<dependencies>`, add:

```xml
        <!-- Testcontainers - optional so downstream services don't pull it at runtime -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <optional>true</optional>
        </dependency>
        <!-- Spring Boot test support - optional for the same reason -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <optional>true</optional>
        </dependency>
```

Note: `<optional>true</optional>` prevents these deps from being transitively pulled into runtime of services using common-core.

- [ ] **Step 2: Compile common-core**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn clean compile -pl common/common-core -am
```

Expected: BUILD SUCCESS. Testcontainers and JUnit 5 jars download.

- [ ] **Step 3: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/common/common-core/pom.xml
git commit -m "build(common): add Testcontainers deps as optional

- testcontainers:postgresql for container-based integration tests
- testcontainers:junit-jupiter for @Testcontainers annotation
- spring-boot-starter-test for MockMvc and test utilities
- All marked optional to avoid leaking to service runtime"
```

---

## Task 3: Create BaseIntegrationTest

**Files:**

- Create: `backend/common/common-core/src/main/java/com/gcrf/library/common/test/BaseIntegrationTest.java`

- [ ] **Step 1: Create the file**

Create `backend/common/common-core/src/main/java/com/gcrf/library/common/test/BaseIntegrationTest.java` with this exact content:

```java
package com.gcrf.library.common.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test base class using a singleton PostgreSQL Testcontainer.
 *
 * All integration tests that need a database must extend this class and
 * use @Transactional to ensure data isolation between tests.
 *
 * <p>Usage:
 * <pre>
 * {@code
 * @SpringBootTest
 * @AutoConfigureMockMvc
 * @Transactional
 * @ActiveProfiles("test")
 * class MyControllerIntegrationTest extends BaseIntegrationTest {
 *     // test methods
 * }
 * }
 * </pre>
 *
 * @author GCRF Team
 * @since 2026-04-14
 */
@Testcontainers
public abstract class BaseIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("gcrf_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }
}
```

- [ ] **Step 2: Compile common-core to verify the class compiles**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn clean compile -pl common/common-core -am
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/common/common-core/src/main/java/com/gcrf/library/common/test/BaseIntegrationTest.java
git commit -m "feat(common): add BaseIntegrationTest for Testcontainers-based integration tests

- Singleton PostgreSQL container (postgres:15-alpine) started once per JVM
- .withReuse(true) for fast local iteration
- @DynamicPropertySource injects spring.datasource.* and spring.flyway.*
- Subclasses combine @SpringBootTest + @Transactional + @ActiveProfiles(test)"
```

---

## Task 4: Migrate auth-service integration tests

**Files to modify:**

- `backend/auth-service/src/test/java/com/gcrf/library/auth/controller/AuthControllerIntegrationTest.java`
- `backend/auth-service/src/test/java/com/gcrf/library/auth/controller/UserControllerIntegrationTest.java`
- `backend/auth-service/src/test/java/com/gcrf/library/auth/JwtTokenFlowIntegrationTest.java`

- [ ] **Step 1: Read the current state of each test class**

```bash
for f in \
  backend/auth-service/src/test/java/com/gcrf/library/auth/controller/AuthControllerIntegrationTest.java \
  backend/auth-service/src/test/java/com/gcrf/library/auth/controller/UserControllerIntegrationTest.java \
  backend/auth-service/src/test/java/com/gcrf/library/auth/JwtTokenFlowIntegrationTest.java
do
  echo "=== $f ==="
  head -30 "$f"
done
```

Note the current class annotations and imports.

- [ ] **Step 2: For each test class, apply these 3 changes**

**Change 1 — Add import:**

```java
import com.gcrf.library.common.test.BaseIntegrationTest;
```

**Change 2 — Change class declaration to extend BaseIntegrationTest:**

```java
// Before:
class AuthControllerIntegrationTest {

// After:
class AuthControllerIntegrationTest extends BaseIntegrationTest {
```

**Change 3 — Ensure @Transactional annotation is present at class level:**

```java
import org.springframework.transaction.annotation.Transactional;
// ...
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AuthControllerIntegrationTest extends BaseIntegrationTest {
```

If `@Transactional` is already present, leave it. If it's on individual methods but not class, move it to class level.

- [ ] **Step 3: Check for hardcoded datasource config**

```bash
grep -n "spring.datasource\|jdbc:postgresql" \
  backend/auth-service/src/test/resources/application*.yml \
  backend/auth-service/src/test/resources/application*.properties 2>&1
```

If any hardcoded URLs are found in test resources, either delete the datasource block (BaseIntegrationTest overrides it via DynamicPropertySource) or leave it and verify BaseIntegrationTest still wins at test time.

- [ ] **Step 4: Verify tests compile and run**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
# Ensure Docker is running first
docker ps > /dev/null || (echo "ERROR: Docker not running" && exit 1)
mvn test -pl auth-service
```

Expected:

- First run: Downloads postgres:15-alpine (~20MB), starts container
- Tests pass
- Container remains (due to `.withReuse(true)`) for faster next runs

If any test fails because of DB state issues (e.g., previous test data interfering), check that `@Transactional` is on the class.

- [ ] **Step 5: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/auth-service/
git commit -m "test(auth): migrate integration tests to Testcontainers

- AuthControllerIntegrationTest, UserControllerIntegrationTest, JwtTokenFlowIntegrationTest
- Extend BaseIntegrationTest for PostgreSQL container
- Add @Transactional for per-method rollback"
```

---

## Task 5: Migrate book-service integration tests

**Files to modify:**

- `backend/book-service/src/test/java/com/gcrf/library/book/controller/BookControllerIntegrationTest.java`
- `backend/book-service/src/test/java/com/gcrf/library/book/controller/CategoryControllerIntegrationTest.java`
- `backend/book-service/src/test/java/com/gcrf/library/book/controller/BookFileControllerIntegrationTest.java`

- [ ] **Step 1: Check for existing BaseIntegrationTest in book-service**

```bash
find backend/book-service/src/test -name "BaseIntegrationTest.java" -type f
```

If one exists locally, it must be deleted — all tests should use the common-core version. Move any helpful setup code from the local version to the common one if needed (but likely the common version is sufficient).

- [ ] **Step 2: Apply migration to the 3 test classes**

For each of the 3 files:

1. Add `import com.gcrf.library.common.test.BaseIntegrationTest;`
2. Change class to `extends BaseIntegrationTest` (replace any previous base class)
3. Ensure `@Transactional` is on the class

- [ ] **Step 3: Check for hardcoded datasource config**

```bash
grep -n "spring.datasource\|jdbc:postgresql" \
  backend/book-service/src/test/resources/application*.yml 2>&1
```

Remove or comment out datasource blocks in test configs if present.

- [ ] **Step 4: Run tests**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
docker ps > /dev/null || (echo "ERROR: Docker not running" && exit 1)
mvn test -pl book-service
```

Expected: Tests pass.

- [ ] **Step 5: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/book-service/
git commit -m "test(book): migrate integration tests to Testcontainers

- BookControllerIntegrationTest, CategoryControllerIntegrationTest, BookFileControllerIntegrationTest
- Remove local BaseIntegrationTest (if existed), use common-core version
- Extend BaseIntegrationTest + @Transactional"
```

---

## Task 6: Migrate circulation-service integration tests

**Files to modify:**

- `backend/circulation-service/src/test/java/com/gcrf/library/circulation/controller/BorrowControllerIntegrationTest.java`
- `backend/circulation-service/src/test/java/com/gcrf/library/circulation/controller/ReserveControllerIntegrationTest.java`

- [ ] **Step 1: Apply migration to 2 test classes**

For each file:

1. Add `import com.gcrf.library.common.test.BaseIntegrationTest;`
2. Change class to `extends BaseIntegrationTest`
3. Ensure `@Transactional` at class level

- [ ] **Step 2: Check test resources**

```bash
grep -n "spring.datasource\|jdbc:postgresql" \
  backend/circulation-service/src/test/resources/application*.yml 2>&1
```

Remove hardcoded datasource if present.

- [ ] **Step 3: Run tests**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
docker ps > /dev/null || (echo "ERROR: Docker not running" && exit 1)
mvn test -pl circulation-service
```

Expected: Tests pass.

- [ ] **Step 4: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/circulation-service/
git commit -m "test(circulation): migrate integration tests to Testcontainers

- BorrowControllerIntegrationTest, ReserveControllerIntegrationTest
- Extend BaseIntegrationTest + @Transactional"
```

---

## Task 7: Migrate reader-service integration tests

**Files to modify:**

- `backend/reader-service/src/test/java/com/gcrf/library/reader/controller/ReaderControllerIntegrationTest.java`

- [ ] **Step 1: Apply migration**

1. Add `import com.gcrf.library.common.test.BaseIntegrationTest;`
2. Change class to `extends BaseIntegrationTest`
3. Ensure `@Transactional` at class level

- [ ] **Step 2: Check test resources**

```bash
grep -n "spring.datasource\|jdbc:postgresql" \
  backend/reader-service/src/test/resources/application*.yml 2>&1
```

Remove hardcoded datasource if present.

- [ ] **Step 3: Run tests**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
docker ps > /dev/null || (echo "ERROR: Docker not running" && exit 1)
mvn test -pl reader-service
```

Expected: Tests pass.

- [ ] **Step 4: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/reader-service/
git commit -m "test(reader): migrate integration tests to Testcontainers

- ReaderControllerIntegrationTest
- Extend BaseIntegrationTest + @Transactional"
```

---

## Task 8: Migrate system-service integration tests

**Files to modify:**

- `backend/system-service/src/test/java/com/gcrf/library/system/controller/LoginLogControllerIntegrationTest.java`
- `backend/system-service/src/test/java/com/gcrf/library/system/controller/MenuControllerIntegrationTest.java`
- `backend/system-service/src/test/java/com/gcrf/library/system/controller/OperationLogControllerIntegrationTest.java`
- `backend/system-service/src/test/java/com/gcrf/library/system/controller/PermissionControllerIntegrationTest.java`
- `backend/system-service/src/test/java/com/gcrf/library/system/controller/RoleControllerIntegrationTest.java`

- [ ] **Step 1: Apply migration to all 5 test classes**

For each file:

1. Add `import com.gcrf.library.common.test.BaseIntegrationTest;`
2. Change class to `extends BaseIntegrationTest`
3. Ensure `@Transactional` at class level

- [ ] **Step 2: Check test resources**

```bash
grep -n "spring.datasource\|jdbc:postgresql" \
  backend/system-service/src/test/resources/application*.yml 2>&1
```

Remove hardcoded datasource if present.

- [ ] **Step 3: Run tests**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
docker ps > /dev/null || (echo "ERROR: Docker not running" && exit 1)
mvn test -pl system-service
```

Expected: Tests pass.

- [ ] **Step 4: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/system-service/
git commit -m "test(system): migrate integration tests to Testcontainers

- Login/Menu/Operation/Permission/Role controller integration tests
- Extend BaseIntegrationTest + @Transactional"
```

---

## Task 9: Migrate notification-service integration tests

**Files to modify:**

- `backend/notification-service/src/test/java/com/gcrf/library/notification/controller/EmailControllerIntegrationTest.java`
- `backend/notification-service/src/test/java/com/gcrf/library/notification/controller/SmsControllerIntegrationTest.java`
- `backend/notification-service/src/test/java/com/gcrf/library/notification/controller/NotificationControllerIntegrationTest.java`

- [ ] **Step 1: Apply migration to 3 test classes**

For each file:

1. Add `import com.gcrf.library.common.test.BaseIntegrationTest;`
2. Change class to `extends BaseIntegrationTest`
3. Ensure `@Transactional` at class level

- [ ] **Step 2: Check test resources**

```bash
grep -n "spring.datasource\|jdbc:postgresql" \
  backend/notification-service/src/test/resources/application*.yml 2>&1
```

Remove hardcoded datasource if present.

- [ ] **Step 3: Run tests**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
docker ps > /dev/null || (echo "ERROR: Docker not running" && exit 1)
mvn test -pl notification-service
```

Expected: Tests pass. Note: NotificationControllerIntegrationTest was rewritten in P0 (gap fix) and the controller method signatures changed — verify tests still align with the current controller.

- [ ] **Step 4: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/notification-service/
git commit -m "test(notification): migrate integration tests to Testcontainers

- Email/Sms/Notification controller integration tests
- Extend BaseIntegrationTest + @Transactional"
```

---

## Task 10: Final verification — full integration test run

- [ ] **Step 1: Docker preflight**

```bash
docker ps
```

Expected: Docker is running, no errors.

- [ ] **Step 2: Full test run across all services**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn test -pl auth-service,book-service,circulation-service,reader-service,system-service,notification-service
```

Expected:

- One PostgreSQL container starts (reused across all tests due to singleton + `.withReuse(true)`)
- All integration tests pass
- Total test time: ~2-5 minutes for first run, <2 minutes for cached subsequent runs

- [ ] **Step 3: Verify single container used**

While tests are running (in another terminal):

```bash
docker ps --filter "ancestor=postgres:15-alpine"
```

Expected: ONE container running (not one per service).

- [ ] **Step 4: Check for any hardcoded DB references that got missed**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
grep -rn "jdbc:postgresql://localhost" src/test/ 2>&1 || echo "None found"
find . -path "*/src/test/resources" -name "application*.yml" -exec grep -l "datasource" {} \;
```

Clean up any remaining hardcoded references.

- [ ] **Step 5: Commit spec and plan docs**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add docs/specs/2026-04-14-p2a-testcontainers-infrastructure-design.md docs/specs/2026-04-14-p2a-testcontainers-infrastructure-plan.md
git commit -m "docs(docs): add P2A Testcontainers infrastructure spec and plan"
```
