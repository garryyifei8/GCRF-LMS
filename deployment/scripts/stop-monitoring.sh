#!/bin/bash
# GCRF Library Management System - Stop Monitoring Stack
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
echo -e "${BLUE}   GCRF Library - Stopping Monitoring Stack${NC}"
echo -e "${BLUE}================================================${NC}"
echo

cd "$DEPLOYMENT_DIR"
docker-compose -f docker-compose.monitoring.yml down

echo
echo -e "${GREEN}✓ Monitoring stack stopped${NC}"
echo
echo -e "${YELLOW}Note: Monitoring data is preserved in volumes.${NC}"
echo -e "${YELLOW}To remove data, run:${NC}"
echo -e "  docker volume rm gcrf-prometheus-data gcrf-grafana-data"
