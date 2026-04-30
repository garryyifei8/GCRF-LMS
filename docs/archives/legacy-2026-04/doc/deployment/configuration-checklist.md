# GCRF Library Management System - Configuration Checklist

**Project**: 国创睿峰智能图书馆管理系统 (GCRF Intelligent Library Management System)
**Version**: 1.0.0
**Last Updated**: 2025-12-01
**Document Status**: Production Ready

---

## Table of Contents

1. [Environment Variables](#environment-variables)
2. [Database Configuration](#database-configuration)
3. [Redis Configuration](#redis-configuration)
4. [RabbitMQ Configuration](#rabbitmq-configuration)
5. [Nacos Configuration](#nacos-configuration)
6. [JWT Configuration](#jwt-configuration)
7. [Email Configuration](#email-configuration)
8. [Object Storage Configuration](#object-storage-configuration)
9. [Monitoring Configuration](#monitoring-configuration)
10. [Security Configuration](#security-configuration)

---

## Environment Variables

### Master Environment File (.env)

This file contains all environment variables used across the system. Place this file in the deployment root directory.

**Location**: `/opt/gcrf-library/deployment/.env`

```bash
################################################################################
# GCRF Library Management System - Production Configuration
#
# SECURITY WARNING:
# - This file contains sensitive credentials
# - Never commit this file to version control
# - Set file permissions to 600 (read/write for owner only)
# - Keep secure backups in encrypted storage
################################################################################

# =============================================================================
# General Configuration
# =============================================================================
ENVIRONMENT=production
APP_NAME=GCRF Library Management System
APP_VERSION=1.0.0
TZ=Asia/Shanghai

# =============================================================================
# Database Configuration (PostgreSQL)
# =============================================================================
# Master/Primary Database
DB_HOST=postgres-primary
DB_PORT=5432
DB_USERNAME=gcrf_admin
DB_PASSWORD=<REPLACE_WITH_STRONG_PASSWORD>

# Database names for each service
DB_NAME_AUTH=gcrf_auth
DB_NAME_BOOK=gcrf_book
DB_NAME_READER=gcrf_reader
DB_NAME_CIRCULATION=gcrf_circulation
DB_NAME_SYSTEM=gcrf_system
DB_NAME_NOTIFICATION=gcrf_notification
DB_NAME_ANALYTICS=gcrf_analytics
DB_NAME_RECOMMEND=gcrf_recommend

# Database Pool Configuration
DB_POOL_MIN_SIZE=5
DB_POOL_MAX_SIZE=20
DB_CONNECTION_TIMEOUT=30000
DB_IDLE_TIMEOUT=600000
DB_MAX_LIFETIME=1800000

# =============================================================================
# Redis Configuration
# =============================================================================
REDIS_HOST=redis-master
REDIS_PORT=6379
REDIS_PASSWORD=<REPLACE_WITH_STRONG_PASSWORD>
REDIS_DATABASE=0
REDIS_TIMEOUT=3000
REDIS_MAX_ACTIVE=50
REDIS_MAX_IDLE=10
REDIS_MIN_IDLE=5

# Redis Sentinel (for HA)
REDIS_SENTINEL_ENABLED=false
REDIS_SENTINEL_MASTER=mymaster
REDIS_SENTINEL_NODES=redis-sentinel-1:26379,redis-sentinel-2:26379,redis-sentinel-3:26379

# =============================================================================
# RabbitMQ Configuration
# =============================================================================
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=admin
RABBITMQ_PASSWORD=<REPLACE_WITH_STRONG_PASSWORD>
RABBITMQ_VIRTUAL_HOST=/gcrf
RABBITMQ_CONNECTION_TIMEOUT=60000
RABBITMQ_REQUESTED_HEARTBEAT=60

# RabbitMQ Management UI
RABBITMQ_MANAGEMENT_PORT=15672

# =============================================================================
# Nacos Configuration (Service Discovery & Configuration)
# =============================================================================
NACOS_SERVER_ADDR=nacos:8848
NACOS_NAMESPACE=production
NACOS_USERNAME=nacos
NACOS_PASSWORD=<REPLACE_WITH_STRONG_PASSWORD>
NACOS_GROUP=GCRF_GROUP

# =============================================================================
# MinIO Configuration (Object Storage)
# =============================================================================
MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=<REPLACE_WITH_STRONG_PASSWORD>
MINIO_BUCKET_NAME=gcrf-books
MINIO_BUCKET_AVATARS=gcrf-avatars
MINIO_BUCKET_DOCUMENTS=gcrf-documents
MINIO_REGION=us-east-1

# =============================================================================
# Elasticsearch Configuration
# =============================================================================
ELASTICSEARCH_NODES=elasticsearch:9200
ELASTICSEARCH_USERNAME=elastic
ELASTICSEARCH_PASSWORD=<REPLACE_WITH_STRONG_PASSWORD>
ELASTICSEARCH_INDEX_PREFIX=gcrf

# =============================================================================
# JWT Configuration
# =============================================================================
# Generate with: openssl rand -base64 64
JWT_SECRET=<REPLACE_WITH_GENERATED_SECRET>
JWT_EXPIRATION=86400
JWT_REFRESH_EXPIRATION=604800
JWT_ISSUER=gcrf-library-system
JWT_ALGORITHM=HS512

# For RS256 (recommended for production)
# JWT_ALGORITHM=RS256
# JWT_PUBLIC_KEY_PATH=/opt/gcrf-library/secrets/jwt-public.pem
# JWT_PRIVATE_KEY_PATH=/opt/gcrf-library/secrets/jwt-private.pem

# =============================================================================
# Security Configuration
# =============================================================================
# BCrypt password hashing
BCRYPT_STRENGTH=12

# CORS Configuration
CORS_ALLOWED_ORIGINS=https://library.gcrf.com,https://www.library.gcrf.com
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS
CORS_ALLOWED_HEADERS=Authorization,Content-Type,X-Requested-With
CORS_MAX_AGE=3600

# Rate Limiting
RATE_LIMIT_ENABLED=true
RATE_LIMIT_REQUESTS_PER_MINUTE=100
RATE_LIMIT_BURST=20

# =============================================================================
# Email Configuration (SMTP)
# =============================================================================
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USERNAME=noreply@gcrf-library.com
SMTP_PASSWORD=<REPLACE_WITH_EMAIL_PASSWORD>
SMTP_FROM=noreply@gcrf-library.com
SMTP_FROM_NAME=GCRF Library System
SMTP_SSL_ENABLED=true
SMTP_STARTTLS_ENABLED=true
SMTP_AUTH_ENABLED=true

# =============================================================================
# Logging Configuration
# =============================================================================
LOGGING_LEVEL=INFO
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_GCRF=INFO
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK=WARN
LOGGING_LEVEL_COM_ALIBABA=WARN

# Log file configuration
LOG_PATH=/logs
LOG_FILE_MAX_SIZE=100MB
LOG_FILE_MAX_HISTORY=30
LOG_FILE_TOTAL_SIZE_CAP=10GB

# =============================================================================
# Monitoring Configuration
# =============================================================================
# Prometheus
PROMETHEUS_ENABLED=true
PROMETHEUS_PORT=9090

# Grafana
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=<REPLACE_WITH_STRONG_PASSWORD>
GRAFANA_PORT=3000

# AlertManager
ALERTMANAGER_ENABLED=true
ALERTMANAGER_EMAIL=ops-team@gcrf-library.com

# =============================================================================
# Application-Specific Configuration
# =============================================================================
# File Upload
MAX_FILE_SIZE=10MB
MAX_REQUEST_SIZE=20MB
UPLOAD_PATH=/data/uploads

# Session Configuration
SESSION_TIMEOUT=1800
MAX_CONCURRENT_SESSIONS=5

# Business Rules
DEFAULT_BORROW_DAYS=30
MAX_BORROW_BOOKS=10
FINE_PER_DAY=0.50
OVERDUE_GRACE_PERIOD_DAYS=3

# =============================================================================
# Third-Party Integration (Optional)
# =============================================================================
# SMS Service (Aliyun, Tencent Cloud, etc.)
SMS_ENABLED=false
SMS_ACCESS_KEY=
SMS_SECRET_KEY=
SMS_SIGN_NAME=
SMS_TEMPLATE_CODE=

# Payment Gateway (Optional)
PAYMENT_ENABLED=false
PAYMENT_MERCHANT_ID=
PAYMENT_API_KEY=

# =============================================================================
# Feature Flags
# =============================================================================
FEATURE_FACE_RECOGNITION=false
FEATURE_RFID_INTEGRATION=false
FEATURE_SELF_SERVICE_KIOSK=true
FEATURE_MOBILE_APP=true
FEATURE_AI_RECOMMENDATION=true
FEATURE_ANALYTICS=true

# =============================================================================
# Infrastructure Service Passwords (Infrastructure Compose)
# =============================================================================
# PostgreSQL
POSTGRES_PASSWORD=<REPLACE_WITH_STRONG_PASSWORD>
POSTGRES_REPLICATION_PASSWORD=<REPLACE_WITH_STRONG_PASSWORD>

# Default user for services
POSTGRES_USER=postgres
POSTGRES_DB=postgres

# Redis
REDIS_REQUIREPASS=<REPLACE_WITH_STRONG_PASSWORD>

# RabbitMQ
RABBITMQ_DEFAULT_USER=admin
RABBITMQ_DEFAULT_PASS=<REPLACE_WITH_STRONG_PASSWORD>
RABBITMQ_ERLANG_COOKIE=<GENERATE_RANDOM_STRING>

# Nacos
NACOS_AUTH_ENABLE=true
NACOS_DB_PLATFORM=postgresql
NACOS_DB_URL=jdbc:postgresql://postgres-primary:5432/nacos?characterEncoding=utf8&connectTimeout=10000&socketTimeout=30000
NACOS_DB_USERNAME=nacos_user
NACOS_DB_PASSWORD=<REPLACE_WITH_STRONG_PASSWORD>

# MinIO
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=<REPLACE_WITH_STRONG_PASSWORD>

# Elasticsearch
ELASTIC_PASSWORD=<REPLACE_WITH_STRONG_PASSWORD>
```

### How to Generate Strong Passwords

```bash
# Generate random password (32 characters)
openssl rand -base64 32

# Generate JWT secret (64 characters)
openssl rand -base64 64

# Generate RSA key pair for JWT (RS256)
openssl genrsa -out jwt-private.pem 2048
openssl rsa -in jwt-private.pem -pubout -out jwt-public.pem

# Generate random string for RabbitMQ Erlang cookie
cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1
```

### File Permissions

```bash
# Set proper permissions for .env file
chmod 600 /opt/gcrf-library/deployment/.env
chown root:root /opt/gcrf-library/deployment/.env
```

---

## Database Configuration

### 1. PostgreSQL Configuration

**File**: `/data/postgresql/postgresql.conf`

```ini
# Connection Settings
listen_addresses = '*'
port = 5432
max_connections = 200
superuser_reserved_connections = 3

# Memory Settings
shared_buffers = 2GB                    # 25% of system RAM
effective_cache_size = 6GB              # 75% of system RAM
maintenance_work_mem = 512MB
work_mem = 10MB

# Write-Ahead Logging (WAL)
wal_level = replica
wal_buffers = 16MB
min_wal_size = 1GB
max_wal_size = 4GB
checkpoint_completion_target = 0.9
checkpoint_timeout = 15min

# Replication
max_wal_senders = 10
max_replication_slots = 10
hot_standby = on
hot_standby_feedback = on

# Query Planning
random_page_cost = 1.1                  # For SSD
effective_io_concurrency = 200          # For SSD

# Logging
logging_collector = on
log_directory = 'log'
log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'
log_rotation_age = 1d
log_rotation_size = 100MB
log_line_prefix = '%t [%p]: user=%u,db=%d,app=%a,client=%h '
log_min_duration_statement = 1000       # Log slow queries (>1s)
log_checkpoints = on
log_connections = on
log_disconnections = on
log_lock_waits = on

# Autovacuum
autovacuum = on
autovacuum_max_workers = 3
autovacuum_naptime = 1min

# Security
ssl = on
ssl_cert_file = '/var/lib/postgresql/server.crt'
ssl_key_file = '/var/lib/postgresql/server.key'
password_encryption = scram-sha-256
```

### 2. PostgreSQL Access Control

**File**: `/data/postgresql/pg_hba.conf`

```conf
# TYPE  DATABASE        USER            ADDRESS                 METHOD

# Local connections
local   all             postgres                                peer
local   all             all                                     scram-sha-256

# IPv4 local connections
host    all             all             127.0.0.1/32            scram-sha-256
host    all             all             172.18.0.0/16           scram-sha-256

# Replication connections
host    replication     replicator      172.18.0.0/16           scram-sha-256

# Application connections
host    gcrf_auth       auth_user       172.18.0.0/16           scram-sha-256
host    gcrf_book       book_user       172.18.0.0/16           scram-sha-256
host    gcrf_reader     reader_user     172.18.0.0/16           scram-sha-256
host    gcrf_circulation circulation_user 172.18.0.0/16         scram-sha-256
host    gcrf_system     system_user     172.18.0.0/16           scram-sha-256
host    gcrf_notification notification_user 172.18.0.0/16       scram-sha-256
host    gcrf_analytics  analytics_user  172.18.0.0/16           scram-sha-256
host    gcrf_recommend  recommend_user  172.18.0.0/16           scram-sha-256

# Reject all other connections
host    all             all             all                     reject
```

### 3. Database User Creation

```sql
-- Create service users
CREATE USER auth_user WITH PASSWORD 'strong_password';
CREATE USER book_user WITH PASSWORD 'strong_password';
CREATE USER reader_user WITH PASSWORD 'strong_password';
CREATE USER circulation_user WITH PASSWORD 'strong_password';
CREATE USER system_user WITH PASSWORD 'strong_password';
CREATE USER notification_user WITH PASSWORD 'strong_password';
CREATE USER analytics_user WITH PASSWORD 'strong_password';
CREATE USER recommend_user WITH PASSWORD 'strong_password';

-- Create replication user
CREATE USER replicator WITH REPLICATION PASSWORD 'strong_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE gcrf_auth TO auth_user;
GRANT ALL PRIVILEGES ON DATABASE gcrf_book TO book_user;
GRANT ALL PRIVILEGES ON DATABASE gcrf_reader TO reader_user;
GRANT ALL PRIVILEGES ON DATABASE gcrf_circulation TO circulation_user;
GRANT ALL PRIVILEGES ON DATABASE gcrf_system TO system_user;
GRANT ALL PRIVILEGES ON DATABASE gcrf_notification TO notification_user;
GRANT ALL PRIVILEGES ON DATABASE gcrf_analytics TO analytics_user;
GRANT ALL PRIVILEGES ON DATABASE gcrf_recommend TO recommend_user;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO auth_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO auth_user;
-- Repeat for other users...
```

---

## Redis Configuration

### Redis Configuration File

**File**: `/data/redis/redis.conf`

```conf
# Network
bind 0.0.0.0
port 6379
protected-mode yes
tcp-backlog 511
timeout 0
tcp-keepalive 300

# Security
requirepass your_strong_password_here

# Rename dangerous commands
rename-command CONFIG ""
rename-command FLUSHALL ""
rename-command FLUSHDB ""
rename-command KEYS ""

# General
daemonize no
supervised no
pidfile /var/run/redis_6379.pid
loglevel notice
logfile ""
databases 16

# Snapshotting (RDB)
save 900 1
save 300 10
save 60 10000
stop-writes-on-bgsave-error yes
rdbcompression yes
rdbchecksum yes
dbfilename dump.rdb
dir /data

# Replication
replica-serve-stale-data yes
replica-read-only yes
repl-diskless-sync no
repl-diskless-sync-delay 5
repl-disable-tcp-nodelay no
replica-priority 100

# Memory Management
maxmemory 4gb
maxmemory-policy allkeys-lru
maxmemory-samples 5

# Append Only File (AOF)
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec
no-appendfsync-on-rewrite no
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb

# Slow Log
slowlog-log-slower-than 10000
slowlog-max-len 128

# Latency Monitor
latency-monitor-threshold 100

# Notifications
notify-keyspace-events ""

# Client Output Buffer
client-output-buffer-limit normal 0 0 0
client-output-buffer-limit replica 256mb 64mb 60
client-output-buffer-limit pubsub 32mb 8mb 60

# Performance
hz 10
dynamic-hz yes
aof-rewrite-incremental-fsync yes
```

---

## RabbitMQ Configuration

### RabbitMQ Configuration File

**File**: `/data/rabbitmq/rabbitmq.conf`

```conf
# Network
listeners.tcp.default = 5672
management.tcp.port = 15672

# Virtual Host
default_vhost = /gcrf
default_user = admin
default_pass = your_strong_password_here

# Permissions
default_permissions.configure = .*
default_permissions.read = .*
default_permissions.write = .*

# Memory
vm_memory_high_watermark.relative = 0.6
vm_memory_high_watermark_paging_ratio = 0.75
disk_free_limit.absolute = 2GB

# Clustering
cluster_formation.peer_discovery_backend = classic_config
cluster_formation.classic_config.nodes.1 = rabbit@rabbitmq1

# Message TTL
default_message_ttl = 86400000

# Queue Settings
queue_master_locator = min-masters

# Management
management.load_definitions = /etc/rabbitmq/definitions.json

# TLS/SSL (Optional but recommended)
listeners.ssl.default = 5671
ssl_options.cacertfile = /etc/rabbitmq/ca_certificate.pem
ssl_options.certfile = /etc/rabbitmq/server_certificate.pem
ssl_options.keyfile = /etc/rabbitmq/server_key.pem
ssl_options.verify = verify_peer
ssl_options.fail_if_no_peer_cert = true
```

### RabbitMQ Definitions (Exchanges, Queues)

**File**: `/data/rabbitmq/definitions.json`

```json
{
  "rabbit_version": "3.12.0",
  "users": [
    {
      "name": "admin",
      "password_hash": "...",
      "hashing_algorithm": "rabbit_password_hashing_sha256",
      "tags": "administrator"
    }
  ],
  "vhosts": [
    {
      "name": "/gcrf"
    }
  ],
  "permissions": [
    {
      "user": "admin",
      "vhost": "/gcrf",
      "configure": ".*",
      "write": ".*",
      "read": ".*"
    }
  ],
  "exchanges": [
    {
      "name": "gcrf.notification.exchange",
      "vhost": "/gcrf",
      "type": "topic",
      "durable": true,
      "auto_delete": false
    },
    {
      "name": "gcrf.event.exchange",
      "vhost": "/gcrf",
      "type": "fanout",
      "durable": true,
      "auto_delete": false
    }
  ],
  "queues": [
    {
      "name": "gcrf.notification.email",
      "vhost": "/gcrf",
      "durable": true,
      "auto_delete": false,
      "arguments": {
        "x-message-ttl": 86400000,
        "x-max-length": 10000
      }
    },
    {
      "name": "gcrf.notification.sms",
      "vhost": "/gcrf",
      "durable": true,
      "auto_delete": false
    },
    {
      "name": "gcrf.event.circulation",
      "vhost": "/gcrf",
      "durable": true,
      "auto_delete": false
    }
  ],
  "bindings": [
    {
      "source": "gcrf.notification.exchange",
      "vhost": "/gcrf",
      "destination": "gcrf.notification.email",
      "destination_type": "queue",
      "routing_key": "notification.email.#"
    },
    {
      "source": "gcrf.notification.exchange",
      "vhost": "/gcrf",
      "destination": "gcrf.notification.sms",
      "destination_type": "queue",
      "routing_key": "notification.sms.#"
    }
  ]
}
```

---

## Nacos Configuration

### Nacos Configuration File

**File**: `/data/nacos/conf/application.properties`

```properties
# Server Settings
server.servlet.contextPath=/nacos
server.port=8848
server.tomcat.max-threads=500

# Database Configuration
spring.datasource.platform=postgresql
db.num=1
db.url.0=jdbc:postgresql://postgres-primary:5432/nacos?characterEncoding=utf8&connectTimeout=10000&socketTimeout=30000
db.user.0=nacos_user
db.password.0=your_strong_password_here

# Authentication
nacos.core.auth.enabled=true
nacos.core.auth.system.type=nacos
nacos.core.auth.plugin.nacos.token.secret.key=your_generated_secret_key_here
nacos.core.auth.server.identity.key=serverIdentity
nacos.core.auth.server.identity.value=security

# Clustering
nacos.inetutils.ip-address=172.18.0.10

# Logging
logging.level.root=INFO
logging.file.name=/logs/nacos/naming.log

# Metrics
management.endpoints.web.exposure.include=*
management.metrics.export.elastic.enabled=false
management.metrics.export.influx.enabled=false

# Feature Flags
nacos.naming.distro.taskDispatchThreadCount=10
nacos.naming.distro.batchSyncKeyCount=1000
nacos.naming.distro.syncRetryDelay=5000
nacos.naming.data.warmup=true
```

### Nacos Service Configuration (via Nacos Console)

Login to Nacos Console (http://nacos:8848/nacos) and configure:

**1. Gateway Service Configuration**
- Data ID: `gateway-service.yml`
- Group: `GCRF_GROUP`
- Configuration Format: YAML

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - StripPrefix=2

        - id: book-service
          uri: lb://book-service
          predicates:
            - Path=/api/v1/books/**
          filters:
            - StripPrefix=2

        # Add other routes...
```

**2. Common Configuration (Shared)**
- Data ID: `common.yml`
- Group: `GCRF_GROUP`

```yaml
# Shared configuration for all services
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  redis:
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 50
        max-idle: 10
        min-idle: 5

# Logging
logging:
  level:
    root: INFO
    com.gcrf: INFO
```

---

## JWT Configuration

### Option 1: HS256 (Symmetric Key)

```bash
# Generate secret
JWT_SECRET=$(openssl rand -base64 64)
echo "JWT_SECRET=$JWT_SECRET" >> .env
```

### Option 2: RS256 (Asymmetric Key Pair - Recommended)

```bash
# Generate private key
openssl genrsa -out /opt/gcrf-library/secrets/jwt-private.pem 2048

# Generate public key
openssl rsa -in /opt/gcrf-library/secrets/jwt-private.pem \
  -pubout -out /opt/gcrf-library/secrets/jwt-public.pem

# Set permissions
chmod 600 /opt/gcrf-library/secrets/jwt-private.pem
chmod 644 /opt/gcrf-library/secrets/jwt-public.pem

# Add to .env
echo "JWT_ALGORITHM=RS256" >> .env
echo "JWT_PRIVATE_KEY_PATH=/opt/gcrf-library/secrets/jwt-private.pem" >> .env
echo "JWT_PUBLIC_KEY_PATH=/opt/gcrf-library/secrets/jwt-public.pem" >> .env
```

---

## Email Configuration

### SMTP Configuration Examples

**Gmail**:
```bash
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-specific-password
SMTP_SSL_ENABLED=false
SMTP_STARTTLS_ENABLED=true
```

**Outlook/Office 365**:
```bash
SMTP_HOST=smtp.office365.com
SMTP_PORT=587
SMTP_USERNAME=your-email@outlook.com
SMTP_PASSWORD=your-password
SMTP_SSL_ENABLED=false
SMTP_STARTTLS_ENABLED=true
```

**Aliyun DirectMail**:
```bash
SMTP_HOST=smtpdm.aliyun.com
SMTP_PORT=465
SMTP_USERNAME=your-username@your-domain.com
SMTP_PASSWORD=your-password
SMTP_SSL_ENABLED=true
SMTP_STARTTLS_ENABLED=false
```

---

## Object Storage Configuration

### MinIO Configuration

**File**: `/data/minio/config.env`

```bash
# MinIO Root Credentials
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=your_strong_password_here

# Region
MINIO_REGION=us-east-1

# Browser
MINIO_BROWSER=on

# Server URL (for console)
MINIO_SERVER_URL=http://minio:9000
MINIO_BROWSER_REDIRECT_URL=http://localhost:9001

# Compression
MINIO_COMPRESS=on
MINIO_COMPRESS_EXTENSIONS=".txt,.log,.csv,.json,.tar,.xml,.bin"
MINIO_COMPRESS_MIME_TYPES="text/*,application/json,application/xml"
```

### Create Buckets and Policies

```bash
# Set MinIO alias
mc alias set local http://localhost:9000 minioadmin your_strong_password

# Create buckets
mc mb local/gcrf-books
mc mb local/gcrf-avatars
mc mb local/gcrf-documents

# Set bucket policies (public read for avatars)
mc anonymous set download local/gcrf-avatars

# Set bucket lifecycle (delete old files after 365 days)
mc ilm add local/gcrf-books --expiry-days 365

# Enable versioning
mc version enable local/gcrf-books
```

---

## Monitoring Configuration

### Prometheus Configuration

**File**: `/opt/gcrf-library/deployment/prometheus/prometheus.yml`

See `installation-steps.md` for complete Prometheus configuration.

### Grafana Data Sources

1. **Prometheus**:
   - URL: http://prometheus:9090
   - Access: Server (default)
   - Scrape interval: 15s

2. **Loki**:
   - URL: http://loki:3100
   - Access: Server (default)

3. **PostgreSQL** (optional, for direct queries):
   - Host: postgres-primary:5432
   - Database: gcrf_analytics
   - User: analytics_user
   - SSL Mode: disable

---

## Security Configuration

### 1. Firewall Rules

```bash
# Ubuntu (UFW)
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable

# CentOS (firewalld)
sudo firewall-cmd --permanent --set-default-zone=drop
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

### 2. Docker Daemon Security

```bash
# Enable Docker Content Trust
export DOCKER_CONTENT_TRUST=1

# Restrict Docker socket permissions
sudo chmod 660 /var/run/docker.sock
sudo chown root:docker /var/run/docker.sock
```

### 3. Secrets Management Checklist

- [ ] All passwords are strong (16+ characters, mixed case, numbers, symbols)
- [ ] JWT secrets are cryptographically random (64+ bytes)
- [ ] Private keys have 600 permissions
- [ ] .env file has 600 permissions
- [ ] Secrets are not committed to version control
- [ ] Backup of secrets stored securely (encrypted)
- [ ] Regular secret rotation scheduled (every 90 days)

---

## Configuration Verification Checklist

### Pre-Deployment Checklist

- [ ] All environment variables set in .env file
- [ ] All passwords generated and recorded securely
- [ ] JWT keys generated (RS256 key pair)
- [ ] Database users created with proper privileges
- [ ] Redis password set and tested
- [ ] RabbitMQ users and vhosts configured
- [ ] Nacos authentication enabled
- [ ] MinIO buckets created with proper policies
- [ ] SMTP credentials tested
- [ ] SSL certificates obtained and installed
- [ ] Firewall rules configured
- [ ] File permissions set correctly (600 for secrets)
- [ ] Backup directories created
- [ ] Log rotation configured

### Post-Deployment Verification

- [ ] All services start without errors
- [ ] Services register with Nacos successfully
- [ ] Database connections work from all services
- [ ] Redis cache operations work
- [ ] RabbitMQ message publishing/consuming works
- [ ] File upload to MinIO works
- [ ] Email sending works (test email)
- [ ] JWT authentication works
- [ ] API Gateway routes traffic correctly
- [ ] Monitoring dashboards display data
- [ ] Logs are being collected
- [ ] Backups are running on schedule
- [ ] Health checks pass for all services
- [ ] SSL certificate is valid and trusted

---

## Configuration Management Best Practices

### 1. Version Control

```bash
# Track configuration templates (without secrets)
git add deployment/.env.example
git add deployment/docker-compose.*.yml
git add deployment/nginx/*.conf.template

# Never commit secrets
echo ".env" >> .gitignore
echo "secrets/" >> .gitignore
```

### 2. Secret Rotation Schedule

| Secret Type | Rotation Frequency | Method |
|-------------|-------------------|---------|
| Database Passwords | Every 90 days | Manual, coordinated downtime |
| JWT Secrets | Every 180 days | Rolling update, dual key period |
| API Keys | Every 90 days | Update and restart affected services |
| SSL Certificates | Before expiration | Automated with Certbot |
| Service Passwords | Every 90 days | Update via Nacos config |

### 3. Configuration Backup

```bash
# Backup script
#!/bin/bash
BACKUP_DIR="/backups/config/$(date +%Y%m%d)"
mkdir -p "$BACKUP_DIR"

# Backup environment file (encrypted)
openssl enc -aes-256-cbc -salt -in /opt/gcrf-library/deployment/.env \
  -out "$BACKUP_DIR/env.enc" -k "your_encryption_password"

# Backup Docker configs
cp -r /opt/gcrf-library/deployment/*.yml "$BACKUP_DIR/"

# Backup Nginx configs
cp -r /etc/nginx/sites-available "$BACKUP_DIR/nginx/"

# Backup database config
docker exec postgres-primary pg_dumpall -U postgres -c > "$BACKUP_DIR/db_config.sql"
```

---

## Summary

This configuration checklist provides a comprehensive guide for configuring all components of the GCRF Library Management System. Ensure all items are configured and verified before going to production.

**Critical Security Reminders**:
1. Never use default passwords in production
2. Enable SSL/TLS for all external-facing services
3. Rotate secrets regularly (90-day schedule recommended)
4. Keep encrypted backups of all configuration
5. Restrict file permissions (600 for secrets, 644 for configs)

For operational procedures and troubleshooting, refer to:
- Operations Manual: `operations-manual.md`
- Troubleshooting Guide: `troubleshooting-guide.md`

---

**Document Version**: 1.0.0
**Last Updated**: 2025-12-01
**Next Review Date**: 2026-03-01
