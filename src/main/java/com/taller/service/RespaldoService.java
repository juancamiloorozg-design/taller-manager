package com.taller.service;

import com.taller.exception.RecursoNoEncontradoException;
import com.taller.util.Tiempo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSetMetaData;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Respaldo diario: un zip con cada tabla en CSV y las fotos de recepcion. Es una copia de los datos
 * para poder recuperarlos, no un volcado de la base (para eso esta mysqldump, ver el README).
 * El hash de las claves de los usuarios no se incluye.
 */
@Service
public class RespaldoService {

    public record Respaldo(String nombre, long bytes, LocalDateTime fecha) {
    }

    private static final Logger LOG = LoggerFactory.getLogger(RespaldoService.class);
    private static final Pattern NOMBRE = Pattern.compile("respaldo-\\d{8}-\\d{6}\\.zip");
    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final List<String> TABLAS = List.of(
            "usuarios", "clientes", "vehiculos", "repuestos", "ordenes_servicio",
            "orden_repuestos", "pagos", "orden_fotos", "auditoria");

    private final JdbcTemplate jdbc;
    private final AuditoriaService auditoria;
    private final Path carpeta;
    private final Path fotos;
    private final int conservar;

    public RespaldoService(JdbcTemplate jdbc, AuditoriaService auditoria,
                           @Value("${taller.respaldos.dir:datos/respaldos}") String carpeta,
                           @Value("${taller.fotos.dir:datos/fotos}") String fotos,
                           @Value("${taller.respaldos.conservar:14}") int conservar) {
        this.jdbc = jdbc;
        this.auditoria = auditoria;
        this.carpeta = Path.of(carpeta).toAbsolutePath().normalize();
        this.fotos = Path.of(fotos).toAbsolutePath().normalize();
        this.conservar = Math.max(1, conservar);
    }

    @Scheduled(cron = "${taller.respaldos.cron:0 0 2 * * *}", zone = "America/Bogota")
    public void respaldoDiario() {
        try {
            Respaldo r = crear();
            auditoria.registrar("sistema", "RESPALDO", "respaldo", null, r.nombre());
        } catch (RuntimeException e) {
            LOG.error("No se pudo crear el respaldo diario", e);
        }
    }

    public synchronized Respaldo crear() {
        try {
            Files.createDirectories(carpeta);
            String nombre = "respaldo-" + Tiempo.ahora().format(SELLO) + ".zip";
            Path destino = carpeta.resolve(nombre);
            Path temporal = carpeta.resolve(nombre + ".parcial");
            try (OutputStream out = Files.newOutputStream(temporal); ZipOutputStream zip = new ZipOutputStream(out)) {
                for (String tabla : TABLAS) {
                    zip.putNextEntry(new ZipEntry("datos/" + tabla + ".csv"));
                    zip.write(csv(tabla));
                    zip.closeEntry();
                }
                copiarFotos(zip);
            }
            Files.move(temporal, destino);
            limpiarAntiguos();
            return describir(destino);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<Respaldo> listar() {
        if (!Files.isDirectory(carpeta)) {
            return List.of();
        }
        try (Stream<Path> archivos = Files.list(carpeta)) {
            List<Respaldo> lista = new ArrayList<>();
            for (Path p : (Iterable<Path>) archivos.filter(a -> NOMBRE.matcher(a.getFileName().toString()).matches())::iterator) {
                lista.add(describir(p));
            }
            lista.sort(Comparator.comparing(Respaldo::nombre).reversed());
            return lista;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Ruta de un respaldo existente. El nombre se valida contra el patron para impedir rutas arbitrarias. */
    public Path ruta(String nombre) {
        if (nombre == null || !NOMBRE.matcher(nombre).matches()) {
            throw new RecursoNoEncontradoException("Respaldo no encontrado");
        }
        Path p = carpeta.resolve(nombre).normalize();
        if (!p.startsWith(carpeta) || !Files.isRegularFile(p)) {
            throw new RecursoNoEncontradoException("Respaldo no encontrado");
        }
        return p;
    }

    private byte[] csv(String tabla) {
        // Los nombres de tabla salen de la lista fija de arriba, nunca de la peticion.
        return jdbc.query("SELECT * FROM " + tabla, rs -> {
            StringBuilder sb = new StringBuilder("﻿");
            ResultSetMetaData meta = rs.getMetaData();
            int n = meta.getColumnCount();
            List<Integer> columnas = new ArrayList<>();
            for (int i = 1; i <= n; i++) {
                String nombre = meta.getColumnLabel(i);
                if (tabla.equals("usuarios") && nombre.equalsIgnoreCase("password_hash")) {
                    continue;
                }
                columnas.add(i);
                sb.append(columnas.size() > 1 ? ";" : "").append(celda(nombre));
            }
            sb.append("\r\n");
            while (rs.next()) {
                boolean primera = true;
                for (int i : columnas) {
                    sb.append(primera ? "" : ";").append(celda(rs.getObject(i)));
                    primera = false;
                }
                sb.append("\r\n");
            }
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        });
    }

    private static String celda(Object valor) {
        if (valor == null) {
            return "";
        }
        String t = valor.toString();
        if (t.contains(";") || t.contains("\"") || t.contains("\n") || t.contains("\r")) {
            return "\"" + t.replace("\"", "\"\"") + "\"";
        }
        return t;
    }

    private void copiarFotos(ZipOutputStream zip) throws IOException {
        if (!Files.isDirectory(fotos)) {
            return;
        }
        try (Stream<Path> archivos = Files.walk(fotos)) {
            for (Path p : (Iterable<Path>) archivos.filter(Files::isRegularFile)::iterator) {
                zip.putNextEntry(new ZipEntry("fotos/" + fotos.relativize(p).toString().replace('\\', '/')));
                Files.copy(p, zip);
                zip.closeEntry();
            }
        }
    }

    private void limpiarAntiguos() throws IOException {
        List<Respaldo> todos = listar();
        for (int i = conservar; i < todos.size(); i++) {
            Files.deleteIfExists(carpeta.resolve(todos.get(i).nombre()));
        }
    }

    private Respaldo describir(Path p) throws IOException {
        LocalDateTime fecha = LocalDateTime.parse(
                p.getFileName().toString().substring(9, 24), SELLO);
        return new Respaldo(p.getFileName().toString(), Files.size(p), fecha);
    }
}
