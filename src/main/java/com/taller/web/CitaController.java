package com.taller.web;

import com.taller.exception.TallerException;
import com.taller.model.Cita;
import com.taller.service.CitaService;
import com.taller.service.VehiculoService;
import com.taller.util.Tiempo;
import com.taller.web.form.CitaForm;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Controller
@RequestMapping("/citas")
public class CitaController {

    private final CitaService citas;
    private final VehiculoService vehiculos;

    public CitaController(CitaService citas, VehiculoService vehiculos) {
        this.citas = citas;
        this.vehiculos = vehiculos;
    }

    @GetMapping
    public String agenda(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                         Model model) {
        LocalDate hoy = Tiempo.ahora().toLocalDate();
        LocalDate lunes = (fecha == null ? hoy : fecha).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        model.addAttribute("inicio", lunes);
        model.addAttribute("fin", lunes.plusDays(6));
        model.addAttribute("anterior", lunes.minusWeeks(1));
        model.addAttribute("siguiente", lunes.plusWeeks(1));
        model.addAttribute("citas", citas.semana(lunes));
        return "citas/agenda";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("citaForm", new CitaForm());
        return formulario(model);
    }

    @PostMapping
    public String agendar(@Valid @ModelAttribute("citaForm") CitaForm form, BindingResult resultado,
                          Authentication autenticacion, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return formulario(model);
        }
        try {
            Cita cita = citas.agendar(form.getVehiculoId(), form.getFechaHora(), form.getMotivo(),
                    autenticacion.getName());
            redirect.addFlashAttribute("mensajeExito", "Cita agendada.");
            redirect.addAttribute("fecha", cita.getFechaHora().toLocalDate());
            return "redirect:/citas";
        } catch (TallerException e) {
            model.addAttribute("mensajeError", e.getMessage());
            return formulario(model);
        }
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, Authentication autenticacion, RedirectAttributes redirect) {
        try {
            redirect.addAttribute("fecha", citas.cancelar(id, autenticacion.getName()));
            redirect.addFlashAttribute("mensajeExito", "Cita cancelada.");
        } catch (TallerException e) {
            redirect.addAttribute("fecha", citas.obtener(id).getFechaHora().toLocalDate());
            redirect.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/citas";
    }

    private String formulario(Model model) {
        model.addAttribute("vehiculos", vehiculos.listarTodos().stream()
                .filter(v -> !v.getCliente().isAnonimizado()).toList());
        return "citas/form";
    }
}
