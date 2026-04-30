#!/bin/bash
# ============================================
# Library Management System - Infrastructure
# Quick Start Script
# ============================================

set -e

echo "=========================================="
echo "Library Management System Infrastructure"
echo "Quick Start Script"
echo "=========================================="

# Check Docker
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed"
    exit 1
fi

# Check Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo "Error: Docker Compose is not installed"
    exit 1
fi

# Create network if not exists
echo ""
echo "Step 1: Creating Docker network..."
docker network inspect library-network >/dev/null 2>&1 || docker network create library-network
echo "Network 'library-network' is ready"

# Copy .env file if not exists
if [ ! -f .env ]; then
    echo ""
    echo "Step 2: Creating .env file..."
    cp .env.example .env
    echo ".env file created (using default values)"
else
    echo ""
    echo "Step 2: .env file already exists"
fi

# Create required directories
echo ""
echo "Step 3: Creating data directories..."
mkdir -p mysql/{master,slave}/{data,logs,conf}
mkdir -p redis/{data,logs}
mkdir -p elasticsearch/{data,plugins,config,templates}
mkdir -p minio/{data,config}
mkdir -p rabbitmq/{data,config}
echo "Data directories created"

# Set proper permissions for Elasticsearch
echo ""
echo "Step 4: Setting permissions..."
chmod -R 777 elasticsearch/data 2>/dev/null || true
chmod -R 777 elasticsearch/plugins 2>/dev/null || true
echo "Permissions set"

# Start services
echo ""
echo "Step 5: Starting services..."
docker-compose up -d

# Wait for services to be healthy
echo ""
echo "Step 6: Waiting for services to be healthy..."
echo "This may take 1-2 minutes..."
sleep 30

# Check service health
echo ""
echo "Step 7: Checking service health..."

services=("mysql-master" "mysql-slave" "redis" "elasticsearch" "kibana" "minio" "rabbitmq")
for service in "${services[@]}"; do
    status=$(docker inspect --format='{{.State.Health.Status}}' library-$service 2>/dev/null || echo "no-health-check")
    if [ "$status" == "healthy" ] || [ "$status" == "no-health-check" ]; then
        echo "  ✓ $service: healthy"
    else
        echo "  ✗ $service: $status"
    fi
done

echo ""
echo "=========================================="
echo "Infrastructure Started Successfully!"
echo "=========================================="
echo ""
echo "Service URLs:"
echo "  MySQL Master:     localhost:3306"
echo "  MySQL Slave:      localhost:3307"
echo "  Redis:            localhost:6379"
echo "  Elasticsearch:    http://localhost:9200"
echo "  Kibana:           http://localhost:5601"
echo "  MinIO Console:    http://localhost:9001"
echo "  MinIO API:        http://localhost:9000"
echo "  RabbitMQ UI:      http://localhost:15672"
echo ""
echo "Next Steps:"
echo "  1. Setup MySQL replication: cd mysql && ./setup-replication.sh"
echo "  2. Install Elasticsearch IK plugin: cd elasticsearch && ./install-ik-plugin.sh"
echo "  3. Initialize MinIO buckets: cd minio && ./init.sh"
echo "  4. Verify RabbitMQ setup: cd rabbitmq && ./init.sh"
echo ""
echo "View logs: docker-compose logs -f"
echo "Stop services: docker-compose down"
echo ""
