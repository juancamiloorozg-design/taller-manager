package com.taller.web;

import com.taller.service.OrdenServicioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/recordatorios")
public class RecordatorioController {

    private final OrdenServicioService ordenService;

    public RecordatorioController(OrdenServicioService ordenService) {
        this.ordenService = ordenService;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("ordenes", ordenService.recordatoriosPendientes());
        return "recordatorios/lista";
    }

    @PostMapping("/{id}/atendido")
    public String atendido(@PathVariable Long id, Authentication autenticacion, RedirectAttributes redirect) {
        ordenService.marcarRecordatorioAtendido(id, autenticacion.getName());
        redirect.addFlashAttribute("mensajeExito", "Listo, quedó marcado como avisado.");
        return "redirect:/recordatorios";
    }
}
