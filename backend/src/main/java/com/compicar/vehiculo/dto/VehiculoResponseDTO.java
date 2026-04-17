package com.compicar.vehiculo.dto;

import com.compicar.vehiculo.TipoVehiculo;
import com.compicar.vehiculo.Vehiculo;

public class VehiculoResponseDTO {
    private Long id;
    private String matricula;
    private String marca;
    private String modelo;
    private Integer plazas;
    private Double consumo;
    private Integer anio;
    private TipoVehiculo tipo;

    public static VehiculoResponseDTO fromEntity(Vehiculo vehiculo) {
        VehiculoResponseDTO dto = new VehiculoResponseDTO();
        dto.setId(vehiculo.getId());
        dto.setMatricula(vehiculo.getMatricula());
        dto.setMarca(vehiculo.getMarca());
        dto.setModelo(vehiculo.getModelo());
        dto.setPlazas(vehiculo.getPlazas());
        dto.setConsumo(vehiculo.getConsumo());
        dto.setAnio(vehiculo.getAnio());
        dto.setTipo(vehiculo.getTipo());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
