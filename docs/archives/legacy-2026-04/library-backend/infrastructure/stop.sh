#!/bin/bash
# ============================================
# Library Management System - Infrastructure
# Stop Script
# ============================================

set -e

echo "=========================================="
echo "Stopping Infrastructure Services"
echo "=========================================="

# Stop services
echo ""
echo "Stopping all services..."
docker-compose down

echo ""
echo "=========================================="
echo "All services stopped successfully!"
echo "=========================================="
echo ""
echo "Data is preserved in:"
echo "  - mysql/master/data"
echo "  - mysql/slave/data"
echo "  - redis/data"
echo "  - elasticsearch/data"
echo "  - minio/data"
echo "  - rabbitmq/data"
echo ""
echo "To remove all data: docker-compose down -v"
echo "To start again: ./start.sh"
echo ""
