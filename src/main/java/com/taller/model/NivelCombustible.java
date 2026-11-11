package com.taller.model;

public enum NivelCombustible {
    VACIO("Reserva"),
    UN_CUARTO("1/4"),
    MEDIO("1/2"),
    TRES_CUARTOS("3/4"),
    LLENO("Lleno");

    private final String etiqueta;

    NivelCombustible(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
