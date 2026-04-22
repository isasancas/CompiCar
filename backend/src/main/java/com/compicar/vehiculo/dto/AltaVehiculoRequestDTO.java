package com.compicar.vehiculo.dto;

import com.compicar.vehiculo.TipoVehiculo;
import com.compicar.vehiculo.validaciones.AnioNoFuturo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class AltaVehiculoRequestDTO {

    @NotBlank(message = "La matricula es obligatoria")
    @Pattern(
        regexp = "^[0-9]{4}[A-Z]{3}$|^[A-Z]{1,2}[0-9]{4}[A-Z]{1,2}$",
        message = "Formato de matricula no valido"
    )
    private String matricula;

    @NotBlank(message = "La marca es obligatoria")
    private String marca;

    @NotBlank(message = "El modelo es obligatorio")
    private String modelo;

    @NotNull(message = "Las plazas son obligatorias")
    @Min(value = 1, message = "Las plazas deben ser al menos 1")
    @Max(value = 9, message = "Las plazas no pueden superar 9")
    private Integer plazas;

    @NotNull(message = "El consumo es obligatorio")
    @Min(value = 1, message = "El consumo debe ser mayor que 0")
    private Double consumo;

    @NotNull(message = "El anio es obligatorio")
    @Min(value = 1950, message = "El anio no es valido")
    @AnioNoFuturo(message = "El anio no puede ser mayor al actual")
    private Integer anio;

    @NotNull(message = "El tipo es obligatorio")
    private TipoVehiculo tipo;

    // Getters y setters
    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public Integer getPlazas() {
        return plazas;
    }

    public void setPlazas(Integer plazas) {
        this.plazas = plazas;
    }

    public Double getConsumo() {
        return consumo;
    }

    public void setConsumo(Double consumo) {
        this.consumo = consumo;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public TipoVehiculo getTipo() {
        return tipo;
    }

    public void setTipo(TipoVehiculo tipo) {
        this.tipo = tipo;
    }

}
