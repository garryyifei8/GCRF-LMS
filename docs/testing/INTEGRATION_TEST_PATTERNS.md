# Integration Test Patterns - GCRF Library Management System

**Version**: 1.0
**Date**: 2025-11-01
**Stage**: Stage 14 - Cross-Service Integration Tests

---

## Table of Contents

1. [Overview](#overview)
2. [Testing Strategy](#testing-strategy)
3. [Test Pyramid](#test-pyramid)
4. [Gateway Integration Patterns](#gateway-integration-patterns)
5. [JWT Testing Patterns](#jwt-testing-patterns)
6. [Common Pitfalls](#common-pitfalls)
7. [Best Practices](#best-practices)
8. [Example Tests](#example-tests)

---

## Overview

This document provides comprehensive patterns and best practices for writing integration tests in the GCRF Library Management System. It is based on real implementations from Stage 13 (Auth Service) and Stage 14 (Cross-Service Integration Tests).

### What are Integration Tests?

**Integration Tests** validate that multiple components work together correctly. In a microservices architecture, this includes:
- Service-to-service communication
- Gateway routing and filtering
- JWT token validation across services
- Database interactions
- External service integrations

### When to Write Integration Tests?

Write integration tests when:
- ✅ Testing interaction between multiple components
- ✅ Validating API Gateway filters and routing
- ✅ Testing authentication/authorization flows
- ✅ Verifying database transactions
- ✅ Testing service discovery integration

**Do NOT** write integration tests for:
- ❌ Pure business logic (use unit tests)
- ❌ Simple CRUD operations (use unit tests)
- ❌ Individual method behavior (use unit tests)

---

## Testing Strategy

### The Test Pyramid

```
           /\
          /  \  E2E Tests (5%)
         /    \
        /------\ Integration Tests (20%)
       /        \
      /----------\ Unit Tests (75%)
     /____________\
```

**Distribution for GCRF Project**:
- **Unit Tests**: 75% - Fast, isolated, test single components
- **Integration Tests**: 20% - Test component interactions
- **E2E Tests**: 5% - Full system tests with all services running

### Test Levels in GCRF

#### 1. Unit Tests
- **Scope**: Single class/method
- **Tools**: JUnit 5, Mockito, AssertJ
- **Example**: `AuthServiceTest.java`, `UserServiceTest.java`
- **Speed**: Very fast (< 1ms per test)

#### 2. Integration Tests
- **Scope**: Multiple components within a service
- **Tools**: `@SpringBootTest`, MockMvc/WebTestClient
- **Example**: `UserControllerIntegrationTest.java`, `GatewayAuthIntegrationTest.java`
- **Speed**: Fast (< 100ms per test)

#### 3. E2E Tests (Future)
- **Scope**: Multiple services with real infrastructure
- **Tools**: Docker Compose, TestContainers
- **Example**: Full login flow through gateway to backend services
- **Speed**: Slow (1-5 seconds per test)

---

## Gateway Integration Patterns

### Pattern 1: Testing Gateway Filters with WebTestClient

**Use Case**: Validate Gateway authentication filter behavior

**Example**: `GatewayAuthIntegrationTest.java`

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.cloud.nacos.config.enabled=false"
})
class GatewayAuthIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void testProtectedPathWithoutTokenReturns401() {
        webTestClient.get()
            .uri("/api/v1/books")
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void testProtectedPathWithValidToken() {
        String token = generateValidToken(123L, "testuser");

        webTestClient.get()
            .uri("/api/v1/books")
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus().is5xxServerError(); // Backend not available, but auth passed
    }
}
```

**Key Points**:
- Use `WebTestClient` for reactive Gateway (not `TestRestTemplate`)
- Use `RANDOM_PORT` to avoid port conflicts
- Disable Nacos for isolated testing
- Test authentication behavior, not routing (backend may return 404/5xx)

### Pattern 2: Testing Whitelist Paths

**Use Case**: Verify certain paths bypass authentication

```java
@Test
void testWhitelistPathAccessibleWithoutToken() {
    webTestClient.get()
        .uri("/api/v1/auth/login")
        .exchange()
        .expectStatus().isNotFound(); // No handler, but NOT 401 Unauthorized
}

@Test
void testActuatorHealthAccessible() {
    webTestClient.get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus().isOk(); // Actuator endpoint works
}
```

**Key Points**:
- Whitelist paths should NOT return 401
- May return 404 (no handler) or 200 (actuator)
- Tests security configuration, not application logic

### Pattern 3: Testing User Context Propagation

**Use Case**: Verify gateway adds user info headers

```java
@Test
void testValidTokenAddsUserHeaders() {
    String token = generateValidToken(456L, "admin");

    // In integration test, verify filter passes request through
    // (Header verification happens in downstream service)
    webTestClient.get()
        .uri("/api/v1/readers")
        .header("Authorization", "Bearer " + token)
        .exchange()
        .expectStatus().is5xxServerError(); // Auth passed, backend unavailable
}
```

**Note**: Full header verification requires mocking downstream services or testing in E2E environment.

---

## JWT Testing Patterns

### Pattern 1: Testing JWT Token Generation

**Use Case**: Validate token structure and claims

**Example**: `JwtTokenFlowIntegrationTest.java`

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.cloud.nacos.config.enabled=false"
})
class JwtTokenFlowIntegrationTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void testTokenGenerationCreatesValidToken() {
        // Arrange
        Map<String, Object> claims = createTestClaims(123L, "testuser", "ADMIN");

        // Act
        String token = jwtUtil.generateToken("testuser", claims);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testTokenContainsCorrectClaims() {
        // Arrange
        Long expectedUserId = 456L;
        String expectedUsername = "admin";
        Map<String, Object> claims = createTestClaims(expectedUserId, expectedUsername, "ADMIN");

        // Act
        String token = jwtUtil.generateToken(expectedUsername, claims);

        // Assert
        assertEquals(expectedUsername, jwtUtil.getUsername(token));
        assertEquals(expectedUserId, jwtUtil.getUserId(token));
    }

    private Map<String, Object> createTestClaims(Long userId, String username, String userType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("userType", userType);
        return claims;
    }
}
```

**Key Points**:
- Use actual `JwtUtil` from common module (not mocked)
- Test with full Spring context to validate configuration
- Verify token structure (3 parts: header.payload.signature)
- Extract and validate all claims

### Pattern 2: Testing Token Expiration

**Use Case**: Validate expired tokens are rejected

```java
@Test
void testExpiredTokenIsRejected() throws Exception {
    // Create token with past expiration using JJWT directly
    Date pastExpiration = new Date(System.currentTimeMillis() - 3600000); // 1 hour ago

    String expiredToken = Jwts.builder()
        .setSubject("testuser")
        .claim("userId", 123L)
        .claim("username", "testuser")
        .setIssuedAt(new Date(System.currentTimeMillis() - 7200000))
        .setExpiration(pastExpiration)
        .signWith(getSecretKey(), SignatureAlgorithm.HS512)
        .compact();

    // Verify expired token is rejected
    assertFalse(jwtUtil.validateToken(expiredToken));
}

@Test
void testTokenExpirationIsEnforced() throws InterruptedException {
    // Create token with 1 second expiration
    String shortLivedToken = jwtUtil.generateToken("testuser",
        createTestClaims(123L, "testuser", "ADMIN"),
        1000L); // 1 second

    // Verify token is valid initially
    assertTrue(jwtUtil.validateToken(shortLivedToken));

    // Wait for expiration
    Thread.sleep(1100);

    // Verify token is now expired
    assertFalse(jwtUtil.validateToken(shortLivedToken));
}
```

**Key Points**:
- Use JJWT library directly for pre-expired tokens
- Use `Thread.sleep()` for time-based expiration testing
- Test both scenarios: pre-expired and time-elapsed expiration

### Pattern 3: Testing Token Signature Validation

**Use Case**: Ensure tampered tokens are rejected

```java
@Test
void testTokenValidationFailsWithWrongSecret() {
    // Generate token with correct secret
    String token = jwtUtil.generateToken("testuser", createTestClaims(123L, "testuser", "ADMIN"));

    // Create JwtUtil with different secret for validation
    SecretKey wrongSecret = Keys.hmacShaKeyFor("DIFFERENT_SECRET_KEY_12345".getBytes());

    // Verify validation fails with wrong secret
    try {
        Jwts.parserBuilder()
            .setSigningKey(wrongSecret)
            .build()
            .parseClaimsJws(token);
        fail("Should have thrown SignatureException");
    } catch (SignatureException e) {
        // Expected
        assertThat(e.getMessage()).contains("signature");
    }
}

@Test
void testTokenSignatureValidation() {
    // Generate valid token
    String token = jwtUtil.generateToken("testuser", createTestClaims(123L, "testuser", "ADMIN"));

    // Tamper with payload (change middle part)
    String[] parts = token.split("\\.");
    String tamperedPayload = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString("{\"sub\":\"hacker\"}".getBytes());
    String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

    // Verify tampered token is rejected
    assertFalse(jwtUtil.validateToken(tamperedToken));
}
```

**Key Points**:
- Test both wrong secret and payload tampering
- Use JJWT parser directly for low-level validation
- Expect `SignatureException` for signature mismatches

---

## Common Pitfalls

### Pitfall 1: Port Conflicts in Tests

**Problem**: Multiple tests start on the same port causing failures

**Solution**: Always use `RANDOM_PORT`

```java
// ❌ Bad - Fixed port
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)

// ✅ Good - Random port
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
```

### Pitfall 2: Service Discovery in Tests

**Problem**: Tests fail because Nacos is not running

**Solution**: Disable Nacos in test configuration

```java
@TestPropertySource(properties = {
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.cloud.nacos.config.enabled=false"
})
```

### Pitfall 3: Database State Pollution

**Problem**: Test data affects other tests

**Solution**: Use transactions or clean up after each test

```java
@Transactional
@Rollback
class MyIntegrationTest {
    // Tests automatically rollback
}

// OR

@AfterEach
void cleanup() {
    userRepository.deleteAll();
}
```

### Pitfall 4: Testing Wrong Layer

**Problem**: Integration test checking unit-level logic

**Solution**: Keep integration tests focused on interactions

```java
// ❌ Bad - Unit test masquerading as integration test
@Test
void testPasswordEncryptionAlgorithm() {
    String encrypted = passwordEncoder.encode("password");
    assertTrue(passwordEncoder.matches("password", encrypted));
}

// ✅ Good - Integration test checking service interaction
@Test
void testLoginFlowWithValidCredentials() {
    LoginRequest request = new LoginRequest("user", "password");
    LoginResponse response = authService.login(request);
    assertThat(response.getToken()).isNotNull();
}
```

### Pitfall 5: Slow Integration Tests

**Problem**: Integration tests take too long to run

**Solution**:
- Limit integration tests (20% of total)
- Use `@DirtiesContext` sparingly (very slow)
- Mock external services
- Use in-memory databases for tests

```java
// ❌ Bad - Recreates context for every test
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)

// ✅ Good - Reuse context
@SpringBootTest
class MyIntegrationTest {
    // Context reused across tests
}
```

---

## Best Practices

### 1. Test Isolation

**Each test should be independent and repeatable**

```java
@BeforeEach
void setUp() {
    // Clean state before each test
    SecurityContextHolder.clearContext();
}

@AfterEach
void tearDown() {
    // Clean up after each test
    testDataRepository.deleteAll();
}
```

### 2. Use Descriptive Test Names

```java
// ❌ Bad
@Test
void test1() { }

// ✅ Good
@Test
void testProtectedPathWithoutTokenReturns401() { }

// ✅ Even better with @DisplayName
@Test
@DisplayName("Protected path without token should return 401 Unauthorized")
void testProtectedPathWithoutTokenReturns401() { }
```

### 3. Follow AAA Pattern

**Arrange, Act, Assert**

```java
@Test
void testUserRegistration() {
    // Arrange
    RegisterRequest request = new RegisterRequest("user", "pass", "user@example.com");

    // Act
    RegisterResponse response = authService.register(request);

    // Assert
    assertThat(response.getUserId()).isNotNull();
    assertThat(response.getUsername()).isEqualTo("user");
}
```

### 4. Use Helper Methods

**Extract common test data creation**

```java
private String generateValidToken(Long userId, String username) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("username", username);
    claims.put("userType", "ADMIN");
    return jwtUtil.generateToken(username, claims);
}
```

### 5. Test Both Happy and Sad Paths

```java
// Happy path
@Test
void testValidTokenPassesAuthentication() {
    String token = generateValidToken(123L, "user");
    assertTrue(jwtUtil.validateToken(token));
}

// Sad paths
@Test
void testInvalidTokenFailsAuthentication() {
    assertFalse(jwtUtil.validateToken("invalid-token"));
}

@Test
void testExpiredTokenFailsAuthentication() {
    String expiredToken = generateExpiredToken();
    assertFalse(jwtUtil.validateToken(expiredToken));
}
```

### 6. Use AssertJ for Fluent Assertions

```java
// ❌ Less readable
assertTrue(result.getUsers().size() > 0);
assertEquals("admin", result.getUsers().get(0).getUsername());

// ✅ More readable
assertThat(result.getUsers())
    .isNotEmpty()
    .first()
    .extracting("username")
    .isEqualTo("admin");
```

### 7. Document Complex Tests

```java
/**
 * Tests that JWT tokens expire after the configured time.
 *
 * This test:
 * 1. Creates a token with 1-second expiration
 * 2. Validates it's initially valid
 * 3. Waits for expiration (1.1 seconds)
 * 4. Validates it's now expired
 *
 * Note: Uses Thread.sleep() which makes test slower but is necessary
 * for time-based expiration testing.
 */
@Test
void testTokenExpirationIsEnforced() throws InterruptedException {
    // Test implementation
}
```

---

## Example Tests

### Example 1: Gateway Authentication Filter Test

**File**: `gateway-service/src/test/java/com/gcrf/gateway/integration/GatewayAuthIntegrationTest.java`

**What it tests**:
- Gateway authentication filter behavior
- JWT token validation at gateway level
- Whitelist path bypass
- HTTP method support (GET, POST, PUT, DELETE)

**Test count**: 13 tests

**Key learnings**:
- Use `WebTestClient` for reactive Gateway
- Test authentication, not routing (backend may be unavailable)
- Disable Nacos for isolated testing

### Example 2: JWT Token Flow Test

**File**: `auth-service/src/test/java/com/gcrf/library/auth/integration/JwtTokenFlowIntegrationTest.java`

**What it tests**:
- Complete JWT token lifecycle
- Token generation and structure
- Claims extraction and validation
- Token expiration enforcement
- Signature validation

**Test count**: 7 tests

**Key learnings**:
- Use actual JwtUtil with full Spring context
- Create expired tokens with JJWT library directly
- Test both time-based and pre-expired tokens
- Validate signature with different secrets

### Example 3: User Controller Integration Test

**File**: `auth-service/src/test/java/com/gcrf/library/auth/controller/UserControllerIntegrationTest.java`

**What it tests**:
- REST API endpoints
- Request/response validation
- Error handling
- Security annotations

**Test count**: 19 tests

**Key learnings**:
- Use `MockMvc` for MVC controllers
- Test with `@WithMockUser` for security context
- Validate request/response JSON structure
- Test error responses (400, 404, 500)

---

## Testing Checklist

Before committing integration tests, verify:

- [ ] Tests use `@SpringBootTest` with appropriate web environment
- [ ] Nacos discovery/config disabled in test properties
- [ ] Random port used to avoid conflicts
- [ ] Tests are independent (no shared state)
- [ ] Both happy and sad paths tested
- [ ] Test names are descriptive
- [ ] AAA pattern followed
- [ ] Helper methods used for common operations
- [ ] Assertions are clear and specific
- [ ] Tests run in < 2 minutes total
- [ ] No `@DirtiesContext` unless absolutely necessary
- [ ] Documentation added for complex tests

---

## Performance Guidelines

### Integration Test Performance Targets

- **Single test**: < 100ms (excluding Spring context startup)
- **Test class**: < 5 seconds (including context startup)
- **All integration tests**: < 2 minutes

### Tips for Fast Integration Tests

1. **Reuse Spring context** - Avoid `@DirtiesContext`
2. **Mock external services** - Don't make real HTTP calls
3. **Use in-memory databases** - H2 for tests, PostgreSQL for production
4. **Limit integration tests** - 20% of total test suite
5. **Run unit tests first** - Fail fast on simple issues

---

## Future Enhancements

### Planned Test Patterns (Future Stages)

1. **TestContainers Pattern** - Full E2E tests with Docker
2. **Contract Testing** - Pact for service contracts
3. **Performance Testing** - JMeter/Gatling integration tests
4. **Chaos Testing** - Resilience testing with service failures

---

## References

### Internal Documentation
- `IMPLEMENTATION_PLAN_STAGE14.md` - Cross-service integration test plan
- `CLAUDE.md` - Project development guidelines
- Test examples in `auth-service/src/test/java/`
- Test examples in `gateway-service/src/test/java/`

### External Resources
- Spring Boot Testing: https://docs.spring.io/spring-boot/docs/3.2.2/reference/html/features.html#features.testing
- AssertJ: https://assertj.github.io/doc/
- JUnit 5: https://junit.org/junit5/docs/current/user-guide/
- Mockito: https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html

---

**Document Version**: 1.0
**Last Updated**: 2025-11-01
**Maintained by**: GCRF Team
**Stage**: Stage 14 - Cross-Service Integration Tests
