# GCRF Volume Management Scripts - Complete Guide

**Version**: 1.0.0
**Last Updated**: 2025-11-01
**Status**: Production Ready

---

## Overview

This document provides comprehensive documentation for the GCRF Library Management System's volume management and backup scripts. These scripts handle backup, restoration, cleanup, and monitoring of all Docker volumes and data.

### Scripts Included

1. **backup-volumes.sh** - Comprehensive backup solution
2. **restore-volumes.sh** - Volume restoration with validation
3. **cleanup-volumes.sh** - Retention policy and cleanup
4. **volume-status.sh** - Monitoring and alerting

---

## Table of Contents

- [Quick Start](#quick-start)
- [Backup Strategy](#backup-strategy)
- [Script Details](#script-details)
  - [Backup Script](#backup-script)
  - [Restore Script](#restore-script)
  - [Cleanup Script](#cleanup-script)
  - [Status Script](#status-script)
- [Automation](#automation)
- [Monitoring Integration](#monitoring-integration)
- [Troubleshooting](#troubleshooting)
- [Best Practices](#best-practices)

---

## Quick Start

### Daily Backup
```bash
# Create full backup with verification
./backup-volumes.sh --verify

# Backup only critical volumes (PostgreSQL, Nacos MySQL)
./backup-volumes.sh --type critical

# Backup with Slack notification
./backup-volumes.sh --slack https://hooks.slack.com/services/YOUR/WEBHOOK/URL
```

### Check Status
```bash
# View comprehensive status
./volume-status.sh

# JSON output for monitoring
./volume-status.sh --json

# Check only alerts
./volume-status.sh --alerts
```

### Restore from Backup
```bash
# Interactive restore (with confirmation)
./restore-volumes.sh --dir /data/backups/20251101_020000

# Force restore without prompts
./restore-volumes.sh --dir /data/backups/20251101_020000 --force

# Restore specific service
./restore-volumes.sh --dir /data/backups/20251101_020000 --target postgres
```

### Cleanup Old Backups
```bash
# Dry-run to preview deletions
./cleanup-volumes.sh --dry-run

# Clean backups older than 30 days
./cleanup-volumes.sh --retention 30

# Clean logs and prune volumes
./cleanup-volumes.sh --logs 7 --prune
```

---

## Backup Strategy

### Volume Classification

#### Critical Volumes (Backup before any operation)
- **gcrf-postgres-primary-data** - Primary database
- **gcrf-nacos-mysql-data** - Nacos configuration database

#### Important Volumes (Daily backup)
- **gcrf-redis-master-data** - Cache and session data
- **gcrf-rabbitmq-data** - Message queue data
- **gcrf-minio-data** - Object storage

#### Optional Volumes (Weekly backup)
- Application logs
- Temporary data

### Retention Policy

The cleanup script implements a tiered retention policy:

| Backup Type | Retention Period | When Created |
|-------------|-----------------|--------------|
| **Daily** | 7 days | Every backup |
| **Weekly** | 28 days (4 weeks) | Sunday backups |
| **Monthly** | 365 days (12 months) | 1st of month backups |

**Example Timeline**:
```
Day 1-7:    All daily backups retained
Day 8-28:   Only Sunday (weekly) backups retained
Day 29-365: Only 1st of month (monthly) backups retained
Day 365+:   Deleted
```

### Backup Components

Each backup includes:

1. **PostgreSQL**
   - Full database dump via `pg_dumpall`
   - Includes all databases, roles, tablespaces
   - Compressed with gzip

2. **Redis**
   - RDB snapshot via `BGSAVE`
   - Includes all keys and data
   - Binary format for fast restore

3. **Nacos MySQL**
   - MySQL dump of `nacos_config` database
   - Includes configuration and service registry
   - Compressed with gzip

4. **RabbitMQ**
   - Complete volume tar archive
   - Includes queues, exchanges, messages
   - Compressed with gzip

5. **MinIO**
   - Complete volume tar archive
   - Includes all buckets and objects
   - Compressed with gzip

6. **Manifest Files**
   - `MANIFEST.sha256` - SHA256 checksums
   - `BACKUP_INFO.txt` - Backup metadata

---

## Script Details

### Backup Script

**File**: `backup-volumes.sh`

#### Features
- Supports selective backup (all/critical/important)
- Automatic checksum generation
- Optional backup verification
- Remote upload to MinIO/S3
- Slack/Email notifications
- Parallel backup execution for speed

#### Usage Examples

```bash
# Full backup with all features
./backup-volumes.sh \
  --dir /data/backups/manual-backup \
  --verify \
  --slack https://hooks.slack.com/services/XXX \
  --remote minio/gcrf-backups

# Critical volumes only (fast, for pre-operation backup)
./backup-volumes.sh --type critical

# Custom backup directory
./backup-volumes.sh --dir /mnt/backup-drive/gcrf/$(date +%Y%m%d)
```

#### Backup Process

1. **Preflight Checks**
   - Verify Docker is running
   - Check container availability
   - Validate backup directory

2. **Data Backup**
   - PostgreSQL: Live dump via `pg_dumpall`
   - Redis: `BGSAVE` then copy RDB file
   - Nacos MySQL: `mysqldump` with single transaction
   - RabbitMQ/MinIO: Volume tar archives

3. **Post-Backup**
   - Generate SHA256 checksums
   - Create backup manifest
   - Verify integrity (if `--verify`)
   - Upload to remote (if `--remote`)
   - Send notifications (if configured)

#### Exit Codes

| Code | Meaning | Action |
|------|---------|--------|
| 0 | Success | Continue operations |
| 1 | General error | Review logs |
| 2 | Docker not running | Start Docker |
| 3 | Container not running | Start containers |
| 4 | Backup failed | Check disk space, permissions |
| 5 | Verification failed | Re-run backup |

---

### Restore Script

**File**: `restore-volumes.sh`

#### Features
- Selective restoration by service
- Automatic service stop/start
- Backup integrity verification
- Post-restore health checks
- Confirmation prompts for safety

#### Usage Examples

```bash
# Full restore with confirmation
./restore-volumes.sh --dir /data/backups/20251101_020000

# Force restore without prompts (automation)
./restore-volumes.sh --dir /data/backups/20251101_020000 --force

# Restore only PostgreSQL
./restore-volumes.sh --dir /data/backups/20251101_020000 --target postgres

# Verify backup before restore
./restore-volumes.sh --dir /data/backups/20251101_020000 --verify
```

#### Restoration Process

1. **Pre-Restore**
   - Validate backup directory
   - Verify backup integrity (if `--verify`)
   - Display backup information
   - Request user confirmation

2. **Service Shutdown**
   - Stop dependent services first (Nacos)
   - Stop target services
   - Wait for graceful shutdown

3. **Data Restoration**
   - PostgreSQL: Import via `psql`
   - Redis: Copy RDB file to volume
   - Nacos MySQL: Import via `mysql`
   - RabbitMQ/MinIO: Extract tar archives

4. **Service Startup**
   - Start services in dependency order
   - Wait for health checks to pass
   - Verify restoration success

5. **Post-Restore Verification**
   - Check PostgreSQL connectivity
   - Verify Redis ping
   - Test Nacos MySQL connection

#### Safety Features

- **Confirmation Required**: User must type "yes" to proceed
- **Service Management**: Automatic stop/start
- **Rollback Support**: Keep current data before restore (manual)
- **Health Checks**: Verify services after restore

---

### Cleanup Script

**File**: `cleanup-volumes.sh`

#### Features
- Tiered retention policy (daily/weekly/monthly)
- Dry-run mode for safety
- Log cleanup
- Docker volume pruning
- Size calculations and reporting

#### Usage Examples

```bash
# Preview what would be deleted
./cleanup-volumes.sh --dry-run

# Clean with default 30-day retention
./cleanup-volumes.sh

# Custom retention period
./cleanup-volumes.sh --retention 60

# Clean logs older than 7 days
./cleanup-volumes.sh --logs 7

# Full cleanup: backups, logs, and volumes
./cleanup-volumes.sh --retention 30 --logs 7 --prune --force
```

#### Cleanup Process

1. **Backup Cleanup**
   - Scan backup directory
   - Classify backups (daily/weekly/monthly)
   - Apply retention policy
   - Calculate freed space
   - Delete old backups (or report in dry-run)

2. **Log Cleanup** (if `--logs`)
   - Scan log directories
   - Find logs older than retention
   - Delete or report

3. **Volume Pruning** (if `--prune`)
   - List unused Docker volumes
   - Show volumes to be pruned
   - Request confirmation (unless `--force`)
   - Execute `docker volume prune`

#### Retention Logic

```bash
# Pseudo-code for retention
if is_monthly_backup(date):
    retention = 365 days
elif is_weekly_backup(date):
    retention = 28 days
else:  # daily backup
    retention = 7 days

if backup_age > retention:
    delete_backup()
```

---

### Status Script

**File**: `volume-status.sh`

#### Features
- Docker volume monitoring
- Disk space tracking with thresholds
- Backup status and age tracking
- Container health checks
- JSON output for integration
- Alert-only mode for automation

#### Usage Examples

```bash
# Full status report
./volume-status.sh

# Custom thresholds
./volume-status.sh --warn 70 --critical 85

# JSON output for Prometheus/monitoring
./volume-status.sh --json | jq .

# Alert-only mode (for cron jobs)
./volume-status.sh --alerts
```

#### Status Checks

1. **Docker Volumes**
   - List all GCRF volumes
   - Show volume sizes
   - Check volume existence
   - Classify by priority (Critical/Important)

2. **Disk Space**
   - Docker root directory usage
   - Backup directory usage
   - Alert on threshold breach (80% warn, 90% critical)
   - Show available space

3. **Backup Status**
   - Latest backup timestamp
   - Backup age (days since last backup)
   - Total backup count
   - Total backup size
   - Alert if no recent backup (>7 days)

4. **Container Health**
   - Container running status
   - Health check status (healthy/unhealthy/starting)
   - Alert on unhealthy containers

#### Alert Thresholds

| Level | Default | When to Alert |
|-------|---------|---------------|
| **Warning** | 80% | Disk usage ≥ 80% |
| **Critical** | 90% | Disk usage ≥ 90% |
| **Backup** | 7 days | No backup in 7+ days |

#### JSON Output Format

```json
{
  "timestamp": "2025-11-01T02:00:00Z",
  "status": "healthy|warning|critical",
  "alerts": {
    "total": 0,
    "warnings": 0,
    "critical": 0
  },
  "disk_space": {
    "docker_root": {
      "path": "/var/lib/docker",
      "usage_percent": 75,
      "status": "ok"
    }
  },
  "backups": {
    "latest": "20251101_020000",
    "age": "0 days ago",
    "count": 15,
    "total_size_bytes": 5368709120
  }
}
```

---

## Automation

### Cron Job Setup

#### Daily Backup (2 AM)
```bash
# /etc/crontab or crontab -e
0 2 * * * /opt/gcrf/deployment/scripts/backup-volumes.sh --verify --slack https://hooks.slack.com/... >> /var/log/gcrf/backup.log 2>&1
```

#### Weekly Cleanup (Sunday 3 AM)
```bash
0 3 * * 0 /opt/gcrf/deployment/scripts/cleanup-volumes.sh --retention 30 --logs 14 >> /var/log/gcrf/cleanup.log 2>&1
```

#### Hourly Status Check
```bash
0 * * * * /opt/gcrf/deployment/scripts/volume-status.sh --alerts >> /var/log/gcrf/status.log 2>&1
```

### Systemd Timer (Alternative to Cron)

**Timer file**: `/etc/systemd/system/gcrf-backup.timer`
```ini
[Unit]
Description=GCRF Daily Backup Timer
Requires=gcrf-backup.service

[Timer]
OnCalendar=daily
OnCalendar=02:00
Persistent=true

[Install]
WantedBy=timers.target
```

**Service file**: `/etc/systemd/system/gcrf-backup.service`
```ini
[Unit]
Description=GCRF Volume Backup Service
After=docker.service

[Service]
Type=oneshot
ExecStart=/opt/gcrf/deployment/scripts/backup-volumes.sh --verify
User=root
StandardOutput=journal
StandardError=journal
```

**Enable timer**:
```bash
systemctl enable gcrf-backup.timer
systemctl start gcrf-backup.timer
systemctl status gcrf-backup.timer
```

---

## Monitoring Integration

### Prometheus Export

Use `volume-status.sh --json` with a cron job to export metrics:

```bash
# /usr/local/bin/gcrf-exporter.sh
#!/bin/bash
OUTPUT=$(/opt/gcrf/deployment/scripts/volume-status.sh --json)
echo "$OUTPUT" | jq -r '
  "# HELP gcrf_disk_usage_percent Disk usage percentage
# TYPE gcrf_disk_usage_percent gauge
gcrf_disk_usage_percent{path=\"docker_root\"} \(.disk_space.docker_root.usage_percent)
# HELP gcrf_backup_age_seconds Time since last backup
# TYPE gcrf_backup_age_seconds gauge
gcrf_backup_age_seconds \(.backups.age | split(" ")[0] | tonumber * 86400)
# HELP gcrf_alerts_total Total alerts
# TYPE gcrf_alerts_total gauge
gcrf_alerts_total{level=\"warning\"} \(.alerts.warnings)
gcrf_alerts_total{level=\"critical\"} \(.alerts.critical)
"'
```

### Grafana Dashboard

**Metrics to track**:
- Disk usage over time
- Backup frequency and size
- Alert count trends
- Container health status

### Slack Notifications

**Webhook Integration**:
```bash
# Backup completion
./backup-volumes.sh --slack https://hooks.slack.com/services/XXX

# Status alerts
STATUS=$(./volume-status.sh --json)
if echo "$STATUS" | jq -e '.alerts.critical > 0' > /dev/null; then
  curl -X POST -H 'Content-type: application/json' \
    --data "{\"text\":\"🚨 GCRF Critical Alert: $STATUS\"}" \
    https://hooks.slack.com/services/XXX
fi
```

---

## Troubleshooting

### Common Issues

#### 1. Backup Fails - Disk Full
**Symptoms**: Exit code 4, "No space left on device"

**Solutions**:
```bash
# Check disk space
df -h /data/backups

# Clean old backups
./cleanup-volumes.sh --retention 7 --dry-run
./cleanup-volumes.sh --retention 7

# Move backups to external storage
rsync -av /data/backups/ /mnt/external-storage/gcrf-backups/
```

#### 2. Restore Fails - Container Won't Start
**Symptoms**: Container exits immediately after restore

**Solutions**:
```bash
# Check container logs
docker logs gcrf-postgres-primary

# Verify data integrity
docker exec gcrf-postgres-primary pg_isready -U postgres

# Restore from different backup
./restore-volumes.sh --dir /data/backups/PREVIOUS_BACKUP
```

#### 3. Cleanup Deletes Too Much
**Symptoms**: Critical backups deleted

**Prevention**:
```bash
# Always dry-run first
./cleanup-volumes.sh --dry-run

# Adjust retention for important dates
./cleanup-volumes.sh --retention 60  # Keep 2 months
```

#### 4. Status Script Shows False Alerts
**Symptoms**: Containers healthy but script reports critical

**Solutions**:
```bash
# Check actual container status
docker ps -a
docker inspect --format='{{.State.Health}}' gcrf-postgres-primary

# Adjust thresholds
./volume-status.sh --warn 85 --critical 95
```

### Debug Mode

Enable debug output in any script:
```bash
# Add to script
set -x  # Enable debug output

# Run with bash -x
bash -x ./backup-volumes.sh
```

---

## Best Practices

### Before Major Operations

1. **Always backup critical volumes**
   ```bash
   ./backup-volumes.sh --type critical --verify
   ```

2. **Check current status**
   ```bash
   ./volume-status.sh
   ```

3. **Ensure sufficient disk space** (20% minimum)

### Regular Maintenance

1. **Weekly backup verification**
   - Test restore to staging environment
   - Verify backup checksums

2. **Monthly cleanup**
   ```bash
   ./cleanup-volumes.sh --retention 30 --logs 30 --prune
   ```

3. **Quarterly disaster recovery drill**
   - Full system restore test
   - Document recovery time

### Security

1. **Protect backup files**
   ```bash
   chmod 600 /data/backups/*/postgres-all.sql.gz
   ```

2. **Encrypt sensitive backups**
   ```bash
   gpg --symmetric --cipher-algo AES256 postgres-all.sql.gz
   ```

3. **Secure remote storage**
   - Use encrypted transport (TLS)
   - Enable bucket versioning
   - Configure access policies

### Performance Optimization

1. **Schedule backups during low-traffic hours**
   - Default: 2 AM daily

2. **Use compression**
   - Already enabled in scripts (gzip)

3. **Incremental backups** (future enhancement)
   - PostgreSQL: WAL archiving
   - MinIO: Versioning + sync

---

## Appendix

### Volume Locations

| Volume | Container Path | Purpose |
|--------|---------------|---------|
| gcrf-postgres-primary-data | /var/lib/postgresql/data | PostgreSQL data |
| gcrf-redis-master-data | /data | Redis RDB/AOF |
| gcrf-nacos-mysql-data | /var/lib/mysql | Nacos config DB |
| gcrf-rabbitmq-data | /var/lib/rabbitmq | RabbitMQ data |
| gcrf-minio-data | /data | MinIO objects |

### Backup Size Estimates

| Component | Typical Size | Compressed Size | Compression Ratio |
|-----------|--------------|-----------------|-------------------|
| PostgreSQL (empty) | 50 MB | 5 MB | 10:1 |
| PostgreSQL (1M records) | 2 GB | 200 MB | 10:1 |
| Redis (10K keys) | 10 MB | 2 MB | 5:1 |
| Nacos MySQL | 50 MB | 5 MB | 10:1 |
| RabbitMQ | 20 MB | 5 MB | 4:1 |
| MinIO (100 files) | 500 MB | 450 MB | 1.1:1 |

### Recovery Time Objectives (RTO)

| Scenario | Estimated RTO | Steps |
|----------|---------------|-------|
| Single service restore | 5-10 minutes | `restore-volumes.sh --target SERVICE` |
| Full system restore | 15-30 minutes | `restore-volumes.sh --dir BACKUP` |
| Disaster recovery | 1-2 hours | Provision new infra + restore |

---

## Support & Contacts

**Documentation**: `deployment/scripts/VOLUME_MANAGEMENT.md`
**Scripts Location**: `deployment/scripts/`
**Log Location**: `/var/log/gcrf/`
**Backup Location**: `/data/backups/`

**Team**: GCRF DevOps Team
**Version**: 1.0.0
**Last Updated**: 2025-11-01
