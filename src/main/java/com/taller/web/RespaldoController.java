package com.taller.web;

import com.taller.service.AuditoriaService;
import com.taller.service.RespaldoService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Solo ADMIN: la regla esta en SecurityConfig para /admin/**. */
@Controller
@RequestMapping("/admin/respaldos")
public class RespaldoController {

    private final RespaldoService respaldos;
    private final AuditoriaService auditoria;

    public RespaldoController(RespaldoService respaldos, AuditoriaService auditoria) {
        this.respaldos = respaldos;
        this.auditoria = auditoria;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("respaldos", respaldos.listar());
        return "admin/respaldos";
    }

    @PostMapping
    public String crear(Authentication auth, RedirectAttributes flash) {
        RespaldoService.Respaldo r = respaldos.crear();
        auditoria.registrar(auth.getName(), "RESPALDO", "respaldo", null, r.nombre());
        flash.addFlashAttribute("mensajeExito", "Respaldo creado: " + r.nombre());
        return "redirect:/admin/respaldos";
    }

    @GetMapping("/{nombre}")
    public ResponseEntity<FileSystemResource> descargar(@PathVariable String nombre, Authentication auth) {
        var ruta = respaldos.ruta(nombre);
        auditoria.registrar(auth.getName(), "DESCARGA_RESPALDO", "respaldo", null, nombre);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .body(new FileSystemResource(ruta));
    }
}
