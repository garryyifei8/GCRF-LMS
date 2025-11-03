#!/bin/bash

###############################################################################
# Service Rollback Script
# Version: 1.0.0
# Description: Emergency rollback script for Docker services
# Author: GCRF Library Management System Team
# Date: 2025-01-01
###############################################################################

set -euo pipefail

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
DEPLOYMENT_METHOD="${DEPLOYMENT_METHOD:-compose}"  # compose, swarm, or k8s
ROLLBACK_LOG="/var/log/rollback.log"
HEALTH_CHECK_TIMEOUT=60
HEALTH_CHECK_INTERVAL=5

# Function to print colored output
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$ROLLBACK_LOG"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$ROLLBACK_LOG"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$ROLLBACK_LOG"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$ROLLBACK_LOG"
    exit 1
}

log_urgent() {
    echo -e "${RED}[URGENT]${NC} ${YELLOW}$1${NC}" | tee -a "$ROLLBACK_LOG"
}

# Function to display usage
usage() {
    cat << EOF
Usage: $0 [OPTIONS] SERVICE_NAME [TARGET_VERSION]

Emergency rollback script for Docker services.

Arguments:
  SERVICE_NAME      Name of the service to rollback
  TARGET_VERSION    Target version to rollback to (optional, uses 'rollback' tag by default)

Options:
  -m, --method METHOD      Deployment method (compose|swarm|k8s) (default: compose)
  -r, --registry REGISTRY  Docker registry (default: gcrf-library)
  -f, --force             Skip confirmation prompts
  -a, --all               Rollback all services
  --health-check         Perform health check after rollback
  --no-backup            Don't backup current version
  -h, --help             Display this help message

Examples:
  # Quick rollback to previous version
  $0 gateway-service

  # Rollback to specific version
  $0 gateway-service v1.1.0

  # Rollback all services
  $0 --all

  # Kubernetes rollback
  $0 -m k8s gateway-service

Emergency Hotkeys:
  - Press Ctrl+C to abort rollback at any time
  - Run with --force to skip confirmations (use with caution)

EOF
    exit 0
}

# Parse command line arguments
FORCE=false
ROLLBACK_ALL=false
HEALTH_CHECK=true
CREATE_BACKUP=true
SERVICE_NAME=""
TARGET_VERSION=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -m|--method)
            DEPLOYMENT_METHOD="$2"
            shift 2
            ;;
        -r|--registry)
            REGISTRY="$2"
            shift 2
            ;;
        -f|--force)
            FORCE=true
            shift
            ;;
        -a|--all)
            ROLLBACK_ALL=true
            shift
            ;;
        --health-check)
            HEALTH_CHECK=true
            shift
            ;;
        --no-backup)
            CREATE_BACKUP=false
            shift
            ;;
        -h|--help)
            usage
            ;;
        *)
            if [[ -z "$SERVICE_NAME" ]]; then
                SERVICE_NAME="$1"
            elif [[ -z "$TARGET_VERSION" ]]; then
                TARGET_VERSION="$1"
            else
                log_error "Unknown argument: $1"
            fi
            shift
            ;;
    esac
done

# Function to get current version
get_current_version() {
    local service="$1"

    case "$DEPLOYMENT_METHOD" in
        compose)
            docker ps --filter "name=$service" --format "{{.Image}}" | head -1 | cut -d: -f2
            ;;
        swarm)
            docker service inspect "$service" --format '{{.Spec.TaskTemplate.ContainerSpec.Image}}' | cut -d: -f2
            ;;
        k8s)
            kubectl get deployment "$service" -o jsonpath='{.spec.template.spec.containers[0].image}' | cut -d: -f2
            ;;
    esac
}

# Function to list available versions
list_available_versions() {
    local service="$1"
    echo "Available versions for $service:"
    docker images "$REGISTRY/$service" --format "table {{.Tag}}\t{{.CreatedAt}}" | head -20
}

# Function to check if version exists
version_exists() {
    local service="$1"
    local version="$2"
    docker image inspect "$REGISTRY/$service:$version" &>/dev/null
}

# Function to backup current version
backup_current() {
    local service="$1"
    local current_version=$(get_current_version "$service")
    local backup_tag="backup-$(date +%Y%m%d-%H%M%S)"

    if [[ -n "$current_version" ]]; then
        log_info "Backing up current version: $current_version as $backup_tag"
        docker tag "$REGISTRY/$service:$current_version" "$REGISTRY/$service:$backup_tag"
    fi
}

# Function to perform rollback
perform_rollback() {
    local service="$1"
    local target_version="$2"
    local image="$REGISTRY/$service:$target_version"

    log_urgent "Starting rollback of $service to version $target_version"

    # Pull the target image
    log_info "Pulling target image: $image"
    if ! docker pull "$image"; then
        log_error "Failed to pull image: $image"
    fi

    # Perform rollback based on deployment method
    case "$DEPLOYMENT_METHOD" in
        compose)
            rollback_compose "$service" "$image"
            ;;
        swarm)
            rollback_swarm "$service" "$image"
            ;;
        k8s)
            rollback_kubernetes "$service" "$image"
            ;;
        *)
            log_error "Unknown deployment method: $DEPLOYMENT_METHOD"
            ;;
    esac
}

# Function to rollback with Docker Compose
rollback_compose() {
    local service="$1"
    local image="$2"

    log_info "Rolling back $service using Docker Compose..."

    # Update the service in docker-compose
    export "${service^^}_IMAGE=$image"

    # Restart the service
    if docker-compose up -d "$service"; then
        log_success "Service restarted with new image"
    else
        log_error "Failed to restart service"
    fi
}

# Function to rollback with Docker Swarm
rollback_swarm() {
    local service="$1"
    local image="$2"

    log_info "Rolling back $service using Docker Swarm..."

    if docker service update --image "$image" "$service"; then
        log_success "Service updated with new image"

        # Wait for update to complete
        log_info "Waiting for rollback to complete..."
        docker service ps "$service" --format "table {{.Name}}\t{{.CurrentState}}"
    else
        log_error "Failed to update service"
    fi
}

# Function to rollback with Kubernetes
rollback_kubernetes() {
    local service="$1"
    local image="$2"

    log_info "Rolling back $service using Kubernetes..."

    # Method 1: Use kubectl rollout undo
    if [[ -z "$target_version" ]] || [[ "$target_version" == "rollback" ]]; then
        log_info "Using kubectl rollout undo..."
        if kubectl rollout undo deployment/"$service"; then
            log_success "Rollback initiated"
            kubectl rollout status deployment/"$service"
        else
            log_error "Failed to rollback deployment"
        fi
    else
        # Method 2: Update image directly
        log_info "Updating image to $image..."
        if kubectl set image deployment/"$service" "$service=$image"; then
            log_success "Image updated"
            kubectl rollout status deployment/"$service"
        else
            log_error "Failed to update image"
        fi
    fi
}

# Function to perform health check
perform_health_check() {
    local service="$1"
    local elapsed=0

    log_info "Performing health check for $service..."

    while [[ $elapsed -lt $HEALTH_CHECK_TIMEOUT ]]; do
        case "$DEPLOYMENT_METHOD" in
            compose)
                if docker-compose ps "$service" | grep -q "Up"; then
                    log_success "Service is healthy"
                    return 0
                fi
                ;;
            swarm)
                local replicas=$(docker service ls --filter "name=$service" --format "{{.Replicas}}")
                if [[ "$replicas" =~ ^([0-9]+)/\1$ ]]; then
                    log_success "Service is healthy ($replicas)"
                    return 0
                fi
                ;;
            k8s)
                local ready=$(kubectl get deployment "$service" -o jsonpath='{.status.conditions[?(@.type=="Available")].status}')
                if [[ "$ready" == "True" ]]; then
                    log_success "Service is healthy"
                    return 0
                fi
                ;;
        esac

        log_info "Waiting for service to be healthy... ($elapsed/$HEALTH_CHECK_TIMEOUT seconds)"
        sleep $HEALTH_CHECK_INTERVAL
        elapsed=$((elapsed + HEALTH_CHECK_INTERVAL))
    done

    log_warning "Health check timed out after $HEALTH_CHECK_TIMEOUT seconds"
    return 1
}

# Function to rollback all services
rollback_all_services() {
    local services=(
        "gateway-service"
        "auth-service"
        "book-service"
        "circulation-service"
        "reader-service"
        "notification-service"
    )

    log_urgent "Rolling back ALL services!"

    for service in "${services[@]}"; do
        echo ""
        log_info "Rolling back $service..."
        perform_rollback "$service" "rollback"

        if [[ "$HEALTH_CHECK" == "true" ]]; then
            perform_health_check "$service"
        fi
    done
}

# Main execution
main() {
    # Create log directory if needed
    mkdir -p "$(dirname "$ROLLBACK_LOG")"

    # Log rollback start
    echo "" | tee -a "$ROLLBACK_LOG"
    echo "=" | tee -a "$ROLLBACK_LOG"
    log_urgent "EMERGENCY ROLLBACK INITIATED - $(date)"
    echo "=" | tee -a "$ROLLBACK_LOG"

    # Check if rolling back all services
    if [[ "$ROLLBACK_ALL" == "true" ]]; then
        if [[ "$FORCE" != "true" ]]; then
            echo -e "${RED}WARNING:${NC} This will rollback ALL services!"
            read -p "Are you sure? Type 'yes' to continue: " confirmation
            if [[ "$confirmation" != "yes" ]]; then
                log_info "Rollback cancelled"
                exit 0
            fi
        fi

        rollback_all_services
        log_success "All services rolled back successfully!"
        exit 0
    fi

    # Validate service name
    if [[ -z "$SERVICE_NAME" ]]; then
        log_error "Service name is required"
    fi

    # Get current version
    local current_version=$(get_current_version "$SERVICE_NAME")
    if [[ -n "$current_version" ]]; then
        log_info "Current version: $current_version"
    fi

    # Determine target version
    if [[ -z "$TARGET_VERSION" ]]; then
        TARGET_VERSION="rollback"
        log_info "No target version specified, using 'rollback' tag"
    fi

    # Check if target version exists
    if ! version_exists "$SERVICE_NAME" "$TARGET_VERSION"; then
        log_warning "Target version '$TARGET_VERSION' not found"
        list_available_versions "$SERVICE_NAME"

        if [[ "$FORCE" != "true" ]]; then
            read -p "Enter target version: " TARGET_VERSION
            if ! version_exists "$SERVICE_NAME" "$TARGET_VERSION"; then
                log_error "Version '$TARGET_VERSION' does not exist"
            fi
        else
            log_error "Cannot proceed without valid target version"
        fi
    fi

    # Confirmation prompt
    if [[ "$FORCE" != "true" ]]; then
        echo ""
        echo -e "${YELLOW}Rollback Summary:${NC}"
        echo "  Service: $SERVICE_NAME"
        echo "  Current: $current_version"
        echo "  Target:  $TARGET_VERSION"
        echo "  Method:  $DEPLOYMENT_METHOD"
        echo ""
        read -p "Proceed with rollback? (y/n): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "Rollback cancelled"
            exit 0
        fi
    fi

    # Create backup if requested
    if [[ "$CREATE_BACKUP" == "true" ]]; then
        backup_current "$SERVICE_NAME"
    fi

    # Perform rollback
    perform_rollback "$SERVICE_NAME" "$TARGET_VERSION"

    # Health check
    if [[ "$HEALTH_CHECK" == "true" ]]; then
        if perform_health_check "$SERVICE_NAME"; then
            log_success "Rollback completed successfully!"
        else
            log_warning "Rollback completed but health check failed"
            log_info "Please verify service manually"
        fi
    else
        log_success "Rollback completed!"
    fi

    # Log final status
    echo ""
    log_info "Final Status:"
    case "$DEPLOYMENT_METHOD" in
        compose)
            docker-compose ps "$SERVICE_NAME"
            ;;
        swarm)
            docker service ps "$SERVICE_NAME" --format "table {{.Name}}\t{{.CurrentState}}"
            ;;
        k8s)
            kubectl get deployment "$SERVICE_NAME"
            kubectl get pods -l app="$SERVICE_NAME"
            ;;
    esac
}

# Trap for cleanup on exit
trap 'echo "Rollback interrupted!"; exit 130' INT TERM

# Ensure we're running with appropriate permissions
if [[ "$DEPLOYMENT_METHOD" == "k8s" ]] && ! command -v kubectl &>/dev/null; then
    log_error "kubectl not found. Please install kubectl for Kubernetes rollback."
fi

if ! command -v docker &>/dev/null; then
    log_error "docker not found. Docker is required for rollback."
fi

# Run main function
main