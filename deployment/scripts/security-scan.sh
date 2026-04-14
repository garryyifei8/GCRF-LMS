#!/bin/bash

# ============================================
# GCRF Library Management System
# Security Scanning Script
# ============================================
# This script performs various security checks:
# - OWASP Dependency Check
# - Port scanning
# - SSL/TLS configuration check
# - HTTP security headers check
# - Container image scanning
# ============================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
BACKEND_DIR="${PROJECT_ROOT}/backend"
REPORT_DIR="${PROJECT_ROOT}/doc/security/scan-reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Target configuration (modify for your environment)
TARGET_HOST="${TARGET_HOST:-localhost}"
GATEWAY_PORT="${GATEWAY_PORT:-8080}"
HTTPS_PORT="${HTTPS_PORT:-443}"

# Tool paths (modify if not in PATH)
NMAP_CMD="${NMAP_CMD:-nmap}"
CURL_CMD="${CURL_CMD:-curl}"
OPENSSL_CMD="${OPENSSL_CMD:-openssl}"
TRIVY_CMD="${TRIVY_CMD:-trivy}"
MVN_CMD="${MVN_CMD:-mvn}"

# ============================================
# Helper Functions
# ============================================

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_section() {
    echo ""
    echo -e "${BLUE}============================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}============================================${NC}"
}

check_tool() {
    local tool=$1
    local install_hint=$2
    if ! command -v "$tool" &> /dev/null; then
        log_warning "Tool not found: $tool"
        if [ -n "$install_hint" ]; then
            log_info "Install with: $install_hint"
        fi
        return 1
    fi
    return 0
}

create_report_dir() {
    mkdir -p "${REPORT_DIR}"
    log_info "Reports will be saved to: ${REPORT_DIR}"
}

# ============================================
# 1. OWASP Dependency Check
# ============================================

run_dependency_check() {
    log_section "1. OWASP Dependency Check"

    local report_file="${REPORT_DIR}/dependency-check-${TIMESTAMP}.html"

    # Check if dependency-check plugin is available
    if [ -f "${BACKEND_DIR}/pom.xml" ]; then
        log_info "Running Maven dependency check..."

        # Check for OWASP Dependency-Check Maven plugin
        cd "${BACKEND_DIR}"

        # Run dependency check (requires plugin to be configured in pom.xml)
        if grep -q "dependency-check-maven" pom.xml 2>/dev/null; then
            ${MVN_CMD} org.owasp:dependency-check-maven:check \
                -DoutputDirectory="${REPORT_DIR}" \
                -DsuppressionFile="${PROJECT_ROOT}/dependency-check-suppressions.xml" \
                2>&1 | tee "${REPORT_DIR}/dependency-check-${TIMESTAMP}.log"
            log_success "Dependency check complete. Report: ${REPORT_DIR}/dependency-check-report.html"
        else
            log_warning "OWASP Dependency-Check plugin not found in pom.xml"
            log_info "Add the following to your pom.xml plugins section:"
            cat << 'EOF'

<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>9.0.9</version>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>
        <formats>
            <format>HTML</format>
            <format>JSON</format>
        </formats>
    </configuration>
</plugin>
EOF

            # Alternative: Run npm audit for frontend
            if [ -d "${PROJECT_ROOT}/web-admin" ]; then
                log_info "Running npm audit for frontend..."
                cd "${PROJECT_ROOT}/web-admin"
                npm audit --json > "${REPORT_DIR}/npm-audit-${TIMESTAMP}.json" 2>&1 || true
                npm audit 2>&1 | tee "${REPORT_DIR}/npm-audit-${TIMESTAMP}.txt" || true
                log_success "NPM audit complete. Report: ${REPORT_DIR}/npm-audit-${TIMESTAMP}.txt"
            fi
        fi
    else
        log_error "Backend pom.xml not found"
    fi
}

# ============================================
# 2. Port Scanning
# ============================================

run_port_scan() {
    log_section "2. Port Scanning"

    local report_file="${REPORT_DIR}/port-scan-${TIMESTAMP}.txt"

    if check_tool "nmap" "brew install nmap (macOS) or apt install nmap (Linux)"; then
        log_info "Scanning ports on ${TARGET_HOST}..."

        # Common service ports
        local ports="22,80,443,5432,5433,5434,6379,6380,6382,5672,15672,8080,8081,8082,8083,8084,8085,8086,8848,9000,9001,9200"

        ${NMAP_CMD} -sV -sC -p${ports} ${TARGET_HOST} \
            -oN "${report_file}" 2>&1 | tee -a "${report_file}"

        log_success "Port scan complete. Report: ${report_file}"

        # Check for unexpected open ports
        log_info "Checking for potentially dangerous open ports..."
        if grep -q "open" "${report_file}"; then
            grep "open" "${report_file}" | while read line; do
                if echo "$line" | grep -qE "(telnet|ftp|mysql|mongo|elastic|redis)"; then
                    log_warning "Potentially sensitive service exposed: $line"
                fi
            done
        fi
    else
        log_warning "Skipping port scan - nmap not available"

        # Fallback: Basic port check with nc
        log_info "Running basic port check with netcat..."
        local basic_ports=(8080 5432 6379 8848 9000)
        for port in "${basic_ports[@]}"; do
            if nc -z -w 2 ${TARGET_HOST} ${port} 2>/dev/null; then
                log_info "Port ${port}: OPEN"
            else
                log_info "Port ${port}: CLOSED/FILTERED"
            fi
        done
    fi
}

# ============================================
# 3. SSL/TLS Configuration Check
# ============================================

run_ssl_check() {
    log_section "3. SSL/TLS Configuration Check"

    local report_file="${REPORT_DIR}/ssl-check-${TIMESTAMP}.txt"

    if check_tool "openssl"; then
        log_info "Checking SSL/TLS configuration on ${TARGET_HOST}:${HTTPS_PORT}..."

        # Skip if HTTPS is not configured
        if ! timeout 5 bash -c "echo | openssl s_client -connect ${TARGET_HOST}:${HTTPS_PORT} 2>/dev/null" | grep -q "CONNECTED"; then
            log_warning "HTTPS not available on ${TARGET_HOST}:${HTTPS_PORT} - skipping SSL checks"
            log_info "Ensure HTTPS is configured before production deployment"
            return
        fi

        {
            echo "SSL/TLS Check Report - ${TIMESTAMP}"
            echo "Target: ${TARGET_HOST}:${HTTPS_PORT}"
            echo "============================================"
            echo ""

            # Check certificate details
            echo "=== Certificate Information ==="
            echo | ${OPENSSL_CMD} s_client -connect ${TARGET_HOST}:${HTTPS_PORT} 2>/dev/null | \
                ${OPENSSL_CMD} x509 -noout -dates -subject -issuer
            echo ""

            # Check supported protocols
            echo "=== Protocol Support ==="
            for protocol in ssl3 tls1 tls1_1 tls1_2 tls1_3; do
                if echo | ${OPENSSL_CMD} s_client -connect ${TARGET_HOST}:${HTTPS_PORT} -${protocol} 2>/dev/null | grep -q "CONNECTED"; then
                    if [[ "$protocol" == "ssl3" || "$protocol" == "tls1" || "$protocol" == "tls1_1" ]]; then
                        echo "[VULNERABLE] ${protocol}: SUPPORTED (should be disabled)"
                    else
                        echo "[OK] ${protocol}: SUPPORTED"
                    fi
                else
                    echo "[OK] ${protocol}: NOT SUPPORTED"
                fi
            done
            echo ""

            # Check cipher suites
            echo "=== Cipher Suites ==="
            echo | ${OPENSSL_CMD} s_client -connect ${TARGET_HOST}:${HTTPS_PORT} 2>/dev/null | grep "Cipher"
            echo ""

            # Check for HSTS header
            echo "=== HSTS Check ==="
            ${CURL_CMD} -sI https://${TARGET_HOST}:${HTTPS_PORT}/ 2>/dev/null | grep -i "strict-transport-security" || echo "HSTS header not found"

        } > "${report_file}" 2>&1

        cat "${report_file}"
        log_success "SSL check complete. Report: ${report_file}"
    else
        log_warning "Skipping SSL check - openssl not available"
    fi
}

# ============================================
# 4. HTTP Security Headers Check
# ============================================

run_headers_check() {
    log_section "4. HTTP Security Headers Check"

    local report_file="${REPORT_DIR}/headers-check-${TIMESTAMP}.txt"
    local target_url="http://${TARGET_HOST}:${GATEWAY_PORT}"

    if check_tool "curl"; then
        log_info "Checking security headers on ${target_url}..."

        # Check if target is reachable
        if ! ${CURL_CMD} -s -o /dev/null -w "%{http_code}" "${target_url}/actuator/health" | grep -qE "^(200|401|403)"; then
            log_warning "Target ${target_url} not reachable - is the Gateway service running?"
            return
        fi

        {
            echo "HTTP Security Headers Check - ${TIMESTAMP}"
            echo "Target: ${target_url}"
            echo "============================================"
            echo ""

            # Get all headers
            echo "=== Response Headers ==="
            ${CURL_CMD} -sI "${target_url}/" 2>/dev/null | head -30
            echo ""

            # Check specific security headers
            echo "=== Security Header Analysis ==="

            local headers=$(${CURL_CMD} -sI "${target_url}/" 2>/dev/null)

            # X-Frame-Options
            if echo "$headers" | grep -qi "x-frame-options"; then
                echo "[OK] X-Frame-Options: $(echo "$headers" | grep -i "x-frame-options")"
            else
                echo "[MISSING] X-Frame-Options: Not set (clickjacking vulnerability)"
            fi

            # X-Content-Type-Options
            if echo "$headers" | grep -qi "x-content-type-options"; then
                echo "[OK] X-Content-Type-Options: $(echo "$headers" | grep -i "x-content-type-options")"
            else
                echo "[MISSING] X-Content-Type-Options: Not set (MIME sniffing vulnerability)"
            fi

            # X-XSS-Protection
            if echo "$headers" | grep -qi "x-xss-protection"; then
                echo "[OK] X-XSS-Protection: $(echo "$headers" | grep -i "x-xss-protection")"
            else
                echo "[MISSING] X-XSS-Protection: Not set"
            fi

            # Content-Security-Policy
            if echo "$headers" | grep -qi "content-security-policy"; then
                echo "[OK] Content-Security-Policy: Present"
            else
                echo "[MISSING] Content-Security-Policy: Not set (XSS vulnerability)"
            fi

            # Strict-Transport-Security
            if echo "$headers" | grep -qi "strict-transport-security"; then
                echo "[OK] Strict-Transport-Security: $(echo "$headers" | grep -i "strict-transport-security")"
            else
                echo "[MISSING] Strict-Transport-Security: Not set (requires HTTPS)"
            fi

            # Referrer-Policy
            if echo "$headers" | grep -qi "referrer-policy"; then
                echo "[OK] Referrer-Policy: $(echo "$headers" | grep -i "referrer-policy")"
            else
                echo "[MISSING] Referrer-Policy: Not set"
            fi

            # Permissions-Policy
            if echo "$headers" | grep -qi "permissions-policy"; then
                echo "[OK] Permissions-Policy: $(echo "$headers" | grep -i "permissions-policy")"
            else
                echo "[MISSING] Permissions-Policy: Not set"
            fi

            # Server header (should be hidden)
            if echo "$headers" | grep -qi "^server:"; then
                echo "[WARNING] Server header exposed: $(echo "$headers" | grep -i "^server:")"
            else
                echo "[OK] Server header: Hidden"
            fi

            # X-Powered-By (should be hidden)
            if echo "$headers" | grep -qi "x-powered-by"; then
                echo "[WARNING] X-Powered-By exposed: $(echo "$headers" | grep -i "x-powered-by")"
            else
                echo "[OK] X-Powered-By: Hidden"
            fi

        } > "${report_file}" 2>&1

        cat "${report_file}"
        log_success "Headers check complete. Report: ${report_file}"
    else
        log_warning "Skipping headers check - curl not available"
    fi
}

# ============================================
# 5. Container Image Scanning
# ============================================

run_container_scan() {
    log_section "5. Container Image Scanning"

    local report_file="${REPORT_DIR}/container-scan-${TIMESTAMP}.txt"

    if check_tool "trivy" "brew install trivy (macOS) or apt install trivy (Linux)"; then
        log_info "Scanning container images..."

        # List of images to scan
        local images=(
            "gcrf-library/gateway-service:latest"
            "gcrf-library/auth-service:latest"
            "gcrf-library/book-service:latest"
            "postgres:15-alpine"
            "redis:7.2-alpine"
            "rabbitmq:3.12-management-alpine"
            "minio/minio:latest"
        )

        {
            echo "Container Image Security Scan - ${TIMESTAMP}"
            echo "============================================"
            echo ""

            for image in "${images[@]}"; do
                echo "=== Scanning: ${image} ==="
                if docker image inspect "${image}" &>/dev/null; then
                    ${TRIVY_CMD} image --severity HIGH,CRITICAL "${image}" 2>&1 || true
                else
                    echo "Image not found locally: ${image}"
                fi
                echo ""
            done

        } > "${report_file}" 2>&1

        log_success "Container scan complete. Report: ${report_file}"
    else
        log_warning "Skipping container scan - trivy not available"
        log_info "Consider using: docker scan <image> (Docker Desktop)"
    fi
}

# ============================================
# 6. Configuration Security Check
# ============================================

run_config_check() {
    log_section "6. Configuration Security Check"

    local report_file="${REPORT_DIR}/config-check-${TIMESTAMP}.txt"

    log_info "Checking for security misconfigurations..."

    {
        echo "Configuration Security Check - ${TIMESTAMP}"
        echo "============================================"
        echo ""

        # Check for hardcoded credentials
        echo "=== Hardcoded Credentials Check ==="

        local credential_patterns="password.*=.*['\"].*['\"]|secret.*=.*['\"].*['\"]|api[_-]?key.*=.*['\"].*['\"]"

        echo "Checking YAML files for hardcoded secrets..."
        find "${BACKEND_DIR}" -name "*.yml" -o -name "*.yaml" | while read file; do
            if grep -lE "${credential_patterns}" "$file" 2>/dev/null; then
                echo "[WARNING] Potential hardcoded credential in: $file"
                grep -nE "${credential_patterns}" "$file" 2>/dev/null || true
            fi
        done
        echo ""

        # Check for default passwords
        echo "=== Default Password Check ==="
        local default_passwords=("nacos" "admin" "password" "123456" "minioadmin" "gcrf_secure_2024" "gcrf_redis_2024" "gcrf_rabbitmq_2024" "gcrf_minio_2024")

        for pwd in "${default_passwords[@]}"; do
            if grep -rl "${pwd}" "${BACKEND_DIR}" --include="*.yml" --include="*.yaml" --include="*.properties" 2>/dev/null; then
                echo "[WARNING] Default password '${pwd}' found in configuration files"
            fi
        done
        echo ""

        # Check for debug mode
        echo "=== Debug Mode Check ==="
        if grep -rn "level:.*debug" "${BACKEND_DIR}" --include="*.yml" 2>/dev/null; then
            echo "[WARNING] Debug logging is enabled - should be disabled in production"
        fi
        echo ""

        # Check for exposed actuator endpoints
        echo "=== Actuator Endpoints Check ==="
        if grep -rn "exposure:.*include:.*\*" "${BACKEND_DIR}" --include="*.yml" 2>/dev/null; then
            echo "[WARNING] All actuator endpoints are exposed"
        fi
        echo ""

        # Check .env files
        echo "=== Environment Files Check ==="
        if [ -f "${PROJECT_ROOT}/.env" ]; then
            echo "[WARNING] .env file exists - ensure it's not committed to version control"
        fi

        if [ -f "${PROJECT_ROOT}/.env.prod" ]; then
            echo "[WARNING] .env.prod file exists - ensure it's not committed to version control"
        fi

        # Check gitignore for sensitive files
        if [ -f "${PROJECT_ROOT}/.gitignore" ]; then
            if ! grep -q ".env" "${PROJECT_ROOT}/.gitignore"; then
                echo "[WARNING] .env files may not be ignored by git"
            fi
        fi
        echo ""

        # Check CORS configuration
        echo "=== CORS Configuration Check ==="
        if grep -rn "allowedOriginPatterns.*\*" "${BACKEND_DIR}" --include="*.yml" 2>/dev/null; then
            echo "[WARNING] CORS allows all origins - should be restricted in production"
        fi

    } > "${report_file}" 2>&1

    cat "${report_file}"
    log_success "Configuration check complete. Report: ${report_file}"
}

# ============================================
# 7. API Security Check
# ============================================

run_api_check() {
    log_section "7. API Security Check"

    local report_file="${REPORT_DIR}/api-check-${TIMESTAMP}.txt"
    local target_url="http://${TARGET_HOST}:${GATEWAY_PORT}"

    log_info "Checking API security on ${target_url}..."

    {
        echo "API Security Check - ${TIMESTAMP}"
        echo "Target: ${target_url}"
        echo "============================================"
        echo ""

        # Check if API is accessible
        if ! ${CURL_CMD} -s -o /dev/null -w "%{http_code}" "${target_url}/actuator/health" | grep -qE "^(200|401|403)"; then
            echo "[INFO] Target ${target_url} not reachable - skipping API checks"
            return
        fi

        # Check authentication requirement
        echo "=== Authentication Check ==="

        # Test protected endpoint without auth
        local protected_response=$(${CURL_CMD} -s -w "\n%{http_code}" "${target_url}/api/v1/books")
        local protected_status=$(echo "$protected_response" | tail -1)

        if [ "$protected_status" = "401" ] || [ "$protected_status" = "403" ]; then
            echo "[OK] Protected endpoints require authentication (${protected_status})"
        else
            echo "[WARNING] Protected endpoint accessible without authentication (${protected_status})"
        fi
        echo ""

        # Check rate limiting
        echo "=== Rate Limiting Check ==="
        echo "Testing login endpoint rate limiting..."

        local rate_limit_hit=false
        for i in {1..15}; do
            local response=$(${CURL_CMD} -s -w "\n%{http_code}" -X POST \
                -H "Content-Type: application/json" \
                -d '{"username":"test","password":"test"}' \
                "${target_url}/api/v1/auth/login")
            local status=$(echo "$response" | tail -1)

            if [ "$status" = "429" ]; then
                echo "[OK] Rate limiting is active (triggered after $i attempts)"
                rate_limit_hit=true
                break
            fi
        done

        if [ "$rate_limit_hit" = false ]; then
            echo "[WARNING] Rate limiting may not be active (no 429 response after 15 attempts)"
        fi
        echo ""

        # Check Swagger/OpenAPI exposure
        echo "=== API Documentation Exposure ==="

        local swagger_endpoints=("/swagger-ui.html" "/doc.html" "/v3/api-docs" "/swagger-resources")

        for endpoint in "${swagger_endpoints[@]}"; do
            local doc_status=$(${CURL_CMD} -s -o /dev/null -w "%{http_code}" "${target_url}${endpoint}")
            if [ "$doc_status" = "200" ]; then
                echo "[WARNING] API documentation exposed: ${endpoint}"
            else
                echo "[OK] ${endpoint} not publicly accessible (${doc_status})"
            fi
        done
        echo ""

        # Check actuator endpoints
        echo "=== Actuator Endpoints Check ==="

        local actuator_endpoints=("/actuator" "/actuator/health" "/actuator/info" "/actuator/metrics" "/actuator/env" "/actuator/configprops")

        for endpoint in "${actuator_endpoints[@]}"; do
            local act_status=$(${CURL_CMD} -s -o /dev/null -w "%{http_code}" "${target_url}${endpoint}")
            case "$endpoint" in
                "/actuator/health")
                    echo "[INFO] ${endpoint}: ${act_status} (should be accessible for health checks)"
                    ;;
                *)
                    if [ "$act_status" = "200" ]; then
                        echo "[WARNING] Sensitive actuator endpoint exposed: ${endpoint}"
                    else
                        echo "[OK] ${endpoint} protected (${act_status})"
                    fi
                    ;;
            esac
        done

    } > "${report_file}" 2>&1

    cat "${report_file}"
    log_success "API check complete. Report: ${report_file}"
}

# ============================================
# Main Execution
# ============================================

print_banner() {
    echo ""
    echo -e "${BLUE}================================================${NC}"
    echo -e "${BLUE}  GCRF Library System - Security Scanner${NC}"
    echo -e "${BLUE}================================================${NC}"
    echo ""
    echo "Target Host: ${TARGET_HOST}"
    echo "Gateway Port: ${GATEWAY_PORT}"
    echo "Timestamp: ${TIMESTAMP}"
    echo ""
}

print_summary() {
    log_section "Scan Summary"

    local summary_file="${REPORT_DIR}/summary-${TIMESTAMP}.txt"

    {
        echo "Security Scan Summary - ${TIMESTAMP}"
        echo "============================================"
        echo ""
        echo "Reports generated:"
        ls -la "${REPORT_DIR}"/*-${TIMESTAMP}* 2>/dev/null || echo "No reports found"
        echo ""
        echo "Next Steps:"
        echo "1. Review all generated reports in ${REPORT_DIR}"
        echo "2. Address any HIGH and CRITICAL findings"
        echo "3. Update security checklist based on findings"
        echo "4. Run scan again after remediation"
        echo ""
    } > "${summary_file}"

    cat "${summary_file}"
    log_success "All scans complete! Reports saved to: ${REPORT_DIR}"
}

main() {
    print_banner
    create_report_dir

    # Parse arguments
    local run_all=true
    local specific_scan=""

    while [[ $# -gt 0 ]]; do
        case $1 in
            --dependency)
                run_all=false
                specific_scan="dependency"
                shift
                ;;
            --ports)
                run_all=false
                specific_scan="ports"
                shift
                ;;
            --ssl)
                run_all=false
                specific_scan="ssl"
                shift
                ;;
            --headers)
                run_all=false
                specific_scan="headers"
                shift
                ;;
            --containers)
                run_all=false
                specific_scan="containers"
                shift
                ;;
            --config)
                run_all=false
                specific_scan="config"
                shift
                ;;
            --api)
                run_all=false
                specific_scan="api"
                shift
                ;;
            --host)
                TARGET_HOST="$2"
                shift 2
                ;;
            --port)
                GATEWAY_PORT="$2"
                shift 2
                ;;
            --help)
                echo "Usage: $0 [options]"
                echo ""
                echo "Options:"
                echo "  --dependency    Run OWASP dependency check only"
                echo "  --ports         Run port scan only"
                echo "  --ssl           Run SSL/TLS check only"
                echo "  --headers       Run HTTP headers check only"
                echo "  --containers    Run container image scan only"
                echo "  --config        Run configuration check only"
                echo "  --api           Run API security check only"
                echo "  --host HOST     Set target host (default: localhost)"
                echo "  --port PORT     Set gateway port (default: 8080)"
                echo "  --help          Show this help message"
                echo ""
                echo "Without options, all scans will be run."
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                exit 1
                ;;
        esac
    done

    if [ "$run_all" = true ]; then
        run_dependency_check
        run_port_scan
        run_ssl_check
        run_headers_check
        run_container_scan
        run_config_check
        run_api_check
    else
        case $specific_scan in
            dependency) run_dependency_check ;;
            ports) run_port_scan ;;
            ssl) run_ssl_check ;;
            headers) run_headers_check ;;
            containers) run_container_scan ;;
            config) run_config_check ;;
            api) run_api_check ;;
        esac
    fi

    print_summary
}

# Run main function
main "$@"
