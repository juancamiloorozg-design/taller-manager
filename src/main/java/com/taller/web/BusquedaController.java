package com.taller.web;

import com.taller.model.Cliente;
import com.taller.model.OrdenServicio;
import com.taller.model.Vehiculo;
import com.taller.repository.ClienteRepository;
import com.taller.repository.OrdenServicioRepository;
import com.taller.repository.VehiculoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** Una sola caja para encontrar clientes, vehiculos y ordenes (por numero). */
@Controller
public class BusquedaController {

    private static final int MAXIMO = 8;

    private final ClienteRepository clientes;
    private final VehiculoRepository vehiculos;
    private final OrdenServicioRepository ordenes;

    public BusquedaController(ClienteRepository clientes, VehiculoRepository vehiculos,
                              OrdenServicioRepository ordenes) {
        this.clientes = clientes;
        this.vehiculos = vehiculos;
        this.ordenes = ordenes;
    }

    @GetMapping("/buscar")
    @Transactional(readOnly = true)
    public String buscar(@RequestParam(defaultValue = "") String q, Model model) {
        String texto = q.trim();
        if (texto.length() > 60) {
            texto = texto.substring(0, 60);
        }
        model.addAttribute("q", texto);
        if (texto.length() >= 2 || texto.matches("\\d+")) {
            List<Cliente> c = clientes.buscar(texto, PageRequest.of(0, MAXIMO)).getContent();
            List<Vehiculo> v = vehiculos.buscar(texto, PageRequest.of(0, MAXIMO)).getContent();
            String digitos = texto.replaceFirst("^#", "");
            List<OrdenServicio> o = List.of();
            if (digitos.matches("\\d{1,9}")) {
                o = ordenes.findByIdConDetalle(Long.parseLong(digitos)).map(List::of).orElse(List.of());
            }
            model.addAttribute("clientes", c);
            model.addAttribute("vehiculos", v);
            model.addAttribute("ordenes", o);
            model.addAttribute("hayResultados", !c.isEmpty() || !v.isEmpty() || !o.isEmpty());
            model.addAttribute("buscado", true);
        } else {
            model.addAttribute("buscado", false);
        }
        return "buscar";
    }
}
