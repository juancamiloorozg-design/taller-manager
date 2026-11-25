package com.taller.model;

public enum Rol {
    ADMIN("Administrador"),
    MECANICO("Mecánico");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
