# Informe Técnico — Restaurante
## Kubernetes con Prometheus & Grafana — Valle Grande

**Alumno:** [Tu nombre]
**Dominio de negocio:** Restaurante
**Servicios:** `menu-service` (Servicio A) · `pedidos-service` (Servicio B)

---

# Parte A — Conceptos

## 1. ¿Qué es Micrometer y por qué se usa en lugar de la librería directa de Prometheus?

Micrometer es una **fachada de métricas** para aplicaciones Java, similar a lo que SLF4J es para los logs. Su objetivo es desacoplar el código de la aplicación del sistema de monitoreo concreto que se use debajo.

Si se usara la librería cliente de Prometheus directamente, el código quedaría atado a Prometheus. Si el equipo decide migrar a Datadog o InfluxDB, habría que reescribir toda la instrumentación. Con Micrometer solo se cambia la dependencia Maven (`micrometer-registry-prometheus`) sin tocar el código.

**Ejemplo del proyecto (`MenuMetrics.java`):**
```java
// Se inyecta MeterRegistry, no nada específico de Prometheus
public MenuMetrics(MeterRegistry registry, PlatoRepository platoRepository) {
    this.registry = registry;
}

// API genérica de Micrometer — el registry decide el formato
Counter.builder("menu_consultas_total")
       .description("Total de consultas al menu")
       .tag("servicio", "menu-service")
       .register(registry);
```

---

## 2. Diferencia entre Counter, Gauge y Timer — ejemplos del código

| Tipo | Qué mide | ¿Puede bajar? |
|------|----------|:---:|
| **Counter** | Ocurrencias acumuladas | No |
| **Gauge** | Valor actual en tiempo real | Sí |
| **Timer** | Duración y cantidad de llamadas | No |

**Counter — `menu_consultas_total`** (`MenuMetrics.java`)
Cuenta cuántas veces se consultó el menú. Solo sube.
```java
consultasTotal = Counter.builder("menu_consultas_total")
        .description("Total de consultas al menu")
        .tag("servicio", "menu-service")
        .register(registry);

// En el controller, al recibir un request:
menuMetrics.getConsultasTotal().increment();
```

**Gauge — `menu_platos_disponibles`** (`MenuMetrics.java`)
Refleja cuántos platos están disponibles en tiempo real. Sube y baja.
```java
Gauge.builder("menu_platos_disponibles",
              platoRepository,
              PlatoRepository::contarDisponibles) // se recalcula en cada scrape
     .description("Platos disponibles en el menu en tiempo real")
     .register(registry);
```

**Timer — `menu_consulta_plato_duration`** (`MenuController.java`)
Mide cuánto tarda cada consulta de plato por ID.
```java
return menuMetrics.getConsultaPlatoTimer().record(() -> {
    delay(); // si hay delay artificial, el timer lo captura
    // ... lógica del endpoint
});
```

---

## 3. ¿Qué es un ServiceMonitor y cómo sabe el Prometheus Operator qué monitorear?

Un **ServiceMonitor** es un Custom Resource (CRD) del Prometheus Operator. En lugar de editar `prometheus.yml` manualmente, se crea un ServiceMonitor y el Operator configura Prometheus automáticamente.

El Operator hace lo siguiente:
1. Vigila todos los ServiceMonitors en los namespaces configurados.
2. Para cada ServiceMonitor, busca Services cuyos **labels coincidan exactamente** con el selector.
3. Genera la configuración de scrape en Prometheus apuntando a esos pods.

**ServiceMonitor del proyecto (`05-service-monitors.yaml`):**
```yaml
spec:
  selector:
    matchLabels:
      app: menu-service      # debe coincidir con el Service
      monitored: "true"
  endpoints:
    - port: http             # nombre del puerto en el Service
      path: /metrics
      interval: 15s
```

**Service (`02-menu-service.yaml`):**
```yaml
metadata:
  labels:
    app: menu-service        # coincide
    monitored: "true"        # coincide
```

Si un label no coincide, Prometheus ignora el target completamente.

---

## 4. Diferencia entre liveness probe y readiness probe

| Probe | Pregunta | Si falla |
|-------|----------|----------|
| **Liveness** | ¿Sigue vivo el proceso? | Kubernetes **reinicia** el pod |
| **Readiness** | ¿Está listo para recibir tráfico? | Kubernetes **quita el pod del Service** sin reiniciarlo |

Spring Boot tarda ~20-30 segundos en arrancar. La readiness probe evita que llegue tráfico antes de que esté listo. La liveness probe detecta si el proceso se quedó colgado y lo reinicia.

**En los Deployments del proyecto:**
```yaml
livenessProbe:
  httpGet:
    path: /api/menu/health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /api/menu/health
    port: 8080
  initialDelaySeconds: 20
  periodSeconds: 5
```

---

## 5. ¿Por qué es necesario apuntar Docker al daemon de Minikube antes de construir?

Minikube corre con su **propio daemon de Docker interno**, separado del daemon local de la PC. Si se hace `docker build` normalmente, la imagen queda en el daemon local donde Kubernetes no la puede ver. El pod queda en `ImagePullBackOff`.

En este proyecto se usó `minikube image build` directamente, que construye la imagen dentro del daemon de Minikube:
```bash
minikube image build -t menu-service:1.0.0 ./microservices/menu-service
```

Por eso los Deployments usan:
```yaml
imagePullPolicy: Never  # no intentes bajarla de internet, usa la local
```

---

## 6. ¿Qué ocurre si no configuras el selector de ServiceMonitors en el values.yaml?

Por defecto Prometheus Operator solo escucha ServiceMonitors del mismo namespace de Prometheus (`monitoring`). Sin configurar `serviceMonitorSelector: {}` y `serviceMonitorNamespaceSelector: {}`, los ServiceMonitors del namespace `restaurante` son completamente ignorados — no aparecen targets, no hay métricas, no hay alertas.

**En `values-prometheus.yaml`:**
```yaml
prometheusSpec:
  serviceMonitorSelector:
    matchLabels: {}       # acepta cualquier ServiceMonitor
  serviceMonitorNamespaceSelector:
    matchLabels: {}       # busca en todos los namespaces
  ruleSelector:
    matchLabels: {}
  ruleNamespaceSelector:
    matchLabels: {}
```

---

# Parte B — PromQL

## 1. Tasa de requests por minuto de pedidos-service en los últimos 5 minutos
```promql
rate(pedidos_confirmados_total[5m]) * 60
```
Para ver total de intentos:
```promql
(rate(pedidos_confirmados_total[5m]) + rate(pedidos_rechazados_total[5m])) * 60
```

## 2. Latencia p95 del endpoint más crítico
```promql
histogram_quantile(
  0.95,
  rate(menu_consulta_plato_duration_seconds_bucket[5m])
)
```

## 3. Estado UP/DOWN de ambos servicios
```promql
up{job=~"menu-service|pedidos-service"}
```

## 4. Query de negocio: porcentaje de pedidos fallidos
```promql
rate(pedidos_rechazados_total[10m]) /
(rate(pedidos_confirmados_total[10m]) + rate(pedidos_rechazados_total[10m]))
* 100
```

---

# Parte C — Casuísticas

## Casuística 1 — El Servicio Caído

**Contexto:** Un deploy erróneo configura `SERVER_PORT=abc`. Spring Boot no puede parsear el puerto, el pod entra en `CrashLoopBackOff`. Los clientes no pueden hacer pedidos.

**Síntoma:** Todos los pedidos se rechazan con "menu-service no disponible".

**Activación:**
```bash
kubectl set env deployment/menu-service -n restaurante SERVER_PORT=abc
kubectl get pods -n restaurante
```

**Diagnóstico en Grafana:** Panel "Estado de los Servicios" cae a 0 (rojo). Alerta `MenuServiceCaido` en FIRING.

**[Insertar captura]**

**Resolución:**
```bash
kubectl set env deployment/menu-service -n restaurante SERVER_PORT=8080
kubectl rollout status deployment/menu-service -n restaurante
```

**Lección:** La liveness probe detecta que el proceso no puede continuar y reinicia el pod. La readiness probe lo retira del balanceador mientras arranca. Sin probes, Kubernetes enviaría tráfico a un pod que no puede responder.

---

## Casuística 2 — El Servicio Lento

**Contexto:** El menu-service empieza a responder en 4 segundos bajo alta carga. pedidos-service tiene timeout de 3 segundos. Todos los pedidos fallan por timeout en cadena.

**Síntoma:** Los clientes hacen el pedido, esperan y reciben error. Los pods están UP.

**Activación:**
```bash
kubectl set env deployment/menu-service -n restaurante MENU_DELAY_MS=4000
# Generar pedidos con PowerShell:
for ($i = 1; $i -le 10; $i++) { Invoke-WebRequest -Uri "http://127.0.0.1:<PUERTO-PEDIDOS>/api/pedidos" -Method POST -ContentType "application/json" -Body '{"platoId":"PL003","cliente":"Test","cantidad":1}' }
```

**Diagnóstico en Grafana:** Panel "Delay Artificial" salta a 4000ms. Timeouts suben. Latencia p95 supera 3s. Alerta `TimeoutsMenuFrecuentes` en FIRING.

**[Insertar captura]**

**Resolución:**
```bash
kubectl set env deployment/menu-service -n restaurante MENU_DELAY_MS=0
```

**Lección:** La latencia se propaga en cadena entre microservicios. Sin el Gauge `menu_delay_artificial_ms` en Grafana, sería imposible saber cuál servicio es el culpable.

---

## Casuística 3 — Plato Agotado (negocio)

**Contexto:** El plato más pedido del día (Filete de Res, PL005) se marca como no disponible por error en cocina. Los clientes siguen intentando pedirlo y reciben rechazos continuos.

**Síntoma:** Alta tasa de pedidos rechazados con "Plato no disponible". Los servicios están UP y con latencia normal.

**Activación:**
```bash
# Marcar PL005 como no disponible
Invoke-WebRequest -Uri "http://127.0.0.1:<PUERTO-MENU>/api/menu/platos/PL005/disponibilidad?disponible=false" -Method PATCH

# Generar pedidos que van a fallar
for ($i = 1; $i -le 10; $i++) { Invoke-WebRequest -Uri "http://127.0.0.1:<PUERTO-PEDIDOS>/api/pedidos" -Method POST -ContentType "application/json" -Body '{"platoId":"PL005","cliente":"Cliente","cantidad":1}' }
```

**Diagnóstico en Grafana:** Panel "Platos Disponibles" baja. Panel de rechazados sube. Alerta `AltaTasaPedidosRechazados` en FIRING.

**[Insertar captura]**

**Resolución:**
```bash
Invoke-WebRequest -Uri "http://127.0.0.1:<PUERTO-MENU>/api/menu/platos/PL005/disponibilidad?disponible=true" -Method PATCH
```

**Lección:** El monitoreo de negocio es tan importante como el de infraestructura. Los servicios estaban técnicamente funcionando pero el negocio perdía pedidos. Solo las métricas de negocio (`menu_platos_disponibles`, `pedidos_rechazados_total`) permitieron detectarlo.

---

# Parte D — Evidencias

## D1. Targets en Prometheus — estado UP
**URL:** `http://127.0.0.1:<PUERTO-PROMETHEUS>/targets`
**[Insertar captura con menu-service y pedidos-service en UP]**

## D2. Dashboard de Grafana con los 8 paneles
**[Insertar captura del dashboard con datos reales]**

## D3. Pods en Running
```bash
kubectl get pods -n restaurante
```
**[Insertar captura de terminal]**

## D4. Alerta en FIRING
**URL:** `http://127.0.0.1:<PUERTO-PROMETHEUS>/alerts`
**[Insertar captura de MenuServiceCaido en FIRING]**
