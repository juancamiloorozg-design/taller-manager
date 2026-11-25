package com.taller.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PasswordPolicyTest {

    @Test
    void rechazaClavesCortasOSinNumeros() {
        assertNotNull(PasswordPolicy.validar("corta1"));
        assertNotNull(PasswordPolicy.validar("soloLetrasLargas"));
        assertNotNull(PasswordPolicy.validar("12345678901"));
    }

    @Test
    void aceptaUnaClaveValida() {
        assertNull(PasswordPolicy.validar("Taller2026ok"));
    }
}
