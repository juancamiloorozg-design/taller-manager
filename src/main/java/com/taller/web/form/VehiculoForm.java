package com.taller.web.form;

import com.taller.service.VehiculoService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class VehiculoForm {

    @NotBlank(message = "La placa es obligatoria.")
    @Pattern(regexp = "(?i)[A-Z]{3}-?[0-9]{2}[0-9A-Z]", message = "Placa no válida. Ejemplo: ABC123 o ABC12D.")
    private String placa;

    @NotBlank(message = "La marca es obligatoria.")
    @Size(max = 60, message = "La marca no puede superar 60 caracteres.")
    private String marca;

    @NotBlank(message = "El modelo es obligatorio.")
    @Size(max = 60, message = "El modelo no puede superar 60 caracteres.")
    private String modelo;

    @NotNull(message = "El año es obligatorio.")
    @Min(value = 1950, message = "El año debe ser 1950 o posterior.")
    private Integer anio;

    @Size(max = 30, message = "El color no puede superar 30 caracteres.")
    private String color;

    @NotNull(message = "Selecciona un cliente.")
    private Long clienteId;

    public VehiculoService.Datos aDatos() {
        return new VehiculoService.Datos(placa, marca, modelo, anio == null ? 0 : anio, color, clienteId);
    }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
}
