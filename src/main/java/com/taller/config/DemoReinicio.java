package com.taller.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Solo en la demostracion publica: cada cierto tiempo borra todo lo que los visitantes hayan creado
 * y vuelve a cargar los datos de ejemplo.
 */
@Component
@ConditionalOnProperty(name = "taller.demo.reinicio-horas")
public class DemoReinicio {

    private static final Logger log = LoggerFactory.getLogger(DemoReinicio.class);
    /** Orden que respeta las llaves foraneas: primero las tablas que dependen de otras. */
    private static final List<String> TABLAS = List.of(
            "orden_fotos", "pagos", "orden_repuestos", "ordenes_servicio",
            "vehiculos", "clientes", "repuestos", "auditoria", "usuarios");

    private final JdbcTemplate jdbc;
    private final DataInitializer administrador;
    private final DemoDataLoader datos;
    private final Path fotos;

    public DemoReinicio(JdbcTemplate jdbc, DataInitializer administrador, DemoDataLoader datos,
                        @Value("${taller.fotos.dir:datos/fotos}") String fotos) {
        this.jdbc = jdbc;
        this.administrador = administrador;
        this.datos = datos;
        this.fotos = Path.of(fotos).toAbsolutePath().normalize();
    }

    @Scheduled(fixedRateString = "#{${taller.demo.reinicio-horas} * 3600000}",
            initialDelayString = "#{${taller.demo.reinicio-horas} * 3600000}")
    public synchronized void reiniciar() {
        log.info("Reiniciando los datos de la demostración");
        for (String tabla : TABLAS) {
            jdbc.update("DELETE FROM " + tabla);
        }
        borrarFotos();
        administrador.run((ApplicationArguments) null);
        datos.sembrar();
    }

    private void borrarFotos() {
        if (!Files.isDirectory(fotos)) {
            return;
        }
        try (Stream<Path> ruta = Files.walk(fotos)) {
            ruta.sorted(Comparator.reverseOrder()).filter(p -> !p.equals(fotos)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    log.warn("No se pudo borrar {}", p);
                }
            });
        } catch (IOException e) {
            log.warn("No se pudieron borrar las fotos de la demostración", e);
        }
    }
}
