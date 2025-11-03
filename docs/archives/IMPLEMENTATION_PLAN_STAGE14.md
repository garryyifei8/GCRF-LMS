# Stage 14: Cross-Service Integration Tests

**Status**: In Progress
**Started**: 2025-11-01
**Goal**: Create comprehensive cross-service integration tests to validate service interactions

---

## Context

After completing Stage 13 with 96 passing tests in auth-service (exceeding 80+ target by 20%), we now need to ensure that services work correctly together in an integrated environment.

**Current State**:
- ✅ Auth Service: 96 unit and integration tests
- ✅ Gateway Service: 8 AuthenticationFilter tests (unit level)
- ❌ **Missing**: End-to-end integration tests simulating real service interactions
- ❌ **Missing**: Cross-service JWT token validation tests
- ❌ **Missing**: Service discovery integration tests

**Key Integration Points Identified**:
1. Gateway → Auth Service (JWT validation, user info forwarding)
2. Gateway routing with Nacos service discovery
3. User context propagation via headers (X-User-Id, X-Username)

---

## Objectives

1. **Validate Gateway-Auth Integration**:
   - Test JWT token flow from login through gateway to downstream services
   - Verify whitelist paths work correctly
   - Ensure user context headers are properly forwarded

2. **Test Cross-Service Communication**:
   - Simulate real HTTP requests through gateway
   - Validate JWT tokens are correctly validated by gateway
   - Test error handling and unauthorized access

3. **Document Integration Patterns**:
   - Create reusable patterns for future integration tests
   - Document best practices for testing microservices

---

## Approach: Integration Test Strategy

Given the microservices architecture, we have two main approaches:

### Approach 1: Service-Level Integration Tests (Recommended for now)
- Test each service with embedded/mocked dependencies
- Use `@SpringBootTest` with `RANDOM_PORT`
- Mock external service calls with WireMock or similar
- **Pros**: Fast, isolated, deterministic
- **Cons**: Doesn't test real network communication

### Approach 2: Full E2E Tests (Future Stage)
- Requires all services running (Docker Compose)
- Real Nacos, PostgreSQL, Redis
- Tests actual network calls
- **Pros**: Tests real environment
- **Cons**: Slow, flaky, requires complex setup

**Decision**: Start with Approach 1 for Stage 14, defer Approach 2 to future stages.

---

## Phases

### Phase 1: Analysis ✅ COMPLETE
**Status**: ✅ Complete
**Deliverables**: Architecture analysis, integration points identified

**Findings**:
- Gateway uses `AuthenticationFilter` (GlobalFilter, order=-100)
- JWT validation done at gateway level
- User info forwarded via `X-User-Id` and `X-Username` headers
- Whitelist paths: `/api/v1/auth/login`, `/api/v1/auth/register`, `/api/v1/auth/health`

---

### Phase 2: Create Implementation Plan 🔄 IN PROGRESS
**Status**: 🔄 In Progress
**Deliverables**: `IMPLEMENTATION_PLAN_STAGE14.md` (this file)

---

### Phase 3: Create Gateway-Auth Integration Test
**Status**: ⏳ Pending
**Deliverables**: `GatewayAuthIntegrationTest.java` in gateway-service

**Test Coverage** (8-10 tests):
1. Login flow creates valid JWT token
2. Valid token allows access to protected routes
3. Invalid token returns 401 Unauthorized
4. Missing token returns 401 Unauthorized
5. Whitelist paths accessible without token
6. Token expiration is enforced
7. User context headers are correctly added
8. Malformed Authorization header returns 401
9. Different HTTP methods (GET, POST, PUT, DELETE) work correctly
10. CORS headers are properly set

**Implementation Notes**:
- Use `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- Use `TestRestTemplate` or `WebTestClient` for reactive
- Mock auth-service responses if needed
- Use test profile to disable Nacos discovery

---

### Phase 4: Create Cross-Service JWT Flow Test
**Status**: ⏳ Pending
**Deliverables**: `CrossServiceJwtFlowTest.java` in auth-service or integration module

**Test Coverage** (5-7 tests):
1. Complete login → token generation → validation flow
2. Token contains correct user claims (userId, username, userType)
3. Token validation succeeds with correct secret
4. Token validation fails with wrong secret
5. Expired token is rejected
6. Token refresh flow works correctly
7. Logout invalidates token (if implemented)

**Implementation Notes**:
- Can run in auth-service module
- Tests JWT lifecycle end-to-end
- Validates token structure and claims
- Tests token expiration and refresh logic

---

### Phase 5: Document Integration Test Patterns
**Status**: ⏳ Pending
**Deliverables**: `INTEGRATION_TEST_PATTERNS.md` in backend/doc/

**Content**:
1. **Testing Strategy Overview**:
   - When to use unit vs integration vs E2E tests
   - Test pyramid for microservices

2. **Gateway Integration Patterns**:
   - How to test Gateway filters
   - Mocking downstream services
   - Testing routing and load balancing

3. **JWT Testing Patterns**:
   - Generating test tokens
   - Validating token structure
   - Testing token expiration

4. **Common Pitfalls**:
   - Port conflicts in tests
   - Service discovery in tests
   - Database state management

5. **Best Practices**:
   - Test isolation
   - Test data management
   - Performance considerations

---

## Success Criteria

- [  ] Gateway-Auth integration test created with 8+ passing tests
- [  ] Cross-service JWT flow test created with 5+ passing tests
- [  ] All existing tests still pass
- [  ] Integration test patterns documented
- [  ] Tests run in CI/CD pipeline (< 2 minutes for integration tests)

---

## Technical Considerations

### 1. Test Isolation
- Each test should be independent
- Use `@DirtiesContext` sparingly (slow)
- Clean up test data after each test
- Use unique test data per test

### 2. Service Dependencies
- Mock external services (Nacos, Auth Service) in integration tests
- Use test containers for databases (optional, adds complexity)
- Use embedded Redis for caching tests

### 3. JWT Token Management
- Create utility class for generating test tokens
- Use same secret as production (from application-test.yml)
- Test with expired tokens, invalid signatures, etc.

### 4. Performance
- Integration tests should complete in < 2 minutes total
- Use `@TestMethodOrder` if test order matters
- Consider using `@Nested` for logical grouping

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Tests become flaky due to timing issues | High | Use deterministic waits, avoid sleep() |
| Port conflicts in CI/CD | Medium | Use `RANDOM_PORT` for all tests |
| Tests too slow | Medium | Limit integration tests, mock external calls |
| Test data pollution | High | Clean up after each test, use transactions |

---

## Dependencies

- All Stage 13 tests must pass
- Gateway service must be runnable
- Auth service must be runnable
- Common modules (JwtUtil, etc.) must be stable

---

## Timeline

- Phase 1 (Analysis): ✅ Complete (30 min)
- Phase 2 (Plan): 🔄 In Progress (30 min)
- Phase 3 (Gateway-Auth Test): ⏳ Pending (2 hours)
- Phase 4 (JWT Flow Test): ⏳ Pending (1 hour)
- Phase 5 (Documentation): ⏳ Pending (1 hour)

**Total Estimated Time**: 5 hours

---

## Notes

- This stage focuses on **service-level integration tests**, not full E2E tests
- Full E2E tests with Docker Compose will be addressed in a future stage
- Tests should be fast and deterministic
- Follow TDD principles: write test first, then fix if needed

---

**Next Action**: Create `GatewayAuthIntegrationTest.java` in gateway-service
