#!/bin/bash
# setup.sh — Levanta todo el sistema del restaurante desde cero
set -e

echo "=== PASO 1: Verificar herramientas ==="
command -v docker   >/dev/null 2>&1 || { echo "Docker no instalado"; exit 1; }
command -v minikube >/dev/null 2>&1 || { echo "Minikube no instalado"; exit 1; }
command -v kubectl  >/dev/null 2>&1 || { echo "kubectl no instalado"; exit 1; }
command -v helm     >/dev/null 2>&1 || { echo "Helm no instalado"; exit 1; }

echo "=== PASO 2: Iniciar Minikube ==="
minikube start --driver=docker --container-runtime=docker --cpus=4 --memory=6144
minikube addons enable metrics-server

echo "=== PASO 3: Instalar Prometheus y Grafana ==="
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace monitoring --create-namespace \
  --values "$(dirname $0)/../k8s/values-prometheus.yaml" --timeout 10m

echo "=== PASO 4: Construir imágenes dentro de Minikube ==="
ROOT="$(dirname $0)/.."
minikube image build -t menu-service:1.0.0    "$ROOT/microservices/menu-service"
minikube image build -t pedidos-service:1.0.0 "$ROOT/microservices/pedidos-service"

echo "=== PASO 5: Aplicar manifiestos ==="
kubectl apply -f "$ROOT/k8s/00-namespace.yaml"
kubectl apply -f "$ROOT/k8s/01-menu-deployment.yaml"
kubectl apply -f "$ROOT/k8s/02-menu-service.yaml"
kubectl apply -f "$ROOT/k8s/03-pedidos-deployment.yaml"
kubectl apply -f "$ROOT/k8s/04-pedidos-service.yaml"
kubectl apply -f "$ROOT/k8s/05-service-monitors.yaml"
kubectl apply -f "$ROOT/k8s/06-prometheus-rules.yaml"

echo "=== PASO 6: Esperando pods ==="
kubectl rollout status deployment/menu-service    -n restaurante --timeout=120s
kubectl rollout status deployment/pedidos-service -n restaurante --timeout=120s

echo "=== LISTO ==="
minikube service list
