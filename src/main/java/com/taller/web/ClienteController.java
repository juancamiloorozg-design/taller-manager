package com.taller.web;

import com.taller.exception.TallerException;
import com.taller.model.Cliente;
import com.taller.service.ClienteService;
import com.taller.service.VehiculoService;
import com.taller.web.form.ClienteForm;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final VehiculoService vehiculoService;

    public ClienteController(ClienteService clienteService, VehiculoService vehiculoService) {
        this.clienteService = clienteService;
        this.vehiculoService = vehiculoService;
    }

    @GetMapping
    public String lista(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "0") int pagina,
                        Model model) {
        Page<Cliente> pag = clienteService.buscar(q, pagina);
        model.addAttribute("pagina", pag);
        model.addAttribute("q", q);
        model.addAttribute("urlBase", "/clientes");
        return "clientes/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("clienteForm", new ClienteForm());
        model.addAttribute("edicion", false);
        return "clientes/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("clienteForm") ClienteForm form,
                        BindingResult resultado,
                        Authentication autenticacion,
                        Model model,
                        RedirectAttributes redirect) {
        model.addAttribute("edicion", false);
        if (resultado.hasErrors()) {
            return "clientes/form";
        }
        try {
            Cliente creado = clienteService.crear(form.aDatos(), form.isAutorizaDatos(), autenticacion.getName());
            redirect.addFlashAttribute("mensajeExito", "Cliente registrado.");
            return "redirect:/clientes/" + creado.getId();
        } catch (TallerException e) {
            model.addAttribute("mensajeError", e.getMessage());
            return "clientes/form";
        }
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        model.addAttribute("cliente", clienteService.obtener(id));
        model.addAttribute("vehiculos", vehiculoService.deCliente(id));
        return "clientes/detalle";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Cliente c = clienteService.obtener(id);
        ClienteForm form = new ClienteForm();
        form.setTipoDocumento(c.getTipoDocumento());
        form.setNumeroDocumento(c.getNumeroDocumento());
        form.setNombre(c.getNombre());
        form.setTelefono(c.getTelefono());
        form.setEmail(c.getEmail());
        form.setDireccion(c.getDireccion());
        model.addAttribute("clienteForm", form);
        model.addAttribute("clienteId", id);
        model.addAttribute("edicion", true);
        return "clientes/form";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("clienteForm") ClienteForm form,
                             BindingResult resultado,
                             Authentication autenticacion,
                             Model model,
                             RedirectAttributes redirect) {
        model.addAttribute("clienteId", id);
        model.addAttribute("edicion", true);
        if (resultado.hasErrors()) {
            return "clientes/form";
        }
        try {
            clienteService.actualizar(id, form.aDatos(), autenticacion.getName());
        } catch (TallerException e) {
            model.addAttribute("mensajeError", e.getMessage());
            return "clientes/form";
        }
        redirect.addFlashAttribute("mensajeExito", "Cliente actualizado.");
        return "redirect:/clientes/" + id;
    }

    /** Solicitud de supresion del titular. */
    @PostMapping("/{id}/anonimizar")
    @PreAuthorize("hasRole('ADMIN')")
    public String anonimizar(@PathVariable Long id, Authentication autenticacion, RedirectAttributes redirect) {
        try {
            clienteService.anonimizar(id, autenticacion.getName());
            redirect.addFlashAttribute("mensajeExito", "Datos personales eliminados. El historial de órdenes se conserva sin identificar al titular.");
        } catch (TallerException e) {
            redirect.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/clientes/" + id;
    }

    /** Solicitud de acceso del titular: descarga un archivo de texto con sus datos. */
    @GetMapping("/{id}/datos.txt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportar(@PathVariable Long id, Authentication autenticacion) {
        String texto = clienteService.exportarDatos(id, autenticacion.getName());
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
        cabeceras.setContentDisposition(ContentDisposition.attachment()
                .filename("datos-cliente-" + id + ".txt").build());
        cabeceras.setCacheControl("no-store");
        return ResponseEntity.ok().headers(cabeceras).body(texto.getBytes(StandardCharsets.UTF_8));
    }
}
