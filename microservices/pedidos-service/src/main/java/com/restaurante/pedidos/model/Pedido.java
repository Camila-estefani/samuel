package com.restaurante.pedidos.model;

import java.time.LocalDateTime;

public class Pedido {
    private String id;
    private String platoId;
    private String cliente;
    private int cantidad;
    private double precioUnitario;
    private double total;
    private String estado; // CONFIRMADO, RECHAZADO
    private String motivoRechazo;
    private LocalDateTime fecha;

    public Pedido() {}

    public Pedido(String platoId, String cliente, int cantidad) {
        this.platoId = platoId;
        this.cliente = cliente;
        this.cantidad = cantidad;
        this.estado = "PENDIENTE";
        this.fecha = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPlatoId() { return platoId; }
    public void setPlatoId(String platoId) { this.platoId = platoId; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double p) { this.precioUnitario = p; this.total = p * cantidad; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
