package com.restaurante.pedidos.model;

public class PlatoDTO {
    private String id;
    private String nombre;
    private String categoria;
    private double precio;
    private boolean disponible;

    public PlatoDTO() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }
    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
}
