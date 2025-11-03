# Docker Build Quick Reference

**Version**: 1.0.0
**Date**: 2025-11-01
**Purpose**: Quick command reference for Docker build operations

---

## 🚀 Quick Commands

### Build Operations

```bash
# Build single service
./scripts/build-service.sh gateway

# Build all services
./scripts/build-all.sh

# Build with no cache
./scripts/build-service.sh gateway --no-cache

# Build specific version
./scripts/build-service.sh gateway --version 1.2.3

# Build for production
./scripts/build-service.sh gateway --env prod

# Parallel build (all services)
./scripts/build-all.sh --parallel
```

### Security Scanning

```bash
# Scan single image
./scripts/security-scan.sh gateway

# Scan all images
./scripts/security-scan-all.sh

# Scan with specific severity
trivy image --severity CRITICAL,HIGH gcrf-library/gateway-service:latest

# Generate SBOM
trivy image --format cyclonedx --output gateway-sbom.json gcrf-library/gateway-service:latest

# Scan and fail on critical
./scripts/security-scan.sh gateway --fail-on CRITICAL
```

### Image Tagging

```bash
# Tag for environment
./scripts/tag-image.sh gateway dev
./scripts/tag-image.sh gateway staging
./scripts/tag-image.sh gateway prod

# Tag with version
./scripts/tag-image.sh gateway --version 1.2.3

# Tag with Git commit
./scripts/tag-image.sh gateway --git-commit

# Tag for rollback
./scripts/tag-image.sh gateway --backup

# Apply all tags
./scripts/tag-all.sh
```

### Registry Operations

```bash
# Login to registry
docker login registry.gcrf.local:5000

# Push single image
./scripts/push-image.sh gateway

# Push all images
./scripts/push-all.sh

# Push specific tag
docker push registry.gcrf.local:5000/gcrf-library/gateway-service:1.2.3

# Pull from registry
docker pull registry.gcrf.local:5000/gcrf-library/gateway-service:latest
```

### Health Checks

```bash
# Check single service
./scripts/health-check.sh gateway

# Check all services
./scripts/health-check-all.sh

# Manual health check
curl http://localhost:8080/actuator/health

# Detailed health info
curl http://localhost:8080/actuator/health | jq '.'
```

### Cleanup Operations

```bash
# Remove unused images
docker image prune -f

# Remove all stopped containers
docker container prune -f

# Clean build cache
docker builder prune -f

# Deep clean (careful!)
docker system prune -a --volumes -f

# Remove specific service images
./scripts/cleanup.sh gateway

# Remove old tags
./scripts/cleanup-old-tags.sh --days 30
```

---

## 📋 Common Workflows

### Complete Build and Deploy

```bash
# 1. Clean environment
./scripts/cleanup.sh --all

# 2. Build all services
./scripts/build-all.sh --parallel

# 3. Run security scans
./scripts/security-scan-all.sh

# 4. Tag for environment
./scripts/tag-all.sh prod

# 5. Push to registry
./scripts/push-all.sh

# 6. Deploy
kubectl apply -f k8s/
```

### Development Workflow

```bash
# 1. Build service
./scripts/build-service.sh gateway --cache

# 2. Run locally
docker run -d --name gateway-dev -p 8080:8080 gcrf-library/gateway-service:latest

# 3. Check logs
docker logs -f gateway-dev

# 4. Test endpoint
curl http://localhost:8080/actuator/health

# 5. Stop and remove
docker stop gateway-dev && docker rm gateway-dev
```

### Hotfix Workflow

```bash
# 1. Create hotfix branch
git checkout -b hotfix/security-patch

# 2. Apply fix and commit
git commit -am "fix: security vulnerability in gateway"

# 3. Build affected service
./scripts/build-service.sh gateway --no-cache

# 4. Security scan
./scripts/security-scan.sh gateway --fail-on CRITICAL

# 5. Tag as hotfix
./scripts/tag-image.sh gateway --hotfix

# 6. Deploy hotfix
./scripts/deploy-hotfix.sh gateway
```

---

## 🔧 Troubleshooting Quick Fixes

### Build Failures

```bash
# Clear Maven cache
docker volume rm maven-cache

# Rebuild without cache
docker build --no-cache .

# Verbose build output
docker build --progress=plain .

# Check Java version
java --version  # Should be 21
```

### Cache Issues

```bash
# Enable BuildKit
export DOCKER_BUILDKIT=1

# Clear BuildKit cache
docker buildx prune -a

# Check cache usage
docker buildx du
```

### Registry Issues

```bash
# Re-authenticate
docker logout registry.gcrf.local:5000
docker login registry.gcrf.local:5000

# Test connectivity
curl -I https://registry.gcrf.local:5000/v2/

# Check credentials
cat ~/.docker/config.json | jq '.auths'
```

### Container Issues

```bash
# Container won't start
docker logs CONTAINER_ID

# Debug interactively
docker run -it --entrypoint /bin/sh IMAGE:TAG

# Check resource limits
docker stats CONTAINER_ID

# Inspect container
docker inspect CONTAINER_ID
```

---

## 📊 Performance Commands

### Build Metrics

```bash
# Time build
time ./scripts/build-service.sh gateway

# Monitor resource usage
docker stats

# Check image size
docker images | grep gcrf-library

# Analyze layers
docker history gcrf-library/gateway-service:latest
```

### Optimization

```bash
# Enable all optimizations
export DOCKER_BUILDKIT=1
export BUILDKIT_PROGRESS=plain
export COMPOSE_DOCKER_CLI_BUILD=1

# Multi-platform build
docker buildx build --platform linux/amd64,linux/arm64 .

# Use registry cache
docker buildx build \
  --cache-from type=registry,ref=registry/image:buildcache \
  --cache-to type=registry,ref=registry/image:buildcache .
```

---

## 🔐 Security Commands

### Vulnerability Scanning

```bash
# Quick scan
trivy image IMAGE:TAG

# Detailed JSON report
trivy image --format json --output report.json IMAGE:TAG

# Check specific CVE
trivy image IMAGE:TAG | grep CVE-2023-12345

# Scan Dockerfile
trivy config Dockerfile
```

### Security Hardening

```bash
# Run as non-root
docker run --user 1000:1000 IMAGE:TAG

# Read-only filesystem
docker run --read-only IMAGE:TAG

# Drop capabilities
docker run --cap-drop=ALL IMAGE:TAG

# Security options
docker run --security-opt no-new-privileges IMAGE:TAG
```

---

## 📝 Configuration Files

### .dockerignore

```
target/
*.log
*.tmp
.git/
.idea/
*.iml
.env
```

### Docker Build Config

```json
{
  "experimental": true,
  "buildkit": true,
  "features": {
    "buildkit": true
  }
}
```

### BuildKit Config

```toml
# /etc/buildkit/buildkitd.toml
[worker.oci]
  max-parallelism = 4

[registry."registry.gcrf.local:5000"]
  insecure = true
```

---

## 🏷️ Tagging Conventions

```bash
# Version tags
IMAGE:1.0.0          # Specific version
IMAGE:1.0            # Minor version
IMAGE:1              # Major version
IMAGE:latest         # Latest build

# Environment tags
IMAGE:dev            # Development
IMAGE:staging        # Staging
IMAGE:prod           # Production

# Git tags
IMAGE:main           # Branch
IMAGE:abc123         # Commit hash
IMAGE:main-abc123    # Branch + commit

# Date tags
IMAGE:20251101       # Daily build
IMAGE:2025-w44       # Weekly build
```

---

## 🔄 CI/CD Commands

### GitHub Actions

```bash
# Test workflow locally
act -j build

# Trigger workflow
gh workflow run docker-build.yml

# View runs
gh run list --workflow=docker-build.yml
```

### GitLab CI

```bash
# Validate CI file
gitlab-ci-lint .gitlab-ci.yml

# Trigger pipeline
git push origin feature-branch

# View pipelines
gitlab pipeline list
```

### Jenkins

```bash
# Trigger build
curl -X POST http://jenkins/job/gcrf-library/build

# View console output
curl http://jenkins/job/gcrf-library/lastBuild/consoleText
```

---

## 📦 Volume Management

```bash
# List volumes
docker volume ls

# Create cache volume
docker volume create maven-cache

# Inspect volume
docker volume inspect maven-cache

# Clean unused volumes
docker volume prune -f

# Backup volume
docker run --rm -v maven-cache:/data -v $(pwd):/backup \
  alpine tar czf /backup/maven-cache.tar.gz /data
```

---

## 🚨 Emergency Commands

### Service Recovery

```bash
# Quick restart
docker restart CONTAINER

# Force recreate
docker-compose up -d --force-recreate SERVICE

# Rollback to previous
./scripts/rollback.sh gateway 1.0.0

# Emergency stop all
docker stop $(docker ps -q)
```

### Diagnostics

```bash
# System info
docker system info

# Disk usage
docker system df

# Events stream
docker system events

# Full diagnostic
./scripts/diagnostic.sh > diagnostic-$(date +%Y%m%d-%H%M%S).log
```

---

## 📚 Help Commands

```bash
# Script help
./scripts/build-service.sh --help

# Docker help
docker build --help
docker run --help

# BuildKit options
docker buildx build --help

# Trivy help
trivy image --help
```

---

## 🔗 Quick Links

- [Master Guide](./DOCKER_BUILD_MASTER_GUIDE.md)
- [Build Optimization](./BUILD_OPTIMIZATION.md)
- [Security Scanning](./SECURITY_SCANNING.md)
- [CI/CD Integration](./CI_CD_INTEGRATION.md)
- [Troubleshooting Guide](./DOCKER_BUILD_MASTER_GUIDE.md#10-troubleshooting)

---

**Pro Tips**:
- Always use `--help` flag to see all options
- Set up aliases for frequently used commands
- Keep this reference handy during deployments
- Update commands as new scripts are added

---

**Last Updated**: 2025-11-01
**Version**: 1.0.0