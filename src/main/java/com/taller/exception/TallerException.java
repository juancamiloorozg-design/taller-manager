package com.taller.exception;

/**
 * Error de regla de negocio (stock insuficiente, placa repetida, etc.).
 * Es una excepcion sin chequeo para que Spring revierta la transaccion automaticamente.
 */
public class TallerException extends RuntimeException {

    public TallerException(String mensaje) {
        super(mensaje);
    }

    public TallerException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
