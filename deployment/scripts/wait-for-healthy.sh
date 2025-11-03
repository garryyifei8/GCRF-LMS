#!/bin/bash
#
# GCRF Library Management System - Health Check Wait Script
#
# Purpose: Wait for Docker containers to become healthy
#
# Usage: ./wait-for-healthy.sh <container1> [container2] [...]
#
# Environment Variables:
#   MAX_WAIT      Maximum wait time in seconds (default: 300)
#   POLL_INTERVAL Poll interval in seconds (default: 5)
#   VERBOSE       Enable verbose output (default: false)
#   NO_COLOR      Disable colored output (default: false)
#
# Exit codes:
#   0 - All services healthy
#   1 - Service not found
#   2 - Service unhealthy
#   3 - Timeout reached
#   4 - Invalid arguments

set -e

# Configuration
readonly MAX_WAIT="${MAX_WAIT:-300}"
readonly POLL_INTERVAL="${POLL_INTERVAL:-5}"
readonly VERBOSE="${VERBOSE:-false}"
readonly NO_COLOR="${NO_COLOR:-false}"

# Color codes (disabled if NO_COLOR is set)
if [ "$NO_COLOR" = "true" ]; then
    readonly COLOR_GREEN=''
    readonly COLOR_YELLOW=''
    readonly COLOR_RED=''
    readonly COLOR_BLUE=''
    readonly COLOR_RESET=''
else
    readonly COLOR_GREEN='\033[0;32m'
    readonly COLOR_YELLOW='\033[1;33m'
    readonly COLOR_RED='\033[0;31m'
    readonly COLOR_BLUE='\033[0;34m'
    readonly COLOR_RESET='\033[0m'
fi

# Symbols for status
readonly SYMBOL_WAITING="⏳"
readonly SYMBOL_SUCCESS="✓"
readonly SYMBOL_FAILED="✗"
readonly SYMBOL_WARNING="⚠"

# Function: Print colored output
print_colored() {
    local color=$1
    shift
    echo -e "${color}$*${COLOR_RESET}"
}

# Function: Print verbose message
verbose_log() {
    if [ "$VERBOSE" = "true" ]; then
        print_colored "$COLOR_BLUE" "[DEBUG] $*"
    fi
}

# Function: Get container health status
get_health_status() {
    local container=$1
    docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}no_health_check{{end}}' "$container" 2>/dev/null || echo "not_found"
}

# Function: Get container state
get_container_state() {
    local container=$1
    docker inspect --format='{{.State.Status}}' "$container" 2>/dev/null || echo "not_found"
}

# Function: Get last health check log
get_last_health_log() {
    local container=$1
    docker inspect --format='{{if .State.Health}}{{range .State.Health.Log}}{{.Output}}{{end}}{{end}}' "$container" 2>/dev/null | tail -1
}

# Function: Check if container has health check
has_health_check() {
    local container=$1
    local health_check=$(docker inspect --format='{{if .State.Health}}true{{else}}false{{end}}' "$container" 2>/dev/null)
    [ "$health_check" = "true" ]
}

# Function: Wait for single container
wait_for_container() {
    local container=$1
    local elapsed=0
    local last_status=""
    local spinner=('⠋' '⠙' '⠹' '⠸' '⠼' '⠴' '⠦' '⠧' '⠇' '⠏')
    local spinner_index=0

    verbose_log "Starting health check for container: $container"

    # Initial check - does container exist?
    local state=$(get_container_state "$container")
    if [ "$state" = "not_found" ]; then
        print_colored "$COLOR_RED" "$SYMBOL_FAILED $container: Container not found"
        return 1
    fi

    # Check if container is running
    if [ "$state" != "running" ]; then
        print_colored "$COLOR_YELLOW" "$SYMBOL_WARNING $container: Container is $state (waiting for it to start...)"

        # Wait for container to start
        while [ "$state" != "running" ] && [ $elapsed -lt "$MAX_WAIT" ]; do
            sleep "$POLL_INTERVAL"
            elapsed=$((elapsed + POLL_INTERVAL))
            state=$(get_container_state "$container")

            if [ "$state" = "not_found" ]; then
                print_colored "$COLOR_RED" "$SYMBOL_FAILED $container: Container disappeared"
                return 1
            fi
        done

        if [ "$state" != "running" ]; then
            print_colored "$COLOR_RED" "$SYMBOL_FAILED $container: Container failed to start (state: $state)"
            return 3
        fi
    fi

    # Check if container has health check
    if ! has_health_check "$container"; then
        # No health check defined, just verify it's running
        print_colored "$COLOR_YELLOW" "$SYMBOL_WARNING $container: No health check defined (verifying running state)"
        sleep 5  # Give it a few seconds to potentially crash

        state=$(get_container_state "$container")
        if [ "$state" = "running" ]; then
            print_colored "$COLOR_GREEN" "$SYMBOL_SUCCESS $container: Running (no health check)"
            return 0
        else
            print_colored "$COLOR_RED" "$SYMBOL_FAILED $container: Container stopped (state: $state)"
            return 2
        fi
    fi

    # Container has health check, wait for it to become healthy
    print_colored "$COLOR_YELLOW" "$SYMBOL_WAITING Waiting for $container to be healthy..."

    while [ $elapsed -lt "$MAX_WAIT" ]; do
        local status=$(get_health_status "$container")

        verbose_log "$container: status=$status, elapsed=${elapsed}s"

        case "$status" in
            healthy)
                print_colored "$COLOR_GREEN" "$SYMBOL_SUCCESS $container is healthy (took ${elapsed}s)"
                return 0
                ;;

            unhealthy)
                print_colored "$COLOR_RED" "$SYMBOL_FAILED $container is unhealthy"

                # Show health check output for debugging
                local health_log=$(get_last_health_log "$container")
                if [ -n "$health_log" ]; then
                    print_colored "$COLOR_RED" "  Last health check: $health_log"
                fi

                # Show container logs
                print_colored "$COLOR_YELLOW" "  Recent logs from $container:"
                docker logs --tail 20 "$container" 2>&1 | sed 's/^/    /'

                return 2
                ;;

            starting)
                if [ "$status" != "$last_status" ]; then
                    print_colored "$COLOR_YELLOW" "  $container: Health check starting..."
                    last_status="$status"
                fi

                # Show spinner
                printf "\r  ${spinner[$spinner_index]} Waiting... ($elapsed/$MAX_WAIT seconds)"
                spinner_index=$(( (spinner_index + 1) % ${#spinner[@]} ))
                ;;

            not_found)
                print_colored "$COLOR_RED" "$SYMBOL_FAILED $container: Container not found"
                return 1
                ;;

            no_health_check)
                # This shouldn't happen as we checked earlier, but handle it
                print_colored "$COLOR_YELLOW" "$SYMBOL_WARNING $container: No health check (assuming healthy)"
                return 0
                ;;

            *)
                verbose_log "$container: Unknown status: $status"
                ;;
        esac

        sleep "$POLL_INTERVAL"
        elapsed=$((elapsed + POLL_INTERVAL))
    done

    # Clear spinner line
    printf "\r                                                  \r"

    # Timeout reached
    print_colored "$COLOR_RED" "$SYMBOL_FAILED $container: Timeout after ${MAX_WAIT}s"

    # Show debugging information
    print_colored "$COLOR_YELLOW" "  Container state: $(get_container_state "$container")"
    print_colored "$COLOR_YELLOW" "  Health status: $(get_health_status "$container")"

    # Show health check log
    local health_log=$(get_last_health_log "$container")
    if [ -n "$health_log" ]; then
        print_colored "$COLOR_YELLOW" "  Last health check: $health_log"
    fi

    # Show container logs
    print_colored "$COLOR_YELLOW" "  Recent logs from $container:"
    docker logs --tail 20 "$container" 2>&1 | sed 's/^/    /'

    return 3
}

# Function: Show summary
show_summary() {
    local total=$1
    local healthy=$2
    local failed=$((total - healthy))

    echo ""
    print_colored "$COLOR_BLUE" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    print_colored "$COLOR_BLUE" "Health Check Summary"
    print_colored "$COLOR_BLUE" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    print_colored "$COLOR_GREEN" "  Healthy:  $healthy/$total"

    if [ $failed -gt 0 ]; then
        print_colored "$COLOR_RED" "  Failed:   $failed/$total"
    fi

    if [ $healthy -eq $total ]; then
        print_colored "$COLOR_GREEN" ""
        print_colored "$COLOR_GREEN" "$SYMBOL_SUCCESS All containers are healthy!"
    else
        print_colored "$COLOR_RED" ""
        print_colored "$COLOR_RED" "$SYMBOL_FAILED Some containers failed health checks"
    fi
}

# Main execution
main() {
    # Check if any containers specified
    if [ $# -eq 0 ]; then
        print_colored "$COLOR_RED" "Error: No containers specified"
        echo "Usage: $0 <container1> [container2] [...]"
        echo ""
        echo "Environment variables:"
        echo "  MAX_WAIT=${MAX_WAIT}s      - Maximum wait time"
        echo "  POLL_INTERVAL=${POLL_INTERVAL}s  - Poll interval"
        echo "  VERBOSE=${VERBOSE}     - Verbose output"
        echo "  NO_COLOR=${NO_COLOR}    - Disable colors"
        exit 4
    fi

    # Show configuration
    verbose_log "Configuration:"
    verbose_log "  MAX_WAIT: ${MAX_WAIT}s"
    verbose_log "  POLL_INTERVAL: ${POLL_INTERVAL}s"
    verbose_log "  Containers to check: $*"

    # Track results
    local total_containers=$#
    local healthy_containers=0
    local failed_containers=()

    # Check Docker daemon
    if ! docker ps &> /dev/null; then
        print_colored "$COLOR_RED" "$SYMBOL_FAILED Docker daemon is not running"
        exit 1
    fi

    # Process each container
    print_colored "$COLOR_BLUE" "Checking health of $total_containers container(s)..."
    echo ""

    for container in "$@"; do
        if wait_for_container "$container"; then
            ((healthy_containers++))
        else
            failed_containers+=("$container")
        fi
        echo ""  # Add spacing between containers
    done

    # Show summary
    show_summary "$total_containers" "$healthy_containers"

    # Exit based on results
    if [ ${#failed_containers[@]} -eq 0 ]; then
        exit 0
    else
        verbose_log "Failed containers: ${failed_containers[*]}"
        exit 3
    fi
}

# Run main function with all arguments
main "$@"