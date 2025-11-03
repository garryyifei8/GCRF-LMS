#!/bin/bash
#
# GCRF Library Management System - Stack Startup Script
#
# Purpose: Orchestrated startup of the complete GCRF stack with health monitoring
#
# Usage: ./start-stack.sh [options]
#   Options:
#     --skip-infrastructure  Skip infrastructure startup (assume already running)
#     --skip-health-check    Skip health check validation
#     --timeout <seconds>    Override health check timeout (default: 300)
#     --verbose              Enable verbose output
#     --help                 Display this help message
#
# Exit codes:
#   0 - Success
#   1 - General error
#   2 - Infrastructure startup failed
#   3 - Service startup failed
#   4 - Health check failed
#   5 - Prerequisites check failed

set -e

# Color codes for output
readonly COLOR_GREEN='\033[0;32m'
readonly COLOR_YELLOW='\033[1;33m'
readonly COLOR_RED='\033[0;31m'
readonly COLOR_BLUE='\033[0;34m'
readonly COLOR_RESET='\033[0m'

# Script configuration
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly DEPLOYMENT_DIR="$(dirname "$SCRIPT_DIR")"
readonly LOG_DIR="${DEPLOYMENT_DIR}/logs"
readonly TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
readonly STARTUP_LOG="${LOG_DIR}/startup_${TIMESTAMP}.log"

# Default values
SKIP_INFRASTRUCTURE=false
SKIP_HEALTH_CHECK=false
HEALTH_CHECK_TIMEOUT=300
VERBOSE=false

# Function: Print colored output
print_colored() {
    local color=$1
    shift
    echo -e "${color}$*${COLOR_RESET}"
}

# Function: Print step header
print_step() {
    echo ""
    print_colored "$COLOR_BLUE" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    print_colored "$COLOR_BLUE" "$1"
    print_colored "$COLOR_BLUE" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

# Function: Log message to file and optionally to stdout
log_message() {
    local message="[$(date '+%Y-%m-%d %H:%M:%S')] $*"
    echo "$message" >> "$STARTUP_LOG"
    if [ "$VERBOSE" = true ]; then
        echo "$message"
    fi
}

# Function: Display help
show_help() {
    grep '^#' "$0" | head -20 | tail -18 | sed 's/^# //' | sed 's/^#//'
}

# Function: Check prerequisites
check_prerequisites() {
    print_step "[Prerequisites Check]"

    local prerequisites_met=true

    # Check Docker
    if command -v docker &> /dev/null; then
        local docker_version=$(docker --version | awk '{print $3}' | tr -d ',')
        print_colored "$COLOR_GREEN" "✓ Docker installed: $docker_version"
        log_message "Docker version: $docker_version"
    else
        print_colored "$COLOR_RED" "✗ Docker is not installed"
        prerequisites_met=false
    fi

    # Check Docker Compose
    if command -v docker-compose &> /dev/null; then
        local compose_version=$(docker-compose --version | awk '{print $4}' | tr -d ',')
        print_colored "$COLOR_GREEN" "✓ Docker Compose installed: $compose_version"
        log_message "Docker Compose version: $compose_version"
    else
        print_colored "$COLOR_RED" "✗ Docker Compose is not installed"
        prerequisites_met=false
    fi

    # Check Docker daemon
    if docker ps &> /dev/null; then
        print_colored "$COLOR_GREEN" "✓ Docker daemon is running"
        log_message "Docker daemon status: running"
    else
        print_colored "$COLOR_RED" "✗ Docker daemon is not running"
        prerequisites_met=false
    fi

    # Check compose files
    local compose_files=(
        "${DEPLOYMENT_DIR}/docker-compose.infrastructure.yml"
        "${DEPLOYMENT_DIR}/docker-compose.services.yml"
    )

    for file in "${compose_files[@]}"; do
        if [ -f "$file" ]; then
            print_colored "$COLOR_GREEN" "✓ Found: $(basename "$file")"
            log_message "Compose file found: $file"
        else
            print_colored "$COLOR_RED" "✗ Missing: $(basename "$file")"
            prerequisites_met=false
        fi
    done

    # Check available disk space (require at least 10GB)
    local available_space=$(df -BG . | awk 'NR==2 {print $4}' | tr -d 'G')
    if [ "$available_space" -ge 10 ]; then
        print_colored "$COLOR_GREEN" "✓ Disk space available: ${available_space}GB"
        log_message "Available disk space: ${available_space}GB"
    else
        print_colored "$COLOR_YELLOW" "⚠ Low disk space: ${available_space}GB (recommended: 10GB+)"
        log_message "WARNING: Low disk space: ${available_space}GB"
    fi

    if [ "$prerequisites_met" = false ]; then
        print_colored "$COLOR_RED" "Prerequisites check failed. Please install missing components."
        exit 5
    fi
}

# Function: Start infrastructure services
start_infrastructure() {
    print_step "[Step 1/4] Starting Infrastructure Services"

    log_message "Starting infrastructure services..."

    cd "$DEPLOYMENT_DIR"

    # Pull latest images
    print_colored "$COLOR_YELLOW" "Pulling latest infrastructure images..."
    if docker-compose -f docker-compose.infrastructure.yml pull 2>&1 | tee -a "$STARTUP_LOG"; then
        print_colored "$COLOR_GREEN" "✓ Images pulled successfully"
    else
        print_colored "$COLOR_YELLOW" "⚠ Some images could not be pulled, using cached versions"
    fi

    # Start infrastructure
    print_colored "$COLOR_YELLOW" "Starting infrastructure containers..."
    if docker-compose -f docker-compose.infrastructure.yml up -d 2>&1 | tee -a "$STARTUP_LOG"; then
        print_colored "$COLOR_GREEN" "✓ Infrastructure containers started"
        log_message "Infrastructure startup completed"
    else
        print_colored "$COLOR_RED" "✗ Failed to start infrastructure"
        log_message "ERROR: Infrastructure startup failed"
        exit 2
    fi
}

# Function: Wait for infrastructure health
wait_for_infrastructure() {
    print_step "[Step 2/4] Waiting for Infrastructure Health"

    local infrastructure_services=(
        "gcrf-postgres-primary"
        "gcrf-redis-master"
        "gcrf-nacos"
        "gcrf-rabbitmq"
        "gcrf-minio"
    )

    print_colored "$COLOR_YELLOW" "Checking infrastructure health (timeout: ${HEALTH_CHECK_TIMEOUT}s)..."

    if [ "$SKIP_HEALTH_CHECK" = false ]; then
        if MAX_WAIT="$HEALTH_CHECK_TIMEOUT" "$SCRIPT_DIR/wait-for-healthy.sh" "${infrastructure_services[@]}"; then
            print_colored "$COLOR_GREEN" "✓ All infrastructure services are healthy"
            log_message "Infrastructure health check passed"
        else
            print_colored "$COLOR_RED" "✗ Infrastructure health check failed"
            log_message "ERROR: Infrastructure health check failed"

            # Show container status
            echo ""
            print_colored "$COLOR_YELLOW" "Current container status:"
            docker-compose -f docker-compose.infrastructure.yml ps

            exit 4
        fi
    else
        print_colored "$COLOR_YELLOW" "⚠ Health check skipped (--skip-health-check)"
        log_message "Infrastructure health check skipped"
    fi
}

# Function: Start application services
start_services() {
    print_step "[Step 3/4] Starting Application Services"

    log_message "Starting application services..."

    cd "$DEPLOYMENT_DIR"

    # Pull latest images
    print_colored "$COLOR_YELLOW" "Pulling latest service images..."
    if docker-compose -f docker-compose.services.yml pull 2>&1 | tee -a "$STARTUP_LOG"; then
        print_colored "$COLOR_GREEN" "✓ Images pulled successfully"
    else
        print_colored "$COLOR_YELLOW" "⚠ Some images could not be pulled, using cached versions"
    fi

    # Start services
    print_colored "$COLOR_YELLOW" "Starting service containers..."
    if docker-compose -f docker-compose.services.yml up -d 2>&1 | tee -a "$STARTUP_LOG"; then
        print_colored "$COLOR_GREEN" "✓ Service containers started"
        log_message "Services startup completed"
    else
        print_colored "$COLOR_RED" "✗ Failed to start services"
        log_message "ERROR: Services startup failed"
        exit 3
    fi
}

# Function: Wait for services health
wait_for_services() {
    print_step "[Step 4/4] Waiting for Services Health"

    local application_services=(
        "gcrf-gateway-service"
        "gcrf-auth-service"
    )

    print_colored "$COLOR_YELLOW" "Checking application services health..."

    if [ "$SKIP_HEALTH_CHECK" = false ]; then
        # Give services time to register with Nacos
        print_colored "$COLOR_YELLOW" "Waiting 10s for service registration..."
        sleep 10

        if MAX_WAIT="$HEALTH_CHECK_TIMEOUT" "$SCRIPT_DIR/wait-for-healthy.sh" "${application_services[@]}"; then
            print_colored "$COLOR_GREEN" "✓ All application services are healthy"
            log_message "Application services health check passed"
        else
            print_colored "$COLOR_RED" "✗ Application services health check failed"
            log_message "ERROR: Application services health check failed"

            # Show container status
            echo ""
            print_colored "$COLOR_YELLOW" "Current container status:"
            docker-compose -f docker-compose.services.yml ps

            exit 4
        fi
    else
        print_colored "$COLOR_YELLOW" "⚠ Health check skipped (--skip-health-check)"
        log_message "Application services health check skipped"
    fi
}

# Function: Generate startup report
generate_report() {
    print_step "[Startup Complete]"

    local end_time=$(date '+%Y-%m-%d %H:%M:%S')

    # Container status
    echo ""
    print_colored "$COLOR_BLUE" "Container Status:"
    docker-compose -f docker-compose.infrastructure.yml -f docker-compose.services.yml ps

    # Service endpoints
    echo ""
    print_colored "$COLOR_BLUE" "Service Endpoints:"
    print_colored "$COLOR_GREEN" "  • API Gateway:      http://localhost:8080"
    print_colored "$COLOR_GREEN" "  • Auth Service:     http://localhost:8081"
    print_colored "$COLOR_GREEN" "  • Nacos Console:    http://localhost:8848/nacos (nacos/nacos)"
    print_colored "$COLOR_GREEN" "  • RabbitMQ UI:      http://localhost:15672 (admin/admin)"
    print_colored "$COLOR_GREEN" "  • MinIO Console:    http://localhost:9001 (admin/admin123)"

    # Health check endpoints
    echo ""
    print_colored "$COLOR_BLUE" "Health Check Endpoints:"
    print_colored "$COLOR_GREEN" "  • Gateway Health:   http://localhost:8080/actuator/health"
    print_colored "$COLOR_GREEN" "  • Auth Health:      http://localhost:8081/actuator/health"

    # Resource usage
    echo ""
    print_colored "$COLOR_BLUE" "Resource Usage:"
    docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}" \
        $(docker ps --filter "name=gcrf-" --format "{{.Names}}")

    # Summary
    echo ""
    print_colored "$COLOR_GREEN" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    print_colored "$COLOR_GREEN" "✅ GCRF Stack Started Successfully!"
    print_colored "$COLOR_GREEN" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    print_colored "$COLOR_GREEN" "Started at: $TIMESTAMP"
    print_colored "$COLOR_GREEN" "Completed:  $end_time"
    print_colored "$COLOR_GREEN" "Log file:   $STARTUP_LOG"

    log_message "Stack startup completed successfully at $end_time"
}

# Function: Cleanup on error
cleanup_on_error() {
    local exit_code=$?
    if [ $exit_code -ne 0 ]; then
        print_colored "$COLOR_RED" ""
        print_colored "$COLOR_RED" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        print_colored "$COLOR_RED" "✗ Startup failed with exit code: $exit_code"
        print_colored "$COLOR_RED" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        print_colored "$COLOR_RED" "Check log file for details: $STARTUP_LOG"

        log_message "Startup failed with exit code: $exit_code"

        # Show recent logs for debugging
        if [ -f "$STARTUP_LOG" ]; then
            echo ""
            print_colored "$COLOR_YELLOW" "Recent log entries:"
            tail -n 20 "$STARTUP_LOG"
        fi
    fi
}

# Main execution
main() {
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --skip-infrastructure)
                SKIP_INFRASTRUCTURE=true
                shift
                ;;
            --skip-health-check)
                SKIP_HEALTH_CHECK=true
                shift
                ;;
            --timeout)
                HEALTH_CHECK_TIMEOUT="$2"
                shift 2
                ;;
            --verbose)
                VERBOSE=true
                shift
                ;;
            --help)
                show_help
                exit 0
                ;;
            *)
                print_colored "$COLOR_RED" "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done

    # Setup error handling
    trap cleanup_on_error EXIT

    # Create log directory
    mkdir -p "$LOG_DIR"

    # Start logging
    log_message "================================================================"
    log_message "GCRF Stack Startup Script Started"
    log_message "================================================================"
    log_message "Script directory: $SCRIPT_DIR"
    log_message "Deployment directory: $DEPLOYMENT_DIR"
    log_message "Options: skip_infrastructure=$SKIP_INFRASTRUCTURE, skip_health_check=$SKIP_HEALTH_CHECK, timeout=$HEALTH_CHECK_TIMEOUT"

    # Print banner
    echo ""
    print_colored "$COLOR_BLUE" "╔════════════════════════════════════════╗"
    print_colored "$COLOR_BLUE" "║   GCRF Library Management System       ║"
    print_colored "$COLOR_BLUE" "║   Stack Startup Orchestration          ║"
    print_colored "$COLOR_BLUE" "╚════════════════════════════════════════╝"
    echo ""
    print_colored "$COLOR_YELLOW" "Starting at: $(date '+%Y-%m-%d %H:%M:%S')"

    # Execute startup steps
    check_prerequisites

    if [ "$SKIP_INFRASTRUCTURE" = false ]; then
        start_infrastructure
        wait_for_infrastructure
    else
        print_colored "$COLOR_YELLOW" "⚠ Infrastructure startup skipped (--skip-infrastructure)"
        log_message "Infrastructure startup skipped"
    fi

    start_services
    wait_for_services

    # Generate final report
    generate_report

    # Clear trap for successful exit
    trap - EXIT
}

# Run main function
main "$@"