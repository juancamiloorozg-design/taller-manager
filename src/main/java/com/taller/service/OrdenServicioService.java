package com.taller.service;

import com.taller.exception.RecursoNoEncontradoException;
import com.taller.exception.TallerException;
import com.taller.model.EstadoOrden;
import com.taller.model.NivelCombustible;
import com.taller.model.OrdenRepuesto;
import com.taller.model.OrdenServicio;
import com.taller.model.Repuesto;
import com.taller.model.Usuario;
import com.taller.model.Vehiculo;
import com.taller.repository.OrdenServicioRepository;
import com.taller.repository.PagoRepository;
import com.taller.repository.RepuestoRepository;
import com.taller.repository.UsuarioRepository;
import com.taller.repository.VehiculoRepository;
import com.taller.util.Tiempo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class OrdenServicioService {

    /** Datos que se capturan al abrir o editar una orden (incluye la recepcion del vehiculo). */
    public record DatosOrden(String descripcion, Integer kilometraje, BigDecimal manoObra,
                             NivelCombustible nivelCombustible, String danosVisibles,
                             String objetosDejados, Long mecanicoId) {
    }

    private static final int TAMANO_PAGINA = 15;
    private static final int DIAS_AVISO = 30;

    private final OrdenServicioRepository ordenes;
    private final VehiculoRepository vehiculos;
    private final RepuestoRepository repuestos;
    private final UsuarioRepository usuarios;
    private final PagoRepository pagos;
    private final AuditoriaService auditoria;

    public OrdenServicioService(OrdenServicioRepository ordenes, VehiculoRepository vehiculos,
                                RepuestoRepository repuestos, UsuarioRepository usuarios,
                                PagoRepository pagos, AuditoriaService auditoria) {
        this.ordenes = ordenes;
        this.vehiculos = vehiculos;
        this.repuestos = repuestos;
        this.usuarios = usuarios;
        this.pagos = pagos;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public Page<OrdenServicio> listar(EstadoOrden estado, int pagina) {
        PageRequest solicitud = PageRequest.of(Math.max(pagina, 0), TAMANO_PAGINA,
                Sort.by(Sort.Direction.DESC, "id"));
        return estado == null ? ordenes.listar(solicitud) : ordenes.listarPorEstado(estado, solicitud);
    }

    @Transactional(readOnly = true)
    public OrdenServicio obtener(Long id) {
        return ordenes.findByIdConDetalle(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una orden con id " + id + "."));
    }

    @Transactional(readOnly = true)
    public List<OrdenServicio> recordatoriosPendientes() {
        return ordenes.recordatoriosPendientes(EstadoOrden.FINALIZADA, Tiempo.ahora().toLocalDate().plusDays(DIAS_AVISO));
    }

    /** Abre una orden. Si es cotizacion queda en COTIZADA hasta que el cliente la apruebe. */
    @Transactional
    public OrdenServicio crear(Long vehiculoId, DatosOrden datos, boolean cotizacion, String actor) {
        Vehiculo vehiculo = vehiculos.findById(vehiculoId)
                .orElseThrow(() -> new TallerException("Selecciona un vehículo válido."));
        OrdenServicio orden = new OrdenServicio(vehiculo, descripcionValida(datos.descripcion()),
                datos.kilometraje(), manoObraValida(datos.manoObra()), actor);
        orden.registrarRecepcion(datos.nivelCombustible(), limpiar(datos.danosVisibles()), limpiar(datos.objetosDejados()));
        orden.setMecanico(mecanico(datos.mecanicoId()));
        if (cotizacion) {
            orden.cambiarEstado(EstadoOrden.COTIZADA);
        }
        ordenes.save(orden);
        auditoria.registrar(actor, cotizacion ? "CREAR_COTIZACION" : "CREAR", "OrdenServicio", orden.getId(),
                "Vehículo " + vehiculoId);
        return orden;
    }

    /**
     * Crea la orden y descuenta los repuestos en una sola transaccion. Los repuestos se procesan
     * en orden de id para que dos ordenes simultaneas no se bloqueen entre si.
     */
    @Transactional
    public OrdenServicio crearConRepuestos(Long vehiculoId, DatosOrden datos, boolean cotizacion,
                                           Map<Long, Integer> cantidades, String actor) {
        OrdenServicio orden = crear(vehiculoId, datos, cotizacion, actor);
        if (cantidades != null) {
            for (Map.Entry<Long, Integer> e : new TreeMap<>(cantidades).entrySet()) {
                if (e.getValue() != null && e.getValue() > 0) {
                    agregarRepuesto(orden.getId(), e.getKey(), e.getValue(), actor);
                }
            }
        }
        return orden;
    }

    @Transactional
    public void actualizarDatos(Long ordenId, DatosOrden datos, String actor) {
        OrdenServicio orden = cargarEditable(ordenId);
        orden.editarDatos(descripcionValida(datos.descripcion()), datos.kilometraje(), manoObraValida(datos.manoObra()));
        orden.registrarRecepcion(datos.nivelCombustible(), limpiar(datos.danosVisibles()), limpiar(datos.objetosDejados()));
        orden.setMecanico(mecanico(datos.mecanicoId()));
        auditoria.registrar(actor, "EDITAR", "OrdenServicio", ordenId, null);
    }

    /** Descuenta del stock y agrega el repuesto a la orden. Si ya estaba, suma la cantidad. */
    @Transactional
    public void agregarRepuesto(Long ordenId, Long repuestoId, int cantidad, String actor) {
        if (cantidad <= 0) {
            throw new TallerException("La cantidad debe ser mayor a cero.");
        }
        OrdenServicio orden = cargarEditable(ordenId);
        Repuesto repuesto = repuestos.findById(repuestoId)
                .orElseThrow(() -> new TallerException("El repuesto seleccionado no existe."));
        if (!repuesto.isActivo()) {
            throw new TallerException("El repuesto '" + repuesto.getNombre() + "' está desactivado.");
        }
        // Una sola instruccion SQL: si no alcanza el stock, devuelve 0 y no cambia nada.
        if (repuestos.descontarStock(repuestoId, cantidad) == 0) {
            throw new TallerException("Stock insuficiente para '" + repuesto.getNombre() + "'. Disponible: "
                    + repuesto.getStock() + ", solicitado: " + cantidad + ".");
        }
        OrdenRepuesto linea = orden.buscarLinea(repuestoId);
        if (linea != null) {
            linea.aumentar(cantidad);
            orden.recalcular();
        } else {
            orden.agregarDetalle(new OrdenRepuesto(repuesto, cantidad));
        }
        auditoria.registrar(actor, "AGREGAR_REPUESTO", "OrdenServicio", ordenId,
                "Repuesto " + repuestoId + " x" + cantidad);
    }

    /** Quita la linea y devuelve las unidades al stock. */
    @Transactional
    public void quitarRepuesto(Long ordenId, Long lineaId, String actor) {
        OrdenServicio orden = cargarEditable(ordenId);
        OrdenRepuesto linea = orden.getDetalle().stream()
                .filter(l -> l.getId().equals(lineaId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("La línea no pertenece a esta orden."));
        repuestos.reponerStock(linea.getRepuesto().getId(), linea.getCantidad());
        orden.quitarDetalle(linea);
        auditoria.registrar(actor, "QUITAR_REPUESTO", "OrdenServicio", ordenId,
                "Repuesto " + linea.getRepuesto().getId() + " x" + linea.getCantidad());
    }

    /** COTIZADA pasa a PENDIENTE (aprobada), PENDIENTE a EN_PROCESO y EN_PROCESO a FINALIZADA. */
    @Transactional
    public void avanzar(Long ordenId, String actor) {
        OrdenServicio orden = obtener(ordenId);
        EstadoOrden anterior = orden.getEstado();
        switch (anterior) {
            case COTIZADA -> orden.aprobar();
            case PENDIENTE -> orden.cambiarEstado(EstadoOrden.EN_PROCESO);
            case EN_PROCESO -> orden.cambiarEstado(EstadoOrden.FINALIZADA);
            default -> throw new TallerException("La orden está " + anterior.getEtiqueta().toLowerCase()
                    + " y no puede avanzar.");
        }
        auditoria.registrar(actor, "CAMBIAR_ESTADO", "OrdenServicio", ordenId,
                anterior.name() + " a " + orden.getEstado().name());
    }

    /** Cancela la orden y devuelve al stock todos sus repuestos. No se puede si ya tiene abonos. */
    @Transactional
    public void cancelar(Long ordenId, String actor) {
        OrdenServicio orden = cargarEditable(ordenId);
        if (pagos.totalDeOrden(ordenId).signum() > 0) {
            throw new TallerException("La orden tiene abonos registrados. Un administrador debe anularlos antes de cancelarla.");
        }
        for (OrdenRepuesto linea : orden.getDetalle()) {
            repuestos.reponerStock(linea.getRepuesto().getId(), linea.getCantidad());
        }
        orden.cambiarEstado(EstadoOrden.CANCELADA);
        auditoria.registrar(actor, "CANCELAR", "OrdenServicio", ordenId, "Stock devuelto");
    }

    /** Define cuando avisarle al cliente su proximo servicio. Solo para ordenes en proceso o finalizadas. */
    @Transactional
    public void programarRecordatorio(Long ordenId, LocalDate fecha, Integer km, String actor) {
        OrdenServicio orden = obtener(ordenId);
        if (orden.getEstado() != EstadoOrden.EN_PROCESO && orden.getEstado() != EstadoOrden.FINALIZADA) {
            throw new TallerException("El próximo servicio se programa cuando la orden está en proceso o finalizada.");
        }
        if (fecha == null) {
            throw new TallerException("Indica la fecha del próximo servicio.");
        }
        if (fecha.isBefore(Tiempo.ahora().toLocalDate())) {
            throw new TallerException("La fecha del próximo servicio no puede ser pasada.");
        }
        if (km != null && km < 0) {
            throw new TallerException("El kilometraje no puede ser negativo.");
        }
        orden.programarRecordatorio(fecha, km);
        auditoria.registrar(actor, "PROGRAMAR_RECORDATORIO", "OrdenServicio", ordenId, "Fecha " + fecha);
    }

    @Transactional
    public void marcarRecordatorioAtendido(Long ordenId, String actor) {
        OrdenServicio orden = obtener(ordenId);
        orden.setRecordatorioAtendido(true);
        auditoria.registrar(actor, "RECORDATORIO_ATENDIDO", "OrdenServicio", ordenId, null);
    }

    private OrdenServicio cargarEditable(Long id) {
        OrdenServicio orden = obtener(id);
        if (orden.getEstado().isTerminal()) {
            throw new TallerException("La orden está " + orden.getEstado().getEtiqueta().toLowerCase()
                    + " y ya no se puede modificar.");
        }
        return orden;
    }

    private Usuario mecanico(Long id) {
        if (id == null) {
            return null;
        }
        Usuario u = usuarios.findById(id)
                .orElseThrow(() -> new TallerException("El mecánico seleccionado no existe."));
        if (!u.isActivo()) {
            throw new TallerException("El usuario seleccionado está inactivo.");
        }
        return u;
    }

    private static String descripcionValida(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new TallerException("La descripción es obligatoria.");
        }
        String texto = descripcion.trim();
        if (texto.length() > 500) {
            throw new TallerException("La descripción no puede superar 500 caracteres.");
        }
        return texto;
    }

    private static BigDecimal manoObraValida(BigDecimal manoObra) {
        if (manoObra == null) {
            return BigDecimal.ZERO;
        }
        if (manoObra.signum() < 0) {
            throw new TallerException("La mano de obra no puede ser negativa.");
        }
        return manoObra;
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
