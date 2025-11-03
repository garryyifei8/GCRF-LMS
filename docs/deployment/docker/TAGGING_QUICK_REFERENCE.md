# Docker Image Tagging Quick Reference

## 🚀 Quick Start Commands

### Production Release
```bash
# Tag version 1.2.0 for production
./scripts/tag-image.sh \
  --env prod \
  --version 1.2.0 \
  --push \
  gateway-service build
```

### Emergency Rollback (< 5 minutes)
```bash
# Quick rollback to previous version
./scripts/rollback-service.sh gateway-service

# Rollback to specific version
./scripts/rollback-service.sh gateway-service v1.1.0

# Force rollback (skip confirmations)
./scripts/rollback-service.sh --force gateway-service v1.1.0
```

### Development Tagging
```bash
# Tag for development
./scripts/tag-image.sh \
  --env dev \
  --push \
  gateway-service latest
```

### Cleanup Old Images
```bash
# Dry run (see what would be deleted)
./scripts/cleanup-tags.sh

# Actually delete old images
./scripts/cleanup-tags.sh --force

# Clean only dev images
./scripts/cleanup-tags.sh --env dev --force
```

## 📋 Tag Naming Patterns

| Environment | Pattern | Example | Retention |
|------------|---------|---------|-----------|
| **Production** | | | |
| | `v{version}` | `v1.2.0` | Forever |
| | `prod-{version}` | `prod-1.2.0` | Forever |
| | `prod-stable` | `prod-stable` | Current |
| **Staging** | | | |
| | `staging-{version}` | `staging-1.2.0` | 90 days |
| | `staging-{date}` | `staging-20250101` | 60 days |
| **Development** | | | |
| | `dev` | `dev` | Current |
| | `dev-{date}` | `dev-20250101` | 30 days |
| | `dev-{branch}` | `dev-feature-auth` | 7 days |
| **Git-based** | | | |
| | `git-{sha}` | `git-a1b2c3d` | 60 days |
| | `branch-{name}` | `branch-main` | 30 days |
| | `pr-{number}` | `pr-123` | 14 days |

## 🔒 Protected Tags (Never Deleted)

- `latest`
- `stable`
- `prod`
- `prod-stable`
- `v*` (all version tags)
- `prod-*` (all production tags)

## 🎯 Common Scenarios

### Scenario 1: Production Release
```bash
# 1. Build image
docker build -t gcrf-library/gateway-service:build .

# 2. Apply production tags
./scripts/tag-image.sh \
  --env prod \
  --version 1.2.0 \
  --push \
  gateway-service build

# 3. Deploy
kubectl set image deployment/gateway \
  gateway=gcrf-library/gateway-service:v1.2.0
```

### Scenario 2: Hotfix Deployment
```bash
# 1. Build hotfix
docker build -t gcrf-library/auth-service:hotfix .

# 2. Tag as hotfix
./scripts/tag-image.sh \
  --env prod \
  --version 1.2.1 \
  --extra-tags "hotfix-cve-2025-001" \
  --push \
  auth-service hotfix

# 3. Deploy immediately
./scripts/rollback-service.sh auth-service v1.2.1
```

### Scenario 3: Feature Testing
```bash
# 1. Build feature branch
docker build -t gcrf-library/book-service:feature .

# 2. Tag for testing
./scripts/tag-image.sh \
  --env dev \
  --extra-tags "feature-search" \
  book-service feature

# 3. Deploy to test environment
docker-compose up -d book-service
```

## 🔄 Rollback Procedures

### Immediate Rollback (Critical)
```bash
# Rollback single service
./scripts/rollback-service.sh --force gateway-service

# Rollback all services
./scripts/rollback-service.sh --all --force
```

### Kubernetes Rollback
```bash
# Method 1: kubectl rollout
kubectl rollout undo deployment/gateway-service

# Method 2: Using our script
./scripts/rollback-service.sh -m k8s gateway-service
```

### Docker Compose Rollback
```bash
# Using our script
./scripts/rollback-service.sh -m compose gateway-service v1.1.0

# Manual method
export GATEWAY_VERSION=v1.1.0
docker-compose up -d gateway-service
```

## 📊 Image Management

### List All Tags for a Service
```bash
docker images gcrf-library/gateway-service \
  --format "table {{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
```

### Check Image Details
```bash
docker inspect gcrf-library/gateway-service:v1.2.0
```

### Pull Specific Version
```bash
docker pull gcrf-library/gateway-service:v1.2.0
```

### Tag Cleanup Schedule
- **Production**: Never deleted
- **Staging**: 90 days retention
- **Development**: 30 days retention
- **Feature branches**: 14 days retention
- **PR builds**: 14 days retention
- **Nightly**: 7 days retention

## 🚨 Emergency Procedures

### Service Down
1. Check current version: `docker ps | grep gateway`
2. Rollback immediately: `./scripts/rollback-service.sh --force gateway-service`
3. Verify health: `curl http://gateway:8080/health`

### Mass Rollback
```bash
# Rollback all services to stable versions
./scripts/rollback-service.sh --all --force
```

### Registry Issues
```bash
# Use local images if registry is down
docker images gcrf-library/* --format "{{.Repository}}:{{.Tag}}"

# Deploy from local cache
docker-compose up -d --no-pull
```

## 📈 CI/CD Integration

### GitHub Actions
```yaml
- name: Tag and Push
  run: |
    ./scripts/tag-image.sh \
      --env ${{ github.ref == 'refs/heads/main' && 'prod' || 'dev' }} \
      --version ${{ github.ref_name }} \
      --push \
      ${{ matrix.service }} build
```

### GitLab CI
```yaml
tag-images:
  script:
    - ./scripts/tag-image.sh --env $CI_COMMIT_BRANCH --push gateway-service build
```

### Jenkins
```groovy
sh './scripts/tag-image.sh --env ${BRANCH_NAME} --push gateway-service build'
```

## 🔍 Troubleshooting

### Tag Already Exists
```bash
# Force overwrite (use carefully!)
./scripts/tag-image.sh --force gateway-service build
```

### Cannot Pull Image
```bash
# Check registry connectivity
docker pull gcrf-library/gateway-service:latest

# List local images
docker images gcrf-library/*
```

### Rollback Failed
```bash
# Check available versions
docker images gcrf-library/gateway-service

# Try manual rollback
docker tag gcrf-library/gateway-service:v1.1.0 \
  gcrf-library/gateway-service:prod-stable
docker-compose up -d gateway-service
```

## 📞 Support Contacts

- **DevOps Team**: devops@gcrf.local
- **On-Call**: +86-xxx-xxxx-xxxx
- **Slack**: #platform-emergency

---

**Remember**: When in doubt, rollback first, investigate later!