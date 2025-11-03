# Network Security Configuration Guide

**GCRF Library Management System - Network Security Architecture**

**Version**: 1.0.0
**Last Updated**: 2025-11-01
**Author**: Infrastructure Team

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Security Zones](#security-zones)
3. [Network Topology](#network-topology)
4. [Port Exposure Strategy](#port-exposure-strategy)
5. [Firewall Configuration](#firewall-configuration)
6. [Network Isolation Rules](#network-isolation-rules)
7. [Security Best Practices](#security-best-practices)
8. [Network Monitoring](#network-monitoring)
9. [Troubleshooting Guide](#troubleshooting-guide)
10. [Compliance & Audit](#compliance--audit)

---

## Architecture Overview

The GCRF Library Management System implements a **3-tier security architecture** with strict network isolation between zones:

### Design Principles

- **Defense in Depth**: Multiple layers of security controls
- **Least Privilege**: Services only access what they need
- **Network Segmentation**: Logical separation of security zones
- **Zero Trust**: Verify every connection, even internal
- **Fail Secure**: Default deny for all traffic

### Network Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    Internet / External                       │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Firewall (iptables)
                       │ Exposed Ports: 80, 8080
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  DMZ ZONE (Frontend Network)                                │
│  Network: gcrf-frontend-network (172.29.0.0/16)            │
│  ┌─────────────────┐         ┌──────────────────┐          │
│  │  Web Admin      │◄───────►│  API Gateway     │          │
│  │  :80 (HTTP)     │         │  :8080 (HTTP)    │          │
│  │  nginx          │         │  Spring Gateway  │          │
│  └─────────────────┘         └────────┬─────────┘          │
│                                        │                     │
│  Security Controls:                    │                     │
│  - Public facing (0.0.0.0)            │                     │
│  - Rate limiting                       │                     │
│  - SSL/TLS termination                │                     │
│  - CORS policies                       │                     │
└────────────────────────────────────────┼─────────────────────┘
                                         │
                                         │ Internal Only
                                         │ No External Access
                                         ▼
┌─────────────────────────────────────────────────────────────┐
│  APPLICATION ZONE (Backend Network)                         │
│  Network: gcrf-backend-network (172.28.0.0/16)             │
│  (Shares infrastructure network for service discovery)      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Auth Service │  │ Book Service │  │ Reader Svc   │     │
│  │ :8081        │  │ :8082        │  │ :8084        │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│  ┌──────────────┐  ┌──────────────┐                        │
│  │ Circulation  │  │ System Svc   │  (Future Services)     │
│  │ :8083        │  │ :8085        │                        │
│  └──────────────┘  └──────────────┘                        │
│                                                              │
│  Security Controls:                                         │
│  - No port exposure (internal only)                        │
│  - Service mesh policies                                    │
│  - JWT validation                                           │
│  - Request authentication                                   │
└────────────────────────────────────────┼─────────────────────┘
                                         │
                                         │ Backend Only
                                         │ No Internet Access
                                         ▼
┌─────────────────────────────────────────────────────────────┐
│  DATA ZONE (Infrastructure Network)                         │
│  Network: gcrf-infrastructure-network (172.28.0.0/16)      │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐            │
│  │ PostgreSQL │ │   Redis    │ │   Nacos    │            │
│  │ :5432      │ │   :6379    │ │   :8848    │            │
│  │ Primary    │ │   Master   │ │   Config   │            │
│  └────────────┘ └────────────┘ └──────┬─────┘            │
│  ┌────────────┐ ┌────────────┐        │                   │
│  │ RabbitMQ   │ │   MinIO    │    Nacos MySQL            │
│  │ :5672      │ │   :9000    │        │                   │
│  │ :15672 UI  │ │   :9001 UI │    :3306 (internal)       │
│  └────────────┘ └────────────┘                            │
│                                                              │
│  Security Controls:                                         │
│  - Management UIs: 127.0.0.1 only                          │
│  - Database: Service authentication                         │
│  - Encryption at rest                                       │
│  - Network isolation from DMZ                               │
└─────────────────────────────────────────────────────────────┘
```

---

## Security Zones

### Zone 1: DMZ (Demilitarized Zone) - Frontend Network

**Purpose**: Public-facing web interface and API gateway

**Network**: `gcrf-frontend-network` (172.29.0.0/16)

**Services**:
- Web Admin (nginx) - Port 80
- API Gateway (Spring Cloud Gateway) - Port 8080

**Trust Level**: Low (Internet-facing)

**Access Control**:
- Exposed to internet (0.0.0.0)
- Rate limiting enabled
- CORS policies enforced
- SSL/TLS termination (production)

**Threats**:
- DDoS attacks
- SQL injection via API
- XSS attacks
- CSRF attacks
- Unauthorized access attempts

**Mitigations**:
- WAF (Web Application Firewall) rules
- Rate limiting (requests/second)
- Input validation
- Authentication required for APIs
- Security headers (CSP, HSTS)

---

### Zone 2: Application Zone - Backend Network

**Purpose**: Business logic microservices

**Network**: `gcrf-backend-network` (alias for gcrf-infrastructure-network 172.28.0.0/16)

**Services**:
- Auth Service - Port 8081 (internal only)
- Book Service - Port 8082 (future)
- Circulation Service - Port 8083 (future)
- Reader Service - Port 8084 (future)
- System Service - Port 8085 (future)
- Notification Service - Port 8086 (future)

**Trust Level**: Medium (Internal services)

**Access Control**:
- NO external port exposure
- Gateway-only access
- JWT token validation
- Service-to-service authentication

**Threats**:
- Lateral movement
- Privilege escalation
- Data exfiltration
- Service impersonation

**Mitigations**:
- Network policies (Docker network isolation)
- JWT signature verification
- Service mesh security (future)
- Audit logging
- Least privilege access

---

### Zone 3: Data Zone - Infrastructure Network

**Purpose**: Data storage and infrastructure services

**Network**: `gcrf-infrastructure-network` (172.28.0.0/16)

**Services**:
- PostgreSQL Primary - Port 5432 (internal only)
- Redis Master - Port 6379 (internal only)
- Nacos Server - Port 8848 (localhost management)
- RabbitMQ - Port 5672/15672 (localhost management UI)
- MinIO - Port 9000/9001 (localhost management UI)
- Nacos MySQL - Port 3306 (internal only)

**Trust Level**: High (Critical data)

**Access Control**:
- NO external access to data ports
- Management UIs: 127.0.0.1 only
- Password authentication required
- Database user isolation

**Threats**:
- Data breach
- Unauthorized data access
- Data corruption
- Ransomware
- Insider threats

**Mitigations**:
- Encryption at rest
- Strong passwords
- User privilege separation
- Backup and recovery
- Audit logging
- Network isolation

---

## Network Topology

### Docker Network Configuration

```yaml
# Infrastructure Network (Data + Application Zone)
networks:
  gcrf-infrastructure-network:
    driver: bridge
    name: gcrf-infrastructure-network
    internal: false  # Services need internet for updates
    ipam:
      driver: default
      config:
        - subnet: 172.28.0.0/16
          gateway: 172.28.0.1

# Frontend Network (DMZ)
networks:
  gcrf-frontend-network:
    driver: bridge
    name: gcrf-frontend-network
    internal: false  # Exposed to host
    ipam:
      driver: default
      config:
        - subnet: 172.29.0.0/16
          gateway: 172.29.0.1
```

### Network Connectivity Matrix

| Source Zone    | Destination Zone | Allowed | Protocol | Purpose |
|----------------|------------------|---------|----------|---------|
| Internet       | DMZ              | Yes     | HTTP/HTTPS | Public web/API access |
| Internet       | Application      | No      | -        | Blocked by firewall |
| Internet       | Data             | No      | -        | Blocked by firewall |
| DMZ            | Application      | Yes     | HTTP     | Gateway to services |
| DMZ            | Data             | No      | -        | No direct access |
| Application    | Data             | Yes     | TCP      | Database/cache access |
| Application    | Application      | Yes     | HTTP     | Service-to-service |
| Localhost      | Data (mgmt)      | Yes     | HTTP/HTTPS | Management UIs |

### Service Discovery Flow

```
┌──────────┐      ┌─────────┐      ┌────────────┐
│ Gateway  │─────►│  Nacos  │◄─────│ Auth Svc   │
└──────────┘      └─────────┘      └────────────┘
     │                  │                  │
     │                  │                  │
     ▼                  ▼                  ▼
┌─────────────────────────────────────────────┐
│  gcrf-infrastructure-network (172.28.0.0)   │
└─────────────────────────────────────────────┘

Flow:
1. Services register with Nacos on startup
2. Gateway discovers service endpoints
3. Client requests route via service names
4. Docker DNS resolves container names
5. Traffic flows within network
```

---

## Port Exposure Strategy

### External Port Mapping

| External Port | Internal Port | Service | Zone | Binding | Purpose |
|---------------|---------------|---------|------|---------|---------|
| 80            | 80            | web-admin | DMZ | 0.0.0.0:80 | Public web UI |
| 8080          | 8080          | gateway | DMZ | 0.0.0.0:8080 | Public API |

### Management Port Mapping (Localhost Only)

| External Port | Internal Port | Service | Zone | Binding | Purpose |
|---------------|---------------|---------|------|---------|---------|
| 8848          | 8848          | nacos | Data | 127.0.0.1:8848 | Service registry UI |
| 9848          | 9848          | nacos | Data | 127.0.0.1:9848 | Nacos gRPC |
| 15672         | 15672         | rabbitmq | Data | 127.0.0.1:15672 | RabbitMQ mgmt UI |
| 5672          | 5672          | rabbitmq | Data | 127.0.0.1:5672 | AMQP protocol |
| 9000          | 9000          | minio | Data | 127.0.0.1:9000 | MinIO API |
| 9001          | 9001          | minio | Data | 127.0.0.1:9001 | MinIO console |

### Internal-Only Ports (No Exposure)

| Port | Service | Zone | Access |
|------|---------|------|--------|
| 8081 | auth-service | App | Gateway only |
| 8082 | book-service | App | Gateway only |
| 8083 | circulation-service | App | Gateway only |
| 8084 | reader-service | App | Gateway only |
| 8085 | system-service | App | Gateway only |
| 8086 | notification-service | App | Gateway only |
| 5432 | postgresql | Data | Services only |
| 6379 | redis | Data | Services only |
| 3306 | nacos-mysql | Data | Nacos only |

### Port Exposure Security Rationale

**Why 0.0.0.0 for DMZ ports (80, 8080)?**
- Public-facing services need internet access
- Protected by application-level security (authentication)
- Rate limiting and WAF rules applied
- SSL/TLS encryption (production)

**Why 127.0.0.1 for management ports?**
- Management interfaces are for operators only
- Prevents unauthorized external access
- Accessible via SSH tunneling for remote management
- Reduces attack surface

**Why no exposure for backend services?**
- Backend services should never be directly accessible
- All traffic must flow through authenticated gateway
- Prevents bypassing API gateway security
- Enforces centralized access control

---

## Firewall Configuration

### iptables Rules Overview

The GCRF system uses iptables to enforce network security at the host level:

```bash
# Default policies
iptables -P INPUT DROP       # Drop all incoming by default
iptables -P FORWARD ACCEPT   # Allow Docker forwarding
iptables -P OUTPUT ACCEPT    # Allow all outgoing

# Allow established connections
iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# Allow loopback
iptables -A INPUT -i lo -j ACCEPT

# Allow SSH (for remote management)
iptables -A INPUT -p tcp --dport 22 -j ACCEPT

# Allow public web services
iptables -A INPUT -p tcp --dport 80 -j ACCEPT
iptables -A INPUT -p tcp --dport 8080 -j ACCEPT

# Allow Docker internal networks
iptables -A INPUT -i docker0 -j ACCEPT
iptables -A INPUT -i br-+ -j ACCEPT

# Restrict management ports to localhost
iptables -A INPUT -p tcp --dport 8848 -s 127.0.0.1 -j ACCEPT
iptables -A INPUT -p tcp --dport 8848 ! -s 127.0.0.1 -j DROP
iptables -A INPUT -p tcp --dport 15672 -s 127.0.0.1 -j ACCEPT
iptables -A INPUT -p tcp --dport 15672 ! -s 127.0.0.1 -j DROP
iptables -A INPUT -p tcp --dport 9001 -s 127.0.0.1 -j ACCEPT
iptables -A INPUT -p tcp --dport 9001 ! -s 127.0.0.1 -j DROP

# Block direct access to backend services
iptables -A INPUT -p tcp --dport 8081:8090 -j DROP
iptables -A INPUT -p tcp --dport 5432 -j DROP
iptables -A INPUT -p tcp --dport 6379 -j DROP
iptables -A INPUT -p tcp --dport 3306 -j DROP

# Log dropped packets (optional)
iptables -A INPUT -j LOG --log-prefix "GCRF-DROPPED: " --log-level 4
```

### Firewall Script Usage

The `configure-firewall.sh` script manages firewall rules:

```bash
# Enable firewall rules
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/deployment/scripts
sudo ./configure-firewall.sh enable

# Check current rules
sudo ./configure-firewall.sh status

# Disable firewall rules (testing only)
sudo ./configure-firewall.sh disable

# Test firewall (dry-run)
sudo ./configure-firewall.sh test
```

### Persistence Across Reboots

**Ubuntu/Debian**:
```bash
# Install iptables-persistent
sudo apt-get install iptables-persistent

# Save current rules
sudo netfilter-persistent save

# Rules will be loaded on boot
```

**RHEL/CentOS**:
```bash
# Save current rules
sudo service iptables save

# Enable on boot
sudo systemctl enable iptables
```

**macOS** (Development):
```bash
# macOS uses pf (Packet Filter) instead of iptables
# For development, Docker Desktop handles port mapping
# Firewall configuration is optional in development
```

---

## Network Isolation Rules

### Docker Network Policies

#### 1. Infrastructure Network Isolation

```yaml
# All infrastructure services are isolated in gcrf-infrastructure-network
# Only services explicitly connected can communicate
networks:
  gcrf-infrastructure-network:
    internal: false  # Allow outbound internet (updates)
```

**Enforcement**:
- Services not in this network cannot access infrastructure
- PostgreSQL, Redis, RabbitMQ, MinIO, Nacos are isolated
- Application services must explicitly join to access

#### 2. Frontend Network Isolation

```yaml
# Public-facing services in separate network
networks:
  gcrf-frontend-network:
    internal: false  # Exposed to host
```

**Enforcement**:
- Gateway bridges both networks (controlled access)
- Web admin only in frontend network
- Cannot directly access backend services

#### 3. Service Communication Rules

**Gateway Service**:
```yaml
networks:
  - gcrf-backend-network      # Access to backend services
  - gcrf-frontend-network     # Access from web admin
```

**Backend Services** (Auth, Book, etc.):
```yaml
networks:
  - gcrf-backend-network      # Only backend network
# NO frontend network access
```

**Infrastructure Services**:
```yaml
networks:
  - gcrf-infrastructure-network  # Only infrastructure network
# NO frontend network access
```

### Container Security Constraints

#### 1. Non-Root User

All application containers run as non-root:

```dockerfile
# Dockerfile best practice
RUN addgroup --system --gid 1001 appuser && \
    adduser --system --uid 1001 --ingroup appuser appuser

USER appuser
```

#### 2. Read-Only Filesystem

```yaml
services:
  gcrf-auth-service:
    read_only: true
    tmpfs:
      - /tmp
      - /app/logs
```

#### 3. No New Privileges

```yaml
services:
  gcrf-auth-service:
    security_opt:
      - no-new-privileges:true
```

#### 4. Resource Limits

```yaml
deploy:
  resources:
    limits:
      cpus: '1.0'
      memory: 1G
    reservations:
      cpus: '0.5'
      memory: 512M
```

---

## Security Best Practices

### 1. Principle of Least Privilege

**Network Access**:
- Services only connect to networks they need
- No direct database access from DMZ
- Gateway is the only bridge between zones

**User Permissions**:
- Separate database users per service
- Read-only users for reporting
- No shared credentials

**File System**:
- Configuration files mounted read-only
- Minimal write permissions
- Secrets in environment variables, not files

### 2. Defense in Depth

**Multiple Security Layers**:
1. Firewall (iptables) - Host level
2. Docker networks - Container level
3. Application authentication - Service level
4. Database permissions - Data level
5. Audit logging - Monitoring level

### 3. Encryption

**In Transit**:
- SSL/TLS for public endpoints (production)
- PostgreSQL SSL connections (production)
- Redis AUTH + TLS (production)
- Service mesh mTLS (future)

**At Rest**:
- PostgreSQL data encryption
- MinIO server-side encryption
- Encrypted volumes (production)
- Encrypted backups

### 4. Regular Security Audits

**Weekly**:
- Review firewall logs
- Check for failed authentication attempts
- Monitor unusual traffic patterns

**Monthly**:
- Security patch updates
- Certificate expiration checks
- Access control review

**Quarterly**:
- Penetration testing
- Vulnerability scanning
- Security policy review

### 5. Incident Response

**Detection**:
- Automated alerting for security events
- Log aggregation and analysis
- Anomaly detection

**Response**:
- Isolation procedures
- Incident documentation
- Communication plan
- Recovery procedures

---

## Network Monitoring

### 1. Traffic Analysis

**Tools**:
```bash
# Monitor Docker network traffic
docker network inspect gcrf-infrastructure-network

# View container network stats
docker stats --format "table {{.Name}}\t{{.NetIO}}"

# Capture network packets (troubleshooting)
sudo tcpdump -i br-$(docker network inspect gcrf-infrastructure-network -f '{{.Id}}' | cut -c 1-12) -w /tmp/capture.pcap
```

**Metrics to Monitor**:
- Network throughput (MB/s)
- Connection counts
- Failed connection attempts
- DNS query patterns
- Unusual traffic spikes

### 2. Service Health Monitoring

**Health Check Endpoints**:
```bash
# Check gateway health
curl http://localhost:8080/actuator/health

# Check auth service (from gateway container)
docker exec gcrf-gateway-service curl http://gcrf-auth-service:8081/actuator/health

# Check infrastructure services
curl http://localhost:8848/nacos/actuator/health
curl http://localhost:15672/api/health/checks/alarms
```

**Monitoring Tools**:
- Prometheus (metrics collection)
- Grafana (visualization)
- ELK Stack (log aggregation)
- Jaeger (distributed tracing)

### 3. Security Event Monitoring

**What to Monitor**:
- Failed authentication attempts (> 5 per minute)
- Port scans (rapid connection attempts)
- Unusual outbound connections
- Privilege escalation attempts
- File access violations

**Log Aggregation**:
```bash
# Container logs
docker logs gcrf-gateway-service --tail 100 --follow

# System logs
journalctl -u docker -f

# Firewall logs
tail -f /var/log/syslog | grep "GCRF-DROPPED"
```

---

## Troubleshooting Guide

### Common Network Issues

#### 1. Cannot Access Gateway from Browser

**Symptoms**:
- Browser shows "Connection refused" or timeout
- `curl http://localhost:8080` fails

**Diagnosis**:
```bash
# Check if gateway container is running
docker ps | grep gcrf-gateway-service

# Check port mapping
docker port gcrf-gateway-service

# Check gateway logs
docker logs gcrf-gateway-service --tail 50

# Verify firewall allows port 8080
sudo iptables -L INPUT -n | grep 8080
```

**Solutions**:
1. Ensure container is running: `docker-compose -f docker-compose.services.yml up -d`
2. Check health: `curl http://localhost:8080/actuator/health`
3. Verify port binding: Should be `0.0.0.0:8080->8080/tcp`
4. Check firewall: `sudo ./configure-firewall.sh status`

---

#### 2. Backend Service Cannot Connect to Database

**Symptoms**:
- Service logs show "Connection refused" to PostgreSQL
- Health check fails with database error

**Diagnosis**:
```bash
# Check if PostgreSQL is running
docker ps | grep gcrf-postgres-primary

# Test database connection from service container
docker exec gcrf-auth-service pg_isready -h postgres-primary -p 5432

# Check if service is in correct network
docker inspect gcrf-auth-service | grep Networks -A 10

# Verify DNS resolution
docker exec gcrf-auth-service nslookup postgres-primary
```

**Solutions**:
1. Ensure PostgreSQL is healthy: `docker inspect gcrf-postgres-primary | grep Health -A 5`
2. Verify network membership: Service must be in `gcrf-infrastructure-network`
3. Check credentials: Verify DB_PASSWORD in `.env`
4. Test direct connection: `docker exec gcrf-postgres-primary psql -U postgres -c "SELECT 1"`

---

#### 3. Gateway Cannot Discover Services in Nacos

**Symptoms**:
- Gateway returns 503 Service Unavailable
- Nacos UI shows no registered services

**Diagnosis**:
```bash
# Check Nacos health
curl http://localhost:8848/nacos/actuator/health

# Check Nacos service list
curl -X GET 'http://localhost:8848/nacos/v1/ns/instance/list?serviceName=auth-service' \
  -u nacos:nacos

# Check service logs for registration errors
docker logs gcrf-auth-service | grep "register"

# Verify network connectivity
docker exec gcrf-gateway-service ping -c 3 gcrf-nacos
```

**Solutions**:
1. Verify Nacos is running and healthy
2. Check service configuration: `NACOS_SERVER_ADDR=nacos:8848`
3. Verify authentication: `NACOS_USERNAME` and `NACOS_PASSWORD` in `.env`
4. Restart service to re-register: `docker restart gcrf-auth-service`
5. Check Nacos logs: `docker logs gcrf-nacos --tail 50`

---

#### 4. Management UI Not Accessible from Localhost

**Symptoms**:
- Cannot access Nacos UI at http://localhost:8848/nacos
- RabbitMQ management at http://localhost:15672 not loading

**Diagnosis**:
```bash
# Check port bindings
docker ps | grep gcrf-nacos

# Verify listening ports
sudo lsof -i :8848
sudo lsof -i :15672

# Test from localhost
curl -v http://localhost:8848/nacos/

# Check firewall rules
sudo iptables -L INPUT -n | grep 8848
```

**Solutions**:
1. Verify port mapping in docker-compose.yml: `127.0.0.1:8848:8848`
2. Ensure container is running: `docker ps`
3. Check health: `docker inspect gcrf-nacos | grep Health -A 5`
4. Restart container: `docker restart gcrf-nacos`
5. For remote access, use SSH tunnel: `ssh -L 8848:localhost:8848 user@server`

---

#### 5. External Clients Cannot Access Web Admin

**Symptoms**:
- Local access works (localhost:80)
- Remote clients cannot connect
- Firewall may be blocking

**Diagnosis**:
```bash
# Check if port 80 is exposed
docker port gcrf-web-admin

# Verify firewall allows port 80
sudo iptables -L INPUT -n | grep "dpt:80"

# Test from external machine
curl -v http://<server-ip>:80

# Check nginx logs
docker logs gcrf-web-admin --tail 50
```

**Solutions**:
1. Verify port binding: Should be `0.0.0.0:80->80/tcp`
2. Enable firewall rule: `sudo ./configure-firewall.sh enable`
3. Check cloud security groups (AWS/Azure/GCP)
4. Verify nginx configuration
5. Test with telnet: `telnet <server-ip> 80`

---

#### 6. Service-to-Service Communication Timeout

**Symptoms**:
- Gateway can discover service in Nacos
- Requests timeout when calling backend service
- No response within 30 seconds

**Diagnosis**:
```bash
# Test direct connection
docker exec gcrf-gateway-service curl -v http://gcrf-auth-service:8081/actuator/health

# Check service health
docker exec gcrf-auth-service curl http://localhost:8081/actuator/health

# Verify DNS resolution
docker exec gcrf-gateway-service nslookup gcrf-auth-service

# Check network latency
docker exec gcrf-gateway-service ping -c 5 gcrf-auth-service
```

**Solutions**:
1. Verify both services in same network
2. Check service health independently
3. Increase timeout in gateway configuration
4. Review service logs for errors
5. Check resource constraints: `docker stats`

---

### Network Diagnostic Commands

#### Docker Network Commands

```bash
# List all networks
docker network ls

# Inspect network details
docker network inspect gcrf-infrastructure-network

# See which containers are connected
docker network inspect gcrf-infrastructure-network -f '{{range .Containers}}{{.Name}} {{end}}'

# Test connectivity between containers
docker exec gcrf-gateway-service ping -c 3 gcrf-auth-service
docker exec gcrf-gateway-service curl http://gcrf-auth-service:8081/actuator/health

# View network stats
docker stats --format "table {{.Name}}\t{{.NetIO}}\t{{.BlockIO}}"
```

#### Port and Connection Commands

```bash
# Check listening ports on host
sudo lsof -i -P -n | grep LISTEN

# Check specific port
sudo lsof -i :8080

# Netstat alternative
sudo netstat -tulpn | grep :8080

# Test port connectivity
telnet localhost 8080
nc -zv localhost 8080

# Scan ports (nmap)
nmap -p 8080-8090 localhost
```

#### DNS Resolution Commands

```bash
# Resolve container name
docker exec gcrf-gateway-service nslookup gcrf-auth-service

# Test DNS from container
docker exec gcrf-gateway-service getent hosts postgres-primary

# Check Docker DNS
docker exec gcrf-gateway-service cat /etc/resolv.conf
```

#### Packet Capture Commands

```bash
# Capture traffic on Docker bridge
sudo tcpdump -i docker0 -n

# Capture specific port
sudo tcpdump -i any port 8080 -n

# Capture between containers
docker run --rm --net=container:gcrf-gateway-service \
  nicolaka/netshoot tcpdump -i any -n

# Save to file for Wireshark analysis
sudo tcpdump -i docker0 -w /tmp/docker-traffic.pcap
```

---

## Compliance & Audit

### Security Compliance Checklist

- [ ] All public ports (80, 8080) have rate limiting
- [ ] Management ports (8848, 15672, 9001) restricted to localhost
- [ ] Backend services (8081-8090) have no external exposure
- [ ] Database ports (5432, 6379, 3306) are internal only
- [ ] Firewall rules are enabled and tested
- [ ] All services use strong passwords (min 16 characters)
- [ ] JWT secrets are cryptographically random
- [ ] SSL/TLS enabled for production
- [ ] Container images are scanned for vulnerabilities
- [ ] No root processes in containers
- [ ] Resource limits configured for all services
- [ ] Health checks configured for all services
- [ ] Logs are being collected and rotated
- [ ] Backup procedures are documented and tested
- [ ] Incident response plan is documented

### Audit Procedures

#### Weekly Audit

```bash
# 1. Check firewall status
sudo ./configure-firewall.sh status

# 2. Verify port exposure
docker ps --format "table {{.Names}}\t{{.Ports}}"

# 3. Check for failed login attempts
docker logs gcrf-auth-service | grep "Failed login" | wc -l

# 4. Review security events
docker logs gcrf-gateway-service | grep -i "error\|denied\|unauthorized"

# 5. Verify all services are healthy
docker ps --filter "health=unhealthy"
```

#### Monthly Audit

```bash
# 1. Security updates
docker images | grep gcrf-library | while read line; do
  docker pull $(echo $line | awk '{print $1":"$2}')
done

# 2. Certificate expiration (if using HTTPS)
echo | openssl s_client -connect localhost:443 2>/dev/null | \
  openssl x509 -noout -dates

# 3. Password rotation
# Update .env with new passwords and restart services

# 4. Review user access
# Check who has SSH access, Docker group membership

# 5. Backup verification
./scripts/backup-volumes.sh
./scripts/restore-volumes.sh --dry-run
```

#### Quarterly Audit

```bash
# 1. Penetration testing
# Use tools like OWASP ZAP, Nmap, Metasploit

# 2. Vulnerability scanning
docker scan gcrf-library/gateway-service:latest
docker scan gcrf-library/auth-service:latest

# 3. Log analysis
# Review 3 months of logs for patterns

# 4. Compliance review
# Ensure all security controls are still effective

# 5. Disaster recovery test
# Full system restore from backup
```

### Audit Logging

**What to Log**:
- Authentication attempts (success and failure)
- Authorization decisions
- Network connections
- Configuration changes
- Data access
- Administrative actions

**Log Retention**:
- Application logs: 30 days
- Security logs: 90 days
- Audit logs: 1 year
- Backup logs: 1 year

**Log Protection**:
- Centralized logging (ELK stack)
- Immutable log storage
- Encrypted log transmission
- Access controls on logs

---

## Security Testing Procedures

### Automated Testing

Run the network security test script:

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/deployment/scripts
./test-network-security.sh
```

This will validate:
1. External access to public ports (80, 8080)
2. Backend service isolation (8081-8090 not accessible)
3. Internal service communication
4. Database connectivity from services
5. Management port restrictions

### Manual Testing

#### Test 1: Verify Public Access

```bash
# Should succeed (200 OK)
curl -v http://localhost:80
curl -v http://localhost:8080/actuator/health
```

#### Test 2: Verify Backend Isolation

```bash
# Should fail (connection refused or timeout)
curl -v --max-time 5 http://localhost:8081/actuator/health
curl -v --max-time 5 http://localhost:8082/actuator/health
```

#### Test 3: Verify Internal Communication

```bash
# Should succeed
docker exec gcrf-gateway-service curl -v http://gcrf-auth-service:8081/actuator/health
```

#### Test 4: Verify Management Port Restriction

```bash
# From localhost - should succeed
curl -v http://localhost:8848/nacos/

# From external IP - should fail (if firewall enabled)
curl -v http://<external-ip>:8848/nacos/
```

#### Test 5: Verify Database Isolation

```bash
# From host - should fail
psql -h localhost -p 5432 -U postgres

# From service - should succeed
docker exec gcrf-auth-service psql -h postgres-primary -p 5432 -U postgres -c "SELECT 1"
```

---

## Additional Resources

### Related Documentation

- [Environment Variables Guide](./ENVIRONMENT_VARIABLES.md)
- [Nacos Configuration Guide](./NACOS_CONFIGURATION.md)
- [Volume Management Guide](./VOLUME_MANAGEMENT.md)
- [Service Integration Example](./SERVICE_INTEGRATION_EXAMPLE.md)

### External References

- [Docker Network Security](https://docs.docker.com/network/security/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [CIS Docker Benchmark](https://www.cisecurity.org/benchmark/docker)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)

### Tools & Utilities

- [iptables Persistent](https://packages.ubuntu.com/focal/iptables-persistent)
- [Docker Security Scanning](https://docs.docker.com/engine/scan/)
- [OWASP ZAP](https://www.zaproxy.org/)
- [Nmap](https://nmap.org/)
- [Wireshark](https://www.wireshark.org/)

---

## Change History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0.0 | 2025-11-01 | Infrastructure Team | Initial network security documentation |

---

**Document Status**: Production Ready
**Review Cycle**: Quarterly
**Next Review**: 2026-02-01

---

**For questions or security incidents, contact**: infrastructure-team@gcrf.com
