# GCRF Library Management System - Security Audit Report

**Audit Date**: 2025-12-01
**Audit Version**: 1.0
**Auditor**: Security Team
**System Version**: 1.0.0-SNAPSHOT
**Classification**: Confidential

---

## Executive Summary

This security audit report provides a comprehensive assessment of the GCRF Library Management System prior to production deployment. The audit covers backend services, frontend application, infrastructure components, and deployment configurations.

### Overall Security Posture

| Category                       | Risk Level | Issues Found | Critical | High  | Medium | Low   |
| ------------------------------ | ---------- | ------------ | -------- | ----- | ------ | ----- |
| Authentication & Authorization | Medium     | 5            | 0        | 2     | 2      | 1     |
| Input Validation               | Low        | 3            | 0        | 0     | 2      | 1     |
| Cryptography                   | Medium     | 4            | 0        | 2     | 1      | 1     |
| Infrastructure Security        | High       | 6            | 1        | 3     | 1      | 1     |
| Data Protection                | Medium     | 4            | 0        | 1     | 2      | 1     |
| Frontend Security              | Medium     | 5            | 0        | 1     | 3      | 1     |
| **Total**                      | **Medium** | **27**       | **1**    | **9** | **11** | **6** |

---

## 1. Audit Scope

### 1.1 In-Scope Components

**Backend Services:**

- Gateway Service (Spring Cloud Gateway)
- Auth Service (Authentication & Authorization)
- Book Service (Book Management)
- Circulation Service (Borrowing/Returns)
- Reader Service (Reader Management)
- System Service (System Administration)
- Notification Service
- Recommend Service
- Chat Service
- Analytics Service

**Frontend:**

- Web Admin (Vue 3 + Element Plus)

**Infrastructure:**

- PostgreSQL 15 (Primary + Replicas)
- Redis 7.2 (Sentinel Mode)
- RabbitMQ 3.12
- MinIO Object Storage
- Nacos Service Discovery
- Elasticsearch

**Configuration & Deployment:**

- Docker Compose configurations
- Environment variables and secrets
- CI/CD pipeline configurations

### 1.2 Out of Scope

- Physical security
- Social engineering
- Third-party service provider security
- Network infrastructure (firewalls, load balancers)

---

## 2. Security Findings

### 2.1 Critical Findings (1)

#### SEC-CRIT-001: Hardcoded Credentials in Infrastructure Configuration

**Location**:

- `/backend/infrastructure/postgresql/docker-compose.yml`
- `/backend/infrastructure/redis/docker-compose.yml`
- `/backend/infrastructure/rabbitmq/docker-compose.yml`
- `/backend/infrastructure/minio/docker-compose.yml`

**Description**:
Multiple infrastructure docker-compose files contain hardcoded passwords that should be environment variables:

```yaml
# PostgreSQL - hardcoded default password
POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-gcrf_secure_2024}
REPLICATION_PASSWORD: ${REPLICATION_PASSWORD:-repl_secure_2024}

# Redis Commander
HTTP_PASSWORD: ${REDIS_COMMANDER_PASSWORD:-gcrf_redis_2024}

# RabbitMQ
RABBITMQ_DEFAULT_PASS=gcrf_rabbitmq_2024
RABBITMQ_ERLANG_COOKIE=GCRF_RABBITMQ_SECRET_COOKIE

# MinIO
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=gcrf_minio_2024
```

**Risk**: Attackers who gain access to source code can immediately access all infrastructure components.

**CVSS Score**: 9.1 (Critical)

**Recommendation**:

1. Remove all hardcoded passwords from configuration files
2. Use environment variables without fallback defaults for production
3. Implement HashiCorp Vault or similar secret management solution
4. Rotate all exposed credentials immediately

---

### 2.2 High Findings (9)

#### SEC-HIGH-001: JWT Secret Key Exposure in Configuration Files

**Location**:

- `/backend/auth-service/src/main/resources/application.yml` (Line 31)
- `/backend/gateway-service/src/main/resources/application.yml` (Line 104)

**Description**:
JWT secret is hardcoded in application.yml files:

```yaml
jwt:
  secret: GCRF_Library_System_Secret_2024_Secure
```

**Risk**: Token forgery if source code is compromised.

**CVSS Score**: 8.1 (High)

**Recommendation**:

1. Move JWT secret to environment variable without defaults
2. Use a minimum of 256 bits (32+ characters) for HS256 or 512 bits for HS512
3. Implement key rotation mechanism

---

#### SEC-HIGH-002: Weak Default JWT Secret

**Location**: `/backend/common/common-security/src/main/java/com/gcrf/library/common/utils/JwtUtil.java` (Line 28)

**Description**:

```java
@Value("${jwt.secret:gcrf-library-management-system-jwt-secret-key-2025}")
private String secret;
```

The fallback default secret is predictable and documented in source code.

**Risk**: If environment variable is not set, system uses weak, known secret.

**CVSS Score**: 8.1 (High)

**Recommendation**:

1. Remove default value, make JWT secret required
2. Application should fail to start if JWT secret is not configured
3. Add validation for minimum secret length

---

#### SEC-HIGH-003: MinIO Anonymous Public Access

**Location**: `/backend/infrastructure/minio/docker-compose.yml` (Lines 43-44)

**Description**:

```bash
/usr/bin/mc anonymous set download myminio/covers;
/usr/bin/mc anonymous set download myminio/avatars;
```

MinIO buckets are configured with anonymous public download access.

**Risk**: Potential data exposure, storage cost attacks, information leakage.

**CVSS Score**: 7.5 (High)

**Recommendation**:

1. Use presigned URLs for public access instead of anonymous policies
2. Implement access logging
3. Add rate limiting at reverse proxy level

---

#### SEC-HIGH-004: Database Connection Without SSL

**Location**: Multiple application.yml files

**Description**:
PostgreSQL connections do not enforce SSL:

```yaml
url: jdbc:postgresql://localhost:5432/auth_service
```

No `sslmode=require` parameter is specified.

**Risk**: Man-in-the-middle attacks can intercept database credentials and data.

**CVSS Score**: 7.4 (High)

**Recommendation**:

1. Configure PostgreSQL with SSL certificates
2. Add `sslmode=require` to all JDBC URLs
3. Verify certificate chain in production

---

#### SEC-HIGH-005: Actuator Endpoints Exposed Without Authentication

**Location**: `/backend/auth-service/src/main/java/com/gcrf/library/auth/config/SecurityConfig.java` (Line 65)

**Description**:

```java
.requestMatchers("/actuator/**").permitAll()
```

Actuator endpoints including /health, /metrics, and /prometheus are publicly accessible.

**Risk**: Information disclosure, internal system state exposure.

**CVSS Score**: 7.5 (High)

**Recommendation**:

1. Protect actuator endpoints with authentication
2. Only expose /health/liveness and /health/readiness publicly
3. Bind actuator to separate management port
4. Implement IP whitelist for metrics endpoints

---

#### SEC-HIGH-006: Swagger/OpenAPI Documentation Exposed in Production

**Location**: Security configurations allow public access to API documentation

**Description**:

```java
.requestMatchers(
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/doc.html",
    "/webjars/**"
).permitAll()
```

**Risk**: Information disclosure about API structure, endpoints, and parameters.

**CVSS Score**: 6.5 (High)

**Recommendation**:

1. Disable Swagger/Knife4j in production profile
2. Protect documentation endpoints with authentication
3. Use conditional configuration based on profile

---

#### SEC-HIGH-007: Sensitive Data Stored in localStorage

**Location**: `/web-admin/src/stores/user.js` (Lines 90-96)

**Description**:

```javascript
persist: {
  enabled: true,
  strategies: [
    {
      key: 'user',
      storage: localStorage
    }
  ]
}
```

JWT tokens and user information are persisted to localStorage.

**Risk**: XSS attacks can steal authentication tokens. localStorage is accessible to all JavaScript in the origin.

**CVSS Score**: 7.1 (High)

**Recommendation**:

1. Store tokens in httpOnly cookies
2. Implement token rotation
3. Add XSS content security policy
4. Consider using sessionStorage for short-lived sessions

---

#### SEC-HIGH-008: Missing Token Blacklist Check in Gateway Filter

**Location**: JWT validation in Gateway service does not check Redis blacklist

**Description**:
The JwtAuthenticationFilter in auth-service checks token blacklist, but Gateway may not perform this check, allowing revoked tokens to pass through.

**Risk**: Revoked tokens remain valid until expiration.

**CVSS Score**: 6.8 (High)

**Recommendation**:

1. Implement centralized token blacklist check in Gateway
2. Use short-lived access tokens with refresh tokens
3. Consider token binding or fingerprinting

---

#### SEC-HIGH-009: CORS Misconfiguration Allows All Origins

**Location**: `/backend/gateway-service/src/main/resources/application.yml` (Line 81)

**Description**:

```yaml
globalcors:
  cors-configurations:
    "[/**]":
      allowedOriginPatterns: "*"
```

The wildcard origin pattern allows requests from any origin.

**Risk**: Cross-origin attacks, credential theft in conjunction with other vulnerabilities.

**CVSS Score**: 6.5 (High)

**Recommendation**:

1. Specify exact allowed origins for production
2. Remove the global wildcard configuration
3. Use the more restrictive gateway.cors configuration already present

---

### 2.3 Medium Findings (11)

#### SEC-MED-001: Missing Rate Limiting on Login Endpoint (Implementation Incomplete)

**Location**: `/backend/gateway-service/src/main/resources/application.yml`

**Description**:
Rate limiting is configured but may not be fully implemented in the filter chain.

**Risk**: Brute force attacks on login endpoint.

**CVSS Score**: 5.3 (Medium)

**Recommendation**:

1. Verify rate limiting filter is properly integrated
2. Add account lockout after failed attempts
3. Implement CAPTCHA for suspicious activity

---

#### SEC-MED-002: Login Response Contains Excessive Information

**Location**: `/backend/auth-service/src/main/java/com/gcrf/library/auth/service/AuthService.java`

**Description**:
Login response includes userId, username, and userType, which may aid attackers.

**Risk**: Information disclosure assists enumeration and targeted attacks.

**CVSS Score**: 5.3 (Medium)

**Recommendation**:

1. Return minimal information in login response
2. Use opaque session tokens instead of embedding user data in JWT claims

---

#### SEC-MED-003: Missing Input Length Validation on Login

**Location**: `/backend/auth-service/src/main/java/com/gcrf/library/auth/dto/LoginRequest.java`

**Description**:

```java
@NotBlank(message = "...")
private String username;

@NotBlank(message = "...")
private String password;
```

Only @NotBlank validation, no maximum length constraints.

**Risk**: Potential DoS through oversized input, buffer-related issues.

**CVSS Score**: 5.3 (Medium)

**Recommendation**:

1. Add @Size(max=100) for username
2. Add @Size(max=256) for password
3. Implement global request size limits

---

#### SEC-MED-004: Nacos Default Credentials

**Location**: Multiple bootstrap.yml and application.yml files

**Description**:

```yaml
nacos:
  discovery:
    server-addr: localhost:8848
    username: nacos
    password: nacos
```

Default Nacos credentials are used.

**Risk**: Unauthorized access to service registry and configuration.

**CVSS Score**: 5.9 (Medium)

**Recommendation**:

1. Change Nacos credentials before production
2. Enable Nacos authentication and authorization
3. Use environment variables for credentials

---

#### SEC-MED-005: Debug Logging Enabled

**Location**: Multiple application.yml files

**Description**:

```yaml
logging:
  level:
    com.gcrf.library: debug
```

Debug logging is enabled by default.

**Risk**: Sensitive information in logs, performance impact.

**CVSS Score**: 4.3 (Medium)

**Recommendation**:

1. Set production logging level to INFO or WARN
2. Implement structured logging with sensitive data masking
3. Configure log rotation and retention

---

#### SEC-MED-006: Content Security Policy Allows Unsafe Inline Scripts

**Location**: `/backend/gateway-service/src/main/resources/application.yml` (Lines 261-262)

**Description**:

```yaml
script-src: "'self' 'unsafe-inline' 'unsafe-eval'"
style-src: "'self' 'unsafe-inline'"
```

CSP allows unsafe-inline and unsafe-eval which weakens XSS protection.

**Risk**: Reduced effectiveness of XSS mitigation.

**CVSS Score**: 5.4 (Medium)

**Recommendation**:

1. Remove 'unsafe-inline' and 'unsafe-eval' where possible
2. Use nonces or hashes for inline scripts
3. Refactor code to avoid eval()

---

#### SEC-MED-007: Token Refresh Mechanism Thread Sleep

**Location**: `/backend/auth-service/src/main/java/com/gcrf/library/auth/service/AuthService.java` (Lines 167-171)

**Description**:

```java
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

Using Thread.sleep for token uniqueness is a code smell and potential DoS vector.

**Risk**: Resource exhaustion, timing attacks.

**CVSS Score**: 4.8 (Medium)

**Recommendation**:

1. Add jti (JWT ID) claim with UUID for token uniqueness
2. Remove Thread.sleep
3. Include issued-at timestamp with millisecond precision

---

#### SEC-MED-008: Missing Security Headers Filter Verification

**Location**: Gateway security headers configuration

**Description**:
Security headers are configured in YAML but filter implementation needs verification.

**Risk**: Security headers may not be applied to all responses.

**CVSS Score**: 4.3 (Medium)

**Recommendation**:

1. Verify SecurityHeadersFilter is in filter chain
2. Add integration tests for security headers
3. Test with security header scanning tools

---

#### SEC-MED-009: GlobalExceptionHandler May Leak Stack Traces

**Location**: `/backend/common/common-web/src/main/java/com/gcrf/library/common/handler/GlobalExceptionHandler.java`

**Description**:

```java
log.error("系统异常: {}", e.getMessage(), e);
return Result.error(ResultCode.INTERNAL_SERVER_ERROR);
```

While error response is sanitized, full stack traces are logged and could be exposed if log level misconfigured.

**Risk**: Information disclosure through error messages.

**CVSS Score**: 4.3 (Medium)

**Recommendation**:

1. Ensure logs are not exposed to clients
2. Add error tracking system (Sentry, etc.)
3. Implement unique error reference codes

---

#### SEC-MED-010: Redis Connection Without TLS

**Location**: Redis configuration in application files

**Description**:
Redis connections do not use TLS encryption.

**Risk**: Credentials and data transmitted in plaintext.

**CVSS Score**: 5.9 (Medium)

**Recommendation**:

1. Enable Redis TLS in production
2. Use STUNNEL or Redis 6+ native TLS
3. Verify Redis authentication is enabled

---

#### SEC-MED-011: Session Management Concerns

**Location**: StatelessSessionCreationPolicy is used

**Description**:
While stateless JWT is appropriate, token lifetime of 24 hours (86400000ms) is long.

**Risk**: Extended attack window if token compromised.

**CVSS Score**: 4.8 (Medium)

**Recommendation**:

1. Reduce access token lifetime to 15-30 minutes
2. Implement refresh token rotation
3. Add token binding or device fingerprinting

---

### 2.4 Low Findings (6)

#### SEC-LOW-001: Missing X-Request-ID Correlation

**Description**: Requests lack correlation IDs for tracing.

**Recommendation**: Add X-Request-ID header generation in Gateway.

---

#### SEC-LOW-002: Frontend API Timeout Too Long

**Location**: `/web-admin/src/utils/request.js` (Line 12)

**Description**: 15-second timeout may be excessive for most operations.

**Recommendation**: Implement per-endpoint timeout configuration.

---

#### SEC-LOW-003: Missing CSRF Token for State-Changing Operations

**Description**: While JWT is used, critical operations lack additional CSRF protection.

**Recommendation**: Consider double-submit cookie pattern for sensitive operations.

---

#### SEC-LOW-004: Password Policy Not Enforced on Login

**Description**: Password complexity is validated on creation but not enforced on existing users.

**Recommendation**: Add password policy enforcement and expiration.

---

#### SEC-LOW-005: Missing Security.txt

**Description**: No /.well-known/security.txt file present.

**Recommendation**: Add security.txt with contact information.

---

#### SEC-LOW-006: Container Running as Root

**Location**: Dockerfiles do not explicitly set non-root user for Java services.

**Recommendation**: Add USER directive to Dockerfiles.

---

## 3. Compliance Assessment

### 3.1 OWASP Top 10 (2021) Compliance

| Risk                                 | Status  | Notes                                                  |
| ------------------------------------ | ------- | ------------------------------------------------------ |
| A01:2021 Broken Access Control       | Partial | JWT-based auth present, needs RBAC verification        |
| A02:2021 Cryptographic Failures      | At Risk | Hardcoded secrets, weak defaults                       |
| A03:2021 Injection                   | Good    | MyBatis-Plus LambdaQueryWrapper prevents SQL injection |
| A04:2021 Insecure Design             | Good    | Defense in depth, but rate limiting needs verification |
| A05:2021 Security Misconfiguration   | At Risk | Default credentials, debug logging                     |
| A06:2021 Vulnerable Components       | Unknown | Dependency scan needed                                 |
| A07:2021 Auth Failures               | Partial | Good JWT implementation, token handling concerns       |
| A08:2021 Software and Data Integrity | Good    | No unsafe deserialization found                        |
| A09:2021 Security Logging            | Partial | Logging present, needs centralization                  |
| A10:2021 SSRF                        | Good    | No SSRF vectors identified                             |

### 3.2 OWASP ASVS Level 1 Assessment

- **V1 Architecture**: Partial compliance
- **V2 Authentication**: Partial compliance (password policy, MFA missing)
- **V3 Session Management**: Partial compliance (long token lifetime)
- **V4 Access Control**: Needs verification
- **V5 Validation**: Good compliance
- **V6 Cryptography**: At Risk (key management)
- **V7 Error Handling**: Good compliance
- **V8 Data Protection**: Partial compliance
- **V9 Communications**: At Risk (TLS not enforced)
- **V10 Malicious Code**: Not assessed
- **V11 Business Logic**: Not assessed
- **V12 Files**: Needs review (MinIO configuration)
- **V13 API**: Partial compliance
- **V14 Configuration**: At Risk

---

## 4. Remediation Roadmap

### 4.1 Immediate Actions (Before Production - Week 1)

| Priority | Finding ID       | Action                                                     | Owner   |
| -------- | ---------------- | ---------------------------------------------------------- | ------- |
| P0       | SEC-CRIT-001     | Remove hardcoded credentials, implement secrets management | DevOps  |
| P0       | SEC-HIGH-001,002 | Externalize and strengthen JWT secrets                     | Backend |
| P0       | SEC-HIGH-004     | Enable database SSL                                        | DevOps  |
| P1       | SEC-HIGH-005     | Protect actuator endpoints                                 | Backend |
| P1       | SEC-HIGH-006     | Disable Swagger in production                              | Backend |

### 4.2 Short-term Actions (Week 2-4)

| Priority | Finding ID   | Action                              | Owner    |
| -------- | ------------ | ----------------------------------- | -------- |
| P1       | SEC-HIGH-007 | Implement httpOnly cookie storage   | Frontend |
| P1       | SEC-HIGH-009 | Fix CORS configuration              | Backend  |
| P2       | SEC-MED-001  | Verify rate limiting implementation | Backend  |
| P2       | SEC-MED-003  | Add input length validation         | Backend  |
| P2       | SEC-MED-004  | Change Nacos credentials            | DevOps   |

### 4.3 Medium-term Actions (Month 2-3)

| Priority | Finding ID   | Action                             | Owner    |
| -------- | ------------ | ---------------------------------- | -------- |
| P2       | SEC-HIGH-003 | Implement presigned URLs for MinIO | Backend  |
| P2       | SEC-MED-006  | Strengthen CSP policy              | Frontend |
| P2       | SEC-MED-010  | Enable Redis TLS                   | DevOps   |
| P3       | SEC-MED-011  | Implement refresh token rotation   | Backend  |

---

## 5. Testing Recommendations

### 5.1 Automated Security Testing

1. **SAST (Static Analysis)**
   - Configure SonarQube with security rules
   - Integrate Semgrep for custom rules
   - Run OWASP Dependency-Check in CI/CD

2. **DAST (Dynamic Analysis)**
   - Schedule OWASP ZAP scans
   - Configure Burp Suite for API testing
   - Implement security regression tests

3. **Container Security**
   - Integrate Trivy for image scanning
   - Implement Snyk for dependency scanning
   - Add Falco for runtime security

### 5.2 Manual Penetration Testing

Recommend third-party penetration test before production launch focusing on:

- Authentication bypass attempts
- Authorization boundary testing
- API security testing
- Business logic vulnerabilities

---

## 6. Appendices

### 6.1 Tools Used

- Manual code review
- Configuration analysis
- Static analysis patterns

### 6.2 References

- OWASP Top 10 2021
- OWASP ASVS 4.0
- CIS Benchmarks for Docker
- Spring Security Best Practices
- NIST Cybersecurity Framework

### 6.3 Document History

| Version | Date       | Author        | Changes              |
| ------- | ---------- | ------------- | -------------------- |
| 1.0     | 2025-12-01 | Security Team | Initial audit report |

---

**CONFIDENTIAL - FOR INTERNAL USE ONLY**

_This report contains sensitive security information. Distribution should be limited to authorized personnel only._
