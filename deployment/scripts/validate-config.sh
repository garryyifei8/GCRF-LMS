#!/bin/bash

################################################################################
# GCRF Library Management System - Configuration Validation Script
################################################################################
# Purpose: Validate environment variables and test service connections
#          before deployment to catch configuration issues early
#
# Usage: ./validate-config.sh [dev|prod]
#
# Exit Codes:
#   0 = All checks passed
#   1 = Missing required environment variables
#   2 = Validation failure (invalid values)
#   3 = Connection test failure
#
# Author: GCRF DevOps Team
# Last Updated: 2025-11-01
################################################################################

# Note: We don't use 'set -e' because we want to handle errors explicitly
# and continue validation even if some checks fail

# ============================================================================
# Color Codes for Output
# ============================================================================
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color
readonly BOLD='\033[1m'

# ============================================================================
# Global Variables
# ============================================================================
ENVIRONMENT="${1:-dev}"
CHECKS_PASSED=0
CHECKS_FAILED=0
WARNINGS=0
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# Load .env file if it exists
ENV_FILE="${PROJECT_ROOT}/.env.${ENVIRONMENT}"
if [[ -f "${ENV_FILE}" ]]; then
    echo -e "${BLUE}Loading environment from: ${ENV_FILE}${NC}"
    # Export variables from .env file (skip comments and empty lines)
    set -a
    # shellcheck disable=SC1090
    source "${ENV_FILE}"
    set +a
else
    echo -e "${YELLOW}⚠ No environment file found at ${ENV_FILE}${NC}"
    echo -e "${YELLOW}  Checking existing environment variables...${NC}"
fi

# ============================================================================
# Helper Functions
# ============================================================================

# Print section header
print_header() {
    echo ""
    echo -e "${BOLD}${BLUE}=== $1 ===${NC}"
    echo ""
}

# Print success message
print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

# Print error message
print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Print warning message
print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Print info message
print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

# Increment pass counter
mark_pass() {
    ((CHECKS_PASSED++))
}

# Increment fail counter
mark_fail() {
    ((CHECKS_FAILED++))
}

# Increment warning counter
mark_warning() {
    ((WARNINGS++))
}

# Mask sensitive value for display
mask_value() {
    local value="$1"
    local length="${#value}"
    if [[ ${length} -gt 8 ]]; then
        echo "${value:0:4}****${value: -4}"
    else
        echo "********"
    fi
}

# ============================================================================
# Validation Functions
# ============================================================================

# Check if a required environment variable is set
check_env_var() {
    local var_name="$1"
    local var_value="${!var_name}"
    local is_required="${2:-true}"

    if [[ -z "${var_value}" ]]; then
        if [[ "${is_required}" == "true" ]]; then
            print_error "${var_name}: NOT SET (required)"
            mark_fail
            return 1
        else
            print_warning "${var_name}: NOT SET (optional)"
            mark_warning
            return 0
        fi
    else
        # Mask sensitive values
        if [[ "${var_name}" =~ (PASSWORD|SECRET|TOKEN) ]]; then
            print_success "${var_name}: $(mask_value "${var_value}")"
        else
            print_success "${var_name}: ${var_value}"
        fi
        mark_pass
        return 0
    fi
}

# Validate JWT secret meets minimum requirements
validate_jwt_secret() {
    local jwt_secret="${JWT_SECRET:-}"

    if [[ -z "${jwt_secret}" ]]; then
        print_error "JWT_SECRET is not set"
        mark_fail
        return 1
    fi

    local length="${#jwt_secret}"
    if [[ ${length} -lt 64 ]]; then
        print_error "JWT_SECRET is too short (${length} chars, minimum 64 required for HS512)"
        mark_fail
        return 1
    fi

    print_success "JWT_SECRET length: ${length} characters (✓ meets HS512 requirement)"
    mark_pass

    # Check if using development default
    local dev_secret="DEV_SECRET_ONLY_NOT_FOR_PRODUCTION_USE_MINIMUM_64_CHARACTERS_REQUIRED_HERE"
    if [[ "${jwt_secret}" == "${dev_secret}" ]]; then
        if [[ "${ENVIRONMENT}" == "prod" ]]; then
            print_error "JWT_SECRET is using DEVELOPMENT default - NEVER use in production!"
            mark_fail
            return 1
        else
            print_warning "JWT_SECRET appears to be dev default - acceptable for dev environment"
            mark_warning
        fi
    fi

    return 0
}

# Test PostgreSQL connection
test_postgres_connection() {
    local db_host="${DB_HOST:-localhost}"
    local db_port="${DB_PORT:-5432}"
    local db_name="${DB_NAME:-gcrf_auth}"
    local db_user="${DB_USERNAME:-postgres}"
    local db_password="${DB_PASSWORD:-}"

    print_info "Testing PostgreSQL connection to ${db_host}:${db_port}/${db_name}..."

    # Check if psql is available
    if command -v psql &> /dev/null; then
        # Use psql for proper connection test with timeout
        if PGPASSWORD="${db_password}" PGCONNECT_TIMEOUT=5 psql -h "${db_host}" -p "${db_port}" -U "${db_user}" -d "${db_name}" -c "SELECT 1;" &> /dev/null; then
            print_success "PostgreSQL at ${db_host}:${db_port} - Connected successfully"
            mark_pass
            return 0
        else
            print_error "PostgreSQL at ${db_host}:${db_port} - Connection failed"
            print_info "  Verify: host, port, credentials, and database exists"
            mark_fail
            return 1
        fi
    else
        # Fallback to nc for basic connectivity check
        if command -v nc &> /dev/null; then
            # Use perl for timeout on macOS (nc doesn't have -w on macOS BSD version)
            if nc -z -G 5 "${db_host}" "${db_port}" &> /dev/null; then
                print_warning "PostgreSQL at ${db_host}:${db_port} - Port is reachable (psql not available for auth test)"
                mark_warning
                return 0
            else
                print_error "PostgreSQL at ${db_host}:${db_port} - Port is not reachable"
                mark_fail
                return 1
            fi
        else
            print_warning "Cannot test PostgreSQL (psql and nc not available)"
            mark_warning
            return 0
        fi
    fi
}

# Test Redis connection
test_redis_connection() {
    local redis_host="${REDIS_HOST:-localhost}"
    local redis_port="${REDIS_PORT:-6379}"
    local redis_password="${REDIS_PASSWORD:-}"

    print_info "Testing Redis connection to ${redis_host}:${redis_port}..."

    # Check if redis-cli is available
    if command -v redis-cli &> /dev/null; then
        local auth_cmd=""
        if [[ -n "${redis_password}" ]]; then
            auth_cmd="-a ${redis_password}"
        fi

        # shellcheck disable=SC2086
        if redis-cli -h "${redis_host}" -p "${redis_port}" --connect-timeout 5 ${auth_cmd} ping 2>/dev/null | grep -q "PONG"; then
            print_success "Redis at ${redis_host}:${redis_port} - Connected successfully (PONG received)"
            mark_pass
            return 0
        else
            print_error "Redis at ${redis_host}:${redis_port} - Connection failed or authentication error"
            print_info "  Verify: host, port, and password"
            mark_fail
            return 1
        fi
    else
        # Fallback to nc for basic connectivity check
        if command -v nc &> /dev/null; then
            if nc -z -G 5 "${redis_host}" "${redis_port}" &> /dev/null; then
                print_warning "Redis at ${redis_host}:${redis_port} - Port is reachable (redis-cli not available for auth test)"
                mark_warning
                return 0
            else
                print_error "Redis at ${redis_host}:${redis_port} - Port is not reachable"
                mark_fail
                return 1
            fi
        else
            print_warning "Cannot test Redis (redis-cli and nc not available)"
            mark_warning
            return 0
        fi
    fi
}

# Test Nacos connection
test_nacos_connection() {
    local nacos_addr="${NACOS_SERVER_ADDR:-localhost:8848}"

    # Split host and port
    local nacos_host="${nacos_addr%:*}"
    local nacos_port="${nacos_addr##*:}"

    # Default to 8848 if no port specified
    if [[ "${nacos_host}" == "${nacos_port}" ]]; then
        nacos_port="8848"
    fi

    print_info "Testing Nacos connection to http://${nacos_host}:${nacos_port}..."

    # Check if curl is available
    if command -v curl &> /dev/null; then
        local health_url="http://${nacos_host}:${nacos_port}/nacos/v1/console/health"

        if curl --connect-timeout 5 --max-time 5 -s -f "${health_url}" &> /dev/null; then
            print_success "Nacos at http://${nacos_host}:${nacos_port} - Health check passed"
            mark_pass
            return 0
        else
            print_error "Nacos at http://${nacos_host}:${nacos_port} - Health check failed"
            print_info "  Verify: Nacos is running and accessible"
            print_info "  Try: curl ${health_url}"
            mark_fail
            return 1
        fi
    else
        # Fallback to nc for basic connectivity check
        if command -v nc &> /dev/null; then
            if nc -z -G 5 "${nacos_host}" "${nacos_port}" &> /dev/null; then
                print_warning "Nacos at ${nacos_host}:${nacos_port} - Port is reachable (curl not available for health check)"
                mark_warning
                return 0
            else
                print_error "Nacos at ${nacos_host}:${nacos_port} - Port is not reachable"
                mark_fail
                return 1
            fi
        else
            print_warning "Cannot test Nacos (curl and nc not available)"
            mark_warning
            return 0
        fi
    fi
}

# ============================================================================
# Main Validation Logic
# ============================================================================

main() {
    print_header "GCRF Configuration Validation (${ENVIRONMENT} environment)"

    # Validate environment argument
    if [[ "${ENVIRONMENT}" != "dev" && "${ENVIRONMENT}" != "prod" ]]; then
        echo -e "${RED}Error: Invalid environment '${ENVIRONMENT}'${NC}"
        echo "Usage: $0 [dev|prod]"
        exit 1
    fi

    # ========================================================================
    # 1. Check Gateway Service Environment Variables
    # ========================================================================
    print_header "Checking Gateway Service Environment Variables"

    check_env_var "NACOS_SERVER_ADDR" "false"
    check_env_var "NACOS_USERNAME" "true"
    check_env_var "NACOS_PASSWORD" "true"
    check_env_var "JWT_SECRET" "true"
    check_env_var "REDIS_HOST" "false"
    check_env_var "REDIS_PORT" "false"
    check_env_var "REDIS_PASSWORD" "true"

    # Optional gateway variables
    check_env_var "CORS_ALLOWED_ORIGINS" "false"
    check_env_var "GATEWAY_CONNECT_TIMEOUT" "false"
    check_env_var "GATEWAY_RESPONSE_TIMEOUT" "false"

    # ========================================================================
    # 2. Check Auth Service Environment Variables
    # ========================================================================
    print_header "Checking Auth Service Environment Variables"

    check_env_var "DB_HOST" "false"
    check_env_var "DB_PORT" "false"
    check_env_var "DB_NAME" "false"
    check_env_var "DB_USERNAME" "true"
    check_env_var "DB_PASSWORD" "true"

    # Redis (shared with gateway)
    print_info "Redis configuration (shared with Gateway):"
    echo "  - REDIS_HOST: ${REDIS_HOST:-localhost}"
    echo "  - REDIS_PORT: ${REDIS_PORT:-6379}"

    # JWT (must match gateway)
    print_info "JWT configuration (must match Gateway):"
    echo "  - JWT_SECRET: $(mask_value "${JWT_SECRET:-}")"

    # ========================================================================
    # 3. Validate JWT Secret
    # ========================================================================
    print_header "Validating JWT Secret"

    validate_jwt_secret

    # ========================================================================
    # 4. Test Database Connection
    # ========================================================================
    print_header "Testing Database Connection"

    test_postgres_connection

    # ========================================================================
    # 5. Test Redis Connection
    # ========================================================================
    print_header "Testing Redis Connection"

    test_redis_connection

    # ========================================================================
    # 6. Test Nacos Connection
    # ========================================================================
    print_header "Testing Nacos Connection"

    test_nacos_connection

    # ========================================================================
    # 7. Environment-Specific Validations
    # ========================================================================
    print_header "Environment-Specific Validations (${ENVIRONMENT})"

    if [[ "${ENVIRONMENT}" == "prod" ]]; then
        # Production-specific checks

        # Check CORS configuration
        local cors_origins="${CORS_ALLOWED_ORIGINS:-}"
        if [[ -z "${cors_origins}" || "${cors_origins}" == *"localhost"* || "${cors_origins}" == *"*"* ]]; then
            print_error "CORS_ALLOWED_ORIGINS not set properly for production"
            print_info "  Should be specific domain(s) like: https://library.gcrf.com"
            mark_fail
        else
            print_success "CORS_ALLOWED_ORIGINS configured for production: ${cors_origins}"
            mark_pass
        fi

        # Check JWT secret is not dev default
        if [[ "${JWT_SECRET:-}" == *"DEV_SECRET"* ]]; then
            print_error "Production is using DEVELOPMENT JWT_SECRET!"
            mark_fail
        fi

        # Check database SSL mode (if URL is set)
        if [[ -n "${DB_URL:-}" ]]; then
            if [[ "${DB_URL}" != *"sslmode=require"* && "${DB_URL}" != *"sslmode=verify"* ]]; then
                print_warning "Database URL does not enforce SSL (sslmode=require missing)"
                mark_warning
            else
                print_success "Database URL enforces SSL connection"
                mark_pass
            fi
        fi

    else
        # Development-specific checks
        print_info "Development environment - using relaxed validation"

        # Warn if production-like settings in dev
        if [[ "${CORS_ALLOWED_ORIGINS:-}" != *"localhost"* && -n "${CORS_ALLOWED_ORIGINS:-}" ]]; then
            print_warning "CORS is restricted in dev environment - may cause frontend issues"
            mark_warning
        fi

        mark_pass
    fi

    # ========================================================================
    # 8. Summary
    # ========================================================================
    print_header "Summary"

    local total_checks=$((CHECKS_PASSED + CHECKS_FAILED))

    echo ""
    if [[ ${CHECKS_FAILED} -eq 0 ]]; then
        print_success "All checks passed (${CHECKS_PASSED}/${total_checks})"
    else
        print_error "Some checks failed (${CHECKS_PASSED}/${total_checks} passed, ${CHECKS_FAILED} failed)"
    fi

    if [[ ${WARNINGS} -gt 0 ]]; then
        print_warning "${WARNINGS} warning(s) detected"
    fi

    echo ""

    if [[ ${CHECKS_FAILED} -eq 0 ]]; then
        echo -e "${GREEN}${BOLD}Configuration is valid for ${ENVIRONMENT} environment.${NC}"
        echo ""
        exit 0
    else
        echo -e "${RED}${BOLD}Configuration validation FAILED for ${ENVIRONMENT} environment.${NC}"
        echo ""
        echo -e "${YELLOW}Remediation steps:${NC}"
        echo "  1. Review error messages above"
        echo "  2. Set missing environment variables in ${ENV_FILE}"
        echo "  3. Verify service connections (database, Redis, Nacos)"
        echo "  4. For production: ensure all secrets are changed from defaults"
        echo "  5. Run validation again: ./validate-config.sh ${ENVIRONMENT}"
        echo ""

        if [[ ${CHECKS_FAILED} -gt 5 ]]; then
            exit 1  # Missing variables
        elif [[ ${CHECKS_FAILED} -le 2 ]]; then
            exit 3  # Connection failures
        else
            exit 2  # Validation failures
        fi
    fi
}

# ============================================================================
# Script Entry Point
# ============================================================================

# Show usage if help requested
if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
    echo "GCRF Configuration Validation Script"
    echo ""
    echo "Usage: $0 [dev|prod]"
    echo ""
    echo "Arguments:"
    echo "  dev   - Validate development environment (default)"
    echo "  prod  - Validate production environment"
    echo ""
    echo "Exit Codes:"
    echo "  0 - All checks passed"
    echo "  1 - Missing required environment variables"
    echo "  2 - Validation failure (invalid values)"
    echo "  3 - Connection test failure"
    echo ""
    echo "Environment Variables:"
    echo "  Gateway Service:"
    echo "    - NACOS_SERVER_ADDR (optional, default: localhost:8848)"
    echo "    - NACOS_USERNAME (required)"
    echo "    - NACOS_PASSWORD (required)"
    echo "    - JWT_SECRET (required, min 64 chars)"
    echo "    - REDIS_HOST (optional, default: localhost)"
    echo "    - REDIS_PASSWORD (required)"
    echo ""
    echo "  Auth Service:"
    echo "    - DB_HOST (optional, default: localhost)"
    echo "    - DB_USERNAME (required)"
    echo "    - DB_PASSWORD (required)"
    echo "    - REDIS_HOST (shared with Gateway)"
    echo "    - REDIS_PASSWORD (shared with Gateway)"
    echo "    - JWT_SECRET (must match Gateway)"
    echo ""
    exit 0
fi

# Run main function
main
