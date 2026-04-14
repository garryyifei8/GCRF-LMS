# GCRF Library Management System - SSL/HTTPS Setup Guide

This document provides comprehensive instructions for configuring HTTPS/TLS for the GCRF Library Management System in both development and production environments.

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Development Setup (Self-Signed)](#development-setup-self-signed)
4. [Production Setup (Let's Encrypt)](#production-setup-lets-encrypt)
5. [Configuration Details](#configuration-details)
6. [Certificate Renewal](#certificate-renewal)
7. [Troubleshooting](#troubleshooting)
8. [Security Best Practices](#security-best-practices)

---

## Overview

### Architecture

```
                    INTERNET
                        |
                   [Port 443]
                        |
                  +----------+
                  |  Nginx   |  <-- SSL Termination
                  | (HTTPS)  |
                  +----------+
                        |
                  [Port 8080]
                        |
                  +----------+
                  |  Gateway |
                  | Service  |
                  +----------+
                        |
            +-----------+-----------+
            |           |           |
        +-------+   +-------+   +-------+
        | Auth  |   | Book  |   | Other |
        +-------+   +-------+   +-------+
```

### Security Features

- **TLS 1.2/1.3 Only**: Older insecure protocols disabled
- **Strong Cipher Suites**: Mozilla Modern configuration
- **HSTS**: HTTP Strict Transport Security with preload
- **OCSP Stapling**: Faster certificate validation
- **Security Headers**: X-Frame-Options, CSP, X-Content-Type-Options
- **Rate Limiting**: Protection against brute force attacks

---

## Quick Start

### Development (5 minutes)

```bash
# Navigate to deployment directory
cd deployment

# Generate self-signed certificates
./scripts/generate-ssl-cert.sh --dev

# Start services with SSL
docker-compose -f docker-compose.infrastructure.yml up -d
docker-compose -f docker-compose.services.yml -f docker-compose.ssl.yml up -d

# Verify
curl -k https://localhost/health
```

### Production (15 minutes)

```bash
# Ensure domain points to server
# Ensure port 80/443 are open

# Generate Let's Encrypt certificates
sudo ./scripts/generate-ssl-cert.sh --prod \
  --domain library.yourdomain.com \
  --email admin@yourdomain.com

# Start services
docker-compose -f docker-compose.infrastructure.yml up -d
docker-compose -f docker-compose.services.yml -f docker-compose.ssl.yml up -d

# Verify
curl https://library.yourdomain.com/health
```

---

## Development Setup (Self-Signed)

### Step 1: Generate Self-Signed Certificates

```bash
cd deployment
./scripts/generate-ssl-cert.sh --dev
```

This creates:
- `deployment/ssl/privkey.pem` - Private key
- `deployment/ssl/fullchain.pem` - Certificate
- `deployment/ssl/chain.pem` - Certificate chain
- `deployment/ssl/dhparam.pem` - DH parameters

### Step 2: Start Services

```bash
# Start infrastructure first
docker-compose -f docker-compose.infrastructure.yml up -d

# Wait for infrastructure to be healthy
./scripts/wait-for-healthy.sh

# Start application services with SSL
docker-compose -f docker-compose.services.yml -f docker-compose.ssl.yml up -d
```

### Step 3: Trust Certificate (Optional)

#### macOS
```bash
# Add to system keychain
sudo security add-trusted-cert -d -r trustRoot \
  -k /Library/Keychains/System.keychain \
  deployment/ssl/fullchain.pem
```

#### Linux (Ubuntu/Debian)
```bash
sudo cp deployment/ssl/fullchain.pem /usr/local/share/ca-certificates/gcrf-dev.crt
sudo update-ca-certificates
```

#### Windows
```powershell
# Import to Trusted Root Certification Authorities
Import-Certificate -FilePath deployment\ssl\fullchain.pem `
  -CertStoreLocation Cert:\LocalMachine\Root
```

### Step 4: Access Application

- **Web Admin**: https://localhost
- **API Gateway**: https://localhost/api/

Note: Browser will show security warning for self-signed certificates. Click "Advanced" and proceed.

---

## Production Setup (Let's Encrypt)

### Prerequisites

1. **Domain configured**: DNS A record pointing to your server
2. **Ports open**: 80 (HTTP) and 443 (HTTPS) accessible from internet
3. **Server access**: Root or sudo access
4. **certbot installed**: See installation below

### Step 1: Install Certbot

#### Ubuntu/Debian
```bash
sudo apt-get update
sudo apt-get install -y certbot
```

#### CentOS/RHEL
```bash
sudo yum install -y epel-release
sudo yum install -y certbot
```

#### Docker (Alternative)
```bash
# Use certbot Docker image (no installation needed)
docker run -it --rm \
  -v /etc/letsencrypt:/etc/letsencrypt \
  -v /var/www/certbot:/var/www/certbot \
  certbot/certbot certonly --help
```

### Step 2: Prepare Environment

```bash
# Create required directories
mkdir -p deployment/ssl
mkdir -p deployment/certbot/www
mkdir -p deployment/certbot/conf

# Ensure web root exists for ACME challenge
mkdir -p deployment/certbot/www/.well-known/acme-challenge
```

### Step 3: Initial Certificate Generation

#### Option A: Standalone Mode (Before Services Running)

```bash
sudo certbot certonly --standalone \
  -d library.yourdomain.com \
  --email admin@yourdomain.com \
  --agree-tos \
  --non-interactive
```

#### Option B: Webroot Mode (With Services Running)

First, start a minimal nginx to serve ACME challenges:

```bash
# Start temporary nginx for ACME challenge
docker run -d --name temp-nginx \
  -p 80:80 \
  -v $(pwd)/deployment/certbot/www:/var/www/certbot \
  nginx:alpine

# Generate certificate
sudo ./scripts/generate-ssl-cert.sh --prod \
  --domain library.yourdomain.com \
  --email admin@yourdomain.com

# Stop temporary nginx
docker stop temp-nginx && docker rm temp-nginx
```

### Step 4: Copy Certificates

```bash
# Copy from Let's Encrypt to deployment directory
sudo cp /etc/letsencrypt/live/library.yourdomain.com/fullchain.pem deployment/ssl/
sudo cp /etc/letsencrypt/live/library.yourdomain.com/privkey.pem deployment/ssl/
sudo cp /etc/letsencrypt/live/library.yourdomain.com/chain.pem deployment/ssl/

# Generate DH parameters
./scripts/generate-ssl-cert.sh --dhparam

# Set permissions
sudo chown -R $(whoami):$(whoami) deployment/ssl
chmod 600 deployment/ssl/privkey.pem
chmod 644 deployment/ssl/fullchain.pem deployment/ssl/chain.pem
```

### Step 5: Start Services

```bash
# Start all services with SSL
docker-compose -f docker-compose.infrastructure.yml up -d
docker-compose -f docker-compose.services.yml -f docker-compose.ssl.yml up -d
```

### Step 6: Verify SSL Configuration

```bash
# Check SSL grade
curl -I https://library.yourdomain.com

# Detailed SSL test
openssl s_client -connect library.yourdomain.com:443 -servername library.yourdomain.com

# Or use online tools:
# https://www.ssllabs.com/ssltest/
# https://www.immuniweb.com/ssl/
```

---

## Configuration Details

### Nginx SSL Configuration

Located at: `deployment/nginx/nginx-ssl.conf`

Key settings:

```nginx
# TLS versions (1.2 and 1.3 only)
ssl_protocols TLSv1.2 TLSv1.3;

# Modern cipher suites
ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:...;

# Session caching for performance
ssl_session_cache shared:SSL:50m;
ssl_session_timeout 1d;

# OCSP Stapling
ssl_stapling on;
ssl_stapling_verify on;

# HSTS (1 year)
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
```

### Rate Limiting

```nginx
# API rate limit: 10 requests/second with burst of 20
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;

# Login rate limit: 5 requests/minute
limit_req_zone $binary_remote_addr zone=login_limit:10m rate=5r/m;
```

### Security Headers

| Header | Value | Purpose |
|--------|-------|---------|
| Strict-Transport-Security | max-age=31536000 | Force HTTPS for 1 year |
| X-Frame-Options | SAMEORIGIN | Prevent clickjacking |
| X-Content-Type-Options | nosniff | Prevent MIME sniffing |
| X-XSS-Protection | 1; mode=block | XSS protection |
| Content-Security-Policy | ... | Control resource loading |
| Permissions-Policy | ... | Restrict browser features |

---

## Certificate Renewal

### Automatic Renewal (Recommended)

#### Using Certbot Timer

```bash
# Check certbot timer status
sudo systemctl status certbot.timer

# Enable if not running
sudo systemctl enable --now certbot.timer
```

#### Using Cron

```bash
# Edit crontab
sudo crontab -e

# Add renewal job (runs weekly at midnight Sunday)
0 0 * * 0 /path/to/deployment/scripts/generate-ssl-cert.sh --renew >> /var/log/ssl-renewal.log 2>&1
```

#### Using Docker Compose

Enable the certbot profile:

```bash
docker-compose -f docker-compose.ssl.yml --profile ssl-renewal up -d gcrf-certbot
```

### Manual Renewal

```bash
# Check certificate expiry
./scripts/generate-ssl-cert.sh --verify

# Renew certificate
sudo ./scripts/generate-ssl-cert.sh --renew

# Reload nginx
docker exec gcrf-nginx nginx -s reload
```

### Renewal Workflow

```
1. Certbot checks certificate expiry
2. If < 30 days remaining, initiates renewal
3. ACME challenge served via /.well-known/acme-challenge/
4. New certificate downloaded
5. Nginx reloaded with new certificate
```

---

## Troubleshooting

### Common Issues

#### 1. Certificate Not Found

**Symptom**: Nginx fails to start with "certificate not found"

**Solution**:
```bash
# Check certificate files exist
ls -la deployment/ssl/

# If missing, regenerate
./scripts/generate-ssl-cert.sh --dev
```

#### 2. Let's Encrypt Rate Limit

**Symptom**: "Too many requests" error from Let's Encrypt

**Solution**:
```bash
# Use staging environment for testing
./scripts/generate-ssl-cert.sh --prod \
  --domain test.yourdomain.com \
  --email admin@yourdomain.com \
  --staging
```

Rate limits:
- 50 certificates per registered domain per week
- 5 duplicate certificates per week
- 300 pending authorizations per account

#### 3. ACME Challenge Failed

**Symptom**: "Unauthorized" error during certificate generation

**Solution**:
```bash
# Check DNS propagation
dig library.yourdomain.com

# Check port 80 is accessible
curl http://library.yourdomain.com/.well-known/acme-challenge/test

# Check firewall
sudo ufw status
sudo iptables -L -n
```

#### 4. SSL Handshake Failure

**Symptom**: "SSL handshake failed" in browser

**Solution**:
```bash
# Check nginx configuration
docker exec gcrf-nginx nginx -t

# Check certificate validity
openssl x509 -in deployment/ssl/fullchain.pem -noout -dates

# Check certificate chain
openssl verify -CAfile deployment/ssl/chain.pem deployment/ssl/fullchain.pem
```

#### 5. HSTS Issues

**Symptom**: Cannot access site after enabling HSTS

**Solution**:
- Clear browser HSTS cache
- Chrome: `chrome://net-internals/#hsts` > Delete domain
- Firefox: History > Clear Recent History > Active Logins

#### 6. Mixed Content Warnings

**Symptom**: Browser shows mixed content warnings

**Solution**:
- Ensure all resources use HTTPS
- Update API endpoints to use HTTPS
- Check for hardcoded HTTP URLs in frontend

### Diagnostic Commands

```bash
# Check nginx logs
docker logs gcrf-nginx

# Test nginx configuration
docker exec gcrf-nginx nginx -t

# Check certificate details
openssl x509 -in deployment/ssl/fullchain.pem -noout -text

# Test SSL connection
openssl s_client -connect localhost:443 -servername localhost

# Check certificate expiry
openssl x509 -in deployment/ssl/fullchain.pem -noout -enddate

# Verify certificate chain
openssl verify -verbose -CAfile deployment/ssl/chain.pem deployment/ssl/fullchain.pem
```

---

## Security Best Practices

### Certificate Management

1. **Keep Private Keys Secure**
   - Never commit private keys to version control
   - Use restricted file permissions (600)
   - Rotate keys periodically

2. **Monitor Expiry**
   - Set up alerts before certificate expiry
   - Test renewal process regularly

3. **Use Strong Key Sizes**
   - RSA: minimum 2048 bits (4096 recommended)
   - ECDSA: P-256 or P-384

### Configuration Hardening

1. **Disable Weak Protocols**
   ```nginx
   ssl_protocols TLSv1.2 TLSv1.3;  # No SSLv3, TLSv1.0, TLSv1.1
   ```

2. **Use Forward Secrecy**
   ```nginx
   ssl_prefer_server_ciphers off;  # Let client choose from our list
   ssl_ecdh_curve secp384r1;
   ```

3. **Enable HSTS Preloading**
   - Submit domain to https://hstspreload.org/
   - Requires: HTTPS on all subdomains, `preload` directive

4. **Regular Security Audits**
   - Test with SSL Labs: https://www.ssllabs.com/ssltest/
   - Check for vulnerabilities: https://observatory.mozilla.org/

### Operational Security

1. **Backup Certificates**
   ```bash
   # Backup Let's Encrypt configuration
   sudo tar -czvf letsencrypt-backup.tar.gz /etc/letsencrypt
   ```

2. **Monitor Certificate Transparency**
   - Use CT logs to detect unauthorized certificates
   - https://crt.sh/?q=yourdomain.com

3. **Incident Response**
   - Have a plan for certificate revocation
   - Know how to quickly generate new certificates

---

## Files Reference

| File | Purpose |
|------|---------|
| `deployment/nginx/nginx-ssl.conf` | Nginx HTTPS configuration |
| `deployment/scripts/generate-ssl-cert.sh` | Certificate generation script |
| `deployment/docker-compose.ssl.yml` | Docker Compose SSL overlay |
| `deployment/ssl/` | Certificate storage directory |
| `deployment/certbot/` | Let's Encrypt configuration |

---

## Support

For issues or questions:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review nginx logs: `docker logs gcrf-nginx`
3. Check certificate status: `./scripts/generate-ssl-cert.sh --verify`

---

Last Updated: 2025-12-01
Version: 1.0.0
