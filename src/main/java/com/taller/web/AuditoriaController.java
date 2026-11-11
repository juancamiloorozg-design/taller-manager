package com.taller.web;

import com.taller.service.AuditoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Solo ADMIN: la regla /admin/** esta en SecurityConfig. */
@Controller
@RequestMapping("/admin/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoria;

    public AuditoriaController(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    @GetMapping
    public String lista(@RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("pagina", auditoria.listar(pagina));
        model.addAttribute("urlBase", "/admin/auditoria");
        return "admin/auditoria";
    }
}
