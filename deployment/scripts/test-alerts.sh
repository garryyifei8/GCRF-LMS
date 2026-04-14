#!/bin/bash
# GCRF Library Management System - Alert Testing Script
# Version: 1.0.0
# Last Updated: 2025-12-01
#
# This script helps test the AlertManager configuration by:
# 1. Sending test alerts directly to AlertManager
# 2. Validating alert rules syntax
# 3. Testing notification channels
#
# Usage: ./test-alerts.sh [command]
# Commands:
#   send-test         - Send test alerts to AlertManager
#   validate-rules    - Validate Prometheus alert rules syntax
#   check-config      - Check AlertManager configuration
#   list-alerts       - List current alerts in AlertManager
#   silence-create    - Create a test silence
#   silence-list      - List active silences
#   help              - Show this help message

set -e

# Configuration
ALERTMANAGER_URL="${ALERTMANAGER_URL:-http://localhost:9093}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:9090}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ALERTMANAGER_DIR="${PROJECT_ROOT}/deployment/alertmanager"
PROMETHEUS_DIR="${PROJECT_ROOT}/deployment/monitoring/prometheus"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if AlertManager is reachable
check_alertmanager() {
    print_info "Checking AlertManager connectivity at ${ALERTMANAGER_URL}..."
    if curl -s "${ALERTMANAGER_URL}/-/healthy" > /dev/null 2>&1; then
        print_success "AlertManager is healthy"
        return 0
    else
        print_error "AlertManager is not reachable at ${ALERTMANAGER_URL}"
        print_info "Make sure AlertManager is running: docker-compose -f docker-compose.monitoring.yml up -d alertmanager"
        return 1
    fi
}

# Check if Prometheus is reachable
check_prometheus() {
    print_info "Checking Prometheus connectivity at ${PROMETHEUS_URL}..."
    if curl -s "${PROMETHEUS_URL}/-/healthy" > /dev/null 2>&1; then
        print_success "Prometheus is healthy"
        return 0
    else
        print_error "Prometheus is not reachable at ${PROMETHEUS_URL}"
        return 1
    fi
}

# Send test alert to AlertManager
send_test_alert() {
    local severity="${1:-warning}"
    local alertname="${2:-TestAlert}"
    local service="${3:-test-service}"

    print_info "Sending test alert: ${alertname} (severity: ${severity})"

    local start_time=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    local labels_json=$(cat <<EOF
{
    "alertname": "${alertname}",
    "severity": "${severity}",
    "service": "${service}",
    "category": "test",
    "team": "platform",
    "environment": "test",
    "cluster": "gcrf-library",
    "instance": "localhost:8080"
}
EOF
)
    local annotations_json=$(cat <<EOF
{
    "summary": "Test alert: ${alertname}",
    "description": "This is a test alert sent from test-alerts.sh script.\nSeverity: ${severity}\nService: ${service}\nTime: ${start_time}",
    "runbook_url": "https://docs.gcrf-library.com/runbooks/test-alert"
}
EOF
)

    local alert_json=$(cat <<EOF
[{
    "labels": ${labels_json},
    "annotations": ${annotations_json},
    "startsAt": "${start_time}",
    "generatorURL": "${PROMETHEUS_URL}/graph"
}]
EOF
)

    local response=$(curl -s -w "\n%{http_code}" -X POST \
        "${ALERTMANAGER_URL}/api/v2/alerts" \
        -H "Content-Type: application/json" \
        -d "${alert_json}")

    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n-1)

    if [ "$http_code" == "200" ]; then
        print_success "Test alert sent successfully!"
        print_info "Check AlertManager UI at ${ALERTMANAGER_URL}/#/alerts"
    else
        print_error "Failed to send test alert (HTTP ${http_code})"
        echo "Response: ${body}"
        return 1
    fi
}

# Send batch of test alerts
send_batch_alerts() {
    print_info "Sending batch of test alerts..."
    echo ""

    # Critical alert
    send_test_alert "critical" "TestServiceDown" "gateway-service"
    sleep 1

    # Warning alert
    send_test_alert "warning" "TestHighCPU" "book-service"
    sleep 1

    # Info alert
    send_test_alert "info" "TestHighTraffic" "reader-service"
    sleep 1

    # Business alert
    send_test_alert "warning" "TestBorrowFailure" "circulation-service"

    echo ""
    print_success "Batch alerts sent! Check AlertManager at ${ALERTMANAGER_URL}"
}

# Resolve a test alert
resolve_test_alert() {
    local alertname="${1:-TestAlert}"

    print_info "Resolving test alert: ${alertname}"

    local start_time=$(date -u -d '5 minutes ago' +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || date -u -v-5M +"%Y-%m-%dT%H:%M:%SZ")
    local end_time=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

    local alert_json=$(cat <<EOF
[{
    "labels": {
        "alertname": "${alertname}",
        "severity": "warning",
        "service": "test-service",
        "environment": "test",
        "cluster": "gcrf-library"
    },
    "annotations": {
        "summary": "Test alert resolved"
    },
    "startsAt": "${start_time}",
    "endsAt": "${end_time}"
}]
EOF
)

    local response=$(curl -s -w "\n%{http_code}" -X POST \
        "${ALERTMANAGER_URL}/api/v2/alerts" \
        -H "Content-Type: application/json" \
        -d "${alert_json}")

    local http_code=$(echo "$response" | tail -n1)

    if [ "$http_code" == "200" ]; then
        print_success "Test alert resolved!"
    else
        print_error "Failed to resolve alert (HTTP ${http_code})"
        return 1
    fi
}

# Validate Prometheus alert rules
validate_rules() {
    print_info "Validating alert rules..."
    echo ""

    # Check if promtool is available
    if ! command -v promtool &> /dev/null; then
        print_warning "promtool not found locally, using Docker..."

        # Validate using Prometheus Docker image
        for rules_file in "${PROMETHEUS_DIR}/rules/"*.yml "${PROMETHEUS_DIR}/alerts/"*.yml; do
            if [ -f "$rules_file" ]; then
                print_info "Validating: $(basename "$rules_file")"
                if docker run --rm -v "${rules_file}:/rules.yml:ro" prom/prometheus:v2.48.0 promtool check rules /rules.yml; then
                    print_success "  Valid"
                else
                    print_error "  Invalid"
                fi
            fi
        done
    else
        # Use local promtool
        for rules_file in "${PROMETHEUS_DIR}/rules/"*.yml "${PROMETHEUS_DIR}/alerts/"*.yml; do
            if [ -f "$rules_file" ]; then
                print_info "Validating: $(basename "$rules_file")"
                if promtool check rules "$rules_file"; then
                    print_success "  Valid"
                else
                    print_error "  Invalid"
                fi
            fi
        done
    fi
}

# Check AlertManager configuration
check_config() {
    print_info "Checking AlertManager configuration..."
    echo ""

    local config_file="${ALERTMANAGER_DIR}/alertmanager.yml"

    if [ ! -f "$config_file" ]; then
        print_error "AlertManager config not found at: $config_file"
        return 1
    fi

    # Check if amtool is available
    if ! command -v amtool &> /dev/null; then
        print_warning "amtool not found locally, using Docker..."

        if docker run --rm -v "${config_file}:/etc/alertmanager/alertmanager.yml:ro" \
            prom/alertmanager:v0.26.0 amtool check-config /etc/alertmanager/alertmanager.yml; then
            print_success "AlertManager configuration is valid"
        else
            print_error "AlertManager configuration has errors"
            return 1
        fi
    else
        if amtool check-config "$config_file"; then
            print_success "AlertManager configuration is valid"
        else
            print_error "AlertManager configuration has errors"
            return 1
        fi
    fi
}

# List current alerts
list_alerts() {
    print_info "Fetching current alerts from AlertManager..."
    echo ""

    local response=$(curl -s "${ALERTMANAGER_URL}/api/v2/alerts")

    if [ -z "$response" ] || [ "$response" == "[]" ]; then
        print_info "No active alerts"
        return 0
    fi

    echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response" | jq . 2>/dev/null || echo "$response"
}

# List alert groups
list_alert_groups() {
    print_info "Fetching alert groups from AlertManager..."
    echo ""

    local response=$(curl -s "${ALERTMANAGER_URL}/api/v2/alerts/groups")
    echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response" | jq . 2>/dev/null || echo "$response"
}

# Create a test silence
create_silence() {
    local alertname="${1:-TestAlert}"
    local duration="${2:-1h}"

    print_info "Creating silence for ${alertname} (duration: ${duration})"

    local start_time=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    local end_time=$(date -u -d "+${duration}" +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || date -u -v+1H +"%Y-%m-%dT%H:%M:%SZ")

    local silence_json=$(cat <<EOF
{
    "matchers": [
        {
            "name": "alertname",
            "value": "${alertname}",
            "isRegex": false,
            "isEqual": true
        }
    ],
    "startsAt": "${start_time}",
    "endsAt": "${end_time}",
    "createdBy": "test-alerts.sh",
    "comment": "Test silence created by test script"
}
EOF
)

    local response=$(curl -s -w "\n%{http_code}" -X POST \
        "${ALERTMANAGER_URL}/api/v2/silences" \
        -H "Content-Type: application/json" \
        -d "${silence_json}")

    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n-1)

    if [ "$http_code" == "200" ]; then
        local silence_id=$(echo "$body" | python3 -c "import sys,json; print(json.load(sys.stdin)['silenceID'])" 2>/dev/null || echo "$body")
        print_success "Silence created: ${silence_id}"
    else
        print_error "Failed to create silence (HTTP ${http_code})"
        echo "Response: ${body}"
        return 1
    fi
}

# List active silences
list_silences() {
    print_info "Fetching active silences from AlertManager..."
    echo ""

    local response=$(curl -s "${ALERTMANAGER_URL}/api/v2/silences?filter=active")

    if [ -z "$response" ] || [ "$response" == "[]" ]; then
        print_info "No active silences"
        return 0
    fi

    echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response" | jq . 2>/dev/null || echo "$response"
}

# Get AlertManager status
get_status() {
    print_info "Fetching AlertManager status..."
    echo ""

    local response=$(curl -s "${ALERTMANAGER_URL}/api/v2/status")
    echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response" | jq . 2>/dev/null || echo "$response"
}

# Get receivers configuration
get_receivers() {
    print_info "Fetching configured receivers..."
    echo ""

    local response=$(curl -s "${ALERTMANAGER_URL}/api/v2/receivers")
    echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response" | jq . 2>/dev/null || echo "$response"
}

# Check Prometheus alert rules status
check_prometheus_rules() {
    if ! check_prometheus; then
        return 1
    fi

    print_info "Fetching alert rules from Prometheus..."
    echo ""

    local response=$(curl -s "${PROMETHEUS_URL}/api/v1/rules?type=alert")
    echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response" | jq . 2>/dev/null || echo "$response"
}

# Check firing alerts in Prometheus
check_prometheus_alerts() {
    if ! check_prometheus; then
        return 1
    fi

    print_info "Fetching firing alerts from Prometheus..."
    echo ""

    local response=$(curl -s "${PROMETHEUS_URL}/api/v1/alerts")
    echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response" | jq . 2>/dev/null || echo "$response"
}

# Test webhook endpoint
test_webhook() {
    local webhook_url="${1:-http://localhost:8086/api/v1/alerts/webhook}"

    print_info "Testing webhook endpoint: ${webhook_url}"

    local test_payload=$(cat <<EOF
{
    "version": "4",
    "status": "firing",
    "alerts": [{
        "status": "firing",
        "labels": {
            "alertname": "WebhookTest",
            "severity": "info"
        },
        "annotations": {
            "summary": "Webhook test"
        },
        "startsAt": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
    }]
}
EOF
)

    local response=$(curl -s -w "\n%{http_code}" -X POST \
        "${webhook_url}" \
        -H "Content-Type: application/json" \
        -d "${test_payload}")

    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n-1)

    if [ "$http_code" == "200" ] || [ "$http_code" == "204" ]; then
        print_success "Webhook test successful!"
    else
        print_warning "Webhook returned HTTP ${http_code}"
        echo "Response: ${body}"
    fi
}

# Show help
show_help() {
    cat << EOF
GCRF Library Alert Testing Script
==================================

Usage: $0 [command] [options]

Commands:
  send-test [severity] [name] [service]
                        Send a single test alert
                        severity: critical, warning, info (default: warning)
                        name: alert name (default: TestAlert)
                        service: service name (default: test-service)

  send-batch            Send batch of different test alerts

  resolve [alertname]   Resolve a test alert

  validate-rules        Validate Prometheus alert rules syntax

  check-config          Check AlertManager configuration

  list-alerts           List current alerts in AlertManager

  list-groups           List alert groups

  prometheus-rules      Show Prometheus alert rules status

  prometheus-alerts     Show firing alerts in Prometheus

  silence-create [name] [duration]
                        Create a silence for an alert
                        duration format: 1h, 30m, etc.

  silence-list          List active silences

  status                Get AlertManager status

  receivers             Get configured receivers

  test-webhook [url]    Test a webhook endpoint

  help                  Show this help message

Environment Variables:
  ALERTMANAGER_URL      AlertManager URL (default: http://localhost:9093)
  PROMETHEUS_URL        Prometheus URL (default: http://localhost:9090)

Examples:
  $0 send-test critical ServiceDown gateway-service
  $0 send-batch
  $0 validate-rules
  $0 silence-create TestAlert 2h
  $0 test-webhook http://localhost:8086/api/v1/alerts/webhook

EOF
}

# Main command handler
main() {
    local command="${1:-help}"

    case "$command" in
        send-test)
            check_alertmanager || exit 1
            send_test_alert "${2:-warning}" "${3:-TestAlert}" "${4:-test-service}"
            ;;
        send-batch)
            check_alertmanager || exit 1
            send_batch_alerts
            ;;
        resolve)
            check_alertmanager || exit 1
            resolve_test_alert "${2:-TestAlert}"
            ;;
        validate-rules|validate)
            validate_rules
            ;;
        check-config|check)
            check_config
            ;;
        list-alerts|list|alerts)
            check_alertmanager || exit 1
            list_alerts
            ;;
        list-groups|groups)
            check_alertmanager || exit 1
            list_alert_groups
            ;;
        prometheus-rules|prom-rules)
            check_prometheus_rules
            ;;
        prometheus-alerts|prom-alerts)
            check_prometheus_alerts
            ;;
        silence-create|silence)
            check_alertmanager || exit 1
            create_silence "${2:-TestAlert}" "${3:-1h}"
            ;;
        silence-list|silences)
            check_alertmanager || exit 1
            list_silences
            ;;
        status)
            check_alertmanager || exit 1
            get_status
            ;;
        receivers)
            check_alertmanager || exit 1
            get_receivers
            ;;
        test-webhook|webhook)
            test_webhook "${2:-http://localhost:8086/api/v1/alerts/webhook}"
            ;;
        health)
            check_alertmanager
            check_prometheus
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "Unknown command: $command"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

# Run main
main "$@"
