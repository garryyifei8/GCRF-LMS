#!/bin/bash
# GCRF Library Management System - CI/CD Docker Build Script
# Version: 1.0.0
# Last Updated: 2025-11-02
#
# Purpose: Build Docker images for all microservices
# Usage: ./ci-docker-build.sh [OPTIONS]
#
# Options:
#   --platform         Target platform (default: linux/amd64)
#   --tag              Image tag (default: latest)
#   --registry         Docker registry URL (optional)
#   --push             Push images to registry
#   --service          Build specific service only
#   --parallel         Build images in parallel (default: 2)
#   --no-cache         Build without cache
#   --help             Show this help message

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend"

# Default options
PLATFORM="linux/amd64"
IMAGE_TAG="latest"
DOCKER_REGISTRY=""
PUSH_IMAGES=false
SPECIFIC_SERVICE=""
PARALLEL_BUILDS=2
NO_CACHE=false
BUILD_TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BUILD_LOG_DIR="$PROJECT_ROOT/docker-build-logs"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --platform)
            PLATFORM="$2"
            shift 2
            ;;
        --tag)
            IMAGE_TAG="$2"
            shift 2
            ;;
        --registry)
            DOCKER_REGISTRY="$2"
            shift 2
            ;;
        --push)
            PUSH_IMAGES=true
            shift
            ;;
        --service)
            SPECIFIC_SERVICE="$2"
            shift 2
            ;;
        --parallel)
            PARALLEL_BUILDS="$2"
            shift 2
            ;;
        --no-cache)
            NO_CACHE=true
            shift
            ;;
        --help)
            grep "^#" "$0" | grep -v "#!/bin/bash" | sed 's/^# //'
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Create build log directory
mkdir -p "$BUILD_LOG_DIR"
BUILD_LOG="$BUILD_LOG_DIR/docker_build_${BUILD_TIMESTAMP}.log"

# Helper functions
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$BUILD_LOG"
}

success() {
    echo -e "${GREEN}✓ $1${NC}" | tee -a "$BUILD_LOG"
}

error() {
    echo -e "${RED}✗ $1${NC}" | tee -a "$BUILD_LOG"
}

warn() {
    echo -e "${YELLOW}⚠ $1${NC}" | tee -a "$BUILD_LOG"
}

# Print banner
echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}   GCRF Library - Docker Image Builder${NC}"
echo -e "${BLUE}======================================================${NC}"
echo
log "Docker build started at $(date)"
log "Build ID: ${BUILD_TIMESTAMP}"
log "Platform: ${PLATFORM}"
log "Image Tag: ${IMAGE_TAG}"
log "Build log: ${BUILD_LOG}"
echo

# ========================================
# Phase 1: Environment Check
# ========================================
log "Phase 1: Checking Docker environment..."
echo

# Check Docker
if ! command -v docker &> /dev/null; then
    error "Docker not found. Please install Docker"
    exit 1
fi

# Check Docker daemon
if ! docker ps &> /dev/null; then
    error "Docker daemon is not running. Please start Docker"
    exit 1
fi

DOCKER_VERSION=$(docker version --format '{{.Server.Version}}')
success "Docker version: $DOCKER_VERSION"

# Check buildx for multi-platform builds
if docker buildx version &> /dev/null; then
    success "Docker buildx available"
else
    warn "Docker buildx not available. Multi-platform builds may not work"
fi

# Check disk space (at least 10GB free)
FREE_SPACE=$(df -BG "$PROJECT_ROOT" | awk 'NR==2 {print $4}' | sed 's/G//')
if [[ "$FREE_SPACE" -lt 10 ]]; then
    warn "Low disk space: ${FREE_SPACE}GB free. Recommend at least 10GB"
else
    success "Disk space: ${FREE_SPACE}GB free"
fi

echo

# ========================================
# Phase 2: Verify Build Artifacts
# ========================================
log "Phase 2: Verifying JAR files..."
echo

if [[ -n "$SPECIFIC_SERVICE" ]]; then
    SERVICES=("$SPECIFIC_SERVICE")
else
    SERVICES=(
        "gateway-service"
        "auth-service"
        "book-service"
        "circulation-service"
        "reader-service"
        "system-service"
        "notification-service"
    )
fi

MISSING_JARS=false

for service in "${SERVICES[@]}"; do
    JAR_FILE="$BACKEND_DIR/$service/target/$service.jar"

    if [[ -f "$JAR_FILE" ]]; then
        JAR_SIZE=$(du -h "$JAR_FILE" | awk '{print $1}')
        success "$service JAR found (Size: $JAR_SIZE)"
    else
        error "$service JAR not found: $JAR_FILE"
        warn "Run ./ci-build-all.sh first to build JARs"
        MISSING_JARS=true
    fi
done

if [[ "$MISSING_JARS" == true ]]; then
    error "Some JAR files are missing. Run ./ci-build-all.sh first"
    exit 1
fi

echo

# ========================================
# Phase 3: Build Docker Images
# ========================================
log "Phase 3: Building Docker images..."
echo

# Function to build a single service image
build_docker_image() {
    local service=$1
    local image_log="$BUILD_LOG_DIR/docker_${service}_${BUILD_TIMESTAMP}.log"

    cd "$BACKEND_DIR"

    # Determine image name
    if [[ -n "$DOCKER_REGISTRY" ]]; then
        IMAGE_NAME="${DOCKER_REGISTRY}/gcrf-${service}:${IMAGE_TAG}"
    else
        IMAGE_NAME="gcrf-${service}:${IMAGE_TAG}"
    fi

    # Check if Dockerfile exists
    if [[ ! -f "$service/Dockerfile" ]]; then
        echo -e "${RED}✗ $service: Dockerfile not found${NC}"
        return 1
    fi

    # Build Docker command
    DOCKER_BUILD_CMD="docker build"

    # Add platform
    DOCKER_BUILD_CMD="$DOCKER_BUILD_CMD --platform $PLATFORM"

    # Add no-cache if specified
    if [[ "$NO_CACHE" == true ]]; then
        DOCKER_BUILD_CMD="$DOCKER_BUILD_CMD --no-cache"
    fi

    # Add tag
    DOCKER_BUILD_CMD="$DOCKER_BUILD_CMD -t $IMAGE_NAME"

    # Add build args
    DOCKER_BUILD_CMD="$DOCKER_BUILD_CMD --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
    DOCKER_BUILD_CMD="$DOCKER_BUILD_CMD --build-arg VERSION=${IMAGE_TAG}"

    # Add context and Dockerfile
    DOCKER_BUILD_CMD="$DOCKER_BUILD_CMD -f $service/Dockerfile ."

    # Execute build
    if eval $DOCKER_BUILD_CMD >> "$image_log" 2>&1; then
        # Get image size
        IMAGE_SIZE=$(docker images "$IMAGE_NAME" --format "{{.Size}}" | head -1)
        echo -e "${GREEN}✓ $service: Image built (Size: $IMAGE_SIZE)${NC}"
        return 0
    else
        echo -e "${RED}✗ $service: Build failed. Check log: $image_log${NC}"
        return 1
    fi
}

# Export function for parallel execution
export -f build_docker_image
export BACKEND_DIR
export PLATFORM
export IMAGE_TAG
export DOCKER_REGISTRY
export NO_CACHE
export BUILD_LOG_DIR
export BUILD_TIMESTAMP
export GREEN
export RED
export NC

# Build images in parallel
BUILD_FAILED=false
echo "${SERVICES[@]}" | xargs -n 1 -P "$PARALLEL_BUILDS" -I {} bash -c 'build_docker_image "$@"' _ {} || BUILD_FAILED=true

if [[ "$BUILD_FAILED" == true ]]; then
    error "One or more Docker image builds failed"
    exit 1
fi

echo

# ========================================
# Phase 4: Verify Images
# ========================================
log "Phase 4: Verifying Docker images..."
echo

for service in "${SERVICES[@]}"; do
    if [[ -n "$DOCKER_REGISTRY" ]]; then
        IMAGE_NAME="${DOCKER_REGISTRY}/gcrf-${service}:${IMAGE_TAG}"
    else
        IMAGE_NAME="gcrf-${service}:${IMAGE_TAG}"
    fi

    if docker images "$IMAGE_NAME" --format "{{.Repository}}:{{.Tag}}" | grep -q "$IMAGE_NAME"; then
        IMAGE_SIZE=$(docker images "$IMAGE_NAME" --format "{{.Size}}" | head -1)
        IMAGE_ID=$(docker images "$IMAGE_NAME" --format "{{.ID}}" | head -1)
        success "$service: $IMAGE_ID (Size: $IMAGE_SIZE)"
    else
        error "$service: Image not found"
    fi
done

echo

# ========================================
# Phase 5: Push Images (Optional)
# ========================================
if [[ "$PUSH_IMAGES" == true ]]; then
    if [[ -z "$DOCKER_REGISTRY" ]]; then
        warn "Registry not specified. Skipping push"
    else
        log "Phase 5: Pushing images to registry..."
        echo

        for service in "${SERVICES[@]}"; do
            IMAGE_NAME="${DOCKER_REGISTRY}/gcrf-${service}:${IMAGE_TAG}"

            log "Pushing $IMAGE_NAME..."

            if docker push "$IMAGE_NAME" >> "$BUILD_LOG" 2>&1; then
                success "$service pushed to registry"
            else
                error "$service push failed"
                BUILD_FAILED=true
            fi
        done

        echo

        if [[ "$BUILD_FAILED" == true ]]; then
            error "Some images failed to push"
            exit 1
        fi
    fi
fi

# ========================================
# Phase 6: Generate Build Report
# ========================================
log "Phase 6: Generating Docker build report..."
echo

BUILD_REPORT="$BUILD_LOG_DIR/docker_build_report_${BUILD_TIMESTAMP}.txt"

cat > "$BUILD_REPORT" << EOF
GCRF Library Management System - Docker Build Report
====================================================

Build ID: ${BUILD_TIMESTAMP}
Build Date: $(date)
Platform: ${PLATFORM}
Image Tag: ${IMAGE_TAG}
Docker Registry: $(if [[ -n "$DOCKER_REGISTRY" ]]; then echo "$DOCKER_REGISTRY"; else echo "None (local)"; fi)
No Cache: ${NO_CACHE}
Images Pushed: ${PUSH_IMAGES}

Environment
-----------
Docker Version: ${DOCKER_VERSION}

Built Images
------------
EOF

for service in "${SERVICES[@]}"; do
    if [[ -n "$DOCKER_REGISTRY" ]]; then
        IMAGE_NAME="${DOCKER_REGISTRY}/gcrf-${service}:${IMAGE_TAG}"
    else
        IMAGE_NAME="gcrf-${service}:${IMAGE_TAG}"
    fi

    if docker images "$IMAGE_NAME" --format "{{.Repository}}:{{.Tag}}" | grep -q "$IMAGE_NAME"; then
        IMAGE_SIZE=$(docker images "$IMAGE_NAME" --format "{{.Size}}" | head -1)
        IMAGE_ID=$(docker images "$IMAGE_NAME" --format "{{.ID}}" | head -1)
        echo "$service: $IMAGE_ID (Size: $IMAGE_SIZE)" >> "$BUILD_REPORT"
    fi
done

cat >> "$BUILD_REPORT" << EOF

Build Logs
----------
Main log: ${BUILD_LOG}
Individual image logs: ${BUILD_LOG_DIR}/docker_*_${BUILD_TIMESTAMP}.log

EOF

success "Docker build report generated: $BUILD_REPORT"

# Display summary
cat "$BUILD_REPORT"

# ========================================
# Summary
# ========================================
echo
echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}   Docker Build Summary${NC}"
echo -e "${BLUE}======================================================${NC}"
echo
echo -e "${GREEN}✓ Docker images built successfully${NC}"
echo
echo "Next Steps:"
echo "  1. Review build report: $BUILD_REPORT"
echo "  2. Test images locally: docker run -d <image-name>"
if [[ "$PUSH_IMAGES" == true ]]; then
    echo "  3. Images are available in registry: $DOCKER_REGISTRY"
else
    echo "  3. Push to registry: ./ci-docker-build.sh --push --registry <registry-url>"
fi
echo "  4. Deploy: ./deploy-services.sh"
echo

exit 0
