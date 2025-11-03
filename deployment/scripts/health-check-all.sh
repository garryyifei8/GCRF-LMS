#!/bin/bash
#
# GCRF Library Management System - System Health Check Script
#
# Purpose: Comprehensive health check for all GCRF stack components
#
# Usage: ./health-check-all.sh [options]
#   Options:
#     --json                 Output results in JSON format
#     --alerts-only          Only show unhealthy services
#     --detailed             Include detailed diagnostics
#     --watch <seconds>      Continuous monitoring mode (refresh interval)
#     --threshold <percent>  Resource alert threshold (default: 80)
#     --output <file>        Save report to file
#     --slack-webhook <url>  Send alerts to Slack
#     --email <address>      Send alerts via email (requires mail configured)
#     --verbose              Enable verbose output
#     --help                 Display this help message
#
# Exit codes:
#   0 - All services healthy
#   1 - Some services unhealthy
#   2 - Critical services down
#   3 - Resource thresholds exceeded
#   4 - Network connectivity issues

set -e

# Color codes for output
readonly COLOR_GREEN='\033[0;32m'
readonly COLOR_YELLOW='\033[1;33m'
readonly COLOR_RED='\033[0;31m'
readonly COLOR_BLUE='\033[0;34m'
readonly COLOR_CYAN='\033[0;36m'
readonly COLOR_RESET='\033[0m'

# Script configuration
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly DEPLOYMENT_DIR="$(dirname "$SCRIPT_DIR")"
readonly TIMESTAMP=$(date '+%Y%m%d_%H%M%S')

# Default values
JSON_OUTPUT=false
ALERTS_ONLY=false
DETAILED=false
WATCH_MODE=false
WATCH_INTERVAL=10
RESOURCE_THRESHOLD=80
OUTPUT_FILE=""
SLACK_WEBHOOK=""
EMAIL_ADDRESS=""
VERBOSE=false

# Health status tracking
declare -A SERVICE_STATUS
declare -A SERVICE_HEALTH
declare -A RESOURCE_USAGE
TOTAL_SERVICES=0
HEALTHY_SERVICES=0
UNHEALTHY_SERVICES=0
WARNING_SERVICES=0

# Critical services that must be healthy
readonly CRITICAL_SERVICES=(
    "gcrf-postgres-primary"
    "gcrf-redis-master"
    "gcrf-nacos"
)

# Function: Print colored output
print_colored() {
    local color=$1
    shift
    if [ "$JSON_OUTPUT" = false ]; then
        echo -e "${color}$*${COLOR_RESET}"
    fi
}

# Function: Print section header
print_header() {
    if [ "$JSON_OUTPUT" = false ]; then
        echo ""
        print_colored "$COLOR_BLUE" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        print_colored "$COLOR_BLUE" "$1"
        print_colored "$COLOR_BLUE" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    fi
}

# Function: Verbose log
verbose_log() {
    if [ "$VERBOSE" = true ] && [ "$JSON_OUTPUT" = false ]; then
        print_colored "$COLOR_CYAN" "[DEBUG] $*"
    fi
}

# Function: Display help
show_help() {
    grep '^#' "$0" | head -20 | tail -18 | sed 's/^# //' | sed 's/^#//'
}

# Function: Clear screen for watch mode
clear_screen() {
    if [ "$WATCH_MODE" = true ] && [ "$JSON_OUTPUT" = false ]; then
        clear
    fi
}

# Function: Get container health status
get_container_health() {
    local container=$1
    docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}no_health_check{{end}}' "$container" 2>/dev/null || echo "not_found"
}

# Function: Get container state
get_container_state() {
    local container=$1
    docker inspect --format='{{.State.Status}}' "$container" 2>/dev/null || echo "not_found"
}

# Function: Get container uptime
get_container_uptime() {
    local container=$1
    local started=$(docker inspect --format='{{.State.StartedAt}}' "$container" 2>/dev/null)

    if [ -n "$started" ] && [ "$started" != "0001-01-01T00:00:00Z" ]; then
        local started_ts=$(date -d "$started" +%s 2>/dev/null || date -r "$started" +%s 2>/dev/null || echo "0")
        local now_ts=$(date +%s)
        local uptime=$((now_ts - started_ts))

        if [ $uptime -gt 0 ]; then
            local days=$((uptime / 86400))
            local hours=$(((uptime % 86400) / 3600))
            local minutes=$(((uptime % 3600) / 60))

            if [ $days -gt 0 ]; then
                echo "${days}d ${hours}h ${minutes}m"
            elif [ $hours -gt 0 ]; then
                echo "${hours}h ${minutes}m"
            else
                echo "${minutes}m"
            fi
        else
            echo "just started"
        fi
    else
        echo "not running"
    fi
}

# Function: Get container resource usage
get_container_resources() {
    local container=$1
    local stats=$(docker stats --no-stream --format "{{.CPUPerc}}|{{.MemUsage}}|{{.MemPerc}}|{{.NetIO}}" "$container" 2>/dev/null)

    if [ -n "$stats" ]; then
        echo "$stats"
    else
        echo "0%|0B / 0B|0%|0B / 0B"
    fi
}

# Function: Check container health
check_container() {
    local container=$1
    local state=$(get_container_state "$container")
    local health=$(get_container_health "$container")
    local uptime=$(get_container_uptime "$container")
    local resources=$(get_container_resources "$container")

    # Parse resources
    IFS='|' read -r cpu mem mem_percent net <<< "$resources"

    # Store in associative arrays
    SERVICE_STATUS["$container"]="$state"
    SERVICE_HEALTH["$container"]="$health"
    RESOURCE_USAGE["$container"]="CPU: $cpu | Mem: $mem ($mem_percent) | Net: $net | Uptime: $uptime"

    # Determine overall status
    if [ "$state" = "not_found" ]; then
        return 2  # Container doesn't exist
    elif [ "$state" != "running" ]; then
        return 1  # Container not running
    elif [ "$health" = "unhealthy" ]; then
        return 1  # Container unhealthy
    elif [ "$health" = "starting" ]; then
        return 3  # Container starting (warning)
    else
        return 0  # Container healthy/running
    fi
}

# Function: Check all containers
check_all_containers() {
    local containers=$(docker ps -a --filter "name=gcrf-" --format "{{.Names}}")

    TOTAL_SERVICES=0
    HEALTHY_SERVICES=0
    UNHEALTHY_SERVICES=0
    WARNING_SERVICES=0

    for container in $containers; do
        ((TOTAL_SERVICES++))

        if check_container "$container"; then
            ((HEALTHY_SERVICES++))
        else
            local exit_code=$?
            if [ $exit_code -eq 3 ]; then
                ((WARNING_SERVICES++))
            else
                ((UNHEALTHY_SERVICES++))
            fi
        fi
    done
}

# Function: Display container status
display_container_status() {
    print_header "Container Status"

    local containers=$(docker ps -a --filter "name=gcrf-" --format "{{.Names}}" | sort)

    for container in $containers; do
        local state="${SERVICE_STATUS[$container]}"
        local health="${SERVICE_HEALTH[$container]}"
        local resources="${RESOURCE_USAGE[$container]}"

        # Skip if alerts only and healthy
        if [ "$ALERTS_ONLY" = true ] && [ "$state" = "running" ] && [ "$health" != "unhealthy" ]; then
            continue
        fi

        # Determine symbol and color
        local symbol=""
        local color=""

        if [ "$state" != "running" ]; then
            symbol="✗"
            color="$COLOR_RED"
        elif [ "$health" = "unhealthy" ]; then
            symbol="✗"
            color="$COLOR_RED"
        elif [ "$health" = "starting" ]; then
            symbol="⏳"
            color="$COLOR_YELLOW"
        elif [ "$health" = "healthy" ] || [ "$health" = "no_health_check" ]; then
            symbol="✓"
            color="$COLOR_GREEN"
        else
            symbol="?"
            color="$COLOR_YELLOW"
        fi

        # Display status
        print_colored "$color" "$symbol $container"
        print_colored "$COLOR_CYAN" "  State: $state | Health: $health"

        if [ "$DETAILED" = true ]; then
            print_colored "$COLOR_CYAN" "  Resources: $resources"
        fi
    done
}

# Function: Check service endpoints
check_endpoints() {
    print_header "Service Endpoints"

    local endpoints=(
        "Gateway API|http://localhost:8080/actuator/health"
        "Auth Service|http://localhost:8081/actuator/health"
        "Nacos Console|http://localhost:8848/nacos/"
        "RabbitMQ Management|http://localhost:15672/api/overview"
        "MinIO Console|http://localhost:9001"
    )

    for endpoint_info in "${endpoints[@]}"; do
        IFS='|' read -r name url <<< "$endpoint_info"

        # Skip if alerts only
        if [ "$ALERTS_ONLY" = true ]; then
            if curl -s -f -m 5 "$url" > /dev/null 2>&1; then
                continue
            fi
        fi

        if curl -s -f -m 5 "$url" > /dev/null 2>&1; then
            print_colored "$COLOR_GREEN" "✓ $name: Accessible"

            if [ "$DETAILED" = true ]; then
                local response_time=$(curl -s -o /dev/null -w "%{time_total}" -m 5 "$url" 2>/dev/null)
                print_colored "$COLOR_CYAN" "  Response time: ${response_time}s"
            fi
        else
            print_colored "$COLOR_RED" "✗ $name: Not accessible"
            verbose_log "  URL: $url"
        fi
    done
}

# Function: Check resource usage
check_resources() {
    print_header "Resource Usage"

    # System resources
    local cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1 2>/dev/null || echo "0")
    local mem_info=$(free -m | awk 'NR==2{printf "%.1f", $3*100/$2}' 2>/dev/null || echo "0")
    local disk_usage=$(df -h / | awk 'NR==2{print $5}' | sed 's/%//' 2>/dev/null || echo "0")

    # Display system resources
    local cpu_color="$COLOR_GREEN"
    local mem_color="$COLOR_GREEN"
    local disk_color="$COLOR_GREEN"

    [ "${cpu_usage%.*}" -ge "$RESOURCE_THRESHOLD" ] && cpu_color="$COLOR_RED"
    [ "${mem_info%.*}" -ge "$RESOURCE_THRESHOLD" ] && mem_color="$COLOR_RED"
    [ "$disk_usage" -ge "$RESOURCE_THRESHOLD" ] && disk_color="$COLOR_RED"

    print_colored "$COLOR_BLUE" "System Resources:"
    print_colored "$cpu_color" "  CPU Usage: ${cpu_usage}%"
    print_colored "$mem_color" "  Memory Usage: ${mem_info}%"
    print_colored "$disk_color" "  Disk Usage: ${disk_usage}%"

    # Docker resources
    echo ""
    print_colored "$COLOR_BLUE" "Container Resources:"

    local containers=$(docker ps --filter "name=gcrf-" --format "{{.Names}}" | sort)
    for container in $containers; do
        local stats=$(docker stats --no-stream --format "{{.CPUPerc}}|{{.MemPerc}}" "$container" 2>/dev/null)

        if [ -n "$stats" ]; then
            IFS='|' read -r cpu mem <<< "$stats"
            local cpu_val="${cpu%\%}"
            local mem_val="${mem%\%}"

            # Skip if alerts only and below threshold
            if [ "$ALERTS_ONLY" = true ]; then
                if [ "${cpu_val%.*}" -lt "$RESOURCE_THRESHOLD" ] && [ "${mem_val%.*}" -lt "$RESOURCE_THRESHOLD" ]; then
                    continue
                fi
            fi

            local container_color="$COLOR_GREEN"
            if [ "${cpu_val%.*}" -ge "$RESOURCE_THRESHOLD" ] || [ "${mem_val%.*}" -ge "$RESOURCE_THRESHOLD" ]; then
                container_color="$COLOR_YELLOW"
            fi

            print_colored "$container_color" "  $container: CPU $cpu | Memory $mem"
        fi
    done
}

# Function: Check network connectivity
check_network() {
    print_header "Network Connectivity"

    # Check Docker networks
    local networks=$(docker network ls --filter "name=gcrf" --format "{{.Name}}")

    for network in $networks; do
        local containers=$(docker network inspect "$network" --format='{{range .Containers}}{{.Name}} {{end}}' 2>/dev/null)
        local count=$(echo "$containers" | wc -w)

        if [ $count -gt 0 ]; then
            print_colored "$COLOR_GREEN" "✓ $network: $count containers connected"
            if [ "$DETAILED" = true ]; then
                print_colored "$COLOR_CYAN" "  Containers: $containers"
            fi
        else
            print_colored "$COLOR_YELLOW" "⚠ $network: No containers connected"
        fi
    done

    # Check inter-service connectivity
    if [ "$DETAILED" = true ]; then
        echo ""
        print_colored "$COLOR_BLUE" "Inter-service Connectivity:"

        # Test gateway -> auth service
        if docker exec gcrf-gateway-service curl -s -f http://gcrf-auth-service:8081/actuator/health > /dev/null 2>&1; then
            print_colored "$COLOR_GREEN" "  ✓ Gateway -> Auth Service: Connected"
        else
            print_colored "$COLOR_YELLOW" "  ⚠ Gateway -> Auth Service: Connection issue"
        fi

        # Test services -> Nacos
        for service in gcrf-gateway-service gcrf-auth-service; do
            if docker ps --format "{{.Names}}" | grep -q "$service"; then
                if docker exec "$service" curl -s -f http://gcrf-nacos:8848/nacos/ > /dev/null 2>&1; then
                    print_colored "$COLOR_GREEN" "  ✓ $service -> Nacos: Connected"
                else
                    print_colored "$COLOR_RED" "  ✗ $service -> Nacos: Not connected"
                fi
            fi
        done
    fi
}

# Function: Generate JSON report
generate_json_report() {
    local report=$(cat <<EOF
{
  "timestamp": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "summary": {
    "total_services": $TOTAL_SERVICES,
    "healthy": $HEALTHY_SERVICES,
    "unhealthy": $UNHEALTHY_SERVICES,
    "warning": $WARNING_SERVICES
  },
  "services": [
EOF
)

    local first=true
    for container in "${!SERVICE_STATUS[@]}"; do
        [ "$first" = true ] && first=false || report+=","

        report+=$(cat <<EOF

    {
      "name": "$container",
      "state": "${SERVICE_STATUS[$container]}",
      "health": "${SERVICE_HEALTH[$container]}",
      "resources": "${RESOURCE_USAGE[$container]}"
    }
EOF
)
    done

    report+=$(cat <<EOF

  ],
  "status": $([ $UNHEALTHY_SERVICES -eq 0 ] && echo '"healthy"' || echo '"unhealthy"')
}
EOF
)

    echo "$report"
}

# Function: Send Slack alert
send_slack_alert() {
    local message=$1

    if [ -n "$SLACK_WEBHOOK" ]; then
        local payload=$(cat <<EOF
{
  "text": "GCRF Health Check Alert",
  "attachments": [{
    "color": "danger",
    "title": "System Health Warning",
    "text": "$message",
    "footer": "GCRF Monitoring",
    "ts": $(date +%s)
  }]
}
EOF
)

        curl -X POST -H 'Content-type: application/json' \
            --data "$payload" \
            "$SLACK_WEBHOOK" > /dev/null 2>&1

        verbose_log "Slack alert sent"
    fi
}

# Function: Send email alert
send_email_alert() {
    local message=$1

    if [ -n "$EMAIL_ADDRESS" ] && command -v mail &> /dev/null; then
        echo "$message" | mail -s "GCRF Health Check Alert" "$EMAIL_ADDRESS"
        verbose_log "Email alert sent to $EMAIL_ADDRESS"
    fi
}

# Function: Generate summary
generate_summary() {
    print_header "Health Check Summary"

    # Overall status
    local overall_status="HEALTHY"
    local overall_color="$COLOR_GREEN"
    local exit_code=0

    if [ $UNHEALTHY_SERVICES -gt 0 ]; then
        overall_status="UNHEALTHY"
        overall_color="$COLOR_RED"
        exit_code=1

        # Check if critical services are down
        for critical in "${CRITICAL_SERVICES[@]}"; do
            if [ "${SERVICE_STATUS[$critical]}" != "running" ] || [ "${SERVICE_HEALTH[$critical]}" = "unhealthy" ]; then
                overall_status="CRITICAL"
                exit_code=2
                break
            fi
        done
    elif [ $WARNING_SERVICES -gt 0 ]; then
        overall_status="WARNING"
        overall_color="$COLOR_YELLOW"
    fi

    print_colored "$overall_color" ""
    print_colored "$overall_color" "Overall Status: $overall_status"
    print_colored "$COLOR_GREEN" "  Healthy Services: $HEALTHY_SERVICES/$TOTAL_SERVICES"

    if [ $WARNING_SERVICES -gt 0 ]; then
        print_colored "$COLOR_YELLOW" "  Warning Services: $WARNING_SERVICES/$TOTAL_SERVICES"
    fi

    if [ $UNHEALTHY_SERVICES -gt 0 ]; then
        print_colored "$COLOR_RED" "  Unhealthy Services: $UNHEALTHY_SERVICES/$TOTAL_SERVICES"

        # List unhealthy services
        echo ""
        print_colored "$COLOR_RED" "Unhealthy Services:"
        for container in "${!SERVICE_STATUS[@]}"; do
            if [ "${SERVICE_STATUS[$container]}" != "running" ] || [ "${SERVICE_HEALTH[$container]}" = "unhealthy" ]; then
                print_colored "$COLOR_RED" "  - $container (${SERVICE_STATUS[$container]}, ${SERVICE_HEALTH[$container]})"
            fi
        done

        # Send alerts
        local alert_message="GCRF Stack: $UNHEALTHY_SERVICES unhealthy services detected"
        send_slack_alert "$alert_message"
        send_email_alert "$alert_message"
    fi

    return $exit_code
}

# Function: Run health check
run_health_check() {
    clear_screen

    if [ "$JSON_OUTPUT" = false ]; then
        print_colored "$COLOR_BLUE" "╔════════════════════════════════════════╗"
        print_colored "$COLOR_BLUE" "║   GCRF System Health Check             ║"
        print_colored "$COLOR_BLUE" "╚════════════════════════════════════════╝"
        print_colored "$COLOR_CYAN" "Timestamp: $(date '+%Y-%m-%d %H:%M:%S')"
    fi

    # Run checks
    check_all_containers

    if [ "$JSON_OUTPUT" = true ]; then
        generate_json_report
    else
        display_container_status
        check_endpoints
        check_resources
        check_network
        generate_summary
    fi
}

# Main execution
main() {
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --json)
                JSON_OUTPUT=true
                shift
                ;;
            --alerts-only)
                ALERTS_ONLY=true
                shift
                ;;
            --detailed)
                DETAILED=true
                shift
                ;;
            --watch)
                WATCH_MODE=true
                WATCH_INTERVAL="$2"
                shift 2
                ;;
            --threshold)
                RESOURCE_THRESHOLD="$2"
                shift 2
                ;;
            --output)
                OUTPUT_FILE="$2"
                shift 2
                ;;
            --slack-webhook)
                SLACK_WEBHOOK="$2"
                shift 2
                ;;
            --email)
                EMAIL_ADDRESS="$2"
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

    # Check Docker daemon
    if ! docker ps &> /dev/null; then
        print_colored "$COLOR_RED" "Error: Docker daemon is not running"
        exit 1
    fi

    # Run health check
    if [ "$WATCH_MODE" = true ]; then
        # Continuous monitoring mode
        while true; do
            if [ -n "$OUTPUT_FILE" ]; then
                run_health_check > "$OUTPUT_FILE"
            else
                run_health_check
            fi

            if [ "$JSON_OUTPUT" = false ]; then
                echo ""
                print_colored "$COLOR_CYAN" "Refreshing in ${WATCH_INTERVAL}s... (Press Ctrl+C to stop)"
            fi

            sleep "$WATCH_INTERVAL"
        done
    else
        # Single run
        if [ -n "$OUTPUT_FILE" ]; then
            run_health_check > "$OUTPUT_FILE"
            [ "$JSON_OUTPUT" = false ] && print_colored "$COLOR_GREEN" "Report saved to: $OUTPUT_FILE"
        else
            run_health_check
        fi
    fi
}

# Run main function
main "$@"