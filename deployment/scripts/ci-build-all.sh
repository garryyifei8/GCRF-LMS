#!/bin/bash
# GCRF Library Management System - CI/CD Build Script
# Version: 1.0.0
# Last Updated: 2025-11-02
#
# Purpose: Build all microservices in parallel for CI/CD pipelines
# Usage: ./ci-build-all.sh [OPTIONS]
#
# Options:
#   --skip-tests    Skip running tests (faster builds)
#   --clean         Run Maven clean before build
#   --parallel      Use Maven parallel builds (default: 4 threads)
#   --profile       Maven profile to use (default: dev)
#   --help          Show this help message

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
SKIP_TESTS=false
CLEAN_BUILD=false
PARALLEL_THREADS=4
MAVEN_PROFILE="dev"
BUILD_TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BUILD_LOG_DIR="$PROJECT_ROOT/build-logs"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --clean)
            CLEAN_BUILD=true
            shift
            ;;
        --parallel)
            PARALLEL_THREADS="$2"
            shift 2
            ;;
        --profile)
            MAVEN_PROFILE="$2"
            shift 2
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
BUILD_LOG="$BUILD_LOG_DIR/build_${BUILD_TIMESTAMP}.log"

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
echo -e "${BLUE}   GCRF Library - CI/CD Build Pipeline${NC}"
echo -e "${BLUE}======================================================${NC}"
echo
log "Build started at $(date)"
log "Build ID: ${BUILD_TIMESTAMP}"
log "Build log: ${BUILD_LOG}"
echo

# ========================================
# Phase 1: Environment Check
# ========================================
log "Phase 1: Checking build environment..."
echo

# Check Java version
if ! command -v java &> /dev/null; then
    error "Java not found. Please install Java 21+"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [[ "$JAVA_VERSION" -lt 21 ]]; then
    error "Java 21+ is required. Current version: $JAVA_VERSION"
    exit 1
fi
success "Java version: $JAVA_VERSION"

# Check Maven
if ! command -v mvn &> /dev/null; then
    error "Maven not found. Please install Maven 3.6+"
    exit 1
fi

MVN_VERSION=$(mvn -version | head -n1 | awk '{print $3}')
success "Maven version: $MVN_VERSION"

# Set JAVA_HOME if not set
if [[ -z "$JAVA_HOME" ]]; then
    if [[ "$OSTYPE" == "darwin"* ]]; then
        export JAVA_HOME=$(/usr/libexec/java_home -v 21)
        log "JAVA_HOME set to: $JAVA_HOME"
    else
        error "JAVA_HOME not set. Please set JAVA_HOME environment variable"
        exit 1
    fi
fi

# Check disk space (at least 5GB free)
FREE_SPACE=$(df -BG "$PROJECT_ROOT" | awk 'NR==2 {print $4}' | sed 's/G//')
if [[ "$FREE_SPACE" -lt 5 ]]; then
    warn "Low disk space: ${FREE_SPACE}GB free. Recommend at least 5GB"
else
    success "Disk space: ${FREE_SPACE}GB free"
fi

echo

# ========================================
# Phase 2: Clean (Optional)
# ========================================
if [[ "$CLEAN_BUILD" == true ]]; then
    log "Phase 2: Cleaning previous builds..."
    echo

    cd "$BACKEND_DIR"

    if mvn clean >> "$BUILD_LOG" 2>&1; then
        success "Maven clean completed"
    else
        error "Maven clean failed. Check log: $BUILD_LOG"
        exit 1
    fi

    echo
fi

# ========================================
# Phase 3: Build Common Modules
# ========================================
log "Phase 3: Building common modules..."
echo

cd "$BACKEND_DIR"

COMMON_MODULES=(
    "common/common-core"
    "common/common-web"
    "common/common-security"
    "common/common-mybatis"
)

for module in "${COMMON_MODULES[@]}"; do
    log "Building $module..."

    if [[ "$SKIP_TESTS" == true ]]; then
        MVN_COMMAND="mvn clean install -pl $module -am -DskipTests -P$MAVEN_PROFILE"
    else
        MVN_COMMAND="mvn clean install -pl $module -am -P$MAVEN_PROFILE"
    fi

    if $MVN_COMMAND >> "$BUILD_LOG" 2>&1; then
        success "$module built successfully"
    else
        error "$module build failed. Check log: $BUILD_LOG"
        exit 1
    fi
done

echo

# ========================================
# Phase 4: Build Microservices (Parallel)
# ========================================
log "Phase 4: Building microservices (parallel)..."
echo

SERVICES=(
    "gateway-service"
    "auth-service"
    "book-service"
    "circulation-service"
    "reader-service"
    "system-service"
    "notification-service"
)

# Function to build a single service
build_service() {
    local service=$1
    local service_log="$BUILD_LOG_DIR/build_${service}_${BUILD_TIMESTAMP}.log"

    cd "$BACKEND_DIR"

    if [[ "$SKIP_TESTS" == true ]]; then
        MVN_COMMAND="mvn clean package -pl $service -am -DskipTests -P$MAVEN_PROFILE"
    else
        MVN_COMMAND="mvn clean package -pl $service -am -P$MAVEN_PROFILE"
    fi

    if $MVN_COMMAND >> "$service_log" 2>&1; then
        echo -e "${GREEN}✓ $service built successfully${NC}"
        return 0
    else
        echo -e "${RED}✗ $service build failed. Check log: $service_log${NC}"
        return 1
    fi
}

# Export function for parallel execution
export -f build_service
export BACKEND_DIR
export SKIP_TESTS
export MAVEN_PROFILE
export BUILD_LOG_DIR
export BUILD_TIMESTAMP
export GREEN
export RED
export NC

# Build services in parallel
BUILD_FAILED=false
echo "${SERVICES[@]}" | xargs -n 1 -P "$PARALLEL_THREADS" -I {} bash -c 'build_service "$@"' _ {} || BUILD_FAILED=true

if [[ "$BUILD_FAILED" == true ]]; then
    error "One or more service builds failed"
    exit 1
fi

echo

# ========================================
# Phase 5: Verify Build Artifacts
# ========================================
log "Phase 5: Verifying build artifacts..."
echo

MISSING_ARTIFACTS=false

for service in "${SERVICES[@]}"; do
    JAR_FILE="$BACKEND_DIR/$service/target/$service.jar"

    if [[ -f "$JAR_FILE" ]]; then
        JAR_SIZE=$(du -h "$JAR_FILE" | awk '{print $1}')
        success "$service JAR found (Size: $JAR_SIZE)"
    else
        error "$service JAR not found: $JAR_FILE"
        MISSING_ARTIFACTS=true
    fi
done

if [[ "$MISSING_ARTIFACTS" == true ]]; then
    error "Some build artifacts are missing"
    exit 1
fi

echo

# ========================================
# Phase 6: Generate Build Report
# ========================================
log "Phase 6: Generating build report..."
echo

BUILD_REPORT="$BUILD_LOG_DIR/build_report_${BUILD_TIMESTAMP}.txt"

cat > "$BUILD_REPORT" << EOF
GCRF Library Management System - Build Report
==============================================

Build ID: ${BUILD_TIMESTAMP}
Build Date: $(date)
Build Profile: ${MAVEN_PROFILE}
Clean Build: ${CLEAN_BUILD}
Tests Skipped: ${SKIP_TESTS}
Parallel Threads: ${PARALLEL_THREADS}

Environment
-----------
Java Version: ${JAVA_VERSION}
Maven Version: ${MVN_VERSION}
JAVA_HOME: ${JAVA_HOME}

Build Results
-------------
Common Modules: ${#COMMON_MODULES[@]} modules built
Microservices: ${#SERVICES[@]} services built

Artifacts
---------
EOF

for service in "${SERVICES[@]}"; do
    JAR_FILE="$BACKEND_DIR/$service/target/$service.jar"
    if [[ -f "$JAR_FILE" ]]; then
        JAR_SIZE=$(du -h "$JAR_FILE" | awk '{print $1}')
        echo "$service.jar - Size: $JAR_SIZE" >> "$BUILD_REPORT"
    fi
done

cat >> "$BUILD_REPORT" << EOF

Build Logs
----------
Main log: ${BUILD_LOG}
Individual service logs: ${BUILD_LOG_DIR}/build_*_${BUILD_TIMESTAMP}.log

EOF

success "Build report generated: $BUILD_REPORT"

# Display summary
cat "$BUILD_REPORT"

# ========================================
# Summary
# ========================================
echo
echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}   Build Summary${NC}"
echo -e "${BLUE}======================================================${NC}"
echo
echo -e "${GREEN}✓ Build completed successfully${NC}"
echo
echo "Next Steps:"
echo "  1. Review build report: $BUILD_REPORT"
echo "  2. Run tests: ./ci-test-all.sh"
echo "  3. Build Docker images: ./ci-docker-build.sh"
echo "  4. Deploy: ./deploy-services.sh"
echo

exit 0
