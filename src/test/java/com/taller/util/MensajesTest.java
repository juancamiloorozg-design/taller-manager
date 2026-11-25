package com.taller.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MensajesTest {

    @Test
    void agregaElIndicativoDeColombia() {
        assertEquals("573001234567", Mensajes.celular("300 123 4567"));
        assertEquals("573001234567", Mensajes.celular("+57 (300) 123-4567"));
    }

    @Test
    void rechazaNumerosQueNoSonCelularColombiano() {
        assertNull(Mensajes.celular(null));
        assertNull(Mensajes.celular("6011234567"));
        assertNull(Mensajes.celular("12345"));
        assertNull(Mensajes.celular("+1 305 555 0100"));
    }
}
