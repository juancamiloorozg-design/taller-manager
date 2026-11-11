package com.taller.service;

import com.taller.exception.RecursoNoEncontradoException;
import com.taller.exception.TallerException;
import com.taller.model.OrdenFoto;
import com.taller.model.OrdenServicio;
import com.taller.repository.OrdenFotoRepository;
import com.taller.repository.OrdenServicioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Fotos de la recepcion. Los archivos se guardan fuera de la carpeta publica, con nombre aleatorio,
 * y solo se entregan a usuarios autenticados. El tipo se verifica por el contenido, no por el nombre.
 */
@Service
public class FotoService {

    public record Archivo(Resource recurso, String tipo) {
    }

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int MAX_POR_ORDEN = 10;

    private final OrdenFotoRepository fotos;
    private final OrdenServicioRepository ordenes;
    private final AuditoriaService auditoria;
    private final Path directorio;

    public FotoService(OrdenFotoRepository fotos, OrdenServicioRepository ordenes, AuditoriaService auditoria,
                       @Value("${taller.fotos.dir:datos/fotos}") String directorio) {
        this.fotos = fotos;
        this.ordenes = ordenes;
        this.auditoria = auditoria;
        this.directorio = Path.of(directorio).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<OrdenFoto> deOrden(Long ordenId) {
        return fotos.findByOrdenIdOrderByIdAsc(ordenId);
    }

    @Transactional
    public OrdenFoto subir(Long ordenId, MultipartFile archivo, String actor) {
        OrdenServicio orden = ordenes.findById(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una orden con id " + ordenId + "."));
        if (archivo == null || archivo.isEmpty()) {
            throw new TallerException("Selecciona una foto.");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new TallerException("La foto supera los 5 MB.");
        }
        if (fotos.countByOrdenId(ordenId) >= MAX_POR_ORDEN) {
            throw new TallerException("Una orden admite máximo " + MAX_POR_ORDEN + " fotos.");
        }
        try {
            byte[] bytes = archivo.getBytes();
            String[] tipo = detectar(bytes);
            if (tipo == null) {
                throw new TallerException("Solo se aceptan fotos JPG, PNG o WEBP.");
            }
            Files.createDirectories(directorio);
            String nombre = UUID.randomUUID() + "." + tipo[1];
            Files.write(directorio.resolve(nombre), bytes);
            OrdenFoto foto = fotos.save(new OrdenFoto(orden, nombre, tipo[0], actor));
            auditoria.registrar(actor, "SUBIR_FOTO", "OrdenServicio", ordenId, null);
            return foto;
        } catch (IOException e) {
            throw new TallerException("No se pudo guardar la foto. Intenta de nuevo.");
        }
    }

    @Transactional(readOnly = true)
    public Archivo cargar(Long ordenId, Long fotoId) {
        OrdenFoto foto = fotos.findByIdAndOrdenId(fotoId, ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La foto no existe."));
        Path ruta = directorio.resolve(foto.getNombreArchivo()).normalize();
        if (!ruta.startsWith(directorio) || !Files.isRegularFile(ruta)) {
            throw new RecursoNoEncontradoException("El archivo de la foto no está disponible.");
        }
        return new Archivo(new FileSystemResource(ruta), foto.getTipoContenido());
    }

    @Transactional
    public void eliminar(Long ordenId, Long fotoId, String actor) {
        OrdenFoto foto = fotos.findByIdAndOrdenId(fotoId, ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La foto no existe."));
        fotos.delete(foto);
        try {
            Path ruta = directorio.resolve(foto.getNombreArchivo()).normalize();
            if (ruta.startsWith(directorio)) {
                Files.deleteIfExists(ruta);
            }
        } catch (IOException e) {
            // El registro ya se borro; un archivo suelto no afecta al sistema.
        }
        auditoria.registrar(actor, "ELIMINAR_FOTO", "OrdenServicio", ordenId, null);
    }

    /** Devuelve {tipo, extension} segun los primeros bytes, o null si no es una imagen permitida. */
    private static String[] detectar(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return new String[]{"image/jpeg", "jpg"};
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return new String[]{"image/png", "png"};
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return new String[]{"image/webp", "webp"};
        }
        return null;
    }
}
