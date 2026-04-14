#!/bin/bash

################################################################################
# GCRF Library Management System - Rollback Script
#
# This script rolls back the GCRF Library Management System to a previous state
# using backups created before deployment.
#
# Usage: ./rollback.sh [backup-directory]
#
# Examples:
#   ./rollback.sh                              # Interactive mode
#   ./rollback.sh /backups/pre-deployment-20251201_140000
#
# CAUTION: This script will:
#   - Stop all application services
#   - Restore database from backup
#   - Restore previous Docker images/containers
#   - Restart services
#
# Exit Codes:
#   0 - Success
#   1 - General error
#   2 - Backup not found or invalid
#   3 - Rollback failed
################################################################################

set -e
set -u

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOYMENT_DIR="$(dirname "$SCRIPT_DIR")"
BACKUP_ROOT="/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="/var/log/gcrf-rollback-${TIMESTAMP}.log"

################################################################################
# Functions
################################################################################

log() {
    local level=$1
    shift
    local message="$@"

    case $level in
        INFO)
            echo -e "${BLUE}[INFO]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        SUCCESS)
            echo -e "${GREEN}[SUCCESS]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        WARNING)
            echo -e "${YELLOW}[WARNING]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        ERROR)
            echo -e "${RED}[ERROR]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        *)
            echo "$message" | tee -a "$LOG_FILE"
            ;;
    esac
}

# List available backups
list_backups() {
    log INFO "Available backups:"
    echo ""

    local backups=($(find "$BACKUP_ROOT" -maxdepth 1 -type d -name "pre-deployment-*" | sort -r))

    if [ ${#backups[@]} -eq 0 ]; then
        log ERROR "No backups found in $BACKUP_ROOT"
        return 1
    fi

    local i=1
    for backup in "${backups[@]}"; do
        local backup_name=$(basename "$backup")
        local backup_date=$(echo "$backup_name" | sed 's/pre-deployment-//')
        local backup_size=$(du -sh "$backup" | cut -f1)

        echo "  [$i] $backup_name"
        echo "      Date: $backup_date"
        echo "      Size: $backup_size"
        echo "      Path: $backup"

        # Check if manifest exists
        if [ -f "$backup/MANIFEST.txt" ]; then
            echo "      Has manifest: Yes"
        fi
        echo ""

        i=$((i + 1))
    done

    return 0
}

# Select backup interactively
select_backup() {
    list_backups

    local backups=($(find "$BACKUP_ROOT" -maxdepth 1 -type d -name "pre-deployment-*" | sort -r))

    echo -n "Select backup number (or 0 to cancel): "
    read selection

    if [ "$selection" = "0" ]; then
        log INFO "Rollback cancelled by user"
        exit 0
    fi

    if [ "$selection" -lt 1 ] || [ "$selection" -gt ${#backups[@]} ]; then
        log ERROR "Invalid selection"
        exit 1
    fi

    BACKUP_DIR="${backups[$((selection - 1))]}"
}

# Validate backup directory
validate_backup() {
    local backup_dir=$1

    log INFO "Validating backup: $backup_dir"

    if [ ! -d "$backup_dir" ]; then
        log ERROR "Backup directory not found: $backup_dir"
        exit 2
    fi

    # Check for required files
    local required_files=("database-backup.sql.gz" "env.backup")
    for file in "${required_files[@]}"; do
        if [ ! -f "$backup_dir/$file" ]; then
            log WARNING "Required file not found: $file"
        else
            log SUCCESS "Found: $file"
        fi
    done

    # Display manifest if exists
    if [ -f "$backup_dir/MANIFEST.txt" ]; then
        log INFO "Backup manifest:"
        cat "$backup_dir/MANIFEST.txt"
    fi
}

# Confirm rollback
confirm_rollback() {
    local backup_dir=$1

    log WARNING "
╔═══════════════════════════════════════════════════════════════════╗
║                         ROLLBACK WARNING                          ║
╚═══════════════════════════════════════════════════════════════════╝

This will:
  1. Stop all application services
  2. Restore database from backup: $(basename $backup_dir)
  3. Restore previous configuration
  4. Restart all services

Current data will be lost!

Backup directory: $backup_dir
"

    echo -n "Are you sure you want to proceed? (type 'yes' to confirm): "
    read confirmation

    if [ "$confirmation" != "yes" ]; then
        log INFO "Rollback cancelled by user"
        exit 0
    fi

    log INFO "Rollback confirmed, proceeding..."
}

# Create pre-rollback backup
create_pre_rollback_backup() {
    log INFO "Creating pre-rollback backup (current state)..."

    local backup_dir="$BACKUP_ROOT/pre-rollback-${TIMESTAMP}"
    mkdir -p "$backup_dir"

    # Backup current database
    if docker ps | grep -q postgres-primary; then
        log INFO "Backing up current database state..."
        docker exec postgres-primary pg_dumpall -U postgres | gzip > "$backup_dir/database-before-rollback.sql.gz"
    fi

    # Backup current configuration
    cp "$DEPLOYMENT_DIR/.env" "$backup_dir/env-before-rollback" 2>/dev/null || true
    cp -r "$DEPLOYMENT_DIR"/*.yml "$backup_dir/" 2>/dev/null || true

    log SUCCESS "Pre-rollback backup created: $backup_dir"
}

# Stop services
stop_services() {
    log INFO "Stopping application services..."

    cd "$DEPLOYMENT_DIR"

    # Stop services in reverse order
    docker-compose -f docker-compose.monitoring.yml down 2>/dev/null || true
    docker-compose -f docker-compose.services.yml down
    # Keep infrastructure running

    log SUCCESS "Application services stopped"
}

# Restore database
restore_database() {
    local backup_dir=$1

    log INFO "Restoring database from backup..."

    local db_backup="$backup_dir/database-backup.sql.gz"

    if [ ! -f "$db_backup" ]; then
        log WARNING "Database backup not found, skipping database restore"
        return
    fi

    # Drop and recreate databases
    log INFO "Dropping existing databases..."
    docker exec postgres-primary psql -U postgres -c "DROP DATABASE IF EXISTS gcrf_auth;" || true
    docker exec postgres-primary psql -U postgres -c "DROP DATABASE IF EXISTS gcrf_book;" || true
    docker exec postgres-primary psql -U postgres -c "DROP DATABASE IF EXISTS gcrf_reader;" || true
    docker exec postgres-primary psql -U postgres -c "DROP DATABASE IF EXISTS gcrf_circulation;" || true
    docker exec postgres-primary psql -U postgres -c "DROP DATABASE IF EXISTS gcrf_system;" || true
    docker exec postgres-primary psql -U postgres -c "DROP DATABASE IF EXISTS gcrf_notification;" || true
    docker exec postgres-primary psql -U postgres -c "DROP DATABASE IF EXISTS gcrf_analytics;" || true
    docker exec postgres-primary psql -U postgres -c "DROP DATABASE IF EXISTS gcrf_recommend;" || true

    # Restore from backup
    log INFO "Restoring database from backup..."
    gunzip -c "$db_backup" | docker exec -i postgres-primary psql -U postgres

    log SUCCESS "Database restored successfully"
}

# Restore configuration
restore_configuration() {
    local backup_dir=$1

    log INFO "Restoring configuration..."

    # Restore .env file
    if [ -f "$backup_dir/env.backup" ]; then
        cp "$backup_dir/env.backup" "$DEPLOYMENT_DIR/.env"
        log SUCCESS ".env file restored"
    fi

    # Restore docker-compose files
    for file in "$backup_dir"/*.yml; do
        if [ -f "$file" ]; then
            local filename=$(basename "$file")
            cp "$file" "$DEPLOYMENT_DIR/$filename"
            log SUCCESS "$filename restored"
        fi
    done

    log SUCCESS "Configuration restored"
}

# Restart services
restart_services() {
    log INFO "Restarting services..."

    cd "$DEPLOYMENT_DIR"

    # Start application services
    docker-compose -f docker-compose.services.yml up -d

    # Wait for services to be healthy
    log INFO "Waiting for services to start..."
    sleep 30

    # Check service health
    local services=(
        "8080:Gateway"
        "8081:Auth"
        "8082:Book"
        "8083:Circulation"
        "8084:Reader"
        "8085:System"
        "8086:Notification"
    )

    local failed_services=()
    for service in "${services[@]}"; do
        local port="${service%%:*}"
        local name="${service##*:}"

        if curl -sf "http://localhost:${port}/actuator/health" &> /dev/null; then
            log SUCCESS "$name service is healthy"
        else
            log WARNING "$name service health check failed"
            failed_services+=("$name")
        fi
    done

    if [ ${#failed_services[@]} -gt 0 ]; then
        log WARNING "Some services are not healthy: ${failed_services[*]}"
        log WARNING "Check logs: docker-compose -f docker-compose.services.yml logs"
    fi

    # Start monitoring services
    if [ -f "docker-compose.monitoring.yml" ]; then
        docker-compose -f docker-compose.monitoring.yml up -d
        log SUCCESS "Monitoring services started"
    fi

    log SUCCESS "Services restarted"
}

# Verify rollback
verify_rollback() {
    log INFO "Verifying rollback..."

    # Check containers are running
    local expected_containers=(
        "gcrf-gateway-service"
        "gcrf-auth-service"
        "gcrf-book-service"
    )

    local failed=false
    for container in "${expected_containers[@]}"; do
        if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
            log SUCCESS "Container $container is running"
        else
            log ERROR "Container $container is not running"
            failed=true
        fi
    done

    # Check API Gateway
    if curl -sf http://localhost:8080/actuator/health &> /dev/null; then
        log SUCCESS "API Gateway is accessible"
    else
        log ERROR "API Gateway is not accessible"
        failed=true
    fi

    if [ "$failed" = true ]; then
        log ERROR "Rollback verification failed"
        log ERROR "Check logs: docker-compose -f docker-compose.services.yml logs"
        exit 3
    fi

    log SUCCESS "Rollback verification passed"
}

# Print rollback summary
print_summary() {
    local backup_dir=$1

    log "" "
╔═══════════════════════════════════════════════════════════════════╗
║                    Rollback Completed Successfully                ║
╚═══════════════════════════════════════════════════════════════════╝

Rollback Time: $(date)
Backup Used: $(basename $backup_dir)
Log File: $LOG_FILE

Services Status:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
$(docker ps --format 'table {{.Names}}\t{{.Status}}' | grep gcrf)

Pre-Rollback Backup:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
A backup of the state before rollback was created:
$BACKUP_ROOT/pre-rollback-${TIMESTAMP}

Next Steps:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. Verify application functionality
2. Check logs for any errors
3. Monitor system for issues
4. If issues persist, contact support

For troubleshooting, see: doc/deployment/troubleshooting-guide.md
"
}

################################################################################
# Main Execution
################################################################################

main() {
    # Create log file
    sudo touch "$LOG_FILE" 2>/dev/null || LOG_FILE="./rollback-${TIMESTAMP}.log"
    touch "$LOG_FILE"

    log "" "
╔═══════════════════════════════════════════════════════════════════╗
║         GCRF Library Management System - Rollback Script          ║
╚═══════════════════════════════════════════════════════════════════╝
"

    # Determine backup directory
    if [ $# -eq 0 ]; then
        # Interactive mode
        select_backup
    else
        BACKUP_DIR=$1
    fi

    # Validate backup
    validate_backup "$BACKUP_DIR"

    # Confirm rollback
    confirm_rollback "$BACKUP_DIR"

    # Create pre-rollback backup
    create_pre_rollback_backup

    # Execute rollback
    stop_services
    restore_database "$BACKUP_DIR"
    restore_configuration "$BACKUP_DIR"
    restart_services

    # Verify rollback
    verify_rollback

    # Print summary
    print_summary "$BACKUP_DIR"

    log SUCCESS "Rollback completed successfully!"
    exit 0
}

# Run main function with all arguments
main "$@"
