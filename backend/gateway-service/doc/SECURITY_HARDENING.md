# Gateway Security Hardening - GCRF Library Management System

## Overview

This document describes the security hardening measures implemented in the Gateway service for the GCRF Library Management System.

## 1. API Rate Limiting

### Implementation

Rate limiting is implemented using Redis with Redisson client, providing distributed rate limiting across multiple gateway instances.

### Configuration

Location: `/backend/gateway-service/src/main/resources/application.yml`

```yaml
gateway:
  rate-limit:
    enabled: true
    anonymous-requests-per-minute: 100      # Default for anonymous users
    authenticated-requests-per-minute: 1000  # Default for authenticated users
    paths:
      "/api/v1/auth/login":
        requests-per-minute: 10
        type: IP                            # Rate limit by IP address
      "/api/v1/auth/register":
        requests-per-minute: 5
        type: IP
      "/api/v1/auth/refresh":
        requests-per-minute: 30
        type: USER                          # Rate limit by user ID
```

### Rate Limit Types

| Type | Description | Use Case |
|------|-------------|----------|
| `IP` | Rate limit by client IP address | Anonymous users, login attempts |
| `USER` | Rate limit by authenticated user ID | API calls by logged-in users |
| `GLOBAL` | Global rate limit across all clients | Emergency throttling |

### Key Files

- `/backend/gateway-service/src/main/java/com/gcrf/gateway/config/RateLimitProperties.java`
- `/backend/gateway-service/src/main/java/com/gcrf/gateway/service/RateLimiterService.java`
- `/backend/gateway-service/src/main/java/com/gcrf/gateway/filter/RateLimitFilter.java`

### Response Headers

When rate limiting is active, the following headers are added to responses:

- `X-RateLimit-Remaining`: Number of remaining requests in the current window
- `Retry-After`: Seconds to wait before retrying (when rate limited)

### Rate Limited Response

```json
{
  "code": 429,
  "message": "Too Many Requests. Please try again later.",
  "data": null
}
```

---

## 2. Security Response Headers

### Implementation

Security headers are added to all responses to protect against common web vulnerabilities.

### Headers Added

| Header | Value | Purpose |
|--------|-------|---------|
| `Content-Security-Policy` | Configurable | Prevents XSS, clickjacking, code injection |
| `X-Frame-Options` | `DENY` | Prevents clickjacking |
| `X-Content-Type-Options` | `nosniff` | Prevents MIME sniffing |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | Forces HTTPS |
| `X-XSS-Protection` | `1; mode=block` | Legacy XSS protection |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Controls referrer leakage |
| `Permissions-Policy` | Restrictive | Limits browser features |
| `Cache-Control` | `no-store, no-cache, must-revalidate` | Prevents caching of sensitive data |

### Content-Security-Policy Details

```
default-src 'self';
script-src 'self' 'unsafe-inline' 'unsafe-eval';
style-src 'self' 'unsafe-inline';
img-src 'self' data: https:;
font-src 'self' data:;
connect-src 'self';
frame-src 'none';
object-src 'none';
base-uri 'self';
form-action 'self';
upgrade-insecure-requests;
block-all-mixed-content;
```

### Key Files

- `/backend/gateway-service/src/main/java/com/gcrf/gateway/config/SecurityHeadersProperties.java`
- `/backend/gateway-service/src/main/java/com/gcrf/gateway/filter/SecurityHeadersFilter.java`

---

## 3. CORS Configuration

### Implementation

CORS is configured with explicit allowlists instead of wildcards to prevent unauthorized cross-origin requests.

### Configuration

```yaml
gateway:
  cors:
    enabled: true
    allowed-origins:
      - http://localhost:3011
      - http://localhost:5173
      # Add production domains here
    allowed-methods:
      - GET
      - POST
      - PUT
      - DELETE
      - PATCH
      - OPTIONS
    allowed-headers:
      - Authorization
      - Content-Type
      - Accept
      - Origin
      - X-Requested-With
    exposed-headers:
      - X-RateLimit-Remaining
      - X-Request-Id
    allow-credentials: true
    max-age: 3600
```

### Security Considerations

1. **Never use `*` for allowed origins in production** - Always specify explicit domains
2. **Credentials require specific origins** - When `allow-credentials: true`, wildcards are not allowed
3. **Limit allowed methods** - Only allow HTTP methods that your API actually uses
4. **Limit allowed headers** - Only allow headers that your application needs

### Key Files

- `/backend/gateway-service/src/main/java/com/gcrf/gateway/config/CorsProperties.java`
- `/backend/gateway-service/src/main/java/com/gcrf/gateway/config/CorsConfig.java`

---

## 4. JWT Token Blacklist

### Implementation

Token blacklist mechanism enables immediate token invalidation upon logout, preventing use of compromised tokens.

### How It Works

1. When a user logs out, the auth-service adds their token to Redis blacklist
2. Gateway checks every authenticated request against the blacklist
3. Blacklisted tokens return 401 Unauthorized
4. Tokens automatically expire from blacklist when they would naturally expire

### Redis Key Format

```
auth:blacklist:{token}
```

### Integration with Auth Service

The Gateway and Auth Service share the same Redis instance and key prefix to ensure consistency.

### Key Files

- `/backend/gateway-service/src/main/java/com/gcrf/gateway/service/TokenBlacklistService.java`
- `/backend/gateway-service/src/main/java/com/gcrf/gateway/filter/AuthenticationFilter.java`

---

## 5. Request Validation

### Whitelist Paths

Paths that do not require authentication:

```yaml
gateway:
  whitelist:
    paths:
      - /api/v1/auth/login
      - /api/v1/auth/register
      - /actuator/health
      - /doc.html
      - /swagger-ui/**
```

### Authentication Flow

```
Request -> RateLimitFilter -> AuthenticationFilter -> SecurityHeadersFilter -> Backend Service
                  |                    |                        |
              Rate Limit          JWT + Blacklist          Add Headers
              Check               Validation
```

---

## 6. Redis Configuration

### Connection Settings

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:gcrf_redis_2024}
      database: 0
```

### Key File

- `/backend/gateway-service/src/main/java/com/gcrf/gateway/config/RedisConfig.java`

---

## 7. Production Deployment Checklist

### Before Deploying to Production

- [ ] Change Redis password to a strong, unique value
- [ ] Update CORS allowed-origins with production domains only
- [ ] Enable HSTS preload if using HTTPS everywhere
- [ ] Review and adjust rate limits based on expected traffic
- [ ] Configure proper logging levels (reduce debug logging)
- [ ] Enable Prometheus metrics export for monitoring
- [ ] Set up alerts for rate limiting and authentication failures
- [ ] Review CSP policy for frontend compatibility

### Environment Variables

```bash
REDIS_HOST=your-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=your-strong-password
```

---

## 8. Monitoring and Alerts

### Metrics to Monitor

1. **Rate Limit Triggers** - High rate of 429 responses may indicate attack
2. **Authentication Failures** - High rate of 401 responses may indicate brute force
3. **Token Blacklist Size** - Unusual growth may indicate mass logout or attack
4. **Redis Connection Health** - Ensure Redis is available for security functions

### Recommended Alerts

| Metric | Threshold | Severity |
|--------|-----------|----------|
| 429 responses/minute | > 100 | Warning |
| 401 responses/minute | > 50 | Warning |
| Redis connection failures | Any | Critical |
| Token blacklist entries | > 10000 | Info |

---

## 9. Security Testing

### Manual Tests

1. **Rate Limiting**
   ```bash
   # Should return 429 after 10 requests
   for i in {1..15}; do curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/v1/auth/login; done
   ```

2. **Token Blacklist**
   ```bash
   # Login, logout, then try to use the old token
   # Should return 401 with "Token has been revoked"
   ```

3. **Security Headers**
   ```bash
   curl -I http://localhost:8080/api/v1/books
   # Check for all security headers in response
   ```

4. **CORS**
   ```bash
   # From a non-allowed origin should fail
   curl -H "Origin: http://malicious-site.com" http://localhost:8080/api/v1/books
   ```

---

## 10. Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-12-01 | Initial security hardening implementation |

---

## References

- OWASP Secure Headers Project: https://owasp.org/www-project-secure-headers/
- Content Security Policy: https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP
- Rate Limiting Best Practices: https://cloud.google.com/architecture/rate-limiting-strategies-techniques
