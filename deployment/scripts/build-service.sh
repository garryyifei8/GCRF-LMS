#!/usr/bin/env bash
################################################################################
# GCRF Library Management System - Single Service Builder
# Description: Build Docker image for a single microservice with optimizations
# Usage: ./build-service.sh <service-name> <version> [options]
# Author: GCRF DevOps Team
# Version: 1.0.0
################################################################################

set -euo pipefail

# Color output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# Script configuration
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
readonly BACKEND_DIR="${PROJECT_ROOT}/backend"
readonly BUILD_LOG_DIR="${PROJECT_ROOT}/deployment/logs/builds"

# Default configuration
PLATFORMS="linux/amd64"
REGISTRY=""
PUSH=false
NO_CACHE=false
VERBOSE=false
BUILD_ARGS=()
TAGS=()

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

################################################################################
# Utility functions
################################################################################

usage() {
    cat <<EOF
Usage: $0 <service-name> <version> [options]

Build Docker image for a single GCRF microservice.

Arguments:
  service-name          Name of the service (e.g., gateway-service, auth-service)
  version              Version tag (e.g., v1.0.0, latest)

Options:
  --platform PLATFORMS  Target platforms (default: linux/amd64)
                       Multi-platform: linux/amd64,linux/arm64
  --registry REGISTRY  Docker registry URL (e.g., harbor.gcrf.com)
  --push               Push image to registry after build
  --no-cache           Build without using cache
  --build-arg KEY=VAL  Pass build arguments (can be used multiple times)
  --tag TAG            Additional tags (can be used multiple times)
  --verbose            Enable verbose output
  -h, --help           Show this help message

Examples:
  # Build for local development
  $0 gateway-service v1.0.0

  # Build and push to registry
  $0 gateway-service v1.0.0 --registry harbor.gcrf.com --push

  # Multi-platform build
  $0 auth-service v1.0.0 --platform linux/amd64,linux/arm64

  # Build with custom arguments
  $0 book-service v1.0.0 --build-arg JAVA_VERSION=21 --tag latest

Services:
  gateway-service        API Gateway (port 8080)
  auth-service          Authentication Service (port 8081)
  book-service          Book Management Service (port 8082)
  circulation-service   Circulation Service (port 8083)
  reader-service        Reader Management Service (port 8084)
  system-service        System Management Service (port 8085)
  notification-service  Notification Service (port 8086)

EOF
    exit 0
}

check_dependencies() {
    local missing_deps=()

    if ! command -v docker &> /dev/null; then
        missing_deps+=("docker")
    fi

    if ! command -v mvn &> /dev/null; then
        missing_deps+=("maven")
    fi

    if ! command -v git &> /dev/null; then
        missing_deps+=("git")
    fi

    if [ ${#missing_deps[@]} -gt 0 ]; then
        log_error "Missing required dependencies: ${missing_deps[*]}"
        log_error "Please install them and try again."
        exit 1
    fi

    # Check Docker BuildKit support
    if ! docker buildx version &> /dev/null; then
        log_warn "Docker BuildKit not available. Using legacy builder."
    fi
}

validate_service() {
    local service=$1
    local service_dir="${BACKEND_DIR}/${service}"

    if [ ! -d "${service_dir}" ]; then
        log_error "Service directory not found: ${service_dir}"
        log_error "Available services: gateway-service, auth-service, book-service, circulation-service, reader-service, system-service, notification-service"
        exit 1
    fi

    if [ ! -f "${service_dir}/pom.xml" ]; then
        log_error "pom.xml not found in ${service_dir}"
        exit 1
    fi

    if [ ! -f "${service_dir}/Dockerfile" ]; then
        log_error "Dockerfile not found in ${service_dir}"
        log_error "Please create Dockerfile before building"
        exit 1
    fi
}

################################################################################
# Build functions
################################################################################

build_jar() {
    local service=$1
    local service_dir="${BACKEND_DIR}/${service}"

    log_info "Building JAR for ${service}..."

    # Set Java 21
    if [ -x "/usr/libexec/java_home" ]; then
        export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || echo "")
        if [ -z "${JAVA_HOME}" ]; then
            log_error "Java 21 not found. Please install Java 21."
            exit 1
        fi
    fi

    local maven_opts=""
    if [ "${VERBOSE}" = false ]; then
        maven_opts="-q"
    fi

    # Build the service and its dependencies
    cd "${BACKEND_DIR}"
    if ! mvn clean package -pl "${service}" -am -DskipTests ${maven_opts}; then
        log_error "Maven build failed for ${service}"
        return 1
    fi

    log_success "JAR build completed for ${service}"
    return 0
}

build_docker_image() {
    local service=$1
    local version=$2
    local service_dir="${BACKEND_DIR}/${service}"

    log_info "Building Docker image for ${service}:${version}..."

    # Prepare build context
    mkdir -p "${BUILD_LOG_DIR}"
    local build_log="${BUILD_LOG_DIR}/${service}-${version}-$(date +%Y%m%d-%H%M%S).log"

    # Get git metadata
    local git_commit=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
    local git_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")
    local build_date=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

    # Build image name
    local image_name="${service}"
    if [ -n "${REGISTRY}" ]; then
        image_name="${REGISTRY}/${service}"
    fi

    # Prepare tags
    local primary_tag="${image_name}:${version}"
    local all_tags=("${primary_tag}")

    # Add additional tags
    for tag in "${TAGS[@]}"; do
        all_tags+=("${image_name}:${tag}")
    done

    # Build tag arguments
    local tag_args=()
    for tag in "${all_tags[@]}"; do
        tag_args+=("-t" "${tag}")
    done

    # Build arguments
    local build_arg_opts=()
    build_arg_opts+=(
        "--build-arg" "VERSION=${version}"
        "--build-arg" "GIT_COMMIT=${git_commit}"
        "--build-arg" "GIT_BRANCH=${git_branch}"
        "--build-arg" "BUILD_DATE=${build_date}"
    )

    for arg in "${BUILD_ARGS[@]}"; do
        build_arg_opts+=("--build-arg" "${arg}")
    done

    # Build labels
    local label_opts=(
        "--label" "org.opencontainers.image.version=${version}"
        "--label" "org.opencontainers.image.revision=${git_commit}"
        "--label" "org.opencontainers.image.created=${build_date}"
        "--label" "org.opencontainers.image.source=https://github.com/gcrf/library-management"
        "--label" "com.gcrf.service.name=${service}"
    )

    # Cache options
    local cache_opts=()
    if [ "${NO_CACHE}" = true ]; then
        cache_opts+=("--no-cache")
    fi

    # Build command
    export DOCKER_BUILDKIT=1

    local build_cmd=(
        docker build
        "${tag_args[@]}"
        "${build_arg_opts[@]}"
        "${label_opts[@]}"
        "${cache_opts[@]}"
        --platform "${PLATFORMS}"
    )

    if [ "${VERBOSE}" = true ]; then
        build_cmd+=("--progress=plain")
    fi

    build_cmd+=("${service_dir}")

    # Execute build
    local start_time=$(date +%s)

    if [ "${VERBOSE}" = true ]; then
        if ! "${build_cmd[@]}" 2>&1 | tee "${build_log}"; then
            log_error "Docker build failed for ${service}"
            log_error "Build log: ${build_log}"
            return 1
        fi
    else
        if ! "${build_cmd[@]}" > "${build_log}" 2>&1; then
            log_error "Docker build failed for ${service}"
            log_error "Build log: ${build_log}"
            tail -n 50 "${build_log}"
            return 1
        fi
    fi

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    log_success "Docker image built in ${duration}s: ${primary_tag}"
    log_info "Build log saved to: ${build_log}"

    # Show image info
    docker images --format "table {{.Repository}}:{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}" | grep "${service}:${version}" || true

    return 0
}

push_image() {
    local service=$1
    local version=$2

    if [ "${PUSH}" = false ]; then
        return 0
    fi

    if [ -z "${REGISTRY}" ]; then
        log_error "Registry not specified. Use --registry option."
        return 1
    fi

    log_info "Pushing image to registry..."

    local image_name="${REGISTRY}/${service}"
    local all_tags=("${version}")
    all_tags+=("${TAGS[@]}")

    for tag in "${all_tags[@]}"; do
        local full_tag="${image_name}:${tag}"
        log_info "Pushing ${full_tag}..."

        if ! docker push "${full_tag}"; then
            log_error "Failed to push ${full_tag}"
            return 1
        fi

        log_success "Pushed ${full_tag}"
    done

    return 0
}

################################################################################
# Main execution
################################################################################

main() {
    # Parse arguments
    if [ $# -lt 2 ]; then
        usage
    fi

    local service=$1
    local version=$2
    shift 2

    # Parse options
    while [ $# -gt 0 ]; do
        case $1 in
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
            --build-arg)
                BUILD_ARGS+=("$2")
                shift 2
                ;;
            --tag)
                TAGS+=("$2")
                shift 2
                ;;
            --verbose)
                VERBOSE=true
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

    # Show build configuration
    log_info "========================================="
    log_info "GCRF Service Builder"
    log_info "========================================="
    log_info "Service:     ${service}"
    log_info "Version:     ${version}"
    log_info "Platforms:   ${PLATFORMS}"
    log_info "Registry:    ${REGISTRY:-<local>}"
    log_info "Push:        ${PUSH}"
    log_info "No Cache:    ${NO_CACHE}"
    log_info "========================================="

    # Check dependencies
    check_dependencies

    # Validate service
    validate_service "${service}"

    # Build JAR
    if ! build_jar "${service}"; then
        log_error "Build failed at JAR stage"
        exit 1
    fi

    # Build Docker image
    if ! build_docker_image "${service}" "${version}"; then
        log_error "Build failed at Docker stage"
        exit 1
    fi

    # Push to registry
    if ! push_image "${service}" "${version}"; then
        log_error "Failed to push image to registry"
        exit 2
    fi

    log_success "========================================="
    log_success "Build completed successfully!"
    log_success "========================================="
    log_success "Image: ${REGISTRY:+${REGISTRY}/}${service}:${version}"

    if [ ${#TAGS[@]} -gt 0 ]; then
        log_success "Tags: ${TAGS[*]}"
    fi
}

# Execute main
main "$@"
