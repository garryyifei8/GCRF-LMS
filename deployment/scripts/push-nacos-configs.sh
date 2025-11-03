#!/bin/bash

# GCRF Library Management System - Nacos Configuration Push Script
# Pushes shared configurations to Nacos for all environments
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
CONFIG_DIR="$PROJECT_ROOT/deployment/nacos/configs"

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
NACOS_GROUP="${NACOS_GROUP:-DEFAULT_GROUP}"
DRY_RUN="${DRY_RUN:-false}"

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

    if ! curl -s -f "http://$NACOS_HOST/nacos/" > /dev/null 2>&1; then
        print_error "Nacos is not accessible at http://$NACOS_HOST"
        print_info "Please ensure Nacos is running: docker-compose -f docker-compose.infrastructure.yml up -d nacos"
        return 1
    fi

    print_success "Nacos is accessible"
    return 0
}

# Function to authenticate with Nacos
authenticate_nacos() {
    print_info "Authenticating with Nacos..."

    # Try to get access token
    response=$(curl -s -X POST "http://$NACOS_HOST/nacos/v1/auth/login" \
        -d "username=$NACOS_USERNAME&password=$NACOS_PASSWORD" 2>/dev/null || echo "{}")

    TOKEN=$(echo "$response" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

    if [ -z "$TOKEN" ]; then
        print_warning "Authentication not required or using default credentials"
        TOKEN=""
        return 0
    else
        print_success "Authentication successful"
        export NACOS_TOKEN="$TOKEN"
        return 0
    fi
}

# Function to create namespace if not exists
create_namespace() {
    local namespace=$1
    local namespace_name=$2

    if [ -z "$namespace" ] || [ "$namespace" == "public" ]; then
        print_info "Using public namespace"
        return 0
    fi

    print_info "Checking namespace: $namespace"

    local auth_param=""
    if [ -n "$TOKEN" ]; then
        auth_param="&accessToken=$TOKEN"
    fi

    # Check if namespace exists
    namespaces=$(curl -s "http://$NACOS_HOST/nacos/v1/console/namespaces${auth_param:+?${auth_param:1}}" 2>/dev/null || echo "[]")

    if echo "$namespaces" | grep -q "\"namespace\":\"$namespace\""; then
        print_success "Namespace '$namespace' already exists"
    else
        print_info "Creating namespace: $namespace"

        curl -s -X POST "http://$NACOS_HOST/nacos/v1/console/namespaces" \
            -d "customNamespaceId=$namespace" \
            -d "namespaceName=$namespace_name" \
            -d "namespaceDesc=GCRF Library Management System - $namespace_name environment" \
            ${auth_param:+-d "${auth_param:1}"} > /dev/null

        print_success "Namespace '$namespace' created"
    fi
}

# Function to push configuration to Nacos
push_config() {
    local data_id=$1
    local content=$2
    local group=${3:-DEFAULT_GROUP}
    local type=${4:-yaml}
    local namespace=${5:-}

    if [ "$DRY_RUN" == "true" ]; then
        print_info "[DRY RUN] Would push: $data_id to namespace: ${namespace:-public}"
        return 0
    fi

    print_info "Pushing configuration: $data_id"

    # Prepare parameters
    local params="dataId=$data_id&group=$group&type=$type"

    if [ -n "$namespace" ]; then
        params="$params&tenant=$namespace"
    fi

    if [ -n "$TOKEN" ]; then
        params="$params&accessToken=$TOKEN"
    fi

    # URL encode content
    encoded_content=$(echo -n "$content" | jq -Rs '.')
    encoded_content=${encoded_content:1:-1}  # Remove quotes

    # Push configuration
    response=$(curl -s -X POST "http://$NACOS_HOST/nacos/v1/cs/configs" \
        -H "Content-Type: application/x-www-form-urlencoded; charset=UTF-8" \
        --data-urlencode "content=$content" \
        -d "$params" 2>/dev/null)

    if [ "$response" == "true" ]; then
        print_success "Configuration pushed: $data_id"
        return 0
    else
        print_error "Failed to push configuration: $data_id"
        echo "  Response: $response"
        return 1
    fi
}

# Function to create shared configurations
create_shared_configs() {
    print_info "Creating shared configuration files..."

    # Ensure config directory exists
    mkdir -p "$CONFIG_DIR"

    # 1. Application shared configuration
    cat > "$CONFIG_DIR/application-shared.yml" << 'EOF'
# GCRF Library Management System - Shared Configuration
# Common settings for all microservices

spring:
  # Jackson configuration
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false

  # Servlet configuration
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

  # Actuator configuration
  management:
    endpoints:
      web:
        exposure:
          include: health,info,metrics
    endpoint:
      health:
        show-details: when-authorized

# Logging configuration
logging:
  level:
    com.gcrf.library: DEBUG
    org.springframework.cloud.gateway: DEBUG
    com.alibaba.nacos: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    path: /var/log/gcrf-library

# Feign configuration
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 5000
        loggerLevel: basic
  hystrix:
    enabled: true

# Hystrix configuration
hystrix:
  command:
    default:
      execution:
        isolation:
          thread:
            timeoutInMilliseconds: 10000
EOF

    # 2. JWT shared configuration
    cat > "$CONFIG_DIR/jwt-shared.yml" << EOF
# JWT Configuration
jwt:
  secret: ${JWT_SECRET:-gcrf-library-jwt-secret-2024-production}
  expiration: ${JWT_EXPIRATION:-86400000}  # 24 hours
  refresh-expiration: ${JWT_REFRESH_EXPIRATION:-604800000}  # 7 days
  header: Authorization
  prefix: "Bearer "
  issuer: gcrf-library-system

  # Token validation
  validation:
    issuer-uri: http://auth-service:8081
    audience: gcrf-library
    clock-skew-seconds: 60
EOF

    # 3. Redis shared configuration
    cat > "$CONFIG_DIR/redis-shared.yml" << EOF
# Redis Configuration
spring:
  data:
    redis:
      host: ${REDIS_HOST:-redis}
      port: ${REDIS_PORT:-6379}
      password: ${REDIS_PASSWORD:-gcrf_redis_2024}
      database: ${REDIS_DATABASE:-0}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
        cluster:
          refresh:
            period: 60s

  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1 hour
      cache-null-values: false
      use-key-prefix: true
      key-prefix: "gcrf:"
EOF

    # 4. Database shared configuration
    cat > "$CONFIG_DIR/database-shared.yml" << EOF
# Database Configuration
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 30000
      pool-name: HikariPool-GCRF
      max-lifetime: 1800000
      connection-timeout: 30000
      connection-test-query: SELECT 1
      leak-detection-threshold: 60000

  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20
          fetch_size: 50
        order_inserts: true
        order_updates: true

# MyBatis Plus Configuration
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
EOF

    # 5. RabbitMQ shared configuration
    cat > "$CONFIG_DIR/rabbitmq-shared.yml" << EOF
# RabbitMQ Configuration
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:-rabbitmq}
    port: ${RABBITMQ_PORT:-5672}
    username: ${RABBITMQ_USERNAME:-admin}
    password: ${RABBITMQ_PASSWORD:-gcrf_rabbitmq_2024}
    virtual-host: ${RABBITMQ_VHOST:-/}

    # Connection settings
    connection-timeout: 15000
    requested-heartbeat: 30
    publisher-confirms: true
    publisher-returns: true

    # Template settings
    template:
      mandatory: true
      receive-timeout: 10000
      reply-timeout: 10000

    # Listener settings
    listener:
      simple:
        acknowledge-mode: manual
        concurrency: 1
        max-concurrency: 10
        prefetch: 1
        retry:
          enabled: true
          initial-interval: 1000
          max-attempts: 3
          max-interval: 10000
          multiplier: 2
EOF

    # 6. Security shared configuration
    cat > "$CONFIG_DIR/security-shared.yml" << EOF
# Security Configuration
security:
  # CORS settings
  cors:
    allowed-origins:
      - "http://localhost:3011"
      - "http://localhost:8080"
      - "${FRONTEND_URL:-http://localhost:3011}"
    allowed-methods:
      - GET
      - POST
      - PUT
      - DELETE
      - OPTIONS
    allowed-headers:
      - "*"
    exposed-headers:
      - Authorization
      - X-Total-Count
    allow-credentials: true
    max-age: 3600

  # Rate limiting
  rate-limit:
    enabled: true
    default-limit: 100
    default-duration: 60  # seconds

  # API endpoints that don't require authentication
  public-endpoints:
    - /api/v1/auth/login
    - /api/v1/auth/register
    - /api/v1/health
    - /actuator/health
    - /v3/api-docs/**
    - /swagger-ui/**
EOF

    print_success "Configuration files created in $CONFIG_DIR"
}

# Function to push all configurations
push_all_configs() {
    local namespace=$1
    local env_name=$2

    print_info "Pushing configurations to namespace: ${namespace:-public} ($env_name)"

    # Create namespace if needed
    if [ -n "$namespace" ] && [ "$namespace" != "public" ]; then
        create_namespace "$namespace" "$env_name"
    fi

    # Push each configuration file
    local configs=(
        "application-shared.yml"
        "jwt-shared.yml"
        "redis-shared.yml"
        "database-shared.yml"
        "rabbitmq-shared.yml"
        "security-shared.yml"
    )

    local success_count=0
    local total_count=${#configs[@]}

    for config in "${configs[@]}"; do
        if [ -f "$CONFIG_DIR/$config" ]; then
            content=$(cat "$CONFIG_DIR/$config")
            if push_config "$config" "$content" "$NACOS_GROUP" "yaml" "$namespace"; then
                ((success_count++))
            fi
        else
            print_warning "Configuration file not found: $CONFIG_DIR/$config"
        fi
    done

    echo
    print_info "Summary: $success_count/$total_count configurations pushed successfully"
}

# Function to verify configurations
verify_configs() {
    local namespace=$1

    print_info "Verifying configurations in namespace: ${namespace:-public}"

    local auth_param=""
    if [ -n "$TOKEN" ]; then
        auth_param="&accessToken=$TOKEN"
    fi

    local namespace_param=""
    if [ -n "$namespace" ] && [ "$namespace" != "public" ]; then
        namespace_param="&tenant=$namespace"
    fi

    local configs=(
        "application-shared.yml"
        "jwt-shared.yml"
        "redis-shared.yml"
        "database-shared.yml"
        "rabbitmq-shared.yml"
        "security-shared.yml"
    )

    echo
    echo "Configuration Status:"
    echo "--------------------"

    for config in "${configs[@]}"; do
        response=$(curl -s "http://$NACOS_HOST/nacos/v1/cs/configs?dataId=${config}&group=${NACOS_GROUP}${namespace_param}${auth_param}" 2>/dev/null)

        if [ -n "$response" ] && [ "$response" != "config data not exist" ]; then
            # Get config size
            size=${#response}
            echo -e "  ${GREEN}✓${NC} $config (${size} bytes)"
        else
            echo -e "  ${RED}✗${NC} $config (not found)"
        fi
    done
    echo
}

# Function to backup existing configurations
backup_configs() {
    local namespace=$1
    local backup_dir="$PROJECT_ROOT/deployment/nacos/backups/$(date +%Y%m%d-%H%M%S)"

    print_info "Backing up existing configurations to $backup_dir"

    mkdir -p "$backup_dir"

    local auth_param=""
    if [ -n "$TOKEN" ]; then
        auth_param="&accessToken=$TOKEN"
    fi

    local namespace_param=""
    if [ -n "$namespace" ] && [ "$namespace" != "public" ]; then
        namespace_param="&tenant=$namespace"
    fi

    local configs=(
        "application-shared.yml"
        "jwt-shared.yml"
        "redis-shared.yml"
        "database-shared.yml"
        "rabbitmq-shared.yml"
        "security-shared.yml"
    )

    for config in "${configs[@]}"; do
        response=$(curl -s "http://$NACOS_HOST/nacos/v1/cs/configs?dataId=${config}&group=${NACOS_GROUP}${namespace_param}${auth_param}" 2>/dev/null)

        if [ -n "$response" ] && [ "$response" != "config data not exist" ]; then
            echo "$response" > "$backup_dir/$config"
            print_success "Backed up: $config"
        fi
    done

    if [ "$(ls -A $backup_dir)" ]; then
        print_success "Configurations backed up to: $backup_dir"
    else
        print_info "No existing configurations to backup"
        rm -rf "$backup_dir"
    fi
}

# Main execution
main() {
    echo "============================================"
    echo "   GCRF Nacos Configuration Push Script    "
    echo "============================================"
    echo
    echo "Target: http://$NACOS_HOST"
    echo "Namespace: ${NACOS_NAMESPACE:-public}"
    echo "Group: $NACOS_GROUP"
    echo

    # Parse command line arguments
    local action="${1:-push}"
    local target_env="${2:-dev}"

    case "$action" in
        create)
            create_shared_configs
            print_success "Configuration files created. Review them in: $CONFIG_DIR"
            echo
            echo "Next step: Run '$0 push [env]' to push configurations to Nacos"
            ;;

        push)
            # Check Nacos accessibility
            if ! check_nacos_accessibility; then
                exit 1
            fi

            # Authenticate
            authenticate_nacos

            # Create configurations if they don't exist
            if [ ! -d "$CONFIG_DIR" ] || [ -z "$(ls -A $CONFIG_DIR)" ]; then
                create_shared_configs
            fi

            # Determine namespace based on environment
            case "$target_env" in
                dev|development)
                    NAMESPACE="dev"
                    ENV_NAME="Development"
                    ;;
                staging|stage)
                    NAMESPACE="staging"
                    ENV_NAME="Staging"
                    ;;
                prod|production)
                    NAMESPACE="prod"
                    ENV_NAME="Production"
                    ;;
                public|default)
                    NAMESPACE=""
                    ENV_NAME="Public"
                    ;;
                *)
                    print_error "Invalid environment: $target_env"
                    echo "Valid environments: dev, staging, prod, public"
                    exit 1
                    ;;
            esac

            # Override with environment variable if set
            if [ -n "$NACOS_NAMESPACE" ]; then
                NAMESPACE="$NACOS_NAMESPACE"
            fi

            # Backup existing configurations
            backup_configs "$NAMESPACE"

            # Push configurations
            push_all_configs "$NAMESPACE" "$ENV_NAME"

            # Verify configurations
            verify_configs "$NAMESPACE"

            print_success "Configuration push completed!"
            ;;

        verify)
            # Check Nacos accessibility
            if ! check_nacos_accessibility; then
                exit 1
            fi

            # Authenticate
            authenticate_nacos

            # Verify configurations
            verify_configs "$NACOS_NAMESPACE"
            ;;

        backup)
            # Check Nacos accessibility
            if ! check_nacos_accessibility; then
                exit 1
            fi

            # Authenticate
            authenticate_nacos

            # Backup configurations
            backup_configs "$NACOS_NAMESPACE"
            ;;

        *)
            echo "Usage: $0 [action] [environment]"
            echo
            echo "Actions:"
            echo "  create    - Create configuration files locally"
            echo "  push      - Push configurations to Nacos (default)"
            echo "  verify    - Verify configurations in Nacos"
            echo "  backup    - Backup existing configurations"
            echo
            echo "Environments:"
            echo "  dev       - Development environment"
            echo "  staging   - Staging environment"
            echo "  prod      - Production environment"
            echo "  public    - Public namespace (default)"
            echo
            echo "Examples:"
            echo "  $0 create              # Create configuration files"
            echo "  $0 push dev           # Push to development environment"
            echo "  $0 push prod          # Push to production environment"
            echo "  $0 verify             # Verify current configurations"
            echo "  $0 backup             # Backup existing configurations"
            echo
            echo "Environment Variables:"
            echo "  NACOS_SERVER_ADDR    - Nacos server address (default: localhost:8848)"
            echo "  NACOS_USERNAME       - Nacos username (default: nacos)"
            echo "  NACOS_PASSWORD       - Nacos password (default: nacos)"
            echo "  NACOS_NAMESPACE      - Override target namespace"
            echo "  NACOS_GROUP          - Configuration group (default: DEFAULT_GROUP)"
            echo "  DRY_RUN              - Set to 'true' for dry run"
            exit 0
            ;;
    esac
}

# Run main function
main "$@"