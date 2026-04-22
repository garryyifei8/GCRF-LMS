#!/usr/bin/env bash
# Deploy GCRF Library System to K8s test cluster
# Usage: ./deploy.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
K8S="$ROOT/deployment/k8s"
BACKEND="$ROOT/backend"
WEBADMIN="$ROOT/web-admin"

# Target servers
MASTER="t1@192.168.1.20"
NODE1="t2@192.168.1.19"
NODE2="t3@192.168.1.21"
SSHPASS="sshpass -p gcrf"
SSH_OPTS="-o StrictHostKeyChecking=no -o ConnectTimeout=10"

SERVICES=(auth-service book-service circulation-service reader-service system-service analytics-service chat-service recommend-service)
PORTS=(8081 8082 8083 8084 8085 8089 8087 8086)

log() { echo -e "\033[36m[$(date +%H:%M:%S)]\033[0m $*"; }
ok()  { echo -e "  \033[32m✓\033[0m $*"; }

# ========================================
# Phase 1: Build JARs (if not already built)
# ========================================
log "Phase 1: Building backend JARs"
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
if [ ! -f "$BACKEND/auth-service/target/auth-service-1.0.0-SNAPSHOT.jar" ]; then
  cd "$BACKEND"
  mvn clean package -DskipTests --no-transfer-progress
fi

log "Phase 1b: Building frontend"
if [ ! -d "$WEBADMIN/dist" ]; then
  cd "$WEBADMIN"
  npm run build
fi

# ========================================
# Phase 2: Create Docker images as tar files
# ========================================
log "Phase 2: Creating Docker image tar files"
TMPDIR=$(mktemp -d)

# Backend services — use a simple JRE base image
for i in "${!SERVICES[@]}"; do
  svc="${SERVICES[$i]}"
  port="${PORTS[$i]}"
  jar="$BACKEND/$svc/target/${svc}-1.0.0-SNAPSHOT.jar"
  if [ ! -f "$jar" ]; then
    echo "  ⚠ $svc JAR not found, skipping"
    continue
  fi

  img="gcrf/${svc}:latest"
  log "  Building image: $img"

  # Create a minimal Dockerfile
  cat > "$TMPDIR/Dockerfile-$svc" <<DEOF
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY ${svc}.jar app.jar
EXPOSE $port
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
DEOF

  cp "$jar" "$TMPDIR/${svc}.jar"
  docker build -f "$TMPDIR/Dockerfile-$svc" -t "$img" "$TMPDIR"
  docker save "$img" | gzip > "$TMPDIR/${svc}.tar.gz"
  ok "$svc → ${svc}.tar.gz ($(du -h "$TMPDIR/${svc}.tar.gz" | cut -f1))"
  rm "$TMPDIR/${svc}.jar"
done

# Web admin
log "  Building image: gcrf/web-admin:latest"
cat > "$TMPDIR/Dockerfile-web" <<'DEOF'
FROM nginx:alpine
COPY dist/ /usr/share/nginx/html/
DEOF
cp -r "$WEBADMIN/dist" "$TMPDIR/dist"
docker build -f "$TMPDIR/Dockerfile-web" -t gcrf/web-admin:latest "$TMPDIR"
docker save gcrf/web-admin:latest | gzip > "$TMPDIR/web-admin.tar.gz"
ok "web-admin → web-admin.tar.gz ($(du -h "$TMPDIR/web-admin.tar.gz" | cut -f1))"
rm -rf "$TMPDIR/dist"

# ========================================
# Phase 3: Transfer images to cluster nodes
# ========================================
log "Phase 3: Transferring images to worker nodes"
for node in "$NODE1" "$NODE2"; do
  log "  → $node"
  for svc in "${SERVICES[@]}" web-admin; do
    if [ -f "$TMPDIR/${svc}.tar.gz" ]; then
      $SSHPASS scp $SSH_OPTS "$TMPDIR/${svc}.tar.gz" "$node:/tmp/" 2>/dev/null
      $SSHPASS ssh $SSH_OPTS "$node" "echo gcrf | sudo -S sh -c 'gunzip -c /tmp/${svc}.tar.gz | ctr -n k8s.io images import - && rm /tmp/${svc}.tar.gz'" 2>/dev/null
      ok "$svc"
    fi
  done
done

# ========================================
# Phase 4: Deploy K8s manifests
# ========================================
log "Phase 4: Deploying to K8s"
$SSHPASS scp $SSH_OPTS "$K8S"/*.yaml "$MASTER:/tmp/gcrf-k8s/" 2>/dev/null || {
  $SSHPASS ssh $SSH_OPTS "$MASTER" "mkdir -p /tmp/gcrf-k8s"
  $SSHPASS scp $SSH_OPTS "$K8S"/*.yaml "$MASTER:/tmp/gcrf-k8s/"
}

$SSHPASS ssh $SSH_OPTS "$MASTER" "echo gcrf | sudo -S sh -c '
  kubectl apply -f /tmp/gcrf-k8s/00-namespace.yaml
  kubectl apply -f /tmp/gcrf-k8s/01-secrets.yaml
  kubectl apply -f /tmp/gcrf-k8s/02-configmap.yaml
  kubectl apply -f /tmp/gcrf-k8s/30-init-databases.yaml
  echo \"Waiting for DB init...\"
  kubectl wait --for=condition=complete job/gcrf-init-db -n gcrf-prod --timeout=60s 2>/dev/null || true
  kubectl apply -f /tmp/gcrf-k8s/10-services.yaml
  kubectl apply -f /tmp/gcrf-k8s/20-web-admin.yaml
  echo \"=== Deployment status ===\"
  kubectl get pods -n gcrf-prod -o wide
'"

# ========================================
# Phase 5: Cleanup
# ========================================
rm -rf "$TMPDIR"

log "Done! Access the app at:"
echo "  http://192.168.1.19:31080"
echo "  http://192.168.1.21:31080"
