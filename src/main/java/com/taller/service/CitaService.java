package com.taller.service;

import com.taller.exception.RecursoNoEncontradoException;
import com.taller.exception.TallerException;
import com.taller.model.Cita;
import com.taller.repository.CitaRepository;
import com.taller.repository.VehiculoRepository;
import com.taller.util.Tiempo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CitaService {

    private final CitaRepository citas;
    private final VehiculoRepository vehiculos;
    private final OrdenServicioService ordenes;
    private final AuditoriaService auditoria;

    public CitaService(CitaRepository citas, VehiculoRepository vehiculos, OrdenServicioService ordenes,
                       AuditoriaService auditoria) {
        this.citas = citas;
        this.vehiculos = vehiculos;
        this.ordenes = ordenes;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Cita> semana(LocalDate lunes) {
        return citas.enSemana(lunes.atStartOfDay(), lunes.plusWeeks(1).atStartOfDay());
    }

    @Transactional(readOnly = true)
    public Cita obtener(Long id) {
        return citas.conDetalle(id).orElseThrow(() -> noEncontrada(id));
    }

    @Transactional
    public Cita agendar(Long vehiculoId, LocalDateTime fechaHora, String motivo, String actor) {
        if (fechaHora == null || fechaHora.isBefore(Tiempo.ahora().withSecond(0).withNano(0))) {
            throw new TallerException("Selecciona una fecha y hora actual o futura.");
        }
        if (motivo == null || motivo.isBlank() || motivo.length() > 500) {
            throw new TallerException("Escribe un motivo de hasta 500 caracteres.");
        }
        var vehiculo = vehiculos.findByIdConCliente(vehiculoId)
                .filter(v -> !v.getCliente().isAnonimizado())
                .orElseThrow(() -> new TallerException("Selecciona un vehículo de un cliente activo."));
        Cita cita = citas.save(new Cita(vehiculo, fechaHora, motivo.strip()));
        auditoria.registrar(actor, "AGENDAR", "Cita", cita.getId(), null);
        return cita;
    }

    @Transactional
    public LocalDate cancelar(Long id, String actor) {
        Cita cita = bloquear(id);
        if (cita.getOrden() != null) {
            throw new TallerException("La cita ya tiene una orden vinculada.");
        }
        if (!cita.isCancelada()) {
            cita.cancelar();
            auditoria.registrar(actor, "CANCELAR", "Cita", id, null);
        }
        return cita.getFechaHora().toLocalDate();
    }

    @Transactional
    public Long abrirOrden(Long id, OrdenServicioService.DatosOrden datos, boolean cotizacion,
                           Map<Long, Integer> cantidades, String actor) {
        Cita cita = bloquear(id);
        if (cita.isCancelada()) {
            throw new TallerException("La cita está cancelada.");
        }
        if (cita.getOrden() != null) {
            return cita.getOrden().getId();
        }
        var orden = ordenes.crearConRepuestos(cita.getVehiculo().getId(), datos, cotizacion, cantidades, actor);
        cita.vincularOrden(orden);
        auditoria.registrar(actor, "VINCULAR_ORDEN", "Cita", id, "Orden " + orden.getId());
        return orden.getId();
    }

    private Cita bloquear(Long id) {
        return citas.paraActualizar(id).orElseThrow(() -> noEncontrada(id));
    }

    private RecursoNoEncontradoException noEncontrada(Long id) {
        return new RecursoNoEncontradoException("No existe una cita con id " + id + ".");
    }
}
