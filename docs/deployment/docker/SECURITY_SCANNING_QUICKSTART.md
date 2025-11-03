# GCRF Security Scanning - Quick Start Guide

**⏱️ 5-Minute Setup** | **🚀 Production-Ready Security**

---

## For Developers: 3 Steps to Secure Containers

### Step 1: Install Trivy (One-time, 2 minutes)

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
./deployment/scripts/install-trivy.sh
```

**Output:** ✅ Trivy installed and configured

---

### Step 2: Scan Your Image (30 seconds)

```bash
# Scan web admin
./deployment/scripts/scan-images.sh gcrf-library-web-admin:latest

# Scan backend service
./deployment/scripts/scan-images.sh gcrf-library/gateway-service:latest

# Scan ALL images
./deployment/scripts/scan-images.sh --all
```

**Output:** Vulnerability report in multiple formats

---

### Step 3: Review Results (1 minute)

```bash
# View HTML report (human-friendly)
open deployment/security-reports/html/*.html

# Or check summary on terminal
cat deployment/security-reports/summary/*.txt
```

**Interpret Results:**
- 🔴 **CRITICAL/HIGH** = ❌ Fix immediately (blocks deployment)
- 🟡 **MEDIUM** = ⚠️ Fix within 30 days
- 🔵 **LOW** = ℹ️ Informational only

---

## For DevOps: Enable CI/CD (5 minutes)

### GitHub Actions

**Already configured!** Workflow activates automatically when you push `.github/workflows/security-scan.yml`

```bash
git add .github/workflows/security-scan.yml
git commit -m "Enable security scanning"
git push
```

**View Results:** GitHub → Security → Code scanning alerts

---

### GitLab CI

Add to your `.gitlab-ci.yml`:

```yaml
include:
  - local: .gitlab-ci-security.yml
```

```bash
git add .gitlab-ci.yml .gitlab-ci-security.yml
git commit -m "Enable security scanning"
git push
```

**View Results:** GitLab → Security & Compliance → Vulnerability Report

---

## Common Commands

```bash
# Quick scan (critical/high only)
trivy image --severity CRITICAL,HIGH IMAGE:TAG

# Generate JSON report
trivy image --format json -o report.json IMAGE:TAG

# Update vulnerability database
trivy image --download-db-only

# Ignore specific CVE (add to .trivyignore)
echo "CVE-2024-12345" >> deployment/config/.trivyignore

# Check if image passes baseline
./deployment/scripts/scan-images.sh IMAGE:TAG
# Exit code 0 = Pass, 1 = Critical found, 2 = High found
```

---

## What Gets Scanned?

✅ **Operating System packages** (Alpine, Debian, Ubuntu, RHEL, etc.)
✅ **Java/Maven dependencies** (Spring Boot, PostgreSQL driver, etc.)
✅ **Node.js/npm packages** (Vue, Element Plus, etc.)
✅ **Container misconfigurations** (Dockerfile issues)
✅ **Embedded secrets** (API keys, passwords)
✅ **License compliance** (GPL, MIT, Apache, etc.)

---

## Security Baseline (Zero-Tolerance)

| Severity | Max Allowed | Action |
|----------|-------------|--------|
| CRITICAL | **0** | ❌ Build fails |
| HIGH | **0** | ❌ Build fails |
| MEDIUM | **≤ 5** | ⚠️ Warning |
| LOW | Unlimited | ℹ️ Track only |

**No exceptions** for CRITICAL/HIGH in production!

---

## Fix Vulnerabilities Fast

### Alpine Base Image Issues

```dockerfile
# Before (has vulnerabilities)
FROM node:20-alpine3.19

# After (patched)
FROM node:20-alpine3.20
```

### Update System Packages

```dockerfile
FROM node:20-alpine3.20

# Add this to your Dockerfile
RUN apk update && \
    apk upgrade --no-cache
```

### Rebuild and Re-scan

```bash
docker build -t IMAGE:TAG .
./deployment/scripts/scan-images.sh IMAGE:TAG
```

**Expected:** CRITICAL/HIGH count drops to 0 ✅

---

## Troubleshooting

### Issue: "Trivy not found"
```bash
# Re-run installation
./deployment/scripts/install-trivy.sh

# Or install via Homebrew (macOS)
brew install aquasecurity/trivy/trivy
```

### Issue: "Database update failed"
```bash
# Clear cache and retry
rm -rf ~/.cache/trivy
trivy image --download-db-only
```

### Issue: "Scan timeout"
```bash
# Increase timeout
trivy image --timeout 20m IMAGE:TAG
```

---

## Get Help

📖 **Full Documentation:** `/deployment/docs/SECURITY_SCANNING.md`
🐛 **Report Issues:** GitHub Issues / GitLab Issues
📧 **Security Team:** security@gcrf.com
💬 **DevOps Slack:** #gcrf-security

---

## Next Steps

After scanning:

1. ✅ **Fix CRITICAL/HIGH** vulnerabilities first
2. ⚠️ **Review MEDIUM** vulnerabilities
3. 📊 **Track progress** via CI/CD dashboard
4. 🔄 **Re-scan weekly** or on every build
5. 📈 **Monitor trends** to improve security posture

---

**Remember:** Security is a continuous process, not a one-time task!

🔒 **Secure by default, secure by design, secure in production.**