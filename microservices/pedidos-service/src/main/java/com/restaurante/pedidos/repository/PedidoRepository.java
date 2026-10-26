package com.restaurante.pedidos.repository;

import com.restaurante.pedidos.model.Pedido;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class PedidoRepository {

    private final Map<String, Pedido> pedidos = new ConcurrentHashMap<>();
    private final AtomicLong contador = new AtomicLong(1);

    public Pedido save(Pedido p) {
        String id = "PED" + String.format("%04d", contador.getAndIncrement());
        p.setId(id);
        pedidos.put(id, p);
        return p;
    }

    public Collection<Pedido> findAll() { return pedidos.values(); }

    public Optional<Pedido> findById(String id) { return Optional.ofNullable(pedidos.get(id)); }

    public List<Pedido> findByEstado(String estado) {
        List<Pedido> res = new ArrayList<>();
        for (Pedido p : pedidos.values())
            if (p.getEstado().equalsIgnoreCase(estado)) res.add(p);
        return res;
    }

    public long contarPorEstado(String estado) {
        return pedidos.values().stream().filter(p -> p.getEstado().equalsIgnoreCase(estado)).count();
    }

    public double totalIngresos() {
        return pedidos.values().stream()
                .filter(p -> "CONFIRMADO".equals(p.getEstado()))
                .mapToDouble(Pedido::getTotal).sum();
    }
}
