#!/usr/bin/env bash
################################################################################
# GCRF Library Management System - Batch Service Builder
# Description: Build all microservices in parallel with progress tracking
# Usage: ./build-all-services.sh [options]
# Author: GCRF DevOps Team
# Version: 1.0.0
################################################################################

set -euo pipefail

# Color output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly CYAN='\033[0;36m'
readonly NC='\033[0m' # No Color

# Script configuration
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
readonly BUILD_LOG_DIR="${PROJECT_ROOT}/deployment/logs/builds"
readonly BUILD_REPORT_DIR="${PROJECT_ROOT}/deployment/reports"

# Service definitions with ports and dependencies
declare -A SERVICE_PORTS=(
    ["gateway-service"]="8080"
    ["auth-service"]="8081"
    ["book-service"]="8082"
    ["circulation-service"]="8083"
    ["reader-service"]="8084"
    ["system-service"]="8085"
    ["notification-service"]="8086"
)

# Build order (respecting dependencies)
readonly BUILD_ORDER=(
    "auth-service"           # No dependencies
    "reader-service"         # No dependencies
    "book-service"           # No dependencies
    "system-service"         # No dependencies
    "notification-service"   # No dependencies
    "circulation-service"    # Depends on auth, reader, book
    "gateway-service"        # Depends on all services
)

# Default configuration
VERSION="latest"
PLATFORMS="linux/amd64"
REGISTRY=""
PUSH=false
NO_CACHE=false
MAX_PARALLEL=4
STOP_ON_ERROR=false
SKIP_TESTS=true
VERBOSE=false
BUILD_REPORT_FORMAT="text"

# Build tracking
declare -A BUILD_STATUS
declare -A BUILD_START_TIME
declare -A BUILD_END_TIME
declare -A BUILD_LOGS
declare -A BUILD_IMAGE_SIZE

################################################################################
# Logging functions
################################################################################

log_info() {
    echo -e "${BLUE}[INFO]${NC} $*"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $*"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $*"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $*" >&2
}

log_progress() {
    echo -e "${CYAN}[PROGRESS]${NC} $*"
}

################################################################################
# Utility functions
################################################################################

usage() {
    cat <<EOF
Usage: $0 [options]

Build all GCRF microservices with parallel execution and progress tracking.

Options:
  --version VERSION     Version tag for all images (default: latest)
  --platform PLATFORMS  Target platforms (default: linux/amd64)
  --registry REGISTRY   Docker registry URL
  --push               Push images to registry after build
  --no-cache           Build without using cache
  --parallel N         Maximum parallel builds (default: 4)
  --stop-on-error      Stop all builds on first error
  --no-skip-tests      Run tests during Maven build
  --verbose            Enable verbose output
  --format FORMAT      Report format: text, json, html (default: text)
  -h, --help           Show this help message

Examples:
  # Build all services locally
  $0

  # Build and push to registry
  $0 --version v1.0.0 --registry harbor.gcrf.com --push

  # Parallel build with custom concurrency
  $0 --parallel 8 --version v1.0.0

  # Build with HTML report
  $0 --version v1.0.0 --format html

Services to build (${#SERVICE_PORTS[@]} total):
$(for service in "${BUILD_ORDER[@]}"; do
    printf "  %-25s (port %s)\n" "${service}" "${SERVICE_PORTS[${service}]}"
done)

EOF
    exit 0
}

setup_directories() {
    mkdir -p "${BUILD_LOG_DIR}"
    mkdir -p "${BUILD_REPORT_DIR}"
}

################################################################################
# Build functions
################################################################################

build_common_modules() {
    log_info "Building common modules first..."

    local backend_dir="${PROJECT_ROOT}/backend"
    local maven_opts="-DskipTests"

    if [ "${VERBOSE}" = false ]; then
        maven_opts+=" -q"
    fi

    # Set Java 21
    if [ -x "/usr/libexec/java_home" ]; then
        export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || echo "")
    fi

    cd "${backend_dir}"

    local common_log="${BUILD_LOG_DIR}/common-modules-$(date +%Y%m%d-%H%M%S).log"

    if ! mvn clean install -pl common ${maven_opts} > "${common_log}" 2>&1; then
        log_error "Failed to build common modules"
        log_error "Log: ${common_log}"
        return 1
    fi

    log_success "Common modules built successfully"
    return 0
}

build_service() {
    local service=$1
    local build_id=$(date +%Y%m%d-%H%M%S)-$$

    BUILD_START_TIME[${service}]=$(date +%s)
    BUILD_STATUS[${service}]="IN_PROGRESS"

    local service_log="${BUILD_LOG_DIR}/${service}-${build_id}.log"
    BUILD_LOGS[${service}]="${service_log}"

    log_progress "Building ${service}..."

    # Build using build-service.sh script
    local build_opts=(
        --version "${VERSION}"
        --platform "${PLATFORMS}"
    )

    if [ -n "${REGISTRY}" ]; then
        build_opts+=(--registry "${REGISTRY}")
    fi

    if [ "${PUSH}" = true ]; then
        build_opts+=(--push)
    fi

    if [ "${NO_CACHE}" = true ]; then
        build_opts+=(--no-cache)
    fi

    if [ "${VERBOSE}" = true ]; then
        build_opts+=(--verbose)
    fi

    # Execute build
    if "${SCRIPT_DIR}/build-service.sh" "${service}" "${VERSION}" "${build_opts[@]}" > "${service_log}" 2>&1; then
        BUILD_STATUS[${service}]="SUCCESS"
        BUILD_END_TIME[${service}]=$(date +%s)

        # Get image size
        local image_name="${service}"
        if [ -n "${REGISTRY}" ]; then
            image_name="${REGISTRY}/${service}"
        fi

        local image_size=$(docker images --format "{{.Size}}" "${image_name}:${VERSION}" 2>/dev/null || echo "unknown")
        BUILD_IMAGE_SIZE[${service}]="${image_size}"

        log_success "✓ ${service} completed"
        return 0
    else
        BUILD_STATUS[${service}]="FAILED"
        BUILD_END_TIME[${service}]=$(date +%s)
        log_error "✗ ${service} failed"

        if [ "${STOP_ON_ERROR}" = true ]; then
            log_error "Stopping all builds due to error"
            killall -9 build-service.sh 2>/dev/null || true
        fi

        return 1
    fi
}

build_all_parallel() {
    local running_jobs=0
    local job_pids=()
    local service_for_pid=()

    log_info "Building ${#BUILD_ORDER[@]} services with max ${MAX_PARALLEL} parallel jobs..."

    for service in "${BUILD_ORDER[@]}"; do
        # Wait if max parallel jobs reached
        while [ ${running_jobs} -ge ${MAX_PARALLEL} ]; do
            # Check for completed jobs
            for i in "${!job_pids[@]}"; do
                local pid=${job_pids[$i]}
                if ! kill -0 "${pid}" 2>/dev/null; then
                    wait "${pid}" || true
                    unset "job_pids[$i]"
                    ((running_jobs--))
                fi
            done
            sleep 1
        done

        # Start build in background
        build_service "${service}" &
        local pid=$!
        job_pids+=("${pid}")
        ((running_jobs++))

        sleep 0.5  # Stagger starts
    done

    # Wait for all remaining jobs
    log_info "Waiting for all builds to complete..."
    for pid in "${job_pids[@]}"; do
        wait "${pid}" || true
    done
}

################################################################################
# Reporting functions
################################################################################

calculate_build_time() {
    local service=$1
    local start=${BUILD_START_TIME[${service}]:-0}
    local end=${BUILD_END_TIME[${service}]:-0}
    echo $((end - start))
}

format_duration() {
    local seconds=$1
    printf "%dm %02ds" $((seconds / 60)) $((seconds % 60))
}

generate_text_report() {
    local report_file="${BUILD_REPORT_DIR}/build-report-$(date +%Y%m%d-%H%M%S).txt"

    {
        echo "========================================="
        echo "GCRF Library Management System"
        echo "Build Report"
        echo "========================================="
        echo "Date:     $(date)"
        echo "Version:  ${VERSION}"
        echo "Platform: ${PLATFORMS}"
        echo "Registry: ${REGISTRY:-<local>}"
        echo "========================================="
        echo ""

        printf "%-25s %-10s %-12s %-12s %-8s\n" "Service" "Status" "Build Time" "Image Size" "Port"
        printf "%s\n" "---------------------------------------------------------------------------------"

        local total_time=0
        local success_count=0
        local failed_count=0

        for service in "${BUILD_ORDER[@]}"; do
            local status=${BUILD_STATUS[${service}]:-UNKNOWN}
            local duration=$(calculate_build_time "${service}")
            local formatted_time=$(format_duration "${duration}")
            local image_size=${BUILD_IMAGE_SIZE[${service}]:-"unknown"}
            local port=${SERVICE_PORTS[${service}]:-"N/A"}

            local status_symbol="?"
            case ${status} in
                SUCCESS)
                    status_symbol="✓"
                    ((success_count++))
                    total_time=$((total_time + duration))
                    ;;
                FAILED)
                    status_symbol="✗"
                    ((failed_count++))
                    ;;
                IN_PROGRESS)
                    status_symbol="⋯"
                    ;;
            esac

            printf "%-25s %-10s %-12s %-12s %-8s\n" \
                "${service}" \
                "${status_symbol} ${status}" \
                "${formatted_time}" \
                "${image_size}" \
                "${port}"
        done

        echo "---------------------------------------------------------------------------------"
        printf "%-25s %-10s %-12s\n" "Total:" "${#BUILD_ORDER[@]}" "$(format_duration ${total_time})"
        echo ""
        echo "Summary:"
        echo "  Successful: ${success_count}"
        echo "  Failed:     ${failed_count}"
        echo "  Success Rate: $(awk "BEGIN {printf \"%.1f%%\", (${success_count} / ${#BUILD_ORDER[@]}) * 100}")"
        echo ""

        if [ ${failed_count} -gt 0 ]; then
            echo "Failed Services:"
            for service in "${BUILD_ORDER[@]}"; do
                if [ "${BUILD_STATUS[${service}]}" = "FAILED" ]; then
                    echo "  - ${service}"
                    echo "    Log: ${BUILD_LOGS[${service}]}"
                fi
            done
        fi

    } | tee "${report_file}"

    echo ""
    log_success "Report saved to: ${report_file}"
}

generate_json_report() {
    local report_file="${BUILD_REPORT_DIR}/build-report-$(date +%Y%m%d-%H%M%S).json"

    {
        echo "{"
        echo "  \"timestamp\": \"$(date -u +"%Y-%m-%dT%H:%M:%SZ")\","
        echo "  \"version\": \"${VERSION}\","
        echo "  \"platform\": \"${PLATFORMS}\","
        echo "  \"registry\": \"${REGISTRY:-null}\","
        echo "  \"services\": ["

        local first=true
        for service in "${BUILD_ORDER[@]}"; do
            [ "${first}" = false ] && echo ","
            first=false

            local status=${BUILD_STATUS[${service}]:-UNKNOWN}
            local duration=$(calculate_build_time "${service}")
            local image_size=${BUILD_IMAGE_SIZE[${service}]:-"unknown"}
            local port=${SERVICE_PORTS[${service}]:-null}

            echo "    {"
            echo "      \"name\": \"${service}\","
            echo "      \"status\": \"${status}\","
            echo "      \"duration\": ${duration},"
            echo "      \"imageSize\": \"${image_size}\","
            echo "      \"port\": ${port},"
            echo "      \"logFile\": \"${BUILD_LOGS[${service}]:-null}\""
            echo -n "    }"
        done

        echo ""
        echo "  ]"
        echo "}"
    } > "${report_file}"

    log_success "JSON report saved to: ${report_file}"
}

generate_html_report() {
    local report_file="${BUILD_REPORT_DIR}/build-report-$(date +%Y%m%d-%H%M%S).html"

    local success_count=0
    local failed_count=0
    for service in "${BUILD_ORDER[@]}"; do
        [ "${BUILD_STATUS[${service}]}" = "SUCCESS" ] && ((success_count++))
        [ "${BUILD_STATUS[${service}]}" = "FAILED" ] && ((failed_count++))
    done

    {
        cat <<'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GCRF Build Report</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f5f5f5; padding: 20px; }
        .container { max-width: 1200px; margin: 0 auto; background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 8px 8px 0 0; }
        .header h1 { font-size: 28px; margin-bottom: 10px; }
        .header p { opacity: 0.9; }
        .summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; padding: 30px; background: #f8f9fa; }
        .summary-card { background: white; padding: 20px; border-radius: 6px; border-left: 4px solid #667eea; }
        .summary-card h3 { color: #666; font-size: 14px; margin-bottom: 8px; }
        .summary-card .value { font-size: 32px; font-weight: bold; color: #333; }
        .table-container { padding: 30px; }
        table { width: 100%; border-collapse: collapse; }
        th { background: #f8f9fa; padding: 12px; text-align: left; font-weight: 600; color: #333; border-bottom: 2px solid #dee2e6; }
        td { padding: 12px; border-bottom: 1px solid #dee2e6; }
        tr:hover { background: #f8f9fa; }
        .status { padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 600; display: inline-block; }
        .status.success { background: #d4edda; color: #155724; }
        .status.failed { background: #f8d7da; color: #721c24; }
        .status.in-progress { background: #fff3cd; color: #856404; }
        .footer { padding: 20px 30px; background: #f8f9fa; border-radius: 0 0 8px 8px; text-align: center; color: #666; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🚀 GCRF Library Management System</h1>
            <p>Build Report - VERSION_PLACEHOLDER</p>
            <p>Generated: TIMESTAMP_PLACEHOLDER</p>
        </div>

        <div class="summary">
            <div class="summary-card">
                <h3>Total Services</h3>
                <div class="value">TOTAL_PLACEHOLDER</div>
            </div>
            <div class="summary-card">
                <h3>Successful</h3>
                <div class="value" style="color: #28a745;">SUCCESS_PLACEHOLDER</div>
            </div>
            <div class="summary-card">
                <h3>Failed</h3>
                <div class="value" style="color: #dc3545;">FAILED_PLACEHOLDER</div>
            </div>
            <div class="summary-card">
                <h3>Success Rate</h3>
                <div class="value" style="color: #667eea;">RATE_PLACEHOLDER</div>
            </div>
        </div>

        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th>Service</th>
                        <th>Status</th>
                        <th>Build Time</th>
                        <th>Image Size</th>
                        <th>Port</th>
                    </tr>
                </thead>
                <tbody>
EOF

        for service in "${BUILD_ORDER[@]}"; do
            local status=${BUILD_STATUS[${service}]:-UNKNOWN}
            local duration=$(calculate_build_time "${service}")
            local formatted_time=$(format_duration "${duration}")
            local image_size=${BUILD_IMAGE_SIZE[${service}]:-"unknown"}
            local port=${SERVICE_PORTS[${service}]:-"N/A"}

            local status_class="in-progress"
            local status_text="${status}"
            case ${status} in
                SUCCESS)
                    status_class="success"
                    status_text="✓ Success"
                    ;;
                FAILED)
                    status_class="failed"
                    status_text="✗ Failed"
                    ;;
            esac

            echo "                    <tr>"
            echo "                        <td><strong>${service}</strong></td>"
            echo "                        <td><span class=\"status ${status_class}\">${status_text}</span></td>"
            echo "                        <td>${formatted_time}</td>"
            echo "                        <td>${image_size}</td>"
            echo "                        <td>${port}</td>"
            echo "                    </tr>"
        done

        cat <<EOF
                </tbody>
            </table>
        </div>

        <div class="footer">
            <p>GCRF DevOps Team - $(date +%Y)</p>
        </div>
    </div>
</body>
</html>
EOF
    } | sed \
        -e "s/VERSION_PLACEHOLDER/${VERSION}/g" \
        -e "s/TIMESTAMP_PLACEHOLDER/$(date)/g" \
        -e "s/TOTAL_PLACEHOLDER/${#BUILD_ORDER[@]}/g" \
        -e "s/SUCCESS_PLACEHOLDER/${success_count}/g" \
        -e "s/FAILED_PLACEHOLDER/${failed_count}/g" \
        -e "s/RATE_PLACEHOLDER/$(awk "BEGIN {printf \"%.1f%%\", (${success_count} / ${#BUILD_ORDER[@]}) * 100}")/g" \
        > "${report_file}"

    log_success "HTML report saved to: ${report_file}"
}

generate_report() {
    case ${BUILD_REPORT_FORMAT} in
        json)
            generate_json_report
            ;;
        html)
            generate_html_report
            ;;
        text|*)
            generate_text_report
            ;;
    esac
}

################################################################################
# Main execution
################################################################################

main() {
    # Parse options
    while [ $# -gt 0 ]; do
        case $1 in
            --version)
                VERSION=$2
                shift 2
                ;;
            --platform)
                PLATFORMS=$2
                shift 2
                ;;
            --registry)
                REGISTRY=$2
                shift 2
                ;;
            --push)
                PUSH=true
                shift
                ;;
            --no-cache)
                NO_CACHE=true
                shift
                ;;
            --parallel)
                MAX_PARALLEL=$2
                shift 2
                ;;
            --stop-on-error)
                STOP_ON_ERROR=true
                shift
                ;;
            --no-skip-tests)
                SKIP_TESTS=false
                shift
                ;;
            --verbose)
                VERBOSE=true
                shift
                ;;
            --format)
                BUILD_REPORT_FORMAT=$2
                shift 2
                ;;
            -h|--help)
                usage
                ;;
            *)
                log_error "Unknown option: $1"
                usage
                ;;
        esac
    done

    # Setup
    setup_directories

    # Show configuration
    log_info "========================================="
    log_info "GCRF Batch Service Builder"
    log_info "========================================="
    log_info "Services:    ${#BUILD_ORDER[@]}"
    log_info "Version:     ${VERSION}"
    log_info "Platforms:   ${PLATFORMS}"
    log_info "Registry:    ${REGISTRY:-<local>}"
    log_info "Parallel:    ${MAX_PARALLEL}"
    log_info "Push:        ${PUSH}"
    log_info "Report:      ${BUILD_REPORT_FORMAT}"
    log_info "========================================="

    # Build common modules first
    if ! build_common_modules; then
        log_error "Failed to build common modules"
        exit 1
    fi

    # Build all services
    local overall_start=$(date +%s)
    build_all_parallel
    local overall_end=$(date +%s)
    local total_duration=$((overall_end - overall_start))

    # Generate report
    echo ""
    log_info "Generating build report..."
    generate_report

    # Summary
    local success_count=0
    local failed_count=0
    for service in "${BUILD_ORDER[@]}"; do
        [ "${BUILD_STATUS[${service}]}" = "SUCCESS" ] && ((success_count++))
        [ "${BUILD_STATUS[${service}]}" = "FAILED" ] && ((failed_count++))
    done

    echo ""
    log_info "========================================="
    log_info "Build Summary"
    log_info "========================================="
    log_info "Total Time:  $(format_duration ${total_duration})"
    log_info "Successful:  ${success_count}/${#BUILD_ORDER[@]}"
    log_info "Failed:      ${failed_count}/${#BUILD_ORDER[@]}"
    log_info "Success Rate: $(awk "BEGIN {printf \"%.1f%%\", (${success_count} / ${#BUILD_ORDER[@]}) * 100}")"
    log_info "========================================="

    if [ ${failed_count} -eq 0 ]; then
        log_success "All services built successfully! 🎉"
        exit 0
    else
        log_error "Some services failed to build"
        exit 1
    fi
}

# Execute main
main "$@"
