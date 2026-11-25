package com.taller.web;

import com.taller.exception.TallerException;
import com.taller.service.UsuarioService;
import com.taller.web.form.PasswordForm;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cuenta")
public class CuentaController {

    private final UsuarioService usuarioService;

    public CuentaController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/password")
    public String formulario(Model model) {
        model.addAttribute("passwordForm", new PasswordForm());
        return "cuenta/password";
    }

    @PostMapping("/password")
    public String cambiar(@Valid @ModelAttribute("passwordForm") PasswordForm form,
                          BindingResult resultado,
                          Authentication autenticacion,
                          Model model,
                          RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return "cuenta/password";
        }
        if (!form.getNueva().equals(form.getConfirmacion())) {
            resultado.rejectValue("confirmacion", "noCoincide", "Las contraseñas no coinciden.");
            return "cuenta/password";
        }
        try {
            usuarioService.cambiarPassword(autenticacion.getName(), form.getActual(), form.getNueva());
        } catch (TallerException e) {
            model.addAttribute("mensajeError", e.getMessage());
            return "cuenta/password";
        }
        redirect.addFlashAttribute("mensajeExito", "Contraseña actualizada.");
        return "redirect:/";
    }
}
