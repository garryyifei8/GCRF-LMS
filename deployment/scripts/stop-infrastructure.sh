#!/bin/bash

# ============================================
# GCRF Library Management System
# Infrastructure Stack Shutdown Script
# ============================================
# This script gracefully stops all infrastructure services
# ensuring data is properly flushed and persisted
# ============================================

set -e  # Exit on any error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
DEPLOYMENT_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$DEPLOYMENT_DIR")"

# Function to print colored messages
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to flush Redis data
flush_redis() {
    print_info "Flushing Redis data to disk..."
    if docker ps --format "table {{.Names}}" | grep -q "gcrf-redis-master"; then
        # Get Redis password from environment
        if [ -f "$PROJECT_ROOT/.env" ]; then
            source "$PROJECT_ROOT/.env"
            docker exec gcrf-redis-master redis-cli -a "${REDIS_PASSWORD}" BGSAVE 2>/dev/null || \
            docker exec gcrf-redis-master redis-cli BGSAVE 2>/dev/null || \
            print_warn "Could not flush Redis data (container might be using authentication)"
        else
            docker exec gcrf-redis-master redis-cli BGSAVE 2>/dev/null || \
            print_warn "Could not flush Redis data"
        fi

        # Wait for background save to complete
        sleep 2

        print_info "Redis data flushed"
    else
        print_warn "Redis container not running"
    fi
}

# Function to checkpoint PostgreSQL
checkpoint_postgres() {
    print_info "Checkpointing PostgreSQL..."
    if docker ps --format "table {{.Names}}" | grep -q "gcrf-postgres-primary"; then
        docker exec gcrf-postgres-primary psql -U postgres -c "CHECKPOINT;" 2>/dev/null || \
        print_warn "Could not checkpoint PostgreSQL"
        print_info "PostgreSQL checkpointed"
    else
        print_warn "PostgreSQL container not running"
    fi
}

# Function to stop services gracefully
stop_infrastructure() {
    print_info "Stopping GCRF infrastructure services..."

    cd "$DEPLOYMENT_DIR"

    # First, flush data from memory-based services
    flush_redis
    checkpoint_postgres

    # Stop all services with extended timeout for graceful shutdown
    print_info "Stopping infrastructure containers (timeout: 60s)..."
    docker-compose -f docker-compose.infrastructure.yml down --timeout 60

    print_info "Infrastructure services stopped"
}

# Function to check if any services are still running
check_remaining() {
    print_info "Checking for remaining GCRF containers..."

    local remaining=$(docker ps --format "table {{.Names}}" | grep "gcrf-" || true)

    if [ -n "$remaining" ]; then
        print_warn "The following GCRF containers are still running:"
        echo "$remaining"
        echo ""
        read -p "Do you want to force stop these containers? (y/N) " -n 1 -r
        echo ""
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker ps --format "{{.Names}}" | grep "gcrf-" | xargs -r docker stop
            print_info "Forced stop of remaining containers"
        fi
    else
        print_info "No GCRF containers are running"
    fi
}

# Function to optionally remove volumes
remove_volumes() {
    echo ""
    print_warn "Volume removal will DELETE all persisted data!"
    read -p "Do you want to remove Docker volumes? (y/N) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_info "Removing infrastructure volumes..."
        docker volume ls --format "{{.Name}}" | grep "gcrf-" | xargs -r docker volume rm
        print_info "Volumes removed"
    else
        print_info "Volumes preserved"

        # Show volume information
        print_info "Existing volumes:"
        docker volume ls --format "table {{.Name}}\t{{.Driver}}\t{{.Mountpoint}}" | grep "gcrf-" || echo "No GCRF volumes found"
    fi
}

# Function to remove network
remove_network() {
    print_info "Checking infrastructure network..."

    if docker network ls --format "{{.Name}}" | grep -q "gcrf-infrastructure-network"; then
        docker network rm gcrf-infrastructure-network 2>/dev/null || \
        print_warn "Could not remove network (might still be in use)"
    fi
}

# Main execution
main() {
    echo "============================================"
    echo "GCRF Library Management System"
    echo "Infrastructure Stack Shutdown"
    echo "============================================"
    echo ""

    # Parse command line arguments
    REMOVE_VOLUMES=false
    FORCE_STOP=false

    while [[ $# -gt 0 ]]; do
        case $1 in
            --remove-volumes|-v)
                REMOVE_VOLUMES=true
                shift
                ;;
            --force|-f)
                FORCE_STOP=true
                shift
                ;;
            --help|-h)
                echo "Usage: $0 [OPTIONS]"
                echo ""
                echo "Options:"
                echo "  -v, --remove-volumes  Remove Docker volumes (data will be lost)"
                echo "  -f, --force          Force stop without graceful shutdown"
                echo "  -h, --help           Show this help message"
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                echo "Use --help for usage information"
                exit 1
                ;;
        esac
    done

    # Stop services
    if [ "$FORCE_STOP" = true ]; then
        print_warn "Force stopping all services..."
        cd "$DEPLOYMENT_DIR"
        docker-compose -f docker-compose.infrastructure.yml down --timeout 5
    else
        stop_infrastructure
    fi

    # Check for remaining containers
    check_remaining

    # Remove network
    remove_network

    # Optionally remove volumes
    if [ "$REMOVE_VOLUMES" = true ]; then
        remove_volumes
    else
        print_info "Volumes preserved. Use --remove-volumes to delete data."
    fi

    print_info "✅ Infrastructure shutdown complete!"
}

# Run main function
main "$@"