package com.taller.model;

import com.taller.exception.TallerException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepuestoTest {

    @Test
    void noAceptaStockNegativo() {
        assertThrows(TallerException.class, () -> new Repuesto("Filtro", BigDecimal.TEN, -1));
        Repuesto r = new Repuesto("Filtro", BigDecimal.TEN, 5);
        assertThrows(TallerException.class, () -> r.setStock(-3));
    }

    @Test
    void avisaStockBajoCuandoLlegaAlMinimo() {
        Repuesto r = new Repuesto("Filtro", BigDecimal.TEN, 5);
        r.setStockMinimo(5);
        assertTrue(r.isStockBajo());
        r.setStock(6);
        assertFalse(r.isStockBajo());
    }
}
