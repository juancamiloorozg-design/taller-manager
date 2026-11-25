package com.taller.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "citas")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(nullable = false)
    private boolean cancelada;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", unique = true)
    private OrdenServicio orden;

    protected Cita() {}

    public Cita(Vehiculo vehiculo, LocalDateTime fechaHora, String motivo) {
        this.vehiculo = vehiculo;
        this.fechaHora = fechaHora;
        this.motivo = motivo;
    }

    public Long getId() { return id; }
    public Vehiculo getVehiculo() { return vehiculo; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getMotivo() { return motivo; }
    public boolean isCancelada() { return cancelada; }
    public OrdenServicio getOrden() { return orden; }

    public void cancelar() { cancelada = true; }
    public void vincularOrden(OrdenServicio orden) { this.orden = orden; }
}
