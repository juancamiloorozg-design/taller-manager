package com.taller.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** Linea de una orden: un repuesto, su cantidad y el precio congelado al agregarlo. */
@Entity
@Table(name = "orden_repuestos")
public class OrdenRepuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenServicio orden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repuesto_id", nullable = false)
    private Repuesto repuesto;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    protected OrdenRepuesto() {
        // Requerido por JPA
    }

    public OrdenRepuesto(Repuesto repuesto, int cantidad) {
        this.repuesto = repuesto;
        this.cantidad = cantidad;
        this.precioUnitario = repuesto.getPrecio();
    }

    public void aumentar(int extra) {
        this.cantidad += extra;
    }

    public BigDecimal getSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public Long getId() { return id; }

    public OrdenServicio getOrden() { return orden; }
    public void setOrden(OrdenServicio orden) { this.orden = orden; }

    public Repuesto getRepuesto() { return repuesto; }

    public int getCantidad() { return cantidad; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
}
