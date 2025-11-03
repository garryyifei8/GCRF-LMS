#!/bin/bash

################################################################################
# GCRF Library Management System - Volume Restore Script
################################################################################
#
# PURPOSE:
#   Restore Docker volumes from backup archives created by backup-volumes.sh
#   Supports selective restoration and automatic service management
#
# USAGE:
#   ./restore-volumes.sh [OPTIONS]
#
# OPTIONS:
#   -d, --dir DIR           Backup directory to restore from (required)
#   -t, --target TARGET     Restore target: all|postgres|redis|nacos|rabbitmq|minio
#   -f, --force             Skip confirmation prompts
#   -k, --keep-services     Don't stop/start services (advanced)
#   --verify                Verify backup before restore
#   -h, --help              Show this help message
#
# EXAMPLES:
#   # Restore all volumes from latest backup
#   ./restore-volumes.sh -d /data/backups/20251101_020000
#
#   # Restore only PostgreSQL database
#   ./restore-volumes.sh -d /data/backups/20251101_020000 -t postgres
#
#   # Force restore without confirmation
#   ./restore-volumes.sh -d /data/backups/20251101_020000 -f
#
# EXIT CODES:
#   0  - Success
#   1  - General error
#   2  - Docker not running
#   3  - Backup directory invalid
#   4  - Restore failed
#   5  - Verification failed
#   6  - User cancelled
#
# WARNINGS:
#   - This script will STOP all services before restoration
#   - Existing data will be OVERWRITTEN
#   - Always backup current state before restoring
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

BACKUP_DIR=""
RESTORE_TARGET="all"
FORCE_RESTORE=false
KEEP_SERVICES=false
VERIFY_BEFORE_RESTORE=false

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Container names
POSTGRES_CONTAINER="gcrf-postgres-primary"
REDIS_CONTAINER="gcrf-redis-master"
NACOS_MYSQL_CONTAINER="gcrf-nacos-mysql"
RABBITMQ_CONTAINER="gcrf-rabbitmq"
MINIO_CONTAINER="gcrf-minio"
NACOS_CONTAINER="gcrf-nacos"

# Volume names
POSTGRES_VOLUME="gcrf-postgres-primary-data"
REDIS_VOLUME="gcrf-redis-master-data"
NACOS_MYSQL_VOLUME="gcrf-nacos-mysql-data"
RABBITMQ_VOLUME="gcrf-rabbitmq-data"
MINIO_VOLUME="gcrf-minio-data"

# Compose file path
COMPOSE_FILE="/Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/deployment/docker-compose.infrastructure.yml"

# Track stopped containers
STOPPED_CONTAINERS=()

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
    sed -n '3,45p' "$0" | sed 's/^# //' | sed 's/^#//'
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

validate_backup_dir() {
    log_step "VALIDATE" "Validating backup directory..."

    if [ -z "$BACKUP_DIR" ]; then
        log_error "Backup directory not specified"
        exit 3
    fi

    if [ ! -d "$BACKUP_DIR" ]; then
        log_error "Backup directory does not exist: ${BACKUP_DIR}"
        exit 3
    fi

    if [ ! -f "${BACKUP_DIR}/MANIFEST.sha256" ]; then
        log_warn "Manifest file not found, backup may be incomplete"
    fi

    if [ ! -f "${BACKUP_DIR}/BACKUP_INFO.txt" ]; then
        log_warn "Backup info file not found"
    fi

    log_success "Backup directory validated"
}

verify_backup_integrity() {
    log_step "VERIFY" "Verifying backup integrity..."

    cd "$BACKUP_DIR"

    if [ ! -f "MANIFEST.sha256" ]; then
        log_warn "Cannot verify backup: manifest file missing"
        return 0
    fi

    if sha256sum -c MANIFEST.sha256 --quiet 2>/dev/null; then
        log_success "Backup integrity verified"
        return 0
    else
        log_error "Backup integrity check failed"
        return 1
    fi
}

confirm_restore() {
    if [ "$FORCE_RESTORE" = true ]; then
        return 0
    fi

    echo ""
    echo -e "${MAGENTA}============================================${NC}"
    echo -e "${MAGENTA}          WARNING: DATA RESTORATION${NC}"
    echo -e "${MAGENTA}============================================${NC}"
    echo ""
    echo "This operation will:"
    echo "  1. STOP all running services"
    echo "  2. OVERWRITE existing data"
    echo "  3. Restore from: ${BACKUP_DIR}"
    echo "  4. Restart services after restoration"
    echo ""
    echo -e "${YELLOW}Current data will be PERMANENTLY LOST!${NC}"
    echo ""
    read -p "Are you sure you want to continue? (yes/no): " -r
    echo ""

    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        log_warn "Restore cancelled by user"
        exit 6
    fi
}

# ============================================
# Service Management Functions
# ============================================

stop_services() {
    if [ "$KEEP_SERVICES" = true ]; then
        log_warn "Skipping service shutdown (--keep-services flag)"
        return 0
    fi

    log_step "SHUTDOWN" "Stopping services for restoration..."

    # Stop services based on restore target
    case $RESTORE_TARGET in
        postgres)
            stop_container "$POSTGRES_CONTAINER"
            ;;
        redis)
            stop_container "$REDIS_CONTAINER"
            ;;
        nacos)
            stop_container "$NACOS_CONTAINER"
            stop_container "$NACOS_MYSQL_CONTAINER"
            ;;
        rabbitmq)
            stop_container "$RABBITMQ_CONTAINER"
            ;;
        minio)
            stop_container "$MINIO_CONTAINER"
            ;;
        all)
            # Stop in reverse dependency order
            stop_container "$NACOS_CONTAINER"
            stop_container "$MINIO_CONTAINER"
            stop_container "$RABBITMQ_CONTAINER"
            stop_container "$REDIS_CONTAINER"
            stop_container "$NACOS_MYSQL_CONTAINER"
            stop_container "$POSTGRES_CONTAINER"
            ;;
    esac

    # Wait for containers to stop
    sleep 5

    log_success "Services stopped"
}

stop_container() {
    local container=$1

    if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        log_info "Stopping container: ${container}"
        docker stop "$container" > /dev/null
        STOPPED_CONTAINERS+=("$container")
    else
        log_warn "Container ${container} is not running"
    fi
}

start_services() {
    if [ "$KEEP_SERVICES" = true ]; then
        return 0
    fi

    log_step "STARTUP" "Starting services after restoration..."

    # Start containers in dependency order
    for container in "${STOPPED_CONTAINERS[@]}"; do
        log_info "Starting container: ${container}"
        docker start "$container" > /dev/null || {
            log_warn "Failed to start ${container}, using docker-compose..."
            cd "$(dirname "$COMPOSE_FILE")"
            docker-compose -f "$COMPOSE_FILE" up -d "$container" 2>/dev/null || true
        }
    done

    # Wait for services to be healthy
    log_info "Waiting for services to become healthy..."
    sleep 10

    log_success "Services started"
}

# ============================================
# Restore Functions
# ============================================

restore_postgresql() {
    log_step "RESTORE" "Restoring PostgreSQL database..."

    local backup_file="${BACKUP_DIR}/postgres-all.sql.gz"

    if [ ! -f "$backup_file" ]; then
        log_error "PostgreSQL backup file not found: ${backup_file}"
        return 1
    fi

    # Start PostgreSQL temporarily for restoration
    if ! docker ps --format '{{.Names}}' | grep -q "^${POSTGRES_CONTAINER}$"; then
        log_info "Starting PostgreSQL container for restoration..."
        docker start "$POSTGRES_CONTAINER" > /dev/null
        sleep 10
    fi

    # Drop existing databases and restore
    if gunzip -c "$backup_file" | docker exec -i "$POSTGRES_CONTAINER" psql -U postgres; then
        log_success "PostgreSQL database restored successfully"
        return 0
    else
        log_error "PostgreSQL restore failed"
        return 1
    fi
}

restore_redis() {
    log_step "RESTORE" "Restoring Redis data..."

    local backup_file="${BACKUP_DIR}/redis-dump.rdb"

    if [ ! -f "$backup_file" ]; then
        log_error "Redis backup file not found: ${backup_file}"
        return 1
    fi

    # Copy dump.rdb to temporary container and then to volume
    local temp_container="gcrf-redis-restore-temp"

    docker run --rm -d --name "$temp_container" \
        -v "${REDIS_VOLUME}:/data" \
        alpine sleep 60 > /dev/null

    docker cp "$backup_file" "${temp_container}:/data/dump.rdb"
    docker stop "$temp_container" > /dev/null 2>&1 || true

    log_success "Redis data restored successfully"
    return 0
}

restore_nacos_mysql() {
    log_step "RESTORE" "Restoring Nacos MySQL database..."

    local backup_file="${BACKUP_DIR}/nacos-mysql.sql.gz"

    if [ ! -f "$backup_file" ]; then
        log_error "Nacos MySQL backup file not found: ${backup_file}"
        return 1
    fi

    # Start MySQL temporarily for restoration
    if ! docker ps --format '{{.Names}}' | grep -q "^${NACOS_MYSQL_CONTAINER}$"; then
        log_info "Starting Nacos MySQL container for restoration..."
        docker start "$NACOS_MYSQL_CONTAINER" > /dev/null
        sleep 15
    fi

    local mysql_password="${NACOS_MYSQL_ROOT_PASSWORD:-root}"

    # Restore database
    if gunzip -c "$backup_file" | docker exec -i "$NACOS_MYSQL_CONTAINER" mysql -u root -p"$mysql_password" nacos_config; then
        log_success "Nacos MySQL database restored successfully"
        return 0
    else
        log_error "Nacos MySQL restore failed"
        return 1
    fi
}

restore_rabbitmq() {
    log_step "RESTORE" "Restoring RabbitMQ data..."

    local backup_file="${BACKUP_DIR}/rabbitmq-data.tar.gz"

    if [ ! -f "$backup_file" ]; then
        log_error "RabbitMQ backup file not found: ${backup_file}"
        return 1
    fi

    # Extract tar archive to volume
    if docker run --rm \
        -v "${RABBITMQ_VOLUME}:/data" \
        -v "${BACKUP_DIR}:/backup:ro" \
        alpine sh -c "rm -rf /data/* && tar xzf /backup/rabbitmq-data.tar.gz -C /data"; then
        log_success "RabbitMQ data restored successfully"
        return 0
    else
        log_error "RabbitMQ restore failed"
        return 1
    fi
}

restore_minio() {
    log_step "RESTORE" "Restoring MinIO data..."

    local backup_file="${BACKUP_DIR}/minio-data.tar.gz"

    if [ ! -f "$backup_file" ]; then
        log_error "MinIO backup file not found: ${backup_file}"
        return 1
    fi

    # Extract tar archive to volume
    if docker run --rm \
        -v "${MINIO_VOLUME}:/data" \
        -v "${BACKUP_DIR}:/backup:ro" \
        alpine sh -c "rm -rf /data/* && tar xzf /backup/minio-data.tar.gz -C /data"; then
        log_success "MinIO data restored successfully"
        return 0
    else
        log_error "MinIO restore failed"
        return 1
    fi
}

# ============================================
# Verification Functions
# ============================================

verify_restoration() {
    log_step "VERIFY" "Verifying restoration..."

    local verification_failed=false

    # Wait for services to be fully ready
    sleep 15

    # Verify based on restore target
    case $RESTORE_TARGET in
        postgres|all)
            verify_postgres || verification_failed=true
            ;;
    esac

    case $RESTORE_TARGET in
        redis|all)
            verify_redis || verification_failed=true
            ;;
    esac

    case $RESTORE_TARGET in
        nacos|all)
            verify_nacos || verification_failed=true
            ;;
    esac

    if [ "$verification_failed" = true ]; then
        log_warn "Some verifications failed, please check manually"
        return 1
    else
        log_success "All verifications passed"
        return 0
    fi
}

verify_postgres() {
    if docker exec "$POSTGRES_CONTAINER" pg_isready -U postgres > /dev/null 2>&1; then
        log_success "PostgreSQL is healthy"
        return 0
    else
        log_error "PostgreSQL health check failed"
        return 1
    fi
}

verify_redis() {
    local redis_password="${REDIS_PASSWORD:-}"
    if [ -n "$redis_password" ]; then
        if docker exec "$REDIS_CONTAINER" redis-cli -a "$redis_password" --no-auth-warning ping | grep -q "PONG"; then
            log_success "Redis is healthy"
            return 0
        fi
    else
        if docker exec "$REDIS_CONTAINER" redis-cli ping | grep -q "PONG"; then
            log_success "Redis is healthy"
            return 0
        fi
    fi
    log_error "Redis health check failed"
    return 1
}

verify_nacos() {
    if docker exec "$NACOS_MYSQL_CONTAINER" mysqladmin ping -h localhost -u root -p"${NACOS_MYSQL_ROOT_PASSWORD:-root}" > /dev/null 2>&1; then
        log_success "Nacos MySQL is healthy"
        return 0
    else
        log_error "Nacos MySQL health check failed"
        return 1
    fi
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
            -t|--target)
                RESTORE_TARGET="$2"
                shift 2
                ;;
            -f|--force)
                FORCE_RESTORE=true
                shift
                ;;
            -k|--keep-services)
                KEEP_SERVICES=true
                shift
                ;;
            --verify)
                VERIFY_BEFORE_RESTORE=true
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
    echo "  GCRF Volume Restore Script"
    echo "============================================"
    echo "Backup Directory: ${BACKUP_DIR}"
    echo "Restore Target: ${RESTORE_TARGET}"
    echo "Started: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "============================================"
    echo ""

    # Preflight checks
    check_dependencies
    validate_backup_dir

    # Verify backup if requested
    if [ "$VERIFY_BEFORE_RESTORE" = true ]; then
        if ! verify_backup_integrity; then
            exit 5
        fi
    fi

    # Show backup info
    if [ -f "${BACKUP_DIR}/BACKUP_INFO.txt" ]; then
        echo ""
        echo "Backup Information:"
        echo "-------------------"
        head -10 "${BACKUP_DIR}/BACKUP_INFO.txt"
        echo ""
    fi

    # Confirm restoration
    confirm_restore

    # Stop services
    stop_services

    # Track restore success
    local restore_failed=false

    # Execute restores based on target
    case $RESTORE_TARGET in
        postgres)
            restore_postgresql || restore_failed=true
            ;;
        redis)
            restore_redis || restore_failed=true
            ;;
        nacos)
            restore_nacos_mysql || restore_failed=true
            ;;
        rabbitmq)
            restore_rabbitmq || restore_failed=true
            ;;
        minio)
            restore_minio || restore_failed=true
            ;;
        all)
            restore_postgresql || restore_failed=true
            restore_redis || restore_failed=true
            restore_nacos_mysql || restore_failed=true
            restore_rabbitmq || restore_failed=true
            restore_minio || restore_failed=true
            ;;
        *)
            log_error "Invalid restore target: ${RESTORE_TARGET}"
            start_services
            exit 1
            ;;
    esac

    # Start services
    start_services

    # Verify restoration
    verify_restoration || restore_failed=true

    # Calculate duration
    local end_time=$(date +%s)
    local duration=$((end_time - START_TIME))

    echo ""
    echo "============================================"
    if [ "$restore_failed" = true ]; then
        log_error "Restore completed with errors"
        echo "Duration: ${duration} seconds"
        echo "Status: FAILED"
        echo "============================================"
        exit 4
    else
        log_success "Restore completed successfully"
        echo "Duration: ${duration} seconds"
        echo "Status: SUCCESS"
        echo "============================================"
        exit 0
    fi
}

# Execute main function
main "$@"
