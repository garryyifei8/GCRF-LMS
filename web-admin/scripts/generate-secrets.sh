#!/bin/bash
# =============================================================================
# GCRF Library Management System - Secure Secrets Generator
# Version: 1.0.0
# Last Updated: 2025-11-01
#
# This script generates cryptographically secure secrets for all services
# Usage: ./generate-secrets.sh [environment]
# Environment: dev, staging, prod (default: dev)
# =============================================================================

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Environment (dev, staging, prod)
ENVIRONMENT=${1:-dev}
OUTPUT_FILE=".env.${ENVIRONMENT}.generated"
TIMESTAMP=$(date +"%Y-%m-%d %H:%M:%S")

# =============================================================================
# Helper Functions
# =============================================================================

# Print colored message
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Generate secure random string using OpenSSL
generate_secret() {
    local length=$1
    openssl rand -base64 "$length" | tr -d '\n' | head -c "$length"
}

# Generate URL-safe password (no @ # % for connection strings)
generate_url_safe_password() {
    local length=$1
    < /dev/urandom tr -dc 'A-Za-z0-9!$^*()_+-=' | head -c "$length"
}

# Generate alphanumeric string
generate_alphanumeric() {
    local length=$1
    < /dev/urandom tr -dc 'A-Za-z0-9' | head -c "$length"
}

# Generate JWT secret (base64, minimum 64 characters)
generate_jwt_secret() {
    openssl rand -base64 64 | tr -d '\n'
}

# Check if OpenSSL is installed
check_dependencies() {
    if ! command -v openssl &> /dev/null; then
        print_message "$RED" "Error: OpenSSL is not installed. Please install it first."
        exit 1
    fi
}

# =============================================================================
# Main Script
# =============================================================================

print_message "$BLUE" "========================================="
print_message "$BLUE" "GCRF Library Management System"
print_message "$BLUE" "Secure Secrets Generator"
print_message "$BLUE" "========================================="
echo ""

# Check dependencies
check_dependencies

print_message "$YELLOW" "Generating secrets for environment: ${ENVIRONMENT}"
print_message "$YELLOW" "Output file: ${OUTPUT_FILE}"
echo ""

# Confirm before proceeding
if [ "$ENVIRONMENT" == "prod" ]; then
    print_message "$RED" "WARNING: Generating production secrets!"
    read -p "Are you sure you want to continue? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        print_message "$RED" "Aborted."
        exit 1
    fi
fi

# =============================================================================
# Generate Secrets
# =============================================================================

print_message "$GREEN" "Generating JWT secret (64+ characters)..."
JWT_SECRET=$(generate_jwt_secret)

print_message "$GREEN" "Generating database passwords (24 characters, URL-safe)..."
DB_PASSWORD=$(generate_url_safe_password 24)
AUTH_DB_PASSWORD=$(generate_url_safe_password 24)
BOOK_DB_PASSWORD=$(generate_url_safe_password 24)
CIRCULATION_DB_PASSWORD=$(generate_url_safe_password 24)
READER_DB_PASSWORD=$(generate_url_safe_password 24)
SYSTEM_DB_PASSWORD=$(generate_url_safe_password 24)
NOTIFICATION_DB_PASSWORD=$(generate_url_safe_password 24)

print_message "$GREEN" "Generating Redis password (32 characters)..."
REDIS_PASSWORD=$(generate_secret 32)

print_message "$GREEN" "Generating Nacos credentials..."
NACOS_PASSWORD=$(generate_url_safe_password 20)
NACOS_AUTH_SECRET=$(generate_secret 32)

print_message "$GREEN" "Generating RabbitMQ password (24 characters)..."
RABBITMQ_PASSWORD=$(generate_url_safe_password 24)

print_message "$GREEN" "Generating MinIO secret key (32 characters)..."
MINIO_SECRET_KEY=$(generate_secret 32)

print_message "$GREEN" "Generating monitoring passwords..."
ACTUATOR_PASSWORD=$(generate_url_safe_password 20)

print_message "$GREEN" "Generating email app password..."
MAIL_PASSWORD=$(generate_alphanumeric 16)

print_message "$GREEN" "Generating backup encryption key..."
BACKUP_ENCRYPTION_KEY=$(generate_secret 32)

print_message "$GREEN" "Generating CSRF token secret..."
CSRF_TOKEN_SECRET=$(generate_secret 32)

print_message "$GREEN" "Generating two-factor auth secret..."
TWO_FACTOR_SECRET=$(generate_secret 32)

# =============================================================================
# Create Environment File
# =============================================================================

print_message "$YELLOW" "Creating environment file..."

cat > "$OUTPUT_FILE" << EOF
# =============================================================================
# GCRF Library Management System - Generated Secrets
# Environment: ${ENVIRONMENT}
# Generated: ${TIMESTAMP}
#
# WARNING: This file contains sensitive information!
# - DO NOT commit this file to version control
# - DO NOT share this file via unsecured channels
# - Store securely in a password manager or secret vault
# - Set file permissions to 600 (owner read/write only)
# =============================================================================

# JWT Security Configuration
JWT_SECRET=${JWT_SECRET}
JWT_ACCESS_TOKEN_VALIDITY=3600000    # 1 hour
JWT_REFRESH_TOKEN_VALIDITY=86400000  # 24 hours
JWT_ALGORITHM=HS512
JWT_ISSUER=gcrf-library-system-${ENVIRONMENT}

# Primary Database Password (PostgreSQL)
DB_PASSWORD=${DB_PASSWORD}

# Service-Specific Database Passwords
AUTH_DB_PASSWORD=${AUTH_DB_PASSWORD}
BOOK_DB_PASSWORD=${BOOK_DB_PASSWORD}
CIRCULATION_DB_PASSWORD=${CIRCULATION_DB_PASSWORD}
READER_DB_PASSWORD=${READER_DB_PASSWORD}
SYSTEM_DB_PASSWORD=${SYSTEM_DB_PASSWORD}
NOTIFICATION_DB_PASSWORD=${NOTIFICATION_DB_PASSWORD}

# Redis Security
REDIS_PASSWORD=${REDIS_PASSWORD}
REDIS_USERNAME=gcrf_service_${ENVIRONMENT}

# Nacos Security
NACOS_PASSWORD=${NACOS_PASSWORD}
NACOS_AUTH_SECRET=${NACOS_AUTH_SECRET}
NACOS_USERNAME=nacos_${ENVIRONMENT}

# RabbitMQ Security
RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}
RABBITMQ_USERNAME=gcrf_rabbit_${ENVIRONMENT}

# MinIO Object Storage
MINIO_SECRET_KEY=${MINIO_SECRET_KEY}
MINIO_ACCESS_KEY=gcrf_minio_${ENVIRONMENT}

# Monitoring & Management
ACTUATOR_PASSWORD=${ACTUATOR_PASSWORD}
ACTUATOR_USERNAME=admin_${ENVIRONMENT}

# Email Configuration
MAIL_PASSWORD=${MAIL_PASSWORD}

# Backup Encryption
BACKUP_ENCRYPTION_KEY=${BACKUP_ENCRYPTION_KEY}

# CSRF Protection
CSRF_TOKEN_SECRET=${CSRF_TOKEN_SECRET}

# Two-Factor Authentication
TWO_FACTOR_SECRET=${TWO_FACTOR_SECRET}

# =============================================================================
# Security Metadata
# =============================================================================
# Generation Details:
# - Script Version: 1.0.0
# - Generation Date: ${TIMESTAMP}
# - Environment: ${ENVIRONMENT}
# - Host: $(hostname)
# - User: $(whoami)
# - OpenSSL Version: $(openssl version)
#
# Rotation Schedule:
# - Production: Every 90 days
# - Staging: Every 180 days
# - Development: Every 365 days
#
# Next Rotation Due: $(date -d "+90 days" +"%Y-%m-%d" 2>/dev/null || date -v +90d +"%Y-%m-%d")
# =============================================================================
EOF

# Set secure file permissions
chmod 600 "$OUTPUT_FILE"

# =============================================================================
# Validation
# =============================================================================

print_message "$YELLOW" "Validating generated secrets..."

# Function to validate secret length
validate_secret_length() {
    local name=$1
    local value=$2
    local min_length=$3

    if [ ${#value} -lt $min_length ]; then
        print_message "$RED" "✗ ${name} is too short (${#value} < ${min_length})"
        return 1
    else
        print_message "$GREEN" "✓ ${name} is valid (${#value} characters)"
        return 0
    fi
}

# Validate all secrets
VALIDATION_PASSED=true

validate_secret_length "JWT_SECRET" "$JWT_SECRET" 64 || VALIDATION_PASSED=false
validate_secret_length "DB_PASSWORD" "$DB_PASSWORD" 20 || VALIDATION_PASSED=false
validate_secret_length "REDIS_PASSWORD" "$REDIS_PASSWORD" 32 || VALIDATION_PASSED=false
validate_secret_length "NACOS_AUTH_SECRET" "$NACOS_AUTH_SECRET" 32 || VALIDATION_PASSED=false
validate_secret_length "MINIO_SECRET_KEY" "$MINIO_SECRET_KEY" 32 || VALIDATION_PASSED=false
validate_secret_length "BACKUP_ENCRYPTION_KEY" "$BACKUP_ENCRYPTION_KEY" 32 || VALIDATION_PASSED=false

echo ""

if [ "$VALIDATION_PASSED" = true ]; then
    print_message "$GREEN" "✓ All secrets passed validation"
else
    print_message "$RED" "✗ Some secrets failed validation. Please regenerate."
    exit 1
fi

# =============================================================================
# Summary
# =============================================================================

echo ""
print_message "$BLUE" "========================================="
print_message "$BLUE" "Secret Generation Complete!"
print_message "$BLUE" "========================================="
echo ""

print_message "$GREEN" "Generated secrets have been saved to: ${OUTPUT_FILE}"
print_message "$GREEN" "File permissions set to 600 (owner read/write only)"
echo ""

print_message "$YELLOW" "IMPORTANT NEXT STEPS:"
print_message "$YELLOW" "1. Review the generated secrets in ${OUTPUT_FILE}"
print_message "$YELLOW" "2. Copy ${OUTPUT_FILE} to your secure location"
print_message "$YELLOW" "3. Update your application configuration"
print_message "$YELLOW" "4. Store secrets in your secret management system:"
print_message "$YELLOW" "   - HashiCorp Vault"
print_message "$YELLOW" "   - AWS Secrets Manager"
print_message "$YELLOW" "   - Azure Key Vault"
print_message "$YELLOW" "   - Kubernetes Secrets"
print_message "$YELLOW" "5. Delete local copies after secure storage"
echo ""

print_message "$RED" "SECURITY REMINDERS:"
print_message "$RED" "- Never commit secrets to Git"
print_message "$RED" "- Never share secrets via email or chat"
print_message "$RED" "- Rotate secrets regularly (90 days for production)"
print_message "$RED" "- Use different secrets for each environment"
print_message "$RED" "- Enable audit logging for secret access"
echo ""

# Optional: Copy to clipboard (macOS)
if [[ "$OSTYPE" == "darwin"* ]]; then
    read -p "Copy JWT secret to clipboard? (y/n): " copy_jwt
    if [ "$copy_jwt" == "y" ]; then
        echo -n "$JWT_SECRET" | pbcopy
        print_message "$GREEN" "JWT secret copied to clipboard (will clear in 30 seconds)"
        (sleep 30 && echo "" | pbcopy) &
    fi
fi

print_message "$GREEN" "Script completed successfully!"