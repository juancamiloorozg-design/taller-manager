package com.taller.model;

import com.taller.util.Tiempo;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Foto del vehiculo tomada en la recepcion o durante el trabajo. El archivo vive en disco. */
@Entity
@Table(name = "orden_fotos")
public class OrdenFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenServicio orden;

    @Column(name = "nombre_archivo", nullable = false, length = 80)
    private String nombreArchivo;

    @Column(name = "tipo_contenido", nullable = false, length = 30)
    private String tipoContenido;

    @Column(name = "subida_por", length = 120)
    private String subidaPor;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    protected OrdenFoto() {
        // Requerido por JPA
    }

    public OrdenFoto(OrdenServicio orden, String nombreArchivo, String tipoContenido, String subidaPor) {
        this.orden = orden;
        this.nombreArchivo = nombreArchivo;
        this.tipoContenido = tipoContenido;
        this.subidaPor = subidaPor;
        this.creadaEn = Tiempo.ahora();
    }

    public Long getId() { return id; }
    public OrdenServicio getOrden() { return orden; }
    public String getNombreArchivo() { return nombreArchivo; }
    public String getTipoContenido() { return tipoContenido; }
    public String getSubidaPor() { return subidaPor; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
}
