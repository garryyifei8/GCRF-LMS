#!/bin/bash

# Environment Manager Script for GCRF Library Management System
# Provides utilities for managing environment configurations

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SECRETS_DIR="${SCRIPT_DIR}/secrets"
ENVIRONMENTS=("dev" "staging" "prod")

# Functions
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

show_help() {
    cat << EOF
GCRF Library Management System - Environment Manager

Usage: ./env-manager.sh [command] [options]

Commands:
  init <env>          Initialize environment configuration
  validate <env>      Validate environment configuration
  switch <env>        Switch to specified environment
  encrypt             Encrypt sensitive values
  decrypt             Decrypt sensitive values
  generate-secrets    Generate secure random secrets
  diff <env1> <env2>  Compare two environment configurations
  export <env>        Export environment variables
  backup <env>        Backup environment configuration
  restore <env>       Restore environment configuration

Examples:
  ./env-manager.sh init prod
  ./env-manager.sh validate staging
  ./env-manager.sh switch dev
  ./env-manager.sh generate-secrets
  ./env-manager.sh diff dev prod

EOF
}

# Initialize environment
init_environment() {
    local env=$1
    print_info "Initializing environment: $env"

    # Create .env file from template
    if [[ ! -f ".env.example" ]]; then
        print_error ".env.example not found"
        exit 1
    fi

    if [[ -f ".env.$env" ]]; then
        print_warning ".env.$env already exists. Backing up..."
        cp ".env.$env" ".env.$env.backup.$(date +%Y%m%d_%H%M%S)"
    fi

    cp ".env.example" ".env.$env"
    print_success "Created .env.$env from template"

    # Create secrets directory
    mkdir -p "$SECRETS_DIR"
    chmod 700 "$SECRETS_DIR"

    # Environment-specific modifications
    case $env in
        dev)
            sed -i '' "s/ENVIRONMENT=.*/ENVIRONMENT=dev/" ".env.$env"
            sed -i '' "s/SPRING_PROFILES_ACTIVE=.*/SPRING_PROFILES_ACTIVE=dev/" ".env.$env"
            sed -i '' "s/LOG_LEVEL=.*/LOG_LEVEL=DEBUG/" ".env.$env"
            ;;
        staging)
            sed -i '' "s/ENVIRONMENT=.*/ENVIRONMENT=staging/" ".env.$env"
            sed -i '' "s/SPRING_PROFILES_ACTIVE=.*/SPRING_PROFILES_ACTIVE=staging/" ".env.$env"
            sed -i '' "s/LOG_LEVEL=.*/LOG_LEVEL=INFO/" ".env.$env"
            ;;
        prod)
            sed -i '' "s/ENVIRONMENT=.*/ENVIRONMENT=prod/" ".env.$env"
            sed -i '' "s/SPRING_PROFILES_ACTIVE=.*/SPRING_PROFILES_ACTIVE=prod/" ".env.$env"
            sed -i '' "s/LOG_LEVEL=.*/LOG_LEVEL=WARN/" ".env.$env"
            sed -i '' "s/DB_SSL_MODE=.*/DB_SSL_MODE=require/" ".env.$env"
            ;;
    esac

    print_success "Environment $env initialized"
    print_info "Please edit .env.$env with your specific values"
}

# Validate environment
validate_environment() {
    local env=$1
    print_info "Validating environment: $env"

    if [[ ! -f "validate-env.sh" ]]; then
        print_error "validate-env.sh not found"
        exit 1
    fi

    bash validate-env.sh "$env"
}

# Switch environment
switch_environment() {
    local env=$1
    print_info "Switching to environment: $env"

    if [[ ! -f ".env.$env" ]]; then
        print_error ".env.$env not found. Run 'init $env' first."
        exit 1
    fi

    # Create symbolic link
    rm -f .env
    ln -s ".env.$env" .env
    print_success "Switched to $env environment"

    # Show current configuration
    print_info "Current configuration:"
    grep -E "^(ENVIRONMENT|SPRING_PROFILES_ACTIVE|DB_HOST|REDIS_HOST)" .env | head -5
}

# Generate secrets
generate_secrets() {
    print_info "Generating secure secrets..."

    mkdir -p "$SECRETS_DIR"
    chmod 700 "$SECRETS_DIR"

    # Generate JWT secret (256-bit)
    jwt_secret=$(openssl rand -base64 32)
    echo "$jwt_secret" > "$SECRETS_DIR/jwt_secret.txt"
    chmod 600 "$SECRETS_DIR/jwt_secret.txt"
    print_success "Generated JWT secret"

    # Generate database password
    db_password=$(openssl rand -base64 24 | tr -d "=+/" | cut -c1-16)
    echo "$db_password" > "$SECRETS_DIR/db_password.txt"
    chmod 600 "$SECRETS_DIR/db_password.txt"
    print_success "Generated database password"

    # Generate Redis password
    redis_password=$(openssl rand -base64 24 | tr -d "=+/" | cut -c1-16)
    echo "$redis_password" > "$SECRETS_DIR/redis_password.txt"
    chmod 600 "$SECRETS_DIR/redis_password.txt"
    print_success "Generated Redis password"

    # Generate Nacos password
    nacos_password=$(openssl rand -base64 24 | tr -d "=+/" | cut -c1-16)
    echo "$nacos_password" > "$SECRETS_DIR/nacos_password.txt"
    chmod 600 "$SECRETS_DIR/nacos_password.txt"
    print_success "Generated Nacos password"

    print_warning "Secrets generated in $SECRETS_DIR/"
    print_warning "Update your .env files with these values"
    print_info "JWT Secret: $jwt_secret"
    print_info "DB Password: $db_password"
    print_info "Redis Password: $redis_password"
    print_info "Nacos Password: $nacos_password"
}

# Encrypt sensitive values
encrypt_values() {
    print_info "Encrypting sensitive values..."

    if [[ ! -f ".env" ]]; then
        print_error "No .env file found"
        exit 1
    fi

    # Create encrypted file
    openssl enc -aes-256-cbc -salt -in .env -out .env.enc -k "${ENCRYPTION_KEY:-default-key}"
    chmod 600 .env.enc
    print_success "Encrypted configuration saved to .env.enc"
}

# Decrypt sensitive values
decrypt_values() {
    print_info "Decrypting sensitive values..."

    if [[ ! -f ".env.enc" ]]; then
        print_error "No .env.enc file found"
        exit 1
    fi

    # Decrypt file
    openssl enc -aes-256-cbc -d -in .env.enc -out .env.decrypted -k "${ENCRYPTION_KEY:-default-key}"
    chmod 600 .env.decrypted
    print_success "Decrypted configuration saved to .env.decrypted"
}

# Compare environments
diff_environments() {
    local env1=$1
    local env2=$2

    print_info "Comparing $env1 and $env2 environments..."

    if [[ ! -f ".env.$env1" ]] || [[ ! -f ".env.$env2" ]]; then
        print_error "Environment files not found"
        exit 1
    fi

    # Show differences
    print_info "Configuration differences:"
    diff --unified=1 --color=always ".env.$env1" ".env.$env2" | grep -E "^[+-][A-Z]" || true
}

# Export environment variables
export_environment() {
    local env=$1

    if [[ ! -f ".env.$env" ]]; then
        print_error ".env.$env not found"
        exit 1
    fi

    print_info "Exporting environment variables from .env.$env"
    set -a
    source ".env.$env"
    set +a
    print_success "Environment variables exported"
}

# Backup environment
backup_environment() {
    local env=$1
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_dir="backups"

    mkdir -p "$backup_dir"

    if [[ ! -f ".env.$env" ]]; then
        print_error ".env.$env not found"
        exit 1
    fi

    local backup_file="$backup_dir/env.$env.$timestamp.tar.gz"

    # Create backup archive
    tar -czf "$backup_file" \
        ".env.$env" \
        ${SECRETS_DIR}/*.txt \
        2>/dev/null || true

    print_success "Backup created: $backup_file"
}

# Restore environment
restore_environment() {
    local env=$1
    local backup_file=$2

    if [[ -z "$backup_file" ]]; then
        # Show available backups
        print_info "Available backups:"
        ls -la backups/env.$env.*.tar.gz 2>/dev/null || print_error "No backups found"
        return
    fi

    if [[ ! -f "$backup_file" ]]; then
        print_error "Backup file not found: $backup_file"
        exit 1
    fi

    print_warning "This will overwrite current configuration. Continue? (y/n)"
    read -r confirm
    if [[ "$confirm" != "y" ]]; then
        print_info "Restore cancelled"
        exit 0
    fi

    # Extract backup
    tar -xzf "$backup_file"
    print_success "Environment restored from $backup_file"
}

# Main execution
main() {
    case "${1:-}" in
        init)
            init_environment "${2:-dev}"
            ;;
        validate)
            validate_environment "${2:-dev}"
            ;;
        switch)
            switch_environment "${2:-dev}"
            ;;
        encrypt)
            encrypt_values
            ;;
        decrypt)
            decrypt_values
            ;;
        generate-secrets)
            generate_secrets
            ;;
        diff)
            diff_environments "${2:-dev}" "${3:-prod}"
            ;;
        export)
            export_environment "${2:-dev}"
            ;;
        backup)
            backup_environment "${2:-dev}"
            ;;
        restore)
            restore_environment "${2:-dev}" "${3:-}"
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "Unknown command: ${1:-}"
            show_help
            exit 1
            ;;
    esac
}

# Run main function
main "$@"