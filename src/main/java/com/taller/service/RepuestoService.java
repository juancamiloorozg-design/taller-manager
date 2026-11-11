package com.taller.service;

import com.taller.exception.RecursoNoEncontradoException;
import com.taller.exception.TallerException;
import com.taller.model.Repuesto;
import com.taller.repository.RepuestoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RepuestoService {

    public record Datos(String codigo, String nombre, BigDecimal precio, int stock, int stockMinimo) {
    }

    private final RepuestoRepository repuestos;
    private final AuditoriaService auditoria;

    public RepuestoService(RepuestoRepository repuestos, AuditoriaService auditoria) {
        this.repuestos = repuestos;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Repuesto> listarTodos() {
        return repuestos.findAllByOrderByNombreAsc();
    }

    /** Repuestos activos con existencias, los unicos que se pueden agregar a una orden. */
    @Transactional(readOnly = true)
    public List<Repuesto> listarDisponibles() {
        return repuestos.findByActivoTrueOrderByNombreAsc().stream()
                .filter(r -> r.getStock() > 0)
                .toList();
    }

    @Transactional(readOnly = true)
    public Repuesto obtener(Long id) {
        return repuestos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un repuesto con id " + id + "."));
    }

    @Transactional
    public Repuesto crear(Datos datos, String actor) {
        if (datos.precio() == null || datos.precio().signum() < 0) {
            throw new TallerException("El precio no puede ser negativo.");
        }
        Repuesto repuesto = new Repuesto(datos.nombre().trim(), datos.precio(), datos.stock());
        repuesto.setCodigo(limpiar(datos.codigo()));
        repuesto.setStockMinimo(datos.stockMinimo());
        repuestos.save(repuesto);
        auditoria.registrar(actor, "CREAR", "Repuesto", repuesto.getId(), repuesto.getNombre());
        return repuesto;
    }

    /** No toca el stock: para eso esta ajustarStock. */
    @Transactional
    public Repuesto actualizar(Long id, Datos datos, String actor) {
        Repuesto repuesto = obtener(id);
        if (datos.precio() == null || datos.precio().signum() < 0) {
            throw new TallerException("El precio no puede ser negativo.");
        }
        repuesto.setCodigo(limpiar(datos.codigo()));
        repuesto.setNombre(datos.nombre().trim());
        repuesto.setPrecio(datos.precio());
        repuesto.setStockMinimo(datos.stockMinimo());
        auditoria.registrar(actor, "EDITAR", "Repuesto", id, repuesto.getNombre());
        return repuesto;
    }

    @Transactional
    public void ajustarStock(Long id, int nuevoStock, String actor) {
        Repuesto repuesto = obtener(id);
        int anterior = repuesto.getStock();
        repuesto.setStock(nuevoStock); // lanza TallerException si es negativo
        auditoria.registrar(actor, "AJUSTAR_STOCK", "Repuesto", id,
                repuesto.getNombre() + ": " + anterior + " a " + nuevoStock);
    }

    @Transactional
    public void cambiarActivo(Long id, String actor) {
        Repuesto repuesto = obtener(id);
        repuesto.setActivo(!repuesto.isActivo());
        auditoria.registrar(actor, repuesto.isActivo() ? "ACTIVAR" : "DESACTIVAR", "Repuesto", id,
                repuesto.getNombre());
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
