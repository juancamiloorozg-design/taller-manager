package com.taller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Paginas publicas: se pueden leer sin iniciar sesion. */
@Controller
public class LegalController {

    @GetMapping("/legal/privacidad")
    public String privacidad() {
        return "legal/privacidad";
    }

    @GetMapping("/legal/aviso-privacidad")
    public String aviso() {
        return "legal/aviso-privacidad";
    }

    @GetMapping("/legal/terminos")
    public String terminos() {
        return "legal/terminos";
    }

    @GetMapping("/legal/cookies")
    public String cookies() {
        return "legal/cookies";
    }
}
