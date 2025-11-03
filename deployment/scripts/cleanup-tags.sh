#!/bin/bash

###############################################################################
# Docker Image Tag Cleanup Script
# Version: 1.0.0
# Description: Clean up old Docker image tags based on retention policy
# Author: GCRF Library Management System Team
# Date: 2025-01-01
###############################################################################

set -euo pipefail

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${CONFIG_FILE:-$SCRIPT_DIR/../config/tag-retention.yaml}"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
REGISTRY="${DOCKER_REGISTRY:-gcrf-library}"
DRY_RUN="${DRY_RUN:-true}"  # Default to dry-run for safety
VERBOSE="${VERBOSE:-false}"
LOG_FILE="${LOG_FILE:-/var/log/docker-cleanup.log}"
AUDIT_LOG="${AUDIT_LOG:-/var/log/docker-cleanup-audit.log}"

# Statistics
IMAGES_CHECKED=0
IMAGES_DELETED=0
SPACE_FREED=0
ERRORS=0

# Function to print colored output
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$LOG_FILE"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
    ((ERRORS++))
}

log_debug() {
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${MAGENTA}[DEBUG]${NC} $1" | tee -a "$LOG_FILE"
    fi
}

log_audit() {
    local action="$1"
    local image="$2"
    local reason="$3"
    local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

    echo "{\"timestamp\":\"$timestamp\",\"action\":\"$action\",\"image\":\"$image\",\"reason\":\"$reason\"}" >> "$AUDIT_LOG"
}

# Function to display usage
usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Clean up old Docker image tags based on retention policy.

Options:
  -c, --config FILE     Path to retention policy config (default: ../config/tag-retention.yaml)
  -r, --registry REG    Docker registry (default: gcrf-library)
  -s, --service SERVICE Only clean specific service
  -e, --env ENV        Only clean specific environment (dev|staging|prod)
  -f, --force          Actually delete images (disables dry-run)
  -v, --verbose        Enable verbose output
  -l, --log FILE       Log file path (default: /var/log/docker-cleanup.log)
  --max-delete N       Maximum number of images to delete
  --min-free-space GB  Stop when this much free space is available
  -h, --help          Display this help message

Safety Features:
  - Dry-run by default (use -f to actually delete)
  - Protected tags are never deleted
  - Audit log of all deletions
  - Rate limiting to prevent mass deletion

Examples:
  # Dry run to see what would be deleted
  $0

  # Actually delete old images
  $0 --force

  # Clean only development images
  $0 --env dev --force

  # Clean specific service with verbose output
  $0 --service gateway-service -v

  # Clean until 100GB free space available
  $0 --min-free-space 100 --force

Environment Variables:
  DOCKER_REGISTRY    Default registry (default: gcrf-library)
  DRY_RUN           Set to false to actually delete
  VERBOSE           Set to true for verbose output
  LOG_FILE          Path to log file
  AUDIT_LOG         Path to audit log
EOF
    exit 0
}

# Parse command line arguments
SERVICE_FILTER=""
ENV_FILTER=""
MAX_DELETE=""
MIN_FREE_SPACE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -c|--config)
            CONFIG_FILE="$2"
            shift 2
            ;;
        -r|--registry)
            REGISTRY="$2"
            shift 2
            ;;
        -s|--service)
            SERVICE_FILTER="$2"
            shift 2
            ;;
        -e|--env)
            ENV_FILTER="$2"
            shift 2
            ;;
        -f|--force)
            DRY_RUN=false
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -l|--log)
            LOG_FILE="$2"
            shift 2
            ;;
        --max-delete)
            MAX_DELETE="$2"
            shift 2
            ;;
        --min-free-space)
            MIN_FREE_SPACE="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        *)
            log_error "Unknown option: $1"
            usage
            ;;
    esac
done

# Create log directories if they don't exist
mkdir -p "$(dirname "$LOG_FILE")"
mkdir -p "$(dirname "$AUDIT_LOG")"

# Function to get image age in days
get_image_age_days() {
    local image="$1"
    local created=$(docker inspect -f '{{.Created}}' "$image" 2>/dev/null || echo "")

    if [[ -z "$created" ]]; then
        echo "999999"  # Return large number if can't determine age
        return
    fi

    local created_timestamp=$(date -d "$created" +%s 2>/dev/null || date -j -f "%Y-%m-%dT%H:%M:%S" "${created%%.*}" +%s 2>/dev/null || echo 0)
    local current_timestamp=$(date +%s)
    local age_seconds=$((current_timestamp - created_timestamp))
    local age_days=$((age_seconds / 86400))

    echo "$age_days"
}

# Function to get image size
get_image_size() {
    local image="$1"
    docker inspect -f '{{.Size}}' "$image" 2>/dev/null || echo "0"
}

# Function to convert bytes to human-readable format
format_size() {
    local bytes="$1"
    if [[ $bytes -lt 1024 ]]; then
        echo "${bytes}B"
    elif [[ $bytes -lt 1048576 ]]; then
        echo "$((bytes / 1024))KB"
    elif [[ $bytes -lt 1073741824 ]]; then
        echo "$((bytes / 1048576))MB"
    else
        echo "$((bytes / 1073741824))GB"
    fi
}

# Function to check if tag matches protected pattern
is_protected_tag() {
    local tag="$1"

    # List of protected patterns (hardcoded for safety)
    local protected_patterns=(
        "^latest$"
        "^stable$"
        "^prod$"
        "^prod-stable$"
        "^v[0-9]+\.[0-9]+\.[0-9]+$"
        "^prod-[0-9]+\.[0-9]+\.[0-9]+$"
        "^lts-.*"
    )

    for pattern in "${protected_patterns[@]}"; do
        if [[ "$tag" =~ $pattern ]]; then
            return 0
        fi
    done

    return 1
}

# Function to determine if image should be deleted based on retention rules
should_delete_image() {
    local image="$1"
    local tag="${image##*:}"
    local age_days=$(get_image_age_days "$image")

    # Check if protected
    if is_protected_tag "$tag"; then
        log_debug "Protected tag: $tag"
        return 1
    fi

    # Development tags (30 days)
    if [[ "$tag" =~ ^dev- ]] && [[ $age_days -gt 30 ]]; then
        return 0
    fi

    # Feature/bugfix branches (14 days)
    if [[ "$tag" =~ ^(feature|bugfix)- ]] && [[ $age_days -gt 14 ]]; then
        return 0
    fi

    # Pull request tags (14 days)
    if [[ "$tag" =~ ^pr- ]] && [[ $age_days -gt 14 ]]; then
        return 0
    fi

    # Nightly builds (7 days)
    if [[ "$tag" =~ ^nightly- ]] && [[ $age_days -gt 7 ]]; then
        return 0
    fi

    # Git SHA tags (60 days)
    if [[ "$tag" =~ ^git-[a-f0-9]{7}$ ]] && [[ $age_days -gt 60 ]]; then
        return 0
    fi

    # Staging tags (90 days)
    if [[ "$tag" =~ ^staging- ]] && [[ $age_days -gt 90 ]]; then
        return 0
    fi

    # Alpha/Beta releases (30/60 days)
    if [[ "$tag" =~ -alpha\.[0-9]+$ ]] && [[ $age_days -gt 30 ]]; then
        return 0
    fi
    if [[ "$tag" =~ -beta\.[0-9]+$ ]] && [[ $age_days -gt 60 ]]; then
        return 0
    fi

    # Dirty builds (1 day)
    if [[ "$tag" =~ ^dirty- ]] && [[ $age_days -gt 1 ]]; then
        return 0
    fi

    # Edge builds (3 days)
    if [[ "$tag" =~ ^edge ]] && [[ $age_days -gt 3 ]]; then
        return 0
    fi

    # Test/QA tags (14 days)
    if [[ "$tag" =~ ^(test|qa)- ]] && [[ $age_days -gt 14 ]]; then
        return 0
    fi

    # Default: don't delete
    return 1
}

# Function to delete an image
delete_image() {
    local image="$1"
    local reason="$2"
    local size=$(get_image_size "$image")
    local size_human=$(format_size "$size")

    if [[ "$DRY_RUN" == "true" ]]; then
        echo -e "${CYAN}[DRY-RUN]${NC} Would delete: $image (${size_human}, reason: $reason)"
        log_audit "DRY_RUN_DELETE" "$image" "$reason"
    else
        log_info "Deleting: $image (${size_human})"
        if docker rmi "$image" 2>/dev/null; then
            log_success "Deleted: $image"
            log_audit "DELETE" "$image" "$reason"
            ((IMAGES_DELETED++))
            SPACE_FREED=$((SPACE_FREED + size))
        else
            log_error "Failed to delete: $image"
            log_audit "DELETE_FAILED" "$image" "Command failed"
        fi
    fi
}

# Function to get disk usage
get_disk_usage() {
    df -h /var/lib/docker 2>/dev/null | awk 'NR==2 {print $4}' | sed 's/G//' || echo "0"
}

# Main cleanup function
cleanup_images() {
    log_info "Starting Docker image cleanup"
    log_info "Registry: $REGISTRY"
    log_info "Dry-run: $DRY_RUN"

    if [[ -n "$SERVICE_FILTER" ]]; then
        log_info "Service filter: $SERVICE_FILTER"
    fi

    if [[ -n "$ENV_FILTER" ]]; then
        log_info "Environment filter: $ENV_FILTER"
    fi

    # Get initial disk space
    local initial_free_space=$(get_disk_usage)
    log_info "Initial free disk space: ${initial_free_space}GB"

    # Get list of images
    local images=()
    if [[ -n "$SERVICE_FILTER" ]]; then
        mapfile -t images < <(docker images "$REGISTRY/$SERVICE_FILTER" --format "{{.Repository}}:{{.Tag}}" | grep -v "<none>")
    else
        mapfile -t images < <(docker images "$REGISTRY/*" --format "{{.Repository}}:{{.Tag}}" | grep -v "<none>")
    fi

    log_info "Found ${#images[@]} images to check"

    # Process each image
    for image in "${images[@]}"; do
        ((IMAGES_CHECKED++))

        # Check rate limiting
        if [[ -n "$MAX_DELETE" ]] && [[ $IMAGES_DELETED -ge $MAX_DELETE ]]; then
            log_warning "Reached maximum deletion limit ($MAX_DELETE)"
            break
        fi

        # Check minimum free space
        if [[ -n "$MIN_FREE_SPACE" ]]; then
            local current_free_space=$(get_disk_usage)
            if [[ $(echo "$current_free_space >= $MIN_FREE_SPACE" | bc -l) -eq 1 ]]; then
                log_info "Reached minimum free space target (${MIN_FREE_SPACE}GB)"
                break
            fi
        fi

        # Apply environment filter
        local tag="${image##*:}"
        if [[ -n "$ENV_FILTER" ]]; then
            case "$ENV_FILTER" in
                dev)
                    [[ ! "$tag" =~ ^(dev|feature|bugfix|pr|nightly|test)- ]] && continue
                    ;;
                staging)
                    [[ ! "$tag" =~ ^(staging|rc|beta)- ]] && continue
                    ;;
                prod)
                    [[ ! "$tag" =~ ^(prod|v[0-9]|stable) ]] && continue
                    ;;
            esac
        fi

        # Check if image should be deleted
        if should_delete_image "$image"; then
            local age_days=$(get_image_age_days "$image")
            delete_image "$image" "Age: ${age_days} days"
        else
            log_debug "Keeping: $image"
        fi
    done

    # Final statistics
    log_info "Cleanup complete!"
    log_info "Images checked: $IMAGES_CHECKED"
    log_info "Images deleted: $IMAGES_DELETED"
    log_info "Space freed: $(format_size $SPACE_FREED)"
    log_info "Errors: $ERRORS"

    # Check final disk space
    local final_free_space=$(get_disk_usage)
    log_info "Final free disk space: ${final_free_space}GB"

    # Send notification if configured
    send_notification
}

# Function to send notification
send_notification() {
    if [[ "$DRY_RUN" == "true" ]]; then
        return
    fi

    local subject="Docker Cleanup Report - $(date +%Y-%m-%d)"
    local message="Docker image cleanup completed.

Images checked: $IMAGES_CHECKED
Images deleted: $IMAGES_DELETED
Space freed: $(format_size $SPACE_FREED)
Errors: $ERRORS"

    # Log the summary
    echo "$message" | tee -a "$LOG_FILE"

    # Send to Slack if webhook is configured
    if [[ -n "${SLACK_WEBHOOK:-}" ]]; then
        curl -X POST "$SLACK_WEBHOOK" \
            -H 'Content-Type: application/json' \
            -d "{\"text\": \"$subject\n\`\`\`$message\`\`\`\"}" \
            2>/dev/null || log_warning "Failed to send Slack notification"
    fi
}

# Function to verify prerequisites
check_prerequisites() {
    # Check if Docker is installed and running
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed"
        exit 1
    fi

    if ! docker info &> /dev/null; then
        log_error "Docker daemon is not running or you don't have permission"
        exit 1
    fi

    # Check if config file exists (optional)
    if [[ -n "$CONFIG_FILE" ]] && [[ ! -f "$CONFIG_FILE" ]]; then
        log_warning "Config file not found: $CONFIG_FILE (using defaults)"
    fi

    # Check write permissions for logs
    if ! touch "$LOG_FILE" 2>/dev/null; then
        log_error "Cannot write to log file: $LOG_FILE"
        exit 1
    fi

    if ! touch "$AUDIT_LOG" 2>/dev/null; then
        log_error "Cannot write to audit log: $AUDIT_LOG"
        exit 1
    fi
}

# Main execution
main() {
    # Check prerequisites
    check_prerequisites

    # Log startup
    log_info "="
    log_info "Docker Image Cleanup Started - $(date)"
    log_info "="

    # Run cleanup
    cleanup_images

    # Exit with error count
    exit $ERRORS
}

# Handle interrupts
trap 'echo "Interrupted! Cleaning up..."; exit 130' INT TERM

# Run main function
main