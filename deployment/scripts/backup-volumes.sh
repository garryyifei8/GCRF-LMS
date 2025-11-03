#!/bin/bash

################################################################################
# GCRF Library Management System - Volume Backup Script
################################################################################
#
# PURPOSE:
#   Comprehensive backup solution for all Docker volumes in the GCRF stack
#   Supports PostgreSQL, Redis, Nacos MySQL, RabbitMQ, and MinIO backups
#
# USAGE:
#   ./backup-volumes.sh [OPTIONS]
#
# OPTIONS:
#   -d, --dir DIR           Backup directory (default: /data/backups/YYYYMMDD_HHMMSS)
#   -t, --type TYPE         Backup type: all|critical|important (default: all)
#   -r, --remote ENDPOINT   Upload to remote MinIO/S3 endpoint
#   -n, --notify EMAIL      Send notification to email on completion
#   -s, --slack URL         Send Slack webhook notification
#   -v, --verify            Verify backup integrity after creation
#   -h, --help              Show this help message
#
# BACKUP CLASSIFICATIONS:
#   Critical:  PostgreSQL, Nacos MySQL (must backup before operations)
#   Important: Redis, RabbitMQ, MinIO (backup daily)
#   Optional:  Logs (backup weekly)
#
# EXIT CODES:
#   0  - Success
#   1  - General error
#   2  - Docker not running
#   3  - Container not running
#   4  - Backup failed
#   5  - Verification failed
#
# AUTHOR: GCRF DevOps Team
# VERSION: 1.0.0
# LAST UPDATED: 2025-11-01
#
################################################################################

set -euo pipefail

# ============================================
# Configuration
# ============================================

# Default backup directory with timestamp
DEFAULT_BACKUP_DIR="/data/backups/$(date +%Y%m%d_%H%M%S)"
BACKUP_DIR="${BACKUP_DIR:-$DEFAULT_BACKUP_DIR}"
BACKUP_TYPE="all"
REMOTE_ENDPOINT=""
NOTIFY_EMAIL=""
SLACK_WEBHOOK=""
VERIFY_BACKUP=false

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Container names
POSTGRES_CONTAINER="gcrf-postgres-primary"
REDIS_CONTAINER="gcrf-redis-master"
NACOS_MYSQL_CONTAINER="gcrf-nacos-mysql"
RABBITMQ_CONTAINER="gcrf-rabbitmq"
MINIO_CONTAINER="gcrf-minio"

# Volume names
POSTGRES_VOLUME="gcrf-postgres-primary-data"
REDIS_VOLUME="gcrf-redis-master-data"
NACOS_MYSQL_VOLUME="gcrf-nacos-mysql-data"
RABBITMQ_VOLUME="gcrf-rabbitmq-data"
MINIO_VOLUME="gcrf-minio-data"

# Script start time
START_TIME=$(date +%s)

# ============================================
# Utility Functions
# ============================================

log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" >&2
}

log_step() {
    echo -e "${CYAN}[$1]${NC} $2"
}

show_help() {
    sed -n '3,40p' "$0" | sed 's/^# //' | sed 's/^#//'
}

check_dependencies() {
    log_step "PREFLIGHT" "Checking dependencies..."

    # Check if Docker is installed
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed"
        exit 2
    fi

    # Check if Docker daemon is running
    if ! docker info &> /dev/null; then
        log_error "Docker daemon is not running"
        exit 2
    fi

    log_success "All dependencies satisfied"
}

check_container_running() {
    local container_name=$1
    if ! docker ps --format '{{.Names}}' | grep -q "^${container_name}$"; then
        log_warn "Container ${container_name} is not running, skipping..."
        return 1
    fi
    return 0
}

create_backup_dir() {
    log_step "SETUP" "Creating backup directory: ${BACKUP_DIR}"
    mkdir -p "$BACKUP_DIR"

    if [ ! -w "$BACKUP_DIR" ]; then
        log_error "Backup directory is not writable: ${BACKUP_DIR}"
        exit 1
    fi

    log_success "Backup directory created"
}

# ============================================
# Backup Functions
# ============================================

backup_postgresql() {
    log_step "1/5" "Backing up PostgreSQL..."

    if ! check_container_running "$POSTGRES_CONTAINER"; then
        return 1
    fi

    local backup_file="${BACKUP_DIR}/postgres-all.sql.gz"

    # Use pg_dumpall to backup all databases including roles and tablespaces
    if docker exec "$POSTGRES_CONTAINER" pg_dumpall -U postgres | gzip > "$backup_file"; then
        local size=$(du -h "$backup_file" | cut -f1)
        log_success "PostgreSQL backup complete (${size}): ${backup_file}"
        return 0
    else
        log_error "PostgreSQL backup failed"
        return 1
    fi
}

backup_redis() {
    log_step "2/5" "Backing up Redis..."

    if ! check_container_running "$REDIS_CONTAINER"; then
        return 1
    fi

    local backup_file="${BACKUP_DIR}/redis-dump.rdb"

    # Trigger background save
    local redis_password="${REDIS_PASSWORD:-}"
    if [ -n "$redis_password" ]; then
        docker exec "$REDIS_CONTAINER" redis-cli -a "$redis_password" --no-auth-warning BGSAVE > /dev/null
    else
        docker exec "$REDIS_CONTAINER" redis-cli BGSAVE > /dev/null
    fi

    # Wait for BGSAVE to complete (check every second, max 60 seconds)
    local max_wait=60
    local waited=0
    while [ $waited -lt $max_wait ]; do
        local save_status
        if [ -n "$redis_password" ]; then
            save_status=$(docker exec "$REDIS_CONTAINER" redis-cli -a "$redis_password" --no-auth-warning LASTSAVE)
        else
            save_status=$(docker exec "$REDIS_CONTAINER" redis-cli LASTSAVE)
        fi

        if [ "$save_status" != "0" ]; then
            break
        fi
        sleep 1
        waited=$((waited + 1))
    done

    # Copy dump.rdb from container
    if docker cp "${REDIS_CONTAINER}:/data/dump.rdb" "$backup_file" 2>/dev/null; then
        local size=$(du -h "$backup_file" | cut -f1)
        log_success "Redis backup complete (${size}): ${backup_file}"
        return 0
    else
        log_error "Redis backup failed"
        return 1
    fi
}

backup_nacos_mysql() {
    log_step "3/5" "Backing up Nacos MySQL..."

    if ! check_container_running "$NACOS_MYSQL_CONTAINER"; then
        return 1
    fi

    local backup_file="${BACKUP_DIR}/nacos-mysql.sql.gz"
    local mysql_password="${NACOS_MYSQL_ROOT_PASSWORD:-root}"

    # Dump Nacos database
    if docker exec "$NACOS_MYSQL_CONTAINER" mysqldump -u root -p"$mysql_password" \
        --single-transaction --routines --triggers --events nacos_config | gzip > "$backup_file"; then
        local size=$(du -h "$backup_file" | cut -f1)
        log_success "Nacos MySQL backup complete (${size}): ${backup_file}"
        return 0
    else
        log_error "Nacos MySQL backup failed"
        return 1
    fi
}

backup_rabbitmq() {
    log_step "4/5" "Backing up RabbitMQ..."

    if ! check_container_running "$RABBITMQ_CONTAINER"; then
        return 1
    fi

    local backup_file="${BACKUP_DIR}/rabbitmq-data.tar.gz"

    # Backup RabbitMQ volume as tar archive
    if docker run --rm \
        -v "${RABBITMQ_VOLUME}:/data:ro" \
        -v "${BACKUP_DIR}:/backup" \
        alpine tar czf "/backup/rabbitmq-data.tar.gz" -C /data .; then
        local size=$(du -h "$backup_file" | cut -f1)
        log_success "RabbitMQ backup complete (${size}): ${backup_file}"
        return 0
    else
        log_error "RabbitMQ backup failed"
        return 1
    fi
}

backup_minio() {
    log_step "5/5" "Backing up MinIO..."

    if ! check_container_running "$MINIO_CONTAINER"; then
        return 1
    fi

    local backup_file="${BACKUP_DIR}/minio-data.tar.gz"

    # Backup MinIO volume as tar archive
    if docker run --rm \
        -v "${MINIO_VOLUME}:/data:ro" \
        -v "${BACKUP_DIR}:/backup" \
        alpine tar czf "/backup/minio-data.tar.gz" -C /data .; then
        local size=$(du -h "$backup_file" | cut -f1)
        log_success "MinIO backup complete (${size}): ${backup_file}"
        return 0
    else
        log_error "MinIO backup failed"
        return 1
    fi
}

# ============================================
# Post-Backup Functions
# ============================================

create_manifest() {
    log_step "MANIFEST" "Creating backup manifest..."

    cd "$BACKUP_DIR"

    # Create SHA256 checksums
    find . -type f ! -name "MANIFEST.sha256" ! -name "BACKUP_INFO.txt" -exec sha256sum {} \; > MANIFEST.sha256

    # Create backup info file
    cat > BACKUP_INFO.txt <<EOF
GCRF Library Management System - Backup Information
====================================================

Backup Date: $(date '+%Y-%m-%d %H:%M:%S')
Backup Type: ${BACKUP_TYPE}
Backup Directory: ${BACKUP_DIR}
Hostname: $(hostname)
Docker Version: $(docker --version)

Files:
------
$(ls -lh | grep -v total)

Total Size: $(du -sh . | cut -f1)

Checksums (SHA256):
-------------------
$(cat MANIFEST.sha256)
EOF

    log_success "Manifest created"
}

verify_backup() {
    log_step "VERIFY" "Verifying backup integrity..."

    cd "$BACKUP_DIR"

    if [ ! -f "MANIFEST.sha256" ]; then
        log_error "Manifest file not found"
        return 1
    fi

    if sha256sum -c MANIFEST.sha256 --quiet; then
        log_success "All backup files verified successfully"
        return 0
    else
        log_error "Backup verification failed"
        return 1
    fi
}

upload_to_remote() {
    if [ -z "$REMOTE_ENDPOINT" ]; then
        return 0
    fi

    log_step "UPLOAD" "Uploading backup to remote storage: ${REMOTE_ENDPOINT}"

    # Check if mc (MinIO Client) is installed
    if ! command -v mc &> /dev/null; then
        log_warn "MinIO Client (mc) not installed, skipping remote upload"
        return 0
    fi

    # Create tar archive of entire backup directory
    local archive_name="gcrf-backup-$(date +%Y%m%d_%H%M%S).tar.gz"
    local archive_path="/tmp/${archive_name}"

    tar czf "$archive_path" -C "$(dirname "$BACKUP_DIR")" "$(basename "$BACKUP_DIR")"

    # Upload to MinIO/S3
    if mc cp "$archive_path" "${REMOTE_ENDPOINT}/${archive_name}"; then
        log_success "Backup uploaded to remote storage"
        rm -f "$archive_path"
        return 0
    else
        log_error "Remote upload failed"
        rm -f "$archive_path"
        return 1
    fi
}

send_notification() {
    local status=$1
    local duration=$2

    if [ -n "$NOTIFY_EMAIL" ]; then
        send_email_notification "$status" "$duration"
    fi

    if [ -n "$SLACK_WEBHOOK" ]; then
        send_slack_notification "$status" "$duration"
    fi
}

send_email_notification() {
    local status=$1
    local duration=$2

    # Placeholder for email notification
    log_info "Email notification would be sent to: ${NOTIFY_EMAIL}"
}

send_slack_notification() {
    local status=$1
    local duration=$2

    if ! command -v curl &> /dev/null; then
        return 0
    fi

    local color="good"
    local emoji=":white_check_mark:"
    if [ "$status" != "SUCCESS" ]; then
        color="danger"
        emoji=":x:"
    fi

    local payload=$(cat <<EOF
{
    "attachments": [{
        "color": "${color}",
        "title": "${emoji} GCRF Backup ${status}",
        "fields": [
            {"title": "Status", "value": "${status}", "short": true},
            {"title": "Duration", "value": "${duration}s", "short": true},
            {"title": "Backup Directory", "value": "${BACKUP_DIR}", "short": false},
            {"title": "Hostname", "value": "$(hostname)", "short": true},
            {"title": "Timestamp", "value": "$(date '+%Y-%m-%d %H:%M:%S')", "short": true}
        ]
    }]
}
EOF
    )

    curl -X POST -H 'Content-type: application/json' --data "$payload" "$SLACK_WEBHOOK" &>/dev/null
}

# ============================================
# Main Execution
# ============================================

parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -d|--dir)
                BACKUP_DIR="$2"
                shift 2
                ;;
            -t|--type)
                BACKUP_TYPE="$2"
                shift 2
                ;;
            -r|--remote)
                REMOTE_ENDPOINT="$2"
                shift 2
                ;;
            -n|--notify)
                NOTIFY_EMAIL="$2"
                shift 2
                ;;
            -s|--slack)
                SLACK_WEBHOOK="$2"
                shift 2
                ;;
            -v|--verify)
                VERIFY_BACKUP=true
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
}

main() {
    parse_args "$@"

    echo "============================================"
    echo "  GCRF Volume Backup Script"
    echo "============================================"
    echo "Backup Directory: ${BACKUP_DIR}"
    echo "Backup Type: ${BACKUP_TYPE}"
    echo "Started: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "============================================"
    echo ""

    # Preflight checks
    check_dependencies
    create_backup_dir

    # Track backup success
    local backup_failed=false

    # Execute backups based on type
    case $BACKUP_TYPE in
        critical)
            backup_postgresql || backup_failed=true
            backup_nacos_mysql || backup_failed=true
            ;;
        important)
            backup_redis || backup_failed=true
            backup_rabbitmq || backup_failed=true
            backup_minio || backup_failed=true
            ;;
        all)
            backup_postgresql || backup_failed=true
            backup_redis || backup_failed=true
            backup_nacos_mysql || backup_failed=true
            backup_rabbitmq || backup_failed=true
            backup_minio || backup_failed=true
            ;;
        *)
            log_error "Invalid backup type: ${BACKUP_TYPE}"
            exit 1
            ;;
    esac

    # Create manifest
    create_manifest

    # Verify backup if requested
    if [ "$VERIFY_BACKUP" = true ]; then
        if ! verify_backup; then
            backup_failed=true
        fi
    fi

    # Upload to remote storage
    upload_to_remote

    # Calculate duration
    local end_time=$(date +%s)
    local duration=$((end_time - START_TIME))

    echo ""
    echo "============================================"
    if [ "$backup_failed" = true ]; then
        log_error "Backup completed with errors"
        echo "Backup Directory: ${BACKUP_DIR}"
        echo "Duration: ${duration} seconds"
        echo "Status: FAILED"
        echo "============================================"
        send_notification "FAILED" "$duration"
        exit 4
    else
        log_success "Backup completed successfully"
        echo "Backup Directory: ${BACKUP_DIR}"
        echo "Duration: ${duration} seconds"
        echo "Total Size: $(du -sh "$BACKUP_DIR" | cut -f1)"
        echo "============================================"
        send_notification "SUCCESS" "$duration"
        exit 0
    fi
}

# Execute main function
main "$@"
