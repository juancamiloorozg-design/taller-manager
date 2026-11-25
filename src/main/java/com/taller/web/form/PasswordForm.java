package com.taller.web.form;

import jakarta.validation.constraints.NotBlank;

public class PasswordForm {

    @NotBlank(message = "Ingresa tu contraseña actual.")
    private String actual;

    @NotBlank(message = "Ingresa la nueva contraseña.")
    private String nueva;

    @NotBlank(message = "Repite la nueva contraseña.")
    private String confirmacion;

    public String getActual() { return actual; }
    public void setActual(String actual) { this.actual = actual; }

    public String getNueva() { return nueva; }
    public void setNueva(String nueva) { this.nueva = nueva; }

    public String getConfirmacion() { return confirmacion; }
    public void setConfirmacion(String confirmacion) { this.confirmacion = confirmacion; }
}
