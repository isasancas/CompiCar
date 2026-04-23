package com.compicar.viaje.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ViajeDTO {
    private Long id;
    private LocalDateTime fechaHoraSalida;
    private String estado;
    private Integer plazasDisponibles;
    private BigDecimal precio;
    private VehiculoDTO vehiculo;
    private List<ParadaDTO> paradas;
    private String slug;
    private String conductorNombre;
    private String conductorSlug;

    public ViajeDTO() {
    }

    public ViajeDTO(Long id, LocalDateTime fechaHoraSalida, String estado, Integer plazasDisponibles,
                   BigDecimal precio, VehiculoDTO vehiculo, List<ParadaDTO> paradas, String slug,
                   String conductorNombre, String conductorSlug) {
        this.id = id;
        this.fechaHoraSalida = fechaHoraSalida;
        this.estado = estado;
        this.plazasDisponibles = plazasDisponibles;
        this.precio = precio;
        this.vehiculo = vehiculo;
        this.paradas = paradas;
        this.slug = slug;
        this.conductorNombre = conductorNombre;
        this.conductorSlug = conductorSlug;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFechaHoraSalida() {
        return fechaHoraSalida;
    }

    public void setFechaHoraSalida(LocalDateTime fechaHoraSalida) {
        this.fechaHoraSalida = fechaHoraSalida;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getPlazasDisponibles() {
        return plazasDisponibles;
    }

    public void setPlazasDisponibles(Integer plazasDisponibles) {
        this.plazasDisponibles = plazasDisponibles;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public VehiculoDTO getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(VehiculoDTO vehiculo) {
        this.vehiculo = vehiculo;
    }

    public List<ParadaDTO> getParadas() {
        return paradas;
    }

    public void setParadas(List<ParadaDTO> paradas) {
        this.paradas = paradas;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getConductorNombre() {
        return conductorNombre;
    }

    public void setConductorNombre(String conductorNombre) {
        this.conductorNombre = conductorNombre;
    }

    public String getConductorSlug() {
        return conductorSlug;
    }

    public void setConductorSlug(String conductorSlug) {
        this.conductorSlug = conductorSlug;
    }
}