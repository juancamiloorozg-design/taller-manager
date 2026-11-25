package com.taller.service;

import com.taller.model.EstadoOrden;
import com.taller.model.OrdenServicio;
import com.taller.repository.OrdenServicioRepository;
import com.taller.repository.PagoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class InformeService {

    public record PorMecanico(String nombre, long ordenes, BigDecimal total) {
    }

    public record RepuestoUsado(String nombre, long unidades, BigDecimal valor) {
    }

    public record Pendiente(OrdenServicio orden, BigDecimal saldo) {
    }

    public record Informe(LocalDate desde, LocalDate hasta, long ordenes, BigDecimal total,
                          BigDecimal manoObra, BigDecimal repuestos, BigDecimal ticketPromedio,
                          BigDecimal abonosRecibidos, List<PorMecanico> porMecanico,
                          List<RepuestoUsado> masUsados, Map<String, BigDecimal> porMes,
                          List<Pendiente> porCobrar, BigDecimal totalPorCobrar) {
    }

    private final OrdenServicioRepository ordenes;
    private final PagoRepository pagos;

    public InformeService(OrdenServicioRepository ordenes, PagoRepository pagos) {
        this.ordenes = ordenes;
        this.pagos = pagos;
    }

    public Informe generar(LocalDate desde, LocalDate hasta) {
        LocalDateTime ini = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();
        List<OrdenServicio> lista = ordenes.finalizadasEntre(EstadoOrden.FINALIZADA, ini, fin);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal manoObra = BigDecimal.ZERO;
        Map<String, long[]> conteo = new LinkedHashMap<>();
        Map<String, BigDecimal> sumas = new LinkedHashMap<>();
        for (OrdenServicio o : lista) {
            total = total.add(o.getTotal());
            manoObra = manoObra.add(o.getManoObra());
            String nombre = o.getMecanico() == null ? "Sin asignar" : o.getMecanico().getNombre();
            conteo.computeIfAbsent(nombre, k -> new long[1])[0]++;
            sumas.merge(nombre, o.getTotal(), BigDecimal::add);
        }
        List<PorMecanico> porMecanico = new ArrayList<>();
        conteo.forEach((nombre, n) -> porMecanico.add(new PorMecanico(nombre, n[0], sumas.get(nombre))));
        porMecanico.sort((a, b) -> b.total().compareTo(a.total()));

        BigDecimal promedio = lista.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(lista.size()), 0, java.math.RoundingMode.HALF_UP);

        List<RepuestoUsado> masUsados = new ArrayList<>();
        for (Object[] fila : ordenes.repuestosMasUsados(EstadoOrden.FINALIZADA, ini, fin, PageRequest.of(0, 10))) {
            masUsados.add(new RepuestoUsado((String) fila[0], ((Number) fila[1]).longValue(), (BigDecimal) fila[2]));
        }

        List<Pendiente> pendientes = pendientesDeCobro();
        BigDecimal totalPendiente = pendientes.stream().map(Pendiente::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Informe(desde, hasta, lista.size(), total, manoObra, total.subtract(manoObra), promedio,
                pagos.sumaEntre(ini, fin), porMecanico, masUsados, ultimosMeses(hasta),
                pendientes, totalPendiente);
    }

    /** Ordenes finalizadas que todavia tienen saldo por cobrar. */
    public List<Pendiente> pendientesDeCobro() {
        Map<Long, BigDecimal> abonado = new HashMap<>();
        for (Object[] fila : pagos.totalesPorOrden()) {
            abonado.put((Long) fila[0], (BigDecimal) fila[1]);
        }
        List<Pendiente> resultado = new ArrayList<>();
        for (OrdenServicio o : ordenes.listarPorEstadoCompleto(EstadoOrden.FINALIZADA)) {
            BigDecimal saldo = o.getTotal().subtract(abonado.getOrDefault(o.getId(), BigDecimal.ZERO));
            if (saldo.signum() > 0) {
                resultado.add(new Pendiente(o, saldo));
            }
        }
        return resultado;
    }

    public BigDecimal totalPorCobrar() {
        return pendientesDeCobro().stream().map(Pendiente::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** CSV para Excel en espanol: separador punto y coma, coma decimal y marca BOM para las tildes. */
    public String csvOrdenes(LocalDate desde, LocalDate hasta) {
        List<OrdenServicio> lista = ordenes.finalizadasEntre(EstadoOrden.FINALIZADA,
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
        Map<Long, BigDecimal> abonado = new HashMap<>();
        for (Object[] fila : pagos.totalesPorOrden()) {
            abonado.put((Long) fila[0], (BigDecimal) fila[1]);
        }
        StringBuilder sb = new StringBuilder("﻿");
        sb.append("Orden;Finalizada;Placa;Mecánico;Mano de obra;Repuestos;Total;Abonado;Saldo\r\n");
        for (OrdenServicio o : lista) {
            BigDecimal pagado = abonado.getOrDefault(o.getId(), BigDecimal.ZERO);
            sb.append(o.getId()).append(';')
                    .append(o.getFinalizadoEn().toLocalDate()).append(';')
                    .append(celda(o.getVehiculo().getPlaca())).append(';')
                    .append(celda(o.getMecanico() == null ? "Sin asignar" : o.getMecanico().getNombre())).append(';')
                    .append(numero(o.getManoObra())).append(';')
                    .append(numero(o.getTotalRepuestos())).append(';')
                    .append(numero(o.getTotal())).append(';')
                    .append(numero(pagado)).append(';')
                    .append(numero(o.getTotal().subtract(pagado))).append("\r\n");
        }
        return sb.toString();
    }

    private Map<String, BigDecimal> ultimosMeses(LocalDate hasta) {
        YearMonth ultimo = YearMonth.from(hasta);
        LocalDateTime ini = ultimo.minusMonths(5).atDay(1).atStartOfDay();
        LocalDateTime fin = ultimo.plusMonths(1).atDay(1).atStartOfDay();
        Map<String, BigDecimal> meses = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            meses.put(ultimo.minusMonths(i).toString(), BigDecimal.ZERO);
        }
        for (OrdenServicio o : ordenes.finalizadasEntre(EstadoOrden.FINALIZADA, ini, fin)) {
            meses.merge(YearMonth.from(o.getFinalizadoEn()).toString(), o.getTotal(), BigDecimal::add);
        }
        return meses;
    }

    private static String numero(BigDecimal v) {
        return v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /** Evita que Excel interprete una celda como formula (inyeccion de CSV) y protege separadores. */
    private static String celda(String texto) {
        String t = texto == null ? "" : texto;
        if (!t.isEmpty() && "=+-@\t\r".indexOf(t.charAt(0)) >= 0) {
            t = "'" + t;
        }
        if (t.contains(";") || t.contains("\"") || t.contains("\n")) {
            t = "\"" + t.replace("\"", "\"\"") + "\"";
        }
        return t;
    }
}
