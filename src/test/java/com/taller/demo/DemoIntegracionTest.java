package com.taller.demo;

import com.taller.config.DemoReinicio;
import com.taller.model.EstadoOrden;
import com.taller.repository.ClienteRepository;
import com.taller.repository.OrdenServicioRepository;
import com.taller.repository.UsuarioRepository;
import com.taller.service.DashboardService;
import com.taller.service.RespaldoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "taller.respaldos.dir=target/prueba-respaldos",
        "taller.fotos.dir=target/prueba-fotos"})
@ActiveProfiles("demo")
class DemoIntegracionTest {

    @Autowired ClienteRepository clientes;
    @Autowired OrdenServicioRepository ordenes;
    @Autowired UsuarioRepository usuarios;
    @Autowired DashboardService dashboard;
    @Autowired RespaldoService respaldos;
    @Autowired DemoReinicio reinicio;

    @Test
    void cargaLosDatosDeEjemploEnTodosLosEstados() {
        assertEquals(8, clientes.count());
        assertEquals(3, usuarios.count());
        var abiertas = dashboard.ordenesAbiertas();
        assertEquals(2, abiertas.get(EstadoOrden.COTIZADA).size());
        assertEquals(2, abiertas.get(EstadoOrden.PENDIENTE).size());
        assertEquals(2, abiertas.get(EstadoOrden.EN_PROCESO).size());
        // Dos ya vencidos y uno que vence dentro de la ventana de aviso
        assertEquals(3, dashboard.recordatoriosPendientes());
        assertTrue(dashboard.porCobrar().signum() > 0);
    }

    @Test
    void elRespaldoTieneTodasLasTablasYNoLlevaLasClaves() throws Exception {
        var r = respaldos.crear();
        try (ZipFile zip = new ZipFile(respaldos.ruta(r.nombre()).toFile())) {
            ZipEntry entrada = zip.getEntry("datos/usuarios.csv");
            assertNotNull(entrada);
            String texto;
            try (InputStream in = zip.getInputStream(entrada)) {
                texto = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            assertTrue(texto.contains("admin@taller.local"));
            assertFalse(texto.toUpperCase().contains("PASSWORD_HASH"));
            assertNotNull(zip.getEntry("datos/ordenes_servicio.csv"));
        }
    }

    @Test
    void soloSeAbrenRespaldosConNombreValido() {
        assertThrows(RuntimeException.class, () -> respaldos.ruta("../application.properties"));
        assertThrows(RuntimeException.class, () -> respaldos.ruta("respaldo-1.zip"));
    }

    @Test
    void elReinicioBorraLoQueHayaCreadoUnVisitanteYVuelveALosDatosDeEjemplo() {
        long antes = ordenes.count();
        reinicio.reiniciar();
        assertEquals(antes, ordenes.count());
        assertEquals(8, clientes.count());
        assertEquals(3, usuarios.count());
    }
}
