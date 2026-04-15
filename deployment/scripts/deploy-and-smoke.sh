#!/usr/bin/env bash
# Bring up full stack, wait for health, smoke-test all /actuator/health
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

log() { echo -e "\033[36m[$(date +%H:%M:%S)]\033[0m $*"; }
ok()  { echo -e "  \033[32m✓\033[0m $*"; }
err() { echo -e "  \033[31m✗\033[0m $*"; }

log "Phase 1: Infrastructure"
docker compose -f docker-compose.infrastructure.yml up -d
log "Waiting 30s for infra warm-up..."
sleep 30
docker compose -f docker-compose.infrastructure.yml ps

log "Phase 2: Build + start services"
docker compose -f docker-compose.services.yml up -d --build

log "Phase 3: Wait for service healthy"
SERVICES=(
  "gateway:8080"
  "auth:8081"
  "book:8082"
  "circulation:8083"
  "reader:8084"
  "notification:8085"
  "analytics:8087"
  "chat:8088"
  "recommend:8089"
  "system:8090"
)
TIMEOUT_PER_SERVICE=180
FAILED=()

for entry in "${SERVICES[@]}"; do
  IFS=: read -r name port <<< "$entry"
  printf "  Waiting %-16s (port %s)..." "$name" "$port"
  for i in $(seq 1 "$TIMEOUT_PER_SERVICE"); do
    if curl -sf "http://localhost:${port}/actuator/health" > /dev/null 2>&1; then
      printf " \033[32m✓\033[0m %ds\n" "$i"
      break
    fi
    sleep 1
    if [ "$i" -eq "$TIMEOUT_PER_SERVICE" ]; then
      printf " \033[31m✗\033[0m timeout\n"
      FAILED+=("$name")
    fi
  done
done

log "Phase 4: Final report"
if [ ${#FAILED[@]} -gt 0 ]; then
  err "Services failed health check: ${FAILED[*]}"
  for name in "${FAILED[@]}"; do
    log "Logs for ${name}-service:"
    docker compose -f docker-compose.services.yml logs --tail=50 "${name}-service" || true
  done
  exit 1
fi

ok "All ${#SERVICES[@]} services healthy."
exit 0
