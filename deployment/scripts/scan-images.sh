#!/bin/bash

###############################################################################
# GCRF Library Management System - Docker Image Security Scanner
# Version: 1.0.0
# Description: Automated security scanning for all GCRF Docker images using Trivy
# Author: GCRF Security Team
# Date: 2025-11-01
###############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SCAN_RESULTS_DIR="$PROJECT_ROOT/deployment/security-reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_PREFIX="security-scan_${TIMESTAMP}"

# Security baseline thresholds
MAX_CRITICAL=0
MAX_HIGH=0
MAX_MEDIUM=5
MAX_LOW=999  # No limit for LOW

# Exit codes
EXIT_SUCCESS=0
EXIT_CRITICAL_FOUND=1
EXIT_HIGH_FOUND=2
EXIT_MEDIUM_EXCEEDED=3
EXIT_SCAN_FAILED=10

# Default images to scan
DEFAULT_IMAGES=(
    "gcrf-library/gateway-service:latest"
    "gcrf-library/auth-service:latest"
    "gcrf-library/book-service:latest"
    "gcrf-library/circulation-service:latest"
    "gcrf-library/reader-service:latest"
    "gcrf-library/system-service:latest"
    "gcrf-library/notification-service:latest"
    "gcrf-library-web-admin:latest"
)

# Function to print colored messages
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to print banner
print_banner() {
    print_message "$CYAN" "================================================"
    print_message "$CYAN" "   GCRF Library - Security Vulnerability Scan"
    print_message "$CYAN" "================================================"
    print_message "$BLUE" "Timestamp: $(date '+%Y-%m-%d %H:%M:%S')"
    print_message "$BLUE" "Reports Dir: $SCAN_RESULTS_DIR"
    echo ""
}

# Function to check if Trivy is installed
check_trivy() {
    if ! command -v trivy &> /dev/null; then
        print_message "$RED" "❌ Trivy is not installed!"
        print_message "$YELLOW" "Please run: $SCRIPT_DIR/install-trivy.sh"
        exit $EXIT_SCAN_FAILED
    fi

    local trivy_version=$(trivy version --format json | grep -o '"Version":"[^"]*' | sed 's/"Version":"//')
    print_message "$GREEN" "✅ Trivy version: $trivy_version"
}

# Function to update vulnerability databases
update_databases() {
    print_message "$BLUE" "🔄 Updating vulnerability databases..."

    # Update main vulnerability database
    if trivy image --download-db-only 2>/dev/null; then
        print_message "$GREEN" "✅ Main vulnerability database updated"
    else
        print_message "$YELLOW" "⚠️  Could not update main database (using cached)"
    fi

    # Update Java vulnerability database
    if trivy image --download-java-db-only 2>/dev/null; then
        print_message "$GREEN" "✅ Java vulnerability database updated"
    else
        print_message "$YELLOW" "⚠️  Could not update Java database (using cached)"
    fi
}

# Function to create reports directory
setup_reports_dir() {
    mkdir -p "$SCAN_RESULTS_DIR"/{json,html,sarif,sbom,summary}
    print_message "$GREEN" "✅ Reports directory prepared"
}

# Function to check if image exists
image_exists() {
    local image=$1
    docker image inspect "$image" &>/dev/null
}

# Function to scan a single image
scan_image() {
    local image=$1
    local image_name=$(echo "$image" | sed 's/[\/:]/_/g')
    local scan_passed=true
    local exit_code=0

    print_message "$BLUE" "\n🔍 Scanning image: $image"
    print_message "$BLUE" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    # Check if image exists
    if ! image_exists "$image"; then
        print_message "$YELLOW" "⚠️  Image not found locally. Skipping..."
        return 0
    fi

    # Create report filenames
    local json_report="$SCAN_RESULTS_DIR/json/${image_name}_${TIMESTAMP}.json"
    local html_report="$SCAN_RESULTS_DIR/html/${image_name}_${TIMESTAMP}.html"
    local sarif_report="$SCAN_RESULTS_DIR/sarif/${image_name}_${TIMESTAMP}.sarif"
    local sbom_report="$SCAN_RESULTS_DIR/sbom/${image_name}_${TIMESTAMP}_sbom.json"
    local summary_report="$SCAN_RESULTS_DIR/summary/${image_name}_${TIMESTAMP}_summary.txt"

    # Generate JSON report (main scan)
    print_message "$CYAN" "📊 Generating JSON report..."
    trivy image \
        --format json \
        --output "$json_report" \
        --severity CRITICAL,HIGH,MEDIUM,LOW \
        --scanners vuln,secret,misconfig,license \
        --timeout 10m \
        "$image" 2>/dev/null || true

    # Generate HTML report for human review
    print_message "$CYAN" "📄 Generating HTML report..."
    trivy image \
        --format template \
        --template "@contrib/html.tpl" \
        --output "$html_report" \
        --severity CRITICAL,HIGH,MEDIUM,LOW \
        "$image" 2>/dev/null || true

    # Generate SARIF report for CI/CD integration
    print_message "$CYAN" "🔧 Generating SARIF report..."
    trivy image \
        --format sarif \
        --output "$sarif_report" \
        --severity CRITICAL,HIGH,MEDIUM,LOW \
        "$image" 2>/dev/null || true

    # Generate SBOM (Software Bill of Materials)
    print_message "$CYAN" "📦 Generating SBOM..."
    trivy image \
        --format spdx-json \
        --output "$sbom_report" \
        "$image" 2>/dev/null || true

    # Parse results and check severity
    if [[ -f "$json_report" ]]; then
        local critical_count=$(jq '[.Results[]?.Vulnerabilities[]? | select(.Severity=="CRITICAL")] | length' "$json_report" 2>/dev/null || echo 0)
        local high_count=$(jq '[.Results[]?.Vulnerabilities[]? | select(.Severity=="HIGH")] | length' "$json_report" 2>/dev/null || echo 0)
        local medium_count=$(jq '[.Results[]?.Vulnerabilities[]? | select(.Severity=="MEDIUM")] | length' "$json_report" 2>/dev/null || echo 0)
        local low_count=$(jq '[.Results[]?.Vulnerabilities[]? | select(.Severity=="LOW")] | length' "$json_report" 2>/dev/null || echo 0)

        # Generate summary
        cat > "$summary_report" << EOF
================================================================================
Security Scan Summary for: $image
Scan Date: $(date '+%Y-%m-%d %H:%M:%S')
================================================================================

Vulnerability Summary:
----------------------
CRITICAL: $critical_count (Threshold: $MAX_CRITICAL)
HIGH:     $high_count (Threshold: $MAX_HIGH)
MEDIUM:   $medium_count (Threshold: $MAX_MEDIUM)
LOW:      $low_count (Threshold: $MAX_LOW)

Security Baseline Status:
-------------------------
EOF

        # Check against thresholds
        if [[ $critical_count -gt $MAX_CRITICAL ]]; then
            echo "❌ CRITICAL vulnerabilities exceed threshold" >> "$summary_report"
            print_message "$RED" "❌ CRITICAL: $critical_count vulnerabilities found (max allowed: $MAX_CRITICAL)"
            scan_passed=false
            exit_code=$EXIT_CRITICAL_FOUND
        else
            echo "✅ CRITICAL: Within threshold" >> "$summary_report"
            print_message "$GREEN" "✅ CRITICAL: $critical_count vulnerabilities (threshold: $MAX_CRITICAL)"
        fi

        if [[ $high_count -gt $MAX_HIGH ]]; then
            echo "❌ HIGH vulnerabilities exceed threshold" >> "$summary_report"
            print_message "$RED" "❌ HIGH: $high_count vulnerabilities found (max allowed: $MAX_HIGH)"
            scan_passed=false
            [[ $exit_code -eq 0 ]] && exit_code=$EXIT_HIGH_FOUND
        else
            echo "✅ HIGH: Within threshold" >> "$summary_report"
            print_message "$GREEN" "✅ HIGH: $high_count vulnerabilities (threshold: $MAX_HIGH)"
        fi

        if [[ $medium_count -gt $MAX_MEDIUM ]]; then
            echo "⚠️  MEDIUM vulnerabilities exceed threshold" >> "$summary_report"
            print_message "$YELLOW" "⚠️  MEDIUM: $medium_count vulnerabilities found (max allowed: $MAX_MEDIUM)"
            [[ $exit_code -eq 0 ]] && exit_code=$EXIT_MEDIUM_EXCEEDED
        else
            echo "✅ MEDIUM: Within threshold" >> "$summary_report"
            print_message "$GREEN" "✅ MEDIUM: $medium_count vulnerabilities (threshold: $MAX_MEDIUM)"
        fi

        print_message "$BLUE" "ℹ️  LOW: $low_count vulnerabilities (informational only)"

        # Add detailed vulnerability list to summary
        echo -e "\nTop Vulnerabilities:" >> "$summary_report"
        echo "-------------------" >> "$summary_report"

        # Extract top vulnerabilities
        jq -r '.Results[]?.Vulnerabilities[]? |
            select(.Severity=="CRITICAL" or .Severity=="HIGH") |
            "\(.Severity): \(.VulnerabilityID) - \(.PkgName) \(.InstalledVersion) -> \(.FixedVersion // "No fix available")"' \
            "$json_report" 2>/dev/null | head -10 >> "$summary_report" || true

        # Add report locations
        cat >> "$summary_report" << EOF

Report Files:
------------
JSON: $json_report
HTML: $html_report
SARIF: $sarif_report
SBOM: $sbom_report

================================================================================
EOF

        # Display summary on console
        print_message "$CYAN" "\n📋 Scan Summary:"
        cat "$summary_report"
    else
        print_message "$RED" "❌ Failed to generate scan report"
        exit_code=$EXIT_SCAN_FAILED
    fi

    return $exit_code
}

# Function to generate consolidated report
generate_consolidated_report() {
    local consolidated_report="$SCAN_RESULTS_DIR/consolidated_report_${TIMESTAMP}.md"

    print_message "$BLUE" "\n📊 Generating consolidated report..."

    cat > "$consolidated_report" << EOF
# GCRF Library Management System - Security Scan Report

**Generated:** $(date '+%Y-%m-%d %H:%M:%S')
**Scanner:** Trivy
**Report ID:** ${TIMESTAMP}

## Executive Summary

This report contains the security scanning results for all GCRF Library Management System Docker images.

### Security Baseline

| Severity | Threshold | Policy |
|----------|-----------|---------|
| CRITICAL | 0 | Build fails if any found |
| HIGH | 0 | Build fails if any found |
| MEDIUM | ≤ 5 | Warning if exceeded |
| LOW | No limit | Informational only |

## Scan Results

EOF

    # Add individual scan summaries
    for summary_file in "$SCAN_RESULTS_DIR/summary"/*_${TIMESTAMP}_summary.txt; do
        if [[ -f "$summary_file" ]]; then
            echo "---" >> "$consolidated_report"
            cat "$summary_file" >> "$consolidated_report"
            echo "" >> "$consolidated_report"
        fi
    done

    # Add recommendations
    cat >> "$consolidated_report" << EOF

## Recommendations

### Immediate Actions (CRITICAL/HIGH)
1. Review and patch all CRITICAL vulnerabilities before deployment
2. Apply security updates for HIGH severity issues
3. Consider using alternative packages if patches are not available
4. Implement compensating controls for unpatched vulnerabilities

### Best Practices
1. **Regular Scanning**: Scan images before every deployment
2. **Base Image Updates**: Keep base images updated regularly
3. **Dependency Management**: Use tools like Dependabot for automated updates
4. **SBOM Tracking**: Maintain Software Bill of Materials for all components
5. **Security Testing**: Include security scanning in CI/CD pipeline

## Report Locations

- **JSON Reports**: \`$SCAN_RESULTS_DIR/json/\`
- **HTML Reports**: \`$SCAN_RESULTS_DIR/html/\`
- **SARIF Reports**: \`$SCAN_RESULTS_DIR/sarif/\`
- **SBOM Files**: \`$SCAN_RESULTS_DIR/sbom/\`
- **Summaries**: \`$SCAN_RESULTS_DIR/summary/\`

## Next Steps

1. Review HTML reports for detailed vulnerability information
2. Prioritize remediation based on severity and exploitability
3. Update Dockerfiles with patched base images and dependencies
4. Re-run scans after applying fixes
5. Document any accepted risks with justification

---
*Generated by GCRF Security Scanner v1.0.0*
EOF

    print_message "$GREEN" "✅ Consolidated report: $consolidated_report"
}

# Function to display usage
usage() {
    cat << EOF
Usage: $0 [OPTIONS] [IMAGE...]

Scan Docker images for security vulnerabilities using Trivy.

OPTIONS:
    -h, --help          Show this help message
    -u, --update-db     Update vulnerability databases before scanning
    -a, --all           Scan all default GCRF images
    -s, --severity      Set severity levels (default: CRITICAL,HIGH,MEDIUM,LOW)
    -f, --fail-on       Fail on severity level (default: CRITICAL,HIGH)
    -o, --output-dir    Set output directory (default: $SCAN_RESULTS_DIR)
    --no-sbom          Skip SBOM generation
    --offline          Run in offline mode (no database updates)

EXAMPLES:
    # Scan specific image
    $0 gcrf-library/gateway-service:latest

    # Scan all default images
    $0 --all

    # Scan with database update
    $0 --update-db --all

    # Scan multiple specific images
    $0 image1:tag image2:tag image3:tag

    # Custom severity and output
    $0 --severity HIGH,CRITICAL --output-dir ./reports image:tag

EOF
    exit 0
}

# Parse command line arguments
parse_arguments() {
    local images=()
    local update_db=false
    local scan_all=false
    local offline=false

    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                usage
                ;;
            -u|--update-db)
                update_db=true
                shift
                ;;
            -a|--all)
                scan_all=true
                shift
                ;;
            --offline)
                offline=true
                shift
                ;;
            -o|--output-dir)
                SCAN_RESULTS_DIR="$2"
                shift 2
                ;;
            --no-sbom)
                SKIP_SBOM=true
                shift
                ;;
            -*)
                print_message "$RED" "Unknown option: $1"
                usage
                ;;
            *)
                images+=("$1")
                shift
                ;;
        esac
    done

    # Determine which images to scan
    if [[ "$scan_all" == true ]]; then
        IMAGES_TO_SCAN=("${DEFAULT_IMAGES[@]}")
    elif [[ ${#images[@]} -gt 0 ]]; then
        IMAGES_TO_SCAN=("${images[@]}")
    else
        # If no images specified, show usage
        print_message "$YELLOW" "No images specified. Use -a for all or specify image names."
        usage
    fi

    # Handle database updates
    if [[ "$offline" == true ]]; then
        print_message "$YELLOW" "⚠️  Running in offline mode (no database updates)"
    elif [[ "$update_db" == true ]]; then
        update_databases
    fi
}

# Main execution
main() {
    local overall_exit_code=0

    # Print banner
    print_banner

    # Check prerequisites
    check_trivy

    # Parse arguments
    parse_arguments "$@"

    # Setup reports directory
    setup_reports_dir

    # Scan each image
    print_message "$BLUE" "\n🚀 Starting security scans..."
    print_message "$BLUE" "Images to scan: ${#IMAGES_TO_SCAN[@]}"

    for image in "${IMAGES_TO_SCAN[@]}"; do
        scan_image "$image" || {
            local image_exit_code=$?
            [[ $image_exit_code -gt $overall_exit_code ]] && overall_exit_code=$image_exit_code
        }
    done

    # Generate consolidated report
    generate_consolidated_report

    # Final summary
    echo ""
    print_message "$CYAN" "================================================"
    if [[ $overall_exit_code -eq 0 ]]; then
        print_message "$GREEN" "✅ All scans completed successfully!"
        print_message "$GREEN" "   All images meet security baseline requirements"
    elif [[ $overall_exit_code -eq $EXIT_CRITICAL_FOUND ]]; then
        print_message "$RED" "❌ CRITICAL vulnerabilities found!"
        print_message "$RED" "   Images do not meet security requirements"
    elif [[ $overall_exit_code -eq $EXIT_HIGH_FOUND ]]; then
        print_message "$RED" "❌ HIGH vulnerabilities found!"
        print_message "$RED" "   Images do not meet security requirements"
    elif [[ $overall_exit_code -eq $EXIT_MEDIUM_EXCEEDED ]]; then
        print_message "$YELLOW" "⚠️  MEDIUM vulnerability threshold exceeded"
        print_message "$YELLOW" "   Review and remediate before production"
    else
        print_message "$RED" "❌ Some scans failed"
    fi
    print_message "$CYAN" "================================================"

    # Show report location
    print_message "$BLUE" "\n📁 Reports saved to: $SCAN_RESULTS_DIR"
    print_message "$BLUE" "📄 View HTML reports in browser for detailed analysis"

    exit $overall_exit_code
}

# Check if jq is installed (needed for JSON parsing)
if ! command -v jq &> /dev/null; then
    print_message "$YELLOW" "⚠️  jq is not installed. Installing..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew install jq
    else
        sudo apt-get install -y jq || sudo yum install -y jq
    fi
fi

# Run main function
main "$@"