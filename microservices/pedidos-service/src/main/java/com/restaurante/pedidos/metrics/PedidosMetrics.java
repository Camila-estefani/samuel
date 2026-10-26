package com.restaurante.pedidos.metrics;

import com.restaurante.pedidos.repository.PedidoRepository;
import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class PedidosMetrics {

    private final MeterRegistry registry;
    private final PedidoRepository pedidoRepository;

    private Counter pedidosConfirmadosTotal;
    private Counter pedidosRechazadosTotal;
    private Counter timeoutsMenuTotal;
    private Counter erroresComunicacionTotal;
    private Timer procesamientoTimer;

    public PedidosMetrics(MeterRegistry registry, PedidoRepository pedidoRepository) {
        this.registry = registry;
        this.pedidoRepository = pedidoRepository;
    }

    @PostConstruct
    public void init() {
        pedidosConfirmadosTotal = Counter.builder("pedidos_confirmados_total")
                .description("Pedidos confirmados exitosamente")
                .tag("servicio", "pedidos-service")
                .register(registry);

        pedidosRechazadosTotal = Counter.builder("pedidos_rechazados_total")
                .description("Pedidos rechazados por plato no disponible o error")
                .tag("servicio", "pedidos-service")
                .register(registry);

        timeoutsMenuTotal = Counter.builder("pedidos_timeouts_menu_total")
                .description("Timeouts al consultar el menu-service")
                .tag("servicio", "pedidos-service")
                .register(registry);

        erroresComunicacionTotal = Counter.builder("pedidos_errores_comunicacion_total")
                .description("Errores de comunicacion con menu-service")
                .tag("servicio", "pedidos-service")
                .register(registry);

        Gauge.builder("pedidos_confirmados_activos", pedidoRepository,
                        r -> (double) r.contarPorEstado("CONFIRMADO"))
                .description("Total de pedidos confirmados acumulados")
                .tag("servicio", "pedidos-service")
                .register(registry);

        Gauge.builder("pedidos_ingresos_total", pedidoRepository, PedidoRepository::totalIngresos)
                .description("Ingresos totales por pedidos confirmados")
                .tag("servicio", "pedidos-service")
                .register(registry);

        procesamientoTimer = Timer.builder("pedidos_procesamiento_duration")
                .description("Tiempo de procesamiento de un pedido")
                .tag("servicio", "pedidos-service")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public Counter getPedidosConfirmadosTotal() { return pedidosConfirmadosTotal; }
    public Counter getPedidosRechazadosTotal() { return pedidosRechazadosTotal; }
    public Counter getTimeoutsMenuTotal() { return timeoutsMenuTotal; }
    public Counter getErroresComunicacionTotal() { return erroresComunicacionTotal; }
    public Timer getProcesamientoTimer() { return procesamientoTimer; }
}
