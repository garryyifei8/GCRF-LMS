#!/bin/bash
#
# GCRF Library Management System - Stack Shutdown Script
#
# Purpose: Gracefully shutdown the complete GCRF stack with data persistence
#
# Usage: ./stop-stack.sh [options]
#   Options:
#     --force                Force immediate shutdown (no grace period)
#     --remove-volumes       Remove all data volumes (DESTRUCTIVE!)
#     --skip-backup          Skip data backup before shutdown
#     --services-only        Only stop application services
#     --infrastructure-only  Only stop infrastructure
#     --timeout <seconds>    Override grace period (default: 30 for services, 60 for infra)
#     --verbose              Enable verbose output
#     --help                 Display this help message
#
# Exit codes:
#   0 - Success
#   1 - General error
#   2 - Backup failed
#   3 - Service shutdown failed
#   4 - Infrastructure shutdown failed

set -e

# Color codes for output
readonly COLOR_GREEN='\033[0;32m'
readonly COLOR_YELLOW='\033[1;33m'
readonly COLOR_RED='\033[0;31m'
readonly COLOR_BLUE='\033[0;34m'
readonly COLOR_RESET='\033[0m'

# Script configuration
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly DEPLOYMENT_DIR="$(dirname "$SCRIPT_DIR")"
readonly LOG_DIR="${DEPLOYMENT_DIR}/logs"
readonly TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
readonly SHUTDOWN_LOG="${LOG_DIR}/shutdown_${TIMESTAMP}.log"

# Default values
FORCE_SHUTDOWN=false
REMOVE_VOLUMES=false
SKIP_BACKUP=false
SERVICES_ONLY=false
INFRASTRUCTURE_ONLY=false
SERVICE_TIMEOUT=30
INFRA_TIMEOUT=60
VERBOSE=false

# Redis configuration (from environment or defaults)
readonly REDIS_PASSWORD="${REDIS_PASSWORD:-admin123}"

# Function: Print colored output
print_colored() {
    local color=$1
    shift
    echo -e "${color}$*${COLOR_RESET}"
}

# Function: Print step header
print_step() {
    echo ""
    print_colored "$COLOR_BLUE" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    print_colored "$COLOR_BLUE" "$1"
    print_colored "$COLOR_BLUE" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

# Function: Log message to file and optionally to stdout
log_message() {
    local message="[$(date '+%Y-%m-%d %H:%M:%S')] $*"
    echo "$message" >> "$SHUTDOWN_LOG"
    if [ "$VERBOSE" = true ]; then
        echo "$message"
    fi
}

# Function: Display help
show_help() {
    grep '^#' "$0" | head -20 | tail -18 | sed 's/^# //' | sed 's/^#//'
}

# Function: Confirm destructive action
confirm_action() {
    local message=$1
    local response

    print_colored "$COLOR_YELLOW" ""
    print_colored "$COLOR_YELLOW" "⚠️  WARNING: $message"
    print_colored "$COLOR_YELLOW" "This action cannot be undone!"
    echo -n "Are you sure you want to proceed? (yes/no): "
    read -r response

    if [ "$response" != "yes" ]; then
        print_colored "$COLOR_GREEN" "Operation cancelled."
        exit 0
    fi
}

# Function: Get running containers
get_running_containers() {
    docker ps --filter "name=gcrf-" --format "{{.Names}}"
}

# Function: Backup critical data
backup_data() {
    print_step "[Data Backup]"

    log_message "Starting data backup..."

    # Check if backup script exists
    if [ -f "$SCRIPT_DIR/backup-volumes.sh" ]; then
        print_colored "$COLOR_YELLOW" "Creating backup before shutdown..."

        if "$SCRIPT_DIR/backup-volumes.sh" --quick 2>&1 | tee -a "$SHUTDOWN_LOG"; then
            print_colored "$COLOR_GREEN" "✓ Data backup completed"
            log_message "Data backup successful"
        else
            print_colored "$COLOR_RED" "✗ Backup failed"
            log_message "ERROR: Data backup failed"

            if [ "$FORCE_SHUTDOWN" = false ]; then
                print_colored "$COLOR_RED" "Aborting shutdown due to backup failure"
                print_colored "$COLOR_YELLOW" "Use --force to shutdown anyway"
                exit 2
            else
                print_colored "$COLOR_YELLOW" "⚠ Continuing with shutdown (--force specified)"
            fi
        fi
    else
        print_colored "$COLOR_YELLOW" "⚠ Backup script not found, skipping backup"
        log_message "WARNING: Backup script not found"
    fi
}

# Function: Flush data stores
flush_data_stores() {
    print_step "[Data Persistence]"

    log_message "Flushing data stores..."

    # Flush Redis
    if docker ps --format "{{.Names}}" | grep -q "gcrf-redis-master"; then
        print_colored "$COLOR_YELLOW" "Flushing Redis data to disk..."
        if docker exec gcrf-redis-master redis-cli -a "$REDIS_PASSWORD" BGSAVE 2>/dev/null; then
            print_colored "$COLOR_GREEN" "✓ Redis data saved"
            log_message "Redis BGSAVE completed"

            # Wait for background save to complete
            print_colored "$COLOR_YELLOW" "Waiting for Redis background save..."
            sleep 3

            # Check lastsave time
            local lastsave=$(docker exec gcrf-redis-master redis-cli -a "$REDIS_PASSWORD" LASTSAVE 2>/dev/null)
            if [ -n "$lastsave" ]; then
                print_colored "$COLOR_GREEN" "✓ Redis last save: $(date -r "$lastsave" 2>/dev/null || echo "$lastsave")"
                log_message "Redis last save timestamp: $lastsave"
            fi
        else
            print_colored "$COLOR_YELLOW" "⚠ Could not save Redis data (container may be stopped)"
            log_message "WARNING: Redis BGSAVE failed or container not running"
        fi
    fi

    # Checkpoint PostgreSQL
    if docker ps --format "{{.Names}}" | grep -q "gcrf-postgres-primary"; then
        print_colored "$COLOR_YELLOW" "Creating PostgreSQL checkpoint..."
        if docker exec gcrf-postgres-primary su - postgres -c "psql -c 'CHECKPOINT;'" 2>/dev/null; then
            print_colored "$COLOR_GREEN" "✓ PostgreSQL checkpoint created"
            log_message "PostgreSQL CHECKPOINT completed"
        else
            print_colored "$COLOR_YELLOW" "⚠ Could not create PostgreSQL checkpoint"
            log_message "WARNING: PostgreSQL CHECKPOINT failed or container not running"
        fi
    fi

    # Flush RabbitMQ (sync messages to disk)
    if docker ps --format "{{.Names}}" | grep -q "gcrf-rabbitmq"; then
        print_colored "$COLOR_YELLOW" "Syncing RabbitMQ messages..."
        if docker exec gcrf-rabbitmq rabbitmqctl sync_queue 2>/dev/null; then
            print_colored "$COLOR_GREEN" "✓ RabbitMQ messages synced"
            log_message "RabbitMQ sync completed"
        else
            print_colored "$COLOR_YELLOW" "⚠ Could not sync RabbitMQ (no critical queues)"
            log_message "WARNING: RabbitMQ sync not required or failed"
        fi
    fi
}

# Function: Stop application services
stop_services() {
    print_step "[Stopping Application Services]"

    log_message "Stopping application services with ${SERVICE_TIMEOUT}s grace period..."

    cd "$DEPLOYMENT_DIR"

    # Get list of running service containers
    local running_services=$(docker-compose -f docker-compose.services.yml ps -q 2>/dev/null)

    if [ -z "$running_services" ]; then
        print_colored "$COLOR_YELLOW" "No application services are running"
        log_message "No application services to stop"
        return 0
    fi

    # Stop services with grace period
    print_colored "$COLOR_YELLOW" "Stopping services (timeout: ${SERVICE_TIMEOUT}s)..."

    if [ "$FORCE_SHUTDOWN" = true ]; then
        # Force immediate stop
        if docker-compose -f docker-compose.services.yml kill 2>&1 | tee -a "$SHUTDOWN_LOG"; then
            print_colored "$COLOR_GREEN" "✓ Services stopped forcefully"
            log_message "Services force stopped"
        else
            print_colored "$COLOR_RED" "✗ Failed to force stop services"
            log_message "ERROR: Force stop failed"
            exit 3
        fi
    else
        # Graceful stop with timeout
        if docker-compose -f docker-compose.services.yml down --timeout "$SERVICE_TIMEOUT" 2>&1 | tee -a "$SHUTDOWN_LOG"; then
            print_colored "$COLOR_GREEN" "✓ Services stopped gracefully"
            log_message "Services stopped gracefully"
        else
            print_colored "$COLOR_RED" "✗ Failed to stop services"
            log_message "ERROR: Service shutdown failed"
            exit 3
        fi
    fi
}

# Function: Stop infrastructure services
stop_infrastructure() {
    print_step "[Stopping Infrastructure Services]"

    log_message "Stopping infrastructure with ${INFRA_TIMEOUT}s grace period..."

    cd "$DEPLOYMENT_DIR"

    # Get list of running infrastructure containers
    local running_infra=$(docker-compose -f docker-compose.infrastructure.yml ps -q 2>/dev/null)

    if [ -z "$running_infra" ]; then
        print_colored "$COLOR_YELLOW" "No infrastructure services are running"
        log_message "No infrastructure services to stop"
        return 0
    fi

    # Stop infrastructure with longer grace period
    print_colored "$COLOR_YELLOW" "Stopping infrastructure (timeout: ${INFRA_TIMEOUT}s)..."

    if [ "$FORCE_SHUTDOWN" = true ]; then
        # Force immediate stop
        if docker-compose -f docker-compose.infrastructure.yml kill 2>&1 | tee -a "$SHUTDOWN_LOG"; then
            print_colored "$COLOR_GREEN" "✓ Infrastructure stopped forcefully"
            log_message "Infrastructure force stopped"
        else
            print_colored "$COLOR_RED" "✗ Failed to force stop infrastructure"
            log_message "ERROR: Infrastructure force stop failed"
            exit 4
        fi
    else
        # Graceful stop with timeout
        if docker-compose -f docker-compose.infrastructure.yml down --timeout "$INFRA_TIMEOUT" 2>&1 | tee -a "$SHUTDOWN_LOG"; then
            print_colored "$COLOR_GREEN" "✓ Infrastructure stopped gracefully"
            log_message "Infrastructure stopped gracefully"
        else
            print_colored "$COLOR_RED" "✗ Failed to stop infrastructure"
            log_message "ERROR: Infrastructure shutdown failed"
            exit 4
        fi
    fi
}

# Function: Remove volumes
remove_volumes() {
    print_step "[Volume Removal]"

    confirm_action "This will delete ALL data volumes including databases!"

    log_message "Removing all volumes..."

    cd "$DEPLOYMENT_DIR"

    print_colored "$COLOR_RED" "Removing all data volumes..."

    # Remove volumes from both compose files
    docker-compose -f docker-compose.infrastructure.yml down -v 2>&1 | tee -a "$SHUTDOWN_LOG"
    docker-compose -f docker-compose.services.yml down -v 2>&1 | tee -a "$SHUTDOWN_LOG"

    # List remaining volumes
    local remaining_volumes=$(docker volume ls --filter "name=gcrf" --format "{{.Name}}")

    if [ -n "$remaining_volumes" ]; then
        print_colored "$COLOR_YELLOW" "Removing remaining volumes:"
        echo "$remaining_volumes"
        echo "$remaining_volumes" | xargs -r docker volume rm 2>&1 | tee -a "$SHUTDOWN_LOG"
    fi

    print_colored "$COLOR_GREEN" "✓ All volumes removed"
    log_message "All volumes removed"
}

# Function: Verify shutdown
verify_shutdown() {
    print_step "[Shutdown Verification]"

    log_message "Verifying shutdown..."

    # Check for remaining containers
    local remaining=$(get_running_containers)

    if [ -z "$remaining" ]; then
        print_colored "$COLOR_GREEN" "✓ All containers stopped successfully"
        log_message "All containers stopped"
    else
        print_colored "$COLOR_YELLOW" "⚠ Some containers are still running:"
        echo "$remaining"
        log_message "WARNING: Containers still running: $remaining"

        if [ "$FORCE_SHUTDOWN" = true ]; then
            print_colored "$COLOR_YELLOW" "Force killing remaining containers..."
            echo "$remaining" | xargs -r docker kill 2>&1 | tee -a "$SHUTDOWN_LOG"
        fi
    fi

    # Show resource cleanup
    echo ""
    print_colored "$COLOR_BLUE" "Resource Status:"

    # Networks
    local networks=$(docker network ls --filter "name=gcrf" --format "{{.Name}}" | wc -l)
    print_colored "$COLOR_GREEN" "  Networks: $networks remaining"

    # Volumes (if not removed)
    if [ "$REMOVE_VOLUMES" = false ]; then
        local volumes=$(docker volume ls --filter "name=gcrf" --format "{{.Name}}" | wc -l)
        print_colored "$COLOR_GREEN" "  Volumes: $volumes preserved"
    fi

    # Images
    local images=$(docker images --filter "reference=gcrf-*" --format "{{.Repository}}:{{.Tag}}" | wc -l)
    print_colored "$COLOR_GREEN" "  Images: $images cached"
}

# Function: Generate shutdown report
generate_report() {
    print_step "[Shutdown Complete]"

    local end_time=$(date '+%Y-%m-%d %H:%M:%S')

    # Summary
    print_colored "$COLOR_GREEN" ""
    print_colored "$COLOR_GREEN" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    print_colored "$COLOR_GREEN" "✅ GCRF Stack Shutdown Complete"
    print_colored "$COLOR_GREEN" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    print_colored "$COLOR_GREEN" "Started at:  $TIMESTAMP"
    print_colored "$COLOR_GREEN" "Completed:   $end_time"
    print_colored "$COLOR_GREEN" "Log file:    $SHUTDOWN_LOG"

    if [ "$REMOVE_VOLUMES" = true ]; then
        print_colored "$COLOR_RED" "⚠ All data volumes were removed"
    else
        print_colored "$COLOR_GREEN" "✓ Data volumes preserved"
    fi

    log_message "Stack shutdown completed successfully at $end_time"
}

# Function: Cleanup on error
cleanup_on_error() {
    local exit_code=$?
    if [ $exit_code -ne 0 ]; then
        print_colored "$COLOR_RED" ""
        print_colored "$COLOR_RED" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        print_colored "$COLOR_RED" "✗ Shutdown failed with exit code: $exit_code"
        print_colored "$COLOR_RED" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        print_colored "$COLOR_RED" "Check log file for details: $SHUTDOWN_LOG"

        log_message "Shutdown failed with exit code: $exit_code"
    fi
}

# Main execution
main() {
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --force)
                FORCE_SHUTDOWN=true
                shift
                ;;
            --remove-volumes)
                REMOVE_VOLUMES=true
                shift
                ;;
            --skip-backup)
                SKIP_BACKUP=true
                shift
                ;;
            --services-only)
                SERVICES_ONLY=true
                shift
                ;;
            --infrastructure-only)
                INFRASTRUCTURE_ONLY=true
                shift
                ;;
            --timeout)
                SERVICE_TIMEOUT="$2"
                INFRA_TIMEOUT="$2"
                shift 2
                ;;
            --verbose)
                VERBOSE=true
                shift
                ;;
            --help)
                show_help
                exit 0
                ;;
            *)
                print_colored "$COLOR_RED" "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done

    # Validate conflicting options
    if [ "$SERVICES_ONLY" = true ] && [ "$INFRASTRUCTURE_ONLY" = true ]; then
        print_colored "$COLOR_RED" "Error: Cannot specify both --services-only and --infrastructure-only"
        exit 1
    fi

    # Setup error handling
    trap cleanup_on_error EXIT

    # Create log directory
    mkdir -p "$LOG_DIR"

    # Start logging
    log_message "================================================================"
    log_message "GCRF Stack Shutdown Script Started"
    log_message "================================================================"
    log_message "Options: force=$FORCE_SHUTDOWN, remove_volumes=$REMOVE_VOLUMES, skip_backup=$SKIP_BACKUP"

    # Print banner
    echo ""
    print_colored "$COLOR_BLUE" "╔════════════════════════════════════════╗"
    print_colored "$COLOR_BLUE" "║   GCRF Library Management System       ║"
    print_colored "$COLOR_BLUE" "║   Graceful Shutdown Orchestration      ║"
    print_colored "$COLOR_BLUE" "╚════════════════════════════════════════╝"
    echo ""
    print_colored "$COLOR_YELLOW" "Starting at: $(date '+%Y-%m-%d %H:%M:%S')"

    # Check if any containers are running
    local running_containers=$(get_running_containers)
    if [ -z "$running_containers" ]; then
        print_colored "$COLOR_YELLOW" "No GCRF containers are currently running"
        exit 0
    fi

    # Show current status
    print_colored "$COLOR_BLUE" "Currently running containers:"
    echo "$running_containers" | sed 's/^/  /'

    # Execute shutdown steps
    if [ "$SKIP_BACKUP" = false ] && [ "$FORCE_SHUTDOWN" = false ]; then
        backup_data
    fi

    if [ "$FORCE_SHUTDOWN" = false ]; then
        flush_data_stores
    fi

    if [ "$INFRASTRUCTURE_ONLY" = false ]; then
        stop_services
    fi

    if [ "$SERVICES_ONLY" = false ]; then
        stop_infrastructure
    fi

    if [ "$REMOVE_VOLUMES" = true ]; then
        remove_volumes
    fi

    verify_shutdown
    generate_report

    # Clear trap for successful exit
    trap - EXIT
}

# Run main function
main "$@"