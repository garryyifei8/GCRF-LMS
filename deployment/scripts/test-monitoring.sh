#!/bin/bash
# GCRF Library Management System - Test Monitoring Stack
# Version: 1.0.0
# Last Updated: 2025-11-01
#
# Purpose: Comprehensive validation of the monitoring system
# Usage: ./test-monitoring.sh

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DEPLOYMENT_DIR="$PROJECT_ROOT/deployment"

# Helper functions
pass() {
    ((TESTS_PASSED++))
    ((TESTS_TOTAL++))
    echo -e "${GREEN}✓ $1${NC}"
}

fail() {
    ((TESTS_FAILED++))
    ((TESTS_TOTAL++))
    echo -e "${RED}✗ $1${NC}"
}

info() {
    echo -e "${BLUE}→ $1${NC}"
}

warn() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Test function
test_endpoint() {
    local url=$1
    local description=$2
    local expected_code=${3:-200}

    if curl -s -o /dev/null -w "%{http_code}" "$url" | grep -q "^$expected_code$"; then
        pass "$description"
        return 0
    else
        fail "$description"
        return 1
    fi
}

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}   GCRF Library - Monitoring Stack Validation${NC}"
echo -e "${BLUE}======================================================${NC}"
echo

# ========================================
# Phase 1: Container Health Check
# ========================================
echo -e "${YELLOW}Phase 1: Checking Docker Containers${NC}"
echo

services=("prometheus" "grafana" "node-exporter" "postgres-exporter" "redis-exporter")

for service in "${services[@]}"; do
    container_name="gcrf-${service}"
    if docker ps --format '{{.Names}}' | grep -q "^${container_name}$"; then
        if [[ "$(docker inspect -f '{{.State.Health.Status}}' "$container_name" 2>/dev/null)" == "healthy" ]] || \
           [[ -z "$(docker inspect -f '{{.State.Health.Status}}' "$container_name" 2>/dev/null)" ]]; then
            pass "Container $service is running"
        else
            fail "Container $service is unhealthy"
        fi
    else
        fail "Container $service is not running"
    fi
done

echo

# ========================================
# Phase 2: Prometheus Validation
# ========================================
echo -e "${YELLOW}Phase 2: Validating Prometheus${NC}"
echo

# Test Prometheus API
test_endpoint "http://localhost:9090/-/healthy" "Prometheus health endpoint"
test_endpoint "http://localhost:9090/api/v1/status/config" "Prometheus config API"

# Test Prometheus targets
info "Checking Prometheus targets..."
TARGETS_JSON=$(curl -s "http://localhost:9090/api/v1/targets")

# Check each expected target
targets=(
    "gateway-service"
    "auth-service"
    "book-service"
    "circulation-service"
    "reader-service"
    "node-exporter"
    "postgres-exporter"
    "redis-exporter"
)

for target in "${targets[@]}"; do
    if echo "$TARGETS_JSON" | grep -q "\"job\":\"$target\""; then
        # Check if target is UP
        if echo "$TARGETS_JSON" | grep "\"job\":\"$target\"" | grep -q "\"health\":\"up\""; then
            pass "Prometheus target '$target' is UP"
        else
            fail "Prometheus target '$target' is DOWN"
        fi
    else
        fail "Prometheus target '$target' not found"
    fi
done

# Test alert rules
info "Checking Prometheus alert rules..."
RULES_JSON=$(curl -s "http://localhost:9090/api/v1/rules")

if echo "$RULES_JSON" | grep -q "\"type\":\"alerting\""; then
    RULES_COUNT=$(echo "$RULES_JSON" | grep -o "\"type\":\"alerting\"" | wc -l)
    pass "Prometheus alert rules loaded ($RULES_COUNT rules)"
else
    fail "No alert rules found"
fi

echo

# ========================================
# Phase 3: Grafana Validation
# ========================================
echo -e "${YELLOW}Phase 3: Validating Grafana${NC}"
echo

# Test Grafana API
test_endpoint "http://localhost:3000/api/health" "Grafana health endpoint"

# Test Grafana datasources
info "Checking Grafana datasources..."
DATASOURCES_JSON=$(curl -s -u admin:admin "http://localhost:3000/api/datasources")

if echo "$DATASOURCES_JSON" | grep -q "\"type\":\"prometheus\""; then
    pass "Grafana Prometheus datasource configured"
else
    fail "Grafana Prometheus datasource not found"
fi

echo

# ========================================
# Phase 4: Exporters Validation
# ========================================
echo -e "${YELLOW}Phase 4: Validating Exporters${NC}"
echo

# Node Exporter
test_endpoint "http://localhost:9100/metrics" "Node Exporter metrics endpoint"

# PostgreSQL Exporter
test_endpoint "http://localhost:9187/metrics" "PostgreSQL Exporter metrics endpoint"

# Redis Exporter
test_endpoint "http://localhost:9121/metrics" "Redis Exporter metrics endpoint"

echo

# ========================================
# Phase 5: Services Actuator Endpoints
# ========================================
echo -e "${YELLOW}Phase 5: Validating Service Actuator Endpoints${NC}"
echo

services_actuator=(
    "8080:gateway-service"
    "8081:auth-service"
    "8082:book-service"
    "8083:circulation-service"
    "8084:reader-service"
)

for service_config in "${services_actuator[@]}"; do
    IFS=':' read -r port service_name <<< "$service_config"

    # Check if service is running
    if curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/actuator/health" | grep -q "^200$"; then
        pass "$service_name Actuator health endpoint (:$port)"

        # Check Prometheus endpoint
        if curl -s "http://localhost:$port/actuator/prometheus" | grep -q "jvm_memory_used_bytes"; then
            pass "$service_name Prometheus metrics available"
        else
            warn "$service_name Prometheus metrics may be incomplete"
        fi
    else
        warn "$service_name not running (:$port) - skipping"
    fi
done

echo

# ========================================
# Phase 6: Metrics Data Validation
# ========================================
echo -e "${YELLOW}Phase 6: Validating Metrics Data${NC}"
echo

# Query Prometheus for key metrics
info "Checking if metrics are being collected..."

metrics_to_check=(
    "up"
    "jvm_memory_used_bytes"
    "http_server_requests_seconds_count"
    "hikaricp_connections_active"
    "node_cpu_seconds_total"
    "pg_stat_database_numbackends"
    "redis_memory_used_bytes"
)

for metric in "${metrics_to_check}"; do
    QUERY_RESULT=$(curl -s "http://localhost:9090/api/v1/query?query=$metric" | grep -o "\"status\":\"success\"")

    if [[ -n "$QUERY_RESULT" ]]; then
        pass "Metric '$metric' is available"
    else
        fail "Metric '$metric' is not available"
    fi
done

echo

# ========================================
# Phase 7: Alert Rules Validation
# ========================================
echo -e "${YELLOW}Phase 7: Validating Alert Rules Syntax${NC}"
echo

# Check infrastructure alerts
if docker exec gcrf-prometheus promtool check rules /etc/prometheus/alerts/infrastructure-alerts.yml >/dev/null 2>&1; then
    pass "infrastructure-alerts.yml syntax valid"
else
    fail "infrastructure-alerts.yml has syntax errors"
fi

# Check service alerts
if docker exec gcrf-prometheus promtool check rules /etc/prometheus/alerts/service-alerts.yml >/dev/null 2>&1; then
    pass "service-alerts.yml syntax valid"
else
    fail "service-alerts.yml has syntax errors"
fi

echo

# ========================================
# Phase 8: Data Persistence Validation
# ========================================
echo -e "${YELLOW}Phase 8: Validating Data Persistence${NC}"
echo

# Check Prometheus data directory
PROM_DATA_SIZE=$(docker exec gcrf-prometheus du -sh /prometheus 2>/dev/null | awk '{print $1}')
if [[ -n "$PROM_DATA_SIZE" ]]; then
    pass "Prometheus data directory exists (Size: $PROM_DATA_SIZE)"
else
    fail "Prometheus data directory not found"
fi

# Check Grafana data directory
GRAFANA_DATA_SIZE=$(docker exec gcrf-grafana du -sh /var/lib/grafana 2>/dev/null | awk '{print $1}')
if [[ -n "$GRAFANA_DATA_SIZE" ]]; then
    pass "Grafana data directory exists (Size: $GRAFANA_DATA_SIZE)"
else
    fail "Grafana data directory not found"
fi

echo

# ========================================
# Summary
# ========================================
echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}   Test Summary${NC}"
echo -e "${BLUE}======================================================${NC}"
echo
echo -e "Total Tests:  ${TESTS_TOTAL}"
echo -e "${GREEN}Passed:       ${TESTS_PASSED}${NC}"

if [[ $TESTS_FAILED -gt 0 ]]; then
    echo -e "${RED}Failed:       ${TESTS_FAILED}${NC}"
else
    echo -e "Failed:       ${TESTS_FAILED}"
fi

echo
if [[ $TESTS_FAILED -eq 0 ]]; then
    echo -e "${GREEN}✓ All tests passed! Monitoring stack is healthy.${NC}"
    EXIT_CODE=0
else
    echo -e "${RED}✗ Some tests failed. Please review the output above.${NC}"
    EXIT_CODE=1
fi

echo
echo -e "${YELLOW}Next Steps:${NC}"
echo -e "  1. Access Prometheus: ${BLUE}http://localhost:9090${NC}"
echo -e "  2. Access Grafana:    ${BLUE}http://localhost:3000${NC} (admin/admin)"
echo -e "  3. Import dashboards: See deployment/monitoring/GRAFANA_QUICKSTART.md"
echo

exit $EXIT_CODE
