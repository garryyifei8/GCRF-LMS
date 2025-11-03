#!/bin/bash
# GCRF Library Management System - CI/CD Test Script
# Version: 1.0.0
# Last Updated: 2025-11-02
#
# Purpose: Run all tests (unit + integration) for CI/CD pipelines
# Usage: ./ci-test-all.sh [OPTIONS]
#
# Options:
#   --unit-only        Run only unit tests
#   --integration-only Run only integration tests
#   --service          Test specific service (e.g., --service auth-service)
#   --parallel         Use parallel test execution (default: 2 threads)
#   --coverage         Generate code coverage reports
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
UNIT_ONLY=false
INTEGRATION_ONLY=false
SPECIFIC_SERVICE=""
PARALLEL_THREADS=2
GENERATE_COVERAGE=false
TEST_TIMESTAMP=$(date +%Y%m%d_%H%M%S)
TEST_LOG_DIR="$PROJECT_ROOT/test-logs"

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --unit-only)
            UNIT_ONLY=true
            shift
            ;;
        --integration-only)
            INTEGRATION_ONLY=true
            shift
            ;;
        --service)
            SPECIFIC_SERVICE="$2"
            shift 2
            ;;
        --parallel)
            PARALLEL_THREADS="$2"
            shift 2
            ;;
        --coverage)
            GENERATE_COVERAGE=true
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

# Create test log directory
mkdir -p "$TEST_LOG_DIR"
TEST_LOG="$TEST_LOG_DIR/test_${TEST_TIMESTAMP}.log"

# Helper functions
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$TEST_LOG"
}

success() {
    echo -e "${GREEN}✓ $1${NC}" | tee -a "$TEST_LOG"
}

error() {
    echo -e "${RED}✗ $1${NC}" | tee -a "$TEST_LOG"
}

warn() {
    echo -e "${YELLOW}⚠ $1${NC}" | tee -a "$TEST_LOG"
}

# Print banner
echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}   GCRF Library - CI/CD Test Runner${NC}"
echo -e "${BLUE}======================================================${NC}"
echo
log "Test run started at $(date)"
log "Test ID: ${TEST_TIMESTAMP}"
log "Test log: ${TEST_LOG}"
echo

# ========================================
# Phase 1: Environment Check
# ========================================
log "Phase 1: Checking test environment..."
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

# Set JAVA_HOME if not set
if [[ -z "$JAVA_HOME" ]]; then
    if [[ "$OSTYPE" == "darwin"* ]]; then
        export JAVA_HOME=$(/usr/libexec/java_home -v 21)
        log "JAVA_HOME set to: $JAVA_HOME"
    fi
fi

# Check test dependencies (Docker for integration tests)
if [[ "$INTEGRATION_ONLY" == true ]] || [[ "$UNIT_ONLY" == false ]]; then
    if ! command -v docker &> /dev/null; then
        warn "Docker not found. Integration tests may fail"
    else
        if docker ps &> /dev/null; then
            success "Docker is running"
        else
            warn "Docker is installed but not running"
        fi
    fi
fi

echo

# ========================================
# Phase 2: Define Test Targets
# ========================================
log "Phase 2: Preparing test targets..."
echo

if [[ -n "$SPECIFIC_SERVICE" ]]; then
    SERVICES=("$SPECIFIC_SERVICE")
    log "Testing specific service: $SPECIFIC_SERVICE"
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
    log "Testing all services: ${#SERVICES[@]} services"
fi

# Common modules should be tested first
COMMON_MODULES=(
    "common/common-core"
    "common/common-web"
    "common/common-security"
    "common/common-mybatis"
)

echo

# ========================================
# Phase 3: Test Common Modules
# ========================================
log "Phase 3: Testing common modules..."
echo

for module in "${COMMON_MODULES[@]}"; do
    log "Testing $module..."

    cd "$BACKEND_DIR"

    MODULE_TEST_LOG="$TEST_LOG_DIR/test_${module##*/}_${TEST_TIMESTAMP}.log"

    # Build test command based on options
    if [[ "$UNIT_ONLY" == true ]]; then
        TEST_COMMAND="mvn test -pl $module -Dtest=!*IntegrationTest"
    elif [[ "$INTEGRATION_ONLY" == true ]]; then
        TEST_COMMAND="mvn test -pl $module -Dtest=*IntegrationTest"
    else
        TEST_COMMAND="mvn test -pl $module"
    fi

    # Add coverage if requested
    if [[ "$GENERATE_COVERAGE" == true ]]; then
        TEST_COMMAND="$TEST_COMMAND jacoco:report"
    fi

    if $TEST_COMMAND >> "$MODULE_TEST_LOG" 2>&1; then
        # Parse test results
        TEST_RESULT=$(grep -E "Tests run:" "$MODULE_TEST_LOG" | tail -1)
        success "$module: $TEST_RESULT"

        # Extract numbers
        if [[ -n "$TEST_RESULT" ]]; then
            TESTS=$(echo "$TEST_RESULT" | grep -oP 'Tests run: \K\d+')
            FAILURES=$(echo "$TEST_RESULT" | grep -oP 'Failures: \K\d+')
            SKIPPED=$(echo "$TEST_RESULT" | grep -oP 'Skipped: \K\d+')

            TOTAL_TESTS=$((TOTAL_TESTS + TESTS))
            FAILED_TESTS=$((FAILED_TESTS + FAILURES))
            SKIPPED_TESTS=$((SKIPPED_TESTS + SKIPPED))
        fi
    else
        error "$module tests failed. Check log: $MODULE_TEST_LOG"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
done

echo

# ========================================
# Phase 4: Test Microservices
# ========================================
log "Phase 4: Testing microservices..."
echo

# Function to test a single service
test_service() {
    local service=$1
    local service_test_log="$TEST_LOG_DIR/test_${service}_${TEST_TIMESTAMP}.log"

    cd "$BACKEND_DIR"

    # Build test command based on options
    if [[ "$UNIT_ONLY" == true ]]; then
        TEST_COMMAND="mvn test -pl $service -Dtest=!*IntegrationTest"
    elif [[ "$INTEGRATION_ONLY" == true ]]; then
        TEST_COMMAND="mvn test -pl $service -Dtest=*IntegrationTest"
    else
        TEST_COMMAND="mvn test -pl $service"
    fi

    # Add coverage if requested
    if [[ "$GENERATE_COVERAGE" == true ]]; then
        TEST_COMMAND="$TEST_COMMAND jacoco:report"
    fi

    if $TEST_COMMAND >> "$service_test_log" 2>&1; then
        # Parse test results
        TEST_RESULT=$(grep -E "Tests run:" "$service_test_log" | tail -1)
        echo -e "${GREEN}✓ $service: $TEST_RESULT${NC}"
        return 0
    else
        echo -e "${RED}✗ $service tests failed. Check log: $service_test_log${NC}"
        return 1
    fi
}

# Export function for parallel execution
export -f test_service
export BACKEND_DIR
export UNIT_ONLY
export INTEGRATION_ONLY
export GENERATE_COVERAGE
export TEST_LOG_DIR
export TEST_TIMESTAMP
export GREEN
export RED
export NC

# Run tests in parallel
TEST_FAILED=false
echo "${SERVICES[@]}" | xargs -n 1 -P "$PARALLEL_THREADS" -I {} bash -c 'test_service "$@"' _ {} || TEST_FAILED=true

# Parse all service test results
for service in "${SERVICES[@]}"; do
    service_test_log="$TEST_LOG_DIR/test_${service}_${TEST_TIMESTAMP}.log"

    if [[ -f "$service_test_log" ]]; then
        TEST_RESULT=$(grep -E "Tests run:" "$service_test_log" | tail -1)

        if [[ -n "$TEST_RESULT" ]]; then
            TESTS=$(echo "$TEST_RESULT" | grep -oP 'Tests run: \K\d+' || echo "0")
            FAILURES=$(echo "$TEST_RESULT" | grep -oP 'Failures: \K\d+' || echo "0")
            SKIPPED=$(echo "$TEST_RESULT" | grep -oP 'Skipped: \K\d+' || echo "0")

            TOTAL_TESTS=$((TOTAL_TESTS + TESTS))
            FAILED_TESTS=$((FAILED_TESTS + FAILURES))
            SKIPPED_TESTS=$((SKIPPED_TESTS + SKIPPED))
        fi
    fi
done

PASSED_TESTS=$((TOTAL_TESTS - FAILED_TESTS - SKIPPED_TESTS))

echo

# ========================================
# Phase 5: Generate Test Report
# ========================================
log "Phase 5: Generating test report..."
echo

TEST_REPORT="$TEST_LOG_DIR/test_report_${TEST_TIMESTAMP}.txt"

cat > "$TEST_REPORT" << EOF
GCRF Library Management System - Test Report
=============================================

Test Run ID: ${TEST_TIMESTAMP}
Test Date: $(date)
Test Type: $(if [[ "$UNIT_ONLY" == true ]]; then echo "Unit Tests Only"; elif [[ "$INTEGRATION_ONLY" == true ]]; then echo "Integration Tests Only"; else echo "All Tests"; fi)
Parallel Threads: ${PARALLEL_THREADS}
Coverage Report: $(if [[ "$GENERATE_COVERAGE" == true ]]; then echo "Generated"; else echo "Not Generated"; fi)

Test Results
------------
Total Tests:   ${TOTAL_TESTS}
Passed:        ${PASSED_TESTS}
Failed:        ${FAILED_TESTS}
Skipped:       ${SKIPPED_TESTS}

Pass Rate: $(if [[ $TOTAL_TESTS -gt 0 ]]; then echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc; else echo "0"; fi)%

Tested Components
-----------------
Common Modules: ${#COMMON_MODULES[@]} modules
Microservices: ${#SERVICES[@]} services

Test Logs
---------
Main log: ${TEST_LOG}
Individual test logs: ${TEST_LOG_DIR}/test_*_${TEST_TIMESTAMP}.log

EOF

if [[ "$GENERATE_COVERAGE" == true ]]; then
    cat >> "$TEST_REPORT" << EOF

Coverage Reports
----------------
EOF
    for service in "${SERVICES[@]}"; do
        COVERAGE_FILE="$BACKEND_DIR/$service/target/site/jacoco/index.html"
        if [[ -f "$COVERAGE_FILE" ]]; then
            echo "$service: $COVERAGE_FILE" >> "$TEST_REPORT"
        fi
    done
fi

cat >> "$TEST_REPORT" << EOF

EOF

success "Test report generated: $TEST_REPORT"

# Display summary
cat "$TEST_REPORT"

# ========================================
# Summary
# ========================================
echo
echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}   Test Summary${NC}"
echo -e "${BLUE}======================================================${NC}"
echo
echo -e "Total Tests:  ${TOTAL_TESTS}"
echo -e "${GREEN}Passed:       ${PASSED_TESTS}${NC}"

if [[ $FAILED_TESTS -gt 0 ]]; then
    echo -e "${RED}Failed:       ${FAILED_TESTS}${NC}"
    echo
    echo -e "${RED}✗ Tests failed. Please review the logs.${NC}"
    echo
    exit 1
else
    echo -e "Failed:       ${FAILED_TESTS}"
fi

if [[ $SKIPPED_TESTS -gt 0 ]]; then
    echo -e "${YELLOW}Skipped:      ${SKIPPED_TESTS}${NC}"
fi

echo
echo -e "${GREEN}✓ All tests passed successfully${NC}"
echo
echo "Next Steps:"
echo "  1. Review test report: $TEST_REPORT"
if [[ "$GENERATE_COVERAGE" == true ]]; then
    echo "  2. Review coverage reports: backend/*/target/site/jacoco/index.html"
fi
echo "  3. Build Docker images: ./ci-docker-build.sh"
echo

exit 0
