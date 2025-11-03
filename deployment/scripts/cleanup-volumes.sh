#!/bin/bash

################################################################################
# GCRF Library Management System - Volume Cleanup Script
################################################################################
#
# PURPOSE:
#   Clean up old backups, unused volumes, and logs based on retention policy
#   Supports dry-run mode and configurable retention periods
#
# USAGE:
#   ./cleanup-volumes.sh [OPTIONS]
#
# OPTIONS:
#   -b, --backup-dir DIR    Backup directory to clean (default: /data/backups)
#   -r, --retention DAYS    Retention period in days (default: 30)
#   -d, --dry-run           Show what would be deleted without deleting
#   -l, --logs DAYS         Clean logs older than N days
#   -p, --prune             Prune unused Docker volumes (requires confirmation)
#   -f, --force             Skip confirmation prompts
#   -h, --help              Show this help message
#
# RETENTION POLICY:
#   Daily backups:   Keep for 7 days
#   Weekly backups:  Keep for 4 weeks (28 days)
#   Monthly backups: Keep for 12 months (365 days)
#
# EXAMPLES:
#   # Dry-run to see what would be deleted
#   ./cleanup-volumes.sh --dry-run
#
#   # Clean backups older than 30 days
#   ./cleanup-volumes.sh --retention 30
#
#   # Clean logs older than 7 days
#   ./cleanup-volumes.sh --logs 7
#
#   # Prune unused volumes
#   ./cleanup-volumes.sh --prune
#
# EXIT CODES:
#   0  - Success
#   1  - General error
#   2  - Invalid arguments
#   6  - User cancelled
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

BACKUP_DIR="/data/backups"
RETENTION_DAYS=30
DRY_RUN=false
CLEAN_LOGS=false
LOG_RETENTION_DAYS=0
PRUNE_VOLUMES=false
FORCE_CLEANUP=false

# Retention periods for different backup types
DAILY_RETENTION=7      # 7 days
WEEKLY_RETENTION=28    # 4 weeks
MONTHLY_RETENTION=365  # 12 months

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Cleanup statistics
DELETED_BACKUPS=0
FREED_SPACE=0
DELETED_LOGS=0
PRUNED_VOLUMES=0

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

log_dryrun() {
    echo -e "${MAGENTA}[DRY-RUN]${NC} $1"
}

show_help() {
    sed -n '3,40p' "$0" | sed 's/^# //' | sed 's/^#//'
}

human_readable_size() {
    local bytes=$1
    if [ "$bytes" -lt 1024 ]; then
        echo "${bytes}B"
    elif [ "$bytes" -lt 1048576 ]; then
        echo "$((bytes / 1024))KB"
    elif [ "$bytes" -lt 1073741824 ]; then
        echo "$((bytes / 1048576))MB"
    else
        echo "$((bytes / 1073741824))GB"
    fi
}

# ============================================
# Validation Functions
# ============================================

check_dependencies() {
    log_step "PREFLIGHT" "Checking dependencies..."

    # Check if Docker is installed (needed for volume pruning)
    if [ "$PRUNE_VOLUMES" = true ]; then
        if ! command -v docker &> /dev/null; then
            log_error "Docker is not installed"
            exit 2
        fi

        if ! docker info &> /dev/null; then
            log_error "Docker daemon is not running"
            exit 2
        fi
    fi

    log_success "All dependencies satisfied"
}

validate_backup_dir() {
    if [ ! -d "$BACKUP_DIR" ]; then
        log_warn "Backup directory does not exist: ${BACKUP_DIR}"
        return 1
    fi
    return 0
}

# ============================================
# Backup Classification Functions
# ============================================

is_daily_backup() {
    local backup_date=$1
    # Daily backups are any backup
    return 0
}

is_weekly_backup() {
    local backup_date=$1
    # Weekly backups are on Sunday (day 0)
    local day_of_week=$(date -d "$backup_date" +%w 2>/dev/null || date -j -f "%Y%m%d" "$backup_date" +%w 2>/dev/null)
    [ "$day_of_week" = "0" ]
}

is_monthly_backup() {
    local backup_date=$1
    # Monthly backups are on the 1st of the month
    local day_of_month=$(date -d "$backup_date" +%d 2>/dev/null || date -j -f "%Y%m%d" "$backup_date" +%d 2>/dev/null)
    [ "$day_of_month" = "01" ]
}

# ============================================
# Cleanup Functions
# ============================================

cleanup_old_backups() {
    log_step "CLEANUP" "Cleaning up old backups..."

    if ! validate_backup_dir; then
        return 0
    fi

    local current_date=$(date +%s)
    local deleted_count=0
    local freed_bytes=0

    # Find all backup directories
    for backup_path in "$BACKUP_DIR"/*; do
        if [ ! -d "$backup_path" ]; then
            continue
        fi

        local backup_name=$(basename "$backup_path")

        # Extract date from backup directory name (format: YYYYMMDD_HHMMSS or YYYYMMDD)
        local backup_date_str
        if [[ $backup_name =~ ^([0-9]{8})_[0-9]{6}$ ]]; then
            backup_date_str="${BASH_REMATCH[1]}"
        elif [[ $backup_name =~ ^([0-9]{8})$ ]]; then
            backup_date_str="$backup_name"
        else
            log_warn "Skipping invalid backup directory: ${backup_name}"
            continue
        fi

        # Convert backup date to epoch (handle both Linux and macOS)
        local backup_epoch
        if date -d "$backup_date_str" +%s &>/dev/null; then
            backup_epoch=$(date -d "$backup_date_str" +%s)
        elif date -j -f "%Y%m%d" "$backup_date_str" +%s &>/dev/null; then
            backup_epoch=$(date -j -f "%Y%m%d" "$backup_date_str" +%s)
        else
            log_warn "Could not parse date from: ${backup_name}"
            continue
        fi

        local age_days=$(( (current_date - backup_epoch) / 86400 ))

        # Determine retention period based on backup type
        local retention_period=$RETENTION_DAYS

        if is_monthly_backup "$backup_date_str"; then
            retention_period=$MONTHLY_RETENTION
            if [ $age_days -le $retention_period ]; then
                log_info "Keeping monthly backup: ${backup_name} (${age_days} days old)"
                continue
            fi
        elif is_weekly_backup "$backup_date_str"; then
            retention_period=$WEEKLY_RETENTION
            if [ $age_days -le $retention_period ]; then
                log_info "Keeping weekly backup: ${backup_name} (${age_days} days old)"
                continue
            fi
        elif is_daily_backup "$backup_date_str"; then
            retention_period=$DAILY_RETENTION
            if [ $age_days -le $retention_period ]; then
                log_info "Keeping daily backup: ${backup_name} (${age_days} days old)"
                continue
            fi
        fi

        # Calculate size before deletion
        local backup_size=$(du -sb "$backup_path" 2>/dev/null | cut -f1 || echo 0)

        if [ "$DRY_RUN" = true ]; then
            log_dryrun "Would delete backup: ${backup_name} (${age_days} days old, $(human_readable_size $backup_size))"
        else
            log_warn "Deleting old backup: ${backup_name} (${age_days} days old, $(human_readable_size $backup_size))"
            rm -rf "$backup_path"
            deleted_count=$((deleted_count + 1))
            freed_bytes=$((freed_bytes + backup_size))
        fi
    done

    DELETED_BACKUPS=$deleted_count
    FREED_SPACE=$freed_bytes

    if [ "$DRY_RUN" = false ]; then
        log_success "Deleted ${deleted_count} old backups, freed $(human_readable_size $freed_bytes)"
    fi
}

cleanup_old_logs() {
    log_step "LOGS" "Cleaning up old log files..."

    local log_dirs=(
        "/var/log/gcrf"
        "./logs"
        "/data/gcrf/logs"
    )

    local deleted_count=0

    for log_dir in "${log_dirs[@]}"; do
        if [ ! -d "$log_dir" ]; then
            continue
        fi

        log_info "Scanning log directory: ${log_dir}"

        # Find and delete log files older than retention period
        while IFS= read -r -d '' log_file; do
            local log_size=$(du -sb "$log_file" 2>/dev/null | cut -f1 || echo 0)

            if [ "$DRY_RUN" = true ]; then
                log_dryrun "Would delete log: ${log_file} ($(human_readable_size $log_size))"
            else
                log_warn "Deleting old log: ${log_file}"
                rm -f "$log_file"
                deleted_count=$((deleted_count + 1))
            fi
        done < <(find "$log_dir" -type f \( -name "*.log" -o -name "*.log.*" \) -mtime +${LOG_RETENTION_DAYS} -print0 2>/dev/null)
    done

    DELETED_LOGS=$deleted_count

    if [ "$DRY_RUN" = false ] && [ $deleted_count -gt 0 ]; then
        log_success "Deleted ${deleted_count} old log files"
    elif [ $deleted_count -eq 0 ]; then
        log_info "No old log files to delete"
    fi
}

prune_docker_volumes() {
    log_step "PRUNE" "Pruning unused Docker volumes..."

    # List unused volumes
    local unused_volumes=$(docker volume ls -qf dangling=true | grep -v '^$' || true)

    if [ -z "$unused_volumes" ]; then
        log_info "No unused Docker volumes found"
        return 0
    fi

    # Count and calculate size
    local volume_count=$(echo "$unused_volumes" | wc -l)

    echo ""
    echo "Found ${volume_count} unused Docker volumes:"
    echo "-------------------------------------------"
    docker volume ls -f dangling=true
    echo ""

    if [ "$FORCE_CLEANUP" = false ] && [ "$DRY_RUN" = false ]; then
        read -p "Do you want to delete these volumes? (yes/no): " -r
        echo ""

        if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
            log_warn "Volume pruning cancelled by user"
            return 0
        fi
    fi

    if [ "$DRY_RUN" = true ]; then
        log_dryrun "Would prune ${volume_count} unused Docker volumes"
    else
        docker volume prune -f > /dev/null
        PRUNED_VOLUMES=$volume_count
        log_success "Pruned ${volume_count} unused Docker volumes"
    fi
}

list_gcrf_volumes() {
    log_step "VOLUMES" "Listing GCRF Docker volumes..."

    echo ""
    echo "GCRF Docker Volumes:"
    echo "--------------------"

    # List all GCRF volumes with sizes
    docker volume ls --filter "name=gcrf-" --format "table {{.Name}}\t{{.Driver}}\t{{.Mountpoint}}" 2>/dev/null || {
        log_warn "No GCRF volumes found or Docker not available"
        return 0
    }

    echo ""
    log_info "To inspect a volume: docker volume inspect <volume-name>"
}

# ============================================
# Summary Functions
# ============================================

show_summary() {
    echo ""
    echo "============================================"
    echo "  Cleanup Summary"
    echo "============================================"

    if [ "$DRY_RUN" = true ]; then
        echo "Mode: DRY-RUN (no actual deletions)"
    else
        echo "Mode: LIVE (actual deletions performed)"
    fi

    echo ""
    echo "Backups:"
    echo "  - Deleted: ${DELETED_BACKUPS}"
    echo "  - Freed space: $(human_readable_size $FREED_SPACE)"

    if [ "$CLEAN_LOGS" = true ]; then
        echo ""
        echo "Logs:"
        echo "  - Deleted: ${DELETED_LOGS}"
    fi

    if [ "$PRUNE_VOLUMES" = true ]; then
        echo ""
        echo "Volumes:"
        echo "  - Pruned: ${PRUNED_VOLUMES}"
    fi

    echo "============================================"
}

# ============================================
# Main Execution
# ============================================

parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -b|--backup-dir)
                BACKUP_DIR="$2"
                shift 2
                ;;
            -r|--retention)
                RETENTION_DAYS="$2"
                shift 2
                ;;
            -d|--dry-run)
                DRY_RUN=true
                shift
                ;;
            -l|--logs)
                CLEAN_LOGS=true
                LOG_RETENTION_DAYS="$2"
                shift 2
                ;;
            -p|--prune)
                PRUNE_VOLUMES=true
                shift
                ;;
            -f|--force)
                FORCE_CLEANUP=true
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_help
                exit 2
                ;;
        esac
    done
}

main() {
    parse_args "$@"

    echo "============================================"
    echo "  GCRF Volume Cleanup Script"
    echo "============================================"
    echo "Backup Directory: ${BACKUP_DIR}"
    echo "Retention Period: ${RETENTION_DAYS} days"
    if [ "$DRY_RUN" = true ]; then
        echo "Mode: DRY-RUN (preview only)"
    else
        echo "Mode: LIVE (will delete files)"
    fi
    echo "Started: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "============================================"
    echo ""

    # Preflight checks
    check_dependencies

    # List GCRF volumes for reference
    list_gcrf_volumes

    # Cleanup old backups
    cleanup_old_backups

    # Cleanup old logs if requested
    if [ "$CLEAN_LOGS" = true ]; then
        cleanup_old_logs
    fi

    # Prune unused volumes if requested
    if [ "$PRUNE_VOLUMES" = true ]; then
        prune_docker_volumes
    fi

    # Show summary
    show_summary

    if [ "$DRY_RUN" = true ]; then
        echo ""
        log_info "This was a dry-run. Run without --dry-run to actually delete files."
    fi

    exit 0
}

# Execute main function
main "$@"
