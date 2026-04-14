# P2C: 6 个 Controller Integration Tests 补全设计

**日期：** 2026-04-14
**状态：** Approved
**优先级：** P2（测试覆盖补全）
**背景：** 生产就绪审计发现 6 个 Controller 缺少 integration tests。P2C 在 P2A 建立的 Testcontainers 基础上补齐覆盖。

---

## 目标

为以下 6 个 Controller 创建 integration tests：

1. FineController（circulation-service）
2. DepartmentController（system-service）
3. ReaderTypeController（reader-service）
4. NotificationTemplateController（notification-service）
5. SubscriptionController（notification-service）
6. WebSocketNotificationController（notification-service）— 含 STOMP 测试

---

## 共同测试模式

所有 integration test 继承 P2A 建立的 `BaseIntegrationTest`，使用 Testcontainers + `@Transactional` 回滚。

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class XxxControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired XxxMapper xxxMapper;  // For direct data setup/verification

    @BeforeEach
    void setUp() {
        // Create test data via mapper
    }

    @Test
    void endpoint_scenario_expectedBehavior() throws Exception {
        mockMvc.perform(post("/api/v1/xxx")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.field").value(expected));
    }
}
```

---

## Test 1: FineControllerIntegrationTest

**文件：** `backend/circulation-service/src/test/java/com/gcrf/library/circulation/controller/FineControllerIntegrationTest.java`

**端点：** `/api/v1/fines` (circulation-service, 7 endpoints)

**测试用例（~8）：**

| #   | 测试                                                 | 覆盖                                |
| --- | ---------------------------------------------------- | ----------------------------------- |
| 1   | `queryFines_withFilters_shouldReturnPaged`           | `GET /` + 分页 + 过滤               |
| 2   | `queryOverdueRecords_shouldReturnOverdueFines`       | `GET /overdue`                      |
| 3   | `calculateFine_forOverdueBorrow_shouldReturnAmount`  | `POST /calculate/{borrowId}` 有欠款 |
| 4   | `calculateFine_forNonOverdueBorrow_shouldReturnZero` | `POST /calculate/{borrowId}` 无欠款 |
| 5   | `payFine_success_shouldMarkAsPaid`                   | `POST /pay`                         |
| 6   | `payFine_forNonExistentBorrow_shouldReturn400`       | `POST /pay` 错误情况                |
| 7   | `batchReturn_shouldProcessMultipleBorrows`           | `POST /batch-return`                |
| 8   | `getFineStatistics_shouldReturnAggregation`          | `GET /statistics`                   |
| 9   | `health_shouldReturn200`                             | `GET /health`                       |

**依赖：** BorrowMapper 注入用于创建测试借阅记录。

---

## Test 2: DepartmentControllerIntegrationTest

**文件：** `backend/system-service/src/test/java/com/gcrf/library/system/controller/DepartmentControllerIntegrationTest.java`

**端点：** `/api/v1/system/departments` (system-service, 5 endpoints)

**测试用例（~10）：**

| #   | 测试                                                 | 覆盖                     |
| --- | ---------------------------------------------------- | ------------------------ |
| 1   | `createDepartment_success_shouldReturn200AndPersist` | `POST /` 成功            |
| 2   | `createDepartment_withDuplicateCode_shouldReturn500` | `POST /` 重复 code       |
| 3   | `createDepartment_withInvalidData_shouldReturn400`   | `POST /` validation 错误 |
| 4   | `updateDepartment_success_shouldUpdateFields`        | `PUT /` 成功             |
| 5   | `updateDepartment_whenNotFound_shouldReturn500`      | `PUT /` not found        |
| 6   | `getDepartmentById_success_shouldReturnDepartment`   | `GET /{id}` 成功         |
| 7   | `getDepartmentById_whenNotFound_shouldReturn500`     | `GET /{id}` not found    |
| 8   | `queryDepartments_withFilters_shouldReturnFiltered`  | `GET /` 过滤             |
| 9   | `queryDepartments_withPagination_shouldReturnPaged`  | `GET /` 分页             |
| 10  | `deleteDepartment_success_shouldRemove`              | `DELETE /{id}` 成功      |
| 11  | `deleteDepartment_whenHasChildren_shouldReturn500`   | `DELETE /{id}` 有子部门  |

**依赖：** DepartmentMapper 注入。

---

## Test 3: ReaderTypeControllerIntegrationTest

**文件：** `backend/reader-service/src/test/java/com/gcrf/library/reader/controller/ReaderTypeControllerIntegrationTest.java`

**端点：** `/api/v1/readers/types` (reader-service, 5 endpoints)

**测试用例（~8）：**

| #   | 测试                                           | 覆盖                  |
| --- | ---------------------------------------------- | --------------------- |
| 1   | `listAllTypes_shouldReturnAllTypes`            | `GET /` 列表          |
| 2   | `getTypeById_success_shouldReturnType`         | `GET /{id}` 成功      |
| 3   | `getTypeById_whenNotFound_shouldReturn500`     | `GET /{id}` not found |
| 4   | `createType_success_shouldPersist`             | `POST /` 成功         |
| 5   | `createType_withDuplicateCode_shouldReturn500` | `POST /` 重复         |
| 6   | `createType_withInvalidData_shouldReturn400`   | `POST /` validation   |
| 7   | `updateType_success_shouldUpdateFields`        | `PUT /{id}` 成功      |
| 8   | `deleteType_success_shouldSoftDelete`          | `DELETE /{id}` 成功   |

**依赖：** ReaderTypeMapper 注入。默认的 4 个 reader_types 在 Flyway V001 baseline 中已初始化（STUDENT/TEACHER/STAFF/EXTERNAL）。

---

## Test 4: NotificationTemplateControllerIntegrationTest

**文件：** `backend/notification-service/src/test/java/com/gcrf/library/notification/controller/NotificationTemplateControllerIntegrationTest.java`

**端点：** `/api/v1/notification-templates` (notification-service, 8 endpoints)

**测试用例（~12）：**

| #   | 测试                                                       | 覆盖                             |
| --- | ---------------------------------------------------------- | -------------------------------- |
| 1   | `createTemplate_success_shouldPersist`                     | `POST /` 成功                    |
| 2   | `createTemplate_withDuplicateCode_shouldReturn500`         | `POST /` 重复 code               |
| 3   | `updateTemplate_success_shouldUpdate`                      | `PUT /{id}` 成功                 |
| 4   | `updateTemplate_whenNotFound_shouldReturn500`              | `PUT /{id}` not found            |
| 5   | `deleteTemplate_success_shouldSoftDelete`                  | `DELETE /{id}` 成功              |
| 6   | `getTemplateById_success_shouldReturn`                     | `GET /{id}` 成功                 |
| 7   | `getTemplateById_whenNotFound_shouldReturn500`             | `GET /{id}` not found            |
| 8   | `getTemplateByCode_success_shouldReturn`                   | `GET /by-code/{code}` 成功       |
| 9   | `queryTemplates_withPagination_shouldReturnPaged`          | `GET /` 分页                     |
| 10  | `queryTemplates_withTypeFilter_shouldFilter`               | `GET /` 按类型过滤               |
| 11  | `changeTemplateStatus_toInactive_shouldUpdate`             | `PUT /{id}/status?enabled=false` |
| 12  | `renderTemplate_withVariables_shouldReturnRenderedContent` | `POST /{id}/render`              |

**依赖：** NotificationTemplateMapper 注入。Flyway V001 中的 5 个默认模板（WELCOME/VERIFICATION_CODE/BORROW_REMINDER/RESERVE_SUCCESS/OVERDUE_NOTICE）已初始化。

---

## Test 5: SubscriptionControllerIntegrationTest

**文件：** `backend/notification-service/src/test/java/com/gcrf/library/notification/controller/SubscriptionControllerIntegrationTest.java`

**端点：** `/api/v1/subscriptions` (notification-service, 3 endpoints)

**测试用例（~6）：**

| #   | 测试                                                     | 覆盖                                              |
| --- | -------------------------------------------------------- | ------------------------------------------------- |
| 1   | `getUserSubscription_whenNotExists_shouldReturnDefaults` | `GET /user/{userId}` — 首次请求返回默认订阅       |
| 2   | `getUserSubscription_whenExists_shouldReturnStored`      | `GET /user/{userId}` — 存在的订阅                 |
| 3   | `updateSubscription_success_shouldPersist`               | `PUT /user/{userId}` 成功                         |
| 4   | `updateSubscription_withInvalidData_shouldReturn400`     | `PUT /user/{userId}` validation                   |
| 5   | `checkSubscription_whenSubscribed_shouldReturnTrue`      | `GET /check?userId=X&notificationType=Y` — 订阅   |
| 6   | `checkSubscription_whenNotSubscribed_shouldReturnFalse`  | `GET /check?userId=X&notificationType=Y` — 未订阅 |

**依赖：** NotificationSubscriptionMapper 注入。

---

## Test 6: WebSocketNotificationControllerTest — STOMP 完整测试

**文件：** `backend/notification-service/src/test/java/com/gcrf/library/notification/controller/WebSocketNotificationControllerTest.java`

**端点：**

- REST: `GET /api/v1/ws/stats`, `GET /api/v1/ws/online/{userId}`
- STOMP: `/app/notifications`, `/app/ping`, `/app/push`

**特殊配置：**

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WebSocketNotificationControllerTest extends BaseIntegrationTest {
    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    private WebSocketStompClient stompClient;
    private StompSession session;

    @BeforeEach
    void setupStompClient() throws Exception {
        List<Transport> transports = List.of(
            new WebSocketTransport(new StandardWebSocketClient())
        );
        SockJsClient sockJsClient = new SockJsClient(transports);
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        session = stompClient.connectAsync(
            "ws://localhost:" + port + "/ws/notifications",
            new StompSessionHandlerAdapter() {}
        ).get(3, TimeUnit.SECONDS);
    }

    @AfterEach
    void disconnectStomp() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
}
```

**注意事项：**

1. 必须用 `WebEnvironment.RANDOM_PORT` — MockMvc 不支持 WebSocket，需要真实 server
2. 这个测试类无法用 `@AutoConfigureMockMvc`（与 RANDOM_PORT 冲突），改用 `TestRestTemplate` 测 REST
3. STOMP 订阅需要 `CompletableFuture` 接收异步消息

**测试用例（~7）：**

### REST 部分（3）

| #   | 测试                                            | 覆盖                             |
| --- | ----------------------------------------------- | -------------------------------- |
| 1   | `getWebSocketStats_shouldReturnOnlineCount`     | `GET /api/v1/ws/stats`           |
| 2   | `checkUserOnline_whenOnline_shouldReturnTrue`   | `GET /api/v1/ws/online/{userId}` |
| 3   | `checkUserOnline_whenOffline_shouldReturnFalse` | `GET /api/v1/ws/online/{userId}` |

### STOMP 部分（4）

| #   | 测试                                                    | 覆盖                                                         |
| --- | ------------------------------------------------------- | ------------------------------------------------------------ |
| 4   | `handleSubscribe_shouldReceiveSubscriptionConfirmation` | 订阅 `/app/notifications` 返回确认                           |
| 5   | `handlePing_shouldReceivePongResponse`                  | 发 `/app/ping`，从 `/topic/pong` 收 echo                     |
| 6   | `handlePushToUser_shouldReceiveOnUserQueue`             | 发 `/app/push` 到特定用户，从 `/user/queue/notifications` 收 |
| 7   | `handlePushToTopic_shouldReceiveOnTopic`                | 发 `/app/push` broadcast，从 `/topic/notifications` 收       |

**STOMP 测试示例代码：**

```java
@Test
void handlePing_shouldReceivePongResponse() throws Exception {
    // Arrange — subscribe to /topic/pong
    CompletableFuture<Map> pongFuture = new CompletableFuture<>();
    session.subscribe("/topic/pong", new StompFrameHandler() {
        @Override public Type getPayloadType(StompHeaders headers) { return Map.class; }
        @Override public void handleFrame(StompHeaders headers, Object payload) {
            pongFuture.complete((Map) payload);
        }
    });

    // Act — send ping
    session.send("/app/ping", Map.of("clientTime", System.currentTimeMillis()));

    // Assert — pong received within 3s
    Map pong = pongFuture.get(3, TimeUnit.SECONDS);
    assertThat(pong).containsKey("serverTime");
}
```

---

## 修改文件清单

### 新建（6 个）

| 文件                                                                                                                                     | 测试数量 |
| ---------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| `backend/circulation-service/src/test/java/com/gcrf/library/circulation/controller/FineControllerIntegrationTest.java`                   | ~9       |
| `backend/system-service/src/test/java/com/gcrf/library/system/controller/DepartmentControllerIntegrationTest.java`                       | ~11      |
| `backend/reader-service/src/test/java/com/gcrf/library/reader/controller/ReaderTypeControllerIntegrationTest.java`                       | ~8       |
| `backend/notification-service/src/test/java/com/gcrf/library/notification/controller/NotificationTemplateControllerIntegrationTest.java` | ~12      |
| `backend/notification-service/src/test/java/com/gcrf/library/notification/controller/SubscriptionControllerIntegrationTest.java`         | ~6       |
| `backend/notification-service/src/test/java/com/gcrf/library/notification/controller/WebSocketNotificationControllerTest.java`           | ~7       |

**总计：~53 个测试用例**

### 无源码修改

P2C 仅添加测试文件。

---

## 验证方法

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend

docker ps > /dev/null || (echo "Docker not running" && exit 1)

mvn test -pl circulation-service -Dtest=FineControllerIntegrationTest
mvn test -pl system-service -Dtest=DepartmentControllerIntegrationTest
mvn test -pl reader-service -Dtest=ReaderTypeControllerIntegrationTest
mvn test -pl notification-service \
  -Dtest='NotificationTemplateControllerIntegrationTest,SubscriptionControllerIntegrationTest,WebSocketNotificationControllerTest'
```

**预期：** 53 个测试全部通过。
