# GCRF Library Management System - Environment Variables Documentation

**Version**: 1.0.0
**Last Updated**: 2025-11-01
**Classification**: Internal Documentation

---

## Table of Contents

1. [Quick Start Guide](#quick-start-guide)
2. [Security Classification](#security-classification)
3. [Variable Groups](#variable-groups)
   - [Service Discovery & Configuration](#service-discovery--configuration)
   - [Security & Authentication](#security--authentication)
   - [Database Configuration](#database-configuration)
   - [Cache Configuration](#cache-configuration)
   - [Network & CORS](#network--cors)
   - [Server Configuration](#server-configuration)
   - [Performance Tuning](#performance-tuning)
   - [Monitoring & Logging](#monitoring--logging)
4. [Service-Specific Variables](#service-specific-variables)
   - [Gateway Service](#gateway-service-variables)
   - [Auth Service](#auth-service-variables)
5. [Cross-Service Variables](#cross-service-variables)
6. [Environment Templates](#environment-templates)
7. [Security Best Practices](#security-best-practices)
8. [Migration Guide](#migration-guide)
9. [Troubleshooting](#troubleshooting)
10. [Appendix](#appendix)

---

## Quick Start Guide

### Minimum Required Variables

For a minimal production deployment, you **MUST** set these variables:

```bash
# Critical Security Variables
export JWT_SECRET="<generate-with-openssl-rand-base64-96>"
export NACOS_USERNAME="<your-nacos-user>"
export NACOS_PASSWORD="<strong-password>"
export DB_PASSWORD="<database-password>"
export REDIS_PASSWORD="<redis-password>"

# Service Discovery
export NACOS_SERVER_ADDR="nacos-cluster.prod.internal:8848"

# Database
export DB_USERNAME="gcrf_prod_user"
```

### Generate Secure Values

```bash
# Generate JWT secret (minimum 64 characters for HS512)
openssl rand -base64 96

# Generate strong passwords (32+ characters)
openssl rand -base64 32

# Generate database password with special chars escaped
openssl rand -base64 32 | tr -d "=+/"
```

### Validate Configuration

```bash
# Download and run validation script
cd deployment/scripts
chmod +x validate-config.sh
./validate-config.sh prod
```

---

## Security Classification

Each variable is marked with a security level indicator:

- 🔴 **Secret** - Highly sensitive, requires encryption at rest
- 🟡 **Internal** - Internal use only, not exposed externally
- 🟢 **Public** - Can be safely exposed in logs/documentation

---

## Variable Groups

### Service Discovery & Configuration

#### NACOS_SERVER_ADDR

**Description**: Nacos server address for service discovery and configuration management

**Required**: No (has default)

**Default**: `localhost:8848` (dev) / `nacos:8848` (prod)

**Security Level**: 🟡 Internal

**Used By**: gateway-service, auth-service, all microservices

**Validation**:
- Format: `host:port` or comma-separated for cluster
- Port: 1-65535
- Pattern: `^[a-zA-Z0-9.-]+(:[0-9]+)?(,[a-zA-Z0-9.-]+(:[0-9]+)?)*$`

**Values by Environment**:
- **Development**: `localhost:8848`
- **Staging**: `nacos-staging.internal:8848`
- **Production**: `nacos-1.prod.internal:8848,nacos-2.prod.internal:8848,nacos-3.prod.internal:8848`

**Notes**:
- Use service name in Docker/Kubernetes environments
- For HA, provide comma-separated list of all Nacos nodes
- Connection automatically failover in cluster mode

---

#### NACOS_USERNAME

**Description**: Username for Nacos authentication

**Required**: Yes

**Default**: `nacos` (dev only)

**Security Level**: 🟡 Internal

**Used By**: gateway-service, auth-service, all microservices

**Validation**:
- Format: Alphanumeric with underscores
- Length: 3-50 characters
- Pattern: `^[a-zA-Z0-9_]{3,50}$`

**Values by Environment**:
- **Development**: `nacos`
- **Staging**: `staging_nacos_user`
- **Production**: `prod_nacos_readonly` (for services) / `prod_nacos_admin` (for management)

**Notes**:
- Create different users for read-only services and admin operations
- Rotate credentials every 90 days
- Never use default "nacos" in production

---

#### NACOS_PASSWORD

**Description**: Password for Nacos authentication

**Required**: Yes

**Default**: None

**Security Level**: 🔴 Secret

**Used By**: gateway-service, auth-service, all microservices

**Validation**:
- Length: Minimum 12 characters
- Complexity: Must contain uppercase, lowercase, numbers, and special characters
- Pattern: `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{12,}$`

**Values by Environment**:
- **Development**: `nacos` (weak, acceptable for local only)
- **Staging**: Generate with `openssl rand -base64 24`
- **Production**: Generate with `openssl rand -base64 32`

**Notes**:
- Store in secrets management system (Vault, AWS Secrets Manager)
- Rotate every 90 days
- Never log or expose in error messages
- Different password for each environment

---

#### NACOS_NAMESPACE

**Description**: Nacos namespace for environment isolation

**Required**: No

**Default**: `dev` (dev) / `production` (prod)

**Security Level**: 🟢 Public

**Used By**: gateway-service, auth-service, all microservices

**Validation**:
- Format: UUID or alphanumeric string
- Length: 1-128 characters
- Pattern: `^[a-zA-Z0-9-_]{1,128}$`

**Values by Environment**:
- **Development**: `dev` or `development`
- **Staging**: `staging`
- **Production**: `production` or UUID like `a1b2c3d4-e5f6-7890-abcd-ef1234567890`

**Notes**:
- Use namespaces to isolate environments completely
- Production should use UUID for additional security
- Cannot be changed without service restart

---

#### NACOS_GROUP

**Description**: Nacos configuration group for logical service grouping

**Required**: No

**Default**: `DEV_GROUP` (dev) / `GCRF_LIBRARY` (prod)

**Security Level**: 🟢 Public

**Used By**: gateway-service, auth-service, all microservices

**Validation**:
- Format: Alphanumeric with underscores
- Length: 1-128 characters
- Pattern: `^[a-zA-Z0-9_]{1,128}$`

**Values by Environment**:
- **Development**: `DEV_GROUP`
- **Staging**: `STAGING_GROUP`
- **Production**: `GCRF_LIBRARY` or `GCRF_PROD`

**Notes**:
- Use groups to organize related services
- Can be used for blue-green deployments

---

### Security & Authentication

#### JWT_SECRET

**Description**: Secret key for JWT token signing using HS512 algorithm

**Required**: Yes

**Default**: None (dev has insecure default in yml)

**Security Level**: 🔴 Secret

**Used By**: gateway-service, auth-service

**Validation**:
- Length: Minimum 64 characters (512 bits for HS512)
- Format: Base64 or alphanumeric string
- Recommended: 128+ characters for extra security

**Values by Environment**:
- **Development**: `DEV_SECRET_ONLY_NOT_FOR_PRODUCTION_USE_MINIMUM_64_CHARACTERS_REQUIRED_HERE`
- **Staging**: Generate unique secret
- **Production**: Generate unique secret with high entropy

**Generation**:
```bash
# Recommended: 128 character secret
openssl rand -base64 96

# Alternative: UUID-based (less secure)
uuidgen | sed 's/-//g' | head -c 64 | base64
```

**Notes**:
- CRITICAL: Must be identical across gateway and auth service
- Never use development secret in production
- Rotate every 90 days with grace period
- Store in dedicated secrets management
- Plan token migration strategy before rotation

---

#### JWT_EXPIRATION

**Description**: JWT access token expiration time in milliseconds

**Required**: No

**Default**: `86400000` (24 hours)

**Security Level**: 🟢 Public

**Used By**: gateway-service, auth-service

**Validation**:
- Format: Positive integer (milliseconds)
- Range: 300000 (5 min) - 86400000 (24 hours)
- Recommended: 900000 (15 min) - 3600000 (1 hour) for production

**Values by Environment**:
- **Development**: `86400000` (24 hours - convenient for development)
- **Staging**: `7200000` (2 hours)
- **Production**: `3600000` (1 hour) or `1800000` (30 minutes)

**Common Values**:
```
5 minutes:   300000
15 minutes:  900000
30 minutes:  1800000
1 hour:      3600000
2 hours:     7200000
24 hours:    86400000
```

**Notes**:
- Shorter expiration = better security, more refresh overhead
- Must balance security with user experience
- Consider business requirements (session length)

---

#### JWT_REFRESH_EXPIRATION

**Description**: JWT refresh token expiration time in milliseconds

**Required**: No

**Default**: `604800000` (7 days)

**Security Level**: 🟢 Public

**Used By**: auth-service

**Validation**:
- Format: Positive integer (milliseconds)
- Range: Should be > JWT_EXPIRATION
- Recommended: 7-30 days

**Values by Environment**:
- **Development**: `604800000` (7 days)
- **Staging**: `259200000` (3 days)
- **Production**: `604800000` (7 days) or `2592000000` (30 days)

**Notes**:
- Determines how long users stay logged in
- Store refresh tokens in Redis with expiration
- Implement token rotation for better security

---

#### JWT_ISSUER

**Description**: JWT token issuer identifier for validation

**Required**: No

**Default**: `GCRF-Library-System`

**Security Level**: 🟢 Public

**Used By**: gateway-service, auth-service

**Validation**:
- Format: String without spaces
- Length: 1-255 characters
- Pattern: `^[a-zA-Z0-9-_.]+$`

**Values by Environment**:
- **Development**: `GCRF-Library-System-Dev`
- **Staging**: `GCRF-Library-System-Staging`
- **Production**: `GCRF-Library-System` or `gcrf.com`

**Notes**:
- Used to validate token origin
- Can include environment for multi-env debugging
- Part of JWT claims validation

---

### Database Configuration

#### DB_HOST

**Description**: PostgreSQL database hostname or IP address

**Required**: No

**Default**: `localhost`

**Security Level**: 🟡 Internal

**Used By**: auth-service, book-service, reader-service, circulation-service

**Validation**:
- Format: Hostname, IP address, or Docker service name
- Pattern: Valid hostname or IPv4/IPv6 address

**Values by Environment**:
- **Development**: `localhost` or `127.0.0.1`
- **Docker**: `postgres` (service name)
- **Staging**: `postgres-staging.internal` or `10.0.1.50`
- **Production**: `postgres-primary.prod.internal` or managed service endpoint

**Notes**:
- Use service names in containerized environments
- Consider read replicas for scaling
- SSL/TLS required for production

---

#### DB_PORT

**Description**: PostgreSQL database port

**Required**: No

**Default**: `5432`

**Security Level**: 🟢 Public

**Used By**: auth-service, book-service, reader-service, circulation-service

**Validation**:
- Format: Integer
- Range: 1-65535
- Standard: 5432 (PostgreSQL default)

**Values by Environment**:
- **All Environments**: `5432` (unless custom configuration)

**Notes**:
- Only change if using non-standard port
- Firewall rules must allow this port

---

#### DB_NAME

**Description**: PostgreSQL database name

**Required**: No

**Default**: Service-specific (e.g., `auth_service`, `book_service`)

**Security Level**: 🟢 Public

**Used By**: auth-service, book-service, reader-service, circulation-service

**Validation**:
- Format: PostgreSQL identifier
- Pattern: `^[a-z][a-z0-9_]*$`
- Length: 1-63 characters

**Values by Environment**:
- **Development**: `gcrf_auth`, `gcrf_book`, `gcrf_reader`
- **Staging**: `gcrf_staging_auth`, `gcrf_staging_book`
- **Production**: `gcrf_prod_auth`, `gcrf_prod_book`

**Service Mapping**:
```
auth-service:        gcrf_auth
book-service:        gcrf_book
reader-service:      gcrf_reader
circulation-service: gcrf_circulation
```

**Notes**:
- Each service should have its own database
- Use consistent naming convention
- Consider schema-per-service in shared database

---

#### DB_USERNAME

**Description**: PostgreSQL database username

**Required**: Yes

**Default**: `postgres` (dev only)

**Security Level**: 🟡 Internal

**Used By**: auth-service, book-service, reader-service, circulation-service

**Validation**:
- Format: PostgreSQL identifier
- Pattern: `^[a-z][a-z0-9_]*$`
- Length: 1-63 characters

**Values by Environment**:
- **Development**: `postgres` (superuser, acceptable for local)
- **Staging**: `gcrf_staging_user`
- **Production**: `gcrf_prod_readonly` (for services) / `gcrf_prod_admin` (for migrations)

**Notes**:
- Never use superuser in production
- Create service-specific users with minimal privileges
- Implement row-level security if needed

---

#### DB_PASSWORD

**Description**: PostgreSQL database password

**Required**: Yes

**Default**: None

**Security Level**: 🔴 Secret

**Used By**: auth-service, book-service, reader-service, circulation-service

**Validation**:
- Length: Minimum 16 characters
- Complexity: Mixed case, numbers, special characters
- No spaces or control characters

**Values by Environment**:
- **Development**: `gcrf_secure_2024` (weak, local only)
- **Staging**: Generate with `openssl rand -base64 24`
- **Production**: Generate with `openssl rand -base64 32`

**Generation**:
```bash
# Generate password without problematic characters
openssl rand -base64 32 | tr -d "=+/@ \n"
```

**Notes**:
- Store in secrets management system
- Rotate every 90 days
- Use different passwords per service
- Implement password rotation strategy

---

### Cache Configuration

#### REDIS_HOST

**Description**: Redis server hostname or IP address

**Required**: No

**Default**: `localhost` (dev) / `redis` (prod/Docker)

**Security Level**: 🟡 Internal

**Used By**: gateway-service, auth-service

**Validation**:
- Format: Hostname, IP address, or Docker service name
- Pattern: Valid hostname or IPv4/IPv6 address

**Values by Environment**:
- **Development**: `localhost` or `127.0.0.1`
- **Docker**: `redis` (service name)
- **Staging**: `redis-staging.internal`
- **Production**: `redis-cluster.prod.internal` or ElastiCache endpoint

**Notes**:
- Consider Redis Sentinel or Cluster for HA
- Use separate Redis instances for cache vs session storage
- Enable persistence for critical data

---

#### REDIS_PORT

**Description**: Redis server port

**Required**: No

**Default**: `6379`

**Security Level**: 🟢 Public

**Used By**: gateway-service, auth-service

**Validation**:
- Format: Integer
- Range: 1-65535
- Standard: 6379 (Redis default)

**Values by Environment**:
- **All Environments**: `6379` (unless custom configuration)

**Notes**:
- Only change if using non-standard port
- Ensure firewall allows this port

---

#### REDIS_PASSWORD

**Description**: Redis server password

**Required**: Yes (production)

**Default**: None

**Security Level**: 🔴 Secret

**Used By**: gateway-service, auth-service

**Validation**:
- Length: Minimum 16 characters
- Format: No spaces or control characters
- Complexity: Mixed case, numbers, special characters recommended

**Values by Environment**:
- **Development**: `gcrf_redis_2024` (weak, local only)
- **Staging**: Generate with `openssl rand -base64 24`
- **Production**: Generate with `openssl rand -base64 32`

**Notes**:
- Always required in production
- Configure Redis with `requirepass`
- Use ACLs for fine-grained access control
- Rotate every 90 days

---

#### REDIS_DATABASE

**Description**: Redis database number (0-15)

**Required**: No

**Default**: `0`

**Security Level**: 🟢 Public

**Used By**: gateway-service, auth-service

**Validation**:
- Format: Integer
- Range: 0-15

**Values by Environment**:
- **Development**: `0` (default)
- **Staging**: `1` (separate from dev)
- **Production**: `0` (dedicated instance)

**Database Allocation**:
```
0: Session storage
1: Rate limiting
2: Cache
3-15: Reserved
```

**Notes**:
- Redis supports 16 databases (0-15)
- Consider separate Redis instances instead of databases
- Database selection not supported in cluster mode

---

### Network & CORS

#### CORS_ALLOWED_ORIGINS

**Description**: Comma-separated list of allowed origins for Cross-Origin Resource Sharing

**Required**: No (but critical for production)

**Default**: `http://localhost:*` (dev) / specific domains (prod)

**Security Level**: 🟡 Internal

**Used By**: gateway-service

**Validation**:
- Format: Comma-separated URLs or patterns
- Pattern: Valid URL with optional wildcard subdomain
- No trailing slashes

**Values by Environment**:
- **Development**: `http://localhost:*,http://127.0.0.1:*,http://192.168.*.*:*`
- **Staging**: `https://staging.gcrf.com,https://staging-admin.gcrf.com`
- **Production**: `https://library.gcrf.com,https://admin.gcrf.com`

**Pattern Examples**:
```
Specific domain:     https://library.gcrf.com
Multiple domains:    https://library.gcrf.com,https://admin.gcrf.com
Subdomain wildcard:  https://*.gcrf.com
Port wildcard:       http://localhost:*
```

**Notes**:
- NEVER use `*` in production
- Be as specific as possible
- Include both www and non-www if needed
- Update when adding new frontend domains

---

### Server Configuration

#### SERVER_PORT

**Description**: Primary HTTP server port for the service

**Required**: No

**Default**: Service-specific (8080 for gateway, 8081 for auth, etc.)

**Security Level**: 🟢 Public

**Used By**: All services

**Validation**:
- Format: Integer
- Range: 1024-65535 (non-privileged ports)
- Reserved: Avoid well-known ports

**Service Port Mapping**:
```
gateway-service:      8080
auth-service:         8081
book-service:         8082
circulation-service:  8083
reader-service:       8084
system-service:       8085
notification-service: 8086
```

**Notes**:
- Consistent port mapping across environments
- Document in service registry
- Configure health checks on these ports

---

#### MANAGEMENT_PORT

**Description**: Actuator management endpoints port (health, metrics)

**Required**: No

**Default**: `8090` (gateway) / service port + 10

**Security Level**: 🟡 Internal

**Used By**: gateway-service, all services with actuator

**Validation**:
- Format: Integer
- Range: 1024-65535
- Should differ from SERVER_PORT

**Values by Environment**:
- **Development**: Same as SERVER_PORT (simplified)
- **Production**: Separate port for security (8090-8099 range)

**Notes**:
- Separate management port improves security
- Only expose internally, not to public internet
- Used by monitoring systems (Prometheus, health checks)

---

### Performance Tuning

#### SERVER_IO_THREADS

**Description**: Number of I/O threads for handling requests (Undertow)

**Required**: No

**Default**: `8`

**Security Level**: 🟢 Public

**Used By**: gateway-service

**Validation**:
- Format: Integer
- Range: 2-32
- Formula: Number of CPU cores

**Values by Environment**:
- **Development**: `4` (laptop/desktop)
- **Staging**: `8` (standard server)
- **Production**: `16` (high-performance server)

**Notes**:
- Set to number of CPU cores
- More threads don't always mean better performance
- Monitor CPU usage to optimize

---

#### SERVER_WORKER_THREADS

**Description**: Number of worker threads for blocking operations

**Required**: No

**Default**: `200`

**Security Level**: 🟢 Public

**Used By**: gateway-service

**Validation**:
- Format: Integer
- Range: 10-1000
- Formula: io-threads * 8

**Values by Environment**:
- **Development**: `50` (low concurrency)
- **Staging**: `100` (moderate load)
- **Production**: `200-500` (based on load)

**Notes**:
- Increase for blocking I/O operations
- Monitor thread pool utilization
- Adjust based on actual workload

---

#### THREAD_POOL_CORE_SIZE

**Description**: Core thread pool size for async operations

**Required**: No

**Default**: `10`

**Security Level**: 🟢 Public

**Used By**: gateway-service, all async-enabled services

**Validation**:
- Format: Integer
- Range: 5-100

**Values by Environment**:
- **Development**: `5`
- **Staging**: `10`
- **Production**: `20-50`

**Notes**:
- Core threads always kept alive
- Start with conservative values
- Increase based on monitoring

---

#### THREAD_POOL_MAX_SIZE

**Description**: Maximum thread pool size

**Required**: No

**Default**: `50`

**Security Level**: 🟢 Public

**Used By**: gateway-service, all async-enabled services

**Validation**:
- Format: Integer
- Range: THREAD_POOL_CORE_SIZE to 500
- Must be >= THREAD_POOL_CORE_SIZE

**Values by Environment**:
- **Development**: `20`
- **Staging**: `50`
- **Production**: `100-200`

**Notes**:
- Additional threads created on demand
- Threads beyond core size have keepalive timeout
- Monitor for thread starvation

---

### Monitoring & Logging

#### LOG_PATH

**Description**: Base directory for log files

**Required**: No

**Default**: `/tmp/gcrf-logs` (dev) / `/var/log/gcrf` (prod)

**Security Level**: 🟢 Public

**Used By**: All services

**Validation**:
- Format: Absolute path
- Permissions: Service must have write access
- Space: Ensure adequate disk space

**Values by Environment**:
- **Development**: `/tmp/gcrf-logs` or `./logs`
- **Docker**: `/var/log/gcrf` (volume mounted)
- **Production**: `/var/log/gcrf` or `/opt/gcrf/logs`

**Directory Structure**:
```
/var/log/gcrf/
├── gateway-service.log
├── auth-service.log
├── book-service.log
└── archived/
    └── gateway-service-2024-01-01.log.gz
```

**Notes**:
- Implement log rotation (size and time based)
- Configure retention policy
- Consider centralized logging (ELK, CloudWatch)

---

## Service-Specific Variables

### Gateway Service Variables

Complete list of environment variables used by the Gateway Service:

| Variable | Required | Default | Security | Description |
|----------|----------|---------|----------|-------------|
| NACOS_SERVER_ADDR | No | `localhost:8848` | 🟡 | Nacos server address |
| NACOS_USERNAME | Yes | - | 🟡 | Nacos username |
| NACOS_PASSWORD | Yes | - | 🔴 | Nacos password |
| NACOS_NAMESPACE | No | `production` | 🟢 | Nacos namespace |
| NACOS_GROUP | No | `GCRF_LIBRARY` | 🟢 | Nacos group |
| JWT_SECRET | Yes | - | 🔴 | JWT signing secret |
| JWT_EXPIRATION | No | `86400000` | 🟢 | Token expiration (ms) |
| JWT_REFRESH_WINDOW | No | `14400000` | 🟢 | Refresh window (ms) |
| JWT_ISSUER | No | `GCRF-Library-System` | 🟢 | Token issuer |
| REDIS_HOST | No | `redis` | 🟡 | Redis hostname |
| REDIS_PORT | No | `6379` | 🟢 | Redis port |
| REDIS_PASSWORD | Yes | - | 🔴 | Redis password |
| REDIS_DATABASE | No | `0` | 🟢 | Redis database |
| CORS_ALLOWED_ORIGINS | No | - | 🟡 | CORS origins |
| SERVER_PORT | No | `8080` | 🟢 | Server port |
| MANAGEMENT_PORT | No | `8090` | 🟡 | Actuator port |
| GATEWAY_CONNECT_TIMEOUT | No | `10000` | 🟢 | Connect timeout (ms) |
| GATEWAY_RESPONSE_TIMEOUT | No | `30s` | 🟢 | Response timeout |
| BOOK_SERVICE_RATE_LIMIT | No | `100` | 🟢 | Book service rate limit |
| READER_SERVICE_RATE_LIMIT | No | `100` | 🟢 | Reader service rate limit |
| LOG_PATH | No | `/var/log/gcrf` | 🟢 | Log directory |

### Auth Service Variables

Complete list of environment variables used by the Auth Service:

| Variable | Required | Default | Security | Description |
|----------|----------|---------|----------|-------------|
| NACOS_SERVER_ADDR | No | `localhost:8848` | 🟡 | Nacos server address |
| NACOS_USERNAME | Yes | - | 🟡 | Nacos username |
| NACOS_PASSWORD | Yes | - | 🔴 | Nacos password |
| NACOS_NAMESPACE | No | `production` | 🟢 | Nacos namespace |
| DB_HOST | No | `localhost` | 🟡 | Database host |
| DB_PORT | No | `5432` | 🟢 | Database port |
| DB_NAME | No | `auth_service` | 🟢 | Database name |
| DB_USERNAME | Yes | - | 🟡 | Database username |
| DB_PASSWORD | Yes | - | 🔴 | Database password |
| REDIS_HOST | No | `localhost` | 🟡 | Redis hostname |
| REDIS_PORT | No | `6379` | 🟢 | Redis port |
| REDIS_PASSWORD | Yes | - | 🔴 | Redis password |
| JWT_SECRET | Yes | - | 🔴 | JWT signing secret |
| JWT_EXPIRATION | No | `86400000` | 🟢 | Access token expiration |
| JWT_REFRESH_EXPIRATION | No | `604800000` | 🟢 | Refresh token expiration |

---

## Cross-Service Variables

These variables must be identical across multiple services:

### JWT_SECRET
- **Services**: gateway-service, auth-service
- **Criticality**: HIGH - Mismatch causes authentication failures
- **Validation**: Must be byte-for-byte identical

### REDIS_* Variables
- **Services**: gateway-service, auth-service
- **Shared Resource**: Redis instance for session/cache
- **Note**: Can use different databases (REDIS_DATABASE)

### NACOS_* Variables
- **Services**: All microservices
- **Purpose**: Service discovery and configuration
- **Note**: NACOS_NAMESPACE can differ for isolation

---

## Environment Templates

### Development Environment (.env.dev)

```bash
# Development Environment - Local Development Only
# DO NOT use these values in production!

# Nacos Configuration
NACOS_SERVER_ADDR=localhost:8848
NACOS_USERNAME=nacos
NACOS_PASSWORD=nacos
NACOS_NAMESPACE=dev
NACOS_GROUP=DEV_GROUP

# JWT Configuration (Insecure - Dev Only!)
JWT_SECRET=DEV_SECRET_ONLY_NOT_FOR_PRODUCTION_USE_MINIMUM_64_CHARACTERS_REQUIRED_HERE
JWT_EXPIRATION=86400000
JWT_ISSUER=GCRF-Library-System-Dev

# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=gcrf_auth
DB_USERNAME=postgres
DB_PASSWORD=gcrf_secure_2024

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=gcrf_redis_2024
REDIS_DATABASE=0

# CORS Configuration (Permissive for Dev)
CORS_ALLOWED_ORIGINS=http://localhost:*,http://127.0.0.1:*

# Logging
LOG_PATH=/tmp/gcrf-logs
```

### Docker Compose Environment

```yaml
# docker-compose.env
version: '3.8'
services:
  gateway:
    environment:
      - NACOS_SERVER_ADDR=nacos:8848
      - REDIS_HOST=redis
      - DB_HOST=postgres
      - JWT_SECRET_FILE=/run/secrets/jwt_secret
      - REDIS_PASSWORD_FILE=/run/secrets/redis_password
```

### Kubernetes ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: gcrf-config
  namespace: production
data:
  NACOS_SERVER_ADDR: "nacos-service.production.svc.cluster.local:8848"
  NACOS_NAMESPACE: "production"
  NACOS_GROUP: "GCRF_LIBRARY"
  DB_HOST: "postgres-service.production.svc.cluster.local"
  DB_PORT: "5432"
  REDIS_HOST: "redis-service.production.svc.cluster.local"
  CORS_ALLOWED_ORIGINS: "https://library.gcrf.com,https://admin.gcrf.com"
  LOG_PATH: "/var/log/gcrf"
```

### Kubernetes Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: gcrf-secrets
  namespace: production
type: Opaque
data:
  JWT_SECRET: <base64-encoded-secret>
  DB_PASSWORD: <base64-encoded-password>
  REDIS_PASSWORD: <base64-encoded-password>
  NACOS_USERNAME: <base64-encoded-username>
  NACOS_PASSWORD: <base64-encoded-password>
```

---

## Security Best Practices

### 1. Secret Generation

```bash
# Generate cryptographically secure secrets
generate_secret() {
    openssl rand -base64 "$1" | tr -d '\n'
}

# JWT Secret (128 characters)
JWT_SECRET=$(generate_secret 96)

# Database Password (32 characters, no special chars)
DB_PASSWORD=$(generate_secret 32 | tr -d "=+/@")

# Redis Password (32 characters)
REDIS_PASSWORD=$(generate_secret 32)
```

### 2. Secret Storage

**Never Store Secrets In**:
- Source code
- Docker images
- Configuration files
- Environment files in repositories

**Recommended Storage**:
1. **HashiCorp Vault**
   ```bash
   vault kv put secret/gcrf/prod \
     jwt_secret="${JWT_SECRET}" \
     db_password="${DB_PASSWORD}"
   ```

2. **AWS Secrets Manager**
   ```bash
   aws secretsmanager create-secret \
     --name gcrf/prod/jwt \
     --secret-string "${JWT_SECRET}"
   ```

3. **Kubernetes Secrets**
   ```bash
   kubectl create secret generic gcrf-secrets \
     --from-literal=jwt_secret="${JWT_SECRET}" \
     --namespace=production
   ```

### 3. Secret Rotation

```bash
#!/bin/bash
# Secret rotation script

# 1. Generate new secret
NEW_JWT_SECRET=$(openssl rand -base64 96)

# 2. Update secondary services first
kubectl set env deployment/auth-service \
  JWT_SECRET_NEW="${NEW_JWT_SECRET}" \
  -n production

# 3. Wait for rollout
kubectl rollout status deployment/auth-service -n production

# 4. Update primary service
kubectl set env deployment/gateway-service \
  JWT_SECRET="${NEW_JWT_SECRET}" \
  -n production

# 5. Remove old secret reference
kubectl set env deployment/auth-service \
  JWT_SECRET="${NEW_JWT_SECRET}" \
  JWT_SECRET_NEW- \
  -n production
```

### 4. Environment Isolation

```bash
# Separate credentials per environment
ENVIRONMENTS="dev staging prod"

for ENV in $ENVIRONMENTS; do
  # Generate unique passwords
  DB_PASS=$(openssl rand -base64 32)
  REDIS_PASS=$(openssl rand -base64 32)
  JWT_SECRET=$(openssl rand -base64 96)

  # Store in vault with environment prefix
  vault kv put "secret/gcrf/${ENV}" \
    db_password="${DB_PASS}" \
    redis_password="${REDIS_PASS}" \
    jwt_secret="${JWT_SECRET}"
done
```

### 5. Access Control

```yaml
# Kubernetes RBAC for secrets
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: gcrf-secret-reader
  namespace: production
rules:
- apiGroups: [""]
  resources: ["secrets"]
  resourceNames: ["gcrf-secrets"]
  verbs: ["get"]
```

---

## Migration Guide

### From Hardcoded Configuration

**Step 1: Identify Hardcoded Values**
```bash
# Search for hardcoded secrets
grep -r "password\|secret\|key" --include="*.yml" --include="*.yaml"
```

**Step 2: Extract to Environment Variables**
```yaml
# Before (application.yml)
jwt:
  secret: hardcoded_secret_value

# After (application.yml)
jwt:
  secret: ${JWT_SECRET}
```

**Step 3: Create Environment File**
```bash
# Create .env.prod.example template
cat > .env.prod.example << EOF
# Security
JWT_SECRET=REPLACE_ME
DB_PASSWORD=REPLACE_ME
REDIS_PASSWORD=REPLACE_ME
EOF
```

**Step 4: Update Deployment Scripts**
```bash
# Docker run with env file
docker run --env-file .env.prod gcrf-gateway:latest

# Docker Compose
docker-compose --env-file .env.prod up
```

### From Different Secret Formats

**From JSON to Environment Variables**:
```bash
# Convert secrets.json to .env format
jq -r 'to_entries[] | "\(.key)=\(.value)"' secrets.json > .env
```

**From YAML to Environment Variables**:
```bash
# Convert secrets.yaml to .env format
yq eval '.[] | to_entries | .[] | .key + "=" + .value' secrets.yaml > .env
```

---

## Troubleshooting

### Common Issues

#### 1. JWT Authentication Failures

**Symptom**: 401 Unauthorized errors
```
ERROR: JWT signature does not match locally computed signature
```

**Diagnosis**:
```bash
# Check JWT secret match
echo "Gateway JWT: ${JWT_SECRET}" | md5sum
echo "Auth JWT: ${JWT_SECRET}" | md5sum
# These should be identical
```

**Solution**:
- Ensure JWT_SECRET is identical in gateway and auth service
- Check for trailing spaces or newlines
- Verify base64 encoding if used

#### 2. Database Connection Refused

**Symptom**: Connection timeout or refused
```
ERROR: Connection to localhost:5432 refused
```

**Diagnosis**:
```bash
# Test connection
PGPASSWORD="${DB_PASSWORD}" psql \
  -h "${DB_HOST}" \
  -p "${DB_PORT}" \
  -U "${DB_USERNAME}" \
  -d "${DB_NAME}" \
  -c "SELECT 1;"
```

**Solution**:
- Verify DB_HOST is correct (use service name in Docker)
- Check PostgreSQL is running
- Verify firewall/security group allows connection
- Check pg_hba.conf for authentication method

#### 3. Redis Authentication Failed

**Symptom**: NOAUTH Authentication required
```
ERROR: NOAUTH Authentication required
```

**Diagnosis**:
```bash
# Test Redis connection
redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" \
  -a "${REDIS_PASSWORD}" ping
```

**Solution**:
- Verify REDIS_PASSWORD matches Redis configuration
- Check Redis requirepass setting
- Ensure password doesn't contain special characters that need escaping

#### 4. CORS Errors in Browser

**Symptom**: Browser console shows CORS policy errors
```
Access to XMLHttpRequest blocked by CORS policy
```

**Diagnosis**:
```bash
# Check current CORS configuration
echo "CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS}"
```

**Solution**:
- Add frontend URL to CORS_ALLOWED_ORIGINS
- Include both http/https variants if needed
- Don't use wildcards in production
- Restart gateway after changes

#### 5. Service Discovery Failures

**Symptom**: No instances available for service
```
ERROR: Load balancer does not have available server for client
```

**Diagnosis**:
```bash
# Check Nacos registration
curl "http://${NACOS_SERVER_ADDR}/nacos/v1/ns/instance/list?serviceName=auth-service"
```

**Solution**:
- Verify NACOS_SERVER_ADDR is accessible
- Check NACOS_USERNAME and NACOS_PASSWORD
- Ensure services are in same NACOS_NAMESPACE
- Verify network connectivity between services

### Debug Commands

```bash
# Print all GCRF environment variables
env | grep -E "^(NACOS|JWT|DB|REDIS|CORS|SERVER|LOG)" | sort

# Validate configuration
./deployment/scripts/validate-config.sh prod

# Test specific service
curl -X GET "http://localhost:8080/actuator/health" \
  -H "Accept: application/json"

# Check service logs
docker logs gcrf-gateway --tail 100

# Monitor environment variables in container
docker exec gcrf-gateway printenv | grep NACOS
```

---

## Appendix

### A. Environment Variable Naming Convention

```
SERVICE_COMPONENT_PROPERTY
```

Examples:
- `NACOS_SERVER_ADDR` - Nacos server address
- `DB_CONNECTION_POOL_SIZE` - Database connection pool size
- `JWT_TOKEN_EXPIRATION` - JWT token expiration

### B. Time Duration Formats

Spring Boot supports multiple duration formats:
- Milliseconds: `30000` (30 seconds)
- Duration string: `30s`, `5m`, `2h`, `1d`
- ISO-8601: `PT30S`, `PT5M`, `PT2H`

### C. Size Formats

Spring Boot supports multiple size formats:
- Bytes: `1048576` (1 MB)
- Size string: `10MB`, `512KB`, `1GB`

### D. Boolean Values

Accepted boolean values:
- True: `true`, `1`, `yes`, `on`
- False: `false`, `0`, `no`, `off`

### E. Special Characters in Values

Escaping special characters:
```bash
# Bash escaping
export PASSWORD="pass\$word"  # Escape $
export PATH_VAR="/path/with\ spaces"  # Escape spaces

# YAML escaping
password: "pass$word"  # Quotes preserve literal
path: "/path/with spaces"  # Quotes for spaces

# Docker Compose escaping
PASSWORD: "pass$$word"  # Double $$ for literal $
```

### F. Precedence Order

Environment variable precedence (highest to lowest):
1. Command line arguments
2. System environment variables
3. .env file variables
4. application-{profile}.yml values
5. application.yml defaults

### G. Validation Regex Patterns

Common validation patterns used:

```regex
# Hostname/IP
^[a-zA-Z0-9.-]+(:[0-9]+)?$

# Strong password
^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{12,}$

# URL
^https?:\/\/([\w.-]+)(:[0-9]+)?(\/.*)?$

# Alphanumeric identifier
^[a-zA-Z][a-zA-Z0-9_-]*$

# Base64
^[A-Za-z0-9+/]*={0,2}$
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-11-01 | Initial documentation |

---

## Contact & Support

**Documentation Owner**: DevOps Team
**Last Review**: 2025-11-01
**Next Review**: 2025-02-01

For questions or updates to this documentation:
1. Create an issue in the repository
2. Contact the DevOps team
3. Submit a pull request with proposed changes

---

**End of Document**