# Secrets Management Strategy for GCRF Library Management System

**Version**: 1.0.0
**Last Updated**: 2025-11-01
**Security Level**: CONFIDENTIAL

---

## Table of Contents
1. [JWT Secret Requirements](#1-jwt-secret-requirements)
2. [Password Security Requirements](#2-password-security-requirements)
3. [Security Best Practices](#3-security-best-practices)
4. [Secret Generation Commands](#4-secret-generation-commands)
5. [Environment Configuration](#5-environment-configuration)
6. [Security Checklist](#6-security-checklist)
7. [Incident Response](#7-incident-response)

---

## 1. JWT Secret Requirements

### 1.1 Secret Specifications

#### Minimum Requirements
- **Length**: ≥ 64 characters (512 bits for HS512)
- **Character Set**: Base64-encoded random bytes
- **Uniqueness**: Different secrets for each environment
- **Algorithm**: HS512 (HMAC-SHA512)

#### Recommended Configuration
```yaml
# application-prod.yml
jwt:
  secret: ${JWT_SECRET}  # Injected from environment
  expiration: 86400000   # 24 hours in milliseconds
  refresh-expiration: 604800000  # 7 days
  algorithm: HS512
  issuer: gcrf-library-system
  audience: gcrf-api
```

### 1.2 Secret Generation Methods

#### Method 1: OpenSSL (Recommended for Production)
```bash
# Generate 64-byte (512-bit) random secret
openssl rand -base64 64 | tr -d '\n'

# Generate with specific length
openssl rand -base64 80 | tr -d '\n'

# Generate and save to file (never commit this file)
openssl rand -base64 64 | tr -d '\n' > jwt-secret.key
chmod 600 jwt-secret.key
```

#### Method 2: Using /dev/urandom (Linux/Mac)
```bash
# Generate 64-byte secret
head -c 64 /dev/urandom | base64 | tr -d '\n'

# Alternative with tr
< /dev/urandom tr -dc 'A-Za-z0-9!@#$%^&*()_+=' | head -c 64
```

#### Method 3: Using Node.js
```javascript
// generate-secret.js
const crypto = require('crypto');
const secret = crypto.randomBytes(64).toString('base64');
console.log(secret);
```

#### Method 4: Using Python
```python
# generate_secret.py
import secrets
import base64

secret = base64.b64encode(secrets.token_bytes(64)).decode('utf-8')
print(secret)
```

### 1.3 JWT Secret Rotation Policy

#### Rotation Schedule
- **Production**: Every 90 days (mandatory)
- **Staging**: Every 180 days
- **Development**: Every 365 days or on team member change

#### Rotation Procedure
```bash
#!/bin/bash
# jwt-rotation.sh

# 1. Generate new secret
NEW_SECRET=$(openssl rand -base64 64 | tr -d '\n')
OLD_SECRET=${JWT_SECRET}

# 2. Update environment variable
export JWT_SECRET_NEW=$NEW_SECRET
export JWT_SECRET_OLD=$OLD_SECRET

# 3. Deploy with dual validation (grace period)
# Application should validate both old and new secrets

# 4. After grace period (24-48 hours), remove old secret
unset JWT_SECRET_OLD
export JWT_SECRET=$JWT_SECRET_NEW
```

#### Dual-Key Validation Implementation
```java
@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String currentSecret;

    @Value("${jwt.secret.old:}")
    private String oldSecret;

    public boolean validateToken(String token) {
        try {
            // Try current secret first
            Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(currentSecret.getBytes()))
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            if (StringUtils.hasText(oldSecret)) {
                // Try old secret during rotation period
                try {
                    Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(oldSecret.getBytes()))
                        .build()
                        .parseClaimsJws(token);
                    return true;
                } catch (JwtException ex) {
                    return false;
                }
            }
            return false;
        }
    }
}
```

### 1.4 JWT Security Best Practices

#### Token Configuration
```java
@Configuration
public class JwtConfiguration {

    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return NimbusJwtDecoder.withSecretKey(key)
            .macAlgorithm(MacAlgorithm.HS512)
            .build();
    }

    @Bean
    public JwtEncoder jwtEncoder(@Value("${jwt.secret}") String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        JWSSigner signer = new MACSigner(key);
        NimbusJwsEncoder encoder = new NimbusJwsEncoder(signer);
        return encoder;
    }
}
```

#### Security Headers
```java
// Add to token response
response.setHeader("Cache-Control", "no-store");
response.setHeader("Pragma", "no-cache");
response.setHeader("X-Content-Type-Options", "nosniff");
```

---

## 2. Password Security Requirements

### 2.1 PostgreSQL Database Passwords

#### Requirements
- **Length**: ≥ 20 characters
- **Complexity**: Mixed case, numbers, special characters
- **Special Characters**: Avoid `@`, `#`, `%` in connection strings
- **Rotation**: Every 90 days for production

#### Generation Commands
```bash
# PostgreSQL password (URL-safe)
openssl rand -base64 20 | tr -d '/@#%\n' | head -c 20

# Alternative with specific characters
< /dev/urandom tr -dc 'A-Za-z0-9!$^&*()_+-=' | head -c 24
```

#### Connection String Format
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 2.2 Redis Passwords

#### Requirements
- **Length**: ≥ 32 characters
- **Format**: Alphanumeric with special characters
- **ACL**: Use Redis ACL for user-specific permissions

#### Generation Commands
```bash
# Redis password
openssl rand -base64 32 | tr -d '\n'

# Redis ACL user creation
echo "ACL SETUSER service-user on +@all ~* &* >${REDIS_PASSWORD}"
```

#### Configuration
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}
      timeout: 6000ms
      jedis:
        pool:
          max-active: 10
          max-idle: 8
          min-idle: 5
          max-wait: -1ms
```

### 2.3 Nacos Admin Passwords

#### Requirements
- **Length**: ≥ 16 characters
- **Complexity**: Must include uppercase, lowercase, numbers
- **Authentication**: Enable authentication in production

#### Configuration
```properties
# nacos/conf/application.properties
nacos.core.auth.enabled=true
nacos.core.auth.system.type=nacos
nacos.core.auth.default.token.secret.key=${NACOS_AUTH_SECRET}
nacos.core.auth.plugin.nacos.token.secret.key=${NACOS_AUTH_SECRET}
server.servlet.contextPath=/nacos
```

### 2.4 User Password Requirements

#### Validation Rules
```java
@Component
public class PasswordValidator {

    private static final String PASSWORD_PATTERN =
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{12,}$";

    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    public ValidationResult validate(String password) {
        ValidationResult result = new ValidationResult();

        if (password.length() < 12) {
            result.addError("Password must be at least 12 characters long");
        }

        if (password.length() > 128) {
            result.addError("Password must not exceed 128 characters");
        }

        if (!password.matches(".*[A-Z].*")) {
            result.addError("Password must contain at least one uppercase letter");
        }

        if (!password.matches(".*[a-z].*")) {
            result.addError("Password must contain at least one lowercase letter");
        }

        if (!password.matches(".*[0-9].*")) {
            result.addError("Password must contain at least one digit");
        }

        if (!password.matches(".*[@#$%^&+=!].*")) {
            result.addError("Password must contain at least one special character");
        }

        // Check for common passwords
        if (isCommonPassword(password)) {
            result.addError("Password is too common, please choose another");
        }

        // Check for sequential characters
        if (hasSequentialCharacters(password)) {
            result.addError("Password contains sequential characters");
        }

        return result;
    }

    private boolean isCommonPassword(String password) {
        // Load from common passwords list
        Set<String> commonPasswords = loadCommonPasswords();
        return commonPasswords.contains(password.toLowerCase());
    }

    private boolean hasSequentialCharacters(String password) {
        String sequences = "abcdefghijklmnopqrstuvwxyz0123456789";
        String reversedSequences = new StringBuilder(sequences).reverse().toString();

        for (int i = 0; i <= password.length() - 3; i++) {
            String substring = password.substring(i, i + 3).toLowerCase();
            if (sequences.contains(substring) || reversedSequences.contains(substring)) {
                return true;
            }
        }
        return false;
    }
}
```

#### Password Hashing Configuration
```java
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Use Argon2 for new projects (more secure than BCrypt)
        return new Argon2PasswordEncoder(16, 32, 1, 4096, 3);

        // Alternative: BCrypt with strength 12
        // return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DelegatingPasswordEncoder delegatingPasswordEncoder() {
        // Support multiple encoders for migration
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("argon2", new Argon2PasswordEncoder(16, 32, 1, 4096, 3));
        encoders.put("bcrypt", new BCryptPasswordEncoder(12));
        encoders.put("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());

        DelegatingPasswordEncoder delegatingPasswordEncoder =
            new DelegatingPasswordEncoder("argon2", encoders);

        // Set default for encoding
        delegatingPasswordEncoder.setDefaultPasswordEncoderForMatches(
            new Argon2PasswordEncoder(16, 32, 1, 4096, 3)
        );

        return delegatingPasswordEncoder;
    }
}
```

---

## 3. Security Best Practices

### 3.1 Preventing Secrets from Leaking to Git

#### .gitignore Configuration
```gitignore
# Environment files
.env
.env.*
!.env.example
*.env

# Secret files
*.key
*.pem
*.p12
*.jks
*.keystore
*.truststore
jwt-secret.key

# Configuration with secrets
application-local.yml
application-prod.yml
application-secrets.yml
bootstrap-prod.yml

# IDE files that might contain secrets
.idea/
*.iml
.vscode/settings.json

# Docker secrets
docker-compose.override.yml
secrets/
```

#### Pre-commit Hook
```bash
#!/bin/bash
# .git/hooks/pre-commit

# Check for potential secrets
SECRET_PATTERNS=(
    "password.*=.*['\"].*['\"]"
    "secret.*=.*['\"].*['\"]"
    "api[_-]?key.*=.*['\"].*['\"]"
    "token.*=.*['\"].*['\"]"
    "private[_-]?key"
    "BEGIN RSA PRIVATE KEY"
    "BEGIN OPENSSH PRIVATE KEY"
    "BEGIN DSA PRIVATE KEY"
    "BEGIN EC PRIVATE KEY"
    "BEGIN PGP PRIVATE KEY"
)

for pattern in "${SECRET_PATTERNS[@]}"; do
    if git diff --cached --name-only | xargs grep -E "$pattern" 2>/dev/null; then
        echo "ERROR: Potential secret detected in commit"
        echo "Please remove sensitive data before committing"
        exit 1
    fi
done

# Check for specific file extensions
FORBIDDEN_EXTENSIONS=(
    "*.key"
    "*.pem"
    "*.p12"
    "*.jks"
    ".env"
)

for ext in "${FORBIDDEN_EXTENSIONS[@]}"; do
    if git diff --cached --name-only | grep -E "$ext$"; then
        echo "ERROR: Forbidden file type detected: $ext"
        exit 1
    fi
done

exit 0
```

### 3.2 Environment Variable Injection

#### Docker Compose Configuration
```yaml
version: '3.8'

services:
  auth-service:
    image: gcrf-auth-service:latest
    env_file:
      - .env.docker
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JWT_SECRET=${JWT_SECRET}
      - DB_PASSWORD=${AUTH_DB_PASSWORD}
      - REDIS_PASSWORD=${REDIS_PASSWORD}
    secrets:
      - jwt_secret
      - db_password

secrets:
  jwt_secret:
    file: ./secrets/jwt-secret.key
  db_password:
    file: ./secrets/db-password.txt
```

#### Kubernetes Secrets
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: gcrf-secrets
  namespace: gcrf-system
type: Opaque
data:
  jwt-secret: <base64-encoded-secret>
  db-password: <base64-encoded-password>
  redis-password: <base64-encoded-password>
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
spec:
  template:
    spec:
      containers:
      - name: auth-service
        env:
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: gcrf-secrets
              key: jwt-secret
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: gcrf-secrets
              key: db-password
```

#### Spring Boot Configuration
```java
@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    public void validateConfiguration() {
        Assert.hasText(jwtSecret, "JWT secret must not be empty");
        Assert.isTrue(jwtSecret.length() >= 64,
            "JWT secret must be at least 64 characters");

        // Check if using default/example secret
        if (jwtSecret.equals("change-this-secret-in-production")) {
            throw new IllegalStateException(
                "Default JWT secret detected. Please set a secure secret!");
        }
    }
}
```

### 3.3 Development vs Production Separation

#### Environment-Specific Files
```
/config/
├── application.yml              # Shared configuration
├── application-dev.yml          # Development (can be committed)
├── application-prod.yml.template # Production template (committed)
├── .env.example                 # Example environment variables
├── .env.development            # Development secrets (gitignored)
├── .env.production             # Production secrets (gitignored)
└── vault/
    ├── dev-secrets.json        # Development Vault config
    └── prod-secrets.json       # Production Vault config
```

#### .env.example Template
```bash
# JWT Configuration
JWT_SECRET=generate-using-openssl-rand-base64-64
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=gcrf_library
DB_USERNAME=gcrf_admin
DB_PASSWORD=generate-strong-password-here

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=generate-redis-password-here

# Nacos Configuration
NACOS_SERVER_ADDR=localhost:8848
NACOS_USERNAME=nacos
NACOS_PASSWORD=generate-nacos-password-here
NACOS_AUTH_SECRET=generate-nacos-secret-here

# Service Ports
GATEWAY_PORT=8080
AUTH_SERVICE_PORT=8081
BOOK_SERVICE_PORT=8082
CIRCULATION_SERVICE_PORT=8083
READER_SERVICE_PORT=8084
SYSTEM_SERVICE_PORT=8085
NOTIFICATION_SERVICE_PORT=8086

# Monitoring
ACTUATOR_USERNAME=admin
ACTUATOR_PASSWORD=generate-actuator-password-here
```

---

## 4. Secret Generation Commands

### 4.1 Complete Secret Generation Script
```bash
#!/bin/bash
# generate-secrets.sh

echo "Generating secure secrets for GCRF Library Management System..."

# Function to generate secure random string
generate_secret() {
    local length=$1
    openssl rand -base64 "$length" | tr -d '\n' | head -c "$length"
}

# Function to generate URL-safe password
generate_password() {
    local length=$1
    < /dev/urandom tr -dc 'A-Za-z0-9!$^*()_+-=' | head -c "$length"
}

# Generate secrets
JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
DB_PASSWORD=$(generate_password 24)
REDIS_PASSWORD=$(generate_password 32)
NACOS_PASSWORD=$(generate_password 20)
NACOS_AUTH_SECRET=$(generate_secret 32)
ACTUATOR_PASSWORD=$(generate_password 20)

# Create .env file
cat > .env.generated <<EOF
# Generated on $(date)
# WARNING: This file contains sensitive information. Do not commit to Git!

# JWT Configuration
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=gcrf_library
DB_USERNAME=gcrf_admin
DB_PASSWORD=${DB_PASSWORD}

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=${REDIS_PASSWORD}

# Nacos Configuration
NACOS_SERVER_ADDR=localhost:8848
NACOS_USERNAME=nacos
NACOS_PASSWORD=${NACOS_PASSWORD}
NACOS_AUTH_SECRET=${NACOS_AUTH_SECRET}

# Monitoring
ACTUATOR_USERNAME=admin
ACTUATOR_PASSWORD=${ACTUATOR_PASSWORD}
EOF

# Set appropriate permissions
chmod 600 .env.generated

echo "Secrets generated successfully in .env.generated"
echo "Remember to:"
echo "1. Review the generated secrets"
echo "2. Move to appropriate location"
echo "3. Never commit this file to Git"
echo "4. Store securely in your secrets management system"
```

### 4.2 Secret Validation Script
```bash
#!/bin/bash
# validate-secrets.sh

validate_secret() {
    local name=$1
    local value=$2
    local min_length=$3

    if [ -z "$value" ]; then
        echo "❌ $name is not set"
        return 1
    fi

    if [ ${#value} -lt $min_length ]; then
        echo "❌ $name is too short (minimum $min_length characters)"
        return 1
    fi

    echo "✅ $name is valid"
    return 0
}

# Load environment variables
source .env

# Validate secrets
ERRORS=0

validate_secret "JWT_SECRET" "$JWT_SECRET" 64 || ((ERRORS++))
validate_secret "DB_PASSWORD" "$DB_PASSWORD" 20 || ((ERRORS++))
validate_secret "REDIS_PASSWORD" "$REDIS_PASSWORD" 32 || ((ERRORS++))
validate_secret "NACOS_PASSWORD" "$NACOS_PASSWORD" 16 || ((ERRORS++))
validate_secret "NACOS_AUTH_SECRET" "$NACOS_AUTH_SECRET" 32 || ((ERRORS++))

if [ $ERRORS -eq 0 ]; then
    echo "✅ All secrets are valid"
    exit 0
else
    echo "❌ $ERRORS secret(s) failed validation"
    exit 1
fi
```

---

## 5. Environment Configuration

### 5.1 Spring Boot External Configuration
```yaml
# application.yml
spring:
  config:
    import:
      - optional:classpath:application-${spring.profiles.active}.yml
      - optional:file:./config/application-${spring.profiles.active}.yml
      - optional:file:${user.home}/.gcrf/application-${spring.profiles.active}.yml
```

### 5.2 Docker Environment Injection
```dockerfile
# Dockerfile
FROM openjdk:21-jdk-slim

# Create non-root user
RUN groupadd -r gcrf && useradd -r -g gcrf gcrf

# Copy application
COPY --chown=gcrf:gcrf target/*.jar app.jar

# Use non-root user
USER gcrf

# Set environment variables at runtime
ENTRYPOINT ["sh", "-c", "java \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
    -jar /app.jar"]
```

### 5.3 SystemD Service Configuration
```ini
# /etc/systemd/system/gcrf-auth-service.service
[Unit]
Description=GCRF Auth Service
After=network.target

[Service]
Type=simple
User=gcrf
Group=gcrf
EnvironmentFile=/etc/gcrf/auth-service.env
ExecStart=/usr/bin/java -jar /opt/gcrf/auth-service.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

---

## 6. Security Checklist

### 6.1 JWT Security Checklist

- [ ] JWT secret is at least 64 characters (512 bits)
- [ ] Secret is generated using cryptographically secure random source
- [ ] Different secrets for each environment (dev, staging, prod)
- [ ] Secret rotation policy documented and implemented
- [ ] Dual-key validation during rotation period
- [ ] Token expiration configured appropriately
- [ ] Refresh token mechanism implemented
- [ ] Token blacklist/revocation mechanism in place
- [ ] Secure token storage on client side
- [ ] HTTPS-only transmission enforced
- [ ] Token validation on every request
- [ ] Claims validation (issuer, audience, expiration)
- [ ] Rate limiting on token endpoints
- [ ] Audit logging for token generation/validation failures

### 6.2 Database Security Checklist

- [ ] Strong passwords (≥20 characters) for all database users
- [ ] Different passwords for each environment
- [ ] Database connection encrypted (SSL/TLS)
- [ ] Connection pool properly configured
- [ ] Prepared statements used exclusively
- [ ] Database user has minimum required privileges
- [ ] Regular password rotation schedule
- [ ] Audit logging enabled
- [ ] Backup encryption configured
- [ ] Network access restricted (firewall/security groups)
- [ ] Database patches up to date
- [ ] Connection timeout configured
- [ ] Max connections limited
- [ ] SQL injection prevention measures

### 6.3 Redis Security Checklist

- [ ] Strong password configured (≥32 characters)
- [ ] ACL users configured with specific permissions
- [ ] Network binding to specific interfaces only
- [ ] Protected mode enabled
- [ ] Command renaming/disabling for dangerous commands
- [ ] Max memory limit configured
- [ ] Persistence configured appropriately
- [ ] AOF/RDB backups encrypted
- [ ] SSL/TLS for client connections
- [ ] Connection limits configured
- [ ] Timeout for idle clients
- [ ] Monitoring and alerting configured

### 6.4 Nacos Security Checklist

- [ ] Authentication enabled in production
- [ ] Strong admin password
- [ ] Token secret key configured
- [ ] HTTPS enabled
- [ ] Access control configured
- [ ] Namespace isolation implemented
- [ ] Audit logging enabled
- [ ] Regular security updates
- [ ] Network access restricted
- [ ] Configuration encryption for sensitive data

### 6.5 Application Security Checklist

- [ ] All secrets loaded from environment variables
- [ ] No hardcoded secrets in code
- [ ] Secrets validation on startup
- [ ] Secure error messages (no secret leakage)
- [ ] HTTPS enforced for all endpoints
- [ ] Security headers configured
- [ ] CORS properly configured
- [ ] Rate limiting implemented
- [ ] Input validation on all endpoints
- [ ] Output encoding configured
- [ ] Session management secure
- [ ] CSRF protection enabled
- [ ] Audit logging comprehensive
- [ ] Security monitoring active

---

## 7. Incident Response

### 7.1 Secret Compromise Response

#### Immediate Actions (Within 1 Hour)
1. **Rotate compromised secret immediately**
```bash
# Generate new secret
NEW_SECRET=$(openssl rand -base64 64 | tr -d '\n')

# Update all services
kubectl set env deployment/auth-service JWT_SECRET=$NEW_SECRET

# Or for Docker Compose
docker-compose stop
export JWT_SECRET=$NEW_SECRET
docker-compose up -d
```

2. **Invalidate all existing tokens**
```java
@Service
public class TokenRevocationService {

    private final RedisTemplate<String, String> redisTemplate;

    public void revokeAllTokensBeforeTimestamp(long timestamp) {
        // Add global revocation timestamp
        redisTemplate.opsForValue().set(
            "global:token:revocation:timestamp",
            String.valueOf(timestamp)
        );
    }

    public boolean isTokenRevoked(String jti, long issuedAt) {
        String globalRevocationTime = redisTemplate.opsForValue()
            .get("global:token:revocation:timestamp");

        if (globalRevocationTime != null) {
            long revocationTimestamp = Long.parseLong(globalRevocationTime);
            if (issuedAt < revocationTimestamp) {
                return true; // Token was issued before revocation
            }
        }

        // Check specific token revocation
        return redisTemplate.hasKey("revoked:token:" + jti);
    }
}
```

3. **Audit and investigate**
```sql
-- Check for suspicious activity
SELECT
    user_id,
    ip_address,
    user_agent,
    COUNT(*) as request_count,
    MIN(created_at) as first_seen,
    MAX(created_at) as last_seen
FROM audit_logs
WHERE created_at >= NOW() - INTERVAL '24 hours'
GROUP BY user_id, ip_address, user_agent
HAVING COUNT(*) > 100
ORDER BY request_count DESC;
```

#### Follow-up Actions (Within 24 Hours)
1. Force all users to re-authenticate
2. Review and update security policies
3. Conduct security audit
4. Document incident for compliance
5. Notify affected users if required

### 7.2 Security Monitoring

#### Key Metrics to Monitor
```java
@Component
public class SecurityMetricsCollector {

    private final MeterRegistry meterRegistry;

    public void recordAuthenticationAttempt(boolean success, String method) {
        meterRegistry.counter("auth.attempts",
            "success", String.valueOf(success),
            "method", method
        ).increment();
    }

    public void recordTokenValidation(boolean valid, String reason) {
        meterRegistry.counter("token.validation",
            "valid", String.valueOf(valid),
            "reason", reason
        ).increment();
    }

    public void recordSuspiciousActivity(String type, String userId) {
        meterRegistry.counter("security.suspicious",
            "type", type,
            "user", userId
        ).increment();

        // Alert if threshold exceeded
        if (getSuspiciousCount(userId) > 10) {
            alertSecurityTeam(userId, type);
        }
    }
}
```

#### Alerting Rules
```yaml
# Prometheus alerting rules
groups:
  - name: security_alerts
    interval: 30s
    rules:
      - alert: HighFailedAuthRate
        expr: rate(auth_attempts_total{success="false"}[5m]) > 10
        for: 2m
        annotations:
          summary: High rate of failed authentication attempts

      - alert: JWTValidationFailures
        expr: rate(token_validation_total{valid="false"}[5m]) > 5
        for: 2m
        annotations:
          summary: High rate of JWT validation failures

      - alert: SuspiciousActivity
        expr: rate(security_suspicious_total[5m]) > 1
        for: 1m
        annotations:
          summary: Suspicious security activity detected
```

---

## Appendices

### A. Common Passwords to Block
Store in `src/main/resources/blacklist/common-passwords.txt`:
```
password
123456
password123
admin
letmein
welcome
monkey
1234567890
qwerty
abc123
Password1
password1
123456789
welcome123
```

### B. Security Tools and Libraries

#### Dependencies
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>org.passay</groupId>
    <artifactId>passay</artifactId>
    <version>1.6.3</version>
</dependency>
```

### C. External Security Tools

1. **Secret Scanning**
   - GitGuardian
   - TruffleHog
   - git-secrets

2. **Dependency Scanning**
   - OWASP Dependency Check
   - Snyk
   - Dependabot

3. **Code Analysis**
   - SonarQube
   - SpotBugs
   - PMD

4. **Runtime Protection**
   - AWS Secrets Manager
   - HashiCorp Vault
   - Azure Key Vault

---

## Document Control

**Classification**: CONFIDENTIAL
**Version**: 1.0.0
**Last Review**: 2025-11-01
**Next Review**: 2025-02-01
**Owner**: Security Team
**Distribution**: Development Team, DevOps Team, Security Team

---

*This document contains sensitive security information. Handle according to company security policies.*