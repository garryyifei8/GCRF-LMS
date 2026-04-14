# GCRF Library Management System - Installation Steps

**Project**: 国创睿峰智能图书馆管理系统 (GCRF Intelligent Library Management System)
**Version**: 1.0.0
**Last Updated**: 2025-12-01
**Document Status**: Production Ready

---

## Table of Contents

1. [Pre-Installation Checklist](#pre-installation-checklist)
2. [System Preparation](#system-preparation)
3. [Docker and Docker Compose Installation](#docker-and-docker-compose-installation)
4. [Infrastructure Services Deployment](#infrastructure-services-deployment)
5. [Application Services Deployment](#application-services-deployment)
6. [Monitoring Services Deployment](#monitoring-services-deployment)
7. [Nginx Reverse Proxy Configuration](#nginx-reverse-proxy-configuration)
8. [SSL/TLS Certificate Setup](#ssltls-certificate-setup)
9. [Post-Installation Verification](#post-installation-verification)
10. [Initial System Configuration](#initial-system-configuration)

---

## Pre-Installation Checklist

### 1. Hardware Verification

```bash
# Check CPU cores
nproc

# Check available memory (should be at least 32GB for production)
free -h

# Check disk space
df -h

# Check disk I/O performance
sudo hdparm -tT /dev/sda

# Check network connectivity
ping -c 4 8.8.8.8
```

### 2. Software Prerequisites

- [ ] Ubuntu 22.04 LTS or CentOS Stream 9 installed
- [ ] Root or sudo access available
- [ ] Static IP address configured
- [ ] DNS resolution working
- [ ] Firewall configured (iptables or firewalld)
- [ ] NTP synchronized
- [ ] SELinux configured (CentOS/RHEL) or disabled

### 3. Network Prerequisites

- [ ] Ports 80, 443 open for external access
- [ ] Internal network connectivity between servers
- [ ] DNS records configured (if using domain names)
- [ ] SSL certificates obtained (for HTTPS)

### 4. Required Files

- [ ] Application Docker images or source code
- [ ] `.env` file with production credentials
- [ ] SSL certificate files (if using HTTPS)
- [ ] Database initialization scripts
- [ ] Configuration files

---

## System Preparation

### 1. Update System

**Ubuntu 22.04**:
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y curl wget git vim net-tools
```

**CentOS Stream 9**:
```bash
sudo dnf update -y
sudo dnf install -y curl wget git vim net-tools
```

### 2. Configure System Limits

Edit `/etc/security/limits.conf`:
```bash
sudo tee -a /etc/security/limits.conf <<EOF
* soft nofile 65536
* hard nofile 65536
* soft nproc 32768
* hard nproc 32768
EOF
```

Edit `/etc/sysctl.conf`:
```bash
sudo tee -a /etc/sysctl.conf <<EOF
# Network performance tuning
net.core.somaxconn = 32768
net.ipv4.tcp_max_syn_backlog = 8192
net.ipv4.ip_local_port_range = 10000 65535

# File system
fs.file-max = 2097152

# Virtual memory
vm.swappiness = 10
vm.max_map_count = 262144

# Docker optimization
net.ipv4.ip_forward = 1
net.bridge.bridge-nf-call-iptables = 1
net.bridge.bridge-nf-call-ip6tables = 1
EOF

sudo sysctl -p
```

### 3. Disable Swap (Optional but recommended for databases)

```bash
sudo swapoff -a
sudo sed -i '/swap/d' /etc/fstab
```

### 4. Configure Time Synchronization

**Ubuntu**:
```bash
sudo apt install -y chrony
sudo systemctl enable chrony
sudo systemctl start chrony
```

**CentOS**:
```bash
sudo dnf install -y chrony
sudo systemctl enable chronyd
sudo systemctl start chronyd
```

Verify time sync:
```bash
timedatectl status
```

### 5. Configure Firewall

**Ubuntu (UFW)**:
```bash
# Enable UFW
sudo ufw enable

# Allow SSH (IMPORTANT: Do this first!)
sudo ufw allow 22/tcp

# Allow HTTP and HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Check status
sudo ufw status
```

**CentOS (firewalld)**:
```bash
# Enable firewalld
sudo systemctl enable firewalld
sudo systemctl start firewalld

# Allow HTTP and HTTPS
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload

# Check status
sudo firewall-cmd --list-all
```

### 6. Create Application Directory Structure

```bash
# Create base directories
sudo mkdir -p /opt/gcrf-library
sudo mkdir -p /data/postgresql
sudo mkdir -p /data/redis
sudo mkdir -p /data/minio
sudo mkdir -p /data/elasticsearch
sudo mkdir -p /data/rabbitmq
sudo mkdir -p /data/nacos
sudo mkdir -p /logs/nginx
sudo mkdir -p /logs/services
sudo mkdir -p /backups/postgresql
sudo mkdir -p /backups/minio

# Set permissions
sudo chown -R $(whoami):$(whoami) /opt/gcrf-library
sudo chown -R 999:999 /data/postgresql  # PostgreSQL user
sudo chown -R 1000:1000 /data/minio      # MinIO user
sudo chmod -R 755 /data
sudo chmod -R 755 /logs
sudo chmod -R 700 /backups
```

---

## Docker and Docker Compose Installation

### 1. Install Docker Engine

**Ubuntu 22.04**:
```bash
# Remove old versions
sudo apt remove -y docker docker-engine docker.io containerd runc

# Install dependencies
sudo apt update
sudo apt install -y ca-certificates curl gnupg lsb-release

# Add Docker's official GPG key
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
  sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Set up repository
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Verify installation
docker --version
```

**CentOS Stream 9**:
```bash
# Remove old versions
sudo dnf remove -y docker docker-client docker-client-latest \
  docker-common docker-latest docker-latest-logrotate \
  docker-logrotate docker-engine

# Install dependencies
sudo dnf install -y dnf-plugins-core

# Add Docker repository
sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

# Install Docker
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Verify installation
docker --version
```

### 2. Configure Docker

Create Docker daemon configuration:
```bash
sudo mkdir -p /etc/docker

sudo tee /etc/docker/daemon.json <<EOF
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "storage-driver": "overlay2",
  "data-root": "/var/lib/docker",
  "registry-mirrors": [],
  "insecure-registries": [],
  "live-restore": true,
  "userland-proxy": false,
  "default-ulimits": {
    "nofile": {
      "Name": "nofile",
      "Hard": 65536,
      "Soft": 65536
    }
  }
}
EOF
```

### 3. Start and Enable Docker

```bash
# Start Docker service
sudo systemctl start docker

# Enable Docker to start on boot
sudo systemctl enable docker

# Add current user to docker group (optional, for non-root usage)
sudo usermod -aG docker $(whoami)

# Note: Log out and back in for group changes to take effect
# or run: newgrp docker

# Verify Docker is running
sudo systemctl status docker
docker info
```

### 4. Install Docker Compose (Standalone)

```bash
# Download latest Docker Compose
DOCKER_COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | grep -Po '"tag_name": "\K.*?(?=")')
sudo curl -L "https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose

# Make executable
sudo chmod +x /usr/local/bin/docker-compose

# Verify installation
docker-compose --version
```

### 5. Configure Docker Networks

```bash
# Create custom networks
docker network create --driver bridge gcrf-infrastructure-network \
  --subnet=172.18.0.0/16 \
  --gateway=172.18.0.1

docker network create --driver bridge gcrf-frontend-network \
  --subnet=172.29.0.0/16 \
  --gateway=172.29.0.1

# Verify networks
docker network ls
```

---

## Infrastructure Services Deployment

### 1. Prepare Environment Configuration

```bash
cd /opt/gcrf-library

# Copy environment template
cp .env.infrastructure.example .env

# Edit environment file with production values
vim .env
```

**Required Environment Variables** (see `configuration-checklist.md` for complete list):
```bash
# Database
DB_USERNAME=gcrf_admin
DB_PASSWORD=<strong-password>

# Redis
REDIS_PASSWORD=<strong-password>

# RabbitMQ
RABBITMQ_DEFAULT_USER=admin
RABBITMQ_DEFAULT_PASS=<strong-password>

# Nacos
NACOS_USERNAME=nacos
NACOS_PASSWORD=<strong-password>

# MinIO
MINIO_ROOT_USER=admin
MINIO_ROOT_PASSWORD=<strong-password>

# JWT
JWT_SECRET=<generate-with-openssl>
```

### 2. Generate Secrets

```bash
# Generate strong passwords
openssl rand -base64 32

# Generate JWT secret (RS256 key pair)
openssl genrsa -out jwt-private.pem 2048
openssl rsa -in jwt-private.pem -pubout -out jwt-public.pem

# Store keys securely
sudo mkdir -p /opt/gcrf-library/secrets
sudo mv jwt-private.pem /opt/gcrf-library/secrets/
sudo mv jwt-public.pem /opt/gcrf-library/secrets/
sudo chmod 600 /opt/gcrf-library/secrets/jwt-private.pem
```

### 3. Deploy Infrastructure Services

**Start PostgreSQL**:
```bash
cd /opt/gcrf-library/deployment

# Initialize PostgreSQL
docker-compose -f docker-compose.infrastructure.yml up -d postgres-primary

# Wait for PostgreSQL to be ready
docker-compose -f docker-compose.infrastructure.yml logs -f postgres-primary

# Verify PostgreSQL is running
docker exec -it postgres-primary psql -U postgres -c "SELECT version();"
```

**Initialize Databases**:
```bash
# Run database initialization scripts
docker exec -i postgres-primary psql -U postgres <<EOF
-- Create databases for each service
CREATE DATABASE gcrf_auth;
CREATE DATABASE gcrf_book;
CREATE DATABASE gcrf_reader;
CREATE DATABASE gcrf_circulation;
CREATE DATABASE gcrf_system;
CREATE DATABASE gcrf_notification;
CREATE DATABASE gcrf_analytics;
CREATE DATABASE gcrf_recommend;

-- Create service users
CREATE USER auth_user WITH PASSWORD '${DB_PASSWORD}';
CREATE USER book_user WITH PASSWORD '${DB_PASSWORD}';
CREATE USER reader_user WITH PASSWORD '${DB_PASSWORD}';
CREATE USER circulation_user WITH PASSWORD '${DB_PASSWORD}';
CREATE USER system_user WITH PASSWORD '${DB_PASSWORD}';
CREATE USER notification_user WITH PASSWORD '${DB_PASSWORD}';
CREATE USER analytics_user WITH PASSWORD '${DB_PASSWORD}';
CREATE USER recommend_user WITH PASSWORD '${DB_PASSWORD}';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE gcrf_auth TO auth_user;
GRANT ALL PRIVILEGES ON DATABASE gcrf_book TO book_user;
GRANT ALL PRIVILEGES ON DATABASE gcrf_reader TO reader_user;
GRANT ALL PRIVILEGES ON DATABASE gcrf_circulation TO circulation_user;
GRANT ALL PRIVILEGES ON DATABASE gcrf_system TO system_user;
GRANT ALL PRIVILEGES ON DATABASE gcrf_notification TO notification_user;
GRANT ALL PRIVILEGES ON DATABASE gcrf_analytics TO analytics_user;
GRANT ALL PRIVILEGES ON DATABASE gcrf_recommend TO recommend_user;
EOF

# Run schema migration scripts
docker exec -i postgres-primary psql -U postgres -d gcrf_book < /opt/gcrf-library/backend/book-service/src/main/resources/db/schema.sql
docker exec -i postgres-primary psql -U postgres -d gcrf_reader < /opt/gcrf-library/backend/reader-service/src/main/resources/db/schema.sql
# Repeat for other services...
```

**Start Redis**:
```bash
docker-compose -f docker-compose.infrastructure.yml up -d redis-master

# Verify Redis is running
docker exec redis-master redis-cli -a ${REDIS_PASSWORD} ping
```

**Start RabbitMQ**:
```bash
docker-compose -f docker-compose.infrastructure.yml up -d rabbitmq

# Wait for RabbitMQ to be ready (30-60 seconds)
docker-compose -f docker-compose.infrastructure.yml logs -f rabbitmq

# Verify RabbitMQ is running
curl -u admin:${RABBITMQ_DEFAULT_PASS} http://localhost:15672/api/overview
```

**Start Nacos**:
```bash
docker-compose -f docker-compose.infrastructure.yml up -d nacos

# Wait for Nacos to be ready (30-60 seconds)
docker-compose -f docker-compose.infrastructure.yml logs -f nacos

# Access Nacos console: http://localhost:8848/nacos
# Default credentials: nacos/nacos (change in production!)
```

**Start MinIO**:
```bash
docker-compose -f docker-compose.infrastructure.yml up -d minio

# Verify MinIO is running
docker exec minio mc alias set local http://localhost:9000 ${MINIO_ROOT_USER} ${MINIO_ROOT_PASSWORD}

# Create buckets
docker exec minio mc mb local/gcrf-books
docker exec minio mc mb local/gcrf-avatars
docker exec minio mc mb local/gcrf-documents
```

**Start Elasticsearch**:
```bash
docker-compose -f docker-compose.infrastructure.yml up -d elasticsearch

# Wait for Elasticsearch to be ready
curl http://localhost:9200/_cluster/health
```

### 4. Verify Infrastructure Services

```bash
# Check all services are running
docker-compose -f docker-compose.infrastructure.yml ps

# Expected output: All services should show "Up" status
```

**Health Check Script**:
```bash
#!/bin/bash
# /usr/local/bin/check-infrastructure.sh

echo "Checking Infrastructure Services..."

# PostgreSQL
docker exec postgres-primary pg_isready -U postgres && echo "✓ PostgreSQL: OK" || echo "✗ PostgreSQL: FAILED"

# Redis
docker exec redis-master redis-cli -a ${REDIS_PASSWORD} ping > /dev/null 2>&1 && echo "✓ Redis: OK" || echo "✗ Redis: FAILED"

# RabbitMQ
curl -sf -u admin:${RABBITMQ_DEFAULT_PASS} http://localhost:15672/api/healthchecks/node > /dev/null && echo "✓ RabbitMQ: OK" || echo "✗ RabbitMQ: FAILED"

# Nacos
curl -sf http://localhost:8848/nacos/v1/console/health/readiness > /dev/null && echo "✓ Nacos: OK" || echo "✗ Nacos: FAILED"

# MinIO
curl -sf http://localhost:9000/minio/health/live > /dev/null && echo "✓ MinIO: OK" || echo "✗ MinIO: FAILED"

# Elasticsearch
curl -sf http://localhost:9200/_cluster/health > /dev/null && echo "✓ Elasticsearch: OK" || echo "✗ Elasticsearch: FAILED"
```

---

## Application Services Deployment

### 1. Build Application Images

**Option A: Build from Source**:
```bash
cd /opt/gcrf-library

# Set Java 21 environment
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Build all services
cd backend
mvn clean package -DskipTests

# Build Docker images
docker build -t gcrf-library/gateway-service:latest gateway-service/
docker build -t gcrf-library/auth-service:latest auth-service/
docker build -t gcrf-library/book-service:latest book-service/
docker build -t gcrf-library/reader-service:latest reader-service/
docker build -t gcrf-library/circulation-service:latest circulation-service/
docker build -t gcrf-library/system-service:latest system-service/
docker build -t gcrf-library/notification-service:latest notification-service/
docker build -t gcrf-library/analytics-service:latest analytics-service/
docker build -t gcrf-library/recommend-service:latest recommend-service/

# Build frontend
cd ../web-admin
npm install
npm run build
docker build --platform linux/amd64 -t gcrf-library/web-admin:latest .
```

**Option B: Load Pre-built Images**:
```bash
# Load images from tar files
docker load < gcrf-gateway-service.tar.gz
docker load < gcrf-auth-service.tar.gz
docker load < gcrf-book-service.tar.gz
# ... load other images

# Verify images
docker images | grep gcrf-library
```

### 2. Deploy Application Services

```bash
cd /opt/gcrf-library/deployment

# Start application services
docker-compose -f docker-compose.services.yml up -d

# Monitor startup logs
docker-compose -f docker-compose.services.yml logs -f
```

### 3. Verify Application Services

```bash
# Check all services are registered in Nacos
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=gateway-service

# Check Gateway health
curl http://localhost:8080/actuator/health

# Check Auth service health
curl http://localhost:8081/actuator/health

# Check Book service health
curl http://localhost:8082/actuator/health
```

**Service Startup Order**:
1. Gateway Service (wait 30s)
2. Auth Service (wait 30s)
3. Book Service, Reader Service, Circulation Service (parallel, wait 30s)
4. System Service, Notification Service (parallel, wait 30s)
5. Analytics Service, Recommend Service (parallel, wait 30s)

**Startup Verification Script**:
```bash
#!/bin/bash
# /usr/local/bin/check-services.sh

SERVICES=("gateway:8080" "auth:8081" "book:8082" "reader:8084" "circulation:8083" "system:8085" "notification:8086")

for service in "${SERVICES[@]}"; do
    name="${service%%:*}"
    port="${service##*:}"

    if curl -sf http://localhost:${port}/actuator/health > /dev/null; then
        echo "✓ ${name} service: OK"
    else
        echo "✗ ${name} service: FAILED"
    fi
done
```

---

## Monitoring Services Deployment

### 1. Deploy Prometheus

```bash
cd /opt/gcrf-library/deployment

# Create Prometheus configuration
mkdir -p /opt/gcrf-library/deployment/prometheus

cat > /opt/gcrf-library/deployment/prometheus/prometheus.yml <<EOF
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  # Gateway Service
  - job_name: 'gateway-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['gateway-service:8080']

  # Auth Service
  - job_name: 'auth-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['auth-service:8081']

  # Book Service
  - job_name: 'book-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['book-service:8082']

  # Add other services...

  # PostgreSQL Exporter
  - job_name: 'postgresql'
    static_configs:
      - targets: ['postgres-exporter:9187']

  # Redis Exporter
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
EOF

# Start Prometheus
docker-compose -f docker-compose.monitoring.yml up -d prometheus

# Access Prometheus UI: http://localhost:9090
```

### 2. Deploy Grafana

```bash
# Start Grafana
docker-compose -f docker-compose.monitoring.yml up -d grafana

# Access Grafana: http://localhost:3000
# Default credentials: admin/admin (change on first login)
```

**Add Prometheus Data Source**:
1. Login to Grafana (http://localhost:3000)
2. Go to Configuration > Data Sources
3. Add Prometheus data source
4. URL: http://prometheus:9090
5. Save & Test

**Import Dashboards**:
```bash
# Import pre-configured dashboards
# Dashboard IDs:
# - Spring Boot 2.1 Statistics: 10280
# - PostgreSQL Database: 9628
# - Redis Dashboard: 11835
# - RabbitMQ Overview: 10991
```

### 3. Deploy Loki and Promtail (Log Aggregation)

```bash
# Create Loki configuration
mkdir -p /opt/gcrf-library/deployment/loki

cat > /opt/gcrf-library/deployment/loki/loki-config.yml <<EOF
auth_enabled: false

server:
  http_listen_port: 3100

ingester:
  lifecycler:
    address: 127.0.0.1
    ring:
      kvstore:
        store: inmemory
      replication_factor: 1
  chunk_idle_period: 5m
  chunk_retain_period: 30s

schema_config:
  configs:
    - from: 2020-05-15
      store: boltdb
      object_store: filesystem
      schema: v11
      index:
        prefix: index_
        period: 168h

storage_config:
  boltdb:
    directory: /loki/index
  filesystem:
    directory: /loki/chunks

limits_config:
  enforce_metric_name: false
  reject_old_samples: true
  reject_old_samples_max_age: 168h
EOF

# Start Loki
docker-compose -f docker-compose.observability.yml up -d loki

# Add Loki data source in Grafana
# URL: http://loki:3100
```

### 4. Deploy AlertManager

```bash
# Create AlertManager configuration
mkdir -p /opt/gcrf-library/deployment/alertmanager

cat > /opt/gcrf-library/deployment/alertmanager/alertmanager.yml <<EOF
global:
  resolve_timeout: 5m
  smtp_smarthost: 'smtp.example.com:587'
  smtp_from: 'alertmanager@gcrf-library.com'
  smtp_auth_username: 'alerts@gcrf-library.com'
  smtp_auth_password: 'password'

route:
  receiver: 'email-notifications'
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h

receivers:
  - name: 'email-notifications'
    email_configs:
      - to: 'ops-team@gcrf-library.com'
        headers:
          Subject: '[GCRF Alert] {{ .GroupLabels.alertname }}'
EOF

# Start AlertManager
docker-compose -f docker-compose.monitoring.yml up -d alertmanager

# Access AlertManager: http://localhost:9093
```

---

## Nginx Reverse Proxy Configuration

### 1. Install Nginx (Host Installation)

**Ubuntu**:
```bash
sudo apt install -y nginx
```

**CentOS**:
```bash
sudo dnf install -y nginx
```

### 2. Configure Nginx as Reverse Proxy

```bash
# Create Nginx configuration
sudo tee /etc/nginx/sites-available/gcrf-library <<EOF
# Upstream for Gateway Service
upstream gateway_backend {
    least_conn;
    server localhost:8080 max_fails=3 fail_timeout=30s;
    # Add more gateway instances for HA:
    # server localhost:8081 max_fails=3 fail_timeout=30s;
    # server localhost:8082 max_fails=3 fail_timeout=30s;
}

# Upstream for Web Admin
upstream web_admin {
    server localhost:3011;
}

# HTTP Server - Redirect to HTTPS
server {
    listen 80;
    server_name library.gcrf.com www.library.gcrf.com;

    # Allow Let's Encrypt validation
    location ^~ /.well-known/acme-challenge/ {
        root /var/www/html;
    }

    # Redirect all other traffic to HTTPS
    location / {
        return 301 https://\$server_name\$request_uri;
    }
}

# HTTPS Server
server {
    listen 443 ssl http2;
    server_name library.gcrf.com www.library.gcrf.com;

    # SSL Configuration
    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers 'ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305';
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # Security Headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Logging
    access_log /var/log/nginx/gcrf-library-access.log;
    error_log /var/log/nginx/gcrf-library-error.log;

    # API Gateway Proxy
    location /api/ {
        proxy_pass http://gateway_backend/;
        proxy_http_version 1.1;

        # Headers
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Forwarded-Host \$host;
        proxy_set_header X-Forwarded-Port \$server_port;

        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;

        # Buffering
        proxy_buffering on;
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;
        proxy_busy_buffers_size 8k;

        # WebSocket support
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # Web Admin Frontend
    location / {
        proxy_pass http://web_admin;
        proxy_http_version 1.1;

        # Headers
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;

        # Caching for static assets
        location ~* \.(jpg|jpeg|png|gif|ico|css|js|svg|woff|woff2|ttf|eot)$ {
            proxy_pass http://web_admin;
            expires 30d;
            add_header Cache-Control "public, immutable";
        }
    }

    # Health check endpoint
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
}
EOF

# Enable site
sudo ln -sf /etc/nginx/sites-available/gcrf-library /etc/nginx/sites-enabled/

# Test configuration
sudo nginx -t

# Reload Nginx
sudo systemctl reload nginx
```

### 3. Configure Rate Limiting

```bash
# Add to /etc/nginx/nginx.conf in http block
sudo tee -a /etc/nginx/nginx.conf <<EOF
# Rate limiting zones
limit_req_zone \$binary_remote_addr zone=api_limit:10m rate=100r/m;
limit_req_zone \$binary_remote_addr zone=login_limit:10m rate=5r/m;
limit_conn_zone \$binary_remote_addr zone=conn_limit:10m;
EOF

# Update server block to use rate limiting
# Add to location /api/:
#   limit_req zone=api_limit burst=20 nodelay;
#   limit_conn conn_limit 10;
```

---

## SSL/TLS Certificate Setup

### Option 1: Let's Encrypt (Free, Recommended for Production)

```bash
# Install Certbot
sudo apt install -y certbot python3-certbot-nginx

# Obtain certificate
sudo certbot --nginx -d library.gcrf.com -d www.library.gcrf.com

# Certificates will be placed in:
# /etc/letsencrypt/live/library.gcrf.com/fullchain.pem
# /etc/letsencrypt/live/library.gcrf.com/privkey.pem

# Set up automatic renewal
sudo systemctl enable certbot.timer
sudo systemctl start certbot.timer

# Test renewal
sudo certbot renew --dry-run
```

### Option 2: Self-Signed Certificate (Development/Testing)

```bash
# Generate self-signed certificate
sudo mkdir -p /etc/nginx/ssl
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /etc/nginx/ssl/privkey.pem \
  -out /etc/nginx/ssl/fullchain.pem \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=GCRF/CN=library.gcrf.com"
```

### Option 3: Commercial Certificate

```bash
# Copy commercial certificate files
sudo mkdir -p /etc/nginx/ssl
sudo cp fullchain.pem /etc/nginx/ssl/
sudo cp privkey.pem /etc/nginx/ssl/
sudo chmod 600 /etc/nginx/ssl/privkey.pem
```

---

## Post-Installation Verification

### 1. Complete System Health Check

Run the comprehensive health check script:

```bash
#!/bin/bash
# /usr/local/bin/system-health-check.sh

echo "========================================="
echo "GCRF Library System Health Check"
echo "========================================="
echo ""

# Infrastructure Services
echo "1. Infrastructure Services"
echo "-------------------------"
/usr/local/bin/check-infrastructure.sh
echo ""

# Application Services
echo "2. Application Services"
echo "-------------------------"
/usr/local/bin/check-services.sh
echo ""

# Network Connectivity
echo "3. Network Connectivity"
echo "-------------------------"
curl -sf https://library.gcrf.com/health && echo "✓ External HTTPS: OK" || echo "✗ External HTTPS: FAILED"
curl -sf http://localhost:8080/actuator/health && echo "✓ Gateway Internal: OK" || echo "✗ Gateway Internal: FAILED"
echo ""

# Monitoring
echo "4. Monitoring Services"
echo "-------------------------"
curl -sf http://localhost:9090/-/healthy && echo "✓ Prometheus: OK" || echo "✗ Prometheus: FAILED"
curl -sf http://localhost:3000/api/health && echo "✓ Grafana: OK" || echo "✗ Grafana: FAILED"
echo ""

echo "========================================="
echo "Health Check Complete"
echo "========================================="
```

### 2. Test API Endpoints

```bash
# Test authentication
curl -X POST https://library.gcrf.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Test book API (with token)
curl -X GET https://library.gcrf.com/api/v1/books \
  -H "Authorization: Bearer <token>"

# Test reader API
curl -X GET https://library.gcrf.com/api/v1/readers \
  -H "Authorization: Bearer <token>"
```

### 3. Verify Monitoring

- Access Grafana: https://library.gcrf.com:3000
- Access Prometheus: http://localhost:9090
- Check all dashboards are displaying data
- Verify alerts are configured

### 4. Check Logs

```bash
# Application logs
docker-compose -f docker-compose.services.yml logs -f gateway-service
docker-compose -f docker-compose.services.yml logs -f auth-service

# Nginx logs
tail -f /var/log/nginx/gcrf-library-access.log
tail -f /var/log/nginx/gcrf-library-error.log

# System logs
journalctl -u docker -f
```

---

## Initial System Configuration

### 1. Create Admin User

```bash
# Connect to auth database
docker exec -it postgres-primary psql -U postgres -d gcrf_auth

# Insert admin user (password: admin123, bcrypt hashed)
INSERT INTO users (username, password, email, role, status, created_at)
VALUES (
  'admin',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
  'admin@gcrf-library.com',
  'ADMIN',
  'ACTIVE',
  NOW()
);
```

### 2. Configure System Settings

Login to admin panel: https://library.gcrf.com
- Navigate to System Settings
- Configure email notifications (SMTP settings)
- Configure book borrowing rules
- Configure fine policies
- Set up system announcements

### 3. Import Initial Data

```bash
# Import book categories
# Import sample books
# Import reader types
# Configure circulation rules
```

### 4. Schedule Backup Jobs

```bash
# Add to crontab
crontab -e

# Daily database backup at 2 AM
0 2 * * * /usr/local/bin/backup-database.sh

# Weekly full system backup (Saturday 3 AM)
0 3 * * 6 /usr/local/bin/backup-full-system.sh

# Daily log rotation
0 0 * * * /usr/local/bin/rotate-logs.sh
```

---

## Installation Complete

The GCRF Library Management System is now successfully installed and configured!

### Next Steps

1. **Read the Operations Manual**: `operations-manual.md`
2. **Review the Configuration Checklist**: `configuration-checklist.md`
3. **Set up Monitoring Alerts**: Configure AlertManager rules
4. **Train Staff**: Provide training on system usage
5. **Plan Regular Backups**: Verify backup automation is working

### Important URLs

- **Web Admin**: https://library.gcrf.com
- **API Gateway**: https://library.gcrf.com/api
- **Grafana Monitoring**: https://library.gcrf.com:3000
- **Nacos Console**: http://localhost:8848/nacos (internal only)
- **RabbitMQ Management**: http://localhost:15672 (internal only)

### Support

For issues or questions, refer to:
- Troubleshooting Guide: `troubleshooting-guide.md`
- Operations Manual: `operations-manual.md`
- Architecture Documentation: `/docs/architecture/architect.md`

---

**Document Version**: 1.0.0
**Last Updated**: 2025-12-01
**Installation Tested On**: Ubuntu 22.04 LTS, CentOS Stream 9
