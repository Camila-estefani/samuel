package com.restaurante.menu.controller;

import com.restaurante.menu.metrics.MenuMetrics;
import com.restaurante.menu.model.Plato;
import com.restaurante.menu.repository.PlatoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final PlatoRepository platoRepository;
    private final MenuMetrics menuMetrics;

    @Value("${menu.artificial.delay.ms:0}")
    private long delayMs;

    public MenuController(PlatoRepository platoRepository, MenuMetrics menuMetrics) {
        this.platoRepository = platoRepository;
        this.menuMetrics = menuMetrics;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("menu-service UP");
    }

    @GetMapping("/platos")
    public ResponseEntity<Collection<Plato>> listar() {
        delay();
        menuMetrics.getConsultasTotal().increment();
        return ResponseEntity.ok(platoRepository.findAll());
    }

    @GetMapping("/platos/{id}")
    public ResponseEntity<Plato> obtener(@PathVariable String id) {
        return menuMetrics.getConsultaPlatoTimer().record(() -> {
            delay();
            menuMetrics.getConsultasTotal().increment();
            Optional<Plato> plato = platoRepository.findById(id);
            if (plato.isPresent()) return ResponseEntity.ok(plato.get());
            menuMetrics.getPlatosNoEncontradosTotal().increment();
            return ResponseEntity.<Plato>notFound().build();
        });
    }

    @GetMapping("/platos/categoria/{categoria}")
    public ResponseEntity<List<Plato>> porCategoria(@PathVariable String categoria) {
        delay();
        menuMetrics.getConsultasCategoriaTotal().increment();
        return ResponseEntity.ok(platoRepository.findByCategoria(categoria));
    }

    @PostMapping("/platos")
    public ResponseEntity<Plato> agregar(@RequestBody Plato plato) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platoRepository.save(plato));
    }

    @PutMapping("/platos/{id}")
    public ResponseEntity<Plato> actualizar(@PathVariable String id, @RequestBody Plato datos) {
        Optional<Plato> existente = platoRepository.findById(id);
        if (existente.isEmpty()) {
            menuMetrics.getErroresTotal().increment();
            return ResponseEntity.notFound().build();
        }
        datos.setId(id);
        return ResponseEntity.ok(platoRepository.save(datos));
    }

    @PatchMapping("/platos/{id}/disponibilidad")
    public ResponseEntity<String> cambiarDisponibilidad(@PathVariable String id,
                                                         @RequestParam boolean disponible) {
        Optional<Plato> plato = platoRepository.findById(id);
        if (plato.isEmpty()) {
            menuMetrics.getErroresTotal().increment();
            return ResponseEntity.notFound().build();
        }
        plato.get().setDisponible(disponible);
        platoRepository.save(plato.get());
        return ResponseEntity.ok("Disponibilidad actualizada");
    }

    private void delay() {
        if (delayMs > 0) {
            try { Thread.sleep(delayMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
}
