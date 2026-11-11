package com.taller.web;

import com.taller.exception.TallerException;
import com.taller.model.EstadoOrden;
import com.taller.model.MetodoPago;
import com.taller.model.NivelCombustible;
import com.taller.model.OrdenServicio;
import com.taller.service.FotoService;
import com.taller.service.OrdenServicioService;
import com.taller.service.PagoService;
import com.taller.service.RepuestoService;
import com.taller.service.UsuarioService;
import com.taller.service.VehiculoService;
import com.taller.web.form.OrdenForm;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/ordenes")
public class OrdenController {

    private final OrdenServicioService ordenService;
    private final VehiculoService vehiculoService;
    private final RepuestoService repuestoService;
    private final UsuarioService usuarioService;
    private final PagoService pagoService;
    private final FotoService fotoService;

    public OrdenController(OrdenServicioService ordenService, VehiculoService vehiculoService,
                           RepuestoService repuestoService, UsuarioService usuarioService,
                           PagoService pagoService, FotoService fotoService) {
        this.ordenService = ordenService;
        this.vehiculoService = vehiculoService;
        this.repuestoService = repuestoService;
        this.usuarioService = usuarioService;
        this.pagoService = pagoService;
        this.fotoService = fotoService;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) EstadoOrden estado,
                        @RequestParam(defaultValue = "0") int pagina,
                        Model model) {
        Page<OrdenServicio> pag = ordenService.listar(estado, pagina);
        model.addAttribute("pagina", pag);
        model.addAttribute("estado", estado);
        model.addAttribute("estados", EstadoOrden.values());
        model.addAttribute("urlBase", "/ordenes");
        return "ordenes/lista";
    }

    @GetMapping("/nueva")
    public String nueva(@RequestParam(required = false) Long vehiculoId,
                        @RequestParam(defaultValue = "false") boolean cotizacion,
                        Model model) {
        OrdenForm form = new OrdenForm();
        form.setVehiculoId(vehiculoId);
        form.setCotizacion(cotizacion);
        model.addAttribute("ordenForm", form);
        cargarListas(model);
        return "ordenes/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("ordenForm") OrdenForm form,
                        BindingResult resultado,
                        Authentication autenticacion,
                        Model model,
                        RedirectAttributes redirect) {
        if (form.getVehiculoId() == null) {
            resultado.rejectValue("vehiculoId", "vehiculo.requerido", "Selecciona un vehículo.");
        }
        if (resultado.hasErrors()) {
            cargarListas(model);
            return "ordenes/form";
        }
        try {
            OrdenServicio orden = ordenService.crearConRepuestos(form.getVehiculoId(), form.aDatos(),
                    form.isCotizacion(), form.getCantidades(), autenticacion.getName());
            redirect.addFlashAttribute("mensajeExito", form.isCotizacion() ? "Cotización creada." : "Orden creada.");
            return "redirect:/ordenes/" + orden.getId();
        } catch (TallerException e) {
            model.addAttribute("mensajeError", e.getMessage());
            cargarListas(model);
            return "ordenes/form";
        }
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        OrdenServicio orden = ordenService.obtener(id);
        BigDecimal abonado = pagoService.abonado(id);
        model.addAttribute("orden", orden);
        model.addAttribute("repuestos", repuestoService.listarDisponibles());
        model.addAttribute("pagos", pagoService.deOrden(id));
        model.addAttribute("abonado", abonado);
        model.addAttribute("saldo", orden.getTotal().subtract(abonado));
        model.addAttribute("fotos", fotoService.deOrden(id));
        model.addAttribute("metodos", MetodoPago.values());
        return "ordenes/detalle";
    }

    @GetMapping("/{id}/imprimir")
    public String imprimir(@PathVariable Long id, Model model) {
        OrdenServicio orden = ordenService.obtener(id);
        BigDecimal abonado = pagoService.abonado(id);
        model.addAttribute("orden", orden);
        model.addAttribute("pagos", pagoService.deOrden(id));
        model.addAttribute("abonado", abonado);
        model.addAttribute("saldo", orden.getTotal().subtract(abonado));
        return "ordenes/imprimir";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        OrdenServicio o = ordenService.obtener(id);
        OrdenForm form = new OrdenForm();
        form.setVehiculoId(o.getVehiculo().getId());
        form.setDescripcion(o.getDescripcion());
        form.setKilometraje(o.getKilometraje());
        form.setManoObra(o.getManoObra());
        form.setNivelCombustible(o.getNivelCombustible());
        form.setDanosVisibles(o.getDanosVisibles());
        form.setObjetosDejados(o.getObjetosDejados());
        form.setMecanicoId(o.getMecanico() == null ? null : o.getMecanico().getId());
        model.addAttribute("ordenForm", form);
        model.addAttribute("orden", o);
        cargarCamposRecepcion(model);
        return "ordenes/editar";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("ordenForm") OrdenForm form,
                             BindingResult resultado,
                             Authentication autenticacion,
                             Model model,
                             RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("orden", ordenService.obtener(id));
            cargarCamposRecepcion(model);
            return "ordenes/editar";
        }
        try {
            ordenService.actualizarDatos(id, form.aDatos(), autenticacion.getName());
        } catch (TallerException e) {
            model.addAttribute("orden", ordenService.obtener(id));
            model.addAttribute("mensajeError", e.getMessage());
            cargarCamposRecepcion(model);
            return "ordenes/editar";
        }
        redirect.addFlashAttribute("mensajeExito", "Orden actualizada.");
        return "redirect:/ordenes/" + id;
    }

    @PostMapping("/{id}/repuestos")
    public String agregarRepuesto(@PathVariable Long id,
                                  @RequestParam Long repuestoId,
                                  @RequestParam int cantidad,
                                  Authentication autenticacion,
                                  RedirectAttributes redirect) {
        return accion(id, redirect, "Repuesto agregado.",
                () -> ordenService.agregarRepuesto(id, repuestoId, cantidad, autenticacion.getName()));
    }

    @PostMapping("/{id}/repuestos/{lineaId}/quitar")
    public String quitarRepuesto(@PathVariable Long id, @PathVariable Long lineaId,
                                 Authentication autenticacion, RedirectAttributes redirect) {
        return accion(id, redirect, "Repuesto quitado. Las unidades volvieron al stock.",
                () -> ordenService.quitarRepuesto(id, lineaId, autenticacion.getName()));
    }

    @PostMapping("/{id}/avanzar")
    public String avanzar(@PathVariable Long id, Authentication autenticacion, RedirectAttributes redirect) {
        return accion(id, redirect, "Estado actualizado.", () -> ordenService.avanzar(id, autenticacion.getName()));
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, Authentication autenticacion, RedirectAttributes redirect) {
        return accion(id, redirect, "Cancelada. Los repuestos volvieron al stock.",
                () -> ordenService.cancelar(id, autenticacion.getName()));
    }

    @PostMapping("/{id}/recordatorio")
    public String recordatorio(@PathVariable Long id,
                               @RequestParam("proximaFecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                               @RequestParam(value = "proximoKm", required = false) Integer km,
                               Authentication autenticacion, RedirectAttributes redirect) {
        return accion(id, redirect, "Próximo servicio programado.",
                () -> ordenService.programarRecordatorio(id, fecha, km, autenticacion.getName()));
    }

    @PostMapping("/{id}/pagos")
    public String registrarPago(@PathVariable Long id,
                                @RequestParam BigDecimal monto,
                                @RequestParam MetodoPago metodo,
                                @RequestParam(required = false) String nota,
                                Authentication autenticacion, RedirectAttributes redirect) {
        return accion(id, redirect, "Abono registrado.",
                () -> pagoService.registrar(id, monto, metodo, nota, autenticacion.getName()));
    }

    @PostMapping("/{id}/pagos/{pagoId}/anular")
    @PreAuthorize("hasRole('ADMIN')")
    public String anularPago(@PathVariable Long id, @PathVariable Long pagoId,
                             Authentication autenticacion, RedirectAttributes redirect) {
        return accion(id, redirect, "Abono anulado.", () -> pagoService.anular(id, pagoId, autenticacion.getName()));
    }

    @PostMapping("/{id}/fotos")
    public String subirFoto(@PathVariable Long id, @RequestParam("foto") MultipartFile foto,
                            Authentication autenticacion, RedirectAttributes redirect) {
        return accion(id, redirect, "Foto guardada.", () -> fotoService.subir(id, foto, autenticacion.getName()));
    }

    @GetMapping("/{id}/fotos/{fotoId}")
    public ResponseEntity<org.springframework.core.io.Resource> verFoto(@PathVariable Long id, @PathVariable Long fotoId) {
        FotoService.Archivo archivo = fotoService.cargar(id, fotoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipo()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(archivo.recurso());
    }

    @PostMapping("/{id}/fotos/{fotoId}/eliminar")
    @PreAuthorize("hasRole('ADMIN')")
    public String eliminarFoto(@PathVariable Long id, @PathVariable Long fotoId,
                               Authentication autenticacion, RedirectAttributes redirect) {
        return accion(id, redirect, "Foto eliminada.", () -> fotoService.eliminar(id, fotoId, autenticacion.getName()));
    }

    /** Ejecuta la accion y vuelve al detalle de la orden con un mensaje de exito o de error. */
    private String accion(Long id, RedirectAttributes redirect, String exito, Runnable tarea) {
        try {
            tarea.run();
            redirect.addFlashAttribute("mensajeExito", exito);
        } catch (TallerException e) {
            redirect.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/ordenes/" + id;
    }

    private void cargarListas(Model model) {
        model.addAttribute("vehiculos", vehiculoService.listarTodos());
        model.addAttribute("repuestos", repuestoService.listarDisponibles());
        cargarCamposRecepcion(model);
    }

    private void cargarCamposRecepcion(Model model) {
        model.addAttribute("niveles", NivelCombustible.values());
        model.addAttribute("mecanicos", usuarioService.listarActivos());
    }
}
