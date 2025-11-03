# GCRF Deployment Scripts

This directory contains deployment and configuration validation scripts for the GCRF Library Management System.

## Scripts

### validate-config.sh

**Purpose**: Validate environment variables and test service connections before deployment.

**Usage**:
```bash
# Validate development environment
./deployment/scripts/validate-config.sh dev

# Validate production environment
./deployment/scripts/validate-config.sh prod

# Show help
./deployment/scripts/validate-config.sh --help
```

**Exit Codes**:
- `0` - All checks passed
- `1` - Missing required environment variables
- `2` - Validation failure (invalid values)
- `3` - Connection test failure

**What it Checks**:

1. **Environment Variables**
   - Gateway Service: `NACOS_SERVER_ADDR`, `NACOS_USERNAME`, `NACOS_PASSWORD`, `JWT_SECRET`, `REDIS_HOST`, `REDIS_PASSWORD`, `CORS_ALLOWED_ORIGINS`
   - Auth Service: `DB_HOST`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PASSWORD`, `JWT_SECRET`

2. **JWT Secret Validation**
   - Minimum 64 characters (required for HS512 algorithm)
   - Warns if using development default in any environment
   - Fails if using development default in production

3. **Service Connections**
   - PostgreSQL database (using `psql` or `nc` fallback)
   - Redis cache (using `redis-cli` or `nc` fallback)
   - Nacos service registry (HTTP health check)

4. **Environment-Specific Checks**
   - **Production**: Validates CORS origins, SSL mode, no dev defaults
   - **Development**: Relaxed validation, accepts dev defaults

**Example Output**:

```
=== GCRF Configuration Validation (dev environment) ===

=== Checking Gateway Service Environment Variables ===
✓ NACOS_SERVER_ADDR: localhost:8848
✓ NACOS_USERNAME: nacos
✓ NACOS_PASSWORD: ********
✓ JWT_SECRET: DEV_****HERE
✓ REDIS_HOST: localhost
✓ REDIS_PASSWORD: gcrf****2024

=== Validating JWT Secret ===
✓ JWT_SECRET length: 74 characters (✓ meets HS512 requirement)
⚠ JWT_SECRET appears to be dev default - acceptable for dev environment

=== Testing Database Connection ===
ℹ Testing PostgreSQL connection to localhost:5432/gcrf_auth...
✓ PostgreSQL at localhost:5432 - Connected successfully

=== Testing Redis Connection ===
ℹ Testing Redis connection to localhost:6379...
✓ Redis at localhost:6379 - Connected successfully (PONG received)

=== Testing Nacos Connection ===
ℹ Testing Nacos connection to http://localhost:8848...
✓ Nacos at http://localhost:8848 - Health check passed

=== Summary ===
✓ All checks passed (15/15)
⚠ 2 warning(s)

Configuration is valid for dev environment.
```

**Configuration Files**:

The script automatically loads environment variables from:
- Development: `.env.dev`
- Production: `.env.prod`

Create these files in the project root directory:

```bash
# Example .env.dev
NACOS_SERVER_ADDR=localhost:8848
NACOS_USERNAME=nacos
NACOS_PASSWORD=nacos
JWT_SECRET=DEV_SECRET_ONLY_NOT_FOR_PRODUCTION_USE_MINIMUM_64_CHARACTERS_REQUIRED_HERE
DB_HOST=localhost
DB_USERNAME=postgres
DB_PASSWORD=your_password
REDIS_HOST=localhost
REDIS_PASSWORD=your_redis_password
CORS_ALLOWED_ORIGINS=http://localhost:*
```

```bash
# Example .env.prod
NACOS_SERVER_ADDR=nacos-server:8848
NACOS_USERNAME=prod_nacos_user
NACOS_PASSWORD=strong_password_here
JWT_SECRET=<generate 64+ character random string>
DB_HOST=postgres-server
DB_USERNAME=gcrf_user
DB_PASSWORD=strong_db_password
REDIS_HOST=redis-server
REDIS_PASSWORD=strong_redis_password
CORS_ALLOWED_ORIGINS=https://library.gcrf.com,https://admin.gcrf.com
```

**Generating Secure JWT Secret**:

```bash
# Generate a random 64-character secret
openssl rand -base64 48

# Generate a random 128-character secret (more secure)
openssl rand -base64 96
```

**Integration with CI/CD**:

```yaml
# GitHub Actions example
- name: Validate Production Configuration
  run: |
    ./deployment/scripts/validate-config.sh prod
    if [ $? -ne 0 ]; then
      echo "Configuration validation failed!"
      exit 1
    fi
```

```yaml
# GitLab CI example
validate_config:
  script:
    - chmod +x ./deployment/scripts/validate-config.sh
    - ./deployment/scripts/validate-config.sh prod
  only:
    - main
    - production
```

**Troubleshooting**:

| Issue | Solution |
|-------|----------|
| `psql: command not found` | Install PostgreSQL client or the script will use `nc` fallback |
| `redis-cli: command not found` | Install Redis client or the script will use `nc` fallback |
| `curl: command not found` | Install curl or the script will use `nc` fallback |
| Connection timeout | Check if services are running and accessible |
| JWT secret too short | Generate a 64+ character secret using `openssl rand -base64 48` |
| Production using dev default | Update `JWT_SECRET` in `.env.prod` with a strong random value |

**Pre-Deployment Checklist**:

Before deploying to production, ensure:
- [ ] All required environment variables are set in `.env.prod`
- [ ] JWT_SECRET is at least 64 characters and NOT the dev default
- [ ] Database credentials are correct and SSL is enabled
- [ ] Redis credentials are correct
- [ ] Nacos credentials are correct
- [ ] CORS origins are restricted to production domains only
- [ ] All connection tests pass
- [ ] No warnings about dev defaults

**Best Practices**:

1. **Run validation before every deployment**
   ```bash
   ./deployment/scripts/validate-config.sh prod && deploy.sh
   ```

2. **Add to pre-commit hooks** (for local development)
   ```bash
   #!/bin/sh
   ./deployment/scripts/validate-config.sh dev || echo "Warning: Config validation failed"
   ```

3. **Store secrets securely**
   - Use secret managers (AWS Secrets Manager, HashiCorp Vault, etc.)
   - Never commit `.env.prod` to version control
   - Use `.env.example` as template

4. **Rotate secrets regularly**
   - JWT secrets: Every 90 days
   - Database passwords: Every 90 days
   - Redis passwords: Every 90 days
   - Nacos passwords: Every 90 days

### Volume Management Scripts

**Purpose**: Comprehensive backup, restore, cleanup, and monitoring of Docker volumes.

**Scripts**:
- `backup-volumes.sh` - Create backups of all Docker volumes
- `restore-volumes.sh` - Restore volumes from backup
- `cleanup-volumes.sh` - Clean old backups with retention policy
- `volume-status.sh` - Monitor volumes and disk space

**Quick Start**:
```bash
# Create backup
./deployment/scripts/backup-volumes.sh --verify

# Check status
./deployment/scripts/volume-status.sh

# Clean old backups (dry-run)
./deployment/scripts/cleanup-volumes.sh --dry-run

# Restore from backup
./deployment/scripts/restore-volumes.sh --dir /data/backups/20251101_020000
```

**Documentation**: See `VOLUME_MANAGEMENT.md` for complete guide including:
- Backup strategy and retention policy
- Automation with cron/systemd
- Monitoring integration (Prometheus, Grafana, Slack)
- Troubleshooting and best practices

### Infrastructure Management Scripts

**Purpose**: Start, stop, and manage infrastructure services.

**Scripts**:
- `start-infrastructure.sh` - Start all infrastructure services
- `stop-infrastructure.sh` - Stop all infrastructure services
- `push-nacos-configs.sh` - Push configurations to Nacos
- `test-service-discovery.sh` - Test service discovery

## Future Scripts

Planned deployment scripts:
- `deploy.sh` - Automated deployment script
- `rollback.sh` - Rollback to previous version
- `health-check.sh` - Post-deployment health verification

## Support

For issues or questions:
- Check the main project CLAUDE.md for development guidelines
- Review backend/doc/architect.md for technical specifications
- Create an issue in the project repository
