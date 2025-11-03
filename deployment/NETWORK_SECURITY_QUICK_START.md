# Network Security Quick Start Guide

**GCRF Library Management System**

---

## Quick Commands

### Testing Network Security

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/deployment/scripts

# Run all security tests
./test-network-security.sh

# Verbose output with report
./test-network-security.sh --verbose --report security-audit-$(date +%Y%m%d).txt
```

### Configuring Firewall

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/deployment/scripts

# Test prerequisites (no changes)
sudo ./configure-firewall.sh test

# Enable firewall rules
sudo ./configure-firewall.sh enable

# Check status
sudo ./configure-firewall.sh status

# Disable rules (testing only)
sudo ./configure-firewall.sh disable
```

---

## Port Reference

**Public Access (0.0.0.0)**:
- Port 80: Web Admin UI
- Port 8080: API Gateway

**Localhost Only (127.0.0.1)**:
- Port 8848: Nacos UI → http://localhost:8848/nacos
- Port 15672: RabbitMQ Management → http://localhost:15672
- Port 9001: MinIO Console → http://localhost:9001

**Internal Only (No External Access)**:
- Ports 8081-8090: Backend microservices
- Port 5432: PostgreSQL
- Port 6379: Redis
- Port 3306: Nacos MySQL

---

## Network Architecture

```
Internet → Firewall → DMZ (80, 8080) → Application (8081-8090) → Data (DBs)
            iptables    Frontend Net      Backend Net             Infra Net
```

**3 Security Zones**:
1. **DMZ**: Public-facing (low trust)
2. **Application**: Business logic (medium trust)
3. **Data**: Infrastructure (high trust)

---

## Documentation

- **Full Guide**: `deployment/docs/NETWORK_SECURITY.md` (1,164 lines)
- **Network Diagram**: `deployment/docs/NETWORK_DIAGRAM.txt` (ASCII art)
- **Completion Summary**: `deployment/scripts/TASK3_NETWORK_SECURITY_COMPLETION.md`

---

## Remote Access to Management UIs

Management UIs are restricted to localhost. Use SSH tunneling:

```bash
# Nacos UI
ssh -L 8848:localhost:8848 user@server
# Then access: http://localhost:8848/nacos

# RabbitMQ Management
ssh -L 15672:localhost:15672 user@server
# Then access: http://localhost:15672

# MinIO Console
ssh -L 9001:localhost:9001 user@server
# Then access: http://localhost:9001
```

---

## Troubleshooting

### Cannot access gateway

```bash
# Check if running
docker ps | grep gcrf-gateway-service

# Check logs
docker logs gcrf-gateway-service --tail 50

# Test health
curl http://localhost:8080/actuator/health
```

### Firewall blocks everything

```bash
# Disable firewall temporarily
sudo ./configure-firewall.sh disable

# Test without firewall
./test-network-security.sh

# Re-enable
sudo ./configure-firewall.sh enable
```

### Management UI not accessible

```bash
# Verify port binding
docker port gcrf-nacos

# Should show: 127.0.0.1:8848->8848/tcp
# If shows 0.0.0.0, edit docker-compose.yml
```

---

## Security Checklist

Weekly:
- [ ] Run `./test-network-security.sh`
- [ ] Review failed login attempts
- [ ] Check firewall status

Monthly:
- [ ] Security updates
- [ ] Password rotation
- [ ] Certificate checks

Quarterly:
- [ ] Penetration testing
- [ ] Vulnerability scanning
- [ ] Compliance review

---

## Emergency Contact

For security incidents:
- Review: `deployment/docs/NETWORK_SECURITY.md` → "Incident Response"
- Email: infrastructure-team@gcrf.com

---

**Last Updated**: 2025-11-01
**Version**: 1.0.0
