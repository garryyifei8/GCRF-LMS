#!/bin/bash
# ============================================
# MinIO Bucket Initialization Script
# ============================================

set -e

echo "=== Initializing MinIO Buckets ==="

# MinIO credentials
MINIO_HOST="http://localhost:9000"
MINIO_USER="${MINIO_ROOT_USER:-minioadmin}"
MINIO_PASSWORD="${MINIO_ROOT_PASSWORD:-minioadmin2024}"
MC_ALIAS="library-minio"

# Wait for MinIO to be ready
echo "Waiting for MinIO to be ready..."
until curl -f $MINIO_HOST/minio/health/live > /dev/null 2>&1; do
    echo "Waiting for MinIO..."
    sleep 5
done

echo "MinIO is ready!"

# Install mc (MinIO Client) if not exists
if ! command -v mc &> /dev/null; then
    echo "Installing MinIO Client..."
    curl -o /tmp/mc https://dl.min.io/client/mc/release/darwin-amd64/mc
    chmod +x /tmp/mc
    MC_CMD="/tmp/mc"
else
    MC_CMD="mc"
fi

# Configure mc alias
echo "Configuring MinIO client..."
$MC_CMD alias set $MC_ALIAS $MINIO_HOST $MINIO_USER $MINIO_PASSWORD

# Create buckets
echo "Creating buckets..."
$MC_CMD mb --ignore-existing ${MC_ALIAS}/avatars
$MC_CMD mb --ignore-existing ${MC_ALIAS}/covers
$MC_CMD mb --ignore-existing ${MC_ALIAS}/documents

# Set bucket policies (public-read)
echo "Setting bucket policies..."

# Avatar bucket - public read
$MC_CMD anonymous set download ${MC_ALIAS}/avatars

# Book cover bucket - public read
$MC_CMD anonymous set download ${MC_ALIAS}/covers

# Documents bucket - private (authenticated access only)
$MC_CMD anonymous set none ${MC_ALIAS}/documents

# List buckets
echo ""
echo "=== Created Buckets ==="
$MC_CMD ls $MC_ALIAS

echo ""
echo "=== Bucket Policies ==="
echo "avatars: public-read (anonymous download allowed)"
echo "covers: public-read (anonymous download allowed)"
echo "documents: private (authentication required)"

echo ""
echo "=== MinIO initialization completed ==="
echo "Console URL: http://localhost:9001"
echo "API URL: http://localhost:9000"
echo "Username: $MINIO_USER"
echo "Password: $MINIO_PASSWORD"
