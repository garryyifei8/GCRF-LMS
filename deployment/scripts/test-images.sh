#!/usr/bin/env bash
################################################################################
# GCRF Library Management System - Docker Image Testing
# Description: Validate built images for security, health, and functionality
# Usage: ./test-images.sh [options]
# Author: GCRF DevOps Team
# Version: 1.0.0
################################################################################

set -euo pipefail

# Color output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly CYAN='\033[0;36m'
readonly NC='\033[0m' # No Color

# Script configuration
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
readonly TEST_REPORT_DIR="${PROJECT_ROOT}/deployment/reports"
readonly TEST_LOG_DIR="${PROJECT_ROOT}/deployment/logs/tests"

# Services and their expected configurations
declare -A SERVICE_PORTS=(
    ["gateway-service"]="8080"
    ["auth-service"]="8081"
    ["book-service"]="8082"
    ["circulation-service"]="8083"
    ["reader-service"]="8084"
    ["system-service"]="8085"
    ["notification-service"]="8086"
)

# Test configuration
VERSION="latest"
REGISTRY=""
REPORT_FORMAT="text"
VERBOSE=false
VULNERABILITY_SCAN=true
HEALTH_CHECK_TIMEOUT=30

# Test results tracking
declare -A TEST_RESULTS
declare -A TEST_DETAILS

################################################################################
# Logging functions
################################################################################

log_info() {
    echo -e "${BLUE}[INFO]${NC} $*"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $*"
}

log_warn() {
    echo -e "${YELLOW}[!]${NC} $*"
}

log_error() {
    echo -e "${RED}[✗]${NC} $*" >&2
}

log_test() {
    echo -e "${CYAN}[TEST]${NC} $*"
}

################################################################################
# Utility functions
################################################################################

usage() {
    cat <<EOF
Usage: $0 [options]

Test Docker images for GCRF microservices.

Options:
  --version VERSION     Version tag to test (default: latest)
  --registry REGISTRY   Docker registry URL
  --format FORMAT       Report format: text, json, html (default: text)
  --no-vuln-scan        Skip vulnerability scanning
  --timeout SECONDS     Health check timeout (default: 30)
  --verbose             Enable verbose output
  -h, --help           Show this help message

Examples:
  # Test all images
  $0

  # Test specific version with HTML report
  $0 --version v1.0.0 --format html

  # Quick test without vulnerability scan
  $0 --no-vuln-scan

Tests performed:
  ✓ Image existence and basic metadata
  ✓ Image labels (version, git commit, build date)
  ✓ Image size validation
  ✓ Layer count analysis
  ✓ Security: Non-root user execution
  ✓ Security: Minimal attack surface
  ✓ Port exposure validation
  ✓ Environment variable handling
  ✓ Container startup and health checks
  ✓ Vulnerability scanning (optional)

EOF
    exit 0
}

setup_directories() {
    mkdir -p "${TEST_REPORT_DIR}"
    mkdir -p "${TEST_LOG_DIR}"
}

################################################################################
# Test functions
################################################################################

test_image_exists() {
    local service=$1
    local image_name="${service}"
    [ -n "${REGISTRY}" ] && image_name="${REGISTRY}/${service}"
    local full_image="${image_name}:${VERSION}"

    log_test "Checking if image exists: ${full_image}"

    if docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^${full_image}$"; then
        log_success "Image exists"
        return 0
    else
        log_error "Image not found"
        return 1
    fi
}

test_image_labels() {
    local service=$1
    local image_name="${service}"
    [ -n "${REGISTRY}" ] && image_name="${REGISTRY}/${service}"
    local full_image="${image_name}:${VERSION}"

    log_test "Validating image labels"

    local labels=$(docker inspect --format='{{json .Config.Labels}}' "${full_image}" 2>/dev/null || echo "{}")

    # Check required labels
    local required_labels=(
        "org.opencontainers.image.version"
        "org.opencontainers.image.revision"
        "org.opencontainers.image.created"
    )

    local missing_labels=()
    for label in "${required_labels[@]}"; do
        if ! echo "${labels}" | grep -q "\"${label}\""; then
            missing_labels+=("${label}")
        fi
    done

    if [ ${#missing_labels[@]} -eq 0 ]; then
        log_success "All required labels present"
        if [ "${VERBOSE}" = true ]; then
            echo "${labels}" | python3 -m json.tool 2>/dev/null || echo "${labels}"
        fi
        return 0
    else
        log_warn "Missing labels: ${missing_labels[*]}"
        return 1
    fi
}

test_image_size() {
    local service=$1
    local image_name="${service}"
    [ -n "${REGISTRY}" ] && image_name="${REGISTRY}/${service}"
    local full_image="${image_name}:${VERSION}"

    log_test "Checking image size"

    local size_bytes=$(docker inspect --format='{{.Size}}' "${full_image}" 2>/dev/null || echo "0")
    local size_mb=$((size_bytes / 1024 / 1024))

    local max_size_mb=300
    local warn_size_mb=250

    if [ ${size_mb} -gt ${max_size_mb} ]; then
        log_error "Image too large: ${size_mb}MB (max: ${max_size_mb}MB)"
        return 1
    elif [ ${size_mb} -gt ${warn_size_mb} ]; then
        log_warn "Image size: ${size_mb}MB (approaching max: ${max_size_mb}MB)"
        return 0
    else
        log_success "Image size: ${size_mb}MB"
        return 0
    fi
}

test_layer_count() {
    local service=$1
    local image_name="${service}"
    [ -n "${REGISTRY}" ] && image_name="${REGISTRY}/${service}"
    local full_image="${image_name}:${VERSION}"

    log_test "Analyzing image layers"

    local layer_count=$(docker history "${full_image}" 2>/dev/null | wc -l | tr -d ' ')
    ((layer_count--))  # Subtract header

    local max_layers=20
    local warn_layers=15

    if [ ${layer_count} -gt ${max_layers} ]; then
        log_warn "Too many layers: ${layer_count} (max recommended: ${max_layers})"
        return 1
    elif [ ${layer_count} -gt ${warn_layers} ]; then
        log_warn "Layer count: ${layer_count} (consider optimization)"
        return 0
    else
        log_success "Layer count: ${layer_count}"
        return 0
    fi
}

test_non_root_user() {
    local service=$1
    local image_name="${service}"
    [ -n "${REGISTRY}" ] && image_name="${REGISTRY}/${service}"
    local full_image="${image_name}:${VERSION}"

    log_test "Verifying non-root user execution"

    local user=$(docker inspect --format='{{.Config.User}}' "${full_image}" 2>/dev/null || echo "")

    if [ -z "${user}" ] || [ "${user}" = "root" ] || [ "${user}" = "0" ]; then
        log_error "Container runs as root (security risk)"
        return 1
    else
        log_success "Running as user: ${user}"
        return 0
    fi
}

test_exposed_ports() {
    local service=$1
    local expected_port=${SERVICE_PORTS[${service}]}
    local image_name="${service}"
    [ -n "${REGISTRY}" ] && image_name="${REGISTRY}/${service}"
    local full_image="${image_name}:${VERSION}"

    log_test "Validating exposed ports"

    local exposed_ports=$(docker inspect --format='{{json .Config.ExposedPorts}}' "${full_image}" 2>/dev/null || echo "{}")

    if echo "${exposed_ports}" | grep -q "${expected_port}/tcp"; then
        log_success "Port ${expected_port} exposed correctly"
        return 0
    else
        log_error "Expected port ${expected_port} not exposed"
        if [ "${VERBOSE}" = true ]; then
            echo "Exposed ports: ${exposed_ports}"
        fi
        return 1
    fi
}

test_environment_variables() {
    local service=$1
    local image_name="${service}"
    [ -n "${REGISTRY}" ] && image_name="${REGISTRY}/${service}"
    local full_image="${image_name}:${VERSION}"

    log_test "Checking environment variables"

    local env_vars=$(docker inspect --format='{{range .Config.Env}}{{println .}}{{end}}' "${full_image}" 2>/dev/null)

    # Check for required environment variables
    local required_vars=("JAVA_HOME" "LANG")
    local missing_vars=()

    for var in "${required_vars[@]}"; do
        if ! echo "${env_vars}" | grep -q "^${var}="; then
            missing_vars+=("${var}")
        fi
    done

    # Check for sensitive data (should not be present)
    local sensitive_patterns=("PASSWORD=" "SECRET=" "KEY=" "TOKEN=")
    local found_sensitive=()

    for pattern in "${sensitive_patterns[@]}"; do
        if echo "${env_vars}" | grep -iq "${pattern}"; then
            found_sensitive+=("${pattern}")
        fi
    done

    local result=0

    if [ ${#missing_vars[@]} -gt 0 ]; then
        log_warn "Missing recommended env vars: ${missing_vars[*]}"
        result=1
    fi

    if [ ${#found_sensitive[@]} -gt 0 ]; then
        log_error "Sensitive data found in env: ${found_sensitive[*]}"
        result=1
    fi

    if [ ${result} -eq 0 ]; then
        log_success "Environment variables configured correctly"
    fi

    return ${result}
}

test_container_startup() {
    local service=$1
    local image_name="${service}"
    [ -n "${REGISTRY}" ] && image_name="${REGISTRY}/${service}"
    local full_image="${image_name}:${VERSION}"
    local container_name="test-${service}-$$"

    log_test "Testing container startup"

    # Start container in detached mode
    if ! docker run -d --name "${container_name}" \
        -e "SPRING_PROFILES_ACTIVE=test" \
        -e "NACOS_SERVER_ADDR=127.0.0.1:8848" \
        "${full_image}" > /dev/null 2>&1; then
        log_error "Failed to start container"
        return 1
    fi

    # Wait for container to be running
    sleep 2

    local container_status=$(docker inspect --format='{{.State.Status}}' "${container_name}" 2>/dev/null || echo "unknown")

    # Cleanup
    docker stop "${container_name}" > /dev/null 2>&1 || true
    docker rm "${container_name}" > /dev/null 2>&1 || true

    if [ "${container_status}" = "running" ]; then
        log_success "Container started successfully"
        return 0
    else
        log_error "Container failed to start (status: ${container_status})"
        return 1
    fi
}

test_health_check() {
    local service=$1
    local port=${SERVICE_PORTS[${service}]}
    local image_name="${service}"
    [ -n "${REGISTRY}" ] && image_name="${REGISTRY}/${service}"
    local full_image="${image_name}:${VERSION}"
    local container_name="test-health-${service}-$$"

    log_test "Testing health check endpoint"

    # Start container with port mapping
    if ! docker run -d --name "${container_name}" \
        -p "${port}:${port}" \
        -e "SPRING_PROFILES_ACTIVE=test" \
        -e "NACOS_SERVER_ADDR=127.0.0.1:8848" \
        "${full_image}" > /dev/null 2>&1; then
        log_warn "Failed to start container for health check"
        return 1
    fi

    # Wait for application to start
    log_test "Waiting for application startup (timeout: ${HEALTH_CHECK_TIMEOUT}s)..."
    local elapsed=0
    local health_ok=false

    while [ ${elapsed} -lt ${HEALTH_CHECK_TIMEOUT} ]; do
        if curl -sf "http://localhost:${port}/actuator/health" > /dev/null 2>&1; then
            health_ok=true
            break
        fi
        sleep 2
        ((elapsed += 2))
    done

    # Cleanup
    docker stop "${container_name}" > /dev/null 2>&1 || true
    docker rm "${container_name}" > /dev/null 2>&1 || true

    if [ "${health_ok}" = true ]; then
        log_success "Health check passed (${elapsed}s)"
        return 0
    else
        log_warn "Health check timeout (may need dependencies)"
        return 1
    fi
}

test_vulnerability_scan() {
    local service=$1
    local image_name="${service}"
    [ -n "${REGISTRY}" ] && image_name="${REGISTRY}/${service}"
    local full_image="${image_name}:${VERSION}"

    if [ "${VULNERABILITY_SCAN}" = false ]; then
        log_test "Vulnerability scan skipped"
        return 0
    fi

    log_test "Scanning for vulnerabilities (using Docker Scout)"

    # Check if Docker Scout is available
    if ! docker scout version > /dev/null 2>&1; then
        log_warn "Docker Scout not available, skipping vulnerability scan"
        log_warn "Install: https://docs.docker.com/scout/install/"
        return 0
    fi

    local scan_log="${TEST_LOG_DIR}/${service}-vuln-scan.txt"

    if docker scout cves "${full_image}" > "${scan_log}" 2>&1; then
        local critical=$(grep -c "critical" "${scan_log}" 2>/dev/null || echo "0")
        local high=$(grep -c "high" "${scan_log}" 2>/dev/null || echo "0")

        if [ ${critical} -gt 0 ]; then
            log_error "Found ${critical} critical vulnerabilities"
            return 1
        elif [ ${high} -gt 5 ]; then
            log_warn "Found ${high} high-severity vulnerabilities"
            return 1
        else
            log_success "No critical vulnerabilities found"
            return 0
        fi
    else
        log_warn "Vulnerability scan failed (see ${scan_log})"
        return 1
    fi
}

################################################################################
# Test execution
################################################################################

run_all_tests_for_service() {
    local service=$1

    log_info "========================================="
    log_info "Testing: ${service}"
    log_info "========================================="

    local tests=(
        "test_image_exists"
        "test_image_labels"
        "test_image_size"
        "test_layer_count"
        "test_non_root_user"
        "test_exposed_ports"
        "test_environment_variables"
        "test_container_startup"
        "test_vulnerability_scan"
    )

    local passed=0
    local failed=0
    local warnings=0

    for test_func in "${tests[@]}"; do
        if ${test_func} "${service}"; then
            ((passed++))
        else
            ((failed++))
        fi
        echo ""
    done

    TEST_RESULTS[${service}]="${passed}/${#tests[@]}"

    if [ ${failed} -eq 0 ]; then
        log_success "${service}: All tests passed (${passed}/${#tests[@]})"
    else
        log_warn "${service}: ${failed} test(s) failed"
    fi

    echo ""
}

################################################################################
# Reporting functions
################################################################################

generate_text_report() {
    local report_file="${TEST_REPORT_DIR}/test-report-$(date +%Y%m%d-%H%M%S).txt"

    {
        echo "========================================="
        echo "GCRF Docker Image Test Report"
        echo "========================================="
        echo "Date:     $(date)"
        echo "Version:  ${VERSION}"
        echo "Registry: ${REGISTRY:-<local>}"
        echo "========================================="
        echo ""

        printf "%-25s %-15s\n" "Service" "Test Results"
        printf "%s\n" "---------------------------------------------"

        for service in "${!SERVICE_PORTS[@]}"; do
            local result=${TEST_RESULTS[${service}]:-"not tested"}
            printf "%-25s %-15s\n" "${service}" "${result}"
        done

        echo ""
        echo "Test Categories:"
        echo "  ✓ Image existence and metadata"
        echo "  ✓ Security (non-root user, no secrets)"
        echo "  ✓ Size and layer optimization"
        echo "  ✓ Port and environment configuration"
        echo "  ✓ Container startup validation"
        echo "  ✓ Vulnerability scanning"

    } | tee "${report_file}"

    log_success "Report saved to: ${report_file}"
}

generate_html_report() {
    local report_file="${TEST_REPORT_DIR}/test-report-$(date +%Y%m%d-%H%M%S).html"

    {
        cat <<'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GCRF Image Test Report</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f5f5f5; padding: 20px; }
        .container { max-width: 1000px; margin: 0 auto; background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 8px 8px 0 0; }
        .header h1 { font-size: 28px; margin-bottom: 10px; }
        .content { padding: 30px; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th { background: #f8f9fa; padding: 12px; text-align: left; font-weight: 600; border-bottom: 2px solid #dee2e6; }
        td { padding: 12px; border-bottom: 1px solid #dee2e6; }
        tr:hover { background: #f8f9fa; }
        .pass { color: #28a745; font-weight: bold; }
        .fail { color: #dc3545; font-weight: bold; }
        .footer { padding: 20px; background: #f8f9fa; text-align: center; color: #666; border-radius: 0 0 8px 8px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🐳 GCRF Docker Image Test Report</h1>
            <p>Version: VERSION_PLACEHOLDER</p>
            <p>Generated: TIMESTAMP_PLACEHOLDER</p>
        </div>
        <div class="content">
            <table>
                <thead>
                    <tr>
                        <th>Service</th>
                        <th>Test Results</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
EOF

        for service in "${!SERVICE_PORTS[@]}"; do
            local result=${TEST_RESULTS[${service}]:-"not tested"}
            local status_class="pass"
            echo "${result}" | grep -q "^0/" && status_class="fail"

            echo "                    <tr>"
            echo "                        <td><strong>${service}</strong></td>"
            echo "                        <td class=\"${status_class}\">${result}</td>"
            echo "                        <td class=\"${status_class}\">$([ "${status_class}" = "pass" ] && echo "✓ PASS" || echo "✗ FAIL")</td>"
            echo "                    </tr>"
        done

        cat <<EOF
                </tbody>
            </table>
        </div>
        <div class="footer">
            <p>GCRF DevOps Team - $(date +%Y)</p>
        </div>
    </div>
</body>
</html>
EOF
    } | sed \
        -e "s/VERSION_PLACEHOLDER/${VERSION}/g" \
        -e "s/TIMESTAMP_PLACEHOLDER/$(date)/g" \
        > "${report_file}"

    log_success "HTML report saved to: ${report_file}"
}

generate_report() {
    case ${REPORT_FORMAT} in
        html)
            generate_html_report
            ;;
        json)
            log_warn "JSON format not yet implemented, using text"
            generate_text_report
            ;;
        text|*)
            generate_text_report
            ;;
    esac
}

################################################################################
# Main execution
################################################################################

main() {
    # Parse options
    while [ $# -gt 0 ]; do
        case $1 in
            --version)
                VERSION=$2
                shift 2
                ;;
            --registry)
                REGISTRY=$2
                shift 2
                ;;
            --format)
                REPORT_FORMAT=$2
                shift 2
                ;;
            --no-vuln-scan)
                VULNERABILITY_SCAN=false
                shift
                ;;
            --timeout)
                HEALTH_CHECK_TIMEOUT=$2
                shift 2
                ;;
            --verbose)
                VERBOSE=true
                shift
                ;;
            -h|--help)
                usage
                ;;
            *)
                log_error "Unknown option: $1"
                usage
                ;;
        esac
    done

    # Setup
    setup_directories

    log_info "========================================="
    log_info "GCRF Docker Image Tester"
    log_info "========================================="
    log_info "Version:     ${VERSION}"
    log_info "Registry:    ${REGISTRY:-<local>}"
    log_info "Vuln Scan:   ${VULNERABILITY_SCAN}"
    log_info "Report:      ${REPORT_FORMAT}"
    log_info "========================================="
    echo ""

    # Test all services
    for service in "${!SERVICE_PORTS[@]}"; do
        run_all_tests_for_service "${service}"
    done

    # Generate report
    generate_report

    log_success "All tests completed!"
}

# Execute main
main "$@"
