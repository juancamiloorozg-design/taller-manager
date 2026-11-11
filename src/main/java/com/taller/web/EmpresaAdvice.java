package com.taller.web;

import com.taller.config.EmpresaProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

/** Pone los datos del taller (y, en la demostracion, las claves de prueba) a disposicion de las plantillas. */
@ControllerAdvice
public class EmpresaAdvice {

    private final EmpresaProperties empresa;
    private final boolean demo;
    private final int horas;
    private final String adminEmail;
    private final String adminClave;
    private final String mecanicoEmail;
    private final String mecanicoClave;

    public EmpresaAdvice(EmpresaProperties empresa,
                         @Value("${taller.demo.activo:false}") boolean demo,
                         @Value("${taller.demo.reinicio-horas:0}") int horas,
                         @Value("${taller.admin.email:}") String adminEmail,
                         @Value("${taller.admin.password:}") String adminClave,
                         @Value("${taller.demo.mecanico-email:}") String mecanicoEmail,
                         @Value("${taller.demo.mecanico-password:}") String mecanicoClave) {
        this.empresa = empresa;
        this.demo = demo;
        this.horas = horas;
        this.adminEmail = adminEmail;
        this.adminClave = adminClave;
        this.mecanicoEmail = mecanicoEmail;
        this.mecanicoClave = mecanicoClave;
    }

    @ModelAttribute("empresa")
    public EmpresaProperties empresa() {
        return empresa;
    }

    /** Nulo fuera de la demostracion, para que las plantillas no muestren nada. */
    @ModelAttribute("demo")
    public Map<String, Object> demo() {
        if (!demo) {
            return null;
        }
        return Map.of("horas", horas, "adminEmail", adminEmail, "adminClave", adminClave,
                "mecanicoEmail", mecanicoEmail, "mecanicoClave", mecanicoClave);
    }
}
