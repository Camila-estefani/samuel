package com.restaurante.menu.metrics;

import com.restaurante.menu.repository.PlatoRepository;
import io.micrometer.core.instrument.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class MenuMetrics {

    private final MeterRegistry registry;
    private final PlatoRepository platoRepository;

    @Value("${menu.artificial.delay.ms:0}")
    private long delayMs;

    private Counter consultasTotal;
    private Counter consultasCategoriaTotal;
    private Counter erroresTotal;
    private Counter platosNoEncontradosTotal;
    private Timer consultaPlatoTimer;

    public MenuMetrics(MeterRegistry registry, PlatoRepository platoRepository) {
        this.registry = registry;
        this.platoRepository = platoRepository;
    }

    @PostConstruct
    public void init() {
        consultasTotal = Counter.builder("menu_consultas_total")
                .description("Total de consultas al menu")
                .tag("servicio", "menu-service")
                .register(registry);

        consultasCategoriaTotal = Counter.builder("menu_consultas_categoria_total")
                .description("Consultas filtradas por categoria")
                .tag("servicio", "menu-service")
                .register(registry);

        erroresTotal = Counter.builder("menu_errores_total")
                .description("Errores en el menu-service")
                .tag("servicio", "menu-service")
                .register(registry);

        platosNoEncontradosTotal = Counter.builder("menu_platos_no_encontrados_total")
                .description("Platos consultados que no existen")
                .tag("servicio", "menu-service")
                .register(registry);

        Gauge.builder("menu_platos_disponibles", platoRepository, PlatoRepository::contarDisponibles)
                .description("Platos disponibles en el menu en tiempo real")
                .tag("servicio", "menu-service")
                .register(registry);

        Gauge.builder("menu_delay_artificial_ms", this, m -> (double) m.getDelayMs())
                .description("Delay artificial en ms para simulacion de lentitud")
                .tag("servicio", "menu-service")
                .register(registry);

        consultaPlatoTimer = Timer.builder("menu_consulta_plato_duration")
                .description("Latencia de consulta de plato por ID")
                .tag("servicio", "menu-service")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public Counter getConsultasTotal() { return consultasTotal; }
    public Counter getConsultasCategoriaTotal() { return consultasCategoriaTotal; }
    public Counter getErroresTotal() { return erroresTotal; }
    public Counter getPlatosNoEncontradosTotal() { return platosNoEncontradosTotal; }
    public Timer getConsultaPlatoTimer() { return consultaPlatoTimer; }
    public long getDelayMs() { return delayMs; }
    public void setDelayMs(long delayMs) { this.delayMs = delayMs; }
}
