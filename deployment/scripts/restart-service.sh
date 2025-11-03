#!/bin/bash
#
# GCRF Library Management System - Service Restart Script
#
# Purpose: Gracefully restart individual services with health verification
#
# Usage: ./restart-service.sh <service-name> [options]
#   Options:
#     --rolling              Enable rolling restart (zero-downtime)
#     --force                Force restart without health checks
#     --timeout <seconds>    Health check timeout (default: 120)
#     --scale <number>       Scale service to N instances during rolling restart
#     --verbose              Enable verbose output
#     --help                 Display this help message
#
# Examples:
#   ./restart-service.sh auth-service
#   ./restart-service.sh gateway-service --rolling --scale 2
#   ./restart-service.sh redis-master --force
#
# Exit codes:
#   0 - Success
#   1 - Invalid arguments
#   2 - Service not found
#   3 - Restart failed
#   4 - Health check failed
#   5 - Rollback failed

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

# Default values
ROLLING_RESTART=false
FORCE_RESTART=false
HEALTH_TIMEOUT=120
SCALE_COUNT=2
VERBOSE=false

# Service mappings (service name -> container name)
declare -A SERVICE_MAP=(
    ["postgres"]="gcrf-postgres-primary"
    ["redis"]="gcrf-redis-master"
    ["nacos"]="gcrf-nacos"
    ["rabbitmq"]="gcrf-rabbitmq"
    ["minio"]="gcrf-minio"
    ["gateway"]="gcrf-gateway-service"
    ["auth"]="gcrf-auth-service"
    ["gateway-service"]="gcrf-gateway-service"
    ["auth-service"]="gcrf-auth-service"
)

# Service compose files
declare -A COMPOSE_MAP=(
    ["gcrf-postgres-primary"]="docker-compose.infrastructure.yml"
    ["gcrf-redis-master"]="docker-compose.infrastructure.yml"
    ["gcrf-nacos"]="docker-compose.infrastructure.yml"
    ["gcrf-rabbitmq"]="docker-compose.infrastructure.yml"
    ["gcrf-minio"]="docker-compose.infrastructure.yml"
    ["gcrf-gateway-service"]="docker-compose.services.yml"
    ["gcrf-auth-service"]="docker-compose.services.yml"
)

# Function: Print colored output
print_colored() {
    local color=$1
    shift
    echo -e "${color}$*${COLOR_RESET}"
}

# Function: Verbose log
verbose_log() {
    if [ "$VERBOSE" = true ]; then
        print_colored "$COLOR_BLUE" "[DEBUG] $*"
    fi
}

# Function: Display help
show_help() {
    grep '^#' "$0" | head -22 | tail -20 | sed 's/^# //' | sed 's/^#//'
}

# Function: Get container name from service name
get_container_name() {
    local service=$1
    local container_name

    # Check if it's already a full container name
    if [[ "$service" == gcrf-* ]]; then
        container_name="$service"
    else
        # Try to map from short name
        container_name="${SERVICE_MAP[$service]}"
        if [ -z "$container_name" ]; then
            # Try adding gcrf- prefix
            container_name="gcrf-$service"
        fi
    fi

    echo "$container_name"
}

# Function: Get compose file for container
get_compose_file() {
    local container=$1
    echo "${COMPOSE_MAP[$container]:-docker-compose.services.yml}"
}

# Function: Check if container exists
container_exists() {
    local container=$1
    docker ps -a --format "{{.Names}}" | grep -q "^${container}$"
}

# Function: Check if container is running
container_is_running() {
    local container=$1
    docker ps --format "{{.Names}}" | grep -q "^${container}$"
}

# Function: Get container health status
get_health_status() {
    local container=$1
    docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}no_health_check{{end}}' "$container" 2>/dev/null || echo "not_found"
}

# Function: Wait for container health
wait_for_health() {
    local container=$1
    local timeout=$2

    print_colored "$COLOR_YELLOW" "Waiting for $container to be healthy (timeout: ${timeout}s)..."

    if MAX_WAIT="$timeout" "$SCRIPT_DIR/wait-for-healthy.sh" "$container"; then
        return 0
    else
        return 1
    fi
}

# Function: Save container state
save_container_state() {
    local container=$1
    local state_file="/tmp/gcrf_restart_${container}_${TIMESTAMP}.json"

    verbose_log "Saving state for $container to $state_file"

    # Save container inspect output
    docker inspect "$container" > "$state_file" 2>/dev/null || true

    echo "$state_file"
}

# Function: Standard restart
standard_restart() {
    local container=$1
    local compose_file=$2

    print_colored "$COLOR_YELLOW" "Performing standard restart of $container..."

    # Save current state
    local state_file=$(save_container_state "$container")

    # Stop container
    print_colored "$COLOR_YELLOW" "Stopping $container..."
    if docker stop "$container" --time 30; then
        print_colored "$COLOR_GREEN" "✓ Container stopped"
    else
        print_colored "$COLOR_RED" "✗ Failed to stop container"
        return 1
    fi

    # Start container
    print_colored "$COLOR_YELLOW" "Starting $container..."
    cd "$DEPLOYMENT_DIR"
    if docker-compose -f "$compose_file" up -d "$container" 2>&1; then
        print_colored "$COLOR_GREEN" "✓ Container started"
    else
        print_colored "$COLOR_RED" "✗ Failed to start container"

        # Try to restore from saved state
        print_colored "$COLOR_YELLOW" "Attempting to recover..."
        docker start "$container" 2>/dev/null || true
        return 1
    fi

    # Clean up state file
    rm -f "$state_file"

    return 0
}

# Function: Rolling restart (zero-downtime)
rolling_restart() {
    local container=$1
    local compose_file=$2
    local scale=$3

    print_colored "$COLOR_BLUE" "Performing rolling restart of $container (scale: $scale)..."

    cd "$DEPLOYMENT_DIR"

    # Get service name from compose file
    local service_name=$(docker-compose -f "$compose_file" ps --services | grep -E "${container#gcrf-}" | head -1)

    if [ -z "$service_name" ]; then
        print_colored "$COLOR_YELLOW" "⚠ Cannot determine service name, falling back to standard restart"
        return standard_restart "$container" "$compose_file"
    fi

    verbose_log "Service name: $service_name"

    # Step 1: Scale up
    print_colored "$COLOR_YELLOW" "Scaling up $service_name to $scale instances..."
    if docker-compose -f "$compose_file" up -d --scale "$service_name=$scale" "$service_name" 2>&1; then
        print_colored "$COLOR_GREEN" "✓ Scaled to $scale instances"
    else
        print_colored "$COLOR_RED" "✗ Failed to scale up"
        return 1
    fi

    # Step 2: Wait for new instances to be healthy
    print_colored "$COLOR_YELLOW" "Waiting for new instances to be healthy..."
    sleep 10

    # Get list of all instances
    local instances=$(docker-compose -f "$compose_file" ps -q "$service_name")
    local healthy_count=0

    for instance in $instances; do
        local instance_name=$(docker inspect --format='{{.Name}}' "$instance" | sed 's/^\/\+//')
        local health=$(get_health_status "$instance_name")

        if [ "$health" = "healthy" ] || [ "$health" = "no_health_check" ]; then
            ((healthy_count++))
            print_colored "$COLOR_GREEN" "  ✓ $instance_name is ready"
        else
            print_colored "$COLOR_YELLOW" "  ⏳ $instance_name: $health"
        fi
    done

    if [ $healthy_count -lt $scale ]; then
        print_colored "$COLOR_YELLOW" "⚠ Not all instances are healthy, but continuing..."
    fi

    # Step 3: Remove old instances
    print_colored "$COLOR_YELLOW" "Removing old instances..."

    # Stop the original container
    docker stop "$container" --time 30 2>/dev/null || true
    docker rm "$container" 2>/dev/null || true

    # Step 4: Scale back down
    print_colored "$COLOR_YELLOW" "Scaling back to 1 instance..."
    if docker-compose -f "$compose_file" up -d --scale "$service_name=1" "$service_name" 2>&1; then
        print_colored "$COLOR_GREEN" "✓ Scaled back to 1 instance"
    else
        print_colored "$COLOR_YELLOW" "⚠ Could not scale back (may need manual intervention)"
    fi

    return 0
}

# Function: Force restart
force_restart() {
    local container=$1

    print_colored "$COLOR_YELLOW" "Force restarting $container..."

    # Kill and remove container
    print_colored "$COLOR_YELLOW" "Killing $container..."
    docker kill "$container" 2>/dev/null || true
    docker rm "$container" 2>/dev/null || true

    # Restart using compose
    local compose_file=$(get_compose_file "$container")
    cd "$DEPLOYMENT_DIR"

    print_colored "$COLOR_YELLOW" "Starting $container..."
    if docker-compose -f "$compose_file" up -d 2>&1; then
        print_colored "$COLOR_GREEN" "✓ Container force restarted"
        return 0
    else
        print_colored "$COLOR_RED" "✗ Failed to restart container"
        return 1
    fi
}

# Function: Verify service after restart
verify_service() {
    local container=$1

    print_colored "$COLOR_BLUE" "Verifying service health..."

    # Check if container is running
    if ! container_is_running "$container"; then
        print_colored "$COLOR_RED" "✗ Container is not running"
        return 1
    fi

    # Check health status
    local health=$(get_health_status "$container")

    case "$health" in
        healthy)
            print_colored "$COLOR_GREEN" "✓ Container is healthy"
            ;;
        no_health_check)
            print_colored "$COLOR_GREEN" "✓ Container is running (no health check defined)"
            ;;
        starting)
            print_colored "$COLOR_YELLOW" "⏳ Health check is starting..."
            if wait_for_health "$container" "$HEALTH_TIMEOUT"; then
                print_colored "$COLOR_GREEN" "✓ Container became healthy"
            else
                print_colored "$COLOR_RED" "✗ Container failed health check"
                return 1
            fi
            ;;
        unhealthy)
            print_colored "$COLOR_RED" "✗ Container is unhealthy"

            # Show logs
            print_colored "$COLOR_YELLOW" "Recent logs:"
            docker logs --tail 20 "$container" 2>&1 | sed 's/^/  /'
            return 1
            ;;
        *)
            print_colored "$COLOR_YELLOW" "⚠ Unknown health status: $health"
            ;;
    esac

    # Additional service-specific checks
    case "$container" in
        gcrf-gateway-service)
            print_colored "$COLOR_YELLOW" "Checking Gateway API..."
            if curl -s -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
                print_colored "$COLOR_GREEN" "✓ Gateway API is responding"
            else
                print_colored "$COLOR_YELLOW" "⚠ Gateway API not responding yet"
            fi
            ;;
        gcrf-auth-service)
            print_colored "$COLOR_YELLOW" "Checking Auth Service API..."
            if curl -s -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
                print_colored "$COLOR_GREEN" "✓ Auth Service API is responding"
            else
                print_colored "$COLOR_YELLOW" "⚠ Auth Service API not responding yet"
            fi
            ;;
        gcrf-nacos)
            print_colored "$COLOR_YELLOW" "Checking Nacos console..."
            if curl -s -f http://localhost:8848/nacos/ > /dev/null 2>&1; then
                print_colored "$COLOR_GREEN" "✓ Nacos console is accessible"
            else
                print_colored "$COLOR_YELLOW" "⚠ Nacos console not responding yet"
            fi
            ;;
    esac

    return 0
}

# Main execution
main() {
    # Check arguments
    if [ $# -eq 0 ]; then
        print_colored "$COLOR_RED" "Error: No service specified"
        show_help
        exit 1
    fi

    # Get service name
    local service_input=$1
    shift

    # Parse options
    while [[ $# -gt 0 ]]; do
        case $1 in
            --rolling)
                ROLLING_RESTART=true
                shift
                ;;
            --force)
                FORCE_RESTART=true
                shift
                ;;
            --timeout)
                HEALTH_TIMEOUT="$2"
                shift 2
                ;;
            --scale)
                SCALE_COUNT="$2"
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

    # Create log directory
    mkdir -p "$LOG_DIR"

    # Get container name
    local container=$(get_container_name "$service_input")

    if [ -z "$container" ]; then
        print_colored "$COLOR_RED" "Error: Unknown service: $service_input"
        print_colored "$COLOR_YELLOW" "Available services:"
        for key in "${!SERVICE_MAP[@]}"; do
            echo "  - $key (${SERVICE_MAP[$key]})"
        done
        exit 2
    fi

    verbose_log "Container name: $container"

    # Check if container exists
    if ! container_exists "$container"; then
        print_colored "$COLOR_RED" "Error: Container $container does not exist"

        print_colored "$COLOR_YELLOW" "Available containers:"
        docker ps -a --filter "name=gcrf-" --format "table {{.Names}}\t{{.Status}}"
        exit 2
    fi

    # Get compose file
    local compose_file=$(get_compose_file "$container")
    verbose_log "Compose file: $compose_file"

    # Print header
    print_colored "$COLOR_BLUE" "╔════════════════════════════════════════╗"
    print_colored "$COLOR_BLUE" "║   GCRF Service Restart                 ║"
    print_colored "$COLOR_BLUE" "╚════════════════════════════════════════╝"
    echo ""
    print_colored "$COLOR_BLUE" "Service: $container"
    print_colored "$COLOR_BLUE" "Time: $(date '+%Y-%m-%d %H:%M:%S')"

    # Show current status
    echo ""
    print_colored "$COLOR_BLUE" "Current Status:"
    docker ps -a --filter "name=$container" --format "table {{.Names}}\t{{.Status}}\t{{.State}}"

    # Perform restart
    echo ""
    local restart_result=0

    if [ "$FORCE_RESTART" = true ]; then
        force_restart "$container" || restart_result=$?
    elif [ "$ROLLING_RESTART" = true ]; then
        rolling_restart "$container" "$compose_file" "$SCALE_COUNT" || restart_result=$?
    else
        standard_restart "$container" "$compose_file" || restart_result=$?
    fi

    if [ $restart_result -ne 0 ]; then
        print_colored "$COLOR_RED" "✗ Restart failed"
        exit 3
    fi

    # Verify service
    echo ""
    if [ "$FORCE_RESTART" = false ]; then
        if verify_service "$container"; then
            echo ""
            print_colored "$COLOR_GREEN" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            print_colored "$COLOR_GREEN" "✅ Service restarted successfully!"
            print_colored "$COLOR_GREEN" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        else
            print_colored "$COLOR_RED" "✗ Service verification failed"
            exit 4
        fi
    else
        echo ""
        print_colored "$COLOR_GREEN" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        print_colored "$COLOR_GREEN" "✅ Service force restarted!"
        print_colored "$COLOR_GREEN" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        print_colored "$COLOR_YELLOW" "⚠ Health verification skipped (--force)"
    fi
}

# Run main function
main "$@"