#!/bin/bash

# ============================================
# GCRF Library Management System
# Infrastructure Stack Startup Script
# ============================================
# This script starts all infrastructure services in the correct order
# with health check validation before proceeding
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

# Function to check if Docker is running
check_docker() {
    if ! docker info &> /dev/null; then
        print_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
    print_info "Docker is running"
}

# Function to check if required files exist
check_files() {
    local missing_files=()

    # Check for environment file
    if [ ! -f "$PROJECT_ROOT/.env" ]; then
        if [ -f "$PROJECT_ROOT/.env.prod.example" ]; then
            print_warn ".env file not found. Please copy .env.prod.example to .env and configure it."
            missing_files+=(".env")
        fi
    fi

    # Check for Docker Compose file
    if [ ! -f "$DEPLOYMENT_DIR/docker-compose.infrastructure.yml" ]; then
        print_error "docker-compose.infrastructure.yml not found!"
        missing_files+=("docker-compose.infrastructure.yml")
    fi

    # Check for PostgreSQL init scripts
    if [ ! -d "$DEPLOYMENT_DIR/postgresql/init-scripts" ]; then
        print_warn "PostgreSQL init-scripts directory not found. Creating it..."
        mkdir -p "$DEPLOYMENT_DIR/postgresql/init-scripts"
    fi

    # Check for Nacos schema
    if [ ! -f "$DEPLOYMENT_DIR/nacos/nacos-mysql-schema.sql" ]; then
        print_warn "Nacos MySQL schema not found. Services might fail to start properly."
    fi

    if [ ${#missing_files[@]} -gt 0 ]; then
        print_error "Missing required files: ${missing_files[*]}"
        exit 1
    fi

    print_info "All required files found"
}

# Function to wait for a service to be healthy
wait_for_healthy() {
    local service_name=$1
    local max_wait=${2:-300}  # Default 5 minutes
    local interval=5
    local elapsed=0

    print_info "Waiting for $service_name to be healthy..."

    while [ $elapsed -lt $max_wait ]; do
        # Get container name with prefix
        local container_name="gcrf-${service_name}"

        # Check if container exists
        if ! docker ps -a --format "table {{.Names}}" | grep -q "^${container_name}$"; then
            print_warn "$container_name not found. Waiting..."
            sleep $interval
            elapsed=$((elapsed + interval))
            continue
        fi

        # Check health status
        local health_status=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' "$container_name" 2>/dev/null || echo "error")

        if [ "$health_status" = "healthy" ]; then
            print_info "✓ $service_name is healthy"
            return 0
        elif [ "$health_status" = "no-healthcheck" ]; then
            # For services without health checks, just check if running
            local running_status=$(docker inspect --format='{{.State.Running}}' "$container_name" 2>/dev/null || echo "false")
            if [ "$running_status" = "true" ]; then
                print_info "✓ $service_name is running (no health check defined)"
                return 0
            fi
        fi

        echo -ne "\r⏳ Waiting for $service_name... ($elapsed/$max_wait seconds) [Status: $health_status]"
        sleep $interval
        elapsed=$((elapsed + interval))
    done

    echo ""  # New line after the waiting message
    print_error "✗ $service_name failed to become healthy after $max_wait seconds"

    # Show container logs for debugging
    print_error "Last 20 lines of $container_name logs:"
    docker logs --tail 20 "gcrf-${service_name}" 2>&1 || true

    return 1
}

# Function to start infrastructure services
start_infrastructure() {
    print_info "Starting GCRF infrastructure services..."

    cd "$DEPLOYMENT_DIR"

    # Load environment variables if .env exists
    if [ -f "$PROJECT_ROOT/.env" ]; then
        print_info "Loading environment variables from .env"
        set -a
        source "$PROJECT_ROOT/.env"
        set +a
    fi

    # Start all infrastructure services
    print_info "Starting infrastructure containers..."
    docker-compose -f docker-compose.infrastructure.yml up -d

    # Wait for each service to be healthy
    local services=(
        "postgres-primary"
        "redis-master"
        "nacos-mysql"
        "nacos"
        "rabbitmq"
        "minio"
    )

    for service in "${services[@]}"; do
        if ! wait_for_healthy "$service"; then
            print_error "Failed to start infrastructure. Stopping all services..."
            docker-compose -f docker-compose.infrastructure.yml down
            exit 1
        fi
    done

    print_info "✅ All infrastructure services are healthy and ready!"
}

# Function to show service status
show_status() {
    print_info "Infrastructure service status:"
    echo ""
    docker-compose -f "$DEPLOYMENT_DIR/docker-compose.infrastructure.yml" ps
    echo ""

    print_info "Service endpoints:"
    echo "  PostgreSQL:    localhost:5432 (internal only)"
    echo "  Redis:         localhost:6379 (internal only)"
    echo "  Nacos UI:      http://localhost:8848/nacos"
    echo "  RabbitMQ UI:   http://localhost:15672"
    echo "  MinIO Console: http://localhost:9001"
    echo ""

    print_info "Network information:"
    docker network inspect gcrf-infrastructure-network --format '{{range .Containers}}{{.Name}}: {{.IPv4Address}}{{println}}{{end}}' 2>/dev/null || true
}

# Function to run post-startup checks
post_startup_checks() {
    print_info "Running post-startup checks..."

    # Check PostgreSQL databases
    print_info "Checking PostgreSQL databases..."
    docker exec gcrf-postgres-primary psql -U postgres -c '\l' | grep gcrf_ || print_warn "GCRF databases not found"

    # Check Redis
    print_info "Checking Redis connection..."
    docker exec gcrf-redis-master redis-cli ping || print_warn "Redis ping failed"

    # Check RabbitMQ
    print_info "Checking RabbitMQ status..."
    docker exec gcrf-rabbitmq rabbitmqctl status > /dev/null && echo "  RabbitMQ is operational" || print_warn "RabbitMQ status check failed"

    print_info "Post-startup checks complete"
}

# Main execution
main() {
    echo "============================================"
    echo "GCRF Library Management System"
    echo "Infrastructure Stack Startup"
    echo "============================================"
    echo ""

    # Run checks
    check_docker
    check_files

    # Start services
    start_infrastructure

    # Run post-startup checks
    post_startup_checks

    # Show status
    show_status

    print_info "Infrastructure startup complete!"
    print_info "You can now start the application services."
}

# Run main function
main "$@"