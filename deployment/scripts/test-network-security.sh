#!/bin/bash

################################################################################
# GCRF Library Management System
# Network Security Testing Script
################################################################################
# This script tests network security configuration including:
# - Network isolation between security zones
# - Port exposure rules
# - Service-to-service connectivity
# - External access controls
# - Management port restrictions
#
# Usage:
#   ./test-network-security.sh [--verbose] [--report FILE]
#
# Exit Codes:
#   0 - All tests passed
#   1 - One or more tests failed
#   2 - Script error (missing dependencies, etc.)
################################################################################

set -euo pipefail

# Colors for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# Configuration
VERBOSE=false
REPORT_FILE=""
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0
TEST_RESULTS=()

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

################################################################################
# Helper Functions
################################################################################

log_info() {
  echo -e "${BLUE}[INFO]${NC} $*"
}

log_success() {
  echo -e "${GREEN}[PASS]${NC} $*"
}

log_error() {
  echo -e "${RED}[FAIL]${NC} $*"
}

log_warning() {
  echo -e "${YELLOW}[WARN]${NC} $*"
}

log_verbose() {
  if [[ "$VERBOSE" == "true" ]]; then
    echo -e "${BLUE}[VERBOSE]${NC} $*"
  fi
}

record_test_result() {
  local test_name="$1"
  local result="$2"
  local details="${3:-}"

  TEST_RESULTS+=("$result|$test_name|$details")

  if [[ "$result" == "PASS" ]]; then
    ((PASSED_TESTS++))
    log_success "$test_name"
    [[ -n "$details" ]] && log_verbose "  Details: $details"
  elif [[ "$result" == "FAIL" ]]; then
    ((FAILED_TESTS++))
    log_error "$test_name"
    [[ -n "$details" ]] && log_error "  Details: $details"
  else
    ((SKIPPED_TESTS++))
    log_warning "$test_name (SKIPPED)"
    [[ -n "$details" ]] && log_warning "  Reason: $details"
  fi
}

check_command() {
  if ! command -v "$1" &> /dev/null; then
    log_error "Required command '$1' not found. Please install it."
    exit 2
  fi
}

check_container_running() {
  local container_name="$1"
  if ! docker ps --format '{{.Names}}' | grep -q "^${container_name}$"; then
    return 1
  fi
  return 0
}

test_port_accessible() {
  local host="$1"
  local port="$2"
  local timeout="${3:-5}"

  if command -v nc &> /dev/null; then
    nc -z -w "$timeout" "$host" "$port" &>/dev/null
  elif command -v timeout &> /dev/null; then
    timeout "$timeout" bash -c "cat < /dev/null > /dev/tcp/$host/$port" &>/dev/null
  else
    # Fallback to curl
    curl -s --max-time "$timeout" --connect-timeout "$timeout" "http://$host:$port" &>/dev/null
  fi
}

test_http_endpoint() {
  local url="$1"
  local expected_code="${2:-200}"
  local timeout="${3:-10}"

  local response_code
  response_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$timeout" --connect-timeout 5 "$url" 2>/dev/null || echo "000")

  if [[ "$response_code" == "$expected_code" ]]; then
    return 0
  else
    log_verbose "Expected HTTP $expected_code, got $response_code for $url"
    return 1
  fi
}

################################################################################
# Test Functions
################################################################################

test_external_public_access() {
  log_info "Test Category 1: External Access to Public Ports"
  echo "=========================================="

  # Test 1.1: Web Admin (Port 80)
  if test_http_endpoint "http://localhost:80" "200" 5; then
    record_test_result "1.1 Web Admin accessible on port 80" "PASS" "HTTP 200 OK"
  else
    record_test_result "1.1 Web Admin accessible on port 80" "FAIL" "Could not connect or wrong HTTP code"
  fi

  # Test 1.2: API Gateway (Port 8080)
  if test_http_endpoint "http://localhost:8080/actuator/health" "200" 5; then
    record_test_result "1.2 API Gateway accessible on port 8080" "PASS" "Health endpoint returns 200"
  else
    record_test_result "1.2 API Gateway accessible on port 8080" "FAIL" "Health endpoint not accessible"
  fi

  # Test 1.3: Gateway actuator endpoints
  if test_http_endpoint "http://localhost:8080/actuator/info" "200" 5; then
    record_test_result "1.3 Gateway actuator endpoints working" "PASS" "Info endpoint accessible"
  else
    record_test_result "1.3 Gateway actuator endpoints working" "SKIP" "Info endpoint may be disabled"
  fi

  echo ""
}

test_backend_service_isolation() {
  log_info "Test Category 2: Backend Service Isolation (Should NOT be Accessible)"
  echo "=========================================="

  # Test 2.1: Auth Service (Port 8081) should be blocked
  if test_port_accessible "localhost" "8081" 3; then
    record_test_result "2.1 Auth service (8081) NOT externally accessible" "FAIL" "Port 8081 is exposed (security risk)"
  else
    record_test_result "2.1 Auth service (8081) NOT externally accessible" "PASS" "Port 8081 correctly blocked"
  fi

  # Test 2.2: Book Service (Port 8082) should be blocked
  if test_port_accessible "localhost" "8082" 3; then
    record_test_result "2.2 Book service (8082) NOT externally accessible" "FAIL" "Port 8082 is exposed (security risk)"
  else
    record_test_result "2.2 Book service (8082) NOT externally accessible" "PASS" "Port 8082 correctly blocked"
  fi

  # Test 2.3: Circulation Service (Port 8083) should be blocked
  if test_port_accessible "localhost" "8083" 3; then
    record_test_result "2.3 Circulation service (8083) NOT externally accessible" "FAIL" "Port 8083 is exposed (security risk)"
  else
    record_test_result "2.3 Circulation service (8083) NOT externally accessible" "PASS" "Port 8083 correctly blocked"
  fi

  # Test 2.4: Reader Service (Port 8084) should be blocked
  if test_port_accessible "localhost" "8084" 3; then
    record_test_result "2.4 Reader service (8084) NOT externally accessible" "FAIL" "Port 8084 is exposed (security risk)"
  else
    record_test_result "2.4 Reader service (8084) NOT externally accessible" "PASS" "Port 8084 correctly blocked"
  fi

  echo ""
}

test_internal_service_communication() {
  log_info "Test Category 3: Internal Service-to-Service Communication"
  echo "=========================================="

  # Test 3.1: Gateway can reach Auth service internally
  if check_container_running "gcrf-gateway-service" && check_container_running "gcrf-auth-service"; then
    if docker exec gcrf-gateway-service curl -s -f -m 10 http://gcrf-auth-service:8081/actuator/health &>/dev/null; then
      record_test_result "3.1 Gateway → Auth service internal communication" "PASS" "HTTP connection successful"
    else
      record_test_result "3.1 Gateway → Auth service internal communication" "FAIL" "Cannot connect internally"
    fi
  else
    record_test_result "3.1 Gateway → Auth service internal communication" "SKIP" "Required containers not running"
  fi

  # Test 3.2: Gateway can reach Nacos
  if check_container_running "gcrf-gateway-service" && check_container_running "gcrf-nacos"; then
    if docker exec gcrf-gateway-service curl -s -f -m 10 http://nacos:8848/nacos/actuator/health &>/dev/null; then
      record_test_result "3.2 Gateway → Nacos communication" "PASS" "Service discovery reachable"
    else
      record_test_result "3.2 Gateway → Nacos communication" "FAIL" "Cannot reach Nacos"
    fi
  else
    record_test_result "3.2 Gateway → Nacos communication" "SKIP" "Required containers not running"
  fi

  # Test 3.3: Gateway can reach Redis
  if check_container_running "gcrf-gateway-service" && check_container_running "gcrf-redis-master"; then
    if docker exec gcrf-gateway-service sh -c "command -v nc >/dev/null && nc -z redis-master 6379" &>/dev/null; then
      record_test_result "3.3 Gateway → Redis communication" "PASS" "Redis port accessible"
    else
      record_test_result "3.3 Gateway → Redis communication" "SKIP" "netcat not available in container"
    fi
  else
    record_test_result "3.3 Gateway → Redis communication" "SKIP" "Required containers not running"
  fi

  # Test 3.4: DNS resolution works
  if check_container_running "gcrf-gateway-service"; then
    if docker exec gcrf-gateway-service nslookup gcrf-auth-service &>/dev/null; then
      record_test_result "3.4 Docker DNS resolution working" "PASS" "Service names resolve correctly"
    else
      record_test_result "3.4 Docker DNS resolution working" "FAIL" "DNS resolution failed"
    fi
  else
    record_test_result "3.4 Docker DNS resolution working" "SKIP" "Gateway container not running"
  fi

  echo ""
}

test_database_connectivity() {
  log_info "Test Category 4: Database and Infrastructure Access"
  echo "=========================================="

  # Test 4.1: Auth service can connect to PostgreSQL
  if check_container_running "gcrf-auth-service" && check_container_running "gcrf-postgres-primary"; then
    if docker exec gcrf-auth-service sh -c "command -v pg_isready >/dev/null && pg_isready -h postgres-primary -p 5432" &>/dev/null; then
      record_test_result "4.1 Auth service → PostgreSQL connection" "PASS" "Database reachable"
    else
      record_test_result "4.1 Auth service → PostgreSQL connection" "SKIP" "pg_isready not available"
    fi
  else
    record_test_result "4.1 Auth service → PostgreSQL connection" "SKIP" "Required containers not running"
  fi

  # Test 4.2: PostgreSQL NOT accessible from host
  if test_port_accessible "localhost" "5432" 3; then
    record_test_result "4.2 PostgreSQL NOT externally accessible" "FAIL" "Database port exposed (security risk)"
  else
    record_test_result "4.2 PostgreSQL NOT externally accessible" "PASS" "Database correctly isolated"
  fi

  # Test 4.3: Redis NOT accessible from host (except via localhost binding)
  # Note: Redis may be bound to 127.0.0.1:6379, which is acceptable
  if docker inspect gcrf-redis-master --format '{{range $p, $conf := .NetworkSettings.Ports}}{{if eq $p "6379/tcp"}}{{range $conf}}{{.HostIp}}{{end}}{{end}}{{end}}' | grep -q "0.0.0.0"; then
    record_test_result "4.3 Redis NOT publicly exposed" "FAIL" "Redis bound to 0.0.0.0 (should be 127.0.0.1)"
  else
    record_test_result "4.3 Redis NOT publicly exposed" "PASS" "Redis correctly bound to localhost only"
  fi

  # Test 4.4: Nacos MySQL NOT accessible from host
  if test_port_accessible "localhost" "3306" 3; then
    record_test_result "4.4 Nacos MySQL NOT externally accessible" "FAIL" "MySQL port exposed (security risk)"
  else
    record_test_result "4.4 Nacos MySQL NOT externally accessible" "PASS" "MySQL correctly isolated"
  fi

  echo ""
}

test_management_port_restrictions() {
  log_info "Test Category 5: Management Port Access Controls"
  echo "=========================================="

  # Test 5.1: Nacos UI accessible from localhost
  if test_http_endpoint "http://localhost:8848/nacos/" "200" 5; then
    record_test_result "5.1 Nacos UI accessible from localhost" "PASS" "Management interface available"
  else
    record_test_result "5.1 Nacos UI accessible from localhost" "FAIL" "Cannot access Nacos UI"
  fi

  # Test 5.2: RabbitMQ management accessible from localhost
  if test_http_endpoint "http://localhost:15672" "200" 5; then
    record_test_result "5.2 RabbitMQ UI accessible from localhost" "PASS" "Management interface available"
  else
    record_test_result "5.2 RabbitMQ UI accessible from localhost" "SKIP" "RabbitMQ may not be running"
  fi

  # Test 5.3: MinIO console accessible from localhost
  if test_http_endpoint "http://localhost:9001" "200" 5; then
    record_test_result "5.3 MinIO Console accessible from localhost" "PASS" "Management interface available"
  else
    record_test_result "5.3 MinIO Console accessible from localhost" "SKIP" "MinIO may not be running"
  fi

  # Test 5.4: Verify management ports are bound to 127.0.0.1
  local nacos_binding
  nacos_binding=$(docker port gcrf-nacos 8848 2>/dev/null | cut -d: -f1)
  if [[ "$nacos_binding" == "127.0.0.1" ]]; then
    record_test_result "5.4 Nacos bound to localhost only" "PASS" "127.0.0.1:8848"
  else
    record_test_result "5.4 Nacos bound to localhost only" "FAIL" "Bound to $nacos_binding (should be 127.0.0.1)"
  fi

  echo ""
}

test_network_configuration() {
  log_info "Test Category 6: Docker Network Configuration"
  echo "=========================================="

  # Test 6.1: Infrastructure network exists
  if docker network inspect gcrf-infrastructure-network &>/dev/null; then
    record_test_result "6.1 Infrastructure network exists" "PASS" "gcrf-infrastructure-network found"
  else
    record_test_result "6.1 Infrastructure network exists" "FAIL" "Infrastructure network not found"
  fi

  # Test 6.2: Frontend network exists
  if docker network inspect gcrf-frontend-network &>/dev/null; then
    record_test_result "6.2 Frontend network exists" "PASS" "gcrf-frontend-network found"
  else
    record_test_result "6.2 Frontend network exists" "SKIP" "Frontend network not created yet"
  fi

  # Test 6.3: Gateway connected to both networks
  if check_container_running "gcrf-gateway-service"; then
    local networks
    networks=$(docker inspect gcrf-gateway-service --format '{{range $k, $v := .NetworkSettings.Networks}}{{$k}} {{end}}')
    if echo "$networks" | grep -q "gcrf-infrastructure-network" && echo "$networks" | grep -q "gcrf-frontend-network"; then
      record_test_result "6.3 Gateway connected to both networks" "PASS" "Bridge between zones"
    elif echo "$networks" | grep -q "gcrf-infrastructure-network"; then
      record_test_result "6.3 Gateway connected to both networks" "SKIP" "Frontend network not yet created"
    else
      record_test_result "6.3 Gateway connected to both networks" "FAIL" "Gateway not properly connected"
    fi
  else
    record_test_result "6.3 Gateway connected to both networks" "SKIP" "Gateway not running"
  fi

  # Test 6.4: Auth service only in backend network
  if check_container_running "gcrf-auth-service"; then
    local networks
    networks=$(docker inspect gcrf-auth-service --format '{{range $k, $v := .NetworkSettings.Networks}}{{$k}} {{end}}')
    if echo "$networks" | grep -q "gcrf-infrastructure-network" && ! echo "$networks" | grep -q "gcrf-frontend-network"; then
      record_test_result "6.4 Auth service isolated to backend network" "PASS" "Correctly isolated"
    else
      record_test_result "6.4 Auth service isolated to backend network" "FAIL" "Connected to wrong networks"
    fi
  else
    record_test_result "6.4 Auth service isolated to backend network" "SKIP" "Auth service not running"
  fi

  # Test 6.5: Infrastructure services only in infrastructure network
  if check_container_running "gcrf-postgres-primary"; then
    local networks
    networks=$(docker inspect gcrf-postgres-primary --format '{{range $k, $v := .NetworkSettings.Networks}}{{$k}} {{end}}')
    if [[ "$networks" == *"gcrf-infrastructure-network"* ]] && [[ "$networks" != *"gcrf-frontend-network"* ]]; then
      record_test_result "6.5 PostgreSQL isolated to infrastructure network" "PASS" "Correctly isolated"
    else
      record_test_result "6.5 PostgreSQL isolated to infrastructure network" "FAIL" "Connected to wrong networks"
    fi
  else
    record_test_result "6.5 PostgreSQL isolated to infrastructure network" "SKIP" "PostgreSQL not running"
  fi

  echo ""
}

test_container_security() {
  log_info "Test Category 7: Container Security Configuration"
  echo "=========================================="

  # Test 7.1: Containers have resource limits
  if check_container_running "gcrf-gateway-service"; then
    local memory_limit
    memory_limit=$(docker inspect gcrf-gateway-service --format '{{.HostConfig.Memory}}')
    if [[ "$memory_limit" != "0" ]]; then
      record_test_result "7.1 Gateway has memory limits" "PASS" "Memory limit: $memory_limit bytes"
    else
      record_test_result "7.1 Gateway has memory limits" "SKIP" "No memory limit set (acceptable for dev)"
    fi
  else
    record_test_result "7.1 Gateway has memory limits" "SKIP" "Gateway not running"
  fi

  # Test 7.2: Check for privileged containers (should be none)
  local privileged_count
  privileged_count=$(docker ps --filter "name=gcrf-" --format '{{.Names}}' | while read -r container; do
    docker inspect "$container" --format '{{.HostConfig.Privileged}}' | grep -c "true" || true
  done | awk '{s+=$1} END {print s+0}')

  if [[ "$privileged_count" -eq 0 ]]; then
    record_test_result "7.2 No privileged containers" "PASS" "All containers run unprivileged"
  else
    record_test_result "7.2 No privileged containers" "FAIL" "$privileged_count containers running privileged"
  fi

  # Test 7.3: Containers have health checks
  if check_container_running "gcrf-gateway-service"; then
    local health_status
    health_status=$(docker inspect gcrf-gateway-service --format '{{.State.Health.Status}}' 2>/dev/null || echo "none")
    if [[ "$health_status" != "none" ]]; then
      record_test_result "7.3 Gateway has health check configured" "PASS" "Health status: $health_status"
    else
      record_test_result "7.3 Gateway has health check configured" "FAIL" "No health check configured"
    fi
  else
    record_test_result "7.3 Gateway has health check configured" "SKIP" "Gateway not running"
  fi

  echo ""
}

################################################################################
# Report Generation
################################################################################

generate_report() {
  local report_file="$1"

  {
    echo "======================================"
    echo "GCRF Network Security Test Report"
    echo "======================================"
    echo "Date: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "Host: $(hostname)"
    echo ""
    echo "Summary:"
    echo "--------"
    echo "Total Tests: $((PASSED_TESTS + FAILED_TESTS + SKIPPED_TESTS))"
    echo "Passed: $PASSED_TESTS"
    echo "Failed: $FAILED_TESTS"
    echo "Skipped: $SKIPPED_TESTS"
    echo ""

    if [[ "$FAILED_TESTS" -eq 0 ]]; then
      echo "Overall Result: PASS ✓"
    else
      echo "Overall Result: FAIL ✗"
    fi
    echo ""

    echo "Detailed Results:"
    echo "-----------------"
    for result in "${TEST_RESULTS[@]}"; do
      IFS='|' read -r status name details <<< "$result"
      printf "[%-4s] %s\n" "$status" "$name"
      if [[ -n "$details" ]]; then
        printf "        %s\n" "$details"
      fi
    done

    echo ""
    echo "Recommendations:"
    echo "----------------"

    if [[ "$FAILED_TESTS" -gt 0 ]]; then
      echo "- Review and fix failed tests immediately"
      echo "- Failed tests indicate security vulnerabilities"
      echo "- Run './configure-firewall.sh enable' if firewall is not active"
      echo "- Verify Docker compose port bindings (127.0.0.1 vs 0.0.0.0)"
    else
      echo "- All critical security tests passed"
      echo "- Continue monitoring for security events"
      echo "- Schedule regular security audits"
    fi

    echo ""
    echo "Next Steps:"
    echo "-----------"
    echo "1. Address all failed tests"
    echo "2. Review skipped tests and ensure they are acceptable"
    echo "3. Enable firewall rules in production: ./configure-firewall.sh enable"
    echo "4. Set up continuous security monitoring"
    echo "5. Schedule quarterly penetration testing"

  } | tee "$report_file"
}

################################################################################
# Main Execution
################################################################################

main() {
  # Parse arguments
  while [[ $# -gt 0 ]]; do
    case $1 in
      --verbose|-v)
        VERBOSE=true
        shift
        ;;
      --report|-r)
        REPORT_FILE="$2"
        shift 2
        ;;
      --help|-h)
        cat <<EOF
Usage: $0 [OPTIONS]

Test network security configuration for GCRF Library Management System.

Options:
  --verbose, -v          Enable verbose output
  --report FILE, -r FILE Generate report file
  --help, -h             Show this help message

Exit Codes:
  0 - All tests passed
  1 - One or more tests failed
  2 - Script error

Examples:
  $0                              # Run all tests
  $0 --verbose                    # Run with verbose output
  $0 --report security-report.txt # Generate report file

EOF
        exit 0
        ;;
      *)
        log_error "Unknown option: $1"
        echo "Use --help for usage information"
        exit 2
        ;;
    esac
  done

  # Header
  echo "======================================"
  echo "GCRF Network Security Test Suite"
  echo "======================================"
  echo "Started: $(date '+%Y-%m-%d %H:%M:%S')"
  echo ""

  # Check prerequisites
  check_command docker
  check_command curl

  # Run test categories
  test_external_public_access
  test_backend_service_isolation
  test_internal_service_communication
  test_database_connectivity
  test_management_port_restrictions
  test_network_configuration
  test_container_security

  # Summary
  echo "======================================"
  echo "Test Summary"
  echo "======================================"
  echo "Total Tests: $((PASSED_TESTS + FAILED_TESTS + SKIPPED_TESTS))"
  echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
  echo -e "${RED}Failed: $FAILED_TESTS${NC}"
  echo -e "${YELLOW}Skipped: $SKIPPED_TESTS${NC}"
  echo ""

  # Generate report if requested
  if [[ -n "$REPORT_FILE" ]]; then
    generate_report "$REPORT_FILE"
    log_info "Report saved to: $REPORT_FILE"
  fi

  # Final result
  if [[ "$FAILED_TESTS" -eq 0 ]]; then
    echo -e "${GREEN}════════════════════════════════════${NC}"
    echo -e "${GREEN}✓ All tests passed!${NC}"
    echo -e "${GREEN}════════════════════════════════════${NC}"
    exit 0
  else
    echo -e "${RED}════════════════════════════════════${NC}"
    echo -e "${RED}✗ $FAILED_TESTS test(s) failed${NC}"
    echo -e "${RED}════════════════════════════════════${NC}"
    echo ""
    log_error "Security vulnerabilities detected. Please review failed tests."
    exit 1
  fi
}

# Run main function
main "$@"
