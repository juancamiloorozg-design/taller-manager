package com.taller.model;

public enum EstadoOrden {
    COTIZADA("Cotizada", "primary"),
    PENDIENTE("Pendiente", "warning"),
    EN_PROCESO("En proceso", "info"),
    FINALIZADA("Finalizada", "success"),
    CANCELADA("Cancelada", "secondary");

    private final String etiqueta;
    private final String color;

    EstadoOrden(String etiqueta, String color) {
        this.etiqueta = etiqueta;
        this.color = color;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Sufijo de clase Bootstrap para la insignia (bg-warning, bg-info, ...). */
    public String getColor() {
        return color;
    }

    public boolean isTerminal() {
        return this == FINALIZADA || this == CANCELADA;
    }
}
