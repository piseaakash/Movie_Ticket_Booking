#!/usr/bin/env bash
# Apply all Kubernetes manifests in order. Requires kubectl and namespace ticketing.
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
kubectl apply -f namespace.yaml
kubectl apply -f secret-template.yaml
kubectl apply -f configmap-common.yaml
kubectl apply -f postgres.yaml
echo "Waiting for Postgres to be ready..."
kubectl wait --for=condition=ready pod -l app=postgres -n ticketing --timeout=120s 2>/dev/null || sleep 20
kubectl apply -f auth-service/deployment.yaml
kubectl apply -f user-service/deployment.yaml
kubectl apply -f theatre-service/deployment.yaml
kubectl apply -f movie-service/deployment.yaml
kubectl apply -f payment-service/deployment.yaml
kubectl apply -f booking-service/deployment.yaml
echo "Done. Check: kubectl get pods -n ticketing"
