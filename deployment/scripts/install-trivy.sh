#!/bin/bash

###############################################################################
# GCRF Library Management System - Trivy Installation Script
# Version: 1.0.0
# Description: Install and configure Trivy security scanner for container scanning
# Author: GCRF Security Team
# Date: 2025-11-01
###############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
TRIVY_VERSION="0.56.2"
INSTALL_DIR="/usr/local/bin"
CACHE_DIR="$HOME/.cache/trivy"
DB_REPOSITORY="ghcr.io/aquasecurity/trivy-db"
JAVA_DB_REPOSITORY="ghcr.io/aquasecurity/trivy-java-db"

# Function to print colored messages
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to check if running as root
check_root() {
    if [[ $EUID -eq 0 ]]; then
        print_message "$YELLOW" "⚠️  Running as root is not recommended. Consider running as a regular user."
    fi
}

# Function to detect OS
detect_os() {
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        OS="linux"
        if [[ -f /etc/os-release ]]; then
            . /etc/os-release
            DISTRO=$ID
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        OS="macos"
    else
        print_message "$RED" "❌ Unsupported OS: $OSTYPE"
        exit 1
    fi
    print_message "$BLUE" "🔍 Detected OS: $OS"
}

# Function to check prerequisites
check_prerequisites() {
    print_message "$BLUE" "📋 Checking prerequisites..."

    local missing_tools=()

    # Check for required tools
    for tool in curl tar; do
        if ! command -v $tool &> /dev/null; then
            missing_tools+=($tool)
        fi
    done

    if [[ ${#missing_tools[@]} -gt 0 ]]; then
        print_message "$RED" "❌ Missing required tools: ${missing_tools[@]}"
        print_message "$YELLOW" "Please install missing tools and run again."
        exit 1
    fi

    print_message "$GREEN" "✅ All prerequisites met"
}

# Function to install Trivy on macOS
install_macos() {
    print_message "$BLUE" "🍎 Installing Trivy on macOS..."

    # Check if Homebrew is installed
    if command -v brew &> /dev/null; then
        print_message "$BLUE" "📦 Installing via Homebrew..."
        brew install aquasecurity/trivy/trivy
    else
        print_message "$YELLOW" "⚠️  Homebrew not found. Installing via binary..."
        install_binary "darwin" "arm64"
    fi
}

# Function to install Trivy on Linux
install_linux() {
    print_message "$BLUE" "🐧 Installing Trivy on Linux..."

    case "$DISTRO" in
        ubuntu|debian)
            install_debian
            ;;
        centos|rhel|fedora)
            install_rpm
            ;;
        *)
            print_message "$YELLOW" "⚠️  Unknown distribution. Installing via binary..."
            install_binary "linux" "amd64"
            ;;
    esac
}

# Function to install on Debian-based systems
install_debian() {
    print_message "$BLUE" "📦 Installing via APT..."

    # Add Trivy repository
    sudo apt-get update
    sudo apt-get install -y wget apt-transport-https gnupg lsb-release
    wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | sudo apt-key add -
    echo "deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main" | sudo tee -a /etc/apt/sources.list.d/trivy.list

    # Install Trivy
    sudo apt-get update
    sudo apt-get install -y trivy
}

# Function to install on RPM-based systems
install_rpm() {
    print_message "$BLUE" "📦 Installing via YUM/DNF..."

    # Add Trivy repository
    RELEASE_VERSION=$(grep '^VERSION_ID=' /etc/os-release | cut -d'=' -f2 | tr -d '"')
    cat << EOF | sudo tee /etc/yum.repos.d/trivy.repo
[trivy]
name=Trivy repository
baseurl=https://aquasecurity.github.io/trivy-repo/rpm/releases/\$releasever/\$basearch/
gpgcheck=0
enabled=1
EOF

    # Install Trivy
    sudo yum -y update
    sudo yum -y install trivy
}

# Function to install Trivy via binary
install_binary() {
    local os=$1
    local arch=$2

    print_message "$BLUE" "⬇️  Downloading Trivy binary..."

    # Construct download URL
    local url="https://github.com/aquasecurity/trivy/releases/download/v${TRIVY_VERSION}/trivy_${TRIVY_VERSION}_${os^}-${arch}.tar.gz"

    # Create temporary directory
    local tmp_dir=$(mktemp -d)
    cd "$tmp_dir"

    # Download and extract
    print_message "$BLUE" "📥 Downloading from: $url"
    curl -sfL "$url" -o trivy.tar.gz
    tar -xzf trivy.tar.gz

    # Install binary
    if [[ -w "$INSTALL_DIR" ]]; then
        mv trivy "$INSTALL_DIR/"
    else
        print_message "$YELLOW" "⚠️  Need sudo to install to $INSTALL_DIR"
        sudo mv trivy "$INSTALL_DIR/"
    fi

    # Cleanup
    cd - > /dev/null
    rm -rf "$tmp_dir"

    print_message "$GREEN" "✅ Binary installed to $INSTALL_DIR/trivy"
}

# Function to verify installation
verify_installation() {
    print_message "$BLUE" "🔍 Verifying installation..."

    if ! command -v trivy &> /dev/null; then
        print_message "$RED" "❌ Trivy installation failed"
        exit 1
    fi

    local installed_version=$(trivy version --format json | grep -o '"Version":"[^"]*' | sed 's/"Version":"//')
    print_message "$GREEN" "✅ Trivy installed successfully"
    print_message "$BLUE" "📌 Version: $installed_version"
}

# Function to initialize vulnerability database
init_database() {
    print_message "$BLUE" "🗄️  Initializing vulnerability database..."

    # Create cache directory
    mkdir -p "$CACHE_DIR"

    # Download vulnerability database
    print_message "$BLUE" "📥 Downloading vulnerability database..."
    trivy image --download-db-only

    # Download Java database for better Java vulnerability detection
    print_message "$BLUE" "☕ Downloading Java vulnerability database..."
    trivy image --download-java-db-only

    print_message "$GREEN" "✅ Vulnerability databases initialized"
}

# Function to configure Trivy
configure_trivy() {
    print_message "$BLUE" "⚙️  Configuring Trivy..."

    # Create configuration directory
    local config_dir="$HOME/.trivy"
    mkdir -p "$config_dir"

    # Create basic configuration
    cat > "$config_dir/trivy.yaml" << EOF
# Trivy Configuration for GCRF Library Management System
cache:
  backend: "fs"
  cache_dir: "$CACHE_DIR"

db:
  repository: "$DB_REPOSITORY"
  java-repository: "$JAVA_DB_REPOSITORY"

timeout: 10m0s

# Severity levels to detect
severity:
  - CRITICAL
  - HIGH
  - MEDIUM
  - LOW
  - UNKNOWN

# Vulnerability database update settings
skip-db-update: false
skip-java-db-update: false

# Report settings
format: table
dependency-tree: true

# Scan settings
scanners:
  - vuln
  - secret
  - misconfig
  - license
EOF

    print_message "$GREEN" "✅ Configuration created at $config_dir/trivy.yaml"
}

# Function to create useful aliases
create_aliases() {
    print_message "$BLUE" "🔧 Creating useful aliases..."

    local shell_rc=""
    if [[ "$SHELL" == *"zsh"* ]]; then
        shell_rc="$HOME/.zshrc"
    elif [[ "$SHELL" == *"bash"* ]]; then
        shell_rc="$HOME/.bashrc"
    fi

    if [[ -n "$shell_rc" ]]; then
        cat >> "$shell_rc" << 'EOF'

# Trivy aliases for GCRF project
alias trivy-scan='trivy image --severity CRITICAL,HIGH'
alias trivy-scan-all='trivy image'
alias trivy-update='trivy image --download-db-only && trivy image --download-java-db-only'
alias trivy-sbom='trivy image --format spdx-json'
alias trivy-report='trivy image --format json --output'
EOF
        print_message "$GREEN" "✅ Aliases added to $shell_rc"
        print_message "$YELLOW" "⚠️  Please run 'source $shell_rc' or restart your terminal"
    fi
}

# Function to display post-installation information
post_install_info() {
    print_message "$GREEN" "\n🎉 Trivy installation completed successfully!"
    print_message "$BLUE" "\n📚 Quick Start Commands:"
    echo "  trivy image <image:tag>              # Basic scan"
    echo "  trivy image --severity HIGH,CRITICAL <image:tag>  # Scan for high/critical only"
    echo "  trivy image --format json -o report.json <image:tag>  # Generate JSON report"
    echo "  trivy image --format sarif -o report.sarif <image:tag>  # Generate SARIF report"
    echo "  trivy sbom <image:tag>               # Generate SBOM"
    echo ""
    print_message "$BLUE" "🔍 Test with GCRF images:"
    echo "  trivy image gcrf-library-web-admin:latest"
    echo "  trivy image gcrf-web-backend:latest"
    echo ""
    print_message "$BLUE" "📖 Documentation:"
    echo "  Official docs: https://aquasecurity.github.io/trivy/"
    echo "  GCRF security docs: deployment/docs/SECURITY_SCANNING.md"
}

# Main installation flow
main() {
    print_message "$BLUE" "==============================================="
    print_message "$BLUE" "  GCRF Library - Trivy Security Scanner Setup"
    print_message "$BLUE" "===============================================\n"

    check_root
    detect_os
    check_prerequisites

    # Install Trivy based on OS
    if [[ "$OS" == "macos" ]]; then
        install_macos
    else
        install_linux
    fi

    # Verify and configure
    verify_installation
    init_database
    configure_trivy
    create_aliases

    # Display completion message
    post_install_info
}

# Run main function
main "$@"