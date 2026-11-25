package com.taller.web.form;

import com.taller.service.ClienteService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ClienteForm {

    @NotBlank(message = "Selecciona el tipo de documento.")
    @Pattern(regexp = "CC|CE|NIT|PA|TI", message = "Tipo de documento no válido.")
    private String tipoDocumento = "CC";

    @NotBlank(message = "El número de documento es obligatorio.")
    @Pattern(regexp = "[A-Za-z0-9-]{4,20}", message = "Usa entre 4 y 20 letras, números o guiones.")
    private String numeroDocumento;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres.")
    private String nombre;

    @Pattern(regexp = "^$|[0-9+ ()-]{7,20}", message = "Ingresa un teléfono válido.")
    private String telefono;

    @Email(message = "Ingresa un correo válido.")
    @Size(max = 120, message = "El correo no puede superar 120 caracteres.")
    private String email;

    @Size(max = 200, message = "La dirección no puede superar 200 caracteres.")
    private String direccion;

    /** Solo se usa al crear: el titular acepto la politica de tratamiento de datos. */
    private boolean autorizaDatos;

    public ClienteService.Datos aDatos() {
        return new ClienteService.Datos(tipoDocumento, numeroDocumento, nombre, telefono, email, direccion);
    }

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
    public void setAutorizaDatos(boolean autorizaDatos) { this.autorizaDatos = autorizaDatos; }
}
