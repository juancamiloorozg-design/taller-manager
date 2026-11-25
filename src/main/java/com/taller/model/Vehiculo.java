package com.taller.model;

import com.taller.util.Tiempo;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehiculos")
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String placa;

    @Column(nullable = false, length = 50)
    private String marca;

    @Column(nullable = false, length = 50)
    private String modelo;

    @Column(nullable = false)
    private int anio;

    @Column(length = 30)
    private String color;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    protected Vehiculo() {
        // Requerido por JPA
    }

    public Vehiculo(String placa, String marca, String modelo, int anio, Cliente cliente) {
        setPlaca(placa);
        this.marca = marca;
        this.modelo = modelo;
        this.anio = anio;
        this.cliente = cliente;
    }

    @PrePersist
    void alCrear() {
        if (creadoEn == null) {
            creadoEn = Tiempo.ahora();
        }
    }

    public Long getId() { return id; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) {
        this.placa = placa == null ? null : placa.trim().toUpperCase();
    }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
}
