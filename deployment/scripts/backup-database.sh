#!/bin/bash

################################################################################
# GCRF Library Management System - Database Backup Script
#
# This script performs automated backups of PostgreSQL databases with
# compression, retention policy, and optional remote storage upload.
#
# Usage: ./backup-database.sh [options]
#
# Options:
#   --full             Full backup (all databases, default)
#   --database <name>  Backup specific database only
#   --retention <days> Number of days to retain backups (default: 30)
#   --upload           Upload backup to remote storage (S3/MinIO)
#   --compress         Compress backup (default: gzip)
#   --no-compress      Do not compress backup
#   --verify           Verify backup integrity after creation
#
# Exit Codes:
#   0 - Success
#   1 - General error
#   2 - Backup failed
#   3 - Verification failed
################################################################################

set -e
set -u
set -o pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
BACKUP_DIR="/backups/postgresql"
BACKUP_ARCHIVE_DIR="/backups/postgresql/archive"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="/var/log/gcrf-backup-${TIMESTAMP}.log"

# Default options
BACKUP_TYPE="full"
SPECIFIC_DATABASE=""
RETENTION_DAYS=30
UPLOAD_TO_REMOTE=false
COMPRESS=true
VERIFY_BACKUP=false
REMOTE_BUCKET="s3://gcrf-backups/postgresql"

# Backup file information
BACKUP_FILE=""
BACKUP_SIZE=""

################################################################################
# Functions
################################################################################

log() {
    local level=$1
    shift
    local message="$@"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')

    case $level in
        INFO)
            echo -e "${BLUE}[$timestamp INFO]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        SUCCESS)
            echo -e "${GREEN}[$timestamp SUCCESS]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        WARNING)
            echo -e "${YELLOW}[$timestamp WARNING]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        ERROR)
            echo -e "${RED}[$timestamp ERROR]${NC} $message" | tee -a "$LOG_FILE"
            ;;
    esac
}

# Check prerequisites
check_prerequisites() {
    log INFO "Checking prerequisites..."

    # Check Docker is running
    if ! docker ps &> /dev/null; then
        log ERROR "Docker is not running"
        exit 1
    fi

    # Check PostgreSQL container is running
    if ! docker ps | grep -q postgres-primary; then
        log ERROR "PostgreSQL container is not running"
        exit 1
    fi

    # Check PostgreSQL is ready
    if ! docker exec postgres-primary pg_isready -U postgres &> /dev/null; then
        log ERROR "PostgreSQL is not ready"
        exit 1
    fi

    # Create backup directories
    mkdir -p "$BACKUP_DIR"
    mkdir -p "$BACKUP_ARCHIVE_DIR"

    # Check disk space (need at least 10GB free)
    local free_space=$(df -BG "$BACKUP_DIR" | awk 'NR==2 {print $4}' | sed 's/G//')
    if [ "$free_space" -lt 10 ]; then
        log WARNING "Low disk space: ${free_space}GB free. Recommended: at least 10GB"
    fi

    log SUCCESS "Prerequisites check passed"
}

# Perform backup
perform_backup() {
    log INFO "Starting database backup..."

    if [ "$BACKUP_TYPE" = "full" ]; then
        perform_full_backup
    else
        perform_database_backup "$SPECIFIC_DATABASE"
    fi
}

# Full backup (all databases)
perform_full_backup() {
    log INFO "Performing full backup of all databases..."

    BACKUP_FILE="$BACKUP_DIR/full_backup_${TIMESTAMP}.sql"

    # Perform pg_dumpall
    log INFO "Running pg_dumpall..."
    if docker exec postgres-primary pg_dumpall -U postgres > "$BACKUP_FILE" 2>> "$LOG_FILE"; then
        log SUCCESS "Database dump completed"
    else
        log ERROR "Database dump failed"
        exit 2
    fi

    # Get backup size
    BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    log INFO "Backup size: $BACKUP_SIZE"

    # Compress backup
    if [ "$COMPRESS" = true ]; then
        compress_backup
    fi

    # Create latest symlink
    if [ "$COMPRESS" = true ]; then
        ln -sf "$(basename ${BACKUP_FILE}.gz)" "$BACKUP_DIR/latest.sql.gz"
    else
        ln -sf "$(basename $BACKUP_FILE)" "$BACKUP_DIR/latest.sql"
    fi

    log SUCCESS "Full backup completed: $BACKUP_FILE"
}

# Single database backup
perform_database_backup() {
    local db_name=$1

    log INFO "Performing backup of database: $db_name..."

    BACKUP_FILE="$BACKUP_DIR/${db_name}_${TIMESTAMP}.sql"

    # Check if database exists
    local db_exists=$(docker exec postgres-primary psql -U postgres -lqt | cut -d \| -f 1 | grep -w "$db_name" || echo "")

    if [ -z "$db_exists" ]; then
        log ERROR "Database '$db_name' does not exist"
        exit 2
    fi

    # Perform pg_dump
    log INFO "Running pg_dump for $db_name..."
    if docker exec postgres-primary pg_dump -U postgres "$db_name" > "$BACKUP_FILE" 2>> "$LOG_FILE"; then
        log SUCCESS "Database dump completed"
    else
        log ERROR "Database dump failed"
        exit 2
    fi

    # Get backup size
    BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    log INFO "Backup size: $BACKUP_SIZE"

    # Compress backup
    if [ "$COMPRESS" = true ]; then
        compress_backup
    fi

    log SUCCESS "Database backup completed: $BACKUP_FILE"
}

# Compress backup
compress_backup() {
    log INFO "Compressing backup..."

    if gzip -f "$BACKUP_FILE"; then
        BACKUP_FILE="${BACKUP_FILE}.gz"
        local compressed_size=$(du -h "$BACKUP_FILE" | cut -f1)
        log SUCCESS "Backup compressed: $BACKUP_FILE (size: $compressed_size)"
    else
        log ERROR "Compression failed"
        exit 2
    fi
}

# Verify backup
verify_backup() {
    if [ "$VERIFY_BACKUP" = false ]; then
        return
    fi

    log INFO "Verifying backup integrity..."

    # Check file exists and is readable
    if [ ! -r "$BACKUP_FILE" ]; then
        log ERROR "Backup file is not readable"
        exit 3
    fi

    # Check file size
    local file_size=$(stat -f%z "$BACKUP_FILE" 2>/dev/null || stat -c%s "$BACKUP_FILE")
    if [ "$file_size" -lt 1000 ]; then
        log ERROR "Backup file is too small (${file_size} bytes), possibly corrupt"
        exit 3
    fi

    # Verify gzip integrity if compressed
    if [[ "$BACKUP_FILE" == *.gz ]]; then
        if gzip -t "$BACKUP_FILE" 2>> "$LOG_FILE"; then
            log SUCCESS "Backup file integrity verified"
        else
            log ERROR "Backup file is corrupt"
            exit 3
        fi
    fi

    # Optional: Test restore (commented out by default due to resource usage)
    # test_restore

    log SUCCESS "Backup verification passed"
}

# Test restore (optional, resource-intensive)
test_restore() {
    log INFO "Testing backup restore..."

    local test_db="gcrf_test_restore_$$"

    # Create test database
    docker exec postgres-primary psql -U postgres -c "CREATE DATABASE $test_db;" 2>> "$LOG_FILE"

    # Restore backup to test database
    if [[ "$BACKUP_FILE" == *.gz ]]; then
        if gunzip -c "$BACKUP_FILE" | docker exec -i postgres-primary psql -U postgres -d "$test_db" &>> "$LOG_FILE"; then
            log SUCCESS "Test restore successful"
        else
            log WARNING "Test restore failed (this may be normal for full backups)"
        fi
    else
        if docker exec -i postgres-primary psql -U postgres -d "$test_db" < "$BACKUP_FILE" &>> "$LOG_FILE"; then
            log SUCCESS "Test restore successful"
        else
            log WARNING "Test restore failed"
        fi
    fi

    # Drop test database
    docker exec postgres-primary psql -U postgres -c "DROP DATABASE IF EXISTS $test_db;" 2>> "$LOG_FILE"
}

# Upload to remote storage
upload_to_remote() {
    if [ "$UPLOAD_TO_REMOTE" = false ]; then
        return
    fi

    log INFO "Uploading backup to remote storage..."

    # Check if AWS CLI or mc (MinIO client) is available
    if command -v aws &> /dev/null; then
        upload_to_s3
    elif command -v mc &> /dev/null; then
        upload_to_minio
    else
        log WARNING "Neither AWS CLI nor MinIO client found, skipping remote upload"
        return
    fi
}

# Upload to AWS S3
upload_to_s3() {
    log INFO "Uploading to S3: $REMOTE_BUCKET"

    if aws s3 cp "$BACKUP_FILE" "$REMOTE_BUCKET/$(basename $BACKUP_FILE)" 2>> "$LOG_FILE"; then
        log SUCCESS "Backup uploaded to S3"
    else
        log ERROR "S3 upload failed"
    fi
}

# Upload to MinIO
upload_to_minio() {
    log INFO "Uploading to MinIO..."

    local minio_alias="backup-storage"
    local bucket_name="gcrf-backups"

    # Configure MinIO alias if not already set
    if ! mc alias list | grep -q "$minio_alias"; then
        log INFO "Configuring MinIO alias..."
        mc alias set "$minio_alias" http://minio:9000 ${MINIO_ACCESS_KEY} ${MINIO_SECRET_KEY} 2>> "$LOG_FILE"
    fi

    # Upload to MinIO
    if mc cp "$BACKUP_FILE" "${minio_alias}/${bucket_name}/postgresql/$(basename $BACKUP_FILE)" 2>> "$LOG_FILE"; then
        log SUCCESS "Backup uploaded to MinIO"
    else
        log ERROR "MinIO upload failed"
    fi
}

# Clean up old backups
cleanup_old_backups() {
    log INFO "Cleaning up backups older than $RETENTION_DAYS days..."

    local deleted_count=0

    # Find and delete old backups
    while IFS= read -r -d '' file; do
        local filename=$(basename "$file")
        log INFO "Deleting old backup: $filename"
        rm -f "$file"
        deleted_count=$((deleted_count + 1))
    done < <(find "$BACKUP_DIR" -name "*.sql" -o -name "*.sql.gz" -mtime +$RETENTION_DAYS -print0 2>/dev/null)

    if [ $deleted_count -gt 0 ]; then
        log SUCCESS "Deleted $deleted_count old backup(s)"
    else
        log INFO "No old backups to delete"
    fi

    # Archive old backups to archive directory before deletion (optional)
    # find "$BACKUP_DIR" -name "*.sql.gz" -mtime +7 -mtime -$RETENTION_DAYS -exec mv {} "$BACKUP_ARCHIVE_DIR/" \; 2>/dev/null || true
}

# Generate backup report
generate_report() {
    log INFO "Generating backup report..."

    local total_backups=$(find "$BACKUP_DIR" -name "*.sql*" -type f | wc -l)
    local total_size=$(du -sh "$BACKUP_DIR" | cut -f1)
    local oldest_backup=$(find "$BACKUP_DIR" -name "*.sql*" -type f -printf '%T+ %p\n' 2>/dev/null | sort | head -1 | awk '{print $2}' | xargs basename)
    local newest_backup=$(basename "$BACKUP_FILE")

    cat >> "$LOG_FILE" <<EOF

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        Backup Report
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Timestamp:         $(date)
Backup Type:       $BACKUP_TYPE
Backup File:       $newest_backup
Backup Size:       $BACKUP_SIZE
Compressed:        $([ "$COMPRESS" = true ] && echo "Yes" || echo "No")
Verified:          $([ "$VERIFY_BACKUP" = true ] && echo "Yes" || echo "No")
Remote Upload:     $([ "$UPLOAD_TO_REMOTE" = true ] && echo "Yes" || echo "No")

Backup Statistics:
  Total Backups:   $total_backups
  Total Size:      $total_size
  Oldest Backup:   $oldest_backup
  Retention:       $RETENTION_DAYS days

Status:            SUCCESS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
EOF

    log SUCCESS "Backup completed successfully"
    cat "$LOG_FILE"
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --full)
                BACKUP_TYPE="full"
                shift
                ;;
            --database)
                BACKUP_TYPE="single"
                SPECIFIC_DATABASE="$2"
                shift 2
                ;;
            --retention)
                RETENTION_DAYS="$2"
                shift 2
                ;;
            --upload)
                UPLOAD_TO_REMOTE=true
                shift
                ;;
            --compress)
                COMPRESS=true
                shift
                ;;
            --no-compress)
                COMPRESS=false
                shift
                ;;
            --verify)
                VERIFY_BACKUP=true
                shift
                ;;
            --help)
                cat <<EOF
GCRF Library Management System - Database Backup Script

Usage: $0 [options]

Options:
  --full             Full backup (all databases, default)
  --database <name>  Backup specific database only
  --retention <days> Number of days to retain backups (default: 30)
  --upload           Upload backup to remote storage (S3/MinIO)
  --compress         Compress backup (default: gzip)
  --no-compress      Do not compress backup
  --verify           Verify backup integrity after creation
  --help             Show this help message

Examples:
  $0                                    # Full backup with defaults
  $0 --full --verify --upload           # Full backup with verification and upload
  $0 --database gcrf_book               # Backup single database
  $0 --retention 60                     # Keep backups for 60 days

Backup Location: $BACKUP_DIR
Log File: $LOG_FILE
EOF
                exit 0
                ;;
            *)
                log ERROR "Unknown option: $1"
                log INFO "Use --help for usage information"
                exit 1
                ;;
        esac
    done
}

################################################################################
# Main Execution
################################################################################

main() {
    # Create log file
    touch "$LOG_FILE"

    log "" "
╔═══════════════════════════════════════════════════════════════════╗
║       GCRF Library Management System - Database Backup            ║
╚═══════════════════════════════════════════════════════════════════╝
"

    # Parse arguments
    parse_args "$@"

    # Execute backup workflow
    check_prerequisites
    perform_backup
    verify_backup
    upload_to_remote
    cleanup_old_backups
    generate_report

    log SUCCESS "All backup operations completed successfully!"
    exit 0
}

# Trap errors and log them
trap 'log ERROR "Backup failed with exit code $?"; exit 1' ERR

# Run main function with all arguments
main "$@"
