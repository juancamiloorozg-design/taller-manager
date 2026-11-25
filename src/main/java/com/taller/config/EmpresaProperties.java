package com.taller.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Datos del taller, leidos de application.properties (prefijo taller.empresa).
 * Se usan en las paginas legales y en la orden impresa.
 */
@ConfigurationProperties(prefix = "taller.empresa")
public class EmpresaProperties {

    private String nombre;
    private String nit;
    private String direccion;
    private String ciudad;
    private String email;
    private String telefono;
    private String politicaVersion;
    private String politicaFecha;
    private int cotizacionDias = 8;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getPoliticaVersion() { return politicaVersion; }
    public void setPoliticaVersion(String politicaVersion) { this.politicaVersion = politicaVersion; }

    public String getPoliticaFecha() { return politicaFecha; }
    public void setPoliticaFecha(String politicaFecha) { this.politicaFecha = politicaFecha; }

    public int getCotizacionDias() { return cotizacionDias; }
    public void setCotizacionDias(int cotizacionDias) { this.cotizacionDias = cotizacionDias; }
}
