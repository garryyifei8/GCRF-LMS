# Common-Feign Module

## Overview

The `common-feign` module provides a centralized solution for inter-service communication in the GCRF Library Management System. It includes:

- Feign client interfaces for all services
- Request/response interceptors
- Sentinel circuit breaker integration
- Fallback factories for graceful degradation
- Common DTOs for service communication

## Architecture

```
common-feign/
├── src/main/java/com/gcrf/library/common/feign/
│   ├── client/                 # Feign client interfaces
│   │   ├── AuthServiceClient.java
│   │   ├── BookServiceClient.java
│   │   ├── CirculationServiceClient.java
│   │   └── ReaderServiceClient.java
│   ├── config/                 # Configuration classes
│   │   ├── FeignAutoConfiguration.java
│   │   ├── FeignErrorDecoder.java
│   │   ├── FeignProperties.java
│   │   ├── OkHttpConfiguration.java
│   │   ├── SentinelConfiguration.java
│   │   └── SentinelRuleInitializer.java
│   ├── constant/               # Constants
│   │   └── FeignConstants.java
│   ├── dto/                    # Data Transfer Objects
│   │   ├── BookDTO.java
│   │   ├── CirculationRecordDTO.java
│   │   ├── ReaderDTO.java
│   │   └── UserDTO.java
│   ├── fallback/               # Fallback factories
│   │   ├── AuthServiceFallbackFactory.java
│   │   ├── BookServiceFallbackFactory.java
│   │   ├── CirculationServiceFallbackFactory.java
│   │   └── ReaderServiceFallbackFactory.java
│   └── interceptor/            # Interceptors
│       ├── FeignRequestInterceptor.java
│       └── FeignResponseInterceptor.java
└── src/main/resources/
    └── META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## Usage

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.gcrf.library</groupId>
    <artifactId>common-feign</artifactId>
</dependency>
```

### 2. Enable Feign Clients

```java
@SpringBootApplication
@EnableFeignClients(basePackages = "com.gcrf.library.common.feign.client")
public class YourServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourServiceApplication.class, args);
    }
}
```

### 3. Inject and Use Client

```java
@Service
@RequiredArgsConstructor
public class YourService {

    private final BookServiceClient bookServiceClient;

    public void doSomething(Long bookId) {
        Result<BookDTO> result = bookServiceClient.getBookById(bookId);
        if (result.isSuccess()) {
            BookDTO book = result.getData();
            // Process book data
        }
    }
}
```

## Configuration

### Application Properties

```yaml
library:
  feign:
    enabled: true
    # Log level: NONE, BASIC, HEADERS, FULL
    logger-level: BASIC
    # Connection timeout (ms)
    connect-timeout: 5000
    # Read timeout (ms)
    read-timeout: 10000
    # Forward auth header
    forward-auth: true
    # Forward trace id
    forward-trace-id: true

    # Retry configuration
    retry:
      enabled: true
      max-attempts: 3
      period: 100
      max-period: 1000

    # Sentinel configuration
    sentinel:
      enabled: true
      # Slow call threshold (ms)
      slow-call-threshold: 3000
      # Slow call ratio threshold
      slow-call-ratio-threshold: 0.5
      # Error ratio threshold
      error-ratio-threshold: 0.5
      # Circuit breaker timeout (seconds)
      circuit-breaker-timeout: 10
      # Minimum request amount
      min-request-amount: 5
      # Statistics interval (ms)
      stat-interval-ms: 10000
```

### OkHttp Configuration

The module uses OkHttp as the HTTP client for better performance:

- Connection pool: 200 max idle connections, 5 minutes keep-alive
- Retry on connection failure enabled
- SSL redirect disabled (handled by Feign)

## Feign Clients

### BookServiceClient

| Method | Path | Description |
|--------|------|-------------|
| `getBookById(Long)` | GET /{bookId} | Get book by ID |
| `getBookByIsbn(String)` | GET /isbn/{isbn} | Get book by ISBN |
| `checkAvailability(Long)` | GET /{bookId}/availability | Check if book is available |
| `decreaseAvailableCopies(Long)` | POST /{bookId}/decrease-copies | Decrease available copies |
| `increaseAvailableCopies(Long)` | POST /{bookId}/increase-copies | Increase available copies |
| `getBooksByIds(String)` | GET /batch | Batch get books |
| `queryBooks(...)` | GET / | Query books with pagination |

### ReaderServiceClient

| Method | Path | Description |
|--------|------|-------------|
| `getReaderById(Long)` | GET /{readerId} | Get reader by ID |
| `getReaderByReaderId(String)` | GET /readerId/{readerNo} | Get reader by reader number |
| `canBorrow(Long)` | GET /{readerId}/can-borrow | Check if reader can borrow |
| `getCurrentBorrowCount(Long)` | GET /{readerId}/borrow-count | Get current borrow count |
| `hasOverdueBooks(Long)` | GET /{readerId}/has-overdue | Check for overdue books |
| `getReadersByIds(String)` | GET /batch | Batch get readers |
| `queryReaders(...)` | GET / | Query readers with pagination |

### CirculationServiceClient

| Method | Path | Description |
|--------|------|-------------|
| `getRecordById(Long)` | GET /{recordId} | Get circulation record |
| `getReaderRecords(...)` | GET /reader/{readerId} | Get reader's records |
| `getBookRecords(...)` | GET /book/{bookId} | Get book's records |
| `getCurrentBorrowCount(Long)` | GET /reader/{readerId}/borrow-count | Get current borrow count |
| `hasOverdueBooks(Long)` | GET /reader/{readerId}/has-overdue | Check for overdue |
| `getOverdueRecords(Long)` | GET /reader/{readerId}/overdue | Get overdue records |
| `isBookBorrowed(Long)` | GET /book/{bookId}/is-borrowed | Check if book is borrowed |

### AuthServiceClient

| Method | Path | Description |
|--------|------|-------------|
| `validateToken(String)` | GET /validate | Validate JWT token |
| `getCurrentUserId(String)` | GET /current-user | Get current user ID |
| `getUserInfo(String)` | GET /info | Get current user info |
| `getUserById(Long)` | GET /users/{userId} | Get user by ID |
| `getUsersByIds(String)` | GET /users/batch | Batch get users |
| `hasPermission(...)` | GET /users/{userId}/has-permission | Check permission |
| `hasRole(...)` | GET /users/{userId}/has-role | Check role |

## Circuit Breaker (Sentinel)

### Flow Control Rules

Each service has a default flow control rule:
- QPS limit: 100 requests/second
- Control behavior: Fast fail

### Degrade Rules

Three types of circuit breaker rules are configured:

1. **Slow Call Ratio**
   - Threshold: 3000ms
   - Ratio: 50%
   - Timeout: 10s

2. **Error Ratio**
   - Threshold: 50%
   - Timeout: 10s

3. **Error Count**
   - Threshold: 5 errors in 60s
   - Timeout: 10s

### Fallback Strategy

Each client has a fallback factory that provides graceful degradation:

- **Read operations**: Return empty data or cached data
- **Write operations**: Return error response
- **Security-critical operations**: Fail safe (deny by default)

## Request Interceptor

The `FeignRequestInterceptor` automatically forwards:

- `Authorization` header (Bearer token)
- `X-User-Id` header
- `X-Username` header
- `X-Tenant-Id` header
- `X-Trace-Id` header (generated if not present)
- `X-Request-Source: feign` (marks inter-service calls)

## Error Handling

The `FeignErrorDecoder` handles HTTP errors:

- **4xx errors**: Throws `BusinessException`
- **5xx errors**: Throws `SystemException`
- Attempts to parse error response body for detailed message

## Best Practices

1. **Always check result status**
   ```java
   Result<BookDTO> result = bookServiceClient.getBookById(id);
   if (!result.isSuccess()) {
       // Handle error
       throw new BusinessException(result.getCode(), result.getMessage());
   }
   BookDTO book = result.getData();
   ```

2. **Use unwrap helper**
   ```java
   BookDTO book = FeignResponseInterceptor.unwrap(
       bookServiceClient.getBookById(id)
   );
   ```

3. **Handle fallback gracefully**
   ```java
   Result<BookDTO> result = bookServiceClient.getBookById(id);
   BookDTO book = FeignResponseInterceptor.getOrDefault(result, defaultBook);
   ```

4. **Configure appropriate timeouts** for long-running operations

5. **Monitor circuit breaker status** via Sentinel dashboard

## Dependencies

- Spring Cloud OpenFeign
- Spring Cloud LoadBalancer
- Sentinel (Alibaba)
- OkHttp
- common-core (Result, Exception classes)
