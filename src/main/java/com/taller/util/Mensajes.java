package com.taller.util;

import com.taller.config.EmpresaProperties;
import com.taller.model.Cliente;
import com.taller.model.OrdenServicio;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Arma enlaces de WhatsApp (wa.me) con el mensaje ya escrito. No envia nada: abre WhatsApp en el
 * telefono o en la web de quien trabaja en el taller y el envio lo hace una persona.
 * Disponible en las plantillas como ${@msg.listo(orden)}.
 * Solo devuelve enlace si el cliente autorizo el tratamiento de datos y tiene un celular valido.
 */
@Component("msg")
public class Mensajes {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmpresaProperties empresa;
    private final Formato formato;

    public Mensajes(EmpresaProperties empresa, Formato formato) {
        this.empresa = empresa;
        this.formato = formato;
    }

    public String listo(OrdenServicio o) {
        return enlace(o, "Hola " + nombre(o) + ", le escribimos de " + empresa.getNombre()
                + ". Su vehículo placa " + o.getVehiculo().getPlaca() + " ya está listo para recoger. "
                + "El valor total es " + formato.cop(o.getTotal()) + ".");
    }

    public String cotizacion(OrdenServicio o) {
        return enlace(o, "Hola " + nombre(o) + ", le escribimos de " + empresa.getNombre()
                + ". La cotización #" + o.getId() + " para su vehículo placa " + o.getVehiculo().getPlaca()
                + " es de " + formato.cop(o.getTotal()) + ". Si está de acuerdo, nos avisa y empezamos el trabajo.");
    }

    public String recordatorio(OrdenServicio o) {
        String cuando = o.getProximaFecha() == null ? "pronto" : "hacia el " + o.getProximaFecha().format(FECHA);
        String km = o.getProximoKm() == null ? "" : " o a los " + o.getProximoKm() + " km";
        return enlace(o, "Hola " + nombre(o) + ", le escribimos de " + empresa.getNombre()
                + ". A su vehículo placa " + o.getVehiculo().getPlaca() + " le toca el próximo servicio "
                + cuando + km + ". ¿Le separamos un espacio?");
    }

    private String enlace(OrdenServicio o, String texto) {
        Cliente c = o.getVehiculo().getCliente();
        if (c.isAnonimizado() || !c.isAutorizaDatos()) {
            return null;
        }
        String celular = celular(c.getTelefono());
        if (celular == null) {
            return null;
        }
        String codificado = URLEncoder.encode(texto, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://wa.me/" + celular + "?text=" + codificado;
    }

    private static String nombre(OrdenServicio o) {
        String completo = o.getVehiculo().getCliente().getNombre().trim();
        int espacio = completo.indexOf(' ');
        return espacio > 0 ? completo.substring(0, espacio) : completo;
    }

    /** Acepta celulares colombianos de 10 digitos (3XXXXXXXXX), con o sin 57 adelante. */
    static String celular(String telefono) {
        if (telefono == null) {
            return null;
        }
        String d = telefono.replaceAll("[^0-9]", "");
        if (d.length() == 10 && d.startsWith("3")) {
            return "57" + d;
        }
        if (d.length() == 12 && d.startsWith("573")) {
            return d;
        }
        return null;
    }
}
