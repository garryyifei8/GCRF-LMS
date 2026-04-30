# GCRF Library Management System - Pre-Production Security Checklist

**Version**: 1.0
**Last Updated**: 2025-12-01
**Status**: Required before production deployment

---

## Instructions

This checklist must be completed before the system goes live. Each item requires sign-off from the designated responsible party. Items marked with \*\* are mandatory blockers.

---

## 1. Secrets and Credentials Management

### 1.1 Application Secrets

- [ ] \*\* JWT_SECRET is externalized to environment variable (no defaults)
- [ ] \*\* JWT_SECRET is minimum 256 bits (64+ characters for HS512)
- [ ] \*\* JWT_SECRET is unique per environment (dev/staging/prod)
- [ ] JWT expiration is set to appropriate duration (recommended: 15-30 minutes)
- [ ] Refresh token mechanism is implemented and tested
- [ ] JWT secret rotation procedure is documented

### 1.2 Database Credentials

- [ ] \*\* PostgreSQL password changed from default `gcrf_secure_2024`
- [ ] \*\* Database password is externalized to environment variable
- [ ] \*\* Database password meets complexity requirements (32+ chars, mixed case, numbers, symbols)
- [ ] Database user has minimal required privileges (not superuser)
- [ ] Replication password (`repl_secure_2024`) is changed
- [ ] Connection pool credentials are securely managed

### 1.3 Cache and Message Queue

- [ ] \*\* Redis password changed from default `gcrf_redis_2024`
- [ ] \*\* RabbitMQ password changed from default `gcrf_rabbitmq_2024`
- [ ] \*\* RabbitMQ Erlang cookie is changed and secured
- [ ] Redis Commander password is changed or service is disabled

### 1.4 Object Storage

- [ ] \*\* MinIO root credentials changed from `minioadmin/gcrf_minio_2024`
- [ ] MinIO access keys are unique per service
- [ ] Anonymous bucket access is reviewed and minimized

### 1.5 Service Discovery

- [ ] \*\* Nacos credentials changed from default `nacos/nacos`
- [ ] Nacos namespace isolation is configured
- [ ] Nacos authentication is enabled

### 1.6 Secret Storage

- [ ] Secrets are stored in secure vault (HashiCorp Vault, AWS Secrets Manager, etc.)
- [ ] `.env` files are NOT committed to version control
- [ ] Production secrets are accessible only to authorized personnel
- [ ] Secret access is audited and logged

---

## 2. SSL/TLS Configuration

### 2.1 Database Connections

- [ ] \*\* PostgreSQL SSL is enabled (`sslmode=require`)
- [ ] SSL certificates are properly configured
- [ ] Certificate chain is validated

### 2.2 Redis Connections

- [ ] Redis TLS is enabled for production
- [ ] Redis certificates are properly configured

### 2.3 RabbitMQ Connections

- [ ] RabbitMQ TLS is enabled
- [ ] AMQPS protocol is used (port 5671)

### 2.4 Service-to-Service Communication

- [ ] Internal service communication uses TLS
- [ ] Certificate verification is enabled

### 2.5 External Access

- [ ] \*\* HTTPS is enforced for all public endpoints
- [ ] \*\* Valid SSL certificate is installed (not self-signed)
- [ ] HSTS is enabled with appropriate max-age
- [ ] SSL/TLS configuration follows best practices (TLS 1.2+)

---

## 3. Authentication and Authorization

### 3.1 JWT Configuration

- [ ] JWT tokens include appropriate claims (sub, iat, exp, jti)
- [ ] Token validation checks expiration
- [ ] Token blacklist is implemented and working
- [ ] Token refresh mechanism is tested

### 3.2 Password Security

- [ ] BCrypt is used for password hashing
- [ ] Password complexity requirements are enforced
- [ ] Password reset mechanism is secure
- [ ] Account lockout after failed attempts is configured

### 3.3 Session Management

- [ ] Session timeout is appropriate
- [ ] Concurrent session handling is defined
- [ ] Session invalidation on logout works correctly

### 3.4 Authorization

- [ ] Role-based access control (RBAC) is properly configured
- [ ] API endpoints have appropriate authorization checks
- [ ] Admin functions require elevated privileges
- [ ] Authorization bypass testing has been performed

---

## 4. API Security

### 4.1 Rate Limiting

- [ ] \*\* Login endpoint is rate-limited (max 10 attempts/minute)
- [ ] \*\* Registration endpoint is rate-limited (max 5 attempts/minute)
- [ ] API endpoints have appropriate rate limits
- [ ] Rate limit responses include appropriate headers

### 4.2 Input Validation

- [ ] All API inputs are validated
- [ ] Maximum input lengths are enforced
- [ ] Request body size limits are configured
- [ ] File upload size and type restrictions are in place

### 4.3 Output Encoding

- [ ] JSON responses are properly encoded
- [ ] Error messages do not leak sensitive information
- [ ] Stack traces are not exposed to clients

### 4.4 API Documentation

- [ ] \*\* Swagger/OpenAPI is disabled in production
- [ ] API documentation access requires authentication
- [ ] Sensitive endpoint documentation is restricted

---

## 5. Security Headers

### 5.1 HTTP Headers

- [ ] \*\* X-Frame-Options: DENY is set
- [ ] \*\* X-Content-Type-Options: nosniff is set
- [ ] \*\* X-XSS-Protection: 1; mode=block is set
- [ ] Referrer-Policy is configured
- [ ] Permissions-Policy is configured

### 5.2 Content Security Policy

- [ ] CSP is implemented
- [ ] CSP report-uri is configured for monitoring
- [ ] Unsafe-inline is minimized or eliminated
- [ ] Unsafe-eval is removed where possible

### 5.3 Transport Security

- [ ] \*\* Strict-Transport-Security (HSTS) is enabled
- [ ] HSTS max-age is at least 1 year (31536000)
- [ ] HSTS includeSubDomains is set

---

## 6. CORS Configuration

- [ ] \*\* Allowed origins are explicitly specified (no wildcards)
- [ ] Allowed methods are restricted to necessary HTTP methods
- [ ] Allowed headers are explicitly listed
- [ ] Credentials handling is properly configured
- [ ] Preflight cache is appropriately set

---

## 7. Logging and Monitoring

### 7.1 Security Logging

- [ ] Authentication events are logged (success/failure)
- [ ] Authorization failures are logged
- [ ] Security-relevant events have correlation IDs
- [ ] Log timestamps are consistent (UTC)

### 7.2 Log Protection

- [ ] \*\* Sensitive data (passwords, tokens) is NOT logged
- [ ] Log files are protected from unauthorized access
- [ ] Log rotation is configured
- [ ] Log retention policy is defined

### 7.3 Monitoring

- [ ] Security metrics are collected
- [ ] Alerts are configured for security events
- [ ] Failed login attempts trigger alerts
- [ ] Rate limit violations are monitored

### 7.4 Actuator Endpoints

- [ ] \*\* /actuator endpoints require authentication
- [ ] Only necessary actuator endpoints are exposed
- [ ] Actuator is on separate management port (recommended)

---

## 8. Infrastructure Security

### 8.1 Network Configuration

- [ ] Services are on private network
- [ ] Only necessary ports are exposed
- [ ] Firewall rules are configured
- [ ] Network segmentation is implemented

### 8.2 Container Security

- [ ] \*\* Containers run as non-root user
- [ ] Container images are scanned for vulnerabilities
- [ ] Base images are up-to-date
- [ ] Resource limits are configured

### 8.3 Database Security

- [ ] Database is not publicly accessible
- [ ] Database backup encryption is enabled
- [ ] Point-in-time recovery is configured
- [ ] Database activity is monitored

### 8.4 Object Storage Security

- [ ] MinIO is not publicly accessible
- [ ] Bucket policies are reviewed
- [ ] Access logging is enabled
- [ ] Presigned URLs have appropriate expiration

---

## 9. Dependency Management

### 9.1 Vulnerability Scanning

- [ ] OWASP Dependency-Check is integrated in CI/CD
- [ ] No critical vulnerabilities in dependencies
- [ ] High vulnerabilities are reviewed and mitigated
- [ ] Regular dependency updates are scheduled

### 9.2 Dependency Versions

- [ ] Spring Boot version is up-to-date (3.2.2)
- [ ] Spring Security version is current
- [ ] JJWT library is current version (0.12.3)
- [ ] Frontend dependencies are audited

---

## 10. Frontend Security

### 10.1 Token Storage

- [ ] Tokens are stored securely (httpOnly cookies preferred)
- [ ] Tokens are not exposed in localStorage where avoidable
- [ ] Token refresh is implemented
- [ ] Logout properly clears all tokens

### 10.2 XSS Prevention

- [ ] Vue.js default XSS protections are not bypassed
- [ ] v-html is not used with user input
- [ ] Third-party content is sanitized
- [ ] CSP is configured for frontend

### 10.3 Build Security

- [ ] Production build removes source maps
- [ ] Environment variables are properly handled
- [ ] Debug tools are disabled in production

---

## 11. Incident Response Preparation

- [ ] Security incident response plan is documented
- [ ] Incident response team contacts are identified
- [ ] Breach notification procedures are defined
- [ ] Forensic readiness is established

---

## 12. Compliance Verification

- [ ] OWASP Top 10 risks are addressed
- [ ] Data protection requirements are met
- [ ] Privacy policy is in place
- [ ] Terms of service are defined

---

## Sign-off

| Section               | Reviewer | Date | Status |
| --------------------- | -------- | ---- | ------ |
| 1. Secrets Management |          |      |        |
| 2. SSL/TLS            |          |      |        |
| 3. Auth               |          |      |        |
| 4. API Security       |          |      |        |
| 5. Security Headers   |          |      |        |
| 6. CORS               |          |      |        |
| 7. Logging            |          |      |        |
| 8. Infrastructure     |          |      |        |
| 9. Dependencies       |          |      |        |
| 10. Frontend          |          |      |        |
| 11. Incident Response |          |      |        |
| 12. Compliance        |          |      |        |

**Final Approval**:

| Role            | Name | Signature | Date |
| --------------- | ---- | --------- | ---- |
| Security Lead   |      |           |      |
| Tech Lead       |      |           |      |
| Operations Lead |      |           |      |

---

## Notes

- Items marked \*\* are mandatory and block production deployment
- This checklist should be reviewed and updated for each release
- Evidence of completion should be documented
- Regular security reviews should be scheduled post-deployment
