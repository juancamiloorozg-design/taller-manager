package com.taller.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrdenServicioTest {

    private OrdenServicio nuevaOrden(BigDecimal manoObra) {
        Cliente cliente = new Cliente("CC", "123456", "Cliente de prueba");
        Vehiculo vehiculo = new Vehiculo("ABC123", "Mazda", "3", 2018, cliente);
        return new OrdenServicio(vehiculo, "Cambio de aceite", 50000, manoObra, "test");
    }

    @Test
    void totalSumaManoDeObraYRepuestos() {
        OrdenServicio orden = nuevaOrden(new BigDecimal("60000"));
        orden.agregarDetalle(new OrdenRepuesto(new Repuesto("Filtro", new BigDecimal("28000"), 10), 2));
        orden.agregarDetalle(new OrdenRepuesto(new Repuesto("Aceite", new BigDecimal("42000"), 10), 4));

        assertEquals(0, new BigDecimal("284000").compareTo(orden.getTotal()));
        assertEquals(0, new BigDecimal("224000").compareTo(orden.getTotalRepuestos()));
    }

    @Test
    void quitarUnaLineaRecalculaElTotal() {
        OrdenServicio orden = nuevaOrden(BigDecimal.ZERO);
        OrdenRepuesto linea = new OrdenRepuesto(new Repuesto("Filtro", new BigDecimal("28000"), 10), 2);
        orden.agregarDetalle(linea);
        orden.quitarDetalle(linea);

        assertEquals(0, BigDecimal.ZERO.compareTo(orden.getTotal()));
    }

    @Test
    void manoDeObraNulaSeTomaComoCero() {
        OrdenServicio orden = nuevaOrden(null);
        assertEquals(0, BigDecimal.ZERO.compareTo(orden.getTotal()));
    }

    @Test
    void finalizarGuardaLaFecha() {
        OrdenServicio orden = nuevaOrden(BigDecimal.ZERO);
        orden.cambiarEstado(EstadoOrden.FINALIZADA);

        assertNotNull(orden.getFinalizadoEn());
        assertTrue(orden.getEstado().isTerminal());
    }

    @Test
    void elPrecioDeLaLineaQuedaCongelado() {
        Repuesto filtro = new Repuesto("Filtro", new BigDecimal("28000"), 10);
        OrdenRepuesto linea = new OrdenRepuesto(filtro, 1);
        filtro.setPrecio(new BigDecimal("99000"));

        assertEquals(0, new BigDecimal("28000").compareTo(linea.getPrecioUnitario()));
    }

    @Test
    void aprobarUnaCotizacionLaPasaAPendiente() {
        OrdenServicio orden = nuevaOrden(BigDecimal.ZERO);
        orden.cambiarEstado(EstadoOrden.COTIZADA);

        orden.aprobar();

        assertEquals(EstadoOrden.PENDIENTE, orden.getEstado());
        assertNotNull(orden.getAprobadaEn());
    }

    @Test
    void reprogramarElRecordatorioLoVuelveAPendiente() {
        OrdenServicio orden = nuevaOrden(BigDecimal.ZERO);
        orden.setRecordatorioAtendido(true);

        orden.programarRecordatorio(java.time.LocalDate.of(2027, 2, 1), 70000);

        assertEquals(false, orden.isRecordatorioAtendido());
        assertEquals(70000, orden.getProximoKm());
    }
}
