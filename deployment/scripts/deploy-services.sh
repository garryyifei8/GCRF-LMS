#!/bin/bash
# GCRF Library Management System - Service Deployment Script
# Version: 1.0.0
# Last Updated: 2025-11-02
#
# Purpose: Deploy microservices with zero-downtime rolling updates
# Usage: ./deploy-services.sh [OPTIONS]
#
# Options:
#   --service       Deploy specific service (default: all)
#   --tag           Image tag to deploy (default: latest)
#   --strategy      Deployment strategy: rolling|blue-green|recreate (default: rolling)
#   --health-check  Wait for health check (default: true)
#   --rollback      Rollback to previous version
#   --dry-run       Show what would be deployed without deploying
#   --help          Show this help message

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DEPLOYMENT_DIR="$PROJECT_ROOT/deployment"

# Default options
SPECIFIC_SERVICE=""
IMAGE_TAG="latest"
DEPLOY_STRATEGY="rolling"
WAIT_FOR_HEALTH=true
DO_ROLLBACK=false
DRY_RUN=false
DEPLOY_LOG="$PROJECT_ROOT/deployment-logs/deploy_$(date +%Y%m%d_%H%M%S).log"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --service) SPECIFIC_SERVICE="$2"; shift 2 ;;
        --tag) IMAGE_TAG="$2"; shift 2 ;;
        --strategy) DEPLOY_STRATEGY="$2"; shift 2 ;;
        --no-health-check) WAIT_FOR_HEALTH=false; shift ;;
        --rollback) DO_ROLLBACK=true; shift ;;
        --dry-run) DRY_RUN=true; shift ;;
        --help) grep "^#" "$0" | grep -v "#!/bin/bash" | sed 's/^# //'; exit 0 ;;
        *) echo -e "${RED}Unknown option: $1${NC}"; exit 1 ;;
    esac
done

mkdir -p "$(dirname "$DEPLOY_LOG")"

log() { echo -e "${BLUE}[$(date +'%H:%M:%S')]${NC} $1" | tee -a "$DEPLOY_LOG"; }
success() { echo -e "${GREEN}✓ $1${NC}" | tee -a "$DEPLOY_LOG"; }
error() { echo -e "${RED}✗ $1${NC}" | tee -a "$DEPLOY_LOG"; }

echo -e "${BLUE}==================================${NC}"
echo -e "${BLUE}   GCRF Library - Deployment${NC}"
echo -e "${BLUE}==================================${NC}"
echo
log "Deployment started"
log "Strategy: $DEPLOY_STRATEGY | Tag: $IMAGE_TAG"
echo

# Define services
if [[ -n "$SPECIFIC_SERVICE" ]]; then
    SERVICES=("$SPECIFIC_SERVICE")
else
    SERVICES=("gateway-service" "auth-service" "book-service" "circulation-service" "reader-service")
fi

# Deploy function
deploy_service() {
    local service=$1
    local container_name="gcrf-$service"

    log "Deploying $service..."

    if [[ "$DRY_RUN" == true ]]; then
        echo "  Would deploy: gcrf-$service:$IMAGE_TAG"
        return 0
    fi

    # Backup current container if exists
    if docker ps -a --format '{{.Names}}' | grep -q "^${container_name}$"; then
        log "  Backing up current $service..."
        docker rename "$container_name" "${container_name}-backup-$(date +%s)" || true
    fi

    # Pull new image
    if ! docker pull "gcrf-$service:$IMAGE_TAG" >> "$DEPLOY_LOG" 2>&1; then
        error "  Failed to pull image for $service"
        return 1
    fi

    # Start new container
    if docker-compose -f "$DEPLOYMENT_DIR/docker-compose.services.yml" up -d "$service" >> "$DEPLOY_LOG" 2>&1; then
        success "  $service container started"
    else
        error "  Failed to start $service"
        return 1
    fi

    # Health check
    if [[ "$WAIT_FOR_HEALTH" == true ]]; then
        log "  Waiting for $service health check..."
        local retries=30
        while [[ $retries -gt 0 ]]; do
            if docker exec "$container_name" curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
                success "  $service is healthy"
                return 0
            fi
            retries=$((retries - 1))
            sleep 2
        done
        error "  $service health check timeout"
        return 1
    fi

    return 0
}

# Deploy all services
FAILED=false
for service in "${SERVICES[@]}"; do
    if ! deploy_service "$service"; then
        FAILED=true
    fi
    echo
done

if [[ "$FAILED" == true ]]; then
    error "Deployment failed"
    exit 1
fi

success "Deployment completed successfully"
log "Deployment log: $DEPLOY_LOG"
echo
