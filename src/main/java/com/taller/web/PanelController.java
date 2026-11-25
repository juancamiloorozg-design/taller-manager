package com.taller.web;

import com.taller.model.EstadoOrden;
import com.taller.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PanelController {

    private final DashboardService dashboard;

    public PanelController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/")
    public String panel(Model model) {
        model.addAttribute("pendientes", dashboard.contarOrdenes(EstadoOrden.PENDIENTE));
        model.addAttribute("enProceso", dashboard.contarOrdenes(EstadoOrden.EN_PROCESO));
        model.addAttribute("finalizadoMes", dashboard.finalizadoEnElMes());
        model.addAttribute("totalClientes", dashboard.totalClientes());
        model.addAttribute("totalVehiculos", dashboard.totalVehiculos());
        model.addAttribute("stockBajo", dashboard.stockBajo());
        model.addAttribute("recientes", dashboard.ordenesRecientes());
        model.addAttribute("recordatorios", dashboard.recordatoriosPendientes());
        model.addAttribute("porCobrar", dashboard.porCobrar());
        model.addAttribute("cotizadas", dashboard.contarOrdenes(EstadoOrden.COTIZADA));
        model.addAttribute("abiertas", dashboard.ordenesAbiertas());
        return "panel";
    }
}
