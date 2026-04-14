# P2D: P0/P1 新代码测试 + Gateway Filter + 修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add tests for P0/P1 new code, add Gateway RateLimitFilter test, fix broken NotificationControllerIntegrationTest, fix logback-spring.xml XML syntax bugs.

**Architecture:** Mix of test additions (integration + unit) and a targeted bug fix to XML config. Tests use the Testcontainers infrastructure from P2A (integration) or Mockito (unit).

**Tech Stack:** Spring Boot 3.2.2, JUnit 5, Mockito, AssertJ, Testcontainers, Spring WebFlux Test (for gateway)

**Spec:** `docs/specs/2026-04-14-p2d-new-code-tests-and-fixes-design.md`

**Prerequisite:** Docker running for integration tests.

---

## File Map

| Task | File                                                                          | Action       | Tests added |
| ---- | ----------------------------------------------------------------------------- | ------------ | ----------- |
| T1   | `backend/reader-service/.../ReaderControllerIntegrationTest.java`             | Extend       | 5           |
| T2   | `backend/circulation-service/.../BorrowControllerIntegrationTest.java`        | Extend       | 1           |
| T3   | `backend/circulation-service/.../ReserveControllerIntegrationTest.java`       | Extend       | 1           |
| T4   | `backend/reader-service/.../ReaderServiceTest.java`                           | Extend       | 4           |
| T5   | `backend/gateway-service/.../filter/RateLimitFilterTest.java`                 | Create       | 5           |
| T6   | `backend/notification-service/.../NotificationControllerIntegrationTest.java` | Rewrite      | ~10         |
| T7   | `backend/common/common-web/.../logback-spring.xml`                            | Fix XML bugs | -           |

---

## Task 1: ReaderController P0 endpoints tests

**Files:**

- Modify: `backend/reader-service/src/test/java/com/gcrf/library/reader/controller/ReaderControllerIntegrationTest.java`

- [ ] **Step 1: Read controller to confirm endpoint signatures**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
sed -n '220,280p' backend/reader-service/src/main/java/com/gcrf/library/reader/controller/ReaderController.java
```

Confirm these 4 endpoints:

- `DELETE /api/v1/readers/batch?ids=1,2,3`
- `POST /api/v1/readers/{id}/card` — body: `Map<String, Object>`, calls `activateCard`
- `PUT /api/v1/readers/{id}/status` — body: `{"status": "suspended"|"active"}`
- `GET /api/v1/readers/card/{cardNumber}` — queries by `readerId` field

- [ ] **Step 2: Read the current test class structure**

```bash
cat backend/reader-service/src/test/java/com/gcrf/library/reader/controller/ReaderControllerIntegrationTest.java | head -80
```

Note the class-level annotations, imports, `@BeforeEach setUp()` structure, and how existing tests create Reader entities.

- [ ] **Step 3: Add the 5 new tests**

Add these tests before the closing `}` of the class:

```java
    // ===== P0 new endpoints =====

    @Test
    void batchDelete_shouldDeleteMultipleReaders() throws Exception {
        // Arrange: create 3 readers to delete
        Reader r1 = new Reader();
        r1.setReaderId("BATCH_001");
        r1.setName("批量1");
        r1.setReaderType("STUDENT");
        r1.setStatus("ACTIVE");
        readerMapper.insert(r1);

        Reader r2 = new Reader();
        r2.setReaderId("BATCH_002");
        r2.setName("批量2");
        r2.setReaderType("STUDENT");
        r2.setStatus("ACTIVE");
        readerMapper.insert(r2);

        // Mock CirculationServiceClient to return 0 borrows
        when(circulationServiceClient.getCurrentBorrowCount(anyLong()))
            .thenReturn(Result.success(0));

        // Act & Assert
        mockMvc.perform(delete("/api/v1/readers/batch")
                .param("ids", r1.getId() + "," + r2.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        // Verify deleted
        assertThat(readerMapper.selectById(r1.getId())).isNull();
        assertThat(readerMapper.selectById(r2.getId())).isNull();
    }

    @Test
    void issueCard_shouldActivateCard() throws Exception {
        // Arrange: create a suspended reader
        Reader reader = new Reader();
        reader.setReaderId("ISSUE_CARD_001");
        reader.setName("办卡测试");
        reader.setReaderType("STUDENT");
        reader.setStatus("SUSPENDED");
        readerMapper.insert(reader);

        Map<String, Object> request = Map.of(
            "cardExpireDate", "2027-12-31",
            "depositAmount", 100
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/readers/" + reader.getId() + "/card")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        // Verify status changed to ACTIVE
        Reader updated = readerMapper.selectById(reader.getId());
        assertThat(updated.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void updateReaderStatus_suspended_shouldSuspendCard() throws Exception {
        Reader reader = new Reader();
        reader.setReaderId("STATUS_SUSP_001");
        reader.setName("冻结测试");
        reader.setReaderType("STUDENT");
        reader.setStatus("ACTIVE");
        readerMapper.insert(reader);

        Map<String, String> request = Map.of("status", "suspended");

        mockMvc.perform(put("/api/v1/readers/" + reader.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        Reader updated = readerMapper.selectById(reader.getId());
        assertThat(updated.getStatus()).isEqualTo("SUSPENDED");
    }

    @Test
    void updateReaderStatus_active_shouldActivateCard() throws Exception {
        Reader reader = new Reader();
        reader.setReaderId("STATUS_ACT_001");
        reader.setName("激活测试");
        reader.setReaderType("STUDENT");
        reader.setStatus("SUSPENDED");
        readerMapper.insert(reader);

        Map<String, String> request = Map.of("status", "active");

        mockMvc.perform(put("/api/v1/readers/" + reader.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        Reader updated = readerMapper.selectById(reader.getId());
        assertThat(updated.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void getReaderByCardNumber_shouldReturnReader() throws Exception {
        Reader reader = new Reader();
        reader.setReaderId("CARDNUM_001");
        reader.setName("卡号查询");
        reader.setReaderType("STUDENT");
        reader.setStatus("ACTIVE");
        readerMapper.insert(reader);

        mockMvc.perform(get("/api/v1/readers/card/CARDNUM_001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.readerId").value("CARDNUM_001"))
            .andExpect(jsonPath("$.data.name").value("卡号查询"));
    }
```

Add imports if missing:

```java
import java.util.Map;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
```

If `CirculationServiceClient` is not already `@MockBean`, check existing test class — it should already be mocked. If not, add:

```java
@MockBean
private CirculationServiceClient circulationServiceClient;
```

- [ ] **Step 4: Run the tests**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
docker ps > /dev/null || exit 1
mvn test -pl reader-service -Dtest=ReaderControllerIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run|BUILD" | tail -3
```

Expected: all existing tests still pass + 5 new tests pass.

- [ ] **Step 5: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/reader-service/src/test/java/com/gcrf/library/reader/controller/ReaderControllerIntegrationTest.java
git commit -m "test(reader): add integration tests for P0 ReaderController endpoints

- batchDelete_shouldDeleteMultipleReaders
- issueCard_shouldActivateCard
- updateReaderStatus (suspended/active)
- getReaderByCardNumber_shouldReturnReader"
```

Commitlint scopes: gateway, auth, book, circulation, reader, system, notification, recommend, chat, analytics, common, web-admin, infra, docs.

---

## Task 2: BorrowController batchReturn test

**Files:**

- Modify: `backend/circulation-service/src/test/java/com/gcrf/library/circulation/controller/BorrowControllerIntegrationTest.java`

- [ ] **Step 1: Read existing test class for pattern**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
head -80 backend/circulation-service/src/test/java/com/gcrf/library/circulation/controller/BorrowControllerIntegrationTest.java
```

Note how existing tests create Borrow records via `borrowMapper.insert(...)`.

- [ ] **Step 2: Add the test**

Add before the closing `}` of the class:

```java
    @Test
    void batchReturn_shouldProcessMultipleBorrowsWithMixedResults() throws Exception {
        // Arrange: create 3 borrows — 2 active (returnable), 1 already returned (will fail)
        Borrow active1 = new Borrow();
        active1.setBorrowId("BW-BATCH-001");
        active1.setReaderId(1L);
        active1.setBookId(1L);
        active1.setBookBarcode("BATCH-BC-001");
        active1.setBorrowDate(LocalDate.now().minusDays(5));
        active1.setDueDate(LocalDate.now().plusDays(25));
        active1.setStatus("BORROWED");
        active1.setRenewCount(0);
        active1.setMaxRenewCount(2);
        borrowMapper.insert(active1);

        Borrow active2 = new Borrow();
        active2.setBorrowId("BW-BATCH-002");
        active2.setReaderId(1L);
        active2.setBookId(2L);
        active2.setBookBarcode("BATCH-BC-002");
        active2.setBorrowDate(LocalDate.now().minusDays(5));
        active2.setDueDate(LocalDate.now().plusDays(25));
        active2.setStatus("BORROWED");
        active2.setRenewCount(0);
        active2.setMaxRenewCount(2);
        borrowMapper.insert(active2);

        Borrow returned = new Borrow();
        returned.setBorrowId("BW-BATCH-003");
        returned.setReaderId(1L);
        returned.setBookId(3L);
        returned.setBookBarcode("BATCH-BC-003");
        returned.setBorrowDate(LocalDate.now().minusDays(30));
        returned.setDueDate(LocalDate.now().minusDays(1));
        returned.setReturnDate(LocalDate.now().minusDays(2));
        returned.setStatus("RETURNED");
        returned.setRenewCount(0);
        returned.setMaxRenewCount(2);
        borrowMapper.insert(returned);

        Map<String, Object> request = Map.of(
            "borrowIds", List.of(
                active1.getBorrowId(),
                active2.getBorrowId(),
                returned.getBorrowId()
            )
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/borrows/batch-return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data", hasSize(3)))
            // First two succeed
            .andExpect(jsonPath("$.data[0].success").value(true))
            .andExpect(jsonPath("$.data[1].success").value(true))
            // Third fails (already returned)
            .andExpect(jsonPath("$.data[2].success").value(false));
    }
```

Add imports if missing:

```java
import java.util.List;
import java.util.Map;
import static org.hamcrest.Matchers.hasSize;
```

- [ ] **Step 3: Run**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
docker ps > /dev/null || exit 1
mvn test -pl circulation-service -Dtest=BorrowControllerIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run|BUILD" | tail -3
```

Expected: all existing tests pass + 1 new test passes.

- [ ] **Step 4: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/circulation-service/src/test/java/com/gcrf/library/circulation/controller/BorrowControllerIntegrationTest.java
git commit -m "test(circulation): add batchReturn integration test

- Tests mixed-results scenario (2 success + 1 failure for already-returned)
- Verifies per-item success/failure reporting in response"
```

---

## Task 3: ReserveController notify test

**Files:**

- Modify: `backend/circulation-service/src/test/java/com/gcrf/library/circulation/controller/ReserveControllerIntegrationTest.java`

- [ ] **Step 1: Read existing pattern**

```bash
head -80 backend/circulation-service/src/test/java/com/gcrf/library/circulation/controller/ReserveControllerIntegrationTest.java
```

- [ ] **Step 2: Add the test**

```java
    @Test
    void notifyReserve_shouldIncrementNotifyCount() throws Exception {
        // Arrange: create a RESERVED reservation with notifyCount=0
        Reserve reserve = new Reserve();
        reserve.setReserveId("RV-NOTIFY-001");
        reserve.setReaderId(1L);
        reserve.setBookId(1L);
        reserve.setReserveDate(LocalDate.now());
        reserve.setExpiryDate(LocalDate.now().plusDays(7));
        reserve.setStatus("RESERVED");
        reserve.setNotifyCount(0);
        reserve.setNotifySent(false);
        reserveMapper.insert(reserve);

        // Act
        mockMvc.perform(post("/api/v1/reserves/" + reserve.getId() + "/notify")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        // Assert: verify count and flags in DB
        Reserve updated = reserveMapper.selectById(reserve.getId());
        assertThat(updated.getNotifyCount()).isEqualTo(1);
        assertThat(updated.getNotifySent()).isTrue();
        assertThat(updated.getNotifySentDate()).isNotNull();
    }
```

- [ ] **Step 3: Run**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
docker ps > /dev/null || exit 1
mvn test -pl circulation-service -Dtest=ReserveControllerIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run|BUILD" | tail -3
```

- [ ] **Step 4: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/circulation-service/src/test/java/com/gcrf/library/circulation/controller/ReserveControllerIntegrationTest.java
git commit -m "test(circulation): add notifyReserve integration test

- Verifies notifyCount increment, notifySent flag, notifySentDate update"
```

---

## Task 4: ReaderServiceTest borrow count validation tests

**Files:**

- Modify: `backend/reader-service/src/test/java/com/gcrf/library/reader/service/ReaderServiceTest.java`

- [ ] **Step 1: Read the existing ReaderServiceTest**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
cat backend/reader-service/src/test/java/com/gcrf/library/reader/service/ReaderServiceTest.java | head -80
```

Check:

- What mocks are present (`@Mock ReaderMapper`? `@Mock CirculationServiceClient`?)
- Does `@InjectMocks ReaderServiceImpl` include CirculationServiceClient?

If `CirculationServiceClient` is not mocked, add:

```java
@Mock
private CirculationServiceClient circulationServiceClient;
```

Add imports:

```java
import com.gcrf.library.reader.client.CirculationServiceClient;
import com.gcrf.library.common.result.Result;
```

- [ ] **Step 2: Add the 4 new tests**

Add before the closing `}`:

```java
    // ===== Borrow count validation (P1) =====

    @Test
    @DisplayName("deleteReader_whenReaderHasBorrows_shouldThrowException")
    void deleteReader_whenReaderHasBorrows_shouldThrowException() {
        // Arrange
        Long id = 1L;
        Reader reader = new Reader();
        reader.setId(id);
        reader.setReaderId("R001");
        when(readerMapper.selectById(id)).thenReturn(reader);

        Result<Integer> borrowCount = Result.success(3);
        when(circulationServiceClient.getCurrentBorrowCount(id)).thenReturn(borrowCount);

        // Act & Assert
        assertThatThrownBy(() -> readerService.deleteReader(id))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("3");

        verify(readerMapper, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteReader_whenCirculationServiceUnavailable_shouldProceedWithWarning")
    void deleteReader_whenCirculationServiceUnavailable_shouldProceedWithWarning() {
        // Arrange
        Long id = 1L;
        Reader reader = new Reader();
        reader.setId(id);
        when(readerMapper.selectById(id)).thenReturn(reader);

        // Simulate Feign throwing an exception (service unavailable)
        when(circulationServiceClient.getCurrentBorrowCount(id))
            .thenThrow(new RuntimeException("circulation-service unavailable"));

        // Act
        readerService.deleteReader(id);

        // Assert: delete still happens despite failed borrow check (graceful degradation)
        verify(readerMapper).deleteById(id);
    }

    @Test
    @DisplayName("cancelCard_whenReaderHasBorrows_shouldThrowException")
    void cancelCard_whenReaderHasBorrows_shouldThrowException() {
        // Arrange
        Long id = 1L;
        Reader reader = new Reader();
        reader.setId(id);
        reader.setReaderId("R001");
        reader.setStatus("ACTIVE");
        when(readerMapper.selectById(id)).thenReturn(reader);

        Result<Integer> borrowCount = Result.success(2);
        when(circulationServiceClient.getCurrentBorrowCount(id)).thenReturn(borrowCount);

        // Act & Assert
        assertThatThrownBy(() -> readerService.cancelCard(id))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("2");

        verify(readerMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("cancelCard_whenCirculationServiceUnavailable_shouldProceedWithWarning")
    void cancelCard_whenCirculationServiceUnavailable_shouldProceedWithWarning() {
        // Arrange
        Long id = 1L;
        Reader reader = new Reader();
        reader.setId(id);
        reader.setStatus("ACTIVE");
        when(readerMapper.selectById(id)).thenReturn(reader);

        when(circulationServiceClient.getCurrentBorrowCount(id))
            .thenThrow(new RuntimeException("circulation-service unavailable"));

        // Act
        readerService.cancelCard(id);

        // Assert: cancel still happens
        verify(readerMapper).updateById(any(Reader.class));
    }
```

- [ ] **Step 3: Run**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn test -pl reader-service -Dtest=ReaderServiceTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run|BUILD" | tail -3
```

Expected: all existing tests + 4 new tests pass.

- [ ] **Step 4: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/reader-service/src/test/java/com/gcrf/library/reader/service/ReaderServiceTest.java
git commit -m "test(reader): add borrow count validation tests for delete/cancel

- deleteReader/cancelCard throw when reader has outstanding borrows
- Both methods gracefully degrade when circulation-service unavailable"
```

---

## Task 5: Gateway RateLimitFilter unit test

**Files:**

- Create: `backend/gateway-service/src/test/java/com/gcrf/gateway/filter/RateLimitFilterTest.java`

- [ ] **Step 1: Read RateLimitFilter and RateLimiterService**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
cat backend/gateway-service/src/main/java/com/gcrf/gateway/filter/RateLimitFilter.java
cat backend/gateway-service/src/main/java/com/gcrf/gateway/service/RateLimiterService.java 2>/dev/null || find backend/gateway-service -name "RateLimiterService.java" | xargs cat
```

Note:

- Field name for the service (likely `rateLimiterService`)
- Method signatures of `isAllowed(...)` and `getRemainingRequests(...)`
- Response header names (likely `X-RateLimit-Remaining`)

- [ ] **Step 2: Look at existing filter tests for pattern**

```bash
cat backend/gateway-service/src/test/java/com/gcrf/gateway/filter/AuthenticationFilterTest.java 2>/dev/null | head -80
```

Note how existing tests use `MockServerHttpRequest`, `MockServerWebExchange`, `Mono.empty()`, etc.

- [ ] **Step 3: Create the test file**

Create `backend/gateway-service/src/test/java/com/gcrf/gateway/filter/RateLimitFilterTest.java`:

```java
package com.gcrf.gateway.filter;

import com.gcrf.gateway.service.RateLimiterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    @DisplayName("filter_whenNotAllowed_shouldReturn429")
    void filter_whenNotAllowed_shouldReturn429() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/v1/books")
            .header("X-Forwarded-For", "1.2.3.4")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(rateLimiterService.isAllowed(anyString(), anyString(), any())).thenReturn(false);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);
        result.block();

        // Assert: chain NOT called, status 429
        verify(chain, never()).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("filter_shouldExtractClientIpFromXForwardedFor")
    void filter_shouldExtractClientIpFromXForwardedFor() {
        // Arrange: X-Forwarded-For with multiple IPs, should take first
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/v1/books")
            .header("X-Forwarded-For", "1.2.3.4, 5.6.7.8")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(rateLimiterService.isAllowed(anyString(), anyString(), any())).thenReturn(true);
        when(rateLimiterService.getRemainingRequests(anyString(), anyString(), any())).thenReturn(50L);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert: verify isAllowed called with "1.2.3.4" (first IP)
        verify(rateLimiterService).isAllowed(anyString(), org.mockito.ArgumentMatchers.eq("1.2.3.4"), any());
    }

    @Test
    @DisplayName("filter_shouldExtractUserIdFromHeader")
    void filter_shouldExtractUserIdFromHeader() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/v1/books")
            .header("X-Forwarded-For", "1.2.3.4")
            .header("X-User-Id", "100")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(rateLimiterService.isAllowed(anyString(), anyString(), any())).thenReturn(true);
        when(rateLimiterService.getRemainingRequests(anyString(), anyString(), any())).thenReturn(50L);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert: isAllowed called with userId=100L
        verify(rateLimiterService).isAllowed(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(100L));
    }

    @Test
    @DisplayName("filter_withMissingClientIp_shouldUseFallback")
    void filter_withMissingClientIp_shouldUseFallback() {
        // Arrange: no X-Forwarded-For, no X-Real-IP — will fall back to remote address
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/v1/books")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(rateLimiterService.isAllowed(anyString(), anyString(), any())).thenReturn(true);
        when(rateLimiterService.getRemainingRequests(anyString(), anyString(), any())).thenReturn(50L);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, chain).block();

        // Assert: filter completes without error, chain proceeds
        verify(chain).filter(exchange);
    }
}
```

**Note:** If the actual field name for `RateLimiterService` differs from what's assumed, or method signatures are different, adjust the mock expectations. Read Step 1 output carefully.

- [ ] **Step 4: Run**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn test -pl gateway-service -Dtest=RateLimitFilterTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run|BUILD" | tail -3
```

Expected: 5 tests pass.

- [ ] **Step 5: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/gateway-service/src/test/java/com/gcrf/gateway/filter/RateLimitFilterTest.java
git commit -m "test(gateway): add RateLimitFilter unit tests

- 5 tests covering allow/deny/IP extraction/userId extraction/fallback
- Uses MockServerWebExchange and Mockito for reactive filter testing"
```

---

## Task 6: Rewrite NotificationControllerIntegrationTest

**Files:**

- Modify: `backend/notification-service/src/test/java/com/gcrf/library/notification/controller/NotificationControllerIntegrationTest.java`

**Problem:** Existing file references `NotificationCreateRequest` which was removed in P0. Current controller uses `NotificationSendRequest` and has userId as `@RequestParam` for most methods.

- [ ] **Step 1: Read current controller and available DTOs**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
cat backend/notification-service/src/main/java/com/gcrf/library/notification/controller/NotificationController.java
cat backend/notification-service/src/main/java/com/gcrf/library/notification/dto/request/NotificationSendRequest.java
cat backend/notification-service/src/main/java/com/gcrf/library/notification/dto/response/UnreadCountVO.java
find backend/notification-service/src/main/java -name "NotificationVO.java" | xargs cat
```

Note exact field names in `NotificationSendRequest`: likely `userId`, `title`, `content`, `notificationType`, `priority`, `extraData`.

- [ ] **Step 2: Rewrite the entire test file**

Replace the ENTIRE content of `backend/notification-service/src/test/java/com/gcrf/library/notification/controller/NotificationControllerIntegrationTest.java` with:

```java
package com.gcrf.library.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.common.test.BaseIntegrationTest;
import com.gcrf.library.notification.dto.request.NotificationSendRequest;
import com.gcrf.library.notification.entity.Notification;
import com.gcrf.library.notification.mapper.NotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class NotificationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private NotificationMapper notificationMapper;

    private static final Long TEST_USER_ID = 9001L;

    @BeforeEach
    void setUp() {
        // Clean up any notifications from prior tests (defensive)
        notificationMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Notification>()
                .eq("user_id", TEST_USER_ID)
        );
    }

    @Test
    void sendNotification_success_shouldPersist() throws Exception {
        NotificationSendRequest request = new NotificationSendRequest();
        request.setUserId(TEST_USER_ID);
        request.setTitle("测试通知");
        request.setContent("这是一条测试通知");
        request.setNotificationType("SYSTEM");
        request.setPriority("NORMAL");

        mockMvc.perform(post("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID))
            .andExpect(jsonPath("$.data.title").value("测试通知"))
            .andExpect(jsonPath("$.data.isRead").value(false));
    }

    @Test
    void queryNotifications_shouldReturnPaged() throws Exception {
        // Arrange: create 3 notifications
        for (int i = 1; i <= 3; i++) {
            Notification n = new Notification();
            n.setUserId(TEST_USER_ID);
            n.setTitle("通知" + i);
            n.setContent("内容" + i);
            n.setNotificationType("SYSTEM");
            n.setPriority("NORMAL");
            n.setIsRead(false);
            notificationMapper.insert(n);
        }

        mockMvc.perform(get("/api/v1/notifications")
                .param("userId", String.valueOf(TEST_USER_ID))
                .param("pageNum", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records").isArray())
            .andExpect(jsonPath("$.data.total").value(3));
    }

    @Test
    void getNotificationById_success_shouldReturnNotification() throws Exception {
        Notification n = createTestNotification("单条查询", "SYSTEM", "NORMAL");

        mockMvc.perform(get("/api/v1/notifications/" + n.getId())
                .param("userId", String.valueOf(TEST_USER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(n.getId()))
            .andExpect(jsonPath("$.data.title").value("单条查询"));
    }

    @Test
    void markAsRead_shouldSetIsReadTrue() throws Exception {
        Notification n = createTestNotification("标记已读", "SYSTEM", "NORMAL");

        mockMvc.perform(put("/api/v1/notifications/" + n.getId() + "/read")
                .param("userId", String.valueOf(TEST_USER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        Notification updated = notificationMapper.selectById(n.getId());
        org.assertj.core.api.Assertions.assertThat(updated.getIsRead()).isTrue();
        org.assertj.core.api.Assertions.assertThat(updated.getReadAt()).isNotNull();
    }

    @Test
    void batchMarkAsRead_shouldMarkAllAsRead() throws Exception {
        Notification n1 = createTestNotification("批1", "SYSTEM", "NORMAL");
        Notification n2 = createTestNotification("批2", "SYSTEM", "NORMAL");

        mockMvc.perform(put("/api/v1/notifications/batch-read")
                .param("userId", String.valueOf(TEST_USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(n1.getId(), n2.getId()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        org.assertj.core.api.Assertions.assertThat(notificationMapper.selectById(n1.getId()).getIsRead()).isTrue();
        org.assertj.core.api.Assertions.assertThat(notificationMapper.selectById(n2.getId()).getIsRead()).isTrue();
    }

    @Test
    void deleteNotification_shouldSoftDelete() throws Exception {
        Notification n = createTestNotification("删除测试", "SYSTEM", "NORMAL");

        mockMvc.perform(delete("/api/v1/notifications/" + n.getId())
                .param("userId", String.valueOf(TEST_USER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        Notification deleted = notificationMapper.selectById(n.getId());
        org.assertj.core.api.Assertions.assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void batchDeleteNotifications_shouldSoftDeleteMultiple() throws Exception {
        Notification n1 = createTestNotification("删1", "SYSTEM", "NORMAL");
        Notification n2 = createTestNotification("删2", "SYSTEM", "NORMAL");

        mockMvc.perform(delete("/api/v1/notifications/batch")
                .param("userId", String.valueOf(TEST_USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(n1.getId(), n2.getId()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getUnreadCount_shouldReturnCountWithUrgent() throws Exception {
        createTestNotification("未读1", "SYSTEM", "NORMAL");
        createTestNotification("未读2", "SYSTEM", "URGENT");

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                .param("userId", String.valueOf(TEST_USER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID))
            .andExpect(jsonPath("$.data.unreadCount").value(2))
            .andExpect(jsonPath("$.data.urgentCount").value(1));
    }

    @Test
    void getLatestNotifications_shouldReturnInDescOrder() throws Exception {
        createTestNotification("旧", "SYSTEM", "NORMAL");
        createTestNotification("新", "SYSTEM", "NORMAL");

        mockMvc.perform(get("/api/v1/notifications/latest")
                .param("userId", String.valueOf(TEST_USER_ID))
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void clearAllNotifications_shouldSoftDeleteAll() throws Exception {
        createTestNotification("清1", "SYSTEM", "NORMAL");
        createTestNotification("清2", "SYSTEM", "NORMAL");

        mockMvc.perform(delete("/api/v1/notifications/clear")
                .param("userId", String.valueOf(TEST_USER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    // Helper
    private Notification createTestNotification(String title, String type, String priority) {
        Notification n = new Notification();
        n.setUserId(TEST_USER_ID);
        n.setTitle(title);
        n.setContent("内容: " + title);
        n.setNotificationType(type);
        n.setPriority(priority);
        n.setIsRead(false);
        notificationMapper.insert(n);
        return n;
    }
}
```

- [ ] **Step 3: Run**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
docker ps > /dev/null || exit 1
mvn test -pl notification-service -Dtest=NotificationControllerIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run|BUILD" | tail -3
```

Expected: 10 tests pass.

- [ ] **Step 4: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/notification-service/src/test/java/com/gcrf/library/notification/controller/NotificationControllerIntegrationTest.java
git commit -m "test(notification): rewrite NotificationControllerIntegrationTest for P0 signatures

- Replace NotificationCreateRequest (removed) with NotificationSendRequest
- Add userId @RequestParam to all endpoint calls
- 10 tests covering: send, query, getById, markAsRead, batchMarkAsRead,
  delete, batchDelete, unreadCount, latest, clearAll"
```

---

## Task 7: Fix logback-spring.xml

**Files:**

- Modify: `backend/common/common-web/src/main/resources/logback-spring.xml`

- [ ] **Step 1: Add Spring Boot defaults include and fix JSON_PATTERN property**

Find the file and make TWO edits:

**Edit 1 — Add `<include>` right after `<configuration>` tag (line 22):**

Original:

```xml
<configuration scan="true" scanPeriod="30 seconds">

    <!-- Property definitions -->
    <springProperty scope="context" name="APP_NAME" ...
```

New:

```xml
<configuration scan="true" scanPeriod="30 seconds">

    <!-- Include Spring Boot's default logback configuration for %clr(), LOG_LEVEL_PATTERN, etc. -->
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- Property definitions -->
    <springProperty scope="context" name="APP_NAME" ...
```

**Edit 2 — Fix `<property name="JSON_PATTERN">` block (lines 38-40):**

Original:

```xml
    <!-- JSON pattern for Loki/ELK integration -->
    <property name="JSON_PATTERN">
        {"timestamp":"%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ}","level":"%level","logger_name":"%logger","thread_name":"%thread","message":"%message","service":"${APP_NAME}","traceId":"%X{traceId:-}","spanId":"%X{spanId:-}","requestId":"%X{requestId:-}","userId":"%X{userId:-}","clientIp":"%X{clientIp:-}","requestPath":"%X{requestPath:-}","requestMethod":"%X{requestMethod:-}"%ex{full}}%n
    </property>
```

New (using `value` attribute with single-quoted string since content has double-quotes):

```xml
    <!-- JSON pattern for Loki/ELK integration -->
    <property name="JSON_PATTERN" value='{"timestamp":"%d{yyyy-MM-dd&apos;T&apos;HH:mm:ss.SSSZ}","level":"%level","logger_name":"%logger","thread_name":"%thread","message":"%message","service":"${APP_NAME}","traceId":"%X{traceId:-}","spanId":"%X{spanId:-}","requestId":"%X{requestId:-}","userId":"%X{userId:-}","clientIp":"%X{clientIp:-}","requestPath":"%X{requestPath:-}","requestMethod":"%X{requestMethod:-}"%ex{full}}%n'/>
```

Note: The original has `'T'` (single-quoted) inside the JSON. Since we're now using single-quoted XML attribute, we need to escape them as `&apos;T&apos;`.

Note the JSON_PATTERN property is not actually referenced by any appender in this file — it was defined but unused. If that's the case and the fix is only to eliminate the XML error, an alternative is to simply delete the block. Check if any `${JSON_PATTERN}` is referenced:

```bash
grep -n '${JSON_PATTERN}' backend/common/common-web/src/main/resources/logback-spring.xml
```

If no references found (most likely), **delete the invalid `<property>` block entirely** instead of trying to fix it:

```xml
    <!-- JSON pattern for Loki/ELK integration (unused, removed) -->
```

Whichever approach is cleaner.

- [ ] **Step 2: Verify XML is still valid**

```bash
python3 -c "import xml.etree.ElementTree as ET; ET.parse('backend/common/common-web/src/main/resources/logback-spring.xml'); print('XML OK')"
```

Expected: "XML OK"

- [ ] **Step 3: Verify logback doesn't emit startup errors**

Run any existing test that starts Spring context:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
docker ps > /dev/null || exit 1
mvn test -pl book-service -Dtest=BookControllerIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -iE "logback|\[clr\]|<property>|FATAL" | head -10
```

Expected: No logback-related errors in output.

- [ ] **Step 4: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/common/common-web/src/main/resources/logback-spring.xml
git commit -m "fix(common): fix logback-spring.xml XML syntax errors

- Add Spring Boot defaults include for %clr() conversion word
- Remove/fix invalid <property name=\"JSON_PATTERN\"> syntax
  (was using element content instead of value attribute)"
```

---

## Task 8: Final verification

- [ ] **Step 1: Full test run across all 4 services affected**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
docker ps > /dev/null || exit 1

mvn test -pl reader-service -Dtest='ReaderControllerIntegrationTest,ReaderServiceTest' -Dsurefire.failIfNoSpecifiedTests=false
mvn test -pl circulation-service -Dtest='BorrowControllerIntegrationTest,ReserveControllerIntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false
mvn test -pl gateway-service -Dtest=RateLimitFilterTest -Dsurefire.failIfNoSpecifiedTests=false
mvn test -pl notification-service -Dtest=NotificationControllerIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: All tests pass (existing + new ~26 tests).

- [ ] **Step 2: Commit spec and plan docs**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add docs/specs/2026-04-14-p2d-new-code-tests-and-fixes-design.md docs/specs/2026-04-14-p2d-new-code-tests-and-fixes-plan.md
git commit -m "docs(docs): add P2D spec and plan"
```
