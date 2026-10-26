package com.restaurante.pedidos.service;

import com.restaurante.pedidos.config.WebClientConfig;
import com.restaurante.pedidos.metrics.PedidosMetrics;
import com.restaurante.pedidos.model.PlatoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Service
public class MenuClient {

    private static final Logger log = LoggerFactory.getLogger(MenuClient.class);

    private final WebClient menuWebClient;
    private final WebClientConfig config;
    private final PedidosMetrics pedidosMetrics;

    public MenuClient(WebClient menuWebClient, WebClientConfig config, PedidosMetrics pedidosMetrics) {
        this.menuWebClient = menuWebClient;
        this.config = config;
        this.pedidosMetrics = pedidosMetrics;
    }

    public Optional<PlatoDTO> obtenerPlato(String platoId) {
        try {
            PlatoDTO plato = menuWebClient.get()
                    .uri("/api/menu/platos/{id}", platoId)
                    .retrieve()
                    .bodyToMono(PlatoDTO.class)
                    .timeout(config.getTimeout())
                    .onErrorResume(TimeoutException.class, ex -> {
                        log.error("[MenuClient] TIMEOUT consultando plato {}", platoId);
                        pedidosMetrics.getTimeoutsMenuTotal().increment();
                        return Mono.empty();
                    })
                    .onErrorResume(ex -> {
                        log.error("[MenuClient] ERROR consultando plato {}: {}", platoId, ex.getMessage());
                        pedidosMetrics.getErroresComunicacionTotal().increment();
                        return Mono.empty();
                    })
                    .block();
            return Optional.ofNullable(plato);
        } catch (Exception e) {
            log.error("[MenuClient] Excepcion: {}", e.getMessage());
            pedidosMetrics.getErroresComunicacionTotal().increment();
            return Optional.empty();
        }
    }
}
