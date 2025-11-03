# GCRF Library Management System - Security Scanning Guide

**Version:** 1.0.0
**Last Updated:** 2025-11-01
**Owner:** GCRF Security Team

---

## Table of Contents

1. [Overview](#overview)
2. [Security Scanning Strategy](#security-scanning-strategy)
3. [Trivy Installation](#trivy-installation)
4. [Running Security Scans](#running-security-scans)
5. [Understanding Severity Levels](#understanding-severity-levels)
6. [Security Baseline](#security-baseline)
7. [CI/CD Integration](#cicd-integration)
8. [Remediation Workflows](#remediation-workflows)
9. [Handling False Positives](#handling-false-positives)
10. [SBOM Management](#sbom-management)
11. [Best Practices](#best-practices)
12. [Troubleshooting](#troubleshooting)

---

## Overview

### Purpose

This document provides comprehensive guidance on security vulnerability scanning for the GCRF Library Management System. It covers vulnerability detection, remediation processes, and integration into the development lifecycle.

### Scope

Security scanning covers:
- **Docker Images**: All microservice containers and frontend images
- **Dependencies**: Java/Maven, Node.js/npm packages
- **Base Images**: Operating system packages
- **Configuration**: Docker and Kubernetes misconfigurations
- **Secrets**: Embedded credentials and API keys
- **Licenses**: Open source license compliance

### Tools

**Primary Scanner:** [Trivy](https://aquasecurity.github.io/trivy/) by Aqua Security
- Open source, comprehensive scanner
- Fast and accurate vulnerability detection
- Multiple output formats (JSON, SARIF, HTML, SBOM)
- Excellent CI/CD integration
- Active community and regular updates

---

## Security Scanning Strategy

### Shift-Left Security

Security scanning is integrated early in the development lifecycle:

```
┌─────────────────────────────────────────────────────────────┐
│                   Development Lifecycle                      │
├─────────────┬──────────────┬──────────────┬─────────────────┤
│  Dev Local  │  Pull Request│   Main/Dev   │   Production    │
├─────────────┼──────────────┼──────────────┼─────────────────┤
│ ✓ Pre-commit│ ✓ Automated  │ ✓ Scheduled  │ ✓ Pre-deploy   │
│   scanning  │   scanning   │   scanning   │   gate          │
│             │              │   (daily)    │                 │
└─────────────┴──────────────┴──────────────┴─────────────────┘
```

### Multi-Layer Defense

1. **Developer Workstation**
   - Local scans before commit
   - IDE integration (optional)
   - Quick feedback loop

2. **Pull Request Gate**
   - Automated scans on PR creation
   - Block merges on critical issues
   - Visibility in code review

3. **Continuous Monitoring**
   - Daily scheduled scans
   - Detect newly disclosed vulnerabilities
   - Track remediation progress

4. **Production Gate**
   - Pre-deployment validation
   - Zero tolerance for critical/high issues
   - Deployment approval workflow

---

## Trivy Installation

### Quick Installation

```bash
# Run the installation script
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
./deployment/scripts/install-trivy.sh
```

### Manual Installation

#### macOS
```bash
# Using Homebrew
brew install aquasecurity/trivy/trivy

# Verify installation
trivy version
```

#### Linux (Debian/Ubuntu)
```bash
sudo apt-get install wget apt-transport-https gnupg lsb-release
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | sudo apt-key add -
echo "deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main" | \
  sudo tee -a /etc/apt/sources.list.d/trivy.list
sudo apt-get update
sudo apt-get install trivy
```

#### Linux (RHEL/CentOS)
```bash
cat << EOF | sudo tee /etc/yum.repos.d/trivy.repo
[trivy]
name=Trivy repository
baseurl=https://aquasecurity.github.io/trivy-repo/rpm/releases/\$releasever/\$basearch/
gpgcheck=0
enabled=1
EOF

sudo yum -y install trivy
```

### Database Initialization

```bash
# Download vulnerability databases
trivy image --download-db-only
trivy image --download-java-db-only

# Verify database
trivy version
```

---

## Running Security Scans

### Automated Scanning (Recommended)

Use the automated scanning script for comprehensive scans:

```bash
# Scan all GCRF images
./deployment/scripts/scan-images.sh --all

# Scan with database update
./deployment/scripts/scan-images.sh --update-db --all

# Scan specific image
./deployment/scripts/scan-images.sh gcrf-library/gateway-service:latest

# Scan multiple images
./deployment/scripts/scan-images.sh \
  gcrf-library/gateway-service:latest \
  gcrf-library/auth-service:latest \
  gcrf-library-web-admin:latest
```

### Manual Scanning

#### Basic Vulnerability Scan
```bash
trivy image gcrf-library/gateway-service:latest
```

#### Scan with Specific Severities
```bash
trivy image --severity CRITICAL,HIGH gcrf-library/gateway-service:latest
```

#### Generate JSON Report
```bash
trivy image \
  --format json \
  --output report.json \
  gcrf-library/gateway-service:latest
```

#### Generate HTML Report
```bash
trivy image \
  --format template \
  --template "@contrib/html.tpl" \
  --output report.html \
  gcrf-library/gateway-service:latest
```

#### Generate SARIF Report (for CI/CD)
```bash
trivy image \
  --format sarif \
  --output report.sarif \
  gcrf-library/gateway-service:latest
```

#### Scan with Exit Code on Findings
```bash
trivy image \
  --severity CRITICAL,HIGH \
  --exit-code 1 \
  gcrf-library/gateway-service:latest
```

### Scanning Options

| Option | Description | Example |
|--------|-------------|---------|
| `--severity` | Filter by severity | `--severity CRITICAL,HIGH` |
| `--exit-code` | Exit code on findings | `--exit-code 1` |
| `--format` | Output format | `--format json` |
| `--output` | Output file | `--output report.json` |
| `--ignore-unfixed` | Skip unfixed vulns | `--ignore-unfixed` |
| `--timeout` | Scan timeout | `--timeout 10m` |
| `--scanners` | Scanner types | `--scanners vuln,secret` |

---

## Understanding Severity Levels

### Severity Classifications

Trivy uses CVSS (Common Vulnerability Scoring System) to classify vulnerabilities:

#### CRITICAL (CVSS 9.0-10.0)
**🔴 Immediate Action Required**

- Remote code execution vulnerabilities
- Authentication bypasses
- Privilege escalation to root/admin
- Data exfiltration without authentication

**Example:**
```
CVE-2024-12345 - Remote Code Execution in log4j
Severity: CRITICAL
CVSS: 10.0
Description: Unauthenticated remote code execution via JNDI injection
Impact: Complete system compromise
```

**Response:** Immediate patching or deployment block

---

#### HIGH (CVSS 7.0-8.9)
**🟠 Urgent Remediation Required**

- SQL injection vulnerabilities
- Cross-site scripting (XSS) with data access
- Denial of service (DoS) attacks
- Authentication weaknesses

**Example:**
```
CVE-2024-67890 - SQL Injection in PostgreSQL JDBC driver
Severity: HIGH
CVSS: 8.5
Description: SQL injection via prepared statements
Impact: Database compromise, data theft
```

**Response:** Patch within 7 days or implement compensating controls

---

#### MEDIUM (CVSS 4.0-6.9)
**🟡 Planned Remediation**

- Information disclosure
- Cross-site request forgery (CSRF)
- Local privilege escalation
- Configuration weaknesses

**Example:**
```
CVE-2024-11111 - Information Disclosure
Severity: MEDIUM
CVSS: 5.3
Description: Exposes version information to unauthenticated users
Impact: Information leakage aids targeted attacks
```

**Response:** Patch within 30 days, monitor for exploitation

---

#### LOW (CVSS 0.1-3.9)
**🔵 Informational**

- Minor information leaks
- Client-side issues
- Theoretical vulnerabilities
- Defense-in-depth improvements

**Example:**
```
CVE-2024-22222 - Weak Cipher Supported
Severity: LOW
CVSS: 3.1
Description: Server accepts weak SSL/TLS cipher suites
Impact: Potential future cryptographic weakness
```

**Response:** Remediate during regular maintenance

---

#### UNKNOWN
**⚪ Needs Assessment**

- Newly disclosed, CVSS not yet assigned
- Insufficient information
- Pending vendor analysis

**Response:** Monitor for CVSS assignment, assess manually

---

## Security Baseline

### Thresholds

GCRF Library Management System enforces these thresholds:

| Severity | Threshold | Policy | Action |
|----------|-----------|--------|--------|
| **CRITICAL** | 0 | Zero tolerance | Build fails, deployment blocked |
| **HIGH** | 0 | Zero tolerance | Build fails, deployment blocked |
| **MEDIUM** | ≤ 5 | Acceptable with review | Warning, manual approval |
| **LOW** | Unlimited | Informational | Log only, track remediation |

### Rationale

**CRITICAL/HIGH = 0**: Production systems must not have easily exploitable vulnerabilities that could lead to data breaches, service disruption, or regulatory compliance violations.

**MEDIUM ≤ 5**: Allows some operational flexibility while maintaining strong security posture. MEDIUM vulnerabilities require specific conditions to exploit.

**LOW = Unlimited**: Low severity issues are tracked but don't block deployments. Remediate during regular maintenance windows.

### Exceptions Process

If CRITICAL/HIGH vulnerabilities cannot be immediately patched:

1. **Document** the vulnerability and impact
2. **Assess** exploitability in your environment
3. **Implement** compensating controls:
   - Network isolation
   - Access restrictions
   - WAF rules
   - Monitoring/detection
4. **Request** security exception with justification
5. **Approve** via security team review
6. **Track** in `.trivyignore` with expiry date
7. **Review** monthly and prioritize remediation

---

## CI/CD Integration

### GitHub Actions

The security scan workflow is defined in `.github/workflows/security-scan.yml`.

#### Triggers
- Pull requests to main/develop
- Pushes to main/develop
- Daily scheduled scans (2 AM UTC)
- Manual workflow dispatch

#### Workflow Steps
1. Build Docker images for all services
2. Run Trivy scans in parallel
3. Upload SARIF to GitHub Security Dashboard
4. Generate JSON and HTML reports
5. Check security baseline compliance
6. Generate SBOM for each service
7. Create consolidated report
8. Comment on PR with results

#### Usage

```yaml
# View scan results
GitHub → Security → Code scanning alerts

# Download reports
GitHub → Actions → Security Scan → Artifacts

# Re-run scan
GitHub → Actions → Security Vulnerability Scanning → Run workflow
```

---

### GitLab CI/CD

The security scan pipeline is defined in `.gitlab-ci-security.yml`.

#### Integration

Add to your main `.gitlab-ci.yml`:

```yaml
include:
  - local: .gitlab-ci-security.yml
```

#### Pipeline Stages
1. **build**: Build Docker images
2. **security-scan**: Run Trivy scans
3. **sbom-generation**: Generate SBOMs
4. **report**: Consolidated reporting
5. **notify**: Slack/email notifications

#### Usage

```bash
# View scan results
GitLab → Security & Compliance → Vulnerability Report

# Download reports
GitLab → CI/CD → Pipelines → Job Artifacts

# Manual trigger
GitLab → CI/CD → Pipelines → Run Pipeline
```

---

### Jenkins Integration

```groovy
pipeline {
    agent any

    environment {
        TRIVY_VERSION = '0.56.2'
        SCAN_SEVERITY = 'CRITICAL,HIGH,MEDIUM,LOW'
    }

    stages {
        stage('Build') {
            steps {
                script {
                    docker.build("gcrf-library/gateway-service:${env.BUILD_ID}")
                }
            }
        }

        stage('Security Scan') {
            steps {
                script {
                    sh """
                        trivy image \
                            --severity ${SCAN_SEVERITY} \
                            --format json \
                            --output trivy-report.json \
                            --exit-code 0 \
                            gcrf-library/gateway-service:${env.BUILD_ID}
                    """

                    // Parse results
                    def report = readJSON file: 'trivy-report.json'
                    def critical = countVulnerabilities(report, 'CRITICAL')
                    def high = countVulnerabilities(report, 'HIGH')

                    if (critical > 0 || high > 0) {
                        error("Security scan failed: ${critical} CRITICAL, ${high} HIGH vulnerabilities")
                    }
                }
            }
        }

        stage('Generate Reports') {
            steps {
                sh """
                    trivy image \
                        --format template \
                        --template "@contrib/html.tpl" \
                        --output trivy-report.html \
                        gcrf-library/gateway-service:${env.BUILD_ID}
                """
                publishHTML([
                    reportDir: '.',
                    reportFiles: 'trivy-report.html',
                    reportName: 'Trivy Security Report'
                ])
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'trivy-report.*', allowEmptyArchive: true
        }
    }
}

def countVulnerabilities(report, severity) {
    def count = 0
    report.Results?.each { result ->
        result.Vulnerabilities?.each { vuln ->
            if (vuln.Severity == severity) count++
        }
    }
    return count
}
```

---

## Remediation Workflows

### Step 1: Identify Vulnerabilities

```bash
# Run scan and generate reports
./deployment/scripts/scan-images.sh --all

# Review HTML report (most readable)
open deployment/security-reports/html/gateway-service_*.html

# Review JSON for automation
jq '.Results[].Vulnerabilities[] | select(.Severity=="CRITICAL" or .Severity=="HIGH")' \
  deployment/security-reports/json/gateway-service_*.json
```

### Step 2: Prioritize

**Priority Matrix:**

| Severity | Exploitability | Priority | SLA |
|----------|----------------|----------|-----|
| CRITICAL | Easy | P0 | 24 hours |
| CRITICAL | Moderate | P1 | 48 hours |
| HIGH | Easy | P1 | 7 days |
| HIGH | Moderate | P2 | 14 days |
| MEDIUM | Any | P3 | 30 days |
| LOW | Any | P4 | Best effort |

### Step 3: Assess Impact

Questions to ask:
- Is the vulnerable component used in our application?
- Is the vulnerable function/endpoint exposed?
- What data or systems could be compromised?
- Are there existing mitigating controls?

### Step 4: Remediate

#### Option A: Update Dependency
```bash
# Maven (Java services)
# Update version in pom.xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.2.5</version> <!-- Updated from 3.2.2 -->
</dependency>

# Rebuild and re-scan
mvn clean package
docker build -t gcrf-library/gateway-service:latest .
trivy image gcrf-library/gateway-service:latest
```

#### Option B: Update Base Image
```dockerfile
# Dockerfile - update base image
FROM eclipse-temurin:21-jre-alpine  # Use newer version
# ... rest of Dockerfile

# Rebuild
docker build -t gcrf-library/gateway-service:latest .
```

#### Option C: Replace Dependency
```xml
<!-- Replace vulnerable library with alternative -->
<dependency>
    <groupId>com.alternative</groupId>
    <artifactId>safer-library</artifactId>
    <version>2.0.0</version>
</dependency>
```

#### Option D: Compensating Controls
If patching is not possible:
- Network segmentation
- Web Application Firewall (WAF) rules
- Runtime application self-protection (RASP)
- Enhanced monitoring and alerting

### Step 5: Verify

```bash
# Re-run scan
./deployment/scripts/scan-images.sh gcrf-library/gateway-service:latest

# Verify vulnerability resolved
grep "CVE-2024-12345" deployment/security-reports/json/gateway-service_*.json
# Should return no results
```

### Step 6: Document

Update security changelog:
```markdown
## Security Updates - 2025-11-01

### Resolved Vulnerabilities
- **CVE-2024-12345**: Updated spring-boot from 3.2.2 to 3.2.5
  - Severity: CRITICAL
  - Impact: Remote code execution
  - Resolution: Dependency update
  - Verified: Scan shows no findings

### Accepted Risks
- **CVE-2024-67890**: PostgreSQL JDBC driver
  - Severity: HIGH
  - Reason: Requires authenticated database access
  - Controls: Database access restricted, monitored
  - Review Date: 2025-12-01
```

---

## Handling False Positives

### Identifying False Positives

Common scenarios:
1. Vulnerability in unused code path
2. Vulnerability requires specific configuration not used
3. Scanner incorrectly identifies package version
4. Vulnerability already patched in downstream package

### Adding to Ignore List

Edit `deployment/config/.trivyignore`:

```bash
# CVE-2024-12345 exp:2025-12-31
# Reason: False positive - vulnerability in admin panel we don't use
# Component: spring-boot-admin
# Assessment: Admin endpoints not exposed in production
# Approved by: Security Team, 2025-11-01
CVE-2024-12345

# CVE-2024-67890 exp:2026-01-31
# Reason: Requires physical access to exploit
# Component: usb-driver
# Assessment: Servers in secure datacenter, physical access monitored
# Controls: Physical access logs, 24/7 monitoring
# Approved by: Security Team, 2025-11-01
CVE-2024-67890
```

### Format

```
CVE-YYYY-XXXXX exp:YYYY-MM-DD
# Reason: [clear justification]
# Component: [affected component]
# Assessment: [why it's false positive or accepted risk]
# Controls: [compensating controls if applicable]
# Approved by: [approver], [date]
```

### Review Process

1. **Monthly Review**: Security team reviews all ignored CVEs
2. **Expiry Dates**: All ignores must have expiry dates
3. **Re-assessment**: On expiry, re-assess if ignore still valid
4. **Removal**: Remove ignores when patched or no longer applicable

### Verification

```bash
# Scan with ignore file
trivy image \
  --ignorefile deployment/config/.trivyignore \
  gcrf-library/gateway-service:latest

# Verify specific CVE is ignored
grep "CVE-2024-12345" deployment/security-reports/json/gateway-service_*.json
# Should not appear in results
```

---

## SBOM Management

### What is SBOM?

Software Bill of Materials (SBOM) is a comprehensive inventory of all software components, including:
- Open source libraries
- Commercial packages
- Operating system packages
- Versions and licenses
- Dependencies and relationships

### Why SBOM Matters

1. **Transparency**: Know exactly what's in your software
2. **Vulnerability Management**: Quickly identify affected components
3. **Compliance**: Meet regulatory requirements (e.g., EO 14028)
4. **Supply Chain Security**: Track component sources and integrity
5. **License Compliance**: Identify license obligations

### Generating SBOM

```bash
# SPDX JSON format (recommended)
trivy image \
  --format spdx-json \
  --output sbom-gateway.spdx.json \
  gcrf-library/gateway-service:latest

# CycloneDX format
trivy image \
  --format cyclonedx \
  --output sbom-gateway.cyclonedx.json \
  gcrf-library/gateway-service:latest
```

### SBOM Storage

```
deployment/
└── security-reports/
    └── sbom/
        ├── gateway-service_20251101_123456_sbom.json
        ├── auth-service_20251101_123456_sbom.json
        └── ...
```

### SBOM Analysis

```bash
# Count total components
jq '.packages | length' sbom-gateway.spdx.json

# List all components with versions
jq -r '.packages[] | "\(.name):\(.versionInfo)"' sbom-gateway.spdx.json

# Find components by license
jq -r '.packages[] | select(.licenseConcluded | contains("Apache")) | .name' \
  sbom-gateway.spdx.json

# Check for specific component
jq -r '.packages[] | select(.name | contains("log4j"))' sbom-gateway.spdx.json
```

### SBOM in CI/CD

SBOMs are automatically generated in CI/CD pipelines and:
- Archived as build artifacts
- Compared against previous versions
- Analyzed for new vulnerabilities
- Shared with customers (if required)

---

## Best Practices

### Development

1. **Scan Early, Scan Often**
   - Run scans before committing code
   - Integrate into pre-commit hooks
   - Scan on every pull request

2. **Keep Dependencies Updated**
   - Use Dependabot or Renovate
   - Review and test updates regularly
   - Monitor security advisories

3. **Use Approved Base Images**
   - Maintain golden image repository
   - Regularly update base images
   - Minimize image layers

4. **Minimize Attack Surface**
   - Use distroless or minimal base images
   - Remove unnecessary packages
   - Disable unused features

### Operations

1. **Continuous Monitoring**
   - Daily scheduled scans
   - Alert on new vulnerabilities
   - Track remediation metrics

2. **Incident Response**
   - Have playbooks for critical vulnerabilities
   - Define escalation paths
   - Practice incident drills

3. **Metrics and Reporting**
   - Track time-to-remediate
   - Monitor vulnerability trends
   - Report to leadership

### Security

1. **Defense in Depth**
   - Combine scanning with WAF, IDS/IPS
   - Implement runtime protection
   - Network segmentation

2. **Zero Trust**
   - Assume breach mentality
   - Verify all components
   - Least privilege access

3. **Compliance**
   - Maintain audit trails
   - Document security decisions
   - Regular compliance reviews

---

## Troubleshooting

### Issue: Trivy Database Update Fails

**Symptoms:**
```
FATAL failed to download vulnerability DB
```

**Solution:**
```bash
# Clear cache and retry
rm -rf ~/.cache/trivy
trivy image --download-db-only

# Check internet connectivity
curl -I https://ghcr.io

# Use offline mode with cached database
trivy image --skip-db-update gcrf-library/gateway-service:latest
```

---

### Issue: Scan Times Out

**Symptoms:**
```
context deadline exceeded
```

**Solution:**
```bash
# Increase timeout
trivy image --timeout 20m gcrf-library/gateway-service:latest

# Use faster scanner mode (less thorough)
trivy image --scanners vuln gcrf-library/gateway-service:latest

# Scan in offline mode (no DB updates)
trivy image --skip-db-update --skip-java-db-update \
  gcrf-library/gateway-service:latest
```

---

### Issue: False Positive Flood

**Symptoms:**
Hundreds of LOW severity issues in base OS packages

**Solution:**
```bash
# Focus on high/critical
trivy image --severity CRITICAL,HIGH gcrf-library/gateway-service:latest

# Ignore unfixed vulnerabilities
trivy image --ignore-unfixed gcrf-library/gateway-service:latest

# Use distroless base images (fewer OS packages)
FROM gcr.io/distroless/java21-debian12
```

---

### Issue: Image Not Found

**Symptoms:**
```
Error: failed to analyze image: unable to get image
```

**Solution:**
```bash
# Verify image exists
docker images | grep gcrf-library

# Check image name and tag
docker inspect gcrf-library/gateway-service:latest

# Pull image if remote
docker pull gcrf-library/gateway-service:latest

# Build image if missing
docker build -t gcrf-library/gateway-service:latest .
```

---

### Issue: JSON Report Empty

**Symptoms:**
```json
{"Results": null}
```

**Solution:**
```bash
# Ensure image has vulnerabilities to report
trivy image --format json alpine:latest  # Known to have vulns

# Check Trivy version
trivy version

# Update Trivy
brew upgrade trivy  # macOS
sudo apt update && sudo apt upgrade trivy  # Linux

# Clear cache and rescan
rm -rf ~/.cache/trivy
trivy image --download-db-only
trivy image --format json gcrf-library/gateway-service:latest
```

---

## Appendix

### A. Severity Mapping

| CVSS Score | Severity | Color | Priority |
|------------|----------|-------|----------|
| 9.0 - 10.0 | CRITICAL | 🔴 Red | P0 |
| 7.0 - 8.9 | HIGH | 🟠 Orange | P1 |
| 4.0 - 6.9 | MEDIUM | 🟡 Yellow | P2 |
| 0.1 - 3.9 | LOW | 🔵 Blue | P3 |
| N/A | UNKNOWN | ⚪ White | Assess |

### B. Common CVE Databases

- [NVD](https://nvd.nist.gov/) - National Vulnerability Database
- [CVE](https://cve.mitre.org/) - Common Vulnerabilities and Exposures
- [GitHub Advisory Database](https://github.com/advisories)
- [Snyk Vulnerability DB](https://security.snyk.io/)
- [OSV](https://osv.dev/) - Open Source Vulnerabilities

### C. Useful Commands

```bash
# Quick scan
trivy image gcrf-library/gateway-service:latest

# Detailed scan with all info
trivy image --scanners vuln,secret,misconfig,license \
  --format json gcrf-library/gateway-service:latest | jq .

# Check specific vulnerability
trivy image gcrf-library/gateway-service:latest | grep CVE-2024-12345

# List all installed packages
trivy image --format json gcrf-library/gateway-service:latest | \
  jq -r '.Results[].Packages[] | "\(.Name):\(.Version)"'

# Count vulnerabilities by severity
trivy image --format json gcrf-library/gateway-service:latest | \
  jq '.Results[].Vulnerabilities | group_by(.Severity) |
      map({severity: .[0].Severity, count: length})'
```

### D. Resources

- [Trivy Documentation](https://aquasecurity.github.io/trivy/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [CIS Docker Benchmark](https://www.cisecurity.org/benchmark/docker)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)
- [SLSA Framework](https://slsa.dev/)

---

## Change Log

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0.0 | 2025-11-01 | GCRF Security Team | Initial documentation |

---

**Document Owner:** GCRF Security Team
**Review Schedule:** Quarterly
**Next Review:** 2026-02-01