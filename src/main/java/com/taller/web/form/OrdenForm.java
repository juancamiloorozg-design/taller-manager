package com.taller.web.form;

import com.taller.model.NivelCombustible;
import com.taller.service.OrdenServicioService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class OrdenForm {

    /** Solo se usa al crear. Al editar el vehiculo no cambia. */
    private Long vehiculoId;

    @NotBlank(message = "Describe el trabajo a realizar.")
    @Size(max = 500, message = "La descripción no puede superar 500 caracteres.")
    private String descripcion;

    @Min(value = 0, message = "El kilometraje no puede ser negativo.")
    @Max(value = 3000000, message = "Kilometraje fuera de rango.")
    private Integer kilometraje;

    @NotNull(message = "Ingresa la mano de obra (0 si no aplica).")
    @DecimalMin(value = "0.00", message = "La mano de obra no puede ser negativa.")
    @Digits(integer = 10, fraction = 2, message = "Valor fuera de rango.")
    private BigDecimal manoObra = BigDecimal.ZERO;

    private NivelCombustible nivelCombustible;

    @Size(max = 500, message = "Los daños no pueden superar 500 caracteres.")
    private String danosVisibles;

    @Size(max = 500, message = "El texto no puede superar 500 caracteres.")
    private String objetosDejados;

    private Long mecanicoId;

    /** Solo al crear: true si es una cotizacion que el cliente todavia debe aprobar. */
    private boolean cotizacion;

    /** repuestoId -> cantidad. Se envia como cantidades[5]=2. */
    private Map<Long, Integer> cantidades = new LinkedHashMap<>();

    public Long getVehiculoId() { return vehiculoId; }
    public void setVehiculoId(Long vehiculoId) { this.vehiculoId = vehiculoId; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getKilometraje() { return kilometraje; }
    public void setKilometraje(Integer kilometraje) { this.kilometraje = kilometraje; }

    public BigDecimal getManoObra() { return manoObra; }
    public void setManoObra(BigDecimal manoObra) { this.manoObra = manoObra; }

    public Map<Long, Integer> getCantidades() { return cantidades; }
    public void setCantidades(Map<Long, Integer> cantidades) { this.cantidades = cantidades; }

    public OrdenServicioService.DatosOrden aDatos() {
        return new OrdenServicioService.DatosOrden(descripcion, kilometraje, manoObra,
                nivelCombustible, danosVisibles, objetosDejados, mecanicoId);
    }

    public NivelCombustible getNivelCombustible() { return nivelCombustible; }
    public void setNivelCombustible(NivelCombustible nivelCombustible) { this.nivelCombustible = nivelCombustible; }

    public String getDanosVisibles() { return danosVisibles; }
    public void setDanosVisibles(String danosVisibles) { this.danosVisibles = danosVisibles; }

    public String getObjetosDejados() { return objetosDejados; }
    public void setObjetosDejados(String objetosDejados) { this.objetosDejados = objetosDejados; }

    public Long getMecanicoId() { return mecanicoId; }
    public void setMecanicoId(Long mecanicoId) { this.mecanicoId = mecanicoId; }

    public boolean isCotizacion() { return cotizacion; }
    public void setCotizacion(boolean cotizacion) { this.cotizacion = cotizacion; }
}
