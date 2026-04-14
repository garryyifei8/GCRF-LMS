#!/bin/bash

################################################################################
# GCRF Library Management System - Health Check Script
#
# This script performs comprehensive health checks on all system components.
#
# Usage: ./health-check.sh [options]
#
# Options:
#   --verbose      Show detailed output
#   --json         Output results in JSON format
#   --nagios       Nagios-compatible exit codes and output
#   --email        Send results via email (requires mail command)
#
# Exit Codes:
#   0 - All checks passed
#   1 - Warning: Some non-critical checks failed
#   2 - Critical: Critical checks failed
################################################################################

set -u

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
VERBOSE=false
JSON_OUTPUT=false
NAGIOS_MODE=false
SEND_EMAIL=false
EMAIL_TO="ops@gcrf-library.com"

# Results tracking
CHECKS_PASSED=0
CHECKS_FAILED=0
CHECKS_WARNING=0
FAILED_CHECKS=()
WARNING_CHECKS=()

################################################################################
# Functions
################################################################################

log() {
    if [ "$JSON_OUTPUT" = false ]; then
        local level=$1
        shift
        local message="$@"

        case $level in
            INFO)
                echo -e "${BLUE}[INFO]${NC} $message"
                ;;
            SUCCESS)
                echo -e "${GREEN}[✓]${NC} $message"
                ;;
            WARNING)
                echo -e "${YELLOW}[!]${NC} $message"
                ;;
            ERROR)
                echo -e "${RED}[✗]${NC} $message"
                ;;
            *)
                echo "$message"
                ;;
        esac
    fi
}

check_result() {
    local check_name=$1
    local result=$2
    local message=$3
    local level=${4:-CRITICAL}  # CRITICAL or WARNING

    if [ "$result" -eq 0 ]; then
        log SUCCESS "$check_name: $message"
        CHECKS_PASSED=$((CHECKS_PASSED + 1))
    else
        if [ "$level" = "WARNING" ]; then
            log WARNING "$check_name: $message"
            CHECKS_WARNING=$((CHECKS_WARNING + 1))
            WARNING_CHECKS+=("$check_name: $message")
        else
            log ERROR "$check_name: $message"
            CHECKS_FAILED=$((CHECKS_FAILED + 1))
            FAILED_CHECKS+=("$check_name: $message")
        fi
    fi
}

# System Resources Checks
check_system_resources() {
    log INFO "Checking system resources..."

    # CPU usage
    local cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | sed 's/%us,//')
    if (( $(echo "$cpu_usage < 80" | bc -l) )); then
        check_result "CPU Usage" 0 "CPU usage is ${cpu_usage}%" "WARNING"
    else
        check_result "CPU Usage" 1 "CPU usage is high: ${cpu_usage}%" "WARNING"
    fi

    # Memory usage
    local mem_total=$(free | grep Mem | awk '{print $2}')
    local mem_used=$(free | grep Mem | awk '{print $3}')
    local mem_percent=$(awk "BEGIN {printf \"%.1f\", ($mem_used/$mem_total)*100}")

    if (( $(echo "$mem_percent < 85" | bc -l) )); then
        check_result "Memory Usage" 0 "Memory usage is ${mem_percent}%"
    else
        check_result "Memory Usage" 1 "Memory usage is high: ${mem_percent}%" "WARNING"
    fi

    # Disk usage
    local disk_usage=$(df -h / | tail -1 | awk '{print $5}' | sed 's/%//')
    if [ "$disk_usage" -lt 85 ]; then
        check_result "Disk Usage" 0 "Disk usage is ${disk_usage}%"
    else
        check_result "Disk Usage" 1 "Disk usage is high: ${disk_usage}%" "WARNING"
    fi

    # Load average
    local load_avg=$(uptime | awk -F'load average:' '{print $2}' | awk '{print $1}' | sed 's/,//')
    local cpu_cores=$(nproc)
    if (( $(echo "$load_avg < $cpu_cores" | bc -l) )); then
        check_result "Load Average" 0 "Load average is ${load_avg}"
    else
        check_result "Load Average" 1 "Load average is high: ${load_avg} (cores: ${cpu_cores})" "WARNING"
    fi
}

# Docker Health Checks
check_docker() {
    log INFO "Checking Docker..."

    # Docker daemon
    if docker info &> /dev/null; then
        check_result "Docker Daemon" 0 "Docker daemon is running"
    else
        check_result "Docker Daemon" 1 "Docker daemon is not responding"
        return
    fi

    # Container count
    local running_containers=$(docker ps -q | wc -l)
    local expected_containers=15  # Adjust based on your setup

    if [ "$running_containers" -ge $((expected_containers - 2)) ]; then
        check_result "Docker Containers" 0 "$running_containers containers running"
    else
        check_result "Docker Containers" 1 "Only $running_containers containers running (expected: ~$expected_containers)" "WARNING"
    fi

    # Check for exited containers
    local exited_containers=$(docker ps -a -f status=exited | tail -n +2 | wc -l)
    if [ "$exited_containers" -eq 0 ]; then
        check_result "Exited Containers" 0 "No exited containers"
    else
        check_result "Exited Containers" 1 "$exited_containers containers have exited" "WARNING"
    fi
}

# Infrastructure Services Checks
check_infrastructure() {
    log INFO "Checking infrastructure services..."

    # PostgreSQL
    if docker exec postgres-primary pg_isready -U postgres &> /dev/null; then
        check_result "PostgreSQL" 0 "PostgreSQL is ready"

        # Check connections
        local connections=$(docker exec postgres-primary psql -U postgres -t -c "SELECT count(*) FROM pg_stat_activity;" 2>/dev/null | tr -d ' ')
        if [ -n "$connections" ]; then
            if [ "$connections" -lt 100 ]; then
                check_result "PostgreSQL Connections" 0 "$connections active connections"
            else
                check_result "PostgreSQL Connections" 1 "High number of connections: $connections" "WARNING"
            fi
        fi
    else
        check_result "PostgreSQL" 1 "PostgreSQL is not ready"
    fi

    # Redis
    if docker exec redis-master redis-cli ping &> /dev/null 2>&1; then
        check_result "Redis" 0 "Redis is responding"

        # Check memory usage
        local redis_mem=$(docker exec redis-master redis-cli info memory 2>/dev/null | grep used_memory_human | cut -d: -f2 | tr -d '\r\n ')
        if [ -n "$redis_mem" ] && [ "$VERBOSE" = true ]; then
            log INFO "Redis memory usage: $redis_mem"
        fi
    else
        check_result "Redis" 1 "Redis is not responding"
    fi

    # RabbitMQ
    if curl -sf http://localhost:15672/api/healthchecks/node &> /dev/null; then
        check_result "RabbitMQ" 0 "RabbitMQ is healthy"

        # Check queue depth
        local queue_count=$(curl -sf -u guest:guest http://localhost:15672/api/queues 2>/dev/null | grep -o '"messages":[0-9]*' | cut -d: -f2 | paste -sd+ | bc 2>/dev/null || echo "0")
        if [ "$queue_count" -lt 10000 ]; then
            check_result "RabbitMQ Queues" 0 "$queue_count messages in queues"
        else
            check_result "RabbitMQ Queues" 1 "High number of messages in queues: $queue_count" "WARNING"
        fi
    else
        check_result "RabbitMQ" 1 "RabbitMQ is not healthy"
    fi

    # Nacos
    if curl -sf http://localhost:8848/nacos/v1/console/health/readiness &> /dev/null; then
        check_result "Nacos" 0 "Nacos is ready"

        # Check service count
        local service_count=$(curl -sf http://localhost:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=100 2>/dev/null | grep -o '"count":[0-9]*' | cut -d: -f2)
        if [ -n "$service_count" ] && [ "$service_count" -gt 0 ]; then
            check_result "Nacos Services" 0 "$service_count services registered"
        else
            check_result "Nacos Services" 1 "No services registered in Nacos" "WARNING"
        fi
    else
        check_result "Nacos" 1 "Nacos is not ready"
    fi

    # MinIO
    if curl -sf http://localhost:9000/minio/health/live &> /dev/null; then
        check_result "MinIO" 0 "MinIO is healthy"
    else
        check_result "MinIO" 1 "MinIO is not healthy"
    fi

    # Elasticsearch
    if curl -sf http://localhost:9200/_cluster/health &> /dev/null; then
        check_result "Elasticsearch" 0 "Elasticsearch is healthy"

        local es_status=$(curl -sf http://localhost:9200/_cluster/health 2>/dev/null | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        if [ "$es_status" = "green" ]; then
            check_result "Elasticsearch Cluster" 0 "Cluster status is green"
        elif [ "$es_status" = "yellow" ]; then
            check_result "Elasticsearch Cluster" 1 "Cluster status is yellow" "WARNING"
        else
            check_result "Elasticsearch Cluster" 1 "Cluster status is red"
        fi
    else
        check_result "Elasticsearch" 1 "Elasticsearch is not responding"
    fi
}

# Application Services Checks
check_application_services() {
    log INFO "Checking application services..."

    local services=(
        "8080:Gateway Service"
        "8081:Auth Service"
        "8082:Book Service"
        "8083:Circulation Service"
        "8084:Reader Service"
        "8085:System Service"
        "8086:Notification Service"
    )

    for service in "${services[@]}"; do
        local port="${service%%:*}"
        local name="${service##*:}"

        if curl -sf "http://localhost:${port}/actuator/health" &> /dev/null; then
            check_result "$name" 0 "Service is healthy"

            # Check if registered in Nacos
            if [ "$VERBOSE" = true ]; then
                local service_name=$(echo "$name" | tr '[:upper:]' '[:lower:]' | sed 's/ /-/g')
                local instance_count=$(curl -sf "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=${service_name}" 2>/dev/null | grep -o '"hosts":\[[^]]*\]' | grep -o '"ip"' | wc -l)
                if [ -n "$instance_count" ] && [ "$instance_count" -gt 0 ]; then
                    log INFO "$name: $instance_count instance(s) registered"
                fi
            fi
        else
            check_result "$name" 1 "Service is not responding"
        fi
    done
}

# Network Connectivity Checks
check_network() {
    log INFO "Checking network connectivity..."

    # External connectivity
    if ping -c 1 8.8.8.8 &> /dev/null; then
        check_result "Internet Connectivity" 0 "Internet connection is working"
    else
        check_result "Internet Connectivity" 1 "No internet connection" "WARNING"
    fi

    # Internal connectivity
    if docker exec gcrf-gateway-service ping -c 1 gcrf-auth-service &> /dev/null; then
        check_result "Inter-Service Communication" 0 "Services can communicate"
    else
        check_result "Inter-Service Communication" 1 "Services cannot communicate"
    fi

    # DNS resolution
    if nslookup localhost &> /dev/null; then
        check_result "DNS Resolution" 0 "DNS is working"
    else
        check_result "DNS Resolution" 1 "DNS resolution failed" "WARNING"
    fi
}

# Monitoring Services Checks
check_monitoring() {
    log INFO "Checking monitoring services..."

    # Prometheus
    if curl -sf http://localhost:9090/-/healthy &> /dev/null; then
        check_result "Prometheus" 0 "Prometheus is healthy"
    else
        check_result "Prometheus" 1 "Prometheus is not healthy" "WARNING"
    fi

    # Grafana
    if curl -sf http://localhost:3000/api/health &> /dev/null; then
        check_result "Grafana" 0 "Grafana is healthy"
    else
        check_result "Grafana" 1 "Grafana is not healthy" "WARNING"
    fi
}

# Backup Status Check
check_backups() {
    log INFO "Checking backup status..."

    local latest_backup="/backups/postgresql/latest.sql.gz"

    if [ -f "$latest_backup" ]; then
        local backup_age=$(( ($(date +%s) - $(stat -f %m "$latest_backup" 2>/dev/null || stat -c %Y "$latest_backup")) / 3600 ))

        if [ "$backup_age" -lt 24 ]; then
            check_result "Database Backup" 0 "Latest backup is ${backup_age} hours old"
        elif [ "$backup_age" -lt 48 ]; then
            check_result "Database Backup" 1 "Latest backup is ${backup_age} hours old" "WARNING"
        else
            check_result "Database Backup" 1 "Latest backup is ${backup_age} hours old (too old)"
        fi
    else
        check_result "Database Backup" 1 "No backup file found"
    fi
}

# Generate summary
generate_summary() {
    local total_checks=$((CHECKS_PASSED + CHECKS_FAILED + CHECKS_WARNING))

    if [ "$JSON_OUTPUT" = true ]; then
        # JSON output
        echo "{"
        echo "  \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\","
        echo "  \"total_checks\": $total_checks,"
        echo "  \"passed\": $CHECKS_PASSED,"
        echo "  \"failed\": $CHECKS_FAILED,"
        echo "  \"warnings\": $CHECKS_WARNING,"
        echo "  \"status\": \"$([ $CHECKS_FAILED -eq 0 ] && echo "OK" || echo "CRITICAL")\""
        echo "}"
    else
        echo ""
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "Health Check Summary"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "Timestamp: $(date)"
        echo "Total Checks: $total_checks"
        echo -e "${GREEN}Passed: $CHECKS_PASSED${NC}"
        if [ $CHECKS_WARNING -gt 0 ]; then
            echo -e "${YELLOW}Warnings: $CHECKS_WARNING${NC}"
        fi
        if [ $CHECKS_FAILED -gt 0 ]; then
            echo -e "${RED}Failed: $CHECKS_FAILED${NC}"
        fi

        if [ $CHECKS_FAILED -gt 0 ]; then
            echo ""
            echo "Failed Checks:"
            for check in "${FAILED_CHECKS[@]}"; do
                echo -e "  ${RED}✗${NC} $check"
            done
        fi

        if [ $CHECKS_WARNING -gt 0 ]; then
            echo ""
            echo "Warnings:"
            for check in "${WARNING_CHECKS[@]}"; do
                echo -e "  ${YELLOW}!${NC} $check"
            done
        fi

        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

        if [ $CHECKS_FAILED -eq 0 ]; then
            echo -e "${GREEN}Overall Status: HEALTHY${NC}"
        elif [ $CHECKS_FAILED -gt 0 ]; then
            echo -e "${RED}Overall Status: CRITICAL${NC}"
        fi
    fi
}

# Parse arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --verbose)
                VERBOSE=true
                shift
                ;;
            --json)
                JSON_OUTPUT=true
                shift
                ;;
            --nagios)
                NAGIOS_MODE=true
                shift
                ;;
            --email)
                SEND_EMAIL=true
                shift
                ;;
            --help)
                cat <<EOF
GCRF Library Management System - Health Check Script

Usage: $0 [options]

Options:
  --verbose      Show detailed output
  --json         Output results in JSON format
  --nagios       Nagios-compatible exit codes and output
  --email        Send results via email
  --help         Show this help message

Exit Codes:
  0 - All checks passed
  1 - Warning: Some non-critical checks failed
  2 - Critical: Critical checks failed
EOF
                exit 0
                ;;
            *)
                echo "Unknown option: $1"
                exit 1
                ;;
        esac
    done
}

################################################################################
# Main Execution
################################################################################

main() {
    parse_args "$@"

    if [ "$JSON_OUTPUT" = false ]; then
        echo "╔═══════════════════════════════════════════════════════════════════╗"
        echo "║         GCRF Library Management System - Health Check             ║"
        echo "╚═══════════════════════════════════════════════════════════════════╝"
        echo ""
    fi

    # Run all checks
    check_system_resources
    check_docker
    check_infrastructure
    check_application_services
    check_network
    check_monitoring
    check_backups

    # Generate summary
    generate_summary

    # Send email if requested
    if [ "$SEND_EMAIL" = true ] && command -v mail &> /dev/null; then
        generate_summary | mail -s "GCRF Health Check Report" "$EMAIL_TO"
    fi

    # Determine exit code
    if [ $CHECKS_FAILED -gt 0 ]; then
        if [ "$NAGIOS_MODE" = true ]; then
            echo "CRITICAL - $CHECKS_FAILED checks failed"
            exit 2
        fi
        exit 2
    elif [ $CHECKS_WARNING -gt 0 ]; then
        if [ "$NAGIOS_MODE" = true ]; then
            echo "WARNING - $CHECKS_WARNING checks have warnings"
            exit 1
        fi
        exit 1
    else
        if [ "$NAGIOS_MODE" = true ]; then
            echo "OK - All checks passed"
        fi
        exit 0
    fi
}

main "$@"
