#!/bin/bash
# GCRF Library Management System - Build Benchmark Script
# Compares standard vs optimized Dockerfile performance
# Last Updated: 2025-11-01

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }

# Configuration
SERVICE="${1:-gateway}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend"
REPORT_FILE="/tmp/gcrf-benchmark-${SERVICE}-$(date +%Y%m%d-%H%M%S).txt"

log_info "=== GCRF Build Benchmark: ${SERVICE} Service ==="
log_info "Project: $PROJECT_ROOT"
log_info "Backend: $BACKEND_DIR"

# Check prerequisites
if ! docker buildx version &>/dev/null; then
    log_warning "BuildKit not available, using standard docker build"
    BUILD_CMD="docker build"
else
    log_success "BuildKit available"
    BUILD_CMD="docker buildx build --load"
fi

# Create report header
cat > "$REPORT_FILE" <<EOF
=== GCRF Docker Build Benchmark Report ===
Service: ${SERVICE}
Date: $(date)
Platform: $(uname -m)
Docker: $(docker --version)
BuildKit: $(docker buildx version 2>/dev/null || echo "Not available")

EOF

# Function to build and time
build_and_time() {
    local dockerfile=$1
    local tag=$2
    local description=$3

    log_info "Building with ${description}..."
    local start=$(date +%s)

    cd "$BACKEND_DIR"
    $BUILD_CMD \
        --platform linux/amd64 \
        --file "${SERVICE}-service/${dockerfile}" \
        --tag "gcrf-${SERVICE}:${tag}" \
        --progress=plain \
        . > "/tmp/build-${tag}.log" 2>&1

    local end=$(date +%s)
    local duration=$((end - start))

    log_success "Build completed in ${duration}s"
    echo "${description}: ${duration}s" >> "$REPORT_FILE"

    # Extract cache hits
    local cached_lines=$(grep -c "CACHED" "/tmp/build-${tag}.log" || echo "0")
    echo "  Cache hits: ${cached_lines}" >> "$REPORT_FILE"

    # Get image size
    local size=$(docker images "gcrf-${SERVICE}:${tag}" --format "{{.Size}}")
    echo "  Image size: ${size}" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"

    echo "$duration"
}

# Clean Maven cache for cold build test
log_warning "Cleaning Maven cache for cold build test..."
docker volume rm maven-cache 2>/dev/null || true
docker volume create maven-cache > /dev/null

# Test 1: Standard Dockerfile (cold cache)
log_info "=== Test 1: Standard Dockerfile (Cold Cache) ==="
if [ -f "$BACKEND_DIR/${SERVICE}-service/Dockerfile" ]; then
    standard_cold=$(build_and_time "Dockerfile" "standard-cold" "Standard Dockerfile (Cold)")
else
    log_warning "Standard Dockerfile not found, skipping"
    standard_cold=0
fi

# Test 2: Optimized Dockerfile (cold cache)
log_info "=== Test 2: Optimized Dockerfile (Cold Cache) ==="
docker volume rm maven-cache 2>/dev/null || true
docker volume create maven-cache > /dev/null
optimized_cold=$(build_and_time "Dockerfile.optimized" "optimized-cold" "Optimized Dockerfile (Cold)")

# Test 3: Optimized Dockerfile (warm cache - no changes)
log_info "=== Test 3: Optimized Dockerfile (Warm Cache) ==="
optimized_warm=$(build_and_time "Dockerfile.optimized" "optimized-warm" "Optimized Dockerfile (Warm)")

# Test 4: Optimized Dockerfile (warm cache - source change)
log_info "=== Test 4: Optimized Dockerfile (Source Change) ==="
# Make a trivial change
echo "// Benchmark test comment" >> "$BACKEND_DIR/${SERVICE}-service/src/main/java/com/gcrf/library/${SERVICE}/Application.java"
optimized_source=$(build_and_time "Dockerfile.optimized" "optimized-source" "Optimized Dockerfile (Source Change)")
# Revert change
git -C "$BACKEND_DIR" checkout "${SERVICE}-service/src/" 2>/dev/null || true

# Calculate improvements
cat >> "$REPORT_FILE" <<EOF
=== Performance Summary ===
EOF

if [ "$standard_cold" -gt 0 ]; then
    echo "Standard Dockerfile (Cold):          ${standard_cold}s" >> "$REPORT_FILE"
fi

echo "Optimized Dockerfile (Cold):         ${optimized_cold}s" >> "$REPORT_FILE"
echo "Optimized Dockerfile (Warm):         ${optimized_warm}s" >> "$REPORT_FILE"
echo "Optimized Dockerfile (Source):       ${optimized_source}s" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

if [ "$optimized_cold" -gt 0 ]; then
    local warm_improvement=$((100 - (optimized_warm * 100 / optimized_cold)))
    local source_improvement=$((100 - (optimized_source * 100 / optimized_cold)))

    echo "=== Improvement Analysis ===" >> "$REPORT_FILE"
    echo "Warm cache improvement:              ${warm_improvement}%" >> "$REPORT_FILE"
    echo "Source change improvement:           ${source_improvement}%" >> "$REPORT_FILE"
fi

# Display report
log_success "=== Benchmark Complete ==="
cat "$REPORT_FILE"

log_info "Full report saved to: $REPORT_FILE"
log_info "Build logs: /tmp/build-*.log"
