#!/bin/bash

################################################################################
# GCRF Library Management System - Volume Status Script
################################################################################
#
# PURPOSE:
#   Monitor Docker volumes, disk space, and backup status
#   Provides comprehensive overview of storage health
#
# USAGE:
#   ./volume-status.sh [OPTIONS]
#
# OPTIONS:
#   -b, --backup-dir DIR    Backup directory to check (default: /data/backups)
#   -w, --warn PERCENT      Warning threshold percentage (default: 80)
#   -c, --critical PERCENT  Critical threshold percentage (default: 90)
#   -j, --json              Output in JSON format
#   -a, --alerts            Check and report alerts only
#   -h, --help              Show this help message
#
# EXAMPLES:
#   # Show volume status
#   ./volume-status.sh
#
#   # Check with custom thresholds
#   ./volume-status.sh --warn 70 --critical 85
#
#   # JSON output for monitoring integration
#   ./volume-status.sh --json
#
#   # Only show alerts
#   ./volume-status.sh --alerts
#
# EXIT CODES:
#   0  - All volumes healthy
#   1  - Warning threshold exceeded
#   2  - Critical threshold exceeded
#   3  - Docker not available
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
WARN_THRESHOLD=80
CRITICAL_THRESHOLD=90
JSON_OUTPUT=false
ALERTS_ONLY=false

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Volume names
declare -a CRITICAL_VOLUMES=(
    "gcrf-postgres-primary-data"
    "gcrf-nacos-mysql-data"
)

declare -a IMPORTANT_VOLUMES=(
    "gcrf-redis-master-data"
    "gcrf-rabbitmq-data"
    "gcrf-minio-data"
)

# Status tracking
ALERT_COUNT=0
WARNING_COUNT=0
CRITICAL_COUNT=0

# ============================================
# Utility Functions
# ============================================

log_info() {
    if [ "$JSON_OUTPUT" = false ] && [ "$ALERTS_ONLY" = false ]; then
        echo -e "${BLUE}[INFO]${NC} $1"
    fi
}

log_success() {
    if [ "$JSON_OUTPUT" = false ] && [ "$ALERTS_ONLY" = false ]; then
        echo -e "${GREEN}[OK]${NC} $1"
    fi
}

log_warn() {
    if [ "$JSON_OUTPUT" = false ]; then
        echo -e "${YELLOW}[WARN]${NC} $1"
    fi
}

log_error() {
    if [ "$JSON_OUTPUT" = false ]; then
        echo -e "${RED}[CRITICAL]${NC} $1" >&2
    fi
}

show_help() {
    sed -n '3,35p' "$0" | sed 's/^# //' | sed 's/^#//'
}

human_readable_size() {
    local bytes=$1
    numfmt --to=iec-i --suffix=B "$bytes" 2>/dev/null || {
        # Fallback for systems without numfmt
        if [ "$bytes" -lt 1024 ]; then
            echo "${bytes}B"
        elif [ "$bytes" -lt 1048576 ]; then
            echo "$(awk "BEGIN {printf \"%.1f\", $bytes/1024}")KB"
        elif [ "$bytes" -lt 1073741824 ]; then
            echo "$(awk "BEGIN {printf \"%.1f\", $bytes/1048576}")MB"
        else
            echo "$(awk "BEGIN {printf \"%.2f\", $bytes/1073741824}")GB"
        fi
    }
}

# ============================================
# Docker Functions
# ============================================

check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed"
        exit 3
    fi

    if ! docker info &> /dev/null; then
        log_error "Docker daemon is not running"
        exit 3
    fi
}

get_volume_size() {
    local volume_name=$1

    # Get volume mountpoint
    local mountpoint=$(docker volume inspect "$volume_name" --format '{{.Mountpoint}}' 2>/dev/null || echo "")

    if [ -z "$mountpoint" ]; then
        echo "0"
        return
    fi

    # Calculate size (handle both Linux and macOS)
    local size=0
    if [ -d "$mountpoint" ]; then
        size=$(du -sb "$mountpoint" 2>/dev/null | cut -f1 || echo "0")
    fi

    echo "$size"
}

check_volume_exists() {
    local volume_name=$1
    docker volume inspect "$volume_name" &>/dev/null
}

# ============================================
# Disk Space Functions
# ============================================

get_disk_usage() {
    local path=$1

    # Get disk usage for path (works on both Linux and macOS)
    df -P "$path" 2>/dev/null | awk 'NR==2 {print $5}' | sed 's/%//'
}

get_disk_available() {
    local path=$1

    # Get available space in bytes
    df -P "$path" 2>/dev/null | awk 'NR==2 {print $4 * 1024}'
}

get_disk_total() {
    local path=$1

    # Get total space in bytes
    df -P "$path" 2>/dev/null | awk 'NR==2 {print $2 * 1024}'
}

# ============================================
# Backup Functions
# ============================================

get_latest_backup() {
    if [ ! -d "$BACKUP_DIR" ]; then
        echo "none"
        return
    fi

    local latest=$(find "$BACKUP_DIR" -maxdepth 1 -type d -name "[0-9]*" 2>/dev/null | sort -r | head -1)

    if [ -z "$latest" ]; then
        echo "none"
    else
        basename "$latest"
    fi
}

get_backup_age() {
    local backup_name=$1

    if [ "$backup_name" = "none" ]; then
        echo "never"
        return
    fi

    # Extract date from backup name
    local backup_date_str
    if [[ $backup_name =~ ^([0-9]{8})_[0-9]{6}$ ]]; then
        backup_date_str="${BASH_REMATCH[1]}"
    elif [[ $backup_name =~ ^([0-9]{8})$ ]]; then
        backup_date_str="$backup_name"
    else
        echo "unknown"
        return
    fi

    # Convert to epoch and calculate age
    local current_epoch=$(date +%s)
    local backup_epoch

    if date -d "$backup_date_str" +%s &>/dev/null; then
        backup_epoch=$(date -d "$backup_date_str" +%s)
    elif date -j -f "%Y%m%d" "$backup_date_str" +%s &>/dev/null; then
        backup_epoch=$(date -j -f "%Y%m%d" "$backup_date_str" +%s)
    else
        echo "unknown"
        return
    fi

    local age_days=$(( (current_epoch - backup_epoch) / 86400 ))
    echo "${age_days} days ago"
}

count_backups() {
    if [ ! -d "$BACKUP_DIR" ]; then
        echo "0"
        return
    fi

    find "$BACKUP_DIR" -maxdepth 1 -type d -name "[0-9]*" 2>/dev/null | wc -l | tr -d ' '
}

get_backup_total_size() {
    if [ ! -d "$BACKUP_DIR" ]; then
        echo "0"
        return
    fi

    du -sb "$BACKUP_DIR" 2>/dev/null | cut -f1 || echo "0"
}

# ============================================
# Status Display Functions
# ============================================

show_volume_status() {
    log_info "GCRF Docker Volumes"
    echo ""

    printf "%-35s %-12s %-15s %-10s\n" "VOLUME NAME" "STATUS" "SIZE" "PRIORITY"
    printf "%s\n" "--------------------------------------------------------------------------------"

    # Check critical volumes
    for volume in "${CRITICAL_VOLUMES[@]}"; do
        show_single_volume_status "$volume" "CRITICAL"
    done

    # Check important volumes
    for volume in "${IMPORTANT_VOLUMES[@]}"; do
        show_single_volume_status "$volume" "IMPORTANT"
    done

    echo ""
}

show_single_volume_status() {
    local volume_name=$1
    local priority=$2

    local status="MISSING"
    local status_color=$RED
    local size="N/A"

    if check_volume_exists "$volume_name"; then
        status="OK"
        status_color=$GREEN
        local size_bytes=$(get_volume_size "$volume_name")
        size=$(human_readable_size "$size_bytes")
    else
        ALERT_COUNT=$((ALERT_COUNT + 1))
        WARNING_COUNT=$((WARNING_COUNT + 1))
    fi

    if [ "$JSON_OUTPUT" = false ]; then
        printf "%-35s " "$volume_name"
        echo -e "${status_color}%-12s${NC}" "$status" | tr '\n' ' '
        printf "%-15s %-10s\n" "$size" "$priority"
    fi
}

show_disk_space() {
    log_info "Disk Space Status"
    echo ""

    printf "%-30s %-12s %-12s %-12s %-8s\n" "MOUNT POINT" "TOTAL" "USED" "AVAILABLE" "USAGE %"
    printf "%s\n" "--------------------------------------------------------------------------------"

    # Check Docker data directory
    local docker_root=$(docker info --format '{{.DockerRootDir}}' 2>/dev/null || echo "/var/lib/docker")
    show_disk_space_line "$docker_root" "Docker Root"

    # Check backup directory if exists
    if [ -d "$BACKUP_DIR" ]; then
        show_disk_space_line "$BACKUP_DIR" "Backup Dir"
    fi

    echo ""
}

show_disk_space_line() {
    local path=$1
    local label=$2

    if [ ! -d "$path" ]; then
        return
    fi

    local total=$(get_disk_total "$path")
    local available=$(get_disk_available "$path")
    local used=$((total - available))
    local usage_percent=$(get_disk_usage "$path")

    local status_color=$GREEN
    if [ "$usage_percent" -ge "$CRITICAL_THRESHOLD" ]; then
        status_color=$RED
        ALERT_COUNT=$((ALERT_COUNT + 1))
        CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
    elif [ "$usage_percent" -ge "$WARN_THRESHOLD" ]; then
        status_color=$YELLOW
        ALERT_COUNT=$((ALERT_COUNT + 1))
        WARNING_COUNT=$((WARNING_COUNT + 1))
    fi

    if [ "$JSON_OUTPUT" = false ]; then
        printf "%-30s %-12s %-12s %-12s " \
            "$label" \
            "$(human_readable_size "$total")" \
            "$(human_readable_size "$used")" \
            "$(human_readable_size "$available")"
        echo -e "${status_color}%-8s${NC}" "${usage_percent}%"
    fi

    # Log alerts
    if [ "$usage_percent" -ge "$CRITICAL_THRESHOLD" ]; then
        log_error "CRITICAL: ${label} at ${usage_percent}% (threshold: ${CRITICAL_THRESHOLD}%)"
    elif [ "$usage_percent" -ge "$WARN_THRESHOLD" ]; then
        log_warn "WARNING: ${label} at ${usage_percent}% (threshold: ${WARN_THRESHOLD}%)"
    fi
}

show_backup_status() {
    log_info "Backup Status"
    echo ""

    local latest_backup=$(get_latest_backup)
    local backup_age=$(get_backup_age "$latest_backup")
    local backup_count=$(count_backups)
    local backup_size=$(get_backup_total_size)

    printf "%-30s : %s\n" "Latest Backup" "$latest_backup"
    printf "%-30s : %s\n" "Backup Age" "$backup_age"
    printf "%-30s : %s\n" "Total Backups" "$backup_count"
    printf "%-30s : %s\n" "Total Backup Size" "$(human_readable_size "$backup_size")"

    # Alert if no recent backup for critical volumes
    if [ "$latest_backup" = "none" ]; then
        ALERT_COUNT=$((ALERT_COUNT + 1))
        WARNING_COUNT=$((WARNING_COUNT + 1))
        log_warn "No backups found in ${BACKUP_DIR}"
    elif [[ "$backup_age" =~ ^([0-9]+) ]]; then
        local age_days="${BASH_REMATCH[1]}"
        if [ "$age_days" -gt 7 ]; then
            ALERT_COUNT=$((ALERT_COUNT + 1))
            WARNING_COUNT=$((WARNING_COUNT + 1))
            log_warn "Latest backup is ${age_days} days old (recommended: daily backups)"
        fi
    fi

    echo ""
}

show_container_status() {
    log_info "Container Status"
    echo ""

    printf "%-30s %-12s %-20s\n" "CONTAINER" "STATUS" "HEALTH"
    printf "%s\n" "--------------------------------------------------------------------------------"

    local containers=(
        "gcrf-postgres-primary"
        "gcrf-redis-master"
        "gcrf-nacos-mysql"
        "gcrf-nacos"
        "gcrf-rabbitmq"
        "gcrf-minio"
    )

    for container in "${containers[@]}"; do
        show_single_container_status "$container"
    done

    echo ""
}

show_single_container_status() {
    local container=$1

    local status="not running"
    local health="N/A"
    local status_color=$YELLOW

    if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        status="running"
        status_color=$GREEN

        # Get health status
        health=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "N/A")
        if [ "$health" = "healthy" ]; then
            health="healthy"
        elif [ "$health" = "unhealthy" ]; then
            health="unhealthy"
            status_color=$RED
            ALERT_COUNT=$((ALERT_COUNT + 1))
            CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
        elif [ "$health" = "starting" ]; then
            health="starting"
        fi
    else
        ALERT_COUNT=$((ALERT_COUNT + 1))
        WARNING_COUNT=$((WARNING_COUNT + 1))
    fi

    if [ "$JSON_OUTPUT" = false ]; then
        printf "%-30s " "$container"
        echo -e "${status_color}%-12s${NC} %-20s" "$status" "$health"
    fi

    # Log alerts
    if [ "$status" != "running" ]; then
        log_warn "Container ${container} is not running"
    elif [ "$health" = "unhealthy" ]; then
        log_error "Container ${container} is unhealthy"
    fi
}

# ============================================
# JSON Output Functions
# ============================================

output_json() {
    local latest_backup=$(get_latest_backup)
    local backup_age=$(get_backup_age "$latest_backup")
    local backup_count=$(count_backups)
    local backup_size=$(get_backup_total_size)

    local docker_root=$(docker info --format '{{.DockerRootDir}}' 2>/dev/null || echo "/var/lib/docker")
    local docker_usage=$(get_disk_usage "$docker_root")

    cat <<EOF
{
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "status": "$([ $CRITICAL_COUNT -eq 0 ] && [ $WARNING_COUNT -eq 0 ] && echo "healthy" || [ $CRITICAL_COUNT -gt 0 ] && echo "critical" || echo "warning")",
  "alerts": {
    "total": $ALERT_COUNT,
    "warnings": $WARNING_COUNT,
    "critical": $CRITICAL_COUNT
  },
  "disk_space": {
    "docker_root": {
      "path": "$docker_root",
      "usage_percent": $docker_usage,
      "status": "$([ $docker_usage -ge $CRITICAL_THRESHOLD ] && echo "critical" || [ $docker_usage -ge $WARN_THRESHOLD ] && echo "warning" || echo "ok")"
    }
  },
  "backups": {
    "latest": "$latest_backup",
    "age": "$backup_age",
    "count": $backup_count,
    "total_size_bytes": $backup_size
  }
}
EOF
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
            -w|--warn)
                WARN_THRESHOLD="$2"
                shift 2
                ;;
            -c|--critical)
                CRITICAL_THRESHOLD="$2"
                shift 2
                ;;
            -j|--json)
                JSON_OUTPUT=true
                shift
                ;;
            -a|--alerts)
                ALERTS_ONLY=true
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

    # Check Docker availability
    check_docker

    if [ "$JSON_OUTPUT" = true ]; then
        # Gather data silently
        show_volume_status > /dev/null 2>&1
        show_disk_space > /dev/null 2>&1
        show_backup_status > /dev/null 2>&1
        show_container_status > /dev/null 2>&1

        # Output JSON
        output_json
    else
        # Normal output
        if [ "$ALERTS_ONLY" = false ]; then
            echo "============================================"
            echo "  GCRF Volume Status Monitor"
            echo "============================================"
            echo "Scan Time: $(date '+%Y-%m-%d %H:%M:%S')"
            echo "Warning Threshold: ${WARN_THRESHOLD}%"
            echo "Critical Threshold: ${CRITICAL_THRESHOLD}%"
            echo "============================================"
            echo ""
        fi

        show_volume_status
        show_disk_space
        show_backup_status
        show_container_status

        # Summary
        if [ "$ALERTS_ONLY" = false ]; then
            echo "============================================"
            echo "  Summary"
            echo "============================================"
            printf "Total Alerts: %d (Warnings: %d, Critical: %d)\n" \
                "$ALERT_COUNT" "$WARNING_COUNT" "$CRITICAL_COUNT"
            echo "============================================"
            echo ""
        fi

        if [ $ALERT_COUNT -eq 0 ]; then
            log_success "All systems healthy"
        elif [ $CRITICAL_COUNT -gt 0 ]; then
            log_error "Critical issues detected - immediate action required"
        else
            log_warn "Warnings detected - review recommended"
        fi
    fi

    # Exit with appropriate code
    if [ $CRITICAL_COUNT -gt 0 ]; then
        exit 2
    elif [ $WARNING_COUNT -gt 0 ]; then
        exit 1
    else
        exit 0
    fi
}

# Execute main function
main "$@"
