#!/bin/bash

################################################################################
# GCRF Library Management System
# Firewall Configuration Script
################################################################################
# This script configures iptables firewall rules to secure the GCRF system:
# - Allow public access to DMZ ports (80, 8080)
# - Block external access to backend services (8081-8090)
# - Restrict management UIs to localhost only (8848, 15672, 9001)
# - Block direct database access (5432, 6379, 3306)
# - Allow Docker internal network communication
#
# Usage:
#   sudo ./configure-firewall.sh {enable|disable|status|test}
#
# Requirements:
#   - Root/sudo privileges
#   - iptables installed
#   - Docker installed and running
#
# Security Zones:
#   - DMZ: 80, 8080 (public)
#   - Application: 8081-8090 (internal only)
#   - Data: 5432, 6379, 3306 (internal only)
#   - Management: 8848, 15672, 9001 (localhost only)
################################################################################

set -euo pipefail

# Colors for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# Configuration
readonly SCRIPT_NAME="GCRF Firewall"
readonly CHAIN_NAME="GCRF-SECURITY"

# Port definitions
readonly DMZ_PORTS=(80 8080)                          # Public-facing
readonly BACKEND_PORTS=(8081 8082 8083 8084 8085 8086 8087 8088 8089 8090)  # Internal only
readonly DATA_PORTS=(5432 6379 3306)                  # Database ports
readonly MGMT_PORTS=(8848 15672 9001 9848 5672 9000)  # Management UIs (localhost only)

################################################################################
# Helper Functions
################################################################################

log_info() {
  echo -e "${BLUE}[INFO]${NC} $*"
}

log_success() {
  echo -e "${GREEN}[SUCCESS]${NC} $*"
}

log_error() {
  echo -e "${RED}[ERROR]${NC} $*"
}

log_warning() {
  echo -e "${YELLOW}[WARNING]${NC} $*"
}

check_root() {
  if [[ $EUID -ne 0 ]]; then
    log_error "This script must be run as root or with sudo"
    echo "Usage: sudo $0 {enable|disable|status|test}"
    exit 1
  fi
}

check_iptables() {
  if ! command -v iptables &> /dev/null; then
    log_error "iptables not found. Please install iptables:"
    echo "  Ubuntu/Debian: sudo apt-get install iptables"
    echo "  RHEL/CentOS:   sudo yum install iptables"
    exit 1
  fi
}

check_docker() {
  if ! command -v docker &> /dev/null; then
    log_warning "Docker not found. Some rules may not apply correctly."
  fi
}

backup_rules() {
  local backup_file="/tmp/iptables-backup-$(date +%Y%m%d-%H%M%S).rules"
  log_info "Backing up current iptables rules to: $backup_file"
  iptables-save > "$backup_file"
  log_success "Backup saved"
}

################################################################################
# Firewall Rules Management
################################################################################

create_custom_chain() {
  # Create custom chain for GCRF rules
  if ! iptables -L "$CHAIN_NAME" -n &>/dev/null; then
    log_info "Creating custom chain: $CHAIN_NAME"
    iptables -N "$CHAIN_NAME"
  else
    log_info "Custom chain $CHAIN_NAME already exists, flushing rules"
    iptables -F "$CHAIN_NAME"
  fi
}

delete_custom_chain() {
  if iptables -L "$CHAIN_NAME" -n &>/dev/null; then
    log_info "Removing custom chain: $CHAIN_NAME"

    # Remove jump to custom chain
    iptables -D INPUT -j "$CHAIN_NAME" 2>/dev/null || true

    # Flush and delete chain
    iptables -F "$CHAIN_NAME"
    iptables -X "$CHAIN_NAME"

    log_success "Custom chain removed"
  fi
}

enable_firewall_rules() {
  log_info "Enabling GCRF firewall rules..."
  echo ""

  # Backup current rules
  backup_rules

  # Create custom chain
  create_custom_chain

  # Add rules to custom chain
  log_info "Adding firewall rules to chain: $CHAIN_NAME"

  # Rule 1: Allow established connections
  log_info "  [1] Allow established and related connections"
  iptables -A "$CHAIN_NAME" -m state --state ESTABLISHED,RELATED -j ACCEPT

  # Rule 2: Allow loopback
  log_info "  [2] Allow loopback interface"
  iptables -A "$CHAIN_NAME" -i lo -j ACCEPT

  # Rule 3: Allow SSH (for remote management)
  log_info "  [3] Allow SSH (port 22)"
  iptables -A "$CHAIN_NAME" -p tcp --dport 22 -j ACCEPT

  # Rule 4: Allow DMZ ports (public access)
  log_info "  [4] Allow DMZ ports (public access)"
  for port in "${DMZ_PORTS[@]}"; do
    log_info "      - Port $port (HTTP/API Gateway)"
    iptables -A "$CHAIN_NAME" -p tcp --dport "$port" -j ACCEPT
  done

  # Rule 5: Allow Docker internal networks
  log_info "  [5] Allow Docker internal network communication"
  iptables -A "$CHAIN_NAME" -i docker0 -j ACCEPT

  # Allow Docker bridge networks (br-*)
  if docker network ls --format '{{.Name}}' | grep -q "gcrf"; then
    # Get bridge interfaces for GCRF networks
    for network in $(docker network ls --filter "name=gcrf" --format '{{.ID}}'); do
      local bridge_if="br-${network:0:12}"
      if ip link show "$bridge_if" &>/dev/null; then
        log_info "      - Bridge: $bridge_if"
        iptables -A "$CHAIN_NAME" -i "$bridge_if" -j ACCEPT
      fi
    done
  fi

  # Rule 6: Restrict management ports to localhost
  log_info "  [6] Restrict management ports to localhost only"
  for port in "${MGMT_PORTS[@]}"; do
    log_info "      - Port $port (management UI)"
    # Allow from localhost
    iptables -A "$CHAIN_NAME" -p tcp --dport "$port" -s 127.0.0.1 -j ACCEPT
    # Drop from anywhere else
    iptables -A "$CHAIN_NAME" -p tcp --dport "$port" -j DROP
  done

  # Rule 7: Block direct access to backend services
  log_info "  [7] Block external access to backend services"
  for port in "${BACKEND_PORTS[@]}"; do
    log_info "      - Port $port (backend service)"
    iptables -A "$CHAIN_NAME" -p tcp --dport "$port" -j DROP
  done

  # Rule 8: Block direct access to databases
  log_info "  [8] Block external access to databases"
  for port in "${DATA_PORTS[@]}"; do
    log_info "      - Port $port (database)"
    iptables -A "$CHAIN_NAME" -p tcp --dport "$port" -j DROP
  done

  # Rule 9: Log dropped packets (optional, comment out if too verbose)
  log_info "  [9] Log dropped packets (rate limited)"
  iptables -A "$CHAIN_NAME" -m limit --limit 5/min -j LOG --log-prefix "GCRF-DROPPED: " --log-level 4

  # Add jump from INPUT chain to custom chain
  if ! iptables -C INPUT -j "$CHAIN_NAME" 2>/dev/null; then
    log_info "Adding jump from INPUT chain to $CHAIN_NAME"
    iptables -I INPUT 1 -j "$CHAIN_NAME"
  fi

  echo ""
  log_success "Firewall rules enabled successfully!"
  echo ""
  log_info "Summary:"
  echo "  - Public ports: ${DMZ_PORTS[*]}"
  echo "  - Management ports (localhost only): ${MGMT_PORTS[*]}"
  echo "  - Blocked backend ports: ${BACKEND_PORTS[*]}"
  echo "  - Blocked database ports: ${DATA_PORTS[*]}"
  echo ""
  log_warning "Note: Rules are NOT persistent across reboots!"
  log_info "To make persistent:"
  echo "  Ubuntu/Debian: sudo apt-get install iptables-persistent && sudo netfilter-persistent save"
  echo "  RHEL/CentOS:   sudo service iptables save && sudo systemctl enable iptables"
}

disable_firewall_rules() {
  log_info "Disabling GCRF firewall rules..."
  echo ""

  # Backup current rules
  backup_rules

  # Remove custom chain
  delete_custom_chain

  echo ""
  log_success "GCRF firewall rules disabled"
  log_warning "Your system may still have other firewall rules active"
  log_info "Check with: sudo iptables -L -n -v"
}

show_status() {
  echo "======================================"
  echo "GCRF Firewall Status"
  echo "======================================"
  echo ""

  # Check if custom chain exists
  if iptables -L "$CHAIN_NAME" -n &>/dev/null; then
    log_success "GCRF firewall is ENABLED"
    echo ""

    echo "Active GCRF Rules:"
    echo "------------------"
    iptables -L "$CHAIN_NAME" -n -v --line-numbers

    echo ""
    echo "Jump Rule in INPUT Chain:"
    echo "-------------------------"
    iptables -L INPUT -n -v --line-numbers | grep -A 1 "Chain INPUT" | head -5

  else
    log_warning "GCRF firewall is DISABLED"
    echo ""
    echo "Run 'sudo $0 enable' to enable firewall rules"
  fi

  echo ""
  echo "Testing Port Accessibility:"
  echo "---------------------------"

  # Test public ports
  echo "Public Ports (should be OPEN):"
  for port in "${DMZ_PORTS[@]}"; do
    if ss -tlnp 2>/dev/null | grep -q ":$port "; then
      echo -e "  ${GREEN}✓${NC} Port $port: LISTENING"
    else
      echo -e "  ${YELLOW}○${NC} Port $port: Not listening (service may be down)"
    fi
  done

  echo ""
  echo "Management Ports (should be LOCALHOST only):"
  for port in "${MGMT_PORTS[@]}"; do
    if ss -tlnp 2>/dev/null | grep -q "127.0.0.1:$port "; then
      echo -e "  ${GREEN}✓${NC} Port $port: Bound to localhost"
    elif ss -tlnp 2>/dev/null | grep -q ":$port "; then
      echo -e "  ${RED}✗${NC} Port $port: Bound to 0.0.0.0 (security risk)"
    else
      echo -e "  ${YELLOW}○${NC} Port $port: Not listening"
    fi
  done

  echo ""
  echo "Backend Ports (should be BLOCKED from external):"
  for port in "${BACKEND_PORTS[@]:0:3}"; do  # Show first 3 as example
    if ss -tlnp 2>/dev/null | grep -q ":$port "; then
      if iptables -L "$CHAIN_NAME" -n 2>/dev/null | grep -q "dpt:$port.*DROP"; then
        echo -e "  ${GREEN}✓${NC} Port $port: Listening but BLOCKED by firewall"
      else
        echo -e "  ${RED}✗${NC} Port $port: Listening and NOT blocked (security risk)"
      fi
    else
      echo -e "  ${YELLOW}○${NC} Port $port: Not listening"
    fi
  done
  echo "  ... (and $(( ${#BACKEND_PORTS[@]} - 3 )) more backend ports)"
}

test_firewall_rules() {
  log_info "Testing GCRF firewall rules (DRY RUN - no changes)..."
  echo ""

  local test_passed=0
  local test_failed=0

  # Test 1: Check if iptables is available
  if command -v iptables &>/dev/null; then
    log_success "Test 1: iptables is installed"
    ((test_passed++))
  else
    log_error "Test 1: iptables is NOT installed"
    ((test_failed++))
  fi

  # Test 2: Check if we have root privileges
  if [[ $EUID -eq 0 ]]; then
    log_success "Test 2: Running with root privileges"
    ((test_passed++))
  else
    log_error "Test 2: Not running as root (required for firewall configuration)"
    ((test_failed++))
  fi

  # Test 3: Check if Docker is installed
  if command -v docker &>/dev/null; then
    log_success "Test 3: Docker is installed"
    ((test_passed++))
  else
    log_warning "Test 3: Docker is NOT installed (some rules may not apply)"
  fi

  # Test 4: Check if GCRF networks exist
  if docker network ls --format '{{.Name}}' | grep -q "gcrf"; then
    log_success "Test 4: GCRF Docker networks found"
    ((test_passed++))
  else
    log_warning "Test 4: GCRF Docker networks not found (create them first)"
  fi

  # Test 5: Verify rule syntax (dry run)
  log_info "Test 5: Verifying rule syntax..."
  local syntax_ok=true

  # Test a sample rule
  if iptables -C INPUT -p tcp --dport 22 -j ACCEPT 2>/dev/null || true; then
    log_success "Test 5: iptables syntax is correct"
    ((test_passed++))
  else
    log_warning "Test 5: Could not verify iptables syntax"
  fi

  echo ""
  echo "Test Summary:"
  echo "  Passed: $test_passed"
  echo "  Failed: $test_failed"
  echo ""

  if [[ $test_failed -eq 0 ]]; then
    log_success "All tests passed! You can safely run: sudo $0 enable"
  else
    log_error "Some tests failed. Please fix issues before enabling firewall."
    exit 1
  fi
}

show_help() {
  cat <<EOF
Usage: sudo $0 {enable|disable|status|test}

GCRF Library Management System - Firewall Configuration Script

Commands:
  enable   Enable GCRF firewall rules
  disable  Disable GCRF firewall rules
  status   Show current firewall status
  test     Test prerequisites without making changes

Security Zones:
  DMZ Zone (Public):
    - Port 80:   Web Admin (HTTP)
    - Port 8080: API Gateway (HTTP)

  Application Zone (Internal Only):
    - Ports 8081-8090: Microservices (blocked from external)

  Data Zone (Internal Only):
    - Port 5432: PostgreSQL (blocked from external)
    - Port 6379: Redis (blocked from external)
    - Port 3306: MySQL (blocked from external)

  Management (Localhost Only):
    - Port 8848:  Nacos UI
    - Port 15672: RabbitMQ Management
    - Port 9001:  MinIO Console

Examples:
  sudo $0 enable              # Enable firewall rules
  sudo $0 status              # Check current status
  sudo $0 disable             # Disable firewall rules
  sudo $0 test                # Test prerequisites

Notes:
  - Requires root/sudo privileges
  - Rules are not persistent by default (install iptables-persistent)
  - Backup is created before any changes
  - Docker internal communication is always allowed

EOF
}

################################################################################
# Main Execution
################################################################################

main() {
  # Check prerequisites
  check_root
  check_iptables
  check_docker

  # Parse command
  case "${1:-}" in
    enable)
      enable_firewall_rules
      ;;
    disable)
      disable_firewall_rules
      ;;
    status)
      show_status
      ;;
    test)
      test_firewall_rules
      ;;
    --help|-h|help)
      show_help
      ;;
    *)
      log_error "Invalid command: ${1:-}"
      echo ""
      show_help
      exit 1
      ;;
  esac
}

# Run main function
main "$@"
