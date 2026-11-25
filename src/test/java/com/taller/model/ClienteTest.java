package com.taller.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteTest {

    @Test
    void registrarAutorizacionDejaConstancia() {
        Cliente c = new Cliente("CC", "123456", "Ana");
        c.registrarAutorizacion("2026-11", "mecanico@taller.com");

        assertTrue(c.isAutorizaDatos());
        assertNotNull(c.getFechaAutorizacion());
        assertEquals("2026-11", c.getVersionPolitica());
        assertEquals("mecanico@taller.com", c.getRegistradoPor());
    }

    @Test
    void anonimizarBorraLosDatosPersonales() {
        Cliente c = new Cliente("CC", "123456", "Ana");
        c.setTelefono("3001234567");
        c.setEmail("ana@example.com");
        c.setDireccion("Calle 1");
        c.registrarAutorizacion("2026-11", "x");

        c.anonimizar();

        assertTrue(c.isAnonimizado());
        assertEquals(Cliente.NOMBRE_ANONIMO, c.getNombre());
        assertNull(c.getTelefono());
        assertNull(c.getEmail());
        assertNull(c.getDireccion());
        assertFalse(c.isAutorizaDatos());
        assertFalse(c.getNumeroDocumento().contains("123456"));
    }
}
