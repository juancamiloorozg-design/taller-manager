package com.taller.service;

import com.taller.exception.RecursoNoEncontradoException;
import com.taller.exception.TallerException;
import com.taller.model.EstadoOrden;
import com.taller.model.MetodoPago;
import com.taller.model.OrdenServicio;
import com.taller.model.Pago;
import com.taller.repository.OrdenServicioRepository;
import com.taller.repository.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PagoService {

    private final PagoRepository pagos;
    private final OrdenServicioRepository ordenes;
    private final AuditoriaService auditoria;

    public PagoService(PagoRepository pagos, OrdenServicioRepository ordenes, AuditoriaService auditoria) {
        this.pagos = pagos;
        this.ordenes = ordenes;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Pago> deOrden(Long ordenId) {
        return pagos.findByOrdenIdOrderByIdAsc(ordenId);
    }

    @Transactional(readOnly = true)
    public BigDecimal abonado(Long ordenId) {
        return pagos.totalDeOrden(ordenId);
    }

    /** Registra un abono. No puede superar lo que falta por pagar. */
    @Transactional
    public Pago registrar(Long ordenId, BigDecimal monto, MetodoPago metodo, String nota, String actor) {
        OrdenServicio orden = ordenes.findById(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una orden con id " + ordenId + "."));
        if (orden.getEstado() == EstadoOrden.CANCELADA) {
            throw new TallerException("No se pueden registrar abonos en una orden cancelada.");
        }
        if (monto == null || monto.signum() <= 0) {
            throw new TallerException("El valor del abono debe ser mayor a cero.");
        }
        if (metodo == null) {
            throw new TallerException("Selecciona el medio de pago.");
        }
        BigDecimal saldo = orden.getTotal().subtract(pagos.totalDeOrden(ordenId));
        if (monto.compareTo(saldo) > 0) {
            throw new TallerException("El abono supera lo que falta por pagar ("
                    + saldo.max(BigDecimal.ZERO).toPlainString() + ").");
        }
        String texto = nota == null || nota.isBlank() ? null : nota.trim();
        if (texto != null && texto.length() > 200) {
            throw new TallerException("La nota no puede superar 200 caracteres.");
        }
        Pago pago = pagos.save(new Pago(orden, monto, metodo, texto, actor));
        auditoria.registrar(actor, "REGISTRAR_ABONO", "OrdenServicio", ordenId, metodo.name() + " " + monto.toPlainString());
        return pago;
    }

    @Transactional
    public Long anular(Long ordenId, Long pagoId, String actor) {
        Pago pago = pagos.findById(pagoId)
                .filter(p -> p.getOrden().getId().equals(ordenId))
                .orElseThrow(() -> new RecursoNoEncontradoException("El abono no pertenece a esta orden."));
        pagos.delete(pago);
        auditoria.registrar(actor, "ANULAR_ABONO", "OrdenServicio", ordenId, pago.getMetodo().name() + " " + pago.getMonto().toPlainString());
        return ordenId;
    }
}
