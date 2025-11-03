#!/bin/bash

###############################################################################
# Docker Image Tagging Automation Script
# Version: 1.0.0
# Description: Automated image tagging based on Git metadata and environment
# Author: GCRF Library Management System Team
# Date: 2025-01-01
###############################################################################

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
REGISTRY="${DOCKER_REGISTRY:-gcrf-library}"
DRY_RUN="${DRY_RUN:-false}"
PUSH="${PUSH:-false}"
CLEANUP="${CLEANUP:-false}"
VERBOSE="${VERBOSE:-false}"

# Function to print colored output
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
    exit 1
}

log_debug() {
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${MAGENTA}[DEBUG]${NC} $1"
    fi
}

# Function to display usage
usage() {
    cat << EOF
Usage: $0 [OPTIONS] SERVICE_NAME SOURCE_TAG

Automated Docker image tagging based on Git metadata and environment.

Arguments:
  SERVICE_NAME    Name of the service (e.g., gateway-service, auth-service)
  SOURCE_TAG      Source tag to tag from (e.g., build, latest)

Options:
  -r, --registry REGISTRY    Docker registry (default: gcrf-library)
  -p, --push                 Push tags to registry after tagging
  -c, --cleanup              Remove source tag after tagging
  -d, --dry-run             Show what would be tagged without doing it
  -v, --verbose             Enable verbose output
  -e, --env ENV             Environment (dev|staging|prod|auto)
  -t, --extra-tags TAGS     Additional tags (comma-separated)
  --version VERSION         Override version (default: from git or package.json)
  --no-git-tags            Skip Git-based tags
  --no-env-tags           Skip environment tags
  --force                  Force tagging even if tags exist
  -h, --help               Display this help message

Examples:
  # Basic usage
  $0 gateway-service build

  # Tag and push to registry
  $0 -p gateway-service build

  # Dry run to see what would be tagged
  $0 -d gateway-service build

  # Production release with specific version
  $0 -e prod --version 1.2.0 -p gateway-service build

  # Add extra tags
  $0 -t "stable,latest-release" gateway-service build

Environment Variables:
  DOCKER_REGISTRY    Default registry (default: gcrf-library)
  DRY_RUN           Set to true for dry run
  PUSH              Set to true to push tags
  VERBOSE           Set to true for verbose output
EOF
    exit 0
}

# Parse command line arguments
POSITIONAL_ARGS=()
EXTRA_TAGS=""
ENVIRONMENT="auto"
VERSION_OVERRIDE=""
NO_GIT_TAGS=false
NO_ENV_TAGS=false
FORCE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -r|--registry)
            REGISTRY="$2"
            shift 2
            ;;
        -p|--push)
            PUSH=true
            shift
            ;;
        -c|--cleanup)
            CLEANUP=true
            shift
            ;;
        -d|--dry-run)
            DRY_RUN=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -e|--env)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -t|--extra-tags)
            EXTRA_TAGS="$2"
            shift 2
            ;;
        --version)
            VERSION_OVERRIDE="$2"
            shift 2
            ;;
        --no-git-tags)
            NO_GIT_TAGS=true
            shift
            ;;
        --no-env-tags)
            NO_ENV_TAGS=true
            shift
            ;;
        --force)
            FORCE=true
            shift
            ;;
        -h|--help)
            usage
            ;;
        --*)
            log_error "Unknown option: $1"
            ;;
        *)
            POSITIONAL_ARGS+=("$1")
            shift
            ;;
    esac
done

# Restore positional parameters
set -- "${POSITIONAL_ARGS[@]}"

# Validate arguments
if [[ $# -lt 2 ]]; then
    log_error "Missing required arguments. Use -h for help."
fi

SERVICE_NAME="$1"
SOURCE_TAG="$2"
SOURCE_IMAGE="${REGISTRY}/${SERVICE_NAME}:${SOURCE_TAG}"

# Function to check if image exists
check_image_exists() {
    local image="$1"
    if docker image inspect "$image" &>/dev/null; then
        return 0
    else
        return 1
    fi
}

# Function to get version from various sources
get_version() {
    local version=""

    # Use override if provided
    if [[ -n "$VERSION_OVERRIDE" ]]; then
        version="$VERSION_OVERRIDE"
        log_debug "Using override version: $version"
    # Try to get version from git tag
    elif git describe --tags --exact-match &>/dev/null; then
        version=$(git describe --tags --exact-match)
        version="${version#v}"  # Remove 'v' prefix if present
        log_debug "Using git tag version: $version"
    # Try to get version from package.json
    elif [[ -f "package.json" ]]; then
        version=$(grep -o '"version"[[:space:]]*:[[:space:]]*"[^"]*"' package.json | sed 's/.*"version"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/')
        log_debug "Using package.json version: $version"
    # Try to get version from pom.xml
    elif [[ -f "pom.xml" ]]; then
        version=$(grep -m1 '<version>' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
        log_debug "Using pom.xml version: $version"
    # Default version
    else
        version="0.0.0-dev"
        log_warning "Could not determine version, using default: $version"
    fi

    echo "$version"
}

# Function to get Git metadata
get_git_metadata() {
    local metadata=()

    # Get commit SHA
    if git rev-parse HEAD &>/dev/null; then
        metadata+=("sha:$(git rev-parse --short HEAD)")
        metadata+=("commit:$(git rev-parse HEAD)")
    fi

    # Get branch name
    if git rev-parse --abbrev-ref HEAD &>/dev/null; then
        local branch=$(git rev-parse --abbrev-ref HEAD)
        # Sanitize branch name for Docker tag
        branch=$(echo "$branch" | sed 's/[^a-zA-Z0-9._-]/-/g' | sed 's/^-//' | sed 's/-$//')
        metadata+=("branch:$branch")
    fi

    # Check if dirty
    if [[ -n $(git status --porcelain 2>/dev/null) ]]; then
        metadata+=("dirty:true")
    fi

    echo "${metadata[@]}"
}

# Function to determine environment from branch
determine_environment() {
    if [[ "$ENVIRONMENT" != "auto" ]]; then
        echo "$ENVIRONMENT"
        return
    fi

    local branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")

    case "$branch" in
        main|master)
            echo "prod"
            ;;
        release/*)
            echo "staging"
            ;;
        develop|development)
            echo "dev"
            ;;
        feature/*|bugfix/*|hotfix/*)
            echo "dev"
            ;;
        *)
            echo "dev"
            ;;
    esac
}

# Function to validate tag format
validate_tag() {
    local tag="$1"

    # Check for valid characters (lowercase letters, digits, period, hyphen, underscore)
    if [[ ! "$tag" =~ ^[a-z0-9][a-z0-9._-]*$ ]]; then
        log_warning "Tag '$tag' contains invalid characters"
        return 1
    fi

    # Check length (max 128 characters)
    if [[ ${#tag} -gt 128 ]]; then
        log_warning "Tag '$tag' exceeds 128 characters"
        return 1
    fi

    return 0
}

# Function to apply a tag
apply_tag() {
    local source="$1"
    local target="$2"
    local tag="${target##*:}"

    # Validate tag
    if ! validate_tag "$tag"; then
        log_warning "Skipping invalid tag: $tag"
        return 1
    fi

    # Check if tag already exists
    if [[ "$FORCE" != "true" ]] && check_image_exists "$target"; then
        log_warning "Tag already exists: $target (use --force to override)"
        return 1
    fi

    if [[ "$DRY_RUN" == "true" ]]; then
        echo -e "${CYAN}[DRY-RUN]${NC} Would tag: $source -> $target"
    else
        log_info "Tagging: $source -> $target"
        docker tag "$source" "$target"
        log_success "Tagged: $target"

        if [[ "$PUSH" == "true" ]]; then
            log_info "Pushing: $target"
            docker push "$target"
            log_success "Pushed: $target"
        fi
    fi

    return 0
}

# Function to generate semantic version tags
generate_semver_tags() {
    local version="$1"
    local tags=()

    # Full version (1.2.3)
    tags+=("$version")

    # Version with 'v' prefix (v1.2.3)
    tags+=("v$version")

    # Parse semantic version components
    if [[ "$version" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)(.*)$ ]]; then
        local major="${BASH_REMATCH[1]}"
        local minor="${BASH_REMATCH[2]}"
        local patch="${BASH_REMATCH[3]}"
        local suffix="${BASH_REMATCH[4]}"

        # Only add major/minor tags for stable releases (no suffix)
        if [[ -z "$suffix" ]]; then
            # Major.Minor (1.2)
            tags+=("${major}.${minor}")

            # Major only (1)
            tags+=("${major}")
        fi
    fi

    echo "${tags[@]}"
}

# Function to generate environment-specific tags
generate_env_tags() {
    local env="$1"
    local version="$2"
    local date=$(date +%Y%m%d)
    local timestamp=$(date +%Y%m%d-%H%M%S)
    local tags=()

    case "$env" in
        prod|production)
            tags+=("prod")
            tags+=("prod-$version")
            tags+=("prod-stable")
            tags+=("stable")
            tags+=("rollback-$timestamp")
            ;;
        staging)
            tags+=("staging")
            tags+=("staging-$version")
            tags+=("staging-latest")
            tags+=("staging-$date")
            ;;
        dev|development)
            tags+=("dev")
            tags+=("dev-latest")
            tags+=("dev-$date")
            tags+=("latest")
            ;;
        test|testing)
            tags+=("test")
            tags+=("test-latest")
            tags+=("test-$date")
            ;;
        *)
            log_warning "Unknown environment: $env"
            ;;
    esac

    echo "${tags[@]}"
}

# Function to generate Git-based tags
generate_git_tags() {
    local tags=()
    local metadata=($(get_git_metadata))

    for item in "${metadata[@]}"; do
        case "$item" in
            sha:*)
                tags+=("git-${item#sha:}")
                ;;
            branch:*)
                local branch="${item#branch:}"
                if [[ "$branch" != "HEAD" ]]; then
                    tags+=("branch-$branch")

                    # Add specific tags for common branch types
                    case "$branch" in
                        feature-*)
                            tags+=("feature-${branch#feature-}")
                            ;;
                        bugfix-*)
                            tags+=("bugfix-${branch#bugfix-}")
                            ;;
                        hotfix-*)
                            tags+=("hotfix-${branch#hotfix-}")
                            ;;
                        release-*)
                            tags+=("release-${branch#release-}")
                            ;;
                    esac
                fi
                ;;
            dirty:true)
                tags+=("dirty-$(date +%Y%m%d-%H%M%S)")
                log_warning "Working directory has uncommitted changes"
                ;;
        esac
    done

    # Add PR tag if in CI environment
    if [[ -n "${GITHUB_EVENT_NAME:-}" ]] && [[ "${GITHUB_EVENT_NAME}" == "pull_request" ]]; then
        tags+=("pr-${GITHUB_PR_NUMBER:-unknown}")
    elif [[ -n "${CI_MERGE_REQUEST_IID:-}" ]]; then
        tags+=("pr-${CI_MERGE_REQUEST_IID}")
    fi

    echo "${tags[@]}"
}

# Main execution
main() {
    log_info "Starting image tagging for $SERVICE_NAME"
    log_debug "Source image: $SOURCE_IMAGE"

    # Check if source image exists
    if ! check_image_exists "$SOURCE_IMAGE"; then
        log_error "Source image does not exist: $SOURCE_IMAGE"
    fi

    # Get version and environment
    VERSION=$(get_version)
    ENVIRONMENT=$(determine_environment)

    log_info "Version: $VERSION"
    log_info "Environment: $ENVIRONMENT"

    # Collect all tags
    ALL_TAGS=()

    # Add semantic version tags
    if [[ "$NO_ENV_TAGS" != "true" ]]; then
        SEMVER_TAGS=($(generate_semver_tags "$VERSION"))
        ALL_TAGS+=("${SEMVER_TAGS[@]}")
        log_debug "Semantic version tags: ${SEMVER_TAGS[*]}"
    fi

    # Add environment tags
    if [[ "$NO_ENV_TAGS" != "true" ]]; then
        ENV_TAGS=($(generate_env_tags "$ENVIRONMENT" "$VERSION"))
        ALL_TAGS+=("${ENV_TAGS[@]}")
        log_debug "Environment tags: ${ENV_TAGS[*]}"
    fi

    # Add Git tags
    if [[ "$NO_GIT_TAGS" != "true" ]]; then
        GIT_TAGS=($(generate_git_tags))
        ALL_TAGS+=("${GIT_TAGS[@]}")
        log_debug "Git tags: ${GIT_TAGS[*]}"
    fi

    # Add extra tags if provided
    if [[ -n "$EXTRA_TAGS" ]]; then
        IFS=',' read -ra EXTRA_TAG_ARRAY <<< "$EXTRA_TAGS"
        ALL_TAGS+=("${EXTRA_TAG_ARRAY[@]}")
        log_debug "Extra tags: ${EXTRA_TAG_ARRAY[*]}"
    fi

    # Remove duplicates
    ALL_TAGS=($(echo "${ALL_TAGS[@]}" | tr ' ' '\n' | sort -u | tr '\n' ' '))

    log_info "Total tags to apply: ${#ALL_TAGS[@]}"

    # Apply all tags
    SUCCESS_COUNT=0
    FAILED_COUNT=0

    for tag in "${ALL_TAGS[@]}"; do
        TARGET_IMAGE="${REGISTRY}/${SERVICE_NAME}:${tag}"
        if apply_tag "$SOURCE_IMAGE" "$TARGET_IMAGE"; then
            ((SUCCESS_COUNT++))
        else
            ((FAILED_COUNT++))
        fi
    done

    # Summary
    echo ""
    log_info "Tagging complete!"
    log_success "Successfully applied $SUCCESS_COUNT tags"

    if [[ $FAILED_COUNT -gt 0 ]]; then
        log_warning "Failed to apply $FAILED_COUNT tags"
    fi

    # Cleanup source tag if requested
    if [[ "$CLEANUP" == "true" ]] && [[ "$DRY_RUN" != "true" ]]; then
        log_info "Cleaning up source image: $SOURCE_IMAGE"
        docker rmi "$SOURCE_IMAGE"
        log_success "Removed source image"
    fi

    # List all tags for the service
    if [[ "$VERBOSE" == "true" ]] && [[ "$DRY_RUN" != "true" ]]; then
        echo ""
        log_info "Current tags for $SERVICE_NAME:"
        docker images "${REGISTRY}/${SERVICE_NAME}" --format "table {{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
    fi

    # Exit with error if any tags failed
    if [[ $FAILED_COUNT -gt 0 ]]; then
        exit 1
    fi
}

# Run main function
main