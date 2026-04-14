# P2D: P0/P1 新代码测试覆盖 + Gateway Filter + 预存问题修复

**日期：** 2026-04-14
**状态：** Approved
**优先级：** P2（测试覆盖补齐 + 技术债清理）
**背景：** P0/P1 添加的新代码（ReaderController 4 端点、BorrowController batchReturn、ReserveController notify、ReaderServiceImpl 借阅校验）缺少测试覆盖。Gateway RateLimitFilter 一直无测试。common-web 的 logback-spring.xml 有语法 bug，NotificationControllerIntegrationTest 因 P0 控制器重写失效。

---

## 目标

1. 为 P0/P1 新代码补充测试覆盖
2. 为 Gateway RateLimitFilter 创建单元测试
3. 修复 NotificationControllerIntegrationTest 使其与当前控制器签名一致
4. 修复 common-web logback-spring.xml 的 XML 语法错误和缺失的 Spring Boot defaults include

---

## Section 1：ReaderController 4 个新端点测试

**文件：** 扩展 `backend/reader-service/src/test/java/com/gcrf/library/reader/controller/ReaderControllerIntegrationTest.java`

该测试类已在 P2A 迁移到 Testcontainers。在现有测试后添加 5 个新测试：

| #   | 测试                                             | 覆盖                                                                    |
| --- | ------------------------------------------------ | ----------------------------------------------------------------------- |
| 1   | `batchDelete_shouldDeleteMultipleReaders`        | `DELETE /api/v1/readers/batch?ids=1,2,3` — 循环删除并验证所有读者已删除 |
| 2   | `issueCard_shouldActivateCard`                   | `POST /api/v1/readers/{id}/card` — 验证调用 activateCard                |
| 3   | `updateReaderStatus_suspended_shouldSuspendCard` | `PUT /api/v1/readers/{id}/status` — body: `{"status":"suspended"}`      |
| 4   | `updateReaderStatus_active_shouldActivateCard`   | `PUT /api/v1/readers/{id}/status` — body: `{"status":"active"}`         |
| 5   | `getReaderByCardNumber_shouldReturnReader`       | `GET /api/v1/readers/card/{cardNumber}` — 基于 readerId 字段查询        |

**Mock：** 现有测试已 mock `CirculationServiceClient`（P2A 迁移时验证）。不需新增 mock。

---

## Section 2：BorrowController batchReturn 测试

**文件：** 扩展 `backend/circulation-service/src/test/java/com/gcrf/library/circulation/controller/BorrowControllerIntegrationTest.java`

添加 1 个测试：

```java
@Test
void batchReturn_shouldProcessMultipleBorrowsWithMixedResults() throws Exception {
    // Arrange: create 3 borrows - 2 active (can return), 1 already returned (will fail)
    Borrow active1 = createTestBorrow("BW-BATCH-001", "BORROWED");
    Borrow active2 = createTestBorrow("BW-BATCH-002", "BORROWED");
    Borrow returned = createTestBorrow("BW-BATCH-003", "RETURNED");

    Map<String, Object> request = Map.of(
        "borrowIds", List.of(active1.getBorrowId(), active2.getBorrowId(), returned.getBorrowId())
    );

    // Act & Assert
    mockMvc.perform(post("/api/v1/borrows/batch-return")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data", hasSize(3)))
        .andExpect(jsonPath("$.data[0].success").value(true))
        .andExpect(jsonPath("$.data[1].success").value(true))
        .andExpect(jsonPath("$.data[2].success").value(false));
}
```

---

## Section 3：ReserveController notify 测试

**文件：** 扩展 `backend/circulation-service/src/test/java/com/gcrf/library/circulation/controller/ReserveControllerIntegrationTest.java`

添加 1 个测试：

```java
@Test
void notifyReserve_shouldIncrementNotifyCount() throws Exception {
    // Arrange: create a RESERVED reservation
    Reserve reserve = new Reserve();
    reserve.setReserveId("RV-NOTIFY-001");
    reserve.setReaderId(1L);
    reserve.setBookId(1L);
    reserve.setReserveDate(LocalDate.now());
    reserve.setExpiryDate(LocalDate.now().plusDays(7));
    reserve.setStatus("RESERVED");
    reserve.setNotifyCount(0);
    reserveMapper.insert(reserve);

    // Act & Assert
    mockMvc.perform(post("/api/v1/reserves/" + reserve.getId() + "/notify")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));

    // Verify in DB
    Reserve updated = reserveMapper.selectById(reserve.getId());
    assertThat(updated.getNotifyCount()).isEqualTo(1);
    assertThat(updated.getNotifySent()).isTrue();
    assertThat(updated.getNotifySentDate()).isNotNull();
}
```

---

## Section 4：ReaderServiceTest 借阅数量校验

**⚠️ 文件状态：** P2B 创建了 DepartmentServiceTest、NotificationServiceTest 等，但**未创建** ReaderServiceTest。这里需要**新建**文件。

**文件：** `backend/reader-service/src/test/java/com/gcrf/library/reader/service/ReaderServiceTest.java`

实际上 reader-service 已有 `ReaderServiceTest` 在该路径下（项目原有，测试基础 CRUD 功能）。本任务**扩展**该文件，添加 4 个关于借阅校验的测试：

| #   | 测试                                                                      | 场景                                                                                       |
| --- | ------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| 1   | `deleteReader_whenReaderHasBorrows_shouldThrowException`                  | mock `circulationServiceClient.getCurrentBorrowCount()` 返回 > 0，期望抛 BusinessException |
| 2   | `deleteReader_whenCirculationServiceUnavailable_shouldProceedWithWarning` | mock Feign 客户端抛异常，验证 delete 仍成功（降级逻辑）                                    |
| 3   | `cancelCard_whenReaderHasBorrows_shouldThrowException`                    | mock 返回 > 0，期望抛异常                                                                  |
| 4   | `cancelCard_whenCirculationServiceUnavailable_shouldProceedWithWarning`   | mock 抛异常，验证 cancel 成功                                                              |

示例：

```java
@Test
@DisplayName("deleteReader_whenReaderHasBorrows_shouldThrowException")
void deleteReader_whenReaderHasBorrows_shouldThrowException() {
    // Arrange
    Reader reader = new Reader();
    reader.setId(1L);
    when(readerMapper.selectById(1L)).thenReturn(reader);

    Result<Integer> borrowCountResult = Result.success(3);
    when(circulationServiceClient.getCurrentBorrowCount(1L)).thenReturn(borrowCountResult);

    // Act & Assert
    assertThatThrownBy(() -> service.deleteReader(1L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("3 本未还图书");

    verify(readerMapper, never()).deleteById(any());
}
```

**注意：** 现有 `ReaderServiceTest` 的 `@Mock` 字段可能已有 `ReaderMapper` 但没有 `CirculationServiceClient`。需要添加 mock：

```java
@Mock
private CirculationServiceClient circulationServiceClient;
```

---

## Section 5：Gateway RateLimitFilter 单元测试

**新建：** `backend/gateway-service/src/test/java/com/gcrf/gateway/filter/RateLimitFilterTest.java`

Mock `RateLimiterService`，测试 filter 行为：

| #   | 测试                                            | 场景                                                         |
| --- | ----------------------------------------------- | ------------------------------------------------------------ |
| 1   | `filter_whenAllowed_shouldContinue`             | `isAllowed()` 返回 true，验证 chain.filter 被调用            |
| 2   | `filter_whenNotAllowed_shouldReturn429`         | `isAllowed()` 返回 false，验证返回 429 status                |
| 3   | `filter_shouldExtractClientIpFromXForwardedFor` | 请求带 `X-Forwarded-For: 1.2.3.4, 5.6.7.8`，验证取 `1.2.3.4` |
| 4   | `filter_shouldExtractUserIdFromHeader`          | 请求带 `X-User-Id: 100`，验证传给 `isAllowed`                |
| 5   | `filter_withMissingClientIp_shouldUseFallback`  | 无 X-Forwarded-For 和 X-Real-IP，使用 socket address         |

示例：

```java
@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimiterService rateLimiterService;

    @InjectMocks
    private RateLimitFilter filter;

    @Test
    @DisplayName("filter_whenAllowed_shouldContinue")
    void filter_whenAllowed_shouldContinue() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/v1/books")
            .header("X-Forwarded-For", "1.2.3.4")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(rateLimiterService.isAllowed(anyString(), anyString(), any())).thenReturn(true);
        when(rateLimiterService.getRemainingRequests(anyString(), anyString(), any())).thenReturn(99L);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = filter.filter(exchange, chain);
        result.block();

        // Assert
        verify(chain).filter(exchange);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("99");
    }
}
```

---

## Section 6：修复 NotificationControllerIntegrationTest

**文件：** `backend/notification-service/src/test/java/com/gcrf/library/notification/controller/NotificationControllerIntegrationTest.java`

**问题：** 测试使用 `NotificationCreateRequest`（已不存在），实际控制器使用 `NotificationSendRequest`。P0 rewrote 控制器，所有 endpoint 签名变更。

**当前控制器的实际签名（P0 重写后）：**

- `POST /api/v1/notifications` accepts `NotificationSendRequest`
- `GET /api/v1/notifications?userId=X&pageNum=1&pageSize=10` — 需要 `userId` 作为 `@RequestParam`
- `GET /api/v1/notifications/{id}?userId=X`
- `PUT /api/v1/notifications/{id}/read?userId=X`
- `PUT /api/v1/notifications/batch-read?userId=X` body: `List<Long>` ids
- `DELETE /api/v1/notifications/{id}?userId=X`
- `DELETE /api/v1/notifications/batch?userId=X` body: `List<Long>` ids
- `GET /api/v1/notifications/unread-count?userId=X`
- `GET /api/v1/notifications/latest?userId=X&limit=10`
- `DELETE /api/v1/notifications/clear?userId=X`

**修复策略：**

1. 将所有 `NotificationCreateRequest` 替换为 `NotificationSendRequest`（验证该类存在于 `dto/request/`）
2. 所有需要 userId 的 endpoint 调用添加 `.param("userId", "1")`
3. 对齐 `getUnreadCount` 的响应格式 — 返回 `UnreadCountVO`（含 `unreadCount` + `urgentCount`）
4. 对齐 `markAsRead` — 现在是 PUT 路径 `/api/v1/notifications/{id}/read`

重写整个测试文件（约 10 个测试方法），覆盖所有 10 个 endpoint。

---

## Section 7：修复 common-web logback-spring.xml

**文件：** `backend/common/common-web/src/main/resources/logback-spring.xml`

### Bug 1：`<property>` 语法错误

**当前（行 38-40）：**

```xml
<property name="JSON_PATTERN">
    {"timestamp":"%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ}","level":"%level",...}%n
</property>
```

**问题：** Logback `<property>` 要求 `name` + `value` 两个 attribute，不能用内容作为值。

**修复：**

```xml
<property name="JSON_PATTERN" value='{"timestamp":"%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ}","level":"%level",...}%n' />
```

注意 JSON 字符串需要用单引号包裹 `value`，因为 XML 中有双引号的 JSON。

### Bug 2：缺少 Spring Boot defaults include

**当前：** CONSOLE_PATTERN 使用 `%clr(...)` color conversion word，但未定义。

**修复：** 在 `<configuration>` 开始后第一行添加：

```xml
<configuration scan="true" scanPeriod="30 seconds">

    <!-- Include Spring Boot's default logback configuration for %clr(), LOG_LEVEL_PATTERN, etc. -->
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- Property definitions -->
    <springProperty scope="context" ...
```

### 验证

修复后运行任意 service 的测试，检查 stderr 无 `[clr] 'PatternLayout' conversion word` 或 `<property> requires either name attribute alone, or name+value attributes` 错误。

---

## 文件修改清单

| #   | 文件                                                                          | 操作     | 新增测试 |
| --- | ----------------------------------------------------------------------------- | -------- | -------- |
| 1   | `backend/reader-service/.../ReaderControllerIntegrationTest.java`             | 扩展     | 5        |
| 2   | `backend/circulation-service/.../BorrowControllerIntegrationTest.java`        | 扩展     | 1        |
| 3   | `backend/circulation-service/.../ReserveControllerIntegrationTest.java`       | 扩展     | 1        |
| 4   | `backend/reader-service/.../ReaderServiceTest.java`                           | 扩展     | 4        |
| 5   | `backend/gateway-service/.../filter/RateLimitFilterTest.java`                 | **新建** | 5        |
| 6   | `backend/notification-service/.../NotificationControllerIntegrationTest.java` | 重写     | ~10      |
| 7   | `backend/common/common-web/src/main/resources/logback-spring.xml`             | 修复 bug | -        |

**测试总数增加：** ~26 个

---

## 验证方法

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend

docker ps > /dev/null || (echo "Docker not running" && exit 1)

# Section 1
mvn test -pl reader-service -Dtest=ReaderControllerIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false

# Section 2
mvn test -pl circulation-service -Dtest=BorrowControllerIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false

# Section 3
mvn test -pl circulation-service -Dtest=ReserveControllerIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false

# Section 4
mvn test -pl reader-service -Dtest=ReaderServiceTest -Dsurefire.failIfNoSpecifiedTests=false

# Section 5
mvn test -pl gateway-service -Dtest=RateLimitFilterTest -Dsurefire.failIfNoSpecifiedTests=false

# Section 6
mvn test -pl notification-service -Dtest=NotificationControllerIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false

# Section 7 — no specific test, verify any service starts without logback errors
mvn test -pl book-service -Dtest=BookControllerIntegrationTest 2>&1 | grep -iE "logback|\[clr\]|property"
```

**预期：** 所有新增测试通过；logback 无 XML syntax error。
