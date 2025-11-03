#!/bin/bash

# GCRF Library Management System - Service Discovery Test Script
# Tests Nacos service discovery and configuration management
# Version: 1.0.0

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Load environment variables
if [ -f "$PROJECT_ROOT/deployment/.env.infrastructure" ]; then
    set -a
    source "$PROJECT_ROOT/deployment/.env.infrastructure"
    set +a
fi

# Configuration
NACOS_HOST="${NACOS_SERVER_ADDR:-localhost:8848}"
NACOS_USERNAME="${NACOS_USERNAME:-nacos}"
NACOS_PASSWORD="${NACOS_PASSWORD:-nacos}"
NACOS_NAMESPACE="${NACOS_NAMESPACE:-}"
MAX_RETRIES=5
RETRY_DELAY=2

# Service list for testing
SERVICES=(
    "gateway-service"
    "auth-service"
    "book-service"
    "reader-service"
    "circulation-service"
    "system-service"
    "notification-service"
)

# Shared configuration files
SHARED_CONFIGS=(
    "application-shared.yml"
    "jwt-shared.yml"
    "redis-shared.yml"
    "database-shared.yml"
)

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[⚠]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

# Function to check if Nacos is accessible
check_nacos_accessibility() {
    print_info "Checking Nacos accessibility at http://$NACOS_HOST..."

    for i in $(seq 1 $MAX_RETRIES); do
        if curl -s -f "http://$NACOS_HOST/nacos/" > /dev/null 2>&1; then
            print_success "Nacos is accessible"
            return 0
        fi

        if [ $i -lt $MAX_RETRIES ]; then
            print_warning "Nacos not accessible, retrying in ${RETRY_DELAY}s... (Attempt $i/$MAX_RETRIES)"
            sleep $RETRY_DELAY
        fi
    done

    print_error "Nacos is not accessible at http://$NACOS_HOST"
    return 1
}

# Function to authenticate with Nacos
authenticate_nacos() {
    print_info "Authenticating with Nacos..."

    # Try to get access token
    response=$(curl -s -X POST "http://$NACOS_HOST/nacos/v1/auth/login" \
        -d "username=$NACOS_USERNAME&password=$NACOS_PASSWORD" 2>/dev/null || echo "{}")

    TOKEN=$(echo "$response" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

    if [ -z "$TOKEN" ]; then
        # Try without authentication (for non-secured Nacos)
        print_warning "Authentication not required or failed, proceeding without token"
        TOKEN=""
        return 0
    else
        print_success "Authentication successful"
        export NACOS_TOKEN="$TOKEN"
        return 0
    fi
}

# Function to list registered services
list_registered_services() {
    print_info "Listing registered services..."
    echo
    echo "┌─────────────────────────┬───────────┬────────────────────┐"
    echo "│ Service Name            │ Instances │ Status             │"
    echo "├─────────────────────────┼───────────┼────────────────────┤"

    local total_services=0
    local healthy_services=0

    for service in "${SERVICES[@]}"; do
        local auth_param=""
        if [ -n "$TOKEN" ]; then
            auth_param="&accessToken=$TOKEN"
        fi

        # Get service instances
        response=$(curl -s "http://$NACOS_HOST/nacos/v1/ns/instance/list?serviceName=${service}${auth_param}" 2>/dev/null || echo "{}")

        # Parse instance count
        instance_count=$(echo "$response" | grep -o '"hosts":\[[^]]*\]' | grep -o '"instanceId"' | wc -l | tr -d ' ')

        # Parse health status
        healthy_count=$(echo "$response" | grep -o '"healthy":true' | wc -l | tr -d ' ')

        # Format service name (pad to 23 chars)
        formatted_service=$(printf "%-23s" "$service")

        # Format instance count (pad to 9 chars)
        formatted_count=$(printf "%-9s" "$instance_count")

        # Determine status
        if [ "$instance_count" -eq "0" ]; then
            status="Not Registered     "
            status_color="${YELLOW}"
        elif [ "$healthy_count" -eq "$instance_count" ] && [ "$instance_count" -gt "0" ]; then
            status="Healthy           "
            status_color="${GREEN}"
            ((healthy_services++))
        elif [ "$healthy_count" -gt "0" ]; then
            status="Partial ($healthy_count/$instance_count)     "
            status_color="${YELLOW}"
        else
            status="Unhealthy         "
            status_color="${RED}"
        fi

        if [ "$instance_count" -gt "0" ]; then
            ((total_services++))
        fi

        echo -e "│ $formatted_service │ $formatted_count │ ${status_color}${status}${NC} │"
    done

    echo "└─────────────────────────┴───────────┴────────────────────┘"
    echo
    print_info "Summary: $total_services/${#SERVICES[@]} services registered, $healthy_services healthy"
}

# Function to check service health
check_service_health() {
    local service=$1
    print_info "Checking health for $service..."

    local auth_param=""
    if [ -n "$TOKEN" ]; then
        auth_param="&accessToken=$TOKEN"
    fi

    response=$(curl -s "http://$NACOS_HOST/nacos/v1/ns/instance/list?serviceName=${service}${auth_param}" 2>/dev/null || echo "{}")

    # Parse instances
    instances=$(echo "$response" | grep -o '"instanceId":"[^"]*"' | cut -d'"' -f4)

    if [ -z "$instances" ]; then
        print_warning "No instances found for $service"
        return 1
    fi

    echo "  Instances:"
    for instance in $instances; do
        # Extract instance details from response
        ip=$(echo "$response" | grep -A5 "\"instanceId\":\"$instance\"" | grep -o '"ip":"[^"]*"' | head -1 | cut -d'"' -f4)
        port=$(echo "$response" | grep -A5 "\"instanceId\":\"$instance\"" | grep -o '"port":[0-9]*' | head -1 | cut -d':' -f2)
        healthy=$(echo "$response" | grep -A5 "\"instanceId\":\"$instance\"" | grep -o '"healthy":[^,}]*' | head -1 | cut -d':' -f2)

        if [ "$healthy" == "true" ]; then
            health_status="${GREEN}Healthy${NC}"
        else
            health_status="${RED}Unhealthy${NC}"
        fi

        echo -e "    - $ip:$port [$health_status]"
    done
}

# Function to check configuration management
check_configuration_management() {
    print_info "Checking configuration management..."
    echo

    local configs_found=0

    for config in "${SHARED_CONFIGS[@]}"; do
        local auth_param=""
        if [ -n "$TOKEN" ]; then
            auth_param="?accessToken=$TOKEN"
        fi

        # Check if configuration exists
        response=$(curl -s "http://$NACOS_HOST/nacos/v1/cs/configs${auth_param}&dataId=${config}&group=DEFAULT_GROUP" 2>/dev/null)

        if [ -n "$response" ] && [ "$response" != "config data not exist" ]; then
            print_success "Configuration found: $config"
            ((configs_found++))

            # Show first 3 lines of config
            echo "    Preview:"
            echo "$response" | head -3 | sed 's/^/      /'
            echo "      ..."
        else
            print_warning "Configuration not found: $config"
        fi
    done

    echo
    print_info "Summary: $configs_found/${#SHARED_CONFIGS[@]} shared configurations found"
}

# Function to test service discovery
test_service_discovery() {
    local service=$1
    print_info "Testing service discovery for $service..."

    # Try to resolve service through Nacos
    local auth_param=""
    if [ -n "$TOKEN" ]; then
        auth_param="&accessToken=$TOKEN"
    fi

    response=$(curl -s "http://$NACOS_HOST/nacos/v1/ns/instance/list?serviceName=${service}${auth_param}" 2>/dev/null || echo "{}")

    instances=$(echo "$response" | grep -o '"instanceId"' | wc -l | tr -d ' ')

    if [ "$instances" -gt "0" ]; then
        print_success "Service discovery working: $instances instance(s) found"

        # Extract and display endpoint information
        ips=$(echo "$response" | grep -o '"ip":"[^"]*"' | cut -d'"' -f4 | head -3)
        ports=$(echo "$response" | grep -o '"port":[0-9]*' | cut -d':' -f2 | head -3)

        echo "  Available endpoints:"
        paste <(echo "$ips") <(echo "$ports") | while IFS=$'\t' read -r ip port; do
            echo "    - http://$ip:$port"
        done
    else
        print_warning "No instances found for $service"
    fi
}

# Function to generate detailed report
generate_report() {
    local report_file="$PROJECT_ROOT/deployment/service-discovery-report-$(date +%Y%m%d-%H%M%S).txt"

    {
        echo "======================================"
        echo "GCRF Service Discovery Test Report"
        echo "======================================"
        echo
        echo "Test Date: $(date)"
        echo "Nacos Server: http://$NACOS_HOST"
        echo "Namespace: ${NACOS_NAMESPACE:-default}"
        echo
        echo "Service Registration Status:"
        echo "----------------------------"

        for service in "${SERVICES[@]}"; do
            local auth_param=""
            if [ -n "$TOKEN" ]; then
                auth_param="&accessToken=$TOKEN"
            fi

            response=$(curl -s "http://$NACOS_HOST/nacos/v1/ns/instance/list?serviceName=${service}${auth_param}" 2>/dev/null || echo "{}")
            instance_count=$(echo "$response" | grep -o '"instanceId"' | wc -l | tr -d ' ')

            echo "- $service: $instance_count instance(s)"
        done

        echo
        echo "Configuration Status:"
        echo "--------------------"

        for config in "${SHARED_CONFIGS[@]}"; do
            local auth_param=""
            if [ -n "$TOKEN" ]; then
                auth_param="?accessToken=$TOKEN"
            fi

            response=$(curl -s "http://$NACOS_HOST/nacos/v1/cs/configs${auth_param}&dataId=${config}&group=DEFAULT_GROUP" 2>/dev/null)

            if [ -n "$response" ] && [ "$response" != "config data not exist" ]; then
                echo "- $config: Present"
            else
                echo "- $config: Not Found"
            fi
        done

        echo
        echo "======================================"
    } > "$report_file"

    print_success "Report generated: $report_file"
}

# Main execution
main() {
    echo "========================================="
    echo "   GCRF Service Discovery Test Suite    "
    echo "========================================="
    echo

    # Step 1: Check Nacos accessibility
    if ! check_nacos_accessibility; then
        print_error "Cannot proceed without Nacos access"
        exit 1
    fi
    echo

    # Step 2: Authenticate
    authenticate_nacos
    echo

    # Step 3: List registered services
    list_registered_services
    echo

    # Step 4: Check configuration management
    check_configuration_management
    echo

    # Step 5: Test specific service discovery
    if [ -n "${1:-}" ]; then
        test_service_discovery "$1"
        check_service_health "$1"
    else
        # Test gateway service by default
        test_service_discovery "gateway-service"
    fi
    echo

    # Step 6: Generate report
    generate_report
    echo

    print_success "Service discovery test completed successfully!"
    echo
    echo "Next steps:"
    echo "  1. Start services to register with Nacos"
    echo "  2. Push shared configurations using push-nacos-configs.sh"
    echo "  3. Monitor service health in Nacos dashboard: http://$NACOS_HOST/nacos/"
}

# Check for help flag
if [ "${1:-}" == "-h" ] || [ "${1:-}" == "--help" ]; then
    echo "Usage: $0 [service-name]"
    echo
    echo "Test Nacos service discovery and configuration management"
    echo
    echo "Arguments:"
    echo "  service-name    Optional specific service to test (default: gateway-service)"
    echo
    echo "Environment variables:"
    echo "  NACOS_SERVER_ADDR    Nacos server address (default: localhost:8848)"
    echo "  NACOS_USERNAME       Nacos username (default: nacos)"
    echo "  NACOS_PASSWORD       Nacos password (default: nacos)"
    echo "  NACOS_NAMESPACE      Nacos namespace (default: empty/public)"
    exit 0
fi

# Run main function
main "$@"