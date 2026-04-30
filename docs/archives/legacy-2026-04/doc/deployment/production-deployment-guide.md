# GCRF Library Management System - Production Deployment Guide

**Project**: 国创睿峰智能图书馆管理系统 (GCRF Intelligent Library Management System)
**Version**: 1.0.0
**Last Updated**: 2025-12-01
**Document Status**: Production Ready

---

## Table of Contents

1. [Overview](#overview)
2. [Server Requirements](#server-requirements)
3. [Network Architecture](#network-architecture)
4. [Deployment Topology](#deployment-topology)
5. [Port Planning](#port-planning)
6. [Security Considerations](#security-considerations)
7. [High Availability Setup](#high-availability-setup)
8. [Disaster Recovery Plan](#disaster-recovery-plan)

---

## Overview

This guide provides comprehensive information for deploying the GCRF Library Management System in a production environment. The system uses a microservices architecture with Spring Cloud and supports deployment on physical servers, virtual machines, or container orchestration platforms.

### Architecture Summary

- **Frontend**: Vue 3 + Element Plus (Nginx static hosting)
- **API Gateway**: Spring Cloud Gateway (port 8080)
- **Microservices**: 10+ services (Spring Boot 3.2.2)
- **Service Registry**: Nacos 2.3.x
- **Databases**: PostgreSQL 15+ (primary data store)
- **Cache**: Redis 7.x (single-master, sentinel for HA)
- **Message Queue**: RabbitMQ 3.12.x
- **Object Storage**: MinIO
- **Search Engine**: Elasticsearch 8.x
- **Monitoring**: Prometheus + Grafana + Loki
- **Container Platform**: Docker + Docker Compose

---

## Server Requirements

### Minimum Production Configuration

#### Single Server Deployment (Small Scale: <5000 users)

| Component | Specification |
|-----------|---------------|
| **CPU** | 8 cores (Intel Xeon or equivalent) |
| **Memory** | 32 GB RAM |
| **Storage** | 500 GB SSD (OS + Applications) <br> 1 TB HDD (Data + Backups) |
| **Network** | 1 Gbps Ethernet |
| **OS** | Ubuntu 22.04 LTS / CentOS Stream 9 / RHEL 9 |

#### Multi-Server Deployment (Medium Scale: 5000-20000 users)

**Application Server Cluster (2-3 nodes)**
- **CPU**: 8 cores per node
- **Memory**: 16 GB per node
- **Storage**: 200 GB SSD per node
- **Network**: 1 Gbps Ethernet

**Database Server (Primary + Standby)**
- **CPU**: 16 cores
- **Memory**: 64 GB
- **Storage**: 1 TB NVMe SSD (database files) + 2 TB HDD (backups)
- **Network**: 10 Gbps Ethernet (recommended)

**Cache/Message Queue Server**
- **CPU**: 8 cores
- **Memory**: 32 GB
- **Storage**: 200 GB SSD
- **Network**: 1 Gbps Ethernet

**Storage Server (MinIO)**
- **CPU**: 4 cores
- **Memory**: 8 GB
- **Storage**: 4 TB HDD (object storage)
- **Network**: 1 Gbps Ethernet

**Monitoring Server**
- **CPU**: 4 cores
- **Memory**: 16 GB
- **Storage**: 500 GB SSD
- **Network**: 1 Gbps Ethernet

#### Large Scale Deployment (>20000 users)

**Load Balancer (2 nodes for HA)**
- **CPU**: 4 cores per node
- **Memory**: 8 GB per node
- **Network**: 10 Gbps Ethernet

**Application Server Cluster (5+ nodes)**
- **CPU**: 16 cores per node
- **Memory**: 32 GB per node
- **Storage**: 200 GB SSD per node
- **Network**: 10 Gbps Ethernet

**Database Cluster (Primary + 2 Standby + 2 Read Replicas)**
- **CPU**: 32 cores per node
- **Memory**: 128 GB per node
- **Storage**: 2 TB NVMe SSD per node
- **Network**: 10 Gbps Ethernet

**Redis Cluster (3 masters + 3 replicas)**
- **CPU**: 8 cores per node
- **Memory**: 64 GB per node
- **Storage**: 200 GB SSD per node
- **Network**: 10 Gbps Ethernet

**Elasticsearch Cluster (3+ nodes)**
- **CPU**: 16 cores per node
- **Memory**: 32 GB per node
- **Storage**: 1 TB SSD per node
- **Network**: 10 Gbps Ethernet

### Software Requirements

| Software | Version | Purpose |
|----------|---------|---------|
| **Operating System** | Ubuntu 22.04 LTS / CentOS Stream 9 | Host OS |
| **Docker** | 24.0+ | Container runtime |
| **Docker Compose** | 2.20+ | Container orchestration |
| **Java** | OpenJDK 21 | Application runtime |
| **PostgreSQL** | 15.x or 16.x | Primary database |
| **Redis** | 7.2+ | Cache and session store |
| **RabbitMQ** | 3.12+ | Message broker |
| **Nacos** | 2.3+ | Service registry |
| **MinIO** | RELEASE.2024+ | Object storage |
| **Elasticsearch** | 8.x | Full-text search |
| **Nginx** | 1.24+ | Reverse proxy and load balancer |

### Storage Requirements

#### Database Storage Planning

| Database | Estimated Size (First Year) | Growth Rate |
|----------|---------------------------|-------------|
| gcrf_auth | 100 MB | 10 MB/month |
| gcrf_book | 2 GB | 200 MB/month |
| gcrf_reader | 500 MB | 50 MB/month |
| gcrf_circulation | 5 GB | 500 MB/month |
| gcrf_system | 200 MB | 20 MB/month |
| gcrf_notification | 1 GB | 100 MB/month |
| gcrf_analytics | 10 GB | 1 GB/month |
| gcrf_recommend | 3 GB | 300 MB/month |
| **Total** | **~22 GB** | **~2.2 GB/month** |

#### Object Storage Planning

| Storage Type | Estimated Size | Growth Rate |
|--------------|----------------|-------------|
| Book Cover Images | 50 GB | 5 GB/month |
| Book PDF Files | 200 GB | 20 GB/month |
| User Avatars | 10 GB | 1 GB/month |
| System Documents | 5 GB | 500 MB/month |
| **Total** | **265 GB** | **26.5 GB/month** |

#### Log Storage Planning

| Log Type | Retention Period | Daily Size | Total Size |
|----------|------------------|------------|------------|
| Application Logs | 30 days | 5 GB | 150 GB |
| Audit Logs | 365 days | 1 GB | 365 GB |
| Access Logs | 90 days | 10 GB | 900 GB |
| **Total** | - | **16 GB/day** | **~1.4 TB** |

**Recommendation**: Allocate 2 TB for logs with log rotation and archival.

---

## Network Architecture

### Network Topology Diagram

```
                                    Internet
                                       |
                                       v
                        ┌──────────────────────────┐
                        │  External Firewall/WAF   │
                        │  (HTTPS/443, HTTP/80)    │
                        └──────────────────────────┘
                                       |
                                       v
                        ┌──────────────────────────┐
                        │   Load Balancer (Nginx)  │
                        │   Public IP: X.X.X.X     │
                        │   Ports: 443, 80         │
                        └──────────────────────────┘
                                       |
                    ┌──────────────────┴──────────────────┐
                    |                                     |
        ┌───────────v──────────┐           ┌─────────────v────────┐
        │   DMZ Zone           │           │   Management Zone    │
        │   (172.16.1.0/24)    │           │   (172.16.254.0/24)  │
        │                      │           │                      │
        │ - Web Admin (Nginx)  │           │ - Bastion Host       │
        │ - API Gateway        │           │ - Monitoring (Grafana)│
        └──────────────────────┘           └──────────────────────┘
                    |
        ┌───────────┴────────────────────────┐
        |                                    |
┌───────v──────────┐              ┌─────────v─────────┐
│ Application Zone │              │  Data Zone        │
│ (172.16.10.0/24) │              │  (172.16.20.0/24) │
│                  │              │                   │
│ - Auth Service   │◄────────────►│ - PostgreSQL      │
│ - Book Service   │              │ - Redis Master    │
│ - Reader Service │              │ - Redis Sentinel  │
│ - Circulation    │              │ - RabbitMQ        │
│ - System Service │              │ - Nacos           │
│ - Notification   │              │ - Elasticsearch   │
│ - Analytics      │              │ - MinIO           │
│ - Recommend      │              │                   │
└──────────────────┘              └───────────────────┘
        |                                    |
        └────────────────┬───────────────────┘
                         |
              ┌──────────v──────────┐
              │  Backup Zone        │
              │  (172.16.30.0/24)   │
              │                     │
              │  - Backup Server    │
              │  - Log Archive      │
              └─────────────────────┘
```

### Network Zones and Security

#### DMZ Zone (172.16.1.0/24)
- **Purpose**: Public-facing services
- **Components**:
  - Web Admin Frontend (Nginx)
  - API Gateway (Spring Cloud Gateway)
- **Access**: Internet → DMZ (HTTPS/443, HTTP/80)
- **Firewall Rules**:
  - Inbound: Allow 80/443 from Internet
  - Outbound: Allow to Application Zone on specific ports only
  - No direct access to Data Zone

#### Application Zone (172.16.10.0/24)
- **Purpose**: Business logic microservices
- **Components**: All Spring Boot microservices
- **Access**: DMZ → Application Zone
- **Firewall Rules**:
  - Inbound: Allow from DMZ on ports 8081-8090
  - Outbound: Allow to Data Zone on database/cache ports
  - No direct Internet access (outbound via proxy if needed)

#### Data Zone (172.16.20.0/24)
- **Purpose**: Data persistence and messaging
- **Components**: PostgreSQL, Redis, RabbitMQ, Nacos, Elasticsearch, MinIO
- **Access**: Application Zone → Data Zone
- **Firewall Rules**:
  - Inbound: Allow from Application Zone only
  - Outbound: Block all except internal replication
  - No Internet access
  - No DMZ access

#### Management Zone (172.16.254.0/24)
- **Purpose**: Operations and monitoring
- **Components**: Bastion host, Grafana, Prometheus, AlertManager
- **Access**: VPN/Bastion → All zones (read-only where possible)
- **Firewall Rules**:
  - Inbound: SSH from corporate VPN only
  - Outbound: Allow to all zones for monitoring
  - Grafana accessible via VPN only

#### Backup Zone (172.16.30.0/24)
- **Purpose**: Data backup and archival
- **Components**: Backup server, log archive storage
- **Access**: Data Zone → Backup Zone (one-way)
- **Firewall Rules**:
  - Inbound: Allow from Data Zone for backups
  - Outbound: Block all (air-gapped)
  - No Internet access

### Network Security Best Practices

1. **Firewall Configuration**
   - Use stateful firewalls between all zones
   - Default deny all, explicitly allow required traffic
   - Implement egress filtering
   - Log all denied connections

2. **Internal Communication**
   - Use TLS for all inter-service communication in production
   - Implement mutual TLS (mTLS) for sensitive services
   - Use private Docker networks with encryption

3. **External Access**
   - All external traffic through WAF
   - Implement DDoS protection
   - Use rate limiting on public APIs
   - Enable HTTPS only (redirect HTTP to HTTPS)

4. **VPN Access**
   - Require VPN for all administrative access
   - Use certificate-based authentication
   - Implement 2FA for VPN access
   - Log all VPN sessions

---

## Deployment Topology

### Single Server Deployment Topology

```
┌─────────────────────────────────────────────────────────────┐
│                     Physical Server                         │
│                    (Ubuntu 22.04 LTS)                       │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Docker Engine                           │  │
│  │                                                      │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐    │  │
│  │  │   Nginx    │  │  Gateway   │  │  Frontend  │    │  │
│  │  │  :80/443   │  │   :8080    │  │   :3011    │    │  │
│  │  └────────────┘  └────────────┘  └────────────┘    │  │
│  │                                                      │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐    │  │
│  │  │   Auth     │  │   Book     │  │   Reader   │    │  │
│  │  │  :8081     │  │   :8082    │  │   :8084    │    │  │
│  │  └────────────┘  └────────────┘  └────────────┘    │  │
│  │                                                      │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐    │  │
│  │  │Circulation │  │   System   │  │Notification│    │  │
│  │  │  :8083     │  │   :8085    │  │   :8086    │    │  │
│  │  └────────────┘  └────────────┘  └────────────┘    │  │
│  │                                                      │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐    │  │
│  │  │ Analytics  │  │ Recommend  │  │   Nacos    │    │  │
│  │  │  :8087     │  │   :8088    │  │   :8848    │    │  │
│  │  └────────────┘  └────────────┘  └────────────┘    │  │
│  │                                                      │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐    │  │
│  │  │ PostgreSQL │  │   Redis    │  │  RabbitMQ  │    │  │
│  │  │  :5432     │  │   :6379    │  │   :5672    │    │  │
│  │  └────────────┘  └────────────┘  └────────────┘    │  │
│  │                                                      │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐    │  │
│  │  │   MinIO    │  │Elasticsearch│ │ Prometheus │    │  │
│  │  │:9000/:9001 │  │:9200/:9300 │  │   :9090    │    │  │
│  │  └────────────┘  └────────────┘  └────────────┘    │  │
│  │                                                      │  │
│  │  ┌────────────┐  ┌────────────┐                     │  │
│  │  │  Grafana   │  │    Loki    │                     │  │
│  │  │   :3000    │  │   :3100    │                     │  │
│  │  └────────────┘  └────────────┘                     │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  Volume Mounts:                                             │
│  - /data/postgresql → PostgreSQL data                      │
│  - /data/redis → Redis data                                │
│  - /data/minio → Object storage                            │
│  - /data/elasticsearch → Search indices                    │
│  - /logs → Application logs                                │
│  - /backups → Database backups                             │
└─────────────────────────────────────────────────────────────┘
```

### Multi-Server Deployment Topology (Recommended for Production)

```
┌──────────────────────────────────────────────────────────────┐
│                    Load Balancer Layer                       │
│  ┌────────────────┐              ┌────────────────┐          │
│  │  Nginx LB 1    │              │  Nginx LB 2    │          │
│  │  (Active)      │◄────────────►│  (Standby)     │          │
│  │  VIP: X.X.X.X  │  Keepalived  │                │          │
│  └────────────────┘              └────────────────┘          │
└──────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────v────────┐  ┌───────v────────┐  ┌──────v─────────┐
│  App Server 1  │  │  App Server 2  │  │  App Server 3  │
│                │  │                │  │                │
│ - Gateway      │  │ - Gateway      │  │ - Gateway      │
│ - Auth (x2)    │  │ - Auth (x2)    │  │ - Auth (x2)    │
│ - Book (x2)    │  │ - Book (x2)    │  │ - Book (x2)    │
│ - Reader       │  │ - Reader       │  │ - Circulation  │
│ - Analytics    │  │ - System       │  │ - Notification │
│ - Recommend    │  │ - Nacos        │  │                │
└────────────────┘  └────────────────┘  └────────────────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────v────────┐  ┌───────v────────┐  ┌──────v─────────┐
│  DB Primary    │  │  DB Standby 1  │  │  DB Standby 2  │
│  PostgreSQL    │─►│  PostgreSQL    │─►│  PostgreSQL    │
│  (Write)       │  │  (Read/Sync)   │  │  (Read/Async)  │
└────────────────┘  └────────────────┘  └────────────────┘
        │
        │           ┌────────────────┐  ┌────────────────┐
        ├──────────►│ Redis Master   │─►│ Redis Replica  │
        │           │ + Sentinel     │  │ + Sentinel     │
        │           └────────────────┘  └────────────────┘
        │
        │           ┌────────────────┐  ┌────────────────┐
        ├──────────►│ RabbitMQ Node1 │◄►│ RabbitMQ Node2 │
        │           │ (Cluster)      │  │ (Cluster)      │
        │           └────────────────┘  └────────────────┘
        │
        │           ┌────────────────┐
        └──────────►│ MinIO Cluster  │
                    │ (4 nodes)      │
                    └────────────────┘
```

### Container Deployment Strategy

#### Service Distribution Strategy

**High-Priority Services (Deploy multiple instances)**
- Gateway Service: 2-3 instances
- Auth Service: 2-3 instances
- Book Service: 2-3 instances
- Circulation Service: 2 instances

**Medium-Priority Services (Deploy 1-2 instances)**
- Reader Service: 1-2 instances
- System Service: 1-2 instances
- Notification Service: 1 instance
- Analytics Service: 1 instance

**Low-Priority Services (Deploy 1 instance)**
- Recommend Service: 1 instance
- NLP Service: 1 instance (if implemented)

#### Resource Allocation per Service

| Service | CPU Limit | Memory Limit | CPU Request | Memory Request |
|---------|-----------|--------------|-------------|----------------|
| Gateway | 1.0 core | 1 GB | 0.5 core | 512 MB |
| Auth | 1.0 core | 1 GB | 0.5 core | 512 MB |
| Book | 1.0 core | 1 GB | 0.5 core | 512 MB |
| Reader | 0.5 core | 512 MB | 0.25 core | 256 MB |
| Circulation | 1.0 core | 1 GB | 0.5 core | 512 MB |
| System | 0.5 core | 512 MB | 0.25 core | 256 MB |
| Notification | 0.5 core | 512 MB | 0.25 core | 256 MB |
| Analytics | 2.0 cores | 2 GB | 1.0 core | 1 GB |
| Recommend | 2.0 cores | 2 GB | 1.0 core | 1 GB |
| PostgreSQL | 4.0 cores | 8 GB | 2.0 cores | 4 GB |
| Redis | 2.0 cores | 4 GB | 1.0 core | 2 GB |
| Elasticsearch | 2.0 cores | 4 GB | 1.0 core | 2 GB |

---

## Port Planning

### External Ports (Accessible from Internet)

| Port | Protocol | Service | Purpose |
|------|----------|---------|---------|
| 80 | HTTP | Nginx | HTTP traffic (redirect to 443) |
| 443 | HTTPS | Nginx | HTTPS traffic (primary entry point) |
| 22 | SSH | Bastion Host | Administrative access (VPN only) |

### Internal Ports (Backend Network)

#### Application Services

| Port | Service | Component | Description |
|------|---------|-----------|-------------|
| 8080 | HTTP | API Gateway | Central API gateway |
| 8081 | HTTP | Auth Service | Authentication service |
| 8082 | HTTP | Book Service | Book management |
| 8083 | HTTP | Circulation Service | Lending/returning |
| 8084 | HTTP | Reader Service | Reader management |
| 8085 | HTTP | System Service | System configuration |
| 8086 | HTTP | Notification Service | Notifications |
| 8087 | HTTP | Analytics Service | Data analytics |
| 8088 | HTTP | Recommend Service | Recommendations |
| 8089 | HTTP | NLP Service | Natural language processing |
| 8090 | HTTP | Vision Service | Computer vision |

#### Infrastructure Services

| Port | Service | Component | Description |
|------|---------|-----------|-------------|
| 5432 | TCP | PostgreSQL | Database connections |
| 6379 | TCP | Redis | Cache and session store |
| 5672 | TCP | RabbitMQ | AMQP protocol |
| 15672 | HTTP | RabbitMQ | Management UI |
| 8848 | HTTP | Nacos | Service registry and config |
| 9848 | TCP | Nacos | gRPC port |
| 9000 | HTTP | MinIO | Object storage API |
| 9001 | HTTP | MinIO | Management console |
| 9200 | HTTP | Elasticsearch | REST API |
| 9300 | TCP | Elasticsearch | Node communication |

#### Monitoring Services

| Port | Service | Component | Description |
|------|---------|-----------|-------------|
| 3000 | HTTP | Grafana | Monitoring dashboards |
| 9090 | HTTP | Prometheus | Metrics collection |
| 9093 | HTTP | AlertManager | Alert management |
| 3100 | HTTP | Loki | Log aggregation |

#### Actuator Ports (Health Checks)

All Spring Boot services expose actuator endpoints on their main port:
- `/actuator/health` - Health check endpoint
- `/actuator/metrics` - Metrics endpoint
- `/actuator/info` - Service information

### Port Mapping Matrix

#### Development Environment

```
Host Port → Container Port mapping:
80        → nginx:80
443       → nginx:443
8080      → gateway:8080
8848      → nacos:8848
5432      → postgres:5432
6379      → redis:6379
9000      → minio:9000
9001      → minio:9001
3000      → grafana:3000
9090      → prometheus:9090
```

#### Production Environment

```
External Load Balancer:
443 (HTTPS) → nginx:443 → gateway:8080 → services:80xx

Internal Services:
All services communicate via internal Docker network
No direct port exposure to host (except load balancer)
```

### Firewall Rules Table

#### DMZ Zone Firewall Rules

| Rule # | Source | Destination | Port | Protocol | Action | Purpose |
|--------|--------|-------------|------|----------|--------|---------|
| 1 | Internet | Nginx | 80 | TCP | ALLOW | HTTP traffic |
| 2 | Internet | Nginx | 443 | TCP | ALLOW | HTTPS traffic |
| 3 | Nginx | Gateway | 8080 | TCP | ALLOW | API routing |
| 4 | ANY | ANY | ANY | ANY | DENY | Default deny |

#### Application Zone Firewall Rules

| Rule # | Source | Destination | Port | Protocol | Action | Purpose |
|--------|--------|-------------|------|----------|--------|---------|
| 1 | Gateway | All Services | 8081-8090 | TCP | ALLOW | Service access |
| 2 | Services | PostgreSQL | 5432 | TCP | ALLOW | Database access |
| 3 | Services | Redis | 6379 | TCP | ALLOW | Cache access |
| 4 | Services | RabbitMQ | 5672 | TCP | ALLOW | Message queue |
| 5 | Services | Nacos | 8848 | TCP | ALLOW | Service registry |
| 6 | Services | MinIO | 9000 | TCP | ALLOW | Object storage |
| 7 | Services | Elasticsearch | 9200 | TCP | ALLOW | Search API |
| 8 | ANY | ANY | ANY | ANY | DENY | Default deny |

#### Data Zone Firewall Rules

| Rule # | Source | Destination | Port | Protocol | Action | Purpose |
|--------|--------|-------------|------|----------|--------|---------|
| 1 | App Zone | PostgreSQL | 5432 | TCP | ALLOW | Database access |
| 2 | App Zone | Redis | 6379 | TCP | ALLOW | Cache access |
| 3 | PostgreSQL | Backup Zone | 5432 | TCP | ALLOW | Backup replication |
| 4 | ANY | ANY | ANY | ANY | DENY | Default deny |

---

## Security Considerations

### 1. Application Security

#### JWT Token Security
- **Algorithm**: RS256 (RSA signature with SHA-256)
- **Key Length**: 2048 bits minimum
- **Token Expiration**:
  - Access Token: 1 hour
  - Refresh Token: 7 days
- **Token Rotation**: Implement refresh token rotation
- **Key Storage**: Store private keys in Kubernetes secrets or HashiCorp Vault

#### API Security
- **Rate Limiting**: 100 requests/minute per IP
- **Request Size Limit**: 10 MB maximum
- **CORS Configuration**: Whitelist specific domains only
- **SQL Injection Prevention**: Use parameterized queries (MyBatis-Plus handles this)
- **XSS Prevention**: Sanitize all user inputs
- **CSRF Protection**: Use CSRF tokens for state-changing operations

### 2. Database Security

#### PostgreSQL Security
- **Authentication**: Use strong passwords (16+ characters)
- **Encryption**: Enable SSL/TLS for all connections
- **Access Control**: Create separate users per service with minimal privileges
- **Audit Logging**: Enable pgaudit for sensitive tables
- **Data at Rest**: Use LUKS or database-level encryption

**Example User Privileges**:
```sql
-- Auth service user
CREATE USER auth_user WITH PASSWORD 'strong_password';
GRANT CONNECT ON DATABASE gcrf_auth TO auth_user;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO auth_user;
REVOKE DELETE ON sensitive_tables FROM auth_user;

-- Book service user (read-only for some tables)
CREATE USER book_user WITH PASSWORD 'strong_password';
GRANT CONNECT ON DATABASE gcrf_book TO book_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON books, categories TO book_user;
GRANT SELECT ON system_config TO book_user;
```

### 3. Redis Security

- **Authentication**: Enable requirepass
- **Network Isolation**: Bind to private network only
- **Disable Dangerous Commands**:
  ```
  rename-command CONFIG ""
  rename-command FLUSHALL ""
  rename-command FLUSHDB ""
  ```
- **Protected Mode**: Enable in redis.conf
- **TLS**: Enable for production

### 4. Container Security

#### Image Security
- **Use Official Base Images**: OpenJDK 21-slim, Alpine Linux
- **Scan for Vulnerabilities**: Use Trivy or Clair
- **Multi-stage Builds**: Separate build and runtime images
- **Non-root User**: Run containers as non-root user
- **Read-only Filesystem**: Where possible

#### Docker Security
- **Limit Resources**: Set CPU and memory limits
- **Disable Privileged Containers**: Never use --privileged
- **Use Secrets Management**: Docker secrets or external vault
- **Network Segmentation**: Use custom bridge networks
- **Regular Updates**: Keep Docker engine updated

### 5. SSL/TLS Configuration

#### Nginx SSL Configuration
```nginx
ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers 'ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384';
ssl_prefer_server_ciphers on;
ssl_session_cache shared:SSL:10m;
ssl_session_timeout 10m;
ssl_stapling on;
ssl_stapling_verify on;
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
```

#### Certificate Management
- **Certificate Authority**: Use Let's Encrypt for public-facing services
- **Certificate Renewal**: Automate with Certbot
- **Private Certificates**: Use internal CA for internal services
- **Certificate Monitoring**: Alert on expiration (30 days before)

### 6. Secrets Management

#### Secret Storage Strategy

**Development Environment**:
- Use `.env` files (NOT committed to Git)
- Use Docker secrets for sensitive data

**Production Environment**:
- Use HashiCorp Vault or AWS Secrets Manager
- Use Kubernetes secrets with encryption at rest
- Implement secret rotation (90 days)

#### Sensitive Configuration Items

| Secret Type | Example | Storage Method |
|-------------|---------|----------------|
| Database Passwords | PostgreSQL root password | Vault/K8s Secret |
| JWT Private Key | RS256 private key | Vault/K8s Secret |
| Redis Password | Redis requirepass | Vault/K8s Secret |
| SMTP Credentials | Email service password | Vault/K8s Secret |
| MinIO Keys | Access key and secret key | Vault/K8s Secret |
| API Keys | Third-party API keys | Vault/K8s Secret |

### 7. Network Security

#### Firewall Configuration
- Use iptables or firewalld
- Default deny all incoming traffic
- Allow only required ports
- Log all denied connections

#### DDoS Protection
- Use rate limiting (Nginx limit_req_zone)
- Implement connection limits
- Use CDN with DDoS protection (e.g., Cloudflare)
- Configure SYN flood protection

#### Intrusion Detection
- Deploy OSSEC or Wazuh
- Monitor for suspicious activity
- Alert on multiple failed login attempts
- Implement IP blacklisting

### 8. Compliance and Audit

#### Audit Logging
- Log all authentication attempts
- Log all administrative actions
- Log all data access (sensitive tables)
- Retain audit logs for 1 year minimum

#### Data Privacy
- Implement data masking for sensitive fields
- Support GDPR right to be forgotten
- Encrypt PII at rest and in transit
- Regular privacy impact assessments

#### Compliance Standards
- **SOC 2 Type II**: Access controls, monitoring, incident response
- **ISO 27001**: Information security management
- **PCI-DSS**: If handling payment data
- **HIPAA**: If handling health information (rare for library systems)

---

## High Availability Setup

### 1. Load Balancer HA (Nginx + Keepalived)

#### Architecture
- 2 Nginx servers with Keepalived for failover
- Virtual IP (VIP) for transparent failover
- Health checks to detect failures

#### Configuration

**Keepalived on Master (Nginx LB 1)**:
```bash
vrrp_script check_nginx {
    script "/usr/local/bin/check_nginx.sh"
    interval 2
    weight -20
}

vrrp_instance VI_1 {
    state MASTER
    interface eth0
    virtual_router_id 51
    priority 100
    advert_int 1

    authentication {
        auth_type PASS
        auth_pass secret_password
    }

    virtual_ipaddress {
        192.168.1.100/24
    }

    track_script {
        check_nginx
    }
}
```

**Health Check Script**:
```bash
#!/bin/bash
# /usr/local/bin/check_nginx.sh
if ! curl -f http://localhost:80/health > /dev/null 2>&1; then
    exit 1
fi
exit 0
```

### 2. Database HA (PostgreSQL Streaming Replication)

#### Architecture
- 1 Primary (read/write)
- 2 Standby servers (read-only, hot standby)
- Automatic failover with Patroni or repmgr

#### Replication Setup

**Primary Server Configuration** (`postgresql.conf`):
```ini
wal_level = replica
max_wal_senders = 10
max_replication_slots = 10
hot_standby = on
archive_mode = on
archive_command = 'cp %p /data/postgresql/archive/%f'
```

**Standby Server Configuration** (`recovery.conf`):
```ini
standby_mode = on
primary_conninfo = 'host=postgres-primary port=5432 user=replicator password=secret'
restore_command = 'cp /data/postgresql/archive/%f %p'
trigger_file = '/tmp/postgresql.trigger'
```

#### Automatic Failover with Patroni

**Patroni Configuration** (`patroni.yml`):
```yaml
scope: gcrf-postgres-cluster
name: postgres1

restapi:
  listen: 0.0.0.0:8008
  connect_address: postgres1:8008

etcd:
  host: etcd:2379

bootstrap:
  dcs:
    ttl: 30
    loop_wait: 10
    retry_timeout: 10
    maximum_lag_on_failover: 1048576
    postgresql:
      use_pg_rewind: true
      parameters:
        max_connections: 200
        shared_buffers: 2GB

postgresql:
  listen: 0.0.0.0:5432
  connect_address: postgres1:5432
  data_dir: /data/postgresql
  pgpass: /tmp/pgpass
  authentication:
    replication:
      username: replicator
      password: secret
    superuser:
      username: postgres
      password: secret
```

### 3. Redis HA (Redis Sentinel)

#### Architecture
- 1 Master (read/write)
- 2 Replicas (read-only)
- 3 Sentinel nodes (monitoring and failover)

#### Sentinel Configuration

**Sentinel Config** (`sentinel.conf`):
```ini
port 26379
sentinel monitor gcrf-redis redis-master 6379 2
sentinel down-after-milliseconds gcrf-redis 5000
sentinel parallel-syncs gcrf-redis 1
sentinel failover-timeout gcrf-redis 10000
sentinel auth-pass gcrf-redis your_redis_password
```

**Start Sentinel**:
```bash
docker run -d --name redis-sentinel-1 \
  --network gcrf-backend-network \
  -v /path/to/sentinel.conf:/etc/redis/sentinel.conf \
  redis:7.2-alpine \
  redis-sentinel /etc/redis/sentinel.conf
```

### 4. Application Service HA

#### Strategy
- Deploy multiple instances of critical services
- Use service registry (Nacos) for dynamic discovery
- Implement client-side load balancing (Spring Cloud LoadBalancer)
- Health checks and automatic de-registration

#### Service Scaling

**Scale Gateway Service**:
```bash
docker-compose up -d --scale gcrf-gateway-service=3
```

**Scale Auth Service**:
```bash
docker-compose up -d --scale gcrf-auth-service=2
```

**Service Discovery**:
- Nacos automatically tracks all instances
- Gateway routes to healthy instances only
- Failed instances are removed from rotation

### 5. Message Queue HA (RabbitMQ Cluster)

#### Architecture
- 3-node RabbitMQ cluster
- Mirrored queues for critical queues
- Load balancer for client connections

#### Cluster Setup

**RabbitMQ Cluster Configuration**:
```bash
# Node 1
docker run -d --name rabbitmq1 \
  --hostname rabbitmq1 \
  -e RABBITMQ_ERLANG_COOKIE='secret_cookie' \
  -e RABBITMQ_NODENAME=rabbit@rabbitmq1 \
  rabbitmq:3.12-management-alpine

# Node 2 - Join cluster
docker exec rabbitmq2 rabbitmqctl stop_app
docker exec rabbitmq2 rabbitmqctl join_cluster rabbit@rabbitmq1
docker exec rabbitmq2 rabbitmqctl start_app

# Node 3 - Join cluster
docker exec rabbitmq3 rabbitmqctl stop_app
docker exec rabbitmq3 rabbitmqctl join_cluster rabbit@rabbitmq1
docker exec rabbitmq3 rabbitmqctl start_app
```

**Enable HA Policy**:
```bash
rabbitmqctl set_policy ha-all "^" '{"ha-mode":"all","ha-sync-mode":"automatic"}'
```

---

## Disaster Recovery Plan

### 1. Backup Strategy

#### Database Backup

**Daily Full Backup**:
```bash
#!/bin/bash
# /usr/local/bin/backup-postgresql.sh
BACKUP_DIR="/backups/postgresql"
DATE=$(date +%Y%m%d_%H%M%S)

# Dump all databases
pg_dumpall -h postgres-primary -U postgres > "$BACKUP_DIR/full_backup_$DATE.sql"

# Compress
gzip "$BACKUP_DIR/full_backup_$DATE.sql"

# Upload to S3/MinIO
aws s3 cp "$BACKUP_DIR/full_backup_$DATE.sql.gz" \
  s3://gcrf-backups/postgresql/

# Cleanup old backups (keep 30 days)
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete
```

**Schedule with cron**:
```cron
0 2 * * * /usr/local/bin/backup-postgresql.sh
```

#### Object Storage Backup

**MinIO Replication**:
```bash
# Configure MinIO site replication
mc admin replicate add minio1 minio2 \
  --accesskey minioadmin \
  --secretkey minioadmin
```

### 2. Recovery Procedures

#### Database Recovery

**Point-in-Time Recovery (PITR)**:
```bash
# Stop PostgreSQL
systemctl stop postgresql

# Restore base backup
tar -xzf /backups/postgresql/base_backup_20251201.tar.gz \
  -C /data/postgresql/

# Create recovery configuration
cat > /data/postgresql/recovery.conf <<EOF
restore_command = 'cp /data/postgresql/archive/%f %p'
recovery_target_time = '2025-12-01 14:30:00'
EOF

# Start PostgreSQL
systemctl start postgresql
```

#### Application Recovery

**Restore from Backup**:
```bash
# Pull latest working images
docker pull gcrf-library/gateway-service:v1.0.0
docker pull gcrf-library/auth-service:v1.0.0

# Restart services
docker-compose down
docker-compose up -d
```

### 3. Disaster Recovery Site

#### Active-Passive DR Setup

**Primary Site** (Production):
- Full system deployment
- Handles all traffic

**DR Site** (Disaster Recovery):
- Standby PostgreSQL replica (async replication)
- Standby Redis replica
- Pre-configured application containers (stopped)
- Regular data sync from primary

**Failover Procedure**:
1. Detect primary site failure (monitoring alerts)
2. Promote DR database to primary
3. Update DNS to point to DR site IP
4. Start application containers at DR site
5. Verify all services are operational

**Recovery Time Objective (RTO)**: 30 minutes
**Recovery Point Objective (RPO)**: 5 minutes

### 4. Regular DR Testing

**Quarterly DR Drill Schedule**:
1. Simulate primary site failure
2. Execute failover procedure
3. Verify application functionality
4. Test data integrity
5. Failback to primary site
6. Document lessons learned

---

## Summary

This production deployment guide provides comprehensive information for deploying the GCRF Library Management System in a production environment. Key takeaways:

1. **Server Requirements**: Scale from single server (small) to multi-server cluster (large)
2. **Network Architecture**: Implement zone-based security with DMZ, Application, Data, and Management zones
3. **Deployment Topology**: Use containerized deployment with Docker Compose or Kubernetes
4. **Port Planning**: Careful port allocation with firewall rules between zones
5. **Security**: Multi-layered security with SSL/TLS, secrets management, and audit logging
6. **High Availability**: Load balancer HA, database replication, Redis Sentinel, and service scaling
7. **Disaster Recovery**: Regular backups, PITR capability, and active-passive DR site

For detailed installation steps, configuration checklists, and operational procedures, refer to the companion documents:
- `installation-steps.md`
- `configuration-checklist.md`
- `operations-manual.md`
- `troubleshooting-guide.md`

---

**Document Version**: 1.0.0
**Last Updated**: 2025-12-01
**Next Review Date**: 2026-03-01
