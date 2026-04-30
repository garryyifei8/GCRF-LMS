#!/bin/bash
# ============================================
# Library Management System - Infrastructure
# Verification Script
# ============================================

set -e

echo "=========================================="
echo "Infrastructure Verification"
echo "=========================================="

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_service() {
    local service=$1
    local command=$2

    echo -n "  Checking $service... "
    if eval "$command" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC}"
        return 0
    else
        echo -e "${RED}✗${NC}"
        return 1
    fi
}

echo ""
echo "1. Docker Containers Status"
echo "----------------------------"
docker-compose ps

echo ""
echo "2. Service Health Checks"
echo "------------------------"

# MySQL Master
check_service "MySQL Master" \
    "docker exec library-mysql-master mysqladmin -uroot -plibrary_root_2024 ping"

# MySQL Slave
check_service "MySQL Slave" \
    "docker exec library-mysql-slave mysqladmin -uroot -plibrary_root_2024 ping"

# Redis
check_service "Redis" \
    "docker exec library-redis redis-cli -a library_redis_2024 ping"

# Elasticsearch
check_service "Elasticsearch" \
    "curl -f http://localhost:9200/_cluster/health"

# Kibana
check_service "Kibana" \
    "curl -f http://localhost:5601/api/status"

# MinIO
check_service "MinIO" \
    "curl -f http://localhost:9000/minio/health/live"

# RabbitMQ
check_service "RabbitMQ" \
    "docker exec library-rabbitmq rabbitmq-diagnostics ping"

echo ""
echo "3. MySQL Databases"
echo "------------------"
docker exec library-mysql-master mysql -uroot -plibrary_root_2024 -e "SHOW DATABASES;" | grep library_ || echo "Databases not yet created"

echo ""
echo "4. MySQL Replication Status"
echo "---------------------------"
repl_status=$(docker exec library-mysql-slave mysql -uroot -plibrary_root_2024 -e "SHOW SLAVE STATUS\G" 2>/dev/null | grep "Slave_IO_Running" || echo "Not configured")
if [[ $repl_status == *"Yes"* ]]; then
    echo -e "  ${GREEN}✓${NC} Replication is running"
else
    echo -e "  ${YELLOW}⚠${NC} Replication not configured (run mysql/setup-replication.sh)"
fi

echo ""
echo "5. Elasticsearch Indices"
echo "------------------------"
curl -s http://localhost:9200/_cat/indices?v 2>/dev/null || echo "  No indices yet created"

echo ""
echo "6. Elasticsearch IK Plugin"
echo "--------------------------"
ik_status=$(curl -s http://localhost:9200/_cat/plugins 2>/dev/null | grep analysis-ik || echo "")
if [[ -n "$ik_status" ]]; then
    echo -e "  ${GREEN}✓${NC} IK Analyzer plugin installed"
else
    echo -e "  ${YELLOW}⚠${NC} IK Analyzer not installed (run elasticsearch/install-ik-plugin.sh)"
fi

echo ""
echo "7. MinIO Buckets"
echo "----------------"
echo "  Access MinIO Console: http://localhost:9001"
echo "  Username: minioadmin"
echo "  Password: minioadmin2024"
echo "  Run minio/init.sh to create buckets"

echo ""
echo "8. RabbitMQ Queues"
echo "------------------"
queues=$(curl -s -u rabbitmq:rabbitmq2024 http://localhost:15672/api/queues/%2Flibrary 2>/dev/null | grep -o '"name":"[^"]*"' | wc -l || echo "0")
if [ "$queues" -gt 0 ]; then
    echo -e "  ${GREEN}✓${NC} $queues queues configured"
else
    echo -e "  ${YELLOW}⚠${NC} No queues found (configurations from definitions.json should load automatically)"
fi

echo ""
echo "9. Network Connectivity"
echo "-----------------------"
check_service "Network (library-network)" \
    "docker network inspect library-network"

echo ""
echo "10. Port Availability"
echo "---------------------"
ports=(3306 3307 6379 9200 5601 9000 9001 5672 15672)
for port in "${ports[@]}"; do
    if lsof -i :$port > /dev/null 2>&1; then
        echo -e "  Port $port: ${GREEN}in use${NC}"
    else
        echo -e "  Port $port: ${RED}not in use${NC}"
    fi
done

echo ""
echo "11. Disk Usage"
echo "--------------"
du -sh mysql/*/data redis/data elasticsearch/data minio/data rabbitmq/data 2>/dev/null || echo "  Data directories empty or not created"

echo ""
echo "=========================================="
echo "Verification Complete!"
echo "=========================================="
echo ""
echo "Access URLs:"
echo "  Kibana:        http://localhost:5601"
echo "  MinIO Console: http://localhost:9001"
echo "  RabbitMQ UI:   http://localhost:15672"
echo ""
