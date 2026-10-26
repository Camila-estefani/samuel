# Restaurante — Microservicios con Monitoreo en Kubernetes
Curso: Kubernetes con Prometheus & Grafana — Valle Grande

## Dominio
| Servicio | Rol | Descripción |
|----------|-----|-------------|
| `menu-service` | Servicio A | Gestiona el menú de platos del restaurante |
| `pedidos-service` | Servicio B | Procesa pedidos. **Consulta al menu-service** para verificar disponibilidad |

---

## Comandos para levantar el sistema

### 1. Iniciar Minikube
```powershell
minikube start --driver=docker --container-runtime=docker --cpus=4 --memory=6144
minikube addons enable metrics-server
```

### 2. Instalar Prometheus + Grafana
```powershell
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack --namespace monitoring --create-namespace --values C:\Users\Camila\Desktop\estefani\microservicios-restaurante\k8s\values-prometheus.yaml --timeout 10m
```

### 3. Construir imágenes dentro de Minikube
```powershell
minikube image build -t menu-service:1.0.0 C:\Users\Camila\Desktop\estefani\microservicios-restaurante\microservices\menu-service
minikube image build -t pedidos-service:1.0.0 C:\Users\Camila\Desktop\estefani\microservicios-restaurante\microservices\pedidos-service
```

### 4. Desplegar en Kubernetes
```powershell
kubectl apply -f C:\Users\Camila\Desktop\estefani\microservicios-restaurante\k8s\00-namespace.yaml
kubectl apply -f C:\Users\Camila\Desktop\estefani\microservicios-restaurante\k8s\01-menu-deployment.yaml
kubectl apply -f C:\Users\Camila\Desktop\estefani\microservicios-restaurante\k8s\02-menu-service.yaml
kubectl apply -f C:\Users\Camila\Desktop\estefani\microservicios-restaurante\k8s\03-pedidos-deployment.yaml
kubectl apply -f C:\Users\Camila\Desktop\estefani\microservicios-restaurante\k8s\04-pedidos-service.yaml
kubectl apply -f C:\Users\Camila\Desktop\estefani\microservicios-restaurante\k8s\05-service-monitors.yaml
kubectl apply -f C:\Users\Camila\Desktop\estefani\microservicios-restaurante\k8s\06-prometheus-rules.yaml
```

### 5. Verificar pods
```powershell
kubectl get pods -n restaurante
```

### 6. Acceder a los servicios
```powershell
minikube service menu-service -n restaurante
minikube service pedidos-service -n restaurante
minikube service kube-prometheus-stack-grafana -n monitoring
minikube service kube-prometheus-stack-prometheus -n monitoring
```
Grafana: usuario `admin` / contraseña `Restaurante2024!`

---

## Endpoints

### menu-service
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/menu/health` | Health check |
| GET | `/metrics` | Métricas Prometheus |
| GET | `/api/menu/platos` | Listar platos |
| GET | `/api/menu/platos/{id}` | Obtener plato por ID |
| GET | `/api/menu/platos/categoria/{cat}` | Filtrar por categoría |
| POST | `/api/menu/platos` | Agregar plato |
| PUT | `/api/menu/platos/{id}` | Actualizar plato |
| PATCH | `/api/menu/platos/{id}/disponibilidad?disponible=true/false` | Cambiar disponibilidad |

### pedidos-service
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/pedidos/health` | Health check |
| GET | `/metrics` | Métricas Prometheus |
| POST | `/api/pedidos` | Realizar pedido |
| GET | `/api/pedidos` | Listar pedidos |
| GET | `/api/pedidos/{id}` | Obtener pedido |
| GET | `/api/pedidos/estado/{estado}` | Filtrar por estado |
| GET | `/api/pedidos/resumen` | Resumen de negocio |

---

## Casuísticas

### Casuística 1 — Servicio Caído
```powershell
# Activar
kubectl set env deployment/menu-service -n restaurante SERVER_PORT=abc
# Resolver
kubectl set env deployment/menu-service -n restaurante SERVER_PORT=8080
```

### Casuística 2 — Servicio Lento
```powershell
# Activar
kubectl set env deployment/menu-service -n restaurante MENU_DELAY_MS=4000
# Resolver
kubectl set env deployment/menu-service -n restaurante MENU_DELAY_MS=0
```

### Casuística 3 — Plato No Disponible
```powershell
# Activar (reemplaza <PUERTO> con el puerto del tunnel de menu-service)
Invoke-WebRequest -Uri "http://127.0.0.1:<PUERTO>/api/menu/platos/PL005/disponibilidad?disponible=false" -Method PATCH
# Resolver
Invoke-WebRequest -Uri "http://127.0.0.1:<PUERTO>/api/menu/platos/PL005/disponibilidad?disponible=true" -Method PATCH
```
