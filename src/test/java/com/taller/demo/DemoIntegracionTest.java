package com.taller.demo;

import com.taller.config.DemoReinicio;
import com.taller.model.EstadoOrden;
import com.taller.repository.CitaRepository;
import com.taller.repository.ClienteRepository;
import com.taller.repository.OrdenServicioRepository;
import com.taller.repository.UsuarioRepository;
import com.taller.service.DashboardService;
import com.taller.service.RespaldoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import com.taller.util.Tiempo;
import java.time.format.DateTimeFormatter;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
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
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DemoIntegracionTest {

    @Autowired ClienteRepository clientes;
    @Autowired CitaRepository citas;
    @Autowired MockMvc mvc;
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
            assertNotNull(zip.getEntry("datos/citas.csv"));
        }
    }

    @Test
    void elMecanicoAgendaUnaCitaYLaConvierteUnaSolaVez() throws Exception {
        mvc.perform(get("/citas")).andExpect(status().is3xxRedirection());
        var ingreso = mvc.perform(formLogin().user("mecanico@taller.local").password("Mecanico12345!"))
                .andExpect(status().is3xxRedirection()).andReturn();
        var sesion = (MockHttpSession) ingreso.getRequest().getSession(false);
        assertNotNull(sesion);
        var vehiculoId = citas.conDetalle(citas.findAll().get(0).getId()).orElseThrow().getVehiculo().getId();
        var fecha = Tiempo.ahora().plusDays(2).withHour(10).withMinute(30);
        var fechaInput = fecha.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

        mvc.perform(get("/citas/nueva").session(sesion)).andExpect(status().isOk());
        mvc.perform(post("/citas").session(sesion).with(csrf()).param("vehiculoId", vehiculoId.toString())
                        .param("fechaHora", fechaInput).param("motivo", "Diagnóstico de vibración"))
                .andExpect(redirectedUrlPattern("/citas?fecha=*"));
        var cita = citas.enSemana(fecha.toLocalDate().atStartOfDay(), fecha.toLocalDate().plusDays(1).atStartOfDay())
                .stream().filter(c -> c.getMotivo().equals("Diagnóstico de vibración")).findFirst().orElseThrow();
        mvc.perform(get("/citas").param("fecha", fecha.toLocalDate().toString()).session(sesion))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Diagnóstico de vibración")));
        mvc.perform(get("/ordenes/nueva").param("citaId", cita.getId().toString()).session(sesion))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Vehículo de la cita")))
                .andExpect(content().string(containsString("Diagn&oacute;stico de vibraci&oacute;n")));
        var ordenesAntes = ordenes.count();
        try {
            var primera = mvc.perform(post("/ordenes").session(sesion).with(csrf())
                            .param("citaId", cita.getId().toString()).param("descripcion", cita.getMotivo())
                            .param("manoObra", "50000"))
                    .andExpect(redirectedUrlPattern("/ordenes/*")).andReturn().getResponse().getRedirectedUrl();
            mvc.perform(post("/ordenes").session(sesion).with(csrf())
                            .param("citaId", cita.getId().toString()).param("descripcion", cita.getMotivo())
                            .param("manoObra", "50000"))
                    .andExpect(redirectedUrl(primera));
            assertEquals(ordenesAntes + 1, ordenes.count());
            assertEquals("/ordenes/" + citas.conDetalle(cita.getId()).orElseThrow().getOrden().getId(), primera);
        } finally {
            reinicio.reiniciar();
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
        long citasAntes = citas.count();
        reinicio.reiniciar();
        assertEquals(antes, ordenes.count());
        assertEquals(citasAntes, citas.count());
        assertEquals(8, clientes.count());
        assertEquals(3, usuarios.count());
    }
}
