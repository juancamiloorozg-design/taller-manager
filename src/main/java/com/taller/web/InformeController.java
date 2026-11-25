package com.taller.web;

import com.taller.service.InformeService;
import com.taller.util.Tiempo;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** Solo ADMIN: la regla /informes/** esta en SecurityConfig. */
@Controller
@RequestMapping("/informes")
public class InformeController {

    private final InformeService informes;

    public InformeController(InformeService informes) {
        this.informes = informes;
    }

    @GetMapping
    public String ver(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                      Model model) {
        LocalDate[] rango = rango(desde, hasta);
        model.addAttribute("informe", informes.generar(rango[0], rango[1]));
        model.addAttribute("desde", rango[0]);
        model.addAttribute("hasta", rango[1]);
        return "informes/informe";
    }

    @GetMapping("/ordenes.csv")
    public ResponseEntity<byte[]> csv(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate[] rango = rango(desde, hasta);
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        cabeceras.setContentDisposition(ContentDisposition.attachment()
                .filename("ordenes-" + rango[0] + "-a-" + rango[1] + ".csv").build());
        cabeceras.setCacheControl("no-store");
        return ResponseEntity.ok().headers(cabeceras)
                .body(informes.csvOrdenes(rango[0], rango[1]).getBytes(StandardCharsets.UTF_8));
    }

    private static LocalDate[] rango(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = Tiempo.ahora().toLocalDate();
        LocalDate fin = hasta == null ? hoy : hasta;
        LocalDate ini = desde == null ? fin.withDayOfMonth(1) : desde;
        if (ini.isAfter(fin)) {
            LocalDate t = ini;
            ini = fin;
            fin = t;
        }
        return new LocalDate[]{ini, fin};
    }
}
