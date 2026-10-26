#!/bin/bash
# teardown.sh — Elimina todos los recursos
# Uso: ./teardown.sh           → elimina recursos pero mantiene cluster
#      ./teardown.sh --full    → elimina todo incluyendo el cluster

ROOT="$(dirname $0)/.."

kubectl delete -f "$ROOT/k8s/06-prometheus-rules.yaml"   --ignore-not-found=true
kubectl delete -f "$ROOT/k8s/05-service-monitors.yaml"   --ignore-not-found=true
kubectl delete -f "$ROOT/k8s/04-pedidos-service.yaml"    --ignore-not-found=true
kubectl delete -f "$ROOT/k8s/03-pedidos-deployment.yaml" --ignore-not-found=true
kubectl delete -f "$ROOT/k8s/02-menu-service.yaml"       --ignore-not-found=true
kubectl delete -f "$ROOT/k8s/01-menu-deployment.yaml"    --ignore-not-found=true
kubectl delete -f "$ROOT/k8s/00-namespace.yaml"          --ignore-not-found=true

helm uninstall kube-prometheus-stack -n monitoring --ignore-not-found 2>/dev/null || true
kubectl delete namespace monitoring --ignore-not-found=true

if [ "$1" = "--full" ]; then
  echo "Eliminando cluster..."
  minikube delete
fi

echo "Teardown completado."
