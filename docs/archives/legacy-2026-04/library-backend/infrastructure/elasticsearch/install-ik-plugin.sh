#!/bin/bash
# ============================================
# Install IK Analyzer Plugin for Elasticsearch
# ============================================

set -e

echo "=== Installing IK Analyzer Plugin ==="

# Install IK plugin in running container
docker exec library-elasticsearch elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.11.0/elasticsearch-analysis-ik-8.11.0.zip

echo "=== Restarting Elasticsearch ==="
docker restart library-elasticsearch

echo "=== Waiting for Elasticsearch to be ready ==="
sleep 30

echo "=== Verifying IK plugin installation ==="
curl -X GET "localhost:9200/_cat/plugins?v"

echo ""
echo "=== Creating index templates ==="

# Create book template
curl -X PUT "localhost:9200/_index_template/library-books-template" \
  -H 'Content-Type: application/json' \
  -d @templates/book-template.json

# Create reader template
curl -X PUT "localhost:9200/_index_template/library-readers-template" \
  -H 'Content-Type: application/json' \
  -d @templates/reader-template.json

echo ""
echo "=== IK Plugin installation completed ==="
