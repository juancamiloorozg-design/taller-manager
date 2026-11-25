package com.taller.service;

import com.taller.config.EmpresaProperties;
import com.taller.exception.RecursoNoEncontradoException;
import com.taller.exception.TallerException;
import com.taller.model.Cliente;
import com.taller.model.OrdenServicio;
import com.taller.model.Vehiculo;
import com.taller.repository.ClienteRepository;
import com.taller.repository.OrdenServicioRepository;
import com.taller.repository.VehiculoRepository;
import com.taller.util.Tiempo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ClienteService {

    /** Datos editables de un cliente, sin depender de la capa web. */
    public record Datos(String tipoDocumento, String numeroDocumento, String nombre,
                        String telefono, String email, String direccion) {
    }

    private static final int TAMANO_PAGINA = 15;
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ClienteRepository clientes;
    private final VehiculoRepository vehiculos;
    private final OrdenServicioRepository ordenes;
    private final AuditoriaService auditoria;
    private final EmpresaProperties empresa;

    public ClienteService(ClienteRepository clientes, VehiculoRepository vehiculos,
                          OrdenServicioRepository ordenes, AuditoriaService auditoria,
                          EmpresaProperties empresa) {
        this.clientes = clientes;
        this.vehiculos = vehiculos;
        this.ordenes = ordenes;
        this.auditoria = auditoria;
        this.empresa = empresa;
    }

    @Transactional(readOnly = true)
    public Page<Cliente> buscar(String q, int pagina) {
        String texto = q == null ? "" : q.trim();
        return clientes.buscar(texto, PageRequest.of(Math.max(pagina, 0), TAMANO_PAGINA, Sort.by("nombre")));
    }

    @Transactional(readOnly = true)
    public Cliente obtener(Long id) {
        return clientes.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un cliente con id " + id + "."));
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarActivos() {
        return clientes.findByAnonimizadoFalseOrderByNombreAsc();
    }

    /** Sin autorizacion de tratamiento de datos no se puede registrar al cliente (Ley 1581 de 2012). */
    @Transactional
    public Cliente crear(Datos datos, boolean autorizaDatos, String actor) {
        if (!autorizaDatos) {
            throw new TallerException("Para registrar al cliente se necesita su autorización de tratamiento de datos personales.");
        }
        String tipo = datos.tipoDocumento().trim().toUpperCase();
        String numero = datos.numeroDocumento().trim().toUpperCase();
        if (clientes.existsByTipoDocumentoAndNumeroDocumento(tipo, numero)) {
            throw new TallerException("Ya existe un cliente con ese documento.");
        }
        Cliente cliente = new Cliente(tipo, numero, datos.nombre().trim());
        aplicar(cliente, datos);
        cliente.registrarAutorizacion(empresa.getPoliticaVersion(), actor);
        clientes.save(cliente);
        auditoria.registrar(actor, "CREAR", "Cliente", cliente.getId(), null);
        return cliente;
    }

    @Transactional
    public Cliente actualizar(Long id, Datos datos, String actor) {
        Cliente cliente = obtener(id);
        if (cliente.isAnonimizado()) {
            throw new TallerException("Este cliente fue anonimizado y ya no se puede editar.");
        }
        String tipo = datos.tipoDocumento().trim().toUpperCase();
        String numero = datos.numeroDocumento().trim().toUpperCase();
        if (clientes.existsByTipoDocumentoAndNumeroDocumentoAndIdNot(tipo, numero, id)) {
            throw new TallerException("Ya existe otro cliente con ese documento.");
        }
        cliente.setTipoDocumento(tipo);
        cliente.setNumeroDocumento(numero);
        aplicar(cliente, datos);
        auditoria.registrar(actor, "EDITAR", "Cliente", id, null);
        return cliente;
    }

    /** Atiende una solicitud de supresion: quita los datos personales y conserva el historial de ordenes. */
    @Transactional
    public void anonimizar(Long id, String actor) {
        Cliente cliente = obtener(id);
        if (cliente.isAnonimizado()) {
            throw new TallerException("Este cliente ya fue anonimizado.");
        }
        cliente.anonimizar();
        auditoria.registrar(actor, "SUPRESION_DATOS", "Cliente", id, "Datos personales anonimizados");
    }

    /** Atiende una solicitud de acceso: entrega en texto todo lo que el taller guarda del titular. */
    @Transactional
    public String exportarDatos(Long id, String actor) {
        Cliente c = obtener(id);
        StringBuilder sb = new StringBuilder();
        sb.append("Datos personales del titular\n");
        sb.append("Responsable: ").append(empresa.getNombre()).append(" (").append(empresa.getNit()).append(")\n");
        sb.append("Generado el: ").append(Tiempo.ahora().format(FECHA_HORA)).append("\n\n");

        sb.append("Documento: ").append(c.getTipoDocumento()).append(" ").append(c.getNumeroDocumento()).append("\n");
        sb.append("Nombre: ").append(c.getNombre()).append("\n");
        sb.append("Teléfono: ").append(valor(c.getTelefono())).append("\n");
        sb.append("Correo: ").append(valor(c.getEmail())).append("\n");
        sb.append("Dirección: ").append(valor(c.getDireccion())).append("\n");
        sb.append("Registrado el: ").append(formato(c.getCreadoEn())).append("\n\n");

        sb.append("Autorización de tratamiento de datos: ").append(c.isAutorizaDatos() ? "Sí" : "No").append("\n");
        sb.append("Fecha de la autorización: ").append(formato(c.getFechaAutorizacion())).append("\n");
        sb.append("Versión de la política aceptada: ").append(valor(c.getVersionPolitica())).append("\n");
        sb.append("Registrada por: ").append(valor(c.getRegistradoPor())).append("\n\n");

        sb.append("Vehículos\n");
        List<Vehiculo> lista = vehiculos.findByClienteIdOrderByPlacaAsc(id);
        if (lista.isEmpty()) {
            sb.append("  (ninguno)\n");
        }
        for (Vehiculo v : lista) {
            sb.append("  ").append(v.getPlaca()).append(" ").append(v.getMarca()).append(" ")
                    .append(v.getModelo()).append(" ").append(v.getAnio()).append("\n");
        }

        sb.append("\nÓrdenes de servicio\n");
        List<OrdenServicio> historial = ordenes.findByClienteId(id);
        if (historial.isEmpty()) {
            sb.append("  (ninguna)\n");
        }
        for (OrdenServicio o : historial) {
            sb.append("  Orden ").append(o.getId()).append(" | ").append(formato(o.getCreadoEn()))
                    .append(" | ").append(o.getVehiculo().getPlaca())
                    .append(" | ").append(o.getEstado().getEtiqueta())
                    .append(" | ").append(o.getDescripcion()).append("\n");
        }

        auditoria.registrar(actor, "EXPORTAR_DATOS", "Cliente", id, "Solicitud de acceso del titular");
        return sb.toString();
    }

    private void aplicar(Cliente cliente, Datos datos) {
        cliente.setNombre(datos.nombre().trim());
        cliente.setTelefono(limpiar(datos.telefono()));
        cliente.setEmail(limpiar(datos.email()));
        cliente.setDireccion(limpiar(datos.direccion()));
    }

    private static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    private static String valor(String texto) {
        return texto == null || texto.isBlank() ? "(sin dato)" : texto;
    }

    private static String formato(LocalDateTime fecha) {
        return fecha == null ? "(sin dato)" : fecha.format(FECHA_HORA);
    }
}
