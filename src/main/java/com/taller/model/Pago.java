package com.taller.model;

import com.taller.util.Tiempo;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Abono o pago recibido por una orden. */
@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenServicio orden;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPago metodo;

    @Column(length = 200)
    private String nota;

    @Column(name = "registrado_por", length = 120)
    private String registradoPor;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected Pago() {
        // Requerido por JPA
    }

    public Pago(OrdenServicio orden, BigDecimal monto, MetodoPago metodo, String nota, String registradoPor) {
        this.orden = orden;
        this.monto = monto;
        this.metodo = metodo;
        this.nota = nota;
        this.registradoPor = registradoPor;
        this.fecha = Tiempo.ahora();
    }

    public Long getId() { return id; }
    public OrdenServicio getOrden() { return orden; }
    public BigDecimal getMonto() { return monto; }
    public MetodoPago getMetodo() { return metodo; }
    public String getNota() { return nota; }
    public String getRegistradoPor() { return registradoPor; }
    public LocalDateTime getFecha() { return fecha; }
}
