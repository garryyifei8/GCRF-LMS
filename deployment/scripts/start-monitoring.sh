#!/bin/bash
# GCRF Library Management System - Start Monitoring Stack
# Version: 1.0.0
# Last Updated: 2025-11-01

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DEPLOYMENT_DIR="$PROJECT_ROOT/deployment"

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}   GCRF Library - Starting Monitoring Stack${NC}"
echo -e "${BLUE}================================================${NC}"
echo

# Check if backend network exists
echo -e "${YELLOW}→ Checking backend network...${NC}"
if ! docker network inspect gcrf-backend-network >/dev/null 2>&1; then
    echo -e "${RED}✗ Backend network 'gcrf-backend-network' not found${NC}"
    echo -e "${YELLOW}  Please start the infrastructure first:${NC}"
    echo -e "${YELLOW}  cd $DEPLOYMENT_DIR && docker-compose -f docker-compose.infrastructure.yml up -d${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Backend network found${NC}"
echo

# Start monitoring stack
echo -e "${YELLOW}→ Starting monitoring services...${NC}"
cd "$DEPLOYMENT_DIR"
docker-compose -f docker-compose.monitoring.yml up -d

echo
echo -e "${YELLOW}→ Waiting for services to start...${NC}"
sleep 10

# Check service status
echo
echo -e "${YELLOW}→ Checking service health...${NC}"

services=("prometheus" "grafana" "node-exporter" "postgres-exporter" "redis-exporter")
all_healthy=true

for service in "${services[@]}"; do
    container_name="gcrf-${service}"
    if docker ps --format '{{.Names}}' | grep -q "^${container_name}$"; then
        echo -e "${GREEN}✓ ${service} is running${NC}"
    else
        echo -e "${RED}✗ ${service} is not running${NC}"
        all_healthy=false
    fi
done

echo
if [ "$all_healthy" = true ]; then
    echo -e "${GREEN}================================================${NC}"
    echo -e "${GREEN}   Monitoring Stack Started Successfully!${NC}"
    echo -e "${GREEN}================================================${NC}"
    echo
    echo -e "${BLUE}Access Points:${NC}"
    echo -e "  • Prometheus:  ${GREEN}http://localhost:9090${NC}"
    echo -e "  • Grafana:     ${GREEN}http://localhost:3000${NC}  (admin/admin)"
    echo -e "  • Node Exporter: ${GREEN}http://localhost:9100/metrics${NC}"
    echo
    echo -e "${YELLOW}Next Steps:${NC}"
    echo -e "  1. Open Grafana: http://localhost:3000"
    echo -e "  2. Login with admin/admin"
    echo -e "  3. Import recommended dashboards:"
    echo -e "     - Spring Boot 2.1 System Monitor (ID: 11378)"
    echo -e "     - PostgreSQL Database (ID: 9628)"
    echo -e "     - Redis Dashboard (ID: 11835)"
    echo -e "     - Node Exporter Full (ID: 1860)"
    echo
else
    echo -e "${RED}================================================${NC}"
    echo -e "${RED}   Some services failed to start${NC}"
    echo -e "${RED}================================================${NC}"
    echo
    echo -e "${YELLOW}Check logs with:${NC}"
    echo -e "  docker-compose -f docker-compose.monitoring.yml logs"
    exit 1
fi
