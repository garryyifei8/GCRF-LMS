# P2A: Testcontainers 基础设施 + 迁移现有 integration tests

**日期：** 2026-04-14
**状态：** Approved
**优先级：** P2（测试基座）
**背景：** P2 要求补全后端测试覆盖。现有 integration tests 依赖本地 PostgreSQL，不利于 CI 和隔离。P2A 引入 Testcontainers 作为统一基座，为后续 P2B/P2C/P2D 铺路。

---

## 目标

1. 所有 integration tests 使用 Testcontainers 自动启动 PostgreSQL 容器
2. 单例容器 + `@Transactional` 回滚策略确保速度与隔离
3. 提供 `BaseIntegrationTest` 抽象类作为统一入口
4. 迁移 17 个现有 integration test 类

---

## Section 1：Testcontainers 依赖

### 父 pom 依赖管理

**修改 `backend/pom.xml`：** 在 `<dependencyManagement>` 中添加 Testcontainers BOM（版本由 BOM 统一管理，避免版本漂移）：

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-bom</artifactId>
    <version>1.19.7</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

---

## Section 2：BaseIntegrationTest 在 common-core

### 放置位置

- 位置：`backend/common/common-core/src/main/java/com/gcrf/library/common/test/BaseIntegrationTest.java`
- 使用 `main` scope 而非 `test` scope — 这样其他服务可以直接继承，无需额外 test-jar 配置

### 代码

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

### common-core pom 依赖

在 `backend/common/common-core/pom.xml` 添加（注意：`compile` scope，不是 test — 因为其他模块需要继承该类）：

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
</dependency>
```

**注意：** 这些依赖在 common-core 是 compile scope，但只在 test-runtime 时才真正需要。Spring Boot 的 `starter-test` 已经是 test scope 了 — 这里我们让它变成 compile 以供继承。如果担心生产 jar 臃肿，可以改为 optional 依赖（`<optional>true</optional>`），这样不会传递到其他服务的 runtime。

### 其他服务的依赖

现有服务已经通过 `common-core` 依赖。迁移测试时无需额外添加依赖 — 只需 `extends BaseIntegrationTest`。

---

## Section 3：数据隔离策略

每个继承 `BaseIntegrationTest` 的测试类必须加三个注解：

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class MyControllerIntegrationTest extends BaseIntegrationTest {
    // ...
}
```

**为什么这样工作：**

- `@SpringBootTest` 启动完整 Spring context
- `@AutoConfigureMockMvc` 注入 `MockMvc` 供 controller 测试使用
- `@Transactional` 在 Spring Test 中默认回滚，每个测试方法结束后数据回滚
- `@ActiveProfiles("test")` 加载 `application-test.yml`（若存在）

结合 BaseIntegrationTest 的 `@DynamicPropertySource`，datasource 配置动态注入，完全覆盖本地 DB 配置。

---

## Section 4：迁移现有 integration tests 清单（17 个）

| 服务                 | 测试类                                | 文件路径                                                                                                                         |
| -------------------- | ------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| auth-service         | AuthControllerIntegrationTest         | `backend/auth-service/src/test/java/com/gcrf/library/auth/controller/AuthControllerIntegrationTest.java`                         |
| auth-service         | UserControllerIntegrationTest         | `backend/auth-service/src/test/java/com/gcrf/library/auth/controller/UserControllerIntegrationTest.java`                         |
| auth-service         | JwtTokenFlowIntegrationTest           | `backend/auth-service/src/test/java/com/gcrf/library/auth/JwtTokenFlowIntegrationTest.java`                                      |
| book-service         | BookControllerIntegrationTest         | `backend/book-service/src/test/java/com/gcrf/library/book/controller/BookControllerIntegrationTest.java`                         |
| book-service         | CategoryControllerIntegrationTest     | `backend/book-service/src/test/java/com/gcrf/library/book/controller/CategoryControllerIntegrationTest.java`                     |
| book-service         | BookFileControllerIntegrationTest     | `backend/book-service/src/test/java/com/gcrf/library/book/controller/BookFileControllerIntegrationTest.java`                     |
| circulation-service  | BorrowControllerIntegrationTest       | `backend/circulation-service/src/test/java/com/gcrf/library/circulation/controller/BorrowControllerIntegrationTest.java`         |
| circulation-service  | ReserveControllerIntegrationTest      | `backend/circulation-service/src/test/java/com/gcrf/library/circulation/controller/ReserveControllerIntegrationTest.java`        |
| reader-service       | ReaderControllerIntegrationTest       | `backend/reader-service/src/test/java/com/gcrf/library/reader/controller/ReaderControllerIntegrationTest.java`                   |
| system-service       | LoginLogControllerIntegrationTest     | `backend/system-service/src/test/java/com/gcrf/library/system/controller/LoginLogControllerIntegrationTest.java`                 |
| system-service       | MenuControllerIntegrationTest         | `backend/system-service/src/test/java/com/gcrf/library/system/controller/MenuControllerIntegrationTest.java`                     |
| system-service       | OperationLogControllerIntegrationTest | `backend/system-service/src/test/java/com/gcrf/library/system/controller/OperationLogControllerIntegrationTest.java`             |
| system-service       | PermissionControllerIntegrationTest   | `backend/system-service/src/test/java/com/gcrf/library/system/controller/PermissionControllerIntegrationTest.java`               |
| system-service       | RoleControllerIntegrationTest         | `backend/system-service/src/test/java/com/gcrf/library/system/controller/RoleControllerIntegrationTest.java`                     |
| notification-service | EmailControllerIntegrationTest        | `backend/notification-service/src/test/java/com/gcrf/library/notification/controller/EmailControllerIntegrationTest.java`        |
| notification-service | SmsControllerIntegrationTest          | `backend/notification-service/src/test/java/com/gcrf/library/notification/controller/SmsControllerIntegrationTest.java`          |
| notification-service | NotificationControllerIntegrationTest | `backend/notification-service/src/test/java/com/gcrf/library/notification/controller/NotificationControllerIntegrationTest.java` |

**每个测试类的修改：**

1. 添加 `extends BaseIntegrationTest`
2. 确认有 `@Transactional`（没有则加）
3. 移除任何本地 DB 硬编码配置
4. 如有 `BaseIntegrationTest` 类名冲突（book-service 可能已有同名类），先删除本地版本再继承 common-core 版本

### 不需要迁移的测试

以下测试**不**触数据库，保持不变：

- Gateway 下的所有测试（filter 单元测试、GatewayApplicationTest、ActuatorHealthTest、Knife4jIntegrationTest）
- 所有 `*ServiceTest`（单元测试，用 Mockito）
- 所有 `*FilterTest`（filter 单元测试）

---

## Section 5：test profile 配置

每个服务创建或确认 `src/test/resources/application-test.yml`：

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
  # datasource 配置由 BaseIntegrationTest 的 @DynamicPropertySource 注入
  jpa:
    hibernate:
      ddl-auto: validate
```

如果现有 test profile 已定义 datasource，需移除这些属性，让 BaseIntegrationTest 的动态属性生效。

---

## 修改文件清单

### 新建

| 文件                                                                                             |
| ------------------------------------------------------------------------------------------------ |
| `backend/common/common-core/src/main/java/com/gcrf/library/common/test/BaseIntegrationTest.java` |

### 修改

| 文件                                 | 变更                                                           |
| ------------------------------------ | -------------------------------------------------------------- |
| `backend/pom.xml`                    | 添加 testcontainers-bom                                        |
| `backend/common/common-core/pom.xml` | 添加 testcontainers + junit-jupiter + spring-boot-starter-test |
| 17 个现有 integration test 类        | `extends BaseIntegrationTest` + `@Transactional`               |
| 每个服务的 `application-test.yml`    | 移除硬编码 datasource（如有）                                  |

---

## 验证方法

### 本地验证

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd backend
# Docker must be running
mvn test -pl auth-service,book-service,circulation-service,reader-service,system-service,notification-service
```

**预期：**

- 首次运行：下载 postgres:15-alpine 镜像（~20MB），启动容器（~5s）
- 后续运行：容器复用（`.withReuse(true)`），启动时间 < 1s
- 所有 integration tests 通过

### CI 验证

CI 环境需要 Docker。如 GitHub Actions Ubuntu runner 默认有 Docker。在 `.github/workflows/ci.yml` 的 `build-backend` job 中添加（如果必要）：

```yaml
services:
  docker:
    image: docker:dind
```

或验证 runner 镜像已内置 Docker（ubuntu-latest 已有）。
