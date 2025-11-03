#!/bin/bash
# =============================================================================
# GCRF Library Management System - Secrets Validator
# Version: 1.0.0
# Last Updated: 2025-11-01
#
# This script validates that all secrets meet security requirements
# Usage: ./validate-secrets.sh [env-file]
# Default: .env
# =============================================================================

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Environment file to validate
ENV_FILE=${1:-.env}

# Validation counters
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0
WARNINGS=0

# Common weak passwords to check against
WEAK_PASSWORDS=(
    "password" "123456" "password123" "admin" "letmein"
    "welcome" "monkey" "1234567890" "qwerty" "abc123"
    "Password1" "password1" "123456789" "welcome123"
    "changeme" "secret" "iloveyou" "trustno1" "admin123"
)

# =============================================================================
# Helper Functions
# =============================================================================

print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

print_header() {
    echo ""
    print_message "$BLUE" "========================================="
    print_message "$BLUE" "$1"
    print_message "$BLUE" "========================================="
}

# Check if file exists
check_file() {
    if [ ! -f "$ENV_FILE" ]; then
        print_message "$RED" "Error: File '$ENV_FILE' not found!"
        echo "Usage: $0 [env-file]"
        exit 1
    fi
}

# Load environment variables from file
load_env() {
    set -a
    source "$ENV_FILE"
    set +a
}

# Validate secret length
validate_length() {
    local name=$1
    local value=$2
    local min_length=$3
    local severity=${4:-ERROR}  # ERROR or WARNING

    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))

    if [ -z "$value" ]; then
        print_message "$RED" "✗ [ERROR] ${name} is not set"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        return 1
    fi

    if [ ${#value} -lt $min_length ]; then
        if [ "$severity" == "WARNING" ]; then
            print_message "$YELLOW" "⚠ [WARNING] ${name} is shorter than recommended (${#value} < ${min_length} chars)"
            WARNINGS=$((WARNINGS + 1))
            PASSED_CHECKS=$((PASSED_CHECKS + 1))
        else
            print_message "$RED" "✗ [ERROR] ${name} is too short (${#value} < ${min_length} chars)"
            FAILED_CHECKS=$((FAILED_CHECKS + 1))
        fi
        return 1
    else
        print_message "$GREEN" "✓ [PASS] ${name} length is valid (${#value} chars)"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        return 0
    fi
}

# Validate secret complexity
validate_complexity() {
    local name=$1
    local value=$2

    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))

    local has_upper=false
    local has_lower=false
    local has_digit=false
    local has_special=false

    if [[ "$value" =~ [A-Z] ]]; then has_upper=true; fi
    if [[ "$value" =~ [a-z] ]]; then has_lower=true; fi
    if [[ "$value" =~ [0-9] ]]; then has_digit=true; fi
    if [[ "$value" =~ [^a-zA-Z0-9] ]]; then has_special=true; fi

    if $has_upper && $has_lower && $has_digit && $has_special; then
        print_message "$GREEN" "✓ [PASS] ${name} has good complexity (upper, lower, digit, special)"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        return 0
    else
        print_message "$YELLOW" "⚠ [WARNING] ${name} lacks complexity (needs upper, lower, digit, special)"
        WARNINGS=$((WARNINGS + 1))
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        return 1
    fi
}

# Check for weak passwords
check_weak_password() {
    local name=$1
    local value=$2

    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))

    # Convert to lowercase for comparison
    local lower_value=$(echo "$value" | tr '[:upper:]' '[:lower:]')

    for weak in "${WEAK_PASSWORDS[@]}"; do
        if [[ "$lower_value" == *"$weak"* ]]; then
            print_message "$RED" "✗ [ERROR] ${name} contains weak pattern: '$weak'"
            FAILED_CHECKS=$((FAILED_CHECKS + 1))
            return 1
        fi
    done

    # Check for default/example values
    if [[ "$value" == *"CHANGE"* ]] || [[ "$value" == *"MUST_CHANGE"* ]] || [[ "$value" == *"changeme"* ]]; then
        print_message "$RED" "✗ [ERROR] ${name} contains default/example value"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        return 1
    fi

    print_message "$GREEN" "✓ [PASS] ${name} does not contain weak patterns"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
    return 0
}

# Check for URL-unsafe characters in database passwords
check_url_safe() {
    local name=$1
    local value=$2

    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))

    if [[ "$value" =~ [@#%] ]]; then
        print_message "$YELLOW" "⚠ [WARNING] ${name} contains URL-unsafe characters (@#%), may cause connection issues"
        WARNINGS=$((WARNINGS + 1))
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        return 1
    else
        print_message "$GREEN" "✓ [PASS] ${name} is URL-safe"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        return 0
    fi
}

# Validate base64 encoding
validate_base64() {
    local name=$1
    local value=$2

    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))

    if echo "$value" | base64 -d &>/dev/null; then
        print_message "$GREEN" "✓ [PASS] ${name} is valid base64"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        return 0
    else
        print_message "$YELLOW" "⚠ [WARNING] ${name} is not valid base64 (recommended for JWT)"
        WARNINGS=$((WARNINGS + 1))
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        return 1
    fi
}

# Check SSL/TLS configuration
check_ssl_config() {
    local service=$1
    local enabled_var=$2
    local enabled_value=$3

    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))

    if [[ "$enabled_value" == "true" ]] || [[ "$enabled_value" == "require" ]]; then
        print_message "$GREEN" "✓ [PASS] ${service} SSL/TLS is enabled"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        return 0
    else
        print_message "$YELLOW" "⚠ [WARNING] ${service} SSL/TLS is not enabled (required for production)"
        WARNINGS=$((WARNINGS + 1))
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        return 1
    fi
}

# =============================================================================
# Main Validation
# =============================================================================

print_header "GCRF Library Management System"
print_message "$BLUE" "Secrets Security Validator"
print_message "$YELLOW" "Validating: ${ENV_FILE}"

# Check file exists
check_file

# Load environment variables
print_message "$YELLOW" "Loading environment variables..."
load_env

# =============================================================================
# JWT Validation
# =============================================================================

print_header "JWT Security Validation"

if [ -n "${JWT_SECRET:-}" ]; then
    validate_length "JWT_SECRET" "$JWT_SECRET" 64
    check_weak_password "JWT_SECRET" "$JWT_SECRET"
    validate_base64 "JWT_SECRET" "$JWT_SECRET"
else
    print_message "$RED" "✗ [ERROR] JWT_SECRET is not defined"
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
fi

# =============================================================================
# Database Password Validation
# =============================================================================

print_header "Database Security Validation"

if [ -n "${DB_PASSWORD:-}" ]; then
    validate_length "DB_PASSWORD" "$DB_PASSWORD" 20
    validate_complexity "DB_PASSWORD" "$DB_PASSWORD"
    check_weak_password "DB_PASSWORD" "$DB_PASSWORD"
    check_url_safe "DB_PASSWORD" "$DB_PASSWORD"
else
    print_message "$RED" "✗ [ERROR] DB_PASSWORD is not defined"
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
fi

# Check SSL configuration
check_ssl_config "Database" "DB_SSL_MODE" "${DB_SSL_MODE:-disable}"

# =============================================================================
# Redis Password Validation
# =============================================================================

print_header "Redis Security Validation"

if [ -n "${REDIS_PASSWORD:-}" ]; then
    validate_length "REDIS_PASSWORD" "$REDIS_PASSWORD" 32
    validate_complexity "REDIS_PASSWORD" "$REDIS_PASSWORD"
    check_weak_password "REDIS_PASSWORD" "$REDIS_PASSWORD"
else
    print_message "$YELLOW" "⚠ [WARNING] REDIS_PASSWORD is not defined (required for production)"
    WARNINGS=$((WARNINGS + 1))
fi

# Check SSL configuration
check_ssl_config "Redis" "REDIS_SSL_ENABLED" "${REDIS_SSL_ENABLED:-false}"

# =============================================================================
# Nacos Security Validation
# =============================================================================

print_header "Nacos Security Validation"

if [ -n "${NACOS_PASSWORD:-}" ]; then
    validate_length "NACOS_PASSWORD" "$NACOS_PASSWORD" 16
    check_weak_password "NACOS_PASSWORD" "$NACOS_PASSWORD"
else
    print_message "$YELLOW" "⚠ [WARNING] NACOS_PASSWORD is not defined"
    WARNINGS=$((WARNINGS + 1))
fi

if [ -n "${NACOS_AUTH_SECRET:-}" ]; then
    validate_length "NACOS_AUTH_SECRET" "$NACOS_AUTH_SECRET" 32
    check_weak_password "NACOS_AUTH_SECRET" "$NACOS_AUTH_SECRET"
else
    print_message "$YELLOW" "⚠ [WARNING] NACOS_AUTH_SECRET is not defined"
    WARNINGS=$((WARNINGS + 1))
fi

# Check authentication enabled
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
if [[ "${NACOS_AUTH_ENABLED:-false}" == "true" ]]; then
    print_message "$GREEN" "✓ [PASS] Nacos authentication is enabled"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
else
    print_message "$YELLOW" "⚠ [WARNING] Nacos authentication is disabled (enable for production)"
    WARNINGS=$((WARNINGS + 1))
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
fi

# =============================================================================
# RabbitMQ Security Validation
# =============================================================================

print_header "RabbitMQ Security Validation"

if [ -n "${RABBITMQ_PASSWORD:-}" ]; then
    validate_length "RABBITMQ_PASSWORD" "$RABBITMQ_PASSWORD" 20
    check_weak_password "RABBITMQ_PASSWORD" "$RABBITMQ_PASSWORD"
else
    print_message "$YELLOW" "⚠ [WARNING] RABBITMQ_PASSWORD is not defined"
    WARNINGS=$((WARNINGS + 1))
fi

check_ssl_config "RabbitMQ" "RABBITMQ_SSL_ENABLED" "${RABBITMQ_SSL_ENABLED:-false}"

# =============================================================================
# MinIO Security Validation
# =============================================================================

print_header "MinIO Security Validation"

if [ -n "${MINIO_SECRET_KEY:-}" ]; then
    validate_length "MINIO_SECRET_KEY" "$MINIO_SECRET_KEY" 32
    check_weak_password "MINIO_SECRET_KEY" "$MINIO_SECRET_KEY"
else
    print_message "$YELLOW" "⚠ [WARNING] MINIO_SECRET_KEY is not defined"
    WARNINGS=$((WARNINGS + 1))
fi

check_ssl_config "MinIO" "MINIO_SECURE" "${MINIO_SECURE:-false}"

# =============================================================================
# Monitoring Security Validation
# =============================================================================

print_header "Monitoring Security Validation"

if [ -n "${ACTUATOR_PASSWORD:-}" ]; then
    validate_length "ACTUATOR_PASSWORD" "$ACTUATOR_PASSWORD" 16
    check_weak_password "ACTUATOR_PASSWORD" "$ACTUATOR_PASSWORD"
else
    print_message "$YELLOW" "⚠ [WARNING] ACTUATOR_PASSWORD is not defined"
    WARNINGS=$((WARNINGS + 1))
fi

# =============================================================================
# Security Features Validation
# =============================================================================

print_header "Security Features Validation"

# Check CORS configuration
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
if [ -n "${CORS_ALLOWED_ORIGINS:-}" ]; then
    if [[ "$CORS_ALLOWED_ORIGINS" == "*" ]]; then
        print_message "$RED" "✗ [ERROR] CORS allows all origins (*) - security risk!"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    else
        print_message "$GREEN" "✓ [PASS] CORS origins are restricted"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
    fi
else
    print_message "$YELLOW" "⚠ [WARNING] CORS_ALLOWED_ORIGINS not configured"
    WARNINGS=$((WARNINGS + 1))
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
fi

# Check rate limiting
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
if [[ "${RATE_LIMIT_ENABLED:-false}" == "true" ]]; then
    print_message "$GREEN" "✓ [PASS] Rate limiting is enabled"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
else
    print_message "$YELLOW" "⚠ [WARNING] Rate limiting is disabled"
    WARNINGS=$((WARNINGS + 1))
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
fi

# Check audit logging
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
if [[ "${AUDIT_ENABLED:-false}" == "true" ]]; then
    print_message "$GREEN" "✓ [PASS] Audit logging is enabled"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
else
    print_message "$YELLOW" "⚠ [WARNING] Audit logging is disabled"
    WARNINGS=$((WARNINGS + 1))
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
fi

# Check debug mode
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
if [[ "${DEBUG_MODE:-false}" == "false" ]]; then
    print_message "$GREEN" "✓ [PASS] Debug mode is disabled"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
else
    print_message "$RED" "✗ [ERROR] Debug mode is enabled (disable for production)"
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
fi

# Check Swagger
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
if [[ "${SWAGGER_ENABLED:-false}" == "false" ]]; then
    print_message "$GREEN" "✓ [PASS] Swagger is disabled"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
else
    print_message "$YELLOW" "⚠ [WARNING] Swagger is enabled (disable for production)"
    WARNINGS=$((WARNINGS + 1))
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
fi

# =============================================================================
# Summary Report
# =============================================================================

print_header "Validation Summary"

# Calculate percentages
if [ $TOTAL_CHECKS -gt 0 ]; then
    PASS_PERCENTAGE=$((PASSED_CHECKS * 100 / TOTAL_CHECKS))
else
    PASS_PERCENTAGE=0
fi

print_message "$BLUE" "Total Checks: ${TOTAL_CHECKS}"
print_message "$GREEN" "Passed: ${PASSED_CHECKS}"
print_message "$RED" "Failed: ${FAILED_CHECKS}"
print_message "$YELLOW" "Warnings: ${WARNINGS}"
print_message "$BLUE" "Pass Rate: ${PASS_PERCENTAGE}%"

echo ""

# Overall result
if [ $FAILED_CHECKS -eq 0 ]; then
    if [ $WARNINGS -eq 0 ]; then
        print_message "$GREEN" "✓ EXCELLENT: All security checks passed!"
        print_message "$GREEN" "Your secrets configuration is production-ready."
        exit 0
    else
        print_message "$YELLOW" "⚠ GOOD: No critical failures, but ${WARNINGS} warnings found."
        print_message "$YELLOW" "Review warnings before deploying to production."
        exit 0
    fi
else
    print_message "$RED" "✗ FAILED: ${FAILED_CHECKS} critical security issues found!"
    print_message "$RED" "Fix all errors before deploying."
    echo ""
    print_message "$YELLOW" "Recommendations:"
    print_message "$YELLOW" "1. Generate new secrets using: ./generate-secrets.sh"
    print_message "$YELLOW" "2. Enable SSL/TLS for all services in production"
    print_message "$YELLOW" "3. Use strong, unique passwords for each service"
    print_message "$YELLOW" "4. Enable authentication for all services"
    print_message "$YELLOW" "5. Restrict CORS origins to specific domains"
    exit 1
fi