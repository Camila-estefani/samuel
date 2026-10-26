# setup.ps1 — Levanta todo el sistema del restaurante desde cero
$ROOT = "C:\Users\Camila\Desktop\estefani\microservicios-restaurante"

Write-Host "=== PASO 1: Reiniciando Minikube ===" -ForegroundColor Cyan
minikube delete 2>$null
minikube start --driver=docker --container-runtime=docker --cpus=4 --memory=6144
minikube update-context

Write-Host "=== Esperando que el cluster este listo ===" -ForegroundColor Cyan
Start-Sleep -Seconds 20
kubectl get nodes

Write-Host "=== PASO 2: Instalando Prometheus y Grafana ===" -ForegroundColor Cyan
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack `
    --namespace monitoring `
    --create-namespace `
    --values "$ROOT\k8s\values-prometheus.yaml" `
    --timeout 10m

Write-Host "=== PASO 3: Esperando pods de monitoring ===" -ForegroundColor Cyan
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=grafana -n monitoring --timeout=300s

Write-Host "=== PASO 4: Construyendo imagenes dentro de Minikube ===" -ForegroundColor Cyan
minikube image build -t menu-service:1.0.0 "$ROOT\microservices\menu-service"
minikube image build -t pedidos-service:1.0.0 "$ROOT\microservices\pedidos-service"

Write-Host "=== PASO 5: Desplegando microservicios ===" -ForegroundColor Cyan
kubectl apply -f "$ROOT\k8s\00-namespace.yaml"
kubectl apply -f "$ROOT\k8s\01-menu-deployment.yaml"
kubectl apply -f "$ROOT\k8s\02-menu-service.yaml"
kubectl apply -f "$ROOT\k8s\03-pedidos-deployment.yaml"
kubectl apply -f "$ROOT\k8s\04-pedidos-service.yaml"
kubectl apply -f "$ROOT\k8s\05-service-monitors.yaml"
kubectl apply -f "$ROOT\k8s\06-prometheus-rules.yaml"

Write-Host "=== PASO 6: Esperando pods del restaurante ===" -ForegroundColor Cyan
kubectl rollout status deployment/menu-service -n restaurante --timeout=120s
kubectl rollout status deployment/pedidos-service -n restaurante --timeout=120s

Write-Host "=== LISTO ===" -ForegroundColor Green
kubectl get pods -n restaurante
kubectl get pods -n monitoring
Write-Host ""
Write-Host "Abriendo servicios..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "minikube service menu-service -n restaurante"
Start-Process powershell -ArgumentList "minikube service pedidos-service -n restaurante"
Start-Process powershell -ArgumentList "minikube service kube-prometheus-stack-grafana -n monitoring"
Start-Process powershell -ArgumentList "minikube service kube-prometheus-stack-prometheus -n monitoring"
Write-Host "Grafana: admin / Restaurante2024!" -ForegroundColor Green
