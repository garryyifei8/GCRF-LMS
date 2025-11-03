#!/bin/bash
# ============================================
# RabbitMQ Initialization Script
# ============================================

set -e

echo "=== Initializing RabbitMQ ==="

# RabbitMQ Management API
RABBITMQ_HOST="http://localhost:15672"
RABBITMQ_USER="${RABBITMQ_USER:-rabbitmq}"
RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD:-rabbitmq2024}"
VHOST="/library"

# Wait for RabbitMQ to be ready
echo "Waiting for RabbitMQ to be ready..."
until curl -f $RABBITMQ_HOST/api/overview -u $RABBITMQ_USER:$RABBITMQ_PASSWORD > /dev/null 2>&1; do
    echo "Waiting for RabbitMQ..."
    sleep 5
done

echo "RabbitMQ is ready!"

# The configuration is already loaded from definitions.json
# This script is mainly for verification

echo ""
echo "=== Verifying Virtual Host ==="
curl -s -u $RABBITMQ_USER:$RABBITMQ_PASSWORD $RABBITMQ_HOST/api/vhosts | grep -q "/library" && echo "Virtual host '/library' exists" || echo "Virtual host '/library' not found"

echo ""
echo "=== Verifying Exchanges ==="
curl -s -u $RABBITMQ_USER:$RABBITMQ_PASSWORD $RABBITMQ_HOST/api/exchanges/$VHOST | \
    python3 -c "import sys, json; exchanges = json.load(sys.stdin); print('\n'.join([f\"  - {e['name']} ({e['type']})\" for e in exchanges if e['name'] != '']))"

echo ""
echo "=== Verifying Queues ==="
curl -s -u $RABBITMQ_USER:$RABBITMQ_PASSWORD $RABBITMQ_HOST/api/queues/$VHOST | \
    python3 -c "import sys, json; queues = json.load(sys.stdin); print('\n'.join([f\"  - {q['name']}\" for q in queues]))"

echo ""
echo "=== Verifying Bindings ==="
curl -s -u $RABBITMQ_USER:$RABBITMQ_PASSWORD $RABBITMQ_HOST/api/bindings/$VHOST | \
    python3 -c "import sys, json; bindings = json.load(sys.stdin); print('\n'.join([f\"  - {b['source']} -> {b['destination']} ({b['routing_key']})\" for b in bindings if b['source'] != '']))"

echo ""
echo "=== RabbitMQ initialization completed ==="
echo "Management UI: http://localhost:15672"
echo "AMQP Port: 5672"
echo "Username: $RABBITMQ_USER"
echo "Password: $RABBITMQ_PASSWORD"
echo "Virtual Host: /library"
