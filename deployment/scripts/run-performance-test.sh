#!/bin/bash

#===============================================================================
# GCRF Library Management System - Performance Test Runner
#===============================================================================
# Description: Execute k6 performance tests with result collection
# Version: 1.0.0
# Last Updated: 2025-12-01
#
# Usage:
#   ./run-performance-test.sh <test-type> [options]
#
# Test Types:
#   load    - Basic load test (100 VUs, 5 min)
#   stress  - Stress test (up to 500 VUs)
#   spike   - Spike test (sudden burst to 1000 VUs)
#   soak    - Soak test (50 VUs, 1 hour)
#
# Options:
#   -u, --url       Base URL (default: http://localhost:8080)
#   -o, --output    Output directory (default: ./results)
#   -i, --influxdb  InfluxDB URL for result storage
#   -r, --report    Generate HTML report
#   -h, --help      Show this help message
#
# Examples:
#   ./run-performance-test.sh load
#   ./run-performance-test.sh stress -u http://api.example.com
#   ./run-performance-test.sh load -i http://localhost:8086/k6 -r
#===============================================================================

set -euo pipefail

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
PERFORMANCE_DIR="${PROJECT_ROOT}/deployment/performance"

# Default configuration
BASE_URL="${K6_BASE_URL:-http://localhost:8080}"
OUTPUT_DIR="${PROJECT_ROOT}/deployment/performance/results"
INFLUXDB_URL=""
GENERATE_REPORT=false
TEST_TYPE=""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

#-------------------------------------------------------------------------------
# Helper Functions
#-------------------------------------------------------------------------------

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

print_banner() {
    echo "=================================================================="
    echo "   GCRF Library Management System - Performance Test Runner"
    echo "=================================================================="
    echo ""
}

print_usage() {
    cat << EOF
Usage: $(basename "$0") <test-type> [options]

Test Types:
  load    - Basic load test (100 VUs, 5 minutes)
            Tests normal production traffic patterns

  stress  - Stress test (up to 500 VUs)
            Finds system breaking point with gradual load increase

  spike   - Spike test (sudden burst to 1000 VUs)
            Tests system behavior under sudden traffic surges

  soak    - Soak test (50 VUs, 1 hour)
            Tests system stability over extended period

Options:
  -u, --url <url>        Base URL of the API gateway
                         (default: http://localhost:8080)

  -o, --output <dir>     Output directory for results
                         (default: ./deployment/performance/results)

  -i, --influxdb <url>   InfluxDB URL for storing results
                         (e.g., http://localhost:8086/k6)

  -r, --report           Generate HTML report after test

  -h, --help             Show this help message

Environment Variables:
  K6_BASE_URL           Base URL (overridden by --url)
  K6_ADMIN_USER         Admin username for testing
  K6_ADMIN_PASS         Admin password for testing
  K6_READER_USER        Reader username for testing
  K6_READER_PASS        Reader password for testing
  K6_SOAK_DURATION      Soak test duration in minutes (default: 60)

Examples:
  # Run basic load test
  $(basename "$0") load

  # Run stress test against staging environment
  $(basename "$0") stress -u https://staging-api.example.com

  # Run load test with InfluxDB output and HTML report
  $(basename "$0") load -i http://localhost:8086/k6 -r

  # Run soak test with custom duration (via environment variable)
  K6_SOAK_DURATION=30 $(basename "$0") soak

EOF
}

check_dependencies() {
    local missing_deps=()

    # Check for k6
    if ! command -v k6 &> /dev/null; then
        missing_deps+=("k6")
    fi

    # Check for jq (for JSON processing)
    if ! command -v jq &> /dev/null; then
        missing_deps+=("jq")
    fi

    if [ ${#missing_deps[@]} -ne 0 ]; then
        log_error "Missing required dependencies: ${missing_deps[*]}"
        echo ""
        echo "Installation instructions:"
        echo ""
        if [[ " ${missing_deps[*]} " =~ " k6 " ]]; then
            echo "k6:"
            echo "  macOS:  brew install k6"
            echo "  Linux:  sudo gpg -k && sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69 && echo \"deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main\" | sudo tee /etc/apt/sources.list.d/k6.list && sudo apt-get update && sudo apt-get install k6"
            echo "  Docker: docker pull grafana/k6"
            echo ""
        fi
        if [[ " ${missing_deps[*]} " =~ " jq " ]]; then
            echo "jq:"
            echo "  macOS:  brew install jq"
            echo "  Linux:  sudo apt-get install jq"
            echo ""
        fi
        exit 1
    fi

    log_success "All dependencies are installed"
}

validate_test_type() {
    local test_type=$1

    case "$test_type" in
        load|stress|spike|soak)
            return 0
            ;;
        *)
            log_error "Invalid test type: $test_type"
            echo "Valid test types: load, stress, spike, soak"
            exit 1
            ;;
    esac
}

check_target_health() {
    local url=$1
    log_info "Checking target system health: ${url}"

    local health_url="${url}/actuator/health"

    if curl -s --max-time 10 "${health_url}" > /dev/null 2>&1; then
        log_success "Target system is reachable"
        return 0
    else
        log_warning "Health check failed for ${health_url}"
        echo ""
        read -p "Continue anyway? (y/N) " -n 1 -r
        echo ""
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_error "Aborted by user"
            exit 1
        fi
    fi
}

prepare_output_directory() {
    local output_dir=$1
    local test_type=$2
    local timestamp=$(date +%Y%m%d_%H%M%S)

    # Create test-specific output directory
    local test_output_dir="${output_dir}/${test_type}_${timestamp}"
    mkdir -p "${test_output_dir}"

    echo "${test_output_dir}"
}

run_k6_test() {
    local test_type=$1
    local test_output_dir=$2
    local test_script="${PERFORMANCE_DIR}/${test_type}-test.js"

    if [ ! -f "${test_script}" ]; then
        log_error "Test script not found: ${test_script}"
        exit 1
    fi

    log_info "Running ${test_type} test..."
    log_info "Script: ${test_script}"
    log_info "Base URL: ${BASE_URL}"
    log_info "Output Directory: ${test_output_dir}"

    # Build k6 command
    local k6_cmd="k6 run"

    # Add InfluxDB output if specified
    if [ -n "${INFLUXDB_URL}" ]; then
        k6_cmd="${k6_cmd} --out influxdb=${INFLUXDB_URL}"
        log_info "InfluxDB Output: ${INFLUXDB_URL}"
    fi

    # Add JSON output
    k6_cmd="${k6_cmd} --out json=${test_output_dir}/results.json"

    # Add summary output
    k6_cmd="${k6_cmd} --summary-export=${test_output_dir}/summary.json"

    # Add environment variables
    export K6_BASE_URL="${BASE_URL}"

    echo ""
    echo "=================================================================="
    echo "Starting ${test_type} test at $(date)"
    echo "=================================================================="
    echo ""

    # Change to performance directory and run test
    cd "${PERFORMANCE_DIR}"

    # Execute k6 test
    if ${k6_cmd} "${test_script}" 2>&1 | tee "${test_output_dir}/console.log"; then
        log_success "${test_type} test completed successfully"
        return 0
    else
        log_error "${test_type} test failed"
        return 1
    fi
}

generate_html_report() {
    local test_output_dir=$1
    local summary_file="${test_output_dir}/summary.json"
    local report_file="${test_output_dir}/report.html"

    if [ ! -f "${summary_file}" ]; then
        log_warning "Summary file not found, skipping HTML report generation"
        return
    fi

    log_info "Generating HTML report..."

    # Generate HTML report
    cat > "${report_file}" << 'HTMLTEMPLATE'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GCRF Performance Test Report</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; color: #333; line-height: 1.6; }
        .container { max-width: 1200px; margin: 0 auto; padding: 20px; }
        h1 { color: #1a1a2e; margin-bottom: 20px; text-align: center; }
        h2 { color: #16213e; margin: 20px 0 10px; border-bottom: 2px solid #0f3460; padding-bottom: 5px; }
        .summary-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin: 20px 0; }
        .card { background: white; border-radius: 8px; padding: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); text-align: center; }
        .card-title { font-size: 14px; color: #666; text-transform: uppercase; }
        .card-value { font-size: 32px; font-weight: bold; color: #0f3460; margin: 10px 0; }
        .card-unit { font-size: 12px; color: #999; }
        .card.success { border-left: 4px solid #28a745; }
        .card.warning { border-left: 4px solid #ffc107; }
        .card.danger { border-left: 4px solid #dc3545; }
        table { width: 100%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin: 20px 0; }
        th, td { padding: 12px 15px; text-align: left; border-bottom: 1px solid #eee; }
        th { background: #0f3460; color: white; font-weight: 500; }
        tr:hover { background: #f8f9fa; }
        .badge { display: inline-block; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: 500; }
        .badge-success { background: #d4edda; color: #155724; }
        .badge-danger { background: #f8d7da; color: #721c24; }
        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
    </style>
</head>
<body>
    <div class="container">
        <h1>GCRF Performance Test Report</h1>
        <p style="text-align: center; color: #666;">Generated: <span id="timestamp"></span></p>

        <h2>Summary</h2>
        <div class="summary-cards" id="summary-cards">
            <!-- Cards will be populated by JavaScript -->
        </div>

        <h2>Metrics Details</h2>
        <table id="metrics-table">
            <thead>
                <tr>
                    <th>Metric</th>
                    <th>Average</th>
                    <th>P90</th>
                    <th>P95</th>
                    <th>P99</th>
                    <th>Max</th>
                </tr>
            </thead>
            <tbody id="metrics-body">
                <!-- Rows will be populated by JavaScript -->
            </tbody>
        </table>

        <h2>Thresholds</h2>
        <table id="thresholds-table">
            <thead>
                <tr>
                    <th>Threshold</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody id="thresholds-body">
                <!-- Rows will be populated by JavaScript -->
            </tbody>
        </table>

        <div class="footer">
            <p>GCRF Library Management System - Performance Testing Suite</p>
            <p>Powered by k6</p>
        </div>
    </div>

    <script>
        // This will be replaced with actual data
        const summaryData = SUMMARY_DATA_PLACEHOLDER;

        document.getElementById('timestamp').textContent = new Date().toLocaleString();

        // Populate summary cards
        const cardsHtml = `
            <div class="card ${summaryData.metrics?.http_req_duration?.values['p(95)'] < 500 ? 'success' : 'danger'}">
                <div class="card-title">P95 Response Time</div>
                <div class="card-value">${(summaryData.metrics?.http_req_duration?.values['p(95)'] || 0).toFixed(2)}</div>
                <div class="card-unit">ms</div>
            </div>
            <div class="card success">
                <div class="card-title">Total Requests</div>
                <div class="card-value">${summaryData.metrics?.http_reqs?.values?.count || 0}</div>
                <div class="card-unit">requests</div>
            </div>
            <div class="card ${(summaryData.metrics?.http_req_failed?.values?.rate || 0) < 0.01 ? 'success' : 'danger'}">
                <div class="card-title">Error Rate</div>
                <div class="card-value">${((summaryData.metrics?.http_req_failed?.values?.rate || 0) * 100).toFixed(2)}</div>
                <div class="card-unit">%</div>
            </div>
            <div class="card success">
                <div class="card-title">Requests/sec</div>
                <div class="card-value">${(summaryData.metrics?.http_reqs?.values?.rate || 0).toFixed(2)}</div>
                <div class="card-unit">req/s</div>
            </div>
        `;
        document.getElementById('summary-cards').innerHTML = cardsHtml;

        // Populate metrics table
        const metricsBody = document.getElementById('metrics-body');
        const httpReqDuration = summaryData.metrics?.http_req_duration?.values || {};
        metricsBody.innerHTML = `
            <tr>
                <td>HTTP Request Duration</td>
                <td>${(httpReqDuration.avg || 0).toFixed(2)} ms</td>
                <td>${(httpReqDuration['p(90)'] || 0).toFixed(2)} ms</td>
                <td>${(httpReqDuration['p(95)'] || 0).toFixed(2)} ms</td>
                <td>${(httpReqDuration['p(99)'] || 0).toFixed(2)} ms</td>
                <td>${(httpReqDuration.max || 0).toFixed(2)} ms</td>
            </tr>
        `;

        // Populate thresholds table
        const thresholdsBody = document.getElementById('thresholds-body');
        const thresholds = summaryData.metrics || {};
        let thresholdsHtml = '';
        Object.keys(thresholds).forEach(key => {
            const metric = thresholds[key];
            if (metric.thresholds) {
                Object.keys(metric.thresholds).forEach(threshold => {
                    const passed = metric.thresholds[threshold].ok;
                    thresholdsHtml += `
                        <tr>
                            <td>${key}: ${threshold}</td>
                            <td><span class="badge ${passed ? 'badge-success' : 'badge-danger'}">${passed ? 'PASSED' : 'FAILED'}</span></td>
                        </tr>
                    `;
                });
            }
        });
        thresholdsBody.innerHTML = thresholdsHtml || '<tr><td colspan="2">No thresholds defined</td></tr>';
    </script>
</body>
</html>
HTMLTEMPLATE

    # Replace placeholder with actual data
    local summary_json=$(cat "${summary_file}")
    sed -i.bak "s/SUMMARY_DATA_PLACEHOLDER/${summary_json//\//\\/}/g" "${report_file}" 2>/dev/null || \
        sed -i '' "s/SUMMARY_DATA_PLACEHOLDER/${summary_json//\//\\/}/g" "${report_file}"

    rm -f "${report_file}.bak" 2>/dev/null

    log_success "HTML report generated: ${report_file}"
}

print_summary() {
    local test_output_dir=$1
    local summary_file="${test_output_dir}/summary.json"

    echo ""
    echo "=================================================================="
    echo "Test Results Summary"
    echo "=================================================================="

    if [ -f "${summary_file}" ] && command -v jq &> /dev/null; then
        local p95=$(jq -r '.metrics.http_req_duration.values["p(95)"] // 0' "${summary_file}")
        local error_rate=$(jq -r '.metrics.http_req_failed.values.rate // 0' "${summary_file}")
        local total_reqs=$(jq -r '.metrics.http_reqs.values.count // 0' "${summary_file}")
        local req_rate=$(jq -r '.metrics.http_reqs.values.rate // 0' "${summary_file}")

        echo ""
        echo "Response Time (P95): ${p95}ms"
        echo "Error Rate: $(echo "${error_rate} * 100" | bc -l 2>/dev/null || echo "${error_rate}")%"
        echo "Total Requests: ${total_reqs}"
        echo "Request Rate: ${req_rate}/s"
        echo ""

        # Check against thresholds
        local p95_threshold=500
        local error_threshold=0.01

        if (( $(echo "${p95} < ${p95_threshold}" | bc -l) )); then
            log_success "P95 response time is within threshold (<${p95_threshold}ms)"
        else
            log_warning "P95 response time exceeds threshold (${p95}ms > ${p95_threshold}ms)"
        fi

        if (( $(echo "${error_rate} < ${error_threshold}" | bc -l) )); then
            log_success "Error rate is within threshold (<1%)"
        else
            log_warning "Error rate exceeds threshold"
        fi
    fi

    echo ""
    echo "Results saved to: ${test_output_dir}"
    echo "=================================================================="
}

#-------------------------------------------------------------------------------
# Main Script
#-------------------------------------------------------------------------------

main() {
    print_banner

    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            load|stress|spike|soak)
                TEST_TYPE="$1"
                shift
                ;;
            -u|--url)
                BASE_URL="$2"
                shift 2
                ;;
            -o|--output)
                OUTPUT_DIR="$2"
                shift 2
                ;;
            -i|--influxdb)
                INFLUXDB_URL="$2"
                shift 2
                ;;
            -r|--report)
                GENERATE_REPORT=true
                shift
                ;;
            -h|--help)
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

    # Validate test type
    if [ -z "${TEST_TYPE}" ]; then
        log_error "Test type is required"
        print_usage
        exit 1
    fi

    validate_test_type "${TEST_TYPE}"

    # Check dependencies
    check_dependencies

    # Check target health
    check_target_health "${BASE_URL}"

    # Prepare output directory
    TEST_OUTPUT_DIR=$(prepare_output_directory "${OUTPUT_DIR}" "${TEST_TYPE}")

    # Run the test
    if run_k6_test "${TEST_TYPE}" "${TEST_OUTPUT_DIR}"; then
        # Generate report if requested
        if [ "${GENERATE_REPORT}" = true ]; then
            generate_html_report "${TEST_OUTPUT_DIR}"
        fi

        # Print summary
        print_summary "${TEST_OUTPUT_DIR}"

        log_success "Performance test completed!"
        exit 0
    else
        print_summary "${TEST_OUTPUT_DIR}"
        log_error "Performance test failed!"
        exit 1
    fi
}

# Run main function
main "$@"
