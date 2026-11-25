package com.taller.web;

import com.taller.exception.TallerException;
import com.taller.model.Vehiculo;
import com.taller.service.ClienteService;
import com.taller.service.VehiculoService;
import com.taller.web.form.VehiculoForm;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculoService;
    private final ClienteService clienteService;

    public VehiculoController(VehiculoService vehiculoService, ClienteService clienteService) {
        this.vehiculoService = vehiculoService;
        this.clienteService = clienteService;
    }

    @GetMapping
    public String lista(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "0") int pagina,
                        Model model) {
        Page<Vehiculo> pag = vehiculoService.buscar(q, pagina);
        model.addAttribute("pagina", pag);
        model.addAttribute("q", q);
        model.addAttribute("urlBase", "/vehiculos");
        return "vehiculos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam(required = false) Long clienteId, Model model) {
        VehiculoForm form = new VehiculoForm();
        form.setClienteId(clienteId);
        return preparar(model, form, false, null);
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("vehiculoForm") VehiculoForm form,
                        BindingResult resultado,
                        Authentication autenticacion,
                        Model model,
                        RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return preparar(model, null, false, null);
        }
        try {
            Vehiculo creado = vehiculoService.crear(form.aDatos(), autenticacion.getName());
            redirect.addFlashAttribute("mensajeExito", "Vehículo registrado.");
            return "redirect:/vehiculos/" + creado.getId();
        } catch (TallerException e) {
            model.addAttribute("mensajeError", e.getMessage());
            return preparar(model, null, false, null);
        }
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        model.addAttribute("vehiculo", vehiculoService.obtener(id));
        model.addAttribute("ordenes", vehiculoService.historial(id));
        return "vehiculos/detalle";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Vehiculo v = vehiculoService.obtener(id);
        VehiculoForm form = new VehiculoForm();
        form.setPlaca(v.getPlaca());
        form.setMarca(v.getMarca());
        form.setModelo(v.getModelo());
        form.setAnio(v.getAnio());
        form.setColor(v.getColor());
        form.setClienteId(v.getCliente().getId());
        return preparar(model, form, true, id);
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("vehiculoForm") VehiculoForm form,
                             BindingResult resultado,
                             Authentication autenticacion,
                             Model model,
                             RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return preparar(model, null, true, id);
        }
        try {
            vehiculoService.actualizar(id, form.aDatos(), autenticacion.getName());
        } catch (TallerException e) {
            model.addAttribute("mensajeError", e.getMessage());
            return preparar(model, null, true, id);
        }
        redirect.addFlashAttribute("mensajeExito", "Vehículo actualizado.");
        return "redirect:/vehiculos/" + id;
    }

    @PostMapping("/{id}/eliminar")
    @PreAuthorize("hasRole('ADMIN')")
    public String eliminar(@PathVariable Long id, Authentication autenticacion, RedirectAttributes redirect) {
        try {
            vehiculoService.eliminar(id, autenticacion.getName());
            redirect.addFlashAttribute("mensajeExito", "Vehículo eliminado.");
            return "redirect:/vehiculos";
        } catch (TallerException e) {
            redirect.addFlashAttribute("mensajeError", e.getMessage());
            return "redirect:/vehiculos/" + id;
        }
    }

    private String preparar(Model model, VehiculoForm form, boolean edicion, Long id) {
        if (form != null) {
            model.addAttribute("vehiculoForm", form);
        }
        model.addAttribute("clientes", clienteService.listarActivos());
        model.addAttribute("edicion", edicion);
        model.addAttribute("vehiculoId", id);
        return "vehiculos/form";
    }
}
