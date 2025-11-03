#!/bin/bash

###############################################################################
# CI/CD Image Tagging Example Script
# Version: 1.0.0
# Description: Example script showing how to integrate tagging in CI/CD pipelines
# Author: GCRF Library Management System Team
# Date: 2025-01-01
###############################################################################

set -euo pipefail

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TAG_SCRIPT="$SCRIPT_DIR/tag-image.sh"

# Configuration
REGISTRY="${DOCKER_REGISTRY:-gcrf-library}"
SERVICE_NAME="${1:-gateway-service}"
BUILD_NUMBER="${BUILD_NUMBER:-$(date +%Y%m%d-%H%M%S)}"

echo "="
echo "CI/CD Tagging Example for $SERVICE_NAME"
echo "="

# Function to determine environment from branch
get_environment() {
    local branch="${CI_BRANCH:-${GITHUB_REF_NAME:-${CI_COMMIT_REF_NAME:-$(git rev-parse --abbrev-ref HEAD)}}}"

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
        *)
            echo "dev"
            ;;
    esac
}

# Function to extract version
get_version() {
    # Try CI/CD environment variables first
    if [[ -n "${CI_COMMIT_TAG:-}" ]]; then
        echo "${CI_COMMIT_TAG#v}"
    elif [[ -n "${GITHUB_REF:-}" ]] && [[ "$GITHUB_REF" == refs/tags/* ]]; then
        echo "${GITHUB_REF#refs/tags/v}"
    elif [[ -f "package.json" ]]; then
        grep '"version"' package.json | sed 's/.*"version": "\(.*\)".*/\1/'
    elif [[ -f "pom.xml" ]]; then
        grep -m1 '<version>' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/'
    else
        echo "0.0.0-ci-$BUILD_NUMBER"
    fi
}

# Main execution
main() {
    local environment=$(get_environment)
    local version=$(get_version)

    echo "Environment: $environment"
    echo "Version: $version"
    echo "Build Number: $BUILD_NUMBER"
    echo ""

    # Build the image first
    echo "Step 1: Building Docker image..."
    docker build -t "$REGISTRY/$SERVICE_NAME:build-$BUILD_NUMBER" .

    echo ""
    echo "Step 2: Applying tags based on environment..."

    # Apply tags based on environment
    case "$environment" in
        prod)
            echo "Production deployment detected"

            # Tag with version numbers
            "$TAG_SCRIPT" \
                --env prod \
                --version "$version" \
                --extra-tags "build-$BUILD_NUMBER" \
                --verbose \
                --dry-run \
                "$SERVICE_NAME" \
                "build-$BUILD_NUMBER"

            # Ask for confirmation in production
            read -p "Apply production tags? (y/n): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                "$TAG_SCRIPT" \
                    --env prod \
                    --version "$version" \
                    --extra-tags "build-$BUILD_NUMBER" \
                    --push \
                    "$SERVICE_NAME" \
                    "build-$BUILD_NUMBER"

                echo "Production tags applied and pushed!"
            fi
            ;;

        staging)
            echo "Staging deployment detected"

            "$TAG_SCRIPT" \
                --env staging \
                --version "$version" \
                --extra-tags "build-$BUILD_NUMBER,staging-candidate" \
                --push \
                "$SERVICE_NAME" \
                "build-$BUILD_NUMBER"

            echo "Staging tags applied and pushed!"
            ;;

        dev)
            echo "Development deployment detected"

            "$TAG_SCRIPT" \
                --env dev \
                --version "$version" \
                --extra-tags "build-$BUILD_NUMBER,ci-$BUILD_NUMBER" \
                --push \
                "$SERVICE_NAME" \
                "build-$BUILD_NUMBER"

            echo "Development tags applied and pushed!"
            ;;
    esac

    echo ""
    echo "Step 3: Cleanup old build tag..."
    docker rmi "$REGISTRY/$SERVICE_NAME:build-$BUILD_NUMBER" || true

    echo ""
    echo "Tagging complete!"
    echo ""
    echo "Tagged images:"
    docker images "$REGISTRY/$SERVICE_NAME" --format "table {{.Tag}}\t{{.Size}}\t{{.CreatedAt}}" | head -20
}

# GitHub Actions specific
if [[ -n "${GITHUB_ACTIONS:-}" ]]; then
    echo "Running in GitHub Actions"

    # Set outputs for GitHub Actions
    echo "environment=$environment" >> "$GITHUB_OUTPUT"
    echo "version=$version" >> "$GITHUB_OUTPUT"
    echo "build_number=$BUILD_NUMBER" >> "$GITHUB_OUTPUT"
fi

# GitLab CI specific
if [[ -n "${GITLAB_CI:-}" ]]; then
    echo "Running in GitLab CI"

    # Export variables for GitLab CI
    echo "DEPLOYMENT_ENV=$environment" >> build.env
    echo "APP_VERSION=$version" >> build.env
    echo "BUILD_ID=$BUILD_NUMBER" >> build.env
fi

# Jenkins specific
if [[ -n "${JENKINS_HOME:-}" ]]; then
    echo "Running in Jenkins"

    # Set Jenkins environment variables
    echo "DEPLOYMENT_ENV=$environment" > env.properties
    echo "APP_VERSION=$version" >> env.properties
    echo "BUILD_ID=$BUILD_NUMBER" >> env.properties
fi

# Run main function
main