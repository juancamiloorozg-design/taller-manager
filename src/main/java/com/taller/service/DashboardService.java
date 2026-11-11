package com.taller.service;

import com.taller.model.EstadoOrden;
import com.taller.model.OrdenServicio;
import com.taller.model.Repuesto;
import com.taller.repository.ClienteRepository;
import com.taller.repository.OrdenServicioRepository;
import com.taller.repository.RepuestoRepository;
import com.taller.repository.VehiculoRepository;
import com.taller.util.Tiempo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final OrdenServicioRepository ordenes;
    private final ClienteRepository clientes;
    private final VehiculoRepository vehiculos;
    private final RepuestoRepository repuestos;
    private final OrdenServicioService ordenService;
    private final InformeService informes;

    public DashboardService(OrdenServicioRepository ordenes, ClienteRepository clientes,
                            VehiculoRepository vehiculos, RepuestoRepository repuestos,
                            OrdenServicioService ordenService, InformeService informes) {
        this.ordenes = ordenes;
        this.clientes = clientes;
        this.vehiculos = vehiculos;
        this.repuestos = repuestos;
        this.ordenService = ordenService;
        this.informes = informes;
    }

    public int recordatoriosPendientes() {
        return ordenService.recordatoriosPendientes().size();
    }

    public BigDecimal porCobrar() {
        return informes.totalPorCobrar();
    }

    public long contarOrdenes(EstadoOrden estado) {
        return ordenes.countByEstado(estado);
    }

    /** Suma del total de las ordenes finalizadas desde el primer dia del mes actual. */
    public BigDecimal finalizadoEnElMes() {
        LocalDateTime inicioMes = Tiempo.ahora().toLocalDate().withDayOfMonth(1).atStartOfDay();
        return ordenes.sumaTotalDesde(EstadoOrden.FINALIZADA, inicioMes);
    }

    public long totalClientes() {
        return clientes.count();
    }

    public long totalVehiculos() {
        return vehiculos.count();
    }

    public List<Repuesto> stockBajo() {
        return repuestos.findStockBajo();
    }

    /** Ordenes que siguen abiertas, agrupadas por estado, para el tablero del panel. */
    public Map<EstadoOrden, List<OrdenServicio>> ordenesAbiertas() {
        Map<EstadoOrden, List<OrdenServicio>> porEstado = new EnumMap<>(EstadoOrden.class);
        for (EstadoOrden estado : List.of(EstadoOrden.COTIZADA, EstadoOrden.PENDIENTE, EstadoOrden.EN_PROCESO)) {
            porEstado.put(estado, new java.util.ArrayList<>());
        }
        for (OrdenServicio o : ordenes.abiertas(porEstado.keySet())) {
            porEstado.get(o.getEstado()).add(o);
        }
        return porEstado;
    }

    public List<OrdenServicio> ordenesRecientes() {
        return ordenes.listar(PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "id"))).getContent();
    }
}
