# GCRF Library Management System - CI/CD Integration Guide

**Version**: 1.0.0
**Last Updated**: 2025-11-01
**Purpose**: Integrate optimized Docker builds into CI/CD pipelines

---

## Table of Contents

1. [Overview](#overview)
2. [GitHub Actions](#github-actions)
3. [GitLab CI](#gitlab-ci)
4. [Jenkins](#jenkins)
5. [Azure DevOps](#azure-devops)
6. [Cache Strategies](#cache-strategies)
7. [Best Practices](#best-practices)

---

## Overview

### Benefits of Optimized Builds in CI/CD

- **Faster pipeline execution**: 40-65% reduction in build time
- **Cost savings**: Reduced compute time = lower CI/CD costs
- **Improved developer experience**: Faster feedback loops
- **Better cache utilization**: Registry-based cache persists across runners

### Key Techniques

1. **Registry Cache Export/Import**: Persist cache between pipeline runs
2. **Parallel Service Builds**: Build multiple services simultaneously
3. **Cache Warming**: Scheduled builds to keep cache fresh
4. **Layer Reuse**: Maximize cache hits through strategic ordering

---

## GitHub Actions

### Complete Workflow Example

**File**: `.github/workflows/build-services.yml`

```yaml
name: Build and Push Services

on:
  push:
    branches: [ main, develop ]
    paths:
      - 'backend/**'
      - '.github/workflows/build-services.yml'
  pull_request:
    branches: [ main ]

env:
  REGISTRY: ghcr.io
  IMAGE_PREFIX: gcrf-library

jobs:
  # Job 1: Build Gateway Service
  build-gateway:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
        with:
          driver-opts: |
            network=host

      - name: Log in to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ github.repository_owner }}/${{ env.IMAGE_PREFIX }}/gateway
          tags: |
            type=ref,event=branch
            type=ref,event=pr
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}
            type=sha,prefix={{branch}}-

      - name: Build and push Gateway Service
        uses: docker/build-push-action@v5
        with:
          context: ./backend
          file: ./backend/gateway-service/Dockerfile.optimized
          platforms: linux/amd64,linux/arm64
          push: ${{ github.event_name != 'pull_request' }}
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: |
            type=registry,ref=${{ env.REGISTRY }}/${{ github.repository_owner }}/${{ env.IMAGE_PREFIX }}/gateway:cache
          cache-to: |
            type=registry,ref=${{ env.REGISTRY }}/${{ github.repository_owner }}/${{ env.IMAGE_PREFIX }}/gateway:cache,mode=max
          build-args: |
            BUILDKIT_INLINE_CACHE=1
            SPRING_PROFILES_ACTIVE=prod

  # Job 2: Build Auth Service (Parallel)
  build-auth:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ github.repository_owner }}/${{ env.IMAGE_PREFIX }}/auth
          tags: |
            type=ref,event=branch
            type=sha,prefix={{branch}}-

      - name: Build and push Auth Service
        uses: docker/build-push-action@v5
        with:
          context: ./backend
          file: ./backend/auth-service/Dockerfile.optimized
          platforms: linux/amd64
          push: ${{ github.event_name != 'pull_request' }}
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: |
            type=registry,ref=${{ env.REGISTRY }}/${{ github.repository_owner }}/${{ env.IMAGE_PREFIX }}/auth:cache
          cache-to: |
            type=registry,ref=${{ env.REGISTRY }}/${{ github.repository_owner }}/${{ env.IMAGE_PREFIX }}/auth:cache,mode=max

  # Job 3: Build Report
  build-summary:
    runs-on: ubuntu-latest
    needs: [build-gateway, build-auth]
    if: always()

    steps:
      - name: Generate build summary
        run: |
          echo "## Build Summary" >> $GITHUB_STEP_SUMMARY
          echo "- Gateway: ${{ needs.build-gateway.result }}" >> $GITHUB_STEP_SUMMARY
          echo "- Auth: ${{ needs.build-auth.result }}" >> $GITHUB_STEP_SUMMARY
```

### Cache Warming Job

```yaml
name: Warm Build Cache

on:
  schedule:
    # Run daily at 2 AM UTC
    - cron: '0 2 * * *'
  workflow_dispatch:  # Allow manual trigger

jobs:
  warm-cache:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [gateway, auth, book, circulation, reader, system, notification]

    steps:
      - uses: actions/checkout@v4

      - uses: docker/setup-buildx-action@v3

      - uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Warm cache for ${{ matrix.service }}
        uses: docker/build-push-action@v5
        with:
          context: ./backend
          file: ./backend/${{ matrix.service }}-service/Dockerfile.optimized
          platforms: linux/amd64
          push: false
          cache-to: type=registry,ref=${{ env.REGISTRY }}/gcrf-library/${{ matrix.service }}:cache,mode=max
```

---

## GitLab CI

### Complete Pipeline Example

**File**: `.gitlab-ci.yml`

```yaml
variables:
  DOCKER_DRIVER: overlay2
  DOCKER_TLS_CERTDIR: ""
  REGISTRY: $CI_REGISTRY
  IMAGE_PREFIX: gcrf-library

stages:
  - build
  - test
  - deploy

# Template for service builds
.build-service:
  stage: build
  image: docker:latest
  services:
    - docker:dind
  before_script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
    - apk add --no-cache git
  script:
    - |
      docker buildx create --use --driver docker-container --name builder || true
      docker buildx build \
        --platform linux/amd64 \
        --file backend/${SERVICE_NAME}-service/Dockerfile.optimized \
        --tag ${REGISTRY}/${IMAGE_PREFIX}/${SERVICE_NAME}:${CI_COMMIT_SHA} \
        --tag ${REGISTRY}/${IMAGE_PREFIX}/${SERVICE_NAME}:${CI_COMMIT_REF_SLUG} \
        --cache-from type=registry,ref=${REGISTRY}/${IMAGE_PREFIX}/${SERVICE_NAME}:cache \
        --cache-to type=registry,ref=${REGISTRY}/${IMAGE_PREFIX}/${SERVICE_NAME}:cache,mode=max \
        --push \
        ./backend
  tags:
    - docker

# Gateway Service
build:gateway:
  extends: .build-service
  variables:
    SERVICE_NAME: gateway
  only:
    changes:
      - backend/gateway-service/**
      - backend/common/**
      - .gitlab-ci.yml

# Auth Service
build:auth:
  extends: .build-service
  variables:
    SERVICE_NAME: auth
  only:
    changes:
      - backend/auth-service/**
      - backend/common/**
      - .gitlab-ci.yml

# Parallel build all services (manual trigger)
build:all:
  stage: build
  image: docker:latest
  services:
    - docker:dind
  script:
    - apk add --no-cache bash parallel
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
    - ./deployment/scripts/docker-build-optimized.sh -r ${REGISTRY}/${IMAGE_PREFIX} -t ${CI_COMMIT_SHA} build-parallel gateway auth
  when: manual
  tags:
    - docker

# Cache warming (scheduled)
warm-cache:
  stage: build
  image: docker:latest
  services:
    - docker:dind
  script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
    - |
      for service in gateway auth book circulation reader system notification; do
        docker buildx build \
          --platform linux/amd64 \
          --file backend/${service}-service/Dockerfile.optimized \
          --cache-to type=registry,ref=${REGISTRY}/${IMAGE_PREFIX}/${service}:cache,mode=max \
          --push=false \
          ./backend || true
      done
  only:
    - schedules
  tags:
    - docker
```

---

## Jenkins

### Declarative Pipeline

**File**: `Jenkinsfile`

```groovy
pipeline {
    agent any

    environment {
        REGISTRY = credentials('docker-registry-url')
        REGISTRY_CREDENTIALS = credentials('docker-registry-credentials')
        IMAGE_PREFIX = 'gcrf-library'
        BUILDKIT_PROGRESS = 'plain'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    stages {
        stage('Setup') {
            steps {
                script {
                    // Enable BuildKit
                    env.DOCKER_BUILDKIT = '1'

                    // Set image tags
                    env.IMAGE_TAG = "${env.GIT_COMMIT.take(8)}"
                    env.BRANCH_TAG = env.GIT_BRANCH.replaceAll('/', '-')
                }

                sh 'docker buildx create --use --name jenkins-builder || true'
            }
        }

        stage('Build Services') {
            parallel {
                stage('Build Gateway') {
                    steps {
                        script {
                            buildService('gateway')
                        }
                    }
                }

                stage('Build Auth') {
                    steps {
                        script {
                            buildService('auth')
                        }
                    }
                }

                // Add more services as needed
            }
        }

        stage('Test Images') {
            steps {
                sh '''
                    docker run --rm ${REGISTRY}/${IMAGE_PREFIX}/gateway:${IMAGE_TAG} java -version
                    docker run --rm ${REGISTRY}/${IMAGE_PREFIX}/auth:${IMAGE_TAG} java -version
                '''
            }
        }

        stage('Push Images') {
            when {
                branch 'main'
            }
            steps {
                script {
                    docker.withRegistry("https://${REGISTRY}", REGISTRY_CREDENTIALS) {
                        sh '''
                            docker push ${REGISTRY}/${IMAGE_PREFIX}/gateway:${IMAGE_TAG}
                            docker push ${REGISTRY}/${IMAGE_PREFIX}/auth:${IMAGE_TAG}

                            # Tag as latest
                            docker tag ${REGISTRY}/${IMAGE_PREFIX}/gateway:${IMAGE_TAG} ${REGISTRY}/${IMAGE_PREFIX}/gateway:latest
                            docker tag ${REGISTRY}/${IMAGE_PREFIX}/auth:${IMAGE_TAG} ${REGISTRY}/${IMAGE_PREFIX}/auth:latest

                            docker push ${REGISTRY}/${IMAGE_PREFIX}/gateway:latest
                            docker push ${REGISTRY}/${IMAGE_PREFIX}/auth:latest
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            sh 'docker system prune -f || true'
        }
        success {
            echo 'Build succeeded!'
        }
        failure {
            echo 'Build failed!'
        }
    }
}

// Helper function to build a service
def buildService(serviceName) {
    sh """
        docker buildx build \
            --platform linux/amd64 \
            --file backend/${serviceName}-service/Dockerfile.optimized \
            --tag ${REGISTRY}/${IMAGE_PREFIX}/${serviceName}:${IMAGE_TAG} \
            --tag ${REGISTRY}/${IMAGE_PREFIX}/${serviceName}:${BRANCH_TAG} \
            --cache-from type=registry,ref=${REGISTRY}/${IMAGE_PREFIX}/${serviceName}:cache \
            --cache-to type=registry,ref=${REGISTRY}/${IMAGE_PREFIX}/${serviceName}:cache,mode=max \
            --load \
            ./backend
    """
}
```

### Scripted Pipeline with Build Script

```groovy
node {
    stage('Checkout') {
        checkout scm
    }

    stage('Build All Services') {
        sh """
            ./deployment/scripts/docker-build-optimized.sh \
                -r ${REGISTRY}/${IMAGE_PREFIX} \
                -t ${env.BUILD_NUMBER} \
                -j 4 \
                build-parallel gateway auth book circulation
        """
    }

    stage('Run Benchmark') {
        sh './deployment/scripts/benchmark-build.sh gateway'
        archiveArtifacts artifacts: '/tmp/gcrf-benchmark-*.txt', allowEmptyArchive: true
    }
}
```

---

## Azure DevOps

### Pipeline YAML

**File**: `azure-pipelines.yml`

```yaml
trigger:
  branches:
    include:
      - main
      - develop
  paths:
    include:
      - backend/**

pool:
  vmImage: 'ubuntu-latest'

variables:
  dockerRegistryServiceConnection: 'ACR-Connection'
  imageRepository: 'gcrf-library'
  containerRegistry: 'gcrf.azurecr.io'
  dockerfilePath: 'backend/gateway-service/Dockerfile.optimized'
  tag: '$(Build.BuildId)'

stages:
- stage: Build
  displayName: Build Services
  jobs:
  - job: BuildGateway
    displayName: Build Gateway Service
    steps:
    - task: Docker@2
      displayName: Build Gateway image
      inputs:
        containerRegistry: $(dockerRegistryServiceConnection)
        repository: $(imageRepository)/gateway
        command: build
        Dockerfile: backend/gateway-service/Dockerfile.optimized
        buildContext: backend
        tags: |
          $(tag)
          latest
        arguments: |
          --cache-from type=registry,ref=$(containerRegistry)/$(imageRepository)/gateway:cache
          --cache-to type=registry,ref=$(containerRegistry)/$(imageRepository)/gateway:cache,mode=max
          --build-arg BUILDKIT_INLINE_CACHE=1

    - task: Docker@2
      displayName: Push Gateway image
      inputs:
        containerRegistry: $(dockerRegistryServiceConnection)
        repository: $(imageRepository)/gateway
        command: push
        tags: |
          $(tag)
          latest

  - job: BuildAuth
    displayName: Build Auth Service
    steps:
    - task: Docker@2
      displayName: Build Auth image
      inputs:
        containerRegistry: $(dockerRegistryServiceConnection)
        repository: $(imageRepository)/auth
        command: build
        Dockerfile: backend/auth-service/Dockerfile.optimized
        buildContext: backend
        tags: |
          $(tag)
          latest
        arguments: |
          --cache-from type=registry,ref=$(containerRegistry)/$(imageRepository)/auth:cache
          --cache-to type=registry,ref=$(containerRegistry)/$(imageRepository)/auth:cache,mode=max

    - task: Docker@2
      displayName: Push Auth image
      inputs:
        containerRegistry: $(dockerRegistryServiceConnection)
        repository: $(imageRepository)/auth
        command: push
        tags: |
          $(tag)
          latest

- stage: Test
  displayName: Test Images
  dependsOn: Build
  jobs:
  - job: TestImages
    displayName: Verify Images
    steps:
    - script: |
        docker run --rm $(containerRegistry)/$(imageRepository)/gateway:$(tag) java -version
        docker run --rm $(containerRegistry)/$(imageRepository)/auth:$(tag) java -version
      displayName: Run smoke tests
```

---

## Cache Strategies

### Local Cache (Development)

**Best for**: Local development, single runner

```bash
# Use Docker volume
docker volume create maven-cache

# Build with cache mount
docker buildx build \
  --file Dockerfile.optimized \
  --tag myapp:dev \
  --load \
  .
```

**Pros**:
- Fastest cache access
- No network overhead
- Simple setup

**Cons**:
- Not shared across runners
- Lost when runner is destroyed

### Registry Cache (CI/CD)

**Best for**: Distributed CI/CD, multiple runners

```bash
# Export cache to registry
docker buildx build \
  --cache-from type=registry,ref=myregistry/myapp:cache \
  --cache-to type=registry,ref=myregistry/myapp:cache,mode=max \
  --tag myregistry/myapp:latest \
  --push \
  .
```

**Pros**:
- Shared across all runners
- Persists indefinitely
- Works with ephemeral runners

**Cons**:
- Network overhead
- Registry storage costs
- Slower than local cache

### Inline Cache

**Best for**: Simple setups, minimal configuration

```bash
# Build with inline cache
docker buildx build \
  --build-arg BUILDKIT_INLINE_CACHE=1 \
  --tag myapp:latest \
  --push \
  .

# Use inline cache
docker buildx build \
  --cache-from myapp:latest \
  --tag myapp:v2 \
  .
```

**Pros**:
- No separate cache image
- Simpler pipeline configuration

**Cons**:
- Larger image size
- Less efficient than dedicated cache

### Cache Mode Comparison

| Mode | Description | Use Case | Size Overhead |
|------|-------------|----------|---------------|
| `mode=min` | Only final stage | Production images | Minimal |
| `mode=max` | All stages | Development, CI/CD | Moderate |
| `inline` | Embedded in image | Simple pipelines | High |

---

## Best Practices

### 1. Cache Warming

Schedule daily builds to keep cache fresh:

```yaml
# GitHub Actions
on:
  schedule:
    - cron: '0 2 * * *'  # 2 AM daily
```

### 2. Cache Isolation

Use separate cache per branch:

```bash
# Branch-specific cache
--cache-from type=registry,ref=myapp:cache-${BRANCH_NAME}
--cache-to type=registry,ref=myapp:cache-${BRANCH_NAME},mode=max
```

### 3. Multi-platform Builds

Build for multiple architectures:

```bash
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --cache-from type=registry,ref=myapp:cache \
  --cache-to type=registry,ref=myapp:cache,mode=max \
  --push \
  .
```

### 4. Parallel Service Builds

Use pipeline parallelization:

```yaml
# GitLab CI
build:all:
  parallel:
    matrix:
      - SERVICE: [gateway, auth, book]
```

### 5. Build Metrics

Track build performance:

```bash
# Time each build
time docker buildx build ...

# Log cache hits
docker buildx build --progress=plain ... | grep CACHED
```

### 6. Cost Optimization

- Use `mode=max` only for development branches
- Use `mode=min` for production releases
- Clean old cache images regularly
- Consider cache size limits in registry

### 7. Security Scanning

Integrate security scanning:

```yaml
- name: Scan image
  uses: aquasecurity/trivy-action@master
  with:
    image-ref: 'myapp:${{ github.sha }}'
    format: 'sarif'
    output: 'trivy-results.sarif'
```

---

## Troubleshooting CI/CD Issues

### Issue: Cache Not Used

**Symptoms**: Build time same as cold build

**Solution**:
```bash
# Verify cache exists
docker pull myregistry/myapp:cache

# Check cache-from syntax
--cache-from type=registry,ref=myregistry/myapp:cache
```

### Issue: Authentication Failures

**Symptoms**: "unauthorized: authentication required"

**Solution**:
```bash
# GitHub Actions
- uses: docker/login-action@v3
  with:
    registry: ghcr.io
    username: ${{ github.actor }}
    password: ${{ secrets.GITHUB_TOKEN }}

# GitLab CI
docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
```

### Issue: Parallel Build Conflicts

**Symptoms**: "cache mount conflict"

**Solution**: Use unique cache IDs per service:

```dockerfile
RUN --mount=type=cache,target=/root/.m2,id=maven-${SERVICE_NAME}
```

---

## Summary

### Key Takeaways

✅ Use **registry cache** for distributed CI/CD
✅ Enable **parallel builds** for multiple services
✅ Implement **cache warming** for best performance
✅ Use **mode=max** in development, **mode=min** in production
✅ Monitor **build metrics** to track improvements

### Quick Start Checklist

- [ ] Enable BuildKit in CI/CD runner
- [ ] Configure registry authentication
- [ ] Add cache-from/cache-to to build commands
- [ ] Set up parallel service builds
- [ ] Schedule cache warming job
- [ ] Monitor build times and cache hit rates

---

**Document Version**: 1.0.0
**Last Updated**: 2025-11-01
**Maintained By**: GCRF DevOps Team
**Related Docs**: `BUILD_OPTIMIZATION.md`, `deployment/scripts/docker-build-optimized.sh`
