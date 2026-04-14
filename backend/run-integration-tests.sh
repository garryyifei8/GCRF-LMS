#!/bin/bash
###############################################################################
# GCRF Library Management System - Integration Test Runner
# Author: GCRF Test Team
# Date: 2025-12-01
# Description: Runs all integration tests across backend services
###############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
JAVA_VERSION=21
BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COVERAGE_THRESHOLD=80

###############################################################################
# Functions
###############################################################################

print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

check_prerequisites() {
    print_header "Checking Prerequisites"

    # Check Java version
    if ! command -v java &> /dev/null; then
        print_error "Java not found. Please install Java 21."
        exit 1
    fi

    JAVA_VERSION_INSTALLED=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
    if [ "$JAVA_VERSION_INSTALLED" != "$JAVA_VERSION" ]; then
        print_error "Java $JAVA_VERSION required, but Java $JAVA_VERSION_INSTALLED found."
        print_warning "Run: export JAVA_HOME=\$(/usr/libexec/java_home -v 21)"
        exit 1
    fi
    print_success "Java $JAVA_VERSION detected"

    # Check Maven
    if ! command -v mvn &> /dev/null; then
        print_error "Maven not found. Please install Maven 3.8+."
        exit 1
    fi
    print_success "Maven detected"

    # Check Docker
    if ! docker ps &> /dev/null; then
        print_error "Docker not running. Please start Docker Desktop."
        exit 1
    fi
    print_success "Docker running"

    echo ""
}

run_service_tests() {
    SERVICE=$1
    print_header "Running $SERVICE Tests"

    cd "$BACKEND_DIR"

    if [ ! -d "$SERVICE" ]; then
        print_warning "$SERVICE directory not found. Skipping..."
        return 0
    fi

    echo "Running tests for $SERVICE..."
    if mvn test -pl "$SERVICE" -q; then
        print_success "$SERVICE tests passed"
        return 0
    else
        print_error "$SERVICE tests failed"
        return 1
    fi
}

generate_coverage_report() {
    SERVICE=$1
    print_header "Generating Coverage Report for $SERVICE"

    cd "$BACKEND_DIR"

    if mvn jacoco:report -pl "$SERVICE" -q; then
        REPORT_PATH="$SERVICE/target/site/jacoco/index.html"
        if [ -f "$REPORT_PATH" ]; then
            print_success "Coverage report generated: $REPORT_PATH"

            # Extract coverage percentage (simplified)
            if command -v xmllint &> /dev/null; then
                COVERAGE=$(xmllint --xpath "string(//counter[@type='LINE']/@missed)" \
                    "$SERVICE/target/site/jacoco/jacoco.xml" 2>/dev/null || echo "N/A")
                if [ "$COVERAGE" != "N/A" ]; then
                    print_success "Coverage: $COVERAGE%"
                fi
            fi
        fi
    else
        print_warning "Failed to generate coverage report for $SERVICE"
    fi
}

run_all_tests() {
    print_header "Running All Integration Tests"

    # Services to test
    SERVICES=(
        "book-service"
        "reader-service"
        "circulation-service"
        "auth-service"
    )

    FAILED_SERVICES=()

    for SERVICE in "${SERVICES[@]}"; do
        if ! run_service_tests "$SERVICE"; then
            FAILED_SERVICES+=("$SERVICE")
        fi
        echo ""
    done

    # Summary
    print_header "Test Summary"
    if [ ${#FAILED_SERVICES[@]} -eq 0 ]; then
        print_success "All tests passed! ✨"
    else
        print_error "Tests failed for: ${FAILED_SERVICES[*]}"
        exit 1
    fi
}

run_with_coverage() {
    print_header "Running Tests with Coverage Report"

    cd "$BACKEND_DIR"

    SERVICES=(
        "book-service"
        "reader-service"
        "circulation-service"
        "auth-service"
    )

    for SERVICE in "${SERVICES[@]}"; do
        run_service_tests "$SERVICE"
        generate_coverage_report "$SERVICE"
        echo ""
    done

    print_header "Coverage Reports Generated"
    for SERVICE in "${SERVICES[@]}"; do
        REPORT_PATH="$SERVICE/target/site/jacoco/index.html"
        if [ -f "$REPORT_PATH" ]; then
            echo "  - $SERVICE: file://$BACKEND_DIR/$REPORT_PATH"
        fi
    done
}

clean_build() {
    print_header "Cleaning Build Artifacts"
    cd "$BACKEND_DIR"
    mvn clean -q
    print_success "Build artifacts cleaned"
}

###############################################################################
# Main Script
###############################################################################

main() {
    echo ""
    print_header "GCRF Library Management System"
    print_header "Integration Test Runner"
    echo ""

    # Parse arguments
    case "${1:-all}" in
        clean)
            clean_build
            ;;
        book-service|reader-service|circulation-service|auth-service)
            check_prerequisites
            run_service_tests "$1"
            ;;
        coverage)
            check_prerequisites
            run_with_coverage
            ;;
        all|*)
            check_prerequisites
            run_all_tests
            ;;
    esac

    echo ""
    print_success "Done! 🎉"
}

# Show usage if --help
if [ "${1}" = "--help" ] || [ "${1}" = "-h" ]; then
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  all                Run all integration tests (default)"
    echo "  book-service       Run book-service tests only"
    echo "  reader-service     Run reader-service tests only"
    echo "  circulation-service Run circulation-service tests only"
    echo "  auth-service       Run auth-service tests only"
    echo "  coverage           Run all tests with coverage reports"
    echo "  clean              Clean build artifacts"
    echo "  --help, -h         Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                 # Run all tests"
    echo "  $0 book-service    # Run book-service tests only"
    echo "  $0 coverage        # Generate coverage reports"
    echo ""
    exit 0
fi

# Run main
main "$@"
