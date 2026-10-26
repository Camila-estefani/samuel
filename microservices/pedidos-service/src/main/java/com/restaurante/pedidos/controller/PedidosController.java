package com.restaurante.pedidos.controller;

import com.restaurante.pedidos.metrics.PedidosMetrics;
import com.restaurante.pedidos.model.Pedido;
import com.restaurante.pedidos.model.PlatoDTO;
import com.restaurante.pedidos.repository.PedidoRepository;
import com.restaurante.pedidos.service.MenuClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/pedidos")
public class PedidosController {

    private static final Logger log = LoggerFactory.getLogger(PedidosController.class);

    private final PedidoRepository pedidoRepository;
    private final MenuClient menuClient;
    private final PedidosMetrics pedidosMetrics;

    public PedidosController(PedidoRepository pedidoRepository, MenuClient menuClient, PedidosMetrics pedidosMetrics) {
        this.pedidoRepository = pedidoRepository;
        this.menuClient = menuClient;
        this.pedidosMetrics = pedidosMetrics;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("pedidos-service UP");
    }

    @PostMapping
    public ResponseEntity<?> realizarPedido(@RequestBody Pedido solicitud) {
        return pedidosMetrics.getProcesamientoTimer().record(() -> {
            log.info("Procesando pedido: plato={}, cliente={}", solicitud.getPlatoId(), solicitud.getCliente());

            // Consultar menu-service
            Optional<PlatoDTO> platoOpt = menuClient.obtenerPlato(solicitud.getPlatoId());

            if (platoOpt.isEmpty()) {
                Pedido rechazado = pedidoRepository.save(solicitud);
                rechazado.setEstado("RECHAZADO");
                rechazado.setMotivoRechazo("menu-service no disponible");
                pedidosMetrics.getPedidosRechazadosTotal().increment();
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", "El menu no esta disponible", "pedido", rechazado));
            }

            PlatoDTO plato = platoOpt.get();

            if (!plato.isDisponible()) {
                Pedido rechazado = pedidoRepository.save(solicitud);
                rechazado.setEstado("RECHAZADO");
                rechazado.setMotivoRechazo("Plato no disponible: " + plato.getNombre());
                pedidosMetrics.getPedidosRechazadosTotal().increment();
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Plato no disponible", "pedido", rechazado));
            }

            Pedido pedido = pedidoRepository.save(solicitud);
            pedido.setPrecioUnitario(plato.getPrecio());
            pedido.setEstado("CONFIRMADO");
            pedidosMetrics.getPedidosConfirmadosTotal().increment();
            log.info("Pedido confirmado: id={}, total={}", pedido.getId(), pedido.getTotal());
            return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
        });
    }

    @GetMapping
    public ResponseEntity<Collection<Pedido>> listar() {
        return ResponseEntity.ok(pedidoRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> obtener(@PathVariable String id) {
        return pedidoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Pedido>> porEstado(@PathVariable String estado) {
        return ResponseEntity.ok(pedidoRepository.findByEstado(estado));
    }

    @GetMapping("/resumen")
    public ResponseEntity<Map<String, Object>> resumen() {
        return ResponseEntity.ok(Map.of(
                "confirmados", pedidoRepository.contarPorEstado("CONFIRMADO"),
                "rechazados", pedidoRepository.contarPorEstado("RECHAZADO"),
                "ingresosTotales", pedidoRepository.totalIngresos()
        ));
    }
}
