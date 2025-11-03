#!/bin/bash

# Environment Variable Validation Script for GCRF Library Management System
# Usage: ./validate-env.sh [environment]
# Example: ./validate-env.sh prod

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script configuration
ENVIRONMENT=${1:-dev}
ENV_FILE=".env.${ENVIRONMENT}"
ERRORS=0
WARNINGS=0

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
    ((ERRORS++))
}

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
    ((WARNINGS++))
}

print_header() {
    echo ""
    echo "=========================================="
    echo "$1"
    echo "=========================================="
}

# Function to check if variable exists and is not empty
check_required() {
    local var_name=$1
    local var_value="${!var_name}"

    if [[ -z "$var_value" ]]; then
        print_error "$var_name is not set or empty"
        return 1
    else
        print_success "$var_name is set"
        return 0
    fi
}

# Function to check if variable matches a pattern
check_pattern() {
    local var_name=$1
    local pattern=$2
    local var_value="${!var_name}"

    if [[ ! "$var_value" =~ $pattern ]]; then
        print_error "$var_name does not match pattern: $pattern (value: $var_value)"
        return 1
    else
        print_success "$var_name matches pattern"
        return 0
    fi
}

# Function to check if port is valid
check_port() {
    local var_name=$1
    local var_value="${!var_name}"

    if [[ ! "$var_value" =~ ^[0-9]+$ ]] || [[ "$var_value" -lt 1 ]] || [[ "$var_value" -gt 65535 ]]; then
        print_error "$var_name is not a valid port number (value: $var_value)"
        return 1
    else
        print_success "$var_name is a valid port"
        return 0
    fi
}

# Function to check database connection
check_database() {
    local host=$1
    local port=$2
    local database=$3
    local username=$4
    local password=$5

    if command -v pg_isready &> /dev/null; then
        if PGPASSWORD="$password" pg_isready -h "$host" -p "$port" -U "$username" -d "$database" &> /dev/null; then
            print_success "Database connection successful"
            return 0
        else
            print_error "Cannot connect to database at $host:$port"
            return 1
        fi
    else
        print_warning "pg_isready not found, skipping database connectivity check"
        return 0
    fi
}

# Function to check Redis connection
check_redis() {
    local host=$1
    local port=$2
    local password=$3

    if command -v redis-cli &> /dev/null; then
        if [[ -n "$password" ]]; then
            response=$(redis-cli -h "$host" -p "$port" -a "$password" --no-auth-warning ping 2>/dev/null)
        else
            response=$(redis-cli -h "$host" -p "$port" ping 2>/dev/null)
        fi

        if [[ "$response" == "PONG" ]]; then
            print_success "Redis connection successful"
            return 0
        else
            print_error "Cannot connect to Redis at $host:$port"
            return 1
        fi
    else
        print_warning "redis-cli not found, skipping Redis connectivity check"
        return 0
    fi
}

# Function to check Nacos connection
check_nacos() {
    local server_addr=$1
    local username=$2
    local password=$3

    if command -v curl &> /dev/null; then
        response=$(curl -s -o /dev/null -w "%{http_code}" "http://$server_addr/nacos/v1/console/health/liveness")

        if [[ "$response" == "200" ]]; then
            print_success "Nacos is accessible"
            return 0
        else
            print_error "Cannot connect to Nacos at $server_addr (HTTP $response)"
            return 1
        fi
    else
        print_warning "curl not found, skipping Nacos connectivity check"
        return 0
    fi
}

# Function to validate JWT secret strength
check_jwt_secret() {
    local secret=$1
    local min_length=32

    if [[ ${#secret} -lt $min_length ]]; then
        print_error "JWT_SECRET is too short (minimum $min_length characters)"
        return 1
    fi

    if [[ "$secret" == "your-256-bit-secret-key-change-this-in-production" ]]; then
        print_error "JWT_SECRET is using the default value - MUST be changed for production"
        return 1
    fi

    print_success "JWT_SECRET meets security requirements"
    return 0
}

# Main validation script
main() {
    print_header "GCRF Library Management System - Environment Validation"
    print_info "Environment: ${ENVIRONMENT}"
    print_info "Configuration file: ${ENV_FILE}"

    # Check if environment file exists
    if [[ ! -f "$ENV_FILE" ]]; then
        print_error "Environment file ${ENV_FILE} not found"
        exit 1
    fi

    # Source the environment file
    set -a
    source "$ENV_FILE"
    set +a

    print_info "Environment file loaded successfully"

    # ============================================
    # REQUIRED VARIABLES VALIDATION
    # ============================================

    print_header "Validating Required Variables"

    # Global Configuration
    check_required "ENVIRONMENT"
    check_required "SPRING_PROFILES_ACTIVE"
    check_pattern "ENVIRONMENT" "^(dev|staging|prod)$"

    # Nacos Configuration
    check_required "NACOS_SERVER_ADDR"
    check_pattern "NACOS_SERVER_ADDR" "^[a-zA-Z0-9.-]+:[0-9]+$"

    # Database Configuration
    check_required "DB_HOST"
    check_required "DB_PORT"
    check_required "DB_USERNAME"
    check_required "DB_PASSWORD"
    check_port "DB_PORT"

    # Redis Configuration
    check_required "REDIS_HOST"
    check_required "REDIS_PORT"
    check_port "REDIS_PORT"

    # JWT Configuration
    check_required "JWT_SECRET"
    check_jwt_secret "$JWT_SECRET"

    # Service Ports
    check_required "GATEWAY_SERVER_PORT"
    check_required "AUTH_SERVER_PORT"
    check_port "GATEWAY_SERVER_PORT"
    check_port "AUTH_SERVER_PORT"

    # ============================================
    # CONNECTIVITY CHECKS
    # ============================================

    print_header "Performing Connectivity Checks"

    # Database connectivity
    print_info "Checking PostgreSQL connectivity..."
    check_database "$DB_HOST" "$DB_PORT" "${AUTH_DB_NAME:-gcrf_auth}" "$DB_USERNAME" "$DB_PASSWORD"

    # Redis connectivity
    print_info "Checking Redis connectivity..."
    check_redis "$REDIS_HOST" "$REDIS_PORT" "$REDIS_PASSWORD"

    # Nacos connectivity
    print_info "Checking Nacos connectivity..."
    check_nacos "$NACOS_SERVER_ADDR" "$NACOS_USERNAME" "$NACOS_PASSWORD"

    # ============================================
    # SECURITY VALIDATION
    # ============================================

    print_header "Validating Security Configuration"

    # Check for production-specific requirements
    if [[ "$ENVIRONMENT" == "prod" ]]; then
        print_info "Production environment detected - applying strict validation"

        # Check for default passwords
        if [[ "$DB_PASSWORD" == "postgres123" ]]; then
            print_error "Using default database password in production"
        fi

        if [[ "$NACOS_PASSWORD" == "nacos" ]]; then
            print_error "Using default Nacos password in production"
        fi

        # Check SSL mode
        if [[ "$DB_SSL_MODE" == "disable" ]]; then
            print_warning "Database SSL is disabled in production"
        fi

        # Check CORS origins
        if [[ "$CORS_ALLOWED_ORIGINS" == *"localhost"* ]]; then
            print_warning "CORS allows localhost origins in production"
        fi

        # Check debug/trace logging
        if [[ "$LOG_LEVEL" == "DEBUG" ]] || [[ "$LOG_LEVEL" == "TRACE" ]]; then
            print_warning "Debug/Trace logging enabled in production"
        fi
    fi

    # ============================================
    # OPTIONAL CONFIGURATION VALIDATION
    # ============================================

    print_header "Checking Optional Configuration"

    # Check if read replica is configured
    if [[ -n "$DB_READ_REPLICA_HOST" ]]; then
        print_info "Read replica configured at $DB_READ_REPLICA_HOST:$DB_READ_REPLICA_PORT"
        check_database "$DB_READ_REPLICA_HOST" "$DB_READ_REPLICA_PORT" "${AUTH_DB_NAME:-gcrf_auth}" "$DB_USERNAME" "$DB_PASSWORD"
    fi

    # Check Redis Sentinel configuration
    if [[ "$REDIS_MODE" == "sentinel" ]]; then
        if [[ -z "$REDIS_SENTINEL_NODES" ]] || [[ -z "$REDIS_SENTINEL_MASTER" ]]; then
            print_error "Redis Sentinel mode enabled but REDIS_SENTINEL_NODES or REDIS_SENTINEL_MASTER not set"
        else
            print_success "Redis Sentinel configuration present"
        fi
    fi

    # Check email configuration if notifications enabled
    if [[ -n "$NOTIFICATION_SMTP_HOST" ]]; then
        print_info "Email notifications configured"
        check_required "NOTIFICATION_SMTP_PORT"
        check_required "NOTIFICATION_SMTP_USERNAME"
        check_port "NOTIFICATION_SMTP_PORT"
    fi

    # ============================================
    # SUMMARY
    # ============================================

    print_header "Validation Summary"

    if [[ $ERRORS -eq 0 ]]; then
        if [[ $WARNINGS -eq 0 ]]; then
            print_success "✨ All validation checks passed successfully!"
            print_info "Environment is ready for deployment"
        else
            print_warning "Validation completed with $WARNINGS warning(s)"
            print_info "Review warnings before proceeding to production"
        fi
        exit 0
    else
        print_error "Validation failed with $ERRORS error(s) and $WARNINGS warning(s)"
        print_info "Please fix the errors before deploying"
        exit 1
    fi
}

# Run main function
main "$@"