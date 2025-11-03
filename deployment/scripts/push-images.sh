#!/usr/bin/env bash
################################################################################
# GCRF Library Management System - Docker Image Push
# Description: Push Docker images to registry with retry logic and verification
# Usage: ./push-images.sh [options]
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
readonly PUSH_LOG_DIR="${PROJECT_ROOT}/deployment/logs/push"

# Services
readonly SERVICES=(
    "gateway-service"
    "auth-service"
    "book-service"
    "circulation-service"
    "reader-service"
    "system-service"
    "notification-service"
)

# Default configuration
VERSION="latest"
SOURCE_REGISTRY=""
TARGET_REGISTRY=""
ADDITIONAL_TAGS=()
MAX_RETRIES=3
RETRY_DELAY=5
VERIFY_PUSH=true
PARALLEL=false
DRY_RUN=false

# Push tracking
declare -A PUSH_STATUS
declare -A PUSH_SIZE

################################################################################
# Logging functions
################################################################################

log_info() {
    echo -e "${BLUE}[INFO]${NC} $*"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $*"
}

log_warn() {
    echo -e "${YELLOW}[!]${NC} $*"
}

log_error() {
    echo -e "${RED}[✗]${NC} $*" >&2
}

################################################################################
# Utility functions
################################################################################

usage() {
    cat <<EOF
Usage: $0 [options]

Push GCRF Docker images to registry with retry logic and verification.

Options:
  --version VERSION         Version tag to push (default: latest)
  --source-registry URL     Source registry (if images are tagged with registry)
  --target-registry URL     Target registry (required)
  --tag TAG                 Additional tag to create (can be used multiple times)
  --max-retries N           Maximum push retries (default: 3)
  --retry-delay SECONDS     Delay between retries (default: 5)
  --no-verify              Skip push verification
  --parallel               Push images in parallel
  --dry-run                Show what would be pushed without pushing
  -h, --help               Show this help message

Tagging Strategies:
  Semantic versioning:
    --version v1.0.0 --tag v1.0 --tag v1 --tag latest

  Branch-based:
    --version \$(git rev-parse --abbrev-ref HEAD)

  Commit-based:
    --version \$(git rev-parse --short HEAD)

Examples:
  # Push latest to Harbor
  $0 --target-registry harbor.gcrf.com --version latest

  # Push release with multiple tags
  $0 --target-registry harbor.gcrf.com --version v1.0.0 --tag v1.0 --tag v1 --tag latest

  # Push from one registry to another
  $0 --source-registry local.registry:5000 --target-registry harbor.gcrf.com --version v1.0.0

  # Dry run to preview
  $0 --target-registry harbor.gcrf.com --version v1.0.0 --dry-run

Supported Registries:
  • Docker Hub: docker.io/gcrf
  • Harbor: harbor.gcrf.com
  • Azure Container Registry: gcrf.azurecr.io
  • AWS ECR: 123456789.dkr.ecr.region.amazonaws.com
  • Google Artifact Registry: region-docker.pkg.dev/project/repo

EOF
    exit 0
}

setup_directories() {
    mkdir -p "${PUSH_LOG_DIR}"
}

check_docker_login() {
    local registry=$1

    log_info "Checking Docker login for ${registry}..."

    # Try to get auth info
    if ! docker info 2>/dev/null | grep -q "Username:"; then
        log_warn "Not logged in to Docker"
        log_warn "Run: docker login ${registry}"
        return 1
    fi

    log_success "Docker login OK"
    return 0
}

################################################################################
# Image operations
################################################################################

tag_image() {
    local source_image=$1
    local target_image=$2

    if [ "${DRY_RUN}" = true ]; then
        log_info "[DRY RUN] Would tag: ${source_image} -> ${target_image}"
        return 0
    fi

    log_info "Tagging: ${source_image} -> ${target_image}"

    if docker tag "${source_image}" "${target_image}"; then
        log_success "Tagged successfully"
        return 0
    else
        log_error "Failed to tag image"
        return 1
    fi
}

push_image_with_retry() {
    local image=$1
    local attempt=1

    while [ ${attempt} -le ${MAX_RETRIES} ]; do
        log_info "Pushing ${image} (attempt ${attempt}/${MAX_RETRIES})..."

        if [ "${DRY_RUN}" = true ]; then
            log_info "[DRY RUN] Would push: ${image}"
            return 0
        fi

        local push_log="${PUSH_LOG_DIR}/$(basename ${image})-$(date +%Y%m%d-%H%M%S).log"

        if docker push "${image}" > "${push_log}" 2>&1; then
            log_success "Push successful"

            # Get pushed image size
            local size=$(grep -o "size: [0-9.]*[KMGT]*B" "${push_log}" | tail -1 | awk '{print $2}' || echo "unknown")
            PUSH_SIZE[${image}]="${size}"

            return 0
        else
            log_warn "Push failed (attempt ${attempt}/${MAX_RETRIES})"

            if [ ${attempt} -lt ${MAX_RETRIES} ]; then
                log_info "Retrying in ${RETRY_DELAY} seconds..."
                sleep ${RETRY_DELAY}
            fi
        fi

        ((attempt++))
    done

    log_error "Push failed after ${MAX_RETRIES} attempts"
    return 1
}

verify_push() {
    local image=$1

    if [ "${VERIFY_PUSH}" = false ] || [ "${DRY_RUN}" = true ]; then
        return 0
    fi

    log_info "Verifying pushed image..."

    # Try to pull manifest (without pulling image)
    if docker manifest inspect "${image}" > /dev/null 2>&1; then
        log_success "Image verified in registry"
        return 0
    else
        log_warn "Could not verify image in registry"
        return 1
    fi
}

################################################################################
# Push functions
################################################################################

push_service() {
    local service=$1

    log_info "========================================="
    log_info "Pushing: ${service}"
    log_info "========================================="

    # Build source image name
    local source_image="${service}"
    if [ -n "${SOURCE_REGISTRY}" ]; then
        source_image="${SOURCE_REGISTRY}/${service}"
    fi
    source_image="${source_image}:${VERSION}"

    # Check if source image exists
    if ! docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^${source_image}$"; then
        log_error "Source image not found: ${source_image}"
        PUSH_STATUS[${service}]="NOT_FOUND"
        return 1
    fi

    # Build target image names
    local all_tags=("${VERSION}")
    all_tags+=("${ADDITIONAL_TAGS[@]}")

    local push_failed=false

    for tag in "${all_tags[@]}"; do
        local target_image="${TARGET_REGISTRY}/${service}:${tag}"

        # Tag image
        if ! tag_image "${source_image}" "${target_image}"; then
            push_failed=true
            continue
        fi

        # Push image
        if ! push_image_with_retry "${target_image}"; then
            push_failed=true
            continue
        fi

        # Verify push
        if ! verify_push "${target_image}"; then
            log_warn "Push verification failed for ${target_image}"
        fi

        log_success "Successfully pushed: ${target_image}"
        echo ""
    done

    if [ "${push_failed}" = true ]; then
        PUSH_STATUS[${service}]="FAILED"
        return 1
    else
        PUSH_STATUS[${service}]="SUCCESS"
        return 0
    fi
}

push_all_services() {
    if [ "${PARALLEL}" = true ]; then
        push_all_parallel
    else
        push_all_sequential
    fi
}

push_all_sequential() {
    log_info "Pushing services sequentially..."

    for service in "${SERVICES[@]}"; do
        push_service "${service}"
        echo ""
    done
}

push_all_parallel() {
    log_info "Pushing services in parallel..."

    local pids=()

    for service in "${SERVICES[@]}"; do
        push_service "${service}" &
        pids+=($!)
        sleep 0.5
    done

    # Wait for all pushes to complete
    for pid in "${pids[@]}"; do
        wait ${pid} || true
    done
}

################################################################################
# Reporting
################################################################################

print_summary() {
    echo ""
    log_info "========================================="
    log_info "Push Summary"
    log_info "========================================="

    printf "%-25s %-15s %-15s\n" "Service" "Status" "Size"
    printf "%s\n" "---------------------------------------------------------------"

    local success_count=0
    local failed_count=0

    for service in "${SERVICES[@]}"; do
        local status=${PUSH_STATUS[${service}]:-"NOT_PUSHED"}
        local size=${PUSH_SIZE["${TARGET_REGISTRY}/${service}:${VERSION}"]:-"N/A"}

        local status_symbol="?"
        case ${status} in
            SUCCESS)
                status_symbol="✓"
                ((success_count++))
                ;;
            FAILED)
                status_symbol="✗"
                ((failed_count++))
                ;;
            NOT_FOUND)
                status_symbol="○"
                ((failed_count++))
                ;;
        esac

        printf "%-25s %-15s %-15s\n" "${service}" "${status_symbol} ${status}" "${size}"
    done

    echo "---------------------------------------------------------------"
    echo ""
    log_info "Total:      ${#SERVICES[@]}"
    log_info "Successful: ${success_count}"
    log_info "Failed:     ${failed_count}"
    log_info "Success Rate: $(awk "BEGIN {printf \"%.1f%%\", (${success_count} / ${#SERVICES[@]}) * 100}")"
    echo ""

    if [ -n "${TARGET_REGISTRY}" ]; then
        log_info "Registry: ${TARGET_REGISTRY}"
        log_info "Version:  ${VERSION}"

        if [ ${#ADDITIONAL_TAGS[@]} -gt 0 ]; then
            log_info "Tags:     ${ADDITIONAL_TAGS[*]}"
        fi
    fi

    log_info "========================================="
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
            --source-registry)
                SOURCE_REGISTRY=$2
                shift 2
                ;;
            --target-registry)
                TARGET_REGISTRY=$2
                shift 2
                ;;
            --tag)
                ADDITIONAL_TAGS+=("$2")
                shift 2
                ;;
            --max-retries)
                MAX_RETRIES=$2
                shift 2
                ;;
            --retry-delay)
                RETRY_DELAY=$2
                shift 2
                ;;
            --no-verify)
                VERIFY_PUSH=false
                shift
                ;;
            --parallel)
                PARALLEL=true
                shift
                ;;
            --dry-run)
                DRY_RUN=true
                shift
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

    # Validate required options
    if [ -z "${TARGET_REGISTRY}" ]; then
        log_error "Target registry is required. Use --target-registry option."
        exit 1
    fi

    # Setup
    setup_directories

    # Show configuration
    log_info "========================================="
    log_info "GCRF Image Push Utility"
    log_info "========================================="
    log_info "Source:      ${SOURCE_REGISTRY:-<local>}"
    log_info "Target:      ${TARGET_REGISTRY}"
    log_info "Version:     ${VERSION}"
    log_info "Tags:        ${ADDITIONAL_TAGS[*]:-<none>}"
    log_info "Max Retries: ${MAX_RETRIES}"
    log_info "Parallel:    ${PARALLEL}"
    log_info "Dry Run:     ${DRY_RUN}"
    log_info "========================================="
    echo ""

    # Check Docker login
    if [ "${DRY_RUN}" = false ]; then
        if ! check_docker_login "${TARGET_REGISTRY}"; then
            log_error "Please login to registry first:"
            log_error "  docker login ${TARGET_REGISTRY}"
            exit 1
        fi
    fi

    # Push all services
    push_all_services

    # Print summary
    print_summary

    # Exit with appropriate code
    local failed_count=0
    for service in "${SERVICES[@]}"; do
        [ "${PUSH_STATUS[${service}]}" != "SUCCESS" ] && ((failed_count++))
    done

    if [ ${failed_count} -eq 0 ]; then
        log_success "All images pushed successfully! 🎉"
        exit 0
    else
        log_error "${failed_count} service(s) failed to push"
        exit 1
    fi
}

# Execute main
main "$@"
