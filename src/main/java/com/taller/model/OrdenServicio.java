package com.taller.model;

import com.taller.util.Tiempo;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordenes_servicio")
public class OrdenServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @Column(nullable = false, length = 500)
    private String descripcion;

    private Integer kilometraje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrden estado = EstadoOrden.PENDIENTE;

    @Column(name = "mano_obra", nullable = false, precision = 12, scale = 2)
    private BigDecimal manoObra = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "creado_por", length = 120)
    private String creadoPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @Column(name = "finalizado_en")
    private LocalDateTime finalizadoEn;

    @Column(name = "aprobada_en")
    private LocalDateTime aprobadaEn;

    @Column(name = "proxima_fecha")
    private LocalDate proximaFecha;

    @Column(name = "proximo_km")
    private Integer proximoKm;

    @Column(name = "recordatorio_atendido", nullable = false)
    private boolean recordatorioAtendido;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_combustible", length = 20)
    private NivelCombustible nivelCombustible;

    @Column(name = "danos_visibles", length = 500)
    private String danosVisibles;

    @Column(name = "objetos_dejados", length = 500)
    private String objetosDejados;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mecanico_id")
    private Usuario mecanico;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrdenRepuesto> detalle = new ArrayList<>();

    protected OrdenServicio() {
        // Requerido por JPA
    }

    public OrdenServicio(Vehiculo vehiculo, String descripcion, Integer kilometraje,
                         BigDecimal manoObra, String creadoPor) {
        this.vehiculo = vehiculo;
        this.descripcion = descripcion;
        this.kilometraje = kilometraje;
        this.manoObra = manoObra == null ? BigDecimal.ZERO : manoObra;
        this.creadoPor = creadoPor;
        recalcular();
    }

    @PrePersist
    void alCrear() {
        LocalDateTime ahora = Tiempo.ahora();
        if (creadoEn == null) {
            creadoEn = ahora;
        }
        actualizadoEn = ahora;
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = Tiempo.ahora();
    }

    public void agregarDetalle(OrdenRepuesto linea) {
        linea.setOrden(this);
        detalle.add(linea);
        recalcular();
    }

    public void quitarDetalle(OrdenRepuesto linea) {
        detalle.remove(linea);
        recalcular();
    }

    /** Devuelve la linea de ese repuesto, o null si la orden todavia no lo tiene. */
    public OrdenRepuesto buscarLinea(Long repuestoId) {
        for (OrdenRepuesto linea : detalle) {
            if (linea.getRepuesto().getId().equals(repuestoId)) {
                return linea;
            }
        }
        return null;
    }

    public void editarDatos(String descripcion, Integer kilometraje, BigDecimal manoObra) {
        this.descripcion = descripcion;
        this.kilometraje = kilometraje;
        this.manoObra = manoObra == null ? BigDecimal.ZERO : manoObra;
        recalcular();
    }

    public void registrarRecepcion(NivelCombustible nivel, String danos, String objetos) {
        this.nivelCombustible = nivel;
        this.danosVisibles = danos;
        this.objetosDejados = objetos;
    }

    /** Programa el aviso del proximo servicio. Si ya se habia avisado, vuelve a quedar pendiente. */
    public void programarRecordatorio(LocalDate fecha, Integer km) {
        this.proximaFecha = fecha;
        this.proximoKm = km;
        this.recordatorioAtendido = false;
    }

    public void aprobar() {
        this.aprobadaEn = Tiempo.ahora();
        this.estado = EstadoOrden.PENDIENTE;
    }

    /** Total = mano de obra + suma de (cantidad x precio) de cada repuesto. */
    public void recalcular() {
        BigDecimal repuestos = BigDecimal.ZERO;
        for (OrdenRepuesto linea : detalle) {
            repuestos = repuestos.add(linea.getSubtotal());
        }
        this.total = manoObra.add(repuestos);
    }

    public BigDecimal getTotalRepuestos() {
        return total.subtract(manoObra);
    }

    public void cambiarEstado(EstadoOrden nuevo) {
        this.estado = nuevo;
        if (nuevo == EstadoOrden.FINALIZADA) {
            this.finalizadoEn = Tiempo.ahora();
        }
    }

    public Long getId() { return id; }

    public Vehiculo getVehiculo() { return vehiculo; }

    public String getDescripcion() { return descripcion; }

    public Integer getKilometraje() { return kilometraje; }

    public EstadoOrden getEstado() { return estado; }

    public BigDecimal getManoObra() { return manoObra; }

    public BigDecimal getTotal() { return total; }

    public String getCreadoPor() { return creadoPor; }

    public LocalDateTime getCreadoEn() { return creadoEn; }

    public LocalDateTime getActualizadoEn() { return actualizadoEn; }

    public LocalDateTime getFinalizadoEn() { return finalizadoEn; }

    public List<OrdenRepuesto> getDetalle() { return detalle; }

    public LocalDateTime getAprobadaEn() { return aprobadaEn; }

    public LocalDate getProximaFecha() { return proximaFecha; }

    public Integer getProximoKm() { return proximoKm; }

    public boolean isRecordatorioAtendido() { return recordatorioAtendido; }
    public void setRecordatorioAtendido(boolean atendido) { this.recordatorioAtendido = atendido; }

    public NivelCombustible getNivelCombustible() { return nivelCombustible; }

    public String getDanosVisibles() { return danosVisibles; }

    public String getObjetosDejados() { return objetosDejados; }

    public Usuario getMecanico() { return mecanico; }
    public void setMecanico(Usuario mecanico) { this.mecanico = mecanico; }
}
