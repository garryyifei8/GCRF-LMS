#!/bin/bash

################################################################################
# GCRF Library Management System - Production Deployment Script
#
# This script automates the deployment of the GCRF Library Management System
# to a production environment.
#
# Usage: ./deploy-production.sh [options]
#
# Options:
#   --skip-infrastructure  Skip infrastructure deployment (if already running)
#   --skip-monitoring      Skip monitoring services deployment
#   --force-recreate       Force recreate all containers
#   --dry-run             Show what would be deployed without doing it
#
# Prerequisites:
#   - Docker and Docker Compose installed
#   - .env file configured with production values
#   - All required images available (built or loaded)
#   - Sufficient system resources
#
# Exit Codes:
#   0 - Success
#   1 - General error
#   2 - Prerequisites not met
#   3 - Deployment failed
################################################################################

set -e  # Exit on error
set -u  # Exit on undefined variable

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOYMENT_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$DEPLOYMENT_DIR")"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="/var/log/gcrf-deployment-${TIMESTAMP}.log"

# Command line options
SKIP_INFRASTRUCTURE=false
SKIP_MONITORING=false
FORCE_RECREATE=false
DRY_RUN=false

################################################################################
# Functions
################################################################################

# Print colored message
log() {
    local level=$1
    shift
    local message="$@"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')

    case $level in
        INFO)
            echo -e "${BLUE}[INFO]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        SUCCESS)
            echo -e "${GREEN}[SUCCESS]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        WARNING)
            echo -e "${YELLOW}[WARNING]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        ERROR)
            echo -e "${RED}[ERROR]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        *)
            echo "$message" | tee -a "$LOG_FILE"
            ;;
    esac
}

# Check prerequisites
check_prerequisites() {
    log INFO "Checking prerequisites..."

    # Check Docker
    if ! command -v docker &> /dev/null; then
        log ERROR "Docker is not installed"
        exit 2
    fi
    log SUCCESS "Docker installed: $(docker --version)"

    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        log ERROR "Docker Compose is not installed"
        exit 2
    fi
    log SUCCESS "Docker Compose installed: $(docker-compose --version)"

    # Check Docker daemon
    if ! docker info &> /dev/null; then
        log ERROR "Docker daemon is not running"
        exit 2
    fi
    log SUCCESS "Docker daemon is running"

    # Check .env file exists
    if [ ! -f "$DEPLOYMENT_DIR/.env" ]; then
        log ERROR ".env file not found in $DEPLOYMENT_DIR"
        log ERROR "Please create .env file from .env.example and configure it"
        exit 2
    fi
    log SUCCESS ".env file found"

    # Check required environment variables
    source "$DEPLOYMENT_DIR/.env"
    local required_vars=(
        "DB_PASSWORD"
        "REDIS_PASSWORD"
        "RABBITMQ_DEFAULT_PASS"
        "NACOS_PASSWORD"
        "JWT_SECRET"
        "MINIO_ROOT_PASSWORD"
    )

    for var in "${required_vars[@]}"; do
        if [ -z "${!var:-}" ]; then
            log ERROR "Required environment variable $var is not set"
            exit 2
        fi
    done
    log SUCCESS "All required environment variables are set"

    # Check disk space (need at least 20GB free)
    local free_space=$(df -BG "$DEPLOYMENT_DIR" | awk 'NR==2 {print $4}' | sed 's/G//')
    if [ "$free_space" -lt 20 ]; then
        log WARNING "Low disk space: ${free_space}GB free. Recommended: at least 20GB"
    else
        log SUCCESS "Sufficient disk space: ${free_space}GB free"
    fi

    # Check memory (need at least 8GB)
    local total_mem=$(free -g | awk '/^Mem:/{print $2}')
    if [ "$total_mem" -lt 8 ]; then
        log WARNING "Low memory: ${total_mem}GB. Recommended: at least 8GB"
    else
        log SUCCESS "Sufficient memory: ${total_mem}GB"
    fi
}

# Create backup before deployment
create_backup() {
    log INFO "Creating pre-deployment backup..."

    local backup_dir="/backups/pre-deployment-${TIMESTAMP}"
    mkdir -p "$backup_dir"

    # Backup database if running
    if docker ps | grep -q postgres-primary; then
        log INFO "Backing up PostgreSQL..."
        docker exec postgres-primary pg_dumpall -U postgres | gzip > "$backup_dir/database-backup.sql.gz"
        log SUCCESS "Database backup completed"
    fi

    # Backup configuration
    log INFO "Backing up configuration..."
    cp "$DEPLOYMENT_DIR/.env" "$backup_dir/env.backup"
    cp -r "$DEPLOYMENT_DIR"/*.yml "$backup_dir/" 2>/dev/null || true
    log SUCCESS "Configuration backup completed"

    # Create backup manifest
    cat > "$backup_dir/MANIFEST.txt" <<EOF
GCRF Library Management System - Pre-Deployment Backup
Created: $(date)
Location: $backup_dir

Files:
$(ls -lh $backup_dir)
EOF

    log SUCCESS "Backup completed: $backup_dir"
}

# Deploy infrastructure services
deploy_infrastructure() {
    if [ "$SKIP_INFRASTRUCTURE" = true ]; then
        log INFO "Skipping infrastructure deployment (--skip-infrastructure)"
        return
    fi

    log INFO "Deploying infrastructure services..."

    cd "$DEPLOYMENT_DIR"

    # Pull latest images
    log INFO "Pulling infrastructure images..."
    if [ "$DRY_RUN" = false ]; then
        docker-compose -f docker-compose.infrastructure.yml pull
    fi

    # Start infrastructure services
    log INFO "Starting infrastructure services..."
    if [ "$DRY_RUN" = false ]; then
        local compose_flags=""
        if [ "$FORCE_RECREATE" = true ]; then
            compose_flags="--force-recreate"
        fi

        docker-compose -f docker-compose.infrastructure.yml up -d $compose_flags

        # Wait for services to be healthy
        log INFO "Waiting for infrastructure services to be healthy..."
        sleep 10

        local max_attempts=60
        local attempt=0

        while [ $attempt -lt $max_attempts ]; do
            if docker exec postgres-primary pg_isready -U postgres &> /dev/null && \
               docker exec redis-master redis-cli -a "$REDIS_PASSWORD" ping &> /dev/null 2>&1 && \
               curl -sf http://localhost:8848/nacos/v1/console/health/readiness &> /dev/null; then
                log SUCCESS "Infrastructure services are healthy"
                break
            fi

            attempt=$((attempt + 1))
            log INFO "Waiting for infrastructure services... ($attempt/$max_attempts)"
            sleep 5
        done

        if [ $attempt -eq $max_attempts ]; then
            log ERROR "Infrastructure services did not become healthy in time"
            exit 3
        fi
    else
        log INFO "[DRY RUN] Would start infrastructure services"
    fi

    log SUCCESS "Infrastructure services deployed successfully"
}

# Deploy application services
deploy_application() {
    log INFO "Deploying application services..."

    cd "$DEPLOYMENT_DIR"

    # Pull latest images
    log INFO "Pulling application images..."
    if [ "$DRY_RUN" = false ]; then
        docker-compose -f docker-compose.services.yml pull || true  # May not be in registry
    fi

    # Start application services
    log INFO "Starting application services..."
    if [ "$DRY_RUN" = false ]; then
        local compose_flags=""
        if [ "$FORCE_RECREATE" = true ]; then
            compose_flags="--force-recreate"
        fi

        docker-compose -f docker-compose.services.yml up -d $compose_flags

        # Wait for services to register with Nacos
        log INFO "Waiting for application services to register..."
        sleep 30

        # Check service health
        local services=(
            "8080:Gateway"
            "8081:Auth"
            "8082:Book"
            "8083:Circulation"
            "8084:Reader"
            "8085:System"
            "8086:Notification"
        )

        for service in "${services[@]}"; do
            local port="${service%%:*}"
            local name="${service##*:}"

            local max_attempts=30
            local attempt=0

            while [ $attempt -lt $max_attempts ]; do
                if curl -sf "http://localhost:${port}/actuator/health" &> /dev/null; then
                    log SUCCESS "$name service is healthy (port $port)"
                    break
                fi

                attempt=$((attempt + 1))
                if [ $attempt -eq $max_attempts ]; then
                    log WARNING "$name service did not become healthy (port $port)"
                else
                    sleep 2
                fi
            done
        done
    else
        log INFO "[DRY RUN] Would start application services"
    fi

    log SUCCESS "Application services deployed successfully"
}

# Deploy monitoring services
deploy_monitoring() {
    if [ "$SKIP_MONITORING" = true ]; then
        log INFO "Skipping monitoring deployment (--skip-monitoring)"
        return
    fi

    log INFO "Deploying monitoring services..."

    cd "$DEPLOYMENT_DIR"

    # Check if monitoring compose file exists
    if [ ! -f "docker-compose.monitoring.yml" ]; then
        log WARNING "docker-compose.monitoring.yml not found, skipping monitoring"
        return
    fi

    # Pull latest images
    log INFO "Pulling monitoring images..."
    if [ "$DRY_RUN" = false ]; then
        docker-compose -f docker-compose.monitoring.yml pull
    fi

    # Start monitoring services
    log INFO "Starting monitoring services..."
    if [ "$DRY_RUN" = false ]; then
        local compose_flags=""
        if [ "$FORCE_RECREATE" = true ]; then
            compose_flags="--force-recreate"
        fi

        docker-compose -f docker-compose.monitoring.yml up -d $compose_flags

        # Wait for Prometheus and Grafana to be ready
        sleep 10

        if curl -sf http://localhost:9090/-/healthy &> /dev/null; then
            log SUCCESS "Prometheus is healthy"
        else
            log WARNING "Prometheus health check failed"
        fi

        if curl -sf http://localhost:3000/api/health &> /dev/null; then
            log SUCCESS "Grafana is healthy"
        else
            log WARNING "Grafana health check failed"
        fi
    else
        log INFO "[DRY RUN] Would start monitoring services"
    fi

    log SUCCESS "Monitoring services deployed successfully"
}

# Post-deployment verification
verify_deployment() {
    log INFO "Running post-deployment verification..."

    # Check all containers are running
    log INFO "Checking container status..."
    local expected_containers=(
        "postgres-primary"
        "redis-master"
        "rabbitmq"
        "nacos"
        "minio"
        "gcrf-gateway-service"
        "gcrf-auth-service"
        "gcrf-book-service"
    )

    local failed_containers=()
    for container in "${expected_containers[@]}"; do
        if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
            local status=$(docker inspect --format='{{.State.Status}}' "$container")
            if [ "$status" = "running" ]; then
                log SUCCESS "Container $container is running"
            else
                log ERROR "Container $container is not running (status: $status)"
                failed_containers+=("$container")
            fi
        else
            log ERROR "Container $container not found"
            failed_containers+=("$container")
        fi
    done

    if [ ${#failed_containers[@]} -gt 0 ]; then
        log ERROR "Some containers failed to start: ${failed_containers[*]}"
        exit 3
    fi

    # Check gateway endpoint
    log INFO "Checking API Gateway..."
    if curl -sf http://localhost:8080/actuator/health &> /dev/null; then
        log SUCCESS "API Gateway is accessible"
    else
        log ERROR "API Gateway is not accessible"
        exit 3
    fi

    log SUCCESS "Deployment verification passed"
}

# Print deployment summary
print_summary() {
    log "" "
╔═══════════════════════════════════════════════════════════════════╗
║                  Deployment Completed Successfully                ║
╚═══════════════════════════════════════════════════════════════════╝

Deployment Time: $(date)
Log File: $LOG_FILE

Services Status:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
$(docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' | grep gcrf || echo "No GCRF services found")

Access Points:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
API Gateway:    http://localhost:8080
Nacos Console:  http://localhost:8848/nacos
RabbitMQ Mgmt:  http://localhost:15672
MinIO Console:  http://localhost:9001
Grafana:        http://localhost:3000
Prometheus:     http://localhost:9090

Next Steps:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. Configure Nginx reverse proxy for external access
2. Set up SSL certificates
3. Configure monitoring alerts
4. Import initial data
5. Run system health check: ./scripts/health-check.sh
6. Review logs: tail -f /logs/services/*/application.log

For troubleshooting, see: doc/deployment/troubleshooting-guide.md
"
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --skip-infrastructure)
                SKIP_INFRASTRUCTURE=true
                shift
                ;;
            --skip-monitoring)
                SKIP_MONITORING=true
                shift
                ;;
            --force-recreate)
                FORCE_RECREATE=true
                shift
                ;;
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            --help)
                cat <<EOF
GCRF Library Management System - Production Deployment Script

Usage: $0 [options]

Options:
  --skip-infrastructure  Skip infrastructure deployment
  --skip-monitoring      Skip monitoring services deployment
  --force-recreate       Force recreate all containers
  --dry-run             Show what would be deployed without doing it
  --help                Show this help message

Examples:
  $0                                    # Full deployment
  $0 --skip-infrastructure              # Deploy app services only
  $0 --force-recreate                   # Force recreate all containers
  $0 --dry-run                          # Dry run to see what would happen
EOF
                exit 0
                ;;
            *)
                log ERROR "Unknown option: $1"
                log INFO "Use --help for usage information"
                exit 1
                ;;
        esac
    done
}

################################################################################
# Main Execution
################################################################################

main() {
    # Create log file
    sudo touch "$LOG_FILE"
    sudo chmod 644 "$LOG_FILE"

    log "" "
╔═══════════════════════════════════════════════════════════════════╗
║       GCRF Library Management System - Production Deployment      ║
╚═══════════════════════════════════════════════════════════════════╝
"

    if [ "$DRY_RUN" = true ]; then
        log WARNING "DRY RUN MODE - No actual changes will be made"
    fi

    # Parse arguments
    parse_args "$@"

    # Check prerequisites
    check_prerequisites

    # Create backup
    if [ "$DRY_RUN" = false ]; then
        create_backup
    else
        log INFO "[DRY RUN] Would create backup"
    fi

    # Deploy services
    deploy_infrastructure
    deploy_application
    deploy_monitoring

    # Verify deployment
    if [ "$DRY_RUN" = false ]; then
        verify_deployment
    else
        log INFO "[DRY RUN] Would verify deployment"
    fi

    # Print summary
    print_summary

    log SUCCESS "Deployment completed successfully!"
    exit 0
}

# Run main function with all arguments
main "$@"
