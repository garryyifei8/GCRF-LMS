#!/bin/bash

# ============================================
# GCRF Library Management System
# SSL Certificate Generation Script
# ============================================
# This script generates SSL certificates for:
#   - Development: Self-signed certificates
#   - Production: Let's Encrypt certificates
#
# Usage:
#   ./generate-ssl-cert.sh --dev                    # Self-signed for development
#   ./generate-ssl-cert.sh --prod --domain example.com  # Let's Encrypt production
#   ./generate-ssl-cert.sh --renew                  # Renew Let's Encrypt certificates
#   ./generate-ssl-cert.sh --dhparam                # Generate DH parameters only
#
# Prerequisites:
#   - For Let's Encrypt: certbot installed
#   - Domain must be pointed to this server
#   - Port 80 must be accessible for ACME challenge
# ============================================

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"
SSL_DIR="${PROJECT_ROOT}/deployment/ssl"
CERTBOT_DIR="${PROJECT_ROOT}/deployment/certbot"
NGINX_SSL_DIR="/etc/nginx/ssl"

# Certificate parameters for self-signed
COUNTRY="CN"
STATE="Beijing"
LOCALITY="Beijing"
ORGANIZATION="GCRF"
ORGANIZATIONAL_UNIT="IT"
COMMON_NAME="localhost"
VALIDITY_DAYS=365

# Functions
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

print_usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Options:
  --dev                     Generate self-signed certificates for development
  --prod --domain DOMAIN    Generate Let's Encrypt certificates for production
  --renew                   Renew Let's Encrypt certificates
  --dhparam                 Generate DH parameters only
  --email EMAIL             Email for Let's Encrypt notifications (required for --prod)
  --staging                 Use Let's Encrypt staging environment (for testing)
  --dry-run                 Test mode (don't actually generate certificates)
  --output-dir DIR          Custom output directory for certificates
  --help                    Show this help message

Examples:
  # Development (self-signed)
  $0 --dev

  # Production with Let's Encrypt
  $0 --prod --domain library.example.com --email admin@example.com

  # Test Let's Encrypt (staging)
  $0 --prod --domain library.example.com --email admin@example.com --staging

  # Renew certificates
  $0 --renew

EOF
}

# Check if running as root (needed for Let's Encrypt)
check_root() {
    if [[ $EUID -ne 0 ]]; then
        log_warning "This script should be run as root for Let's Encrypt operations"
        return 1
    fi
    return 0
}

# Create output directories
create_directories() {
    local output_dir="${1:-$SSL_DIR}"
    mkdir -p "$output_dir"
    mkdir -p "$CERTBOT_DIR/www"
    mkdir -p "$CERTBOT_DIR/conf"
    log_info "Created directories: $output_dir, $CERTBOT_DIR"
}

# Generate DH parameters
generate_dhparam() {
    local output_dir="${1:-$SSL_DIR}"
    local dhparam_file="${output_dir}/dhparam.pem"

    if [[ -f "$dhparam_file" ]]; then
        log_warning "DH parameters file already exists: $dhparam_file"
        read -p "Overwrite? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "Skipping DH parameters generation"
            return 0
        fi
    fi

    log_info "Generating DH parameters (this may take several minutes)..."
    openssl dhparam -out "$dhparam_file" 2048
    chmod 600 "$dhparam_file"
    log_success "DH parameters generated: $dhparam_file"
}

# Generate self-signed certificates for development
generate_self_signed() {
    local output_dir="${1:-$SSL_DIR}"
    local common_name="${2:-$COMMON_NAME}"
    local validity="${3:-$VALIDITY_DAYS}"

    create_directories "$output_dir"

    log_info "Generating self-signed certificates for development..."
    log_info "Common Name: $common_name"
    log_info "Validity: $validity days"

    # Generate private key
    local key_file="${output_dir}/privkey.pem"
    log_info "Generating private key..."
    openssl genrsa -out "$key_file" 4096
    chmod 600 "$key_file"

    # Create certificate signing request config
    local csr_config="${output_dir}/csr.conf"
    cat > "$csr_config" << EOF
[req]
default_bits = 4096
prompt = no
default_md = sha256
distinguished_name = dn
req_extensions = req_ext
x509_extensions = v3_ext

[dn]
C = ${COUNTRY}
ST = ${STATE}
L = ${LOCALITY}
O = ${ORGANIZATION}
OU = ${ORGANIZATIONAL_UNIT}
CN = ${common_name}

[req_ext]
subjectAltName = @alt_names

[v3_ext]
subjectAltName = @alt_names
basicConstraints = CA:FALSE
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth

[alt_names]
DNS.1 = ${common_name}
DNS.2 = localhost
DNS.3 = *.localhost
DNS.4 = gcrf-web-admin
DNS.5 = gcrf-gateway-service
IP.1 = 127.0.0.1
IP.2 = ::1
EOF

    # Generate self-signed certificate
    local cert_file="${output_dir}/fullchain.pem"
    log_info "Generating self-signed certificate..."
    openssl req -x509 -nodes \
        -days "$validity" \
        -key "$key_file" \
        -out "$cert_file" \
        -config "$csr_config" \
        -extensions v3_ext

    chmod 644 "$cert_file"

    # Create chain.pem (same as fullchain for self-signed)
    cp "$cert_file" "${output_dir}/chain.pem"

    # Generate DH parameters
    generate_dhparam "$output_dir"

    # Cleanup
    rm -f "$csr_config"

    log_success "Self-signed certificates generated successfully!"
    echo ""
    echo "Certificate files:"
    echo "  Private Key:  ${output_dir}/privkey.pem"
    echo "  Certificate:  ${output_dir}/fullchain.pem"
    echo "  Chain:        ${output_dir}/chain.pem"
    echo "  DH Params:    ${output_dir}/dhparam.pem"
    echo ""
    log_warning "IMPORTANT: Self-signed certificates are for DEVELOPMENT ONLY!"
    log_warning "Browsers will show security warnings. Do not use in production."
    echo ""
    echo "To use with Docker, copy certificates to container or mount volume:"
    echo "  docker cp ${output_dir}/. container_name:/etc/nginx/ssl/"
}

# Generate Let's Encrypt certificates for production
generate_letsencrypt() {
    local domain="$1"
    local email="$2"
    local staging="${3:-false}"
    local dry_run="${4:-false}"
    local output_dir="${5:-$SSL_DIR}"

    # Check if certbot is installed
    if ! command -v certbot &> /dev/null; then
        log_error "certbot is not installed. Please install it first:"
        echo "  # Ubuntu/Debian"
        echo "  sudo apt-get update && sudo apt-get install -y certbot"
        echo ""
        echo "  # CentOS/RHEL"
        echo "  sudo yum install -y certbot"
        echo ""
        echo "  # macOS (via Homebrew)"
        echo "  brew install certbot"
        exit 1
    fi

    check_root || {
        log_error "Let's Encrypt requires root privileges. Please run with sudo."
        exit 1
    }

    create_directories "$output_dir"

    log_info "Generating Let's Encrypt certificate for: $domain"
    log_info "Email: $email"

    # Build certbot command
    local certbot_cmd="certbot certonly --webroot"
    certbot_cmd+=" -w ${CERTBOT_DIR}/www"
    certbot_cmd+=" -d ${domain}"
    certbot_cmd+=" --email ${email}"
    certbot_cmd+=" --agree-tos"
    certbot_cmd+=" --non-interactive"
    certbot_cmd+=" --keep-until-expiring"

    if [[ "$staging" == "true" ]]; then
        certbot_cmd+=" --staging"
        log_warning "Using Let's Encrypt STAGING environment (certificates won't be trusted)"
    fi

    if [[ "$dry_run" == "true" ]]; then
        certbot_cmd+=" --dry-run"
        log_info "DRY RUN mode - no certificates will be generated"
    fi

    # Ensure webroot directory exists
    mkdir -p "${CERTBOT_DIR}/www/.well-known/acme-challenge"

    log_info "Running certbot..."
    if eval "$certbot_cmd"; then
        log_success "Let's Encrypt certificate obtained successfully!"

        # Copy certificates to output directory
        local letsencrypt_dir="/etc/letsencrypt/live/${domain}"
        if [[ -d "$letsencrypt_dir" ]] && [[ "$dry_run" != "true" ]]; then
            cp "${letsencrypt_dir}/fullchain.pem" "${output_dir}/"
            cp "${letsencrypt_dir}/privkey.pem" "${output_dir}/"
            cp "${letsencrypt_dir}/chain.pem" "${output_dir}/"
            chmod 600 "${output_dir}/privkey.pem"
            chmod 644 "${output_dir}/fullchain.pem"
            chmod 644 "${output_dir}/chain.pem"

            log_success "Certificates copied to: $output_dir"
        fi

        # Generate DH parameters if not exists
        if [[ ! -f "${output_dir}/dhparam.pem" ]]; then
            generate_dhparam "$output_dir"
        fi

        echo ""
        echo "Certificate files:"
        echo "  Private Key:  ${output_dir}/privkey.pem"
        echo "  Certificate:  ${output_dir}/fullchain.pem"
        echo "  Chain:        ${output_dir}/chain.pem"
        echo "  DH Params:    ${output_dir}/dhparam.pem"
        echo ""
        echo "Certificate will expire in 90 days. Set up auto-renewal:"
        echo "  sudo crontab -e"
        echo "  Add: 0 0 * * 0 ${SCRIPT_DIR}/generate-ssl-cert.sh --renew"
    else
        log_error "Failed to obtain Let's Encrypt certificate"
        echo ""
        echo "Common issues:"
        echo "  1. Domain not pointing to this server"
        echo "  2. Port 80 blocked by firewall"
        echo "  3. Nginx not serving ACME challenge directory"
        echo ""
        echo "For debugging, check:"
        echo "  sudo certbot certificates"
        echo "  sudo cat /var/log/letsencrypt/letsencrypt.log"
        exit 1
    fi
}

# Renew Let's Encrypt certificates
renew_certificates() {
    local output_dir="${1:-$SSL_DIR}"
    local dry_run="${2:-false}"

    check_root || {
        log_error "Certificate renewal requires root privileges. Please run with sudo."
        exit 1
    }

    if ! command -v certbot &> /dev/null; then
        log_error "certbot is not installed."
        exit 1
    fi

    log_info "Renewing Let's Encrypt certificates..."

    local renew_cmd="certbot renew"
    if [[ "$dry_run" == "true" ]]; then
        renew_cmd+=" --dry-run"
        log_info "DRY RUN mode"
    fi

    if eval "$renew_cmd"; then
        log_success "Certificate renewal completed!"

        # Copy renewed certificates
        if [[ "$dry_run" != "true" ]]; then
            for domain_dir in /etc/letsencrypt/live/*/; do
                domain=$(basename "$domain_dir")
                if [[ "$domain" != "README" ]]; then
                    cp "${domain_dir}fullchain.pem" "${output_dir}/"
                    cp "${domain_dir}privkey.pem" "${output_dir}/"
                    cp "${domain_dir}chain.pem" "${output_dir}/"
                    log_info "Updated certificates for: $domain"
                fi
            done

            # Reload nginx if running
            if command -v nginx &> /dev/null && nginx -t 2>/dev/null; then
                nginx -s reload
                log_success "Nginx reloaded with new certificates"
            elif docker ps --format '{{.Names}}' | grep -q nginx; then
                docker exec gcrf-nginx nginx -s reload
                log_success "Docker nginx container reloaded"
            fi
        fi
    else
        log_error "Certificate renewal failed"
        exit 1
    fi
}

# Verify certificate
verify_certificate() {
    local cert_file="${1:-${SSL_DIR}/fullchain.pem}"

    if [[ ! -f "$cert_file" ]]; then
        log_error "Certificate file not found: $cert_file"
        exit 1
    fi

    log_info "Certificate information:"
    echo ""
    openssl x509 -in "$cert_file" -noout -text | grep -A2 "Subject:\|Issuer:\|Not Before\|Not After\|DNS:"

    echo ""
    log_info "Certificate dates:"
    openssl x509 -in "$cert_file" -noout -dates

    echo ""
    log_info "Certificate fingerprint (SHA-256):"
    openssl x509 -in "$cert_file" -noout -fingerprint -sha256
}

# Main script
main() {
    local mode=""
    local domain=""
    local email=""
    local staging="false"
    local dry_run="false"
    local output_dir="$SSL_DIR"

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --dev)
                mode="dev"
                shift
                ;;
            --prod)
                mode="prod"
                shift
                ;;
            --renew)
                mode="renew"
                shift
                ;;
            --dhparam)
                mode="dhparam"
                shift
                ;;
            --verify)
                mode="verify"
                shift
                ;;
            --domain)
                domain="$2"
                shift 2
                ;;
            --email)
                email="$2"
                shift 2
                ;;
            --staging)
                staging="true"
                shift
                ;;
            --dry-run)
                dry_run="true"
                shift
                ;;
            --output-dir)
                output_dir="$2"
                shift 2
                ;;
            --help|-h)
                print_usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                print_usage
                exit 1
                ;;
        esac
    done

    # Execute based on mode
    case $mode in
        dev)
            generate_self_signed "$output_dir" "$COMMON_NAME" "$VALIDITY_DAYS"
            ;;
        prod)
            if [[ -z "$domain" ]]; then
                log_error "--domain is required for production certificates"
                print_usage
                exit 1
            fi
            if [[ -z "$email" ]]; then
                log_error "--email is required for Let's Encrypt"
                print_usage
                exit 1
            fi
            generate_letsencrypt "$domain" "$email" "$staging" "$dry_run" "$output_dir"
            ;;
        renew)
            renew_certificates "$output_dir" "$dry_run"
            ;;
        dhparam)
            create_directories "$output_dir"
            generate_dhparam "$output_dir"
            ;;
        verify)
            verify_certificate "${output_dir}/fullchain.pem"
            ;;
        *)
            log_error "No mode specified. Use --dev, --prod, --renew, or --dhparam"
            print_usage
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
