# GCRF Library Management System - Security Operations Guide

**Version**: 1.0
**Last Updated**: 2025-12-01
**Classification**: Internal Use Only

---

## Table of Contents

1. [Key Management](#1-key-management)
2. [Secret Rotation Procedures](#2-secret-rotation-procedures)
3. [Security Event Response](#3-security-event-response)
4. [Log Audit Procedures](#4-log-audit-procedures)
5. [Access Control Management](#5-access-control-management)
6. [Vulnerability Management](#6-vulnerability-management)
7. [Backup and Recovery](#7-backup-and-recovery)
8. [Compliance Monitoring](#8-compliance-monitoring)

---

## 1. Key Management

### 1.1 Key Inventory

| Key/Secret           | Location             | Rotation Period | Owner         |
| -------------------- | -------------------- | --------------- | ------------- |
| JWT Secret           | Environment Variable | 90 days         | Security Team |
| PostgreSQL Password  | Vault/Environment    | 90 days         | DBA Team      |
| Redis Password       | Vault/Environment    | 90 days         | DevOps        |
| RabbitMQ Password    | Vault/Environment    | 90 days         | DevOps        |
| MinIO Access Keys    | Vault/Environment    | 90 days         | DevOps        |
| Nacos Credentials    | Vault/Environment    | 90 days         | DevOps        |
| SSL/TLS Certificates | Certificate Store    | 365 days        | Security Team |

### 1.2 Key Generation Standards

#### JWT Secret Key

```bash
# Generate a 512-bit (64 byte) secret for HS512
openssl rand -base64 64

# Or using /dev/urandom
head -c 64 /dev/urandom | base64
```

**Requirements:**

- Minimum 256 bits (32 bytes) for HS256
- Minimum 512 bits (64 bytes) for HS512
- Use cryptographically secure random generator
- Never use predictable values or patterns

#### Database Passwords

```bash
# Generate strong password (32 characters)
openssl rand -base64 32 | tr -d '='

# Using pwgen
pwgen -s 32 1
```

**Requirements:**

- Minimum 32 characters
- Mix of uppercase, lowercase, numbers, symbols
- No dictionary words
- Unique per environment and service

#### Service API Keys

```bash
# Generate UUID-based API key
uuidgen | tr -d '-'

# Or with prefix for identification
echo "gcrf_$(openssl rand -hex 16)"
```

### 1.3 Key Storage

#### Production Environment

**Recommended: HashiCorp Vault**

```bash
# Store JWT secret in Vault
vault kv put secret/gcrf/jwt secret="$(openssl rand -base64 64)"

# Retrieve secret
vault kv get -field=secret secret/gcrf/jwt
```

**Alternative: AWS Secrets Manager**

```bash
# Store secret
aws secretsmanager create-secret \
    --name gcrf/jwt-secret \
    --secret-string "$(openssl rand -base64 64)"

# Retrieve secret
aws secretsmanager get-secret-value --secret-id gcrf/jwt-secret
```

#### Key Access Control

1. Only authorized personnel can access production keys
2. Key access is logged and audited
3. Keys are never stored in:
   - Version control
   - Plain text files
   - Logs
   - Email/chat
   - Documentation

---

## 2. Secret Rotation Procedures

### 2.1 JWT Secret Rotation

**Frequency:** Every 90 days or immediately if compromised

**Procedure:**

1. **Preparation** (T-7 days)

   ```bash
   # Generate new JWT secret
   NEW_JWT_SECRET=$(openssl rand -base64 64)

   # Store in Vault
   vault kv put secret/gcrf/jwt-new secret="${NEW_JWT_SECRET}"
   ```

2. **Dual-Key Period** (T-0)

   ```yaml
   # Update application to accept both old and new keys
   jwt:
     secrets:
       - ${JWT_SECRET_OLD}
       - ${JWT_SECRET_NEW}
   ```

3. **Full Rotation** (T+1 day)
   - Remove old secret from configuration
   - Update Vault with new secret as primary
   - All existing tokens will expire naturally

4. **Verification**

   ```bash
   # Test authentication with new tokens
   curl -X POST http://api.example.com/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"test","password":"test"}'
   ```

5. **Cleanup** (T+7 days)
   - Remove old secret from Vault
   - Update documentation
   - Log rotation completion

### 2.2 Database Password Rotation

**Frequency:** Every 90 days or immediately if compromised

**Procedure:**

1. **Generate New Password**

   ```bash
   NEW_DB_PASSWORD=$(openssl rand -base64 32 | tr -d '=')
   ```

2. **Create New Database User**

   ```sql
   -- Connect as superuser
   CREATE USER gcrf_app_new WITH PASSWORD 'new_password';
   GRANT ALL PRIVILEGES ON DATABASE gcrf_auth TO gcrf_app_new;
   GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO gcrf_app_new;
   ```

3. **Update Application Configuration**
   - Update Vault/environment with new credentials
   - Rolling restart of application instances

4. **Verify Connectivity**

   ```bash
   psql -h postgres-primary -U gcrf_app_new -d gcrf_auth -c "SELECT 1"
   ```

5. **Revoke Old Credentials**
   ```sql
   REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM gcrf_app_old;
   DROP USER gcrf_app_old;
   ```

### 2.3 Redis Password Rotation

**Frequency:** Every 90 days

**Procedure:**

1. **Update Redis Configuration**

   ```bash
   # Generate new password
   NEW_REDIS_PASSWORD=$(openssl rand -base64 32 | tr -d '=')

   # Update Redis (Redis 6+)
   redis-cli CONFIG SET requirepass "${NEW_REDIS_PASSWORD}"
   ```

2. **Update Application Configuration**
   - Update Vault/environment
   - Rolling restart of services

3. **Verify Connectivity**
   ```bash
   redis-cli -a "${NEW_REDIS_PASSWORD}" PING
   ```

### 2.4 SSL Certificate Rotation

**Frequency:** Before expiration (at least 30 days prior)

**Procedure:**

1. **Generate CSR**

   ```bash
   openssl req -new -key server.key -out server.csr \
     -subj "/CN=library.gcrf.com/O=GCRF/C=CN"
   ```

2. **Obtain New Certificate**
   - Submit CSR to Certificate Authority
   - Or use Let's Encrypt for automated renewal

3. **Install Certificate**

   ```bash
   # Backup old certificate
   cp /etc/ssl/certs/server.crt /etc/ssl/certs/server.crt.bak

   # Install new certificate
   cp new_server.crt /etc/ssl/certs/server.crt
   ```

4. **Restart Services**

   ```bash
   nginx -t && nginx -s reload
   ```

5. **Verify Certificate**
   ```bash
   openssl s_client -connect library.gcrf.com:443 2>/dev/null | \
     openssl x509 -noout -dates
   ```

---

## 3. Security Event Response

### 3.1 Incident Classification

| Severity    | Description                                   | Response Time | Escalation    |
| ----------- | --------------------------------------------- | ------------- | ------------- |
| P1 Critical | Active breach, data exfiltration              | Immediate     | CISO, Legal   |
| P2 High     | Suspected breach, auth bypass                 | 1 hour        | Security Lead |
| P3 Medium   | Vulnerability discovered, policy violation    | 4 hours       | Team Lead     |
| P4 Low      | Minor security issue, best practice deviation | 24 hours      | On-call       |

### 3.2 Incident Response Workflow

```
Detection -> Triage -> Containment -> Eradication -> Recovery -> Lessons Learned
```

#### Step 1: Detection

- Automated alerts from monitoring
- User reports
- Security scan findings
- Third-party notifications

#### Step 2: Triage

1. Classify severity level
2. Identify affected systems
3. Assess impact scope
4. Document initial findings

#### Step 3: Containment

**Immediate Actions:**

```bash
# Block suspicious IP
iptables -I INPUT -s <suspicious_ip> -j DROP

# Revoke compromised token (add to blacklist)
redis-cli SETEX "auth:blacklist:<token_hash>" 86400 "compromised"

# Disable compromised account
curl -X PUT http://internal-api/api/v1/users/<user_id>/disable
```

**Network Isolation:**

```bash
# Isolate affected container
docker network disconnect gcrf-network <container_id>
```

#### Step 4: Eradication

1. Identify root cause
2. Remove malicious artifacts
3. Patch vulnerabilities
4. Rotate compromised credentials

#### Step 5: Recovery

1. Restore from clean backup if needed
2. Gradually restore services
3. Monitor for recurring issues
4. Verify system integrity

#### Step 6: Post-Incident

1. Complete incident report
2. Update procedures
3. Implement preventive measures
4. Conduct post-mortem meeting

### 3.3 Communication Templates

#### Internal Notification

```
Subject: [SECURITY-P{SEVERITY}] Security Incident - {TITLE}

Incident ID: {ID}
Severity: P{SEVERITY}
Status: {STATUS}

Summary:
{Brief description}

Impact:
{Affected systems/users}

Actions Taken:
{List of actions}

Next Steps:
{Planned actions}

Contact: {Security Lead}
```

#### External Breach Notification

```
Subject: Important Security Notice from GCRF Library

Dear User,

We are writing to inform you of a security incident that may have
affected your account.

What Happened:
{Description}

Information Involved:
{Types of data}

What We Are Doing:
{Remediation steps}

What You Can Do:
{User recommendations}

For questions, contact: security@gcrf.com

Sincerely,
GCRF Security Team
```

---

## 4. Log Audit Procedures

### 4.1 Log Sources

| Source              | Location               | Retention | Review Frequency |
| ------------------- | ---------------------- | --------- | ---------------- |
| Gateway Access Logs | /var/log/gcrf/gateway/ | 90 days   | Daily            |
| Auth Service Logs   | /var/log/gcrf/auth/    | 180 days  | Daily            |
| Database Audit Logs | PostgreSQL log         | 365 days  | Weekly           |
| Security Events     | SIEM                   | 365 days  | Real-time        |

### 4.2 Security Events to Monitor

#### Critical Events (Real-time alerts)

- Multiple failed login attempts (>5 in 5 minutes)
- Successful login after multiple failures
- Access from unusual geographic location
- Privilege escalation attempts
- SQL injection patterns detected
- Admin account activities

#### High Priority Events (Daily review)

- New user registrations
- Password changes/resets
- Permission changes
- API key creations
- Configuration changes

### 4.3 Log Analysis Queries

#### Failed Login Attempts

```sql
-- PostgreSQL audit log
SELECT
    timestamp,
    username,
    client_ip,
    COUNT(*) as attempts
FROM auth_logs
WHERE event_type = 'LOGIN_FAILED'
AND timestamp > NOW() - INTERVAL '1 hour'
GROUP BY timestamp, username, client_ip
HAVING COUNT(*) > 5;
```

#### Unusual Access Patterns

```bash
# Using grep/awk for gateway logs
grep "401\|403" /var/log/gcrf/gateway/access.log | \
  awk '{print $1}' | sort | uniq -c | sort -rn | head -20
```

#### Successful Auth After Failures

```sql
SELECT
    l1.username,
    l1.client_ip,
    l1.timestamp as success_time,
    (
        SELECT COUNT(*)
        FROM auth_logs l2
        WHERE l2.username = l1.username
        AND l2.event_type = 'LOGIN_FAILED'
        AND l2.timestamp BETWEEN l1.timestamp - INTERVAL '1 hour' AND l1.timestamp
    ) as prior_failures
FROM auth_logs l1
WHERE l1.event_type = 'LOGIN_SUCCESS'
AND l1.timestamp > NOW() - INTERVAL '24 hours';
```

### 4.4 Audit Report Template

```markdown
# Security Audit Report - {DATE}

## Summary

- Total Events: {COUNT}
- Security Alerts: {COUNT}
- Incidents: {COUNT}

## Authentication Events

- Successful Logins: {COUNT}
- Failed Logins: {COUNT}
- Password Resets: {COUNT}
- Account Lockouts: {COUNT}

## Access Anomalies

{List of anomalies}

## Action Items

{Required follow-up actions}

## Compliance Status

- Log Retention: {COMPLIANT/NON-COMPLIANT}
- Access Logging: {COMPLIANT/NON-COMPLIANT}

Reviewed by: {NAME}
Date: {DATE}
```

---

## 5. Access Control Management

### 5.1 User Access Lifecycle

```
Request -> Approval -> Provisioning -> Review -> Deprovisioning
```

### 5.2 Role Definitions

| Role          | Description          | Permissions            |
| ------------- | -------------------- | ---------------------- |
| SUPER_ADMIN   | System administrator | Full system access     |
| LIBRARY_ADMIN | Library manager      | Book/reader management |
| LIBRARIAN     | Staff member         | Circulation operations |
| READER        | End user             | Self-service functions |

### 5.3 Access Review Procedure

**Frequency:** Quarterly

**Checklist:**

- [ ] Review all user accounts
- [ ] Verify role assignments
- [ ] Remove inactive accounts (>90 days)
- [ ] Verify admin accounts are necessary
- [ ] Check service account permissions
- [ ] Review API key usage
- [ ] Document access exceptions

### 5.4 Privileged Access Management

**Admin Account Rules:**

1. Individual admin accounts (no shared accounts)
2. Multi-factor authentication required
3. Session timeout: 15 minutes
4. Access logging enabled
5. Regular access reviews

**Break-Glass Procedure:**

1. Emergency access request submitted
2. Approval by two authorized personnel
3. Time-limited access granted
4. All actions logged
5. Post-incident review required

---

## 6. Vulnerability Management

### 6.1 Scanning Schedule

| Scan Type           | Frequency     | Tool                   | Owner    |
| ------------------- | ------------- | ---------------------- | -------- |
| Dependency Scan     | Daily (CI/CD) | OWASP Dependency-Check | DevOps   |
| Container Scan      | On Build      | Trivy                  | DevOps   |
| DAST Scan           | Weekly        | OWASP ZAP              | Security |
| Infrastructure Scan | Monthly       | Nessus                 | Security |
| Penetration Test    | Annually      | External Vendor        | Security |

### 6.2 Vulnerability Severity Response

| Severity | CVSS Score | Response Time | Actions                            |
| -------- | ---------- | ------------- | ---------------------------------- |
| Critical | 9.0-10.0   | 24 hours      | Immediate patch, incident response |
| High     | 7.0-8.9    | 7 days        | Priority patch, risk assessment    |
| Medium   | 4.0-6.9    | 30 days       | Scheduled patch                    |
| Low      | 0.1-3.9    | 90 days       | Next release cycle                 |

### 6.3 Patch Management Process

1. **Assessment**
   - Review vulnerability details
   - Assess applicability
   - Evaluate risk

2. **Testing**
   - Apply patch in staging
   - Run regression tests
   - Verify functionality

3. **Deployment**
   - Schedule maintenance window
   - Deploy with rollback plan
   - Monitor for issues

4. **Verification**
   - Confirm patch applied
   - Re-scan for vulnerability
   - Update tracking system

---

## 7. Backup and Recovery

### 7.1 Backup Schedule

| System        | Type         | Frequency    | Retention | Location |
| ------------- | ------------ | ------------ | --------- | -------- |
| PostgreSQL    | Full         | Daily        | 30 days   | Off-site |
| PostgreSQL    | Incremental  | Hourly       | 7 days    | Local    |
| Redis         | RDB Snapshot | Every 15 min | 24 hours  | Local    |
| MinIO         | Object Sync  | Continuous   | 90 days   | Off-site |
| Configuration | Git          | On Change    | Unlimited | Remote   |

### 7.2 Recovery Procedures

#### Database Recovery

```bash
# Restore from backup
pg_restore -h localhost -U postgres -d gcrf_auth -c backup.dump

# Point-in-time recovery
pg_restore -h localhost -U postgres -d gcrf_auth \
  --target-time="2025-12-01 10:00:00" backup.dump
```

#### Application Recovery

```bash
# Restore configuration
git checkout <commit_hash> -- src/main/resources/

# Rebuild and deploy
mvn clean package -DskipTests
docker-compose up -d --build
```

### 7.3 Recovery Testing

**Frequency:** Quarterly

**Test Scenarios:**

1. Single service failure
2. Database corruption
3. Complete site failover
4. Data restoration from backup

---

## 8. Compliance Monitoring

### 8.1 Compliance Requirements

| Framework       | Requirement                | Status      | Evidence       |
| --------------- | -------------------------- | ----------- | -------------- |
| OWASP Top 10    | Address all categories     | In Progress | Security audit |
| Data Protection | Encryption at rest/transit | Compliant   | Config review  |
| Access Control  | RBAC implementation        | Compliant   | Access review  |
| Logging         | Security event logging     | Compliant   | Log audit      |

### 8.2 Compliance Metrics

**Key Performance Indicators:**

| Metric                       | Target          | Current | Trend |
| ---------------------------- | --------------- | ------- | ----- |
| MTTR (Security Incidents)    | < 4 hours       | -       | -     |
| Vulnerability Remediation    | < 7 days (High) | -       | -     |
| Access Review Completion     | 100% quarterly  | -       | -     |
| Security Training Completion | 100% annual     | -       | -     |
| Patch Compliance             | > 95%           | -       | -     |

### 8.3 Audit Preparation

**Documentation Required:**

- [ ] Security policies
- [ ] Access control lists
- [ ] Incident response logs
- [ ] Change management records
- [ ] Vulnerability scan reports
- [ ] Penetration test results
- [ ] Training records
- [ ] Backup test results

---

## Appendix A: Contact Information

| Role             | Name | Contact           | Availability   |
| ---------------- | ---- | ----------------- | -------------- |
| Security Lead    | TBD  | security@gcrf.com | Business Hours |
| On-Call Security | TBD  | +86-xxx-xxxx      | 24/7           |
| DBA Team         | TBD  | dba@gcrf.com      | Business Hours |
| DevOps Team      | TBD  | devops@gcrf.com   | Business Hours |

## Appendix B: Tool Access

| Tool       | URL                              | Access        |
| ---------- | -------------------------------- | ------------- |
| Vault      | https://vault.gcrf.internal      | Security Team |
| SIEM       | https://siem.gcrf.internal       | Security Team |
| Prometheus | https://prometheus.gcrf.internal | DevOps        |
| Grafana    | https://grafana.gcrf.internal    | All Staff     |

## Appendix C: Document History

| Version | Date       | Author        | Changes         |
| ------- | ---------- | ------------- | --------------- |
| 1.0     | 2025-12-01 | Security Team | Initial version |
