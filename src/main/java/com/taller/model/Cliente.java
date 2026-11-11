package com.taller.model;

import com.taller.util.Tiempo;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Titular de datos personales. Guarda la prueba de la autorizacion de
 * tratamiento (fecha, version de la politica y quien la registro), como
 * exige la Ley 1581 de 2012.
 */
@Entity
@Table(name = "clientes")
public class Cliente {

    public static final String NOMBRE_ANONIMO = "Cliente anonimizado";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_documento", nullable = false, length = 10)
    private String tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 30)
    private String numeroDocumento;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 30)
    private String telefono;

    @Column(length = 120)
    private String email;

    @Column(length = 200)
    private String direccion;

    @Column(name = "autoriza_datos", nullable = false)
    private boolean autorizaDatos;

    @Column(name = "fecha_autorizacion")
    private LocalDateTime fechaAutorizacion;

    @Column(name = "version_politica", length = 20)
    private String versionPolitica;

    @Column(name = "registrado_por", length = 120)
    private String registradoPor;

    @Column(nullable = false)
    private boolean anonimizado;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    protected Cliente() {
        // Requerido por JPA
    }

    public Cliente(String tipoDocumento, String numeroDocumento, String nombre) {
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.nombre = nombre;
    }

    @PrePersist
    void alCrear() {
        if (creadoEn == null) {
            creadoEn = Tiempo.ahora();
        }
    }

    /** Registra la autorizacion de tratamiento de datos dada por el titular. */
    public void registrarAutorizacion(String version, String registradoPor) {
        this.autorizaDatos = true;
        this.fechaAutorizacion = Tiempo.ahora();
        this.versionPolitica = version;
        this.registradoPor = registradoPor;
    }

    /**
     * Atiende una solicitud de supresion: borra los datos que identifican a la
     * persona pero conserva la fila para que el historial de ordenes siga
     * siendo consistente.
     */
    public void anonimizar() {
        this.nombre = NOMBRE_ANONIMO;
        this.numeroDocumento = "ANON-" + id;
        this.telefono = null;
        this.email = null;
        this.direccion = null;
        this.autorizaDatos = false;
        this.anonimizado = true;
    }

    public Long getId() { return id; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public boolean isAutorizaDatos() { return autorizaDatos; }

    public LocalDateTime getFechaAutorizacion() { return fechaAutorizacion; }

    public String getVersionPolitica() { return versionPolitica; }

    public String getRegistradoPor() { return registradoPor; }

    public boolean isAnonimizado() { return anonimizado; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
}
