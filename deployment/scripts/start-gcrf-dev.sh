#!/usr/bin/env bash
# Start a GCRF backend service locally with offset ports (coexist with edu-dev)
# Usage: ./start-gcrf-dev.sh auth-service
#        ./start-gcrf-dev.sh book-service
#        ./start-gcrf-dev.sh all  (starts key services)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
BACKEND="$ROOT/backend"

# Port mapping: service → offset port
declare -A PORTS=(
  [gateway-service]=18080
  [auth-service]=18081
  [book-service]=18082
  [circulation-service]=18083
  [reader-service]=18084
  [system-service]=18085
  [recommend-service]=18086
  [chat-service]=18087
  [analytics-service]=18089
  [notification-service]=18090
)

# Database mapping: service → database name
declare -A DBS=(
  [auth-service]=gcrf_auth
  [book-service]=gcrf_book
  [circulation-service]=gcrf_circulation
  [reader-service]=gcrf_reader
  [system-service]=gcrf_system
  [notification-service]=gcrf_notification
  [analytics-service]=gcrf_analytics
  [chat-service]=gcrf_chat
  [recommend-service]=gcrf_recommend
)

export JAVA_HOME=$(/usr/libexec/java_home -v 21)

start_service() {
  local svc="$1"
  local port="${PORTS[$svc]}"
  local db="${DBS[$svc]:-}"

  echo "Starting $svc on port $port..."

  local datasource_url=""
  if [ -n "$db" ]; then
    datasource_url="jdbc:postgresql://localhost:25432/${db}?stringtype=unspecified"
  fi

  cd "$BACKEND/$svc"

  SERVER_PORT="$port" \
  SPRING_PROFILES_ACTIVE=dev \
  SPRING_DATASOURCE_URL="$datasource_url" \
  SPRING_DATASOURCE_USERNAME=postgres \
  SPRING_DATASOURCE_PASSWORD=gcrf_dev_2026 \
  SPRING_DATA_REDIS_HOST=localhost \
  SPRING_DATA_REDIS_PORT=26379 \
  SPRING_DATA_REDIS_PASSWORD=gcrf_redis_dev \
  SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR=localhost:28848 \
  SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR=localhost:28848 \
  SPRING_CLOUD_NACOS_DISCOVERY_ENABLED=true \
  SPRING_CLOUD_NACOS_CONFIG_ENABLED=false \
  SPRING_RABBITMQ_HOST=localhost \
  SPRING_RABBITMQ_PORT=25672 \
  SPRING_RABBITMQ_USERNAME=gcrf_mq \
  SPRING_RABBITMQ_PASSWORD=gcrf_mq_dev \
  SPRING_RABBITMQ_VIRTUAL_HOST=/gcrf \
  mvn spring-boot:run -DskipTests &

  echo "  → $svc PID=$! on port $port"
}

stop_all() {
  echo "Stopping all GCRF services..."
  pkill -f "spring-boot:run.*gcrf" 2>/dev/null || true
  echo "Done."
}

case "${1:-help}" in
  all)
    echo "Starting key GCRF services (auth, book, reader, circulation, system)..."
    for svc in auth-service book-service reader-service circulation-service system-service; do
      start_service "$svc"
    done
    echo ""
    echo "=== Services starting in background ==="
    echo "Check logs: tail -f backend/<service>/target/*.log"
    echo "Stop all:   $0 stop"
    ;;
  stop)
    stop_all
    ;;
  help)
    echo "Usage: $0 <service-name|all|stop>"
    echo ""
    echo "Services: ${!PORTS[*]}"
    echo ""
    echo "Infra ports: PG=25432 Redis=26379 Nacos=28848 RabbitMQ=25672 MinIO=29000"
    ;;
  *)
    if [ -n "${PORTS[$1]:-}" ]; then
      start_service "$1"
      wait
    else
      echo "Unknown service: $1"
      echo "Available: ${!PORTS[*]}"
      exit 1
    fi
    ;;
esac
