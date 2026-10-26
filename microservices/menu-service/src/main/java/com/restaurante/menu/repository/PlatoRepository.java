package com.restaurante.menu.repository;

import com.restaurante.menu.model.Plato;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PlatoRepository {

    private final Map<String, Plato> platos = new ConcurrentHashMap<>();

    public PlatoRepository() {
        platos.put("PL001", new Plato("PL001", "Sopa de Tomate", "entrada", 5.50, true));
        platos.put("PL002", new Plato("PL002", "Ensalada César", "entrada", 7.00, true));
        platos.put("PL003", new Plato("PL003", "Pollo a la Plancha", "principal", 12.50, true));
        platos.put("PL004", new Plato("PL004", "Pasta Carbonara", "principal", 11.00, true));
        platos.put("PL005", new Plato("PL005", "Filete de Res", "principal", 18.00, true));
        platos.put("PL006", new Plato("PL006", "Tiramisú", "postre", 6.00, true));
        platos.put("PL007", new Plato("PL007", "Jugo de Naranja", "bebida", 3.00, true));
        platos.put("PL008", new Plato("PL008", "Pastel de Chocolate", "postre", 6.50, false));
    }

    public Collection<Plato> findAll() { return platos.values(); }

    public Optional<Plato> findById(String id) { return Optional.ofNullable(platos.get(id)); }

    public List<Plato> findByCategoria(String categoria) {
        List<Plato> res = new ArrayList<>();
        for (Plato p : platos.values())
            if (p.getCategoria().equalsIgnoreCase(categoria)) res.add(p);
        return res;
    }

    public Plato save(Plato p) { platos.put(p.getId(), p); return p; }

    public long contarDisponibles() {
        return platos.values().stream().filter(Plato::isDisponible).count();
    }

    public long contarTotal() { return platos.size(); }
}
