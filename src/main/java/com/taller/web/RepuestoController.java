package com.taller.web;

import com.taller.exception.TallerException;
import com.taller.model.Repuesto;
import com.taller.service.RepuestoService;
import com.taller.web.form.RepuestoForm;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/repuestos")
public class RepuestoController {

    private final RepuestoService repuestoService;

    public RepuestoController(RepuestoService repuestoService) {
        this.repuestoService = repuestoService;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("repuestos", repuestoService.listarTodos());
        return "repuestos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("repuestoForm", new RepuestoForm());
        model.addAttribute("edicion", false);
        return "repuestos/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("repuestoForm") RepuestoForm form,
                        BindingResult resultado,
                        Authentication autenticacion,
                        Model model,
                        RedirectAttributes redirect) {
        model.addAttribute("edicion", false);
        if (resultado.hasErrors()) {
            return "repuestos/form";
        }
        try {
            repuestoService.crear(form.aDatos(), autenticacion.getName());
        } catch (TallerException e) {
            model.addAttribute("mensajeError", e.getMessage());
            return "repuestos/form";
        }
        redirect.addFlashAttribute("mensajeExito", "Repuesto registrado.");
        return "redirect:/repuestos";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Repuesto r = repuestoService.obtener(id);
        RepuestoForm form = new RepuestoForm();
        form.setCodigo(r.getCodigo());
        form.setNombre(r.getNombre());
        form.setPrecio(r.getPrecio());
        form.setStock(r.getStock());
        form.setStockMinimo(r.getStockMinimo());
        model.addAttribute("repuestoForm", form);
        model.addAttribute("repuestoId", id);
        model.addAttribute("edicion", true);
        return "repuestos/form";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("repuestoForm") RepuestoForm form,
                             BindingResult resultado,
                             Authentication autenticacion,
                             Model model,
                             RedirectAttributes redirect) {
        model.addAttribute("repuestoId", id);
        model.addAttribute("edicion", true);
        if (resultado.hasErrors()) {
            return "repuestos/form";
        }
        try {
            repuestoService.actualizar(id, form.aDatos(), autenticacion.getName());
        } catch (TallerException e) {
            model.addAttribute("mensajeError", e.getMessage());
            return "repuestos/form";
        }
        redirect.addFlashAttribute("mensajeExito", "Repuesto actualizado.");
        return "redirect:/repuestos";
    }

    @PostMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public String ajustarStock(@PathVariable Long id,
                               @RequestParam("stock") int stock,
                               Authentication autenticacion,
                               RedirectAttributes redirect) {
        try {
            repuestoService.ajustarStock(id, stock, autenticacion.getName());
            redirect.addFlashAttribute("mensajeExito", "Stock ajustado.");
        } catch (TallerException e) {
            redirect.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/repuestos";
    }

    @PostMapping("/{id}/activo")
    @PreAuthorize("hasRole('ADMIN')")
    public String cambiarActivo(@PathVariable Long id, Authentication autenticacion, RedirectAttributes redirect) {
        repuestoService.cambiarActivo(id, autenticacion.getName());
        redirect.addFlashAttribute("mensajeExito", "Estado del repuesto actualizado.");
        return "redirect:/repuestos";
    }
}
