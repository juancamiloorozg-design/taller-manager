package com.taller.web;

import com.taller.exception.TallerException;
import com.taller.model.Rol;
import com.taller.service.UsuarioService;
import com.taller.web.form.UsuarioForm;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Solo accesible para ADMIN (regla definida en SecurityConfig). */
@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        return "usuarios/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("usuarioForm", new UsuarioForm());
        model.addAttribute("roles", Rol.values());
        return "usuarios/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("usuarioForm") UsuarioForm form,
                        BindingResult resultado,
                        Authentication autenticacion,
                        Model model,
                        RedirectAttributes redirect) {
        model.addAttribute("roles", Rol.values());
        if (resultado.hasErrors()) {
            return "usuarios/form";
        }
        try {
            usuarioService.crear(form.getEmail(), form.getNombre(), form.getPassword(),
                    form.getRol(), autenticacion.getName());
        } catch (TallerException e) {
            model.addAttribute("mensajeError", e.getMessage());
            return "usuarios/form";
        }
        redirect.addFlashAttribute("mensajeExito", "Usuario creado.");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, Authentication autenticacion, RedirectAttributes redirect) {
        try {
            usuarioService.cambiarEstado(id, autenticacion.getName());
            redirect.addFlashAttribute("mensajeExito", "Estado del usuario actualizado.");
        } catch (TallerException e) {
            redirect.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/password")
    public String restablecer(@PathVariable Long id,
                              @RequestParam("nuevaPassword") String nuevaPassword,
                              Authentication autenticacion,
                              RedirectAttributes redirect) {
        try {
            usuarioService.restablecerPassword(id, nuevaPassword, autenticacion.getName());
            redirect.addFlashAttribute("mensajeExito", "Contraseña restablecida.");
        } catch (TallerException e) {
            redirect.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/usuarios";
    }
}
