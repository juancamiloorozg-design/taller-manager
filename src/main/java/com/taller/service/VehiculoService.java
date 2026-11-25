package com.taller.service;

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

import java.util.List;

@Service
public class VehiculoService {

    public record Datos(String placa, String marca, String modelo, int anio, String color, Long clienteId) {
    }

    private static final int TAMANO_PAGINA = 15;
    private static final int ANIO_MINIMO = 1950;

    private final VehiculoRepository vehiculos;
    private final ClienteRepository clientes;
    private final OrdenServicioRepository ordenes;
    private final AuditoriaService auditoria;

    public VehiculoService(VehiculoRepository vehiculos, ClienteRepository clientes,
                           OrdenServicioRepository ordenes, AuditoriaService auditoria) {
        this.vehiculos = vehiculos;
        this.clientes = clientes;
        this.ordenes = ordenes;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public Page<Vehiculo> buscar(String q, int pagina) {
        String texto = q == null ? "" : q.trim();
        return vehiculos.buscar(texto, PageRequest.of(Math.max(pagina, 0), TAMANO_PAGINA, Sort.by("placa")));
    }

    @Transactional(readOnly = true)
    public Vehiculo obtener(Long id) {
        return vehiculos.findByIdConCliente(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un vehículo con id " + id + "."));
    }

    @Transactional(readOnly = true)
    public List<Vehiculo> listarTodos() {
        return vehiculos.findAllConCliente();
    }

    @Transactional(readOnly = true)
    public List<Vehiculo> deCliente(Long clienteId) {
        return vehiculos.findByClienteIdOrderByPlacaAsc(clienteId);
    }

    @Transactional(readOnly = true)
    public List<OrdenServicio> historial(Long vehiculoId) {
        return ordenes.findByVehiculoIdOrderByIdDesc(vehiculoId);
    }

    @Transactional
    public Vehiculo crear(Datos datos, String actor) {
        String placa = normalizarPlaca(datos.placa());
        validarAnio(datos.anio());
        if (vehiculos.existsByPlaca(placa)) {
            throw new TallerException("Ya existe un vehículo con la placa " + placa + ".");
        }
        Cliente cliente = clienteValido(datos.clienteId());
        Vehiculo vehiculo = new Vehiculo(placa, datos.marca().trim(), datos.modelo().trim(), datos.anio(), cliente);
        vehiculo.setColor(limpiar(datos.color()));
        vehiculos.save(vehiculo);
        auditoria.registrar(actor, "CREAR", "Vehiculo", vehiculo.getId(), "Placa " + placa);
        return vehiculo;
    }

    @Transactional
    public Vehiculo actualizar(Long id, Datos datos, String actor) {
        Vehiculo vehiculo = obtener(id);
        String placa = normalizarPlaca(datos.placa());
        validarAnio(datos.anio());
        if (vehiculos.existsByPlacaAndIdNot(placa, id)) {
            throw new TallerException("Ya existe otro vehículo con la placa " + placa + ".");
        }
        vehiculo.setPlaca(placa);
        vehiculo.setMarca(datos.marca().trim());
        vehiculo.setModelo(datos.modelo().trim());
        vehiculo.setAnio(datos.anio());
        vehiculo.setColor(limpiar(datos.color()));
        vehiculo.setCliente(clienteValido(datos.clienteId()));
        auditoria.registrar(actor, "EDITAR", "Vehiculo", id, "Placa " + placa);
        return vehiculo;
    }

    /** Solo se puede borrar un vehiculo que nunca tuvo ordenes. */
    @Transactional
    public void eliminar(Long id, String actor) {
        Vehiculo vehiculo = obtener(id);
        if (ordenes.existsByVehiculoId(id)) {
            throw new TallerException("No se puede eliminar un vehículo que tiene órdenes de servicio.");
        }
        vehiculos.delete(vehiculo);
        auditoria.registrar(actor, "ELIMINAR", "Vehiculo", id, "Placa " + vehiculo.getPlaca());
    }

    private Cliente clienteValido(Long clienteId) {
        if (clienteId == null) {
            throw new TallerException("Selecciona un cliente.");
        }
        Cliente cliente = clientes.findById(clienteId)
                .orElseThrow(() -> new TallerException("El cliente seleccionado no existe."));
        if (cliente.isAnonimizado()) {
            throw new TallerException("No se pueden asociar vehículos a un cliente anonimizado.");
        }
        return cliente;
    }

    private static String normalizarPlaca(String placa) {
        return placa == null ? "" : placa.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    private static void validarAnio(int anio) {
        int maximo = Tiempo.ahora().getYear() + 1;
        if (anio < ANIO_MINIMO || anio > maximo) {
            throw new TallerException("El año debe estar entre " + ANIO_MINIMO + " y " + maximo + ".");
        }
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
