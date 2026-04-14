# GCRF Library Management System - Deployment Documentation

**Project**: 国创睿峰智能图书馆管理系统 (GCRF Intelligent Library Management System)
**Version**: 1.0.0
**Last Updated**: 2025-12-01

---

## Overview

This directory contains comprehensive deployment documentation for the GCRF Library Management System. The documentation covers everything from initial installation to daily operations and troubleshooting.

## Documentation Structure

### 1. Production Deployment Guide
**File**: `production-deployment-guide.md`

Comprehensive guide covering production deployment requirements and architecture.

**Contents**:
- Server hardware and software requirements
- Network architecture and security zones
- Deployment topology diagrams
- Complete port planning and firewall rules
- Security considerations and best practices
- High availability setup (load balancers, database replication, Redis Sentinel)
- Disaster recovery planning

**Who should read**: System architects, DevOps engineers, IT managers

### 2. Installation Steps
**File**: `installation-steps.md`

Step-by-step installation instructions for production deployment.

**Contents**:
- Pre-installation checklist
- System preparation and tuning
- Docker and Docker Compose installation
- Infrastructure services deployment (PostgreSQL, Redis, RabbitMQ, Nacos, MinIO)
- Application services deployment
- Monitoring services setup (Prometheus, Grafana, Loki)
- Nginx reverse proxy configuration
- SSL/TLS certificate setup
- Post-installation verification

**Who should read**: System administrators, deployment engineers

### 3. Configuration Checklist
**File**: `configuration-checklist.md`

Complete configuration reference for all system components.

**Contents**:
- Environment variables master list
- Database configuration (PostgreSQL)
- Redis configuration
- RabbitMQ configuration and queue definitions
- Nacos service registry configuration
- JWT security configuration (HS256/RS256)
- Email/SMTP configuration
- MinIO object storage configuration
- Monitoring configuration (Prometheus, Grafana)
- Security configuration and secrets management

**Who should read**: System administrators, DevOps engineers, security team

### 4. Operations Manual
**File**: `operations-manual.md`

Day-to-day operational procedures and management tasks.

**Contents**:
- Daily health check routines
- Service management (start, stop, restart, scale)
- Log management and analysis
- Backup and recovery procedures
- Monitoring and alerting setup
- Performance tuning (JVM, database, Redis, network)
- Security operations and auditing
- Scaling operations (vertical and horizontal)
- Maintenance windows planning
- Emergency procedures

**Who should read**: Operations team, DevOps engineers, on-call engineers

### 5. Troubleshooting Guide
**File**: `troubleshooting-guide.md`

Common problems and their solutions.

**Contents**:
- Quick diagnostic commands
- Service startup issues
- Database connection problems
- Authentication and authorization issues
- Performance problems (CPU, memory, slow queries)
- Network and connectivity issues
- Data inconsistency issues
- Container and Docker issues
- Monitoring and logging issues
- Common error messages reference

**Who should read**: Operations team, support engineers, developers

---

## Deployment Scripts

The following scripts are available in `/deployment/scripts/` to automate deployment and operational tasks:

### Production Deployment

#### `deploy-production.sh`
**Purpose**: One-click production deployment script

**Usage**:
```bash
./deploy-production.sh [options]

Options:
  --skip-infrastructure  Skip infrastructure deployment
  --skip-monitoring      Skip monitoring deployment
  --force-recreate       Force recreate all containers
  --dry-run             Show what would be deployed
```

**Features**:
- Prerequisites checking
- Pre-deployment backup
- Infrastructure services deployment
- Application services deployment
- Monitoring services deployment
- Post-deployment verification
- Comprehensive deployment summary

### Rollback

#### `rollback.sh`
**Purpose**: Roll back to a previous backup

**Usage**:
```bash
./rollback.sh [backup-directory]

# Interactive mode (prompts for backup selection)
./rollback.sh

# Specify backup directly
./rollback.sh /backups/pre-deployment-20251201_140000
```

**Features**:
- Lists available backups
- Interactive backup selection
- Pre-rollback backup creation
- Database restoration
- Configuration restoration
- Service restart
- Verification

### Health Check

#### `health-check.sh`
**Purpose**: Comprehensive system health check

**Usage**:
```bash
./health-check.sh [options]

Options:
  --verbose      Show detailed output
  --json         Output results in JSON
  --nagios       Nagios-compatible mode
  --email        Send results via email
```

**Checks**:
- System resources (CPU, memory, disk)
- Docker health
- Infrastructure services (PostgreSQL, Redis, RabbitMQ, Nacos, MinIO, Elasticsearch)
- Application services (Gateway, Auth, Book, Reader, etc.)
- Network connectivity
- Monitoring services (Prometheus, Grafana)
- Backup status

**Exit Codes**:
- 0: All checks passed
- 1: Warnings detected
- 2: Critical failures detected

### Database Backup

#### `backup-database.sh`
**Purpose**: Automated database backup with retention

**Usage**:
```bash
./backup-database.sh [options]

Options:
  --full             Full backup (default)
  --database <name>  Backup specific database
  --retention <days> Retention period (default: 30)
  --upload           Upload to remote storage
  --compress         Compress backup (default)
  --no-compress      Skip compression
  --verify           Verify backup integrity
```

**Features**:
- Full or single database backup
- Automatic compression (gzip)
- Backup verification
- Remote upload (S3/MinIO)
- Automatic retention policy
- Detailed backup reports

**Scheduled Execution** (cron):
```bash
# Daily backup at 2 AM
0 2 * * * /opt/gcrf-library/deployment/scripts/backup-database.sh --full --upload --verify
```

---

## Quick Start Guide

### Initial Production Deployment

1. **Prepare the environment**:
```bash
# Clone repository
cd /opt/gcrf-library

# Create .env file from template
cp deployment/.env.infrastructure.example deployment/.env

# Edit .env with production values
vim deployment/.env
```

2. **Run deployment script**:
```bash
cd deployment/scripts
./deploy-production.sh
```

3. **Verify deployment**:
```bash
./health-check.sh --verbose
```

4. **Configure monitoring**:
- Access Grafana: http://localhost:3000
- Login (admin/admin, change on first login)
- Import dashboards
- Configure alerts in AlertManager

5. **Set up automated backups**:
```bash
# Add to crontab
sudo crontab -e

# Add daily backup at 2 AM
0 2 * * * /opt/gcrf-library/deployment/scripts/backup-database.sh --full --upload --verify >> /var/log/gcrf-backup.log 2>&1
```

6. **Configure Nginx reverse proxy**:
```bash
# Edit Nginx configuration
sudo vim /etc/nginx/sites-available/gcrf-library

# Test configuration
sudo nginx -t

# Reload Nginx
sudo systemctl reload nginx
```

7. **Set up SSL certificates**:
```bash
# Using Let's Encrypt
sudo certbot --nginx -d library.gcrf.com -d www.library.gcrf.com
```

### Daily Operations

**Morning Routine**:
```bash
# Run health check
./health-check.sh --verbose

# Check service logs
docker-compose -f /opt/gcrf-library/deployment/docker-compose.services.yml logs --tail 100

# Check for alerts
curl http://localhost:9093/api/v1/alerts
```

**Restart a Service**:
```bash
cd /opt/gcrf-library/deployment
docker-compose -f docker-compose.services.yml restart gcrf-book-service
```

**View Logs**:
```bash
# Application logs
docker logs -f gcrf-gateway-service

# Nginx logs
tail -f /var/log/nginx/gcrf-library-access.log
```

**Check Resource Usage**:
```bash
# Container resources
docker stats

# System resources
htop
df -h
free -h
```

### Maintenance

**Weekly Maintenance**:
- Review monitoring dashboards
- Check backup status
- Review security logs
- Update containers (if needed)
- Clean up old logs

**Monthly Maintenance**:
- Review and optimize database
- Check and renew SSL certificates
- Review and update security policies
- Capacity planning review
- Disaster recovery drill

---

## Support and Resources

### Internal Resources

- **Architecture Documentation**: `/docs/architecture/architect.md`
- **API Documentation**: `/docs/api/`
- **Development Guide**: `/CLAUDE.md`

### External Resources

- **Spring Boot**: https://docs.spring.io/spring-boot/
- **Docker**: https://docs.docker.com/
- **PostgreSQL**: https://www.postgresql.org/docs/
- **Prometheus**: https://prometheus.io/docs/
- **Grafana**: https://grafana.com/docs/

### Support Contacts

| Issue Type | Contact | Response Time |
|------------|---------|---------------|
| Critical Production Issue | ops@gcrf-library.com | 15 minutes |
| Non-Critical Issue | support@gcrf-library.com | 4 hours |
| Security Issue | security@gcrf-library.com | 30 minutes |
| General Inquiries | info@gcrf-library.com | 1 business day |

### Emergency Contacts

- **On-Call Engineer**: xxx-xxxx-xxxx
- **System Administrator**: xxx-xxxx-xxxx
- **IT Manager**: xxx-xxxx-xxxx

---

## Best Practices

### Security
1. Change all default passwords before deployment
2. Use strong passwords (16+ characters)
3. Enable SSL/TLS for all external-facing services
4. Implement firewall rules between network zones
5. Regular security audits and vulnerability scans
6. Keep all software up to date
7. Implement least privilege access
8. Regular security training for operations team

### Performance
1. Monitor key metrics continuously
2. Set up alerts for threshold violations
3. Regular performance testing
4. Database query optimization
5. Proper resource allocation
6. Implement caching strategies
7. Regular capacity planning

### Reliability
1. Implement high availability for critical services
2. Regular backup verification
3. Disaster recovery drills (quarterly)
4. Monitoring and alerting
5. Incident response procedures
6. Change management process
7. Regular system health checks

### Operations
1. Document all changes
2. Use version control for configurations
3. Automated deployment where possible
4. Regular training for operations team
5. Post-mortem analysis for incidents
6. Continuous improvement mindset
7. Clear escalation procedures

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-12-01 | Initial production deployment documentation |

---

## Contributing

To update this documentation:

1. Make changes in the appropriate markdown file
2. Update the version number and date
3. Add entry to changelog
4. Submit for review
5. Update after approval

---

## License

Copyright © 2025 国创睿峰科技有限公司 (GCRF Technology Co., Ltd.)

This documentation is proprietary and confidential. Unauthorized reproduction or distribution is prohibited.

---

**Document Version**: 1.0.0
**Last Updated**: 2025-12-01
**Next Review Date**: 2026-03-01
**Maintained By**: DevOps Team
