package com.compicar.viaje.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.compicar.parada.dto.ParadaDTO;
import com.compicar.reserva.dto.ReservaDTO;
import com.compicar.vehiculo.dto.VehiculoDTO;
import com.compicar.viajeRecurrente.dto.ViajeRecurrenteDTO;

public class ViajeDTO {

    private Long id;
    private LocalDateTime fechaHoraSalida;
    private String estado;
    private Integer plazasDisponibles;
    private BigDecimal precio;
    private VehiculoDTO vehiculo;
    private List<ParadaDTO> paradas;
    private String slug;
    private Long conductorId;
    private String conductorNombre;
    private String conductorSlug;
    private List<ReservaDTO> reservas;
    private String checkin; // Añadido opcionalmente por si el viaje contiene el código de check-in

    private LocalDateTime fechaFinRecurrencia;
    private List<String> diasSemana;
    private List<ViajeRecurrenteDTO> viajesRecurrentes;

    public ViajeDTO() {
    }

    public ViajeDTO(Long id, LocalDateTime fechaHoraSalida, String estado, Integer plazasDisponibles,
                    BigDecimal precio, VehiculoDTO vehiculo, List<ParadaDTO> paradas, String slug,
                    Long conductorId, String conductorNombre, String conductorSlug, List<ReservaDTO> reservas,
                    LocalDateTime fechaFinRecurrencia, List<String> diasSemana, List<ViajeRecurrenteDTO> viajesRecurrentes, String checkin) {
        this.id = id;
        this.fechaHoraSalida = fechaHoraSalida;
        this.estado = estado;
        this.plazasDisponibles = plazasDisponibles;
        this.precio = precio;
        this.checkin = checkin;
        this.vehiculo = vehiculo;
        this.paradas = paradas;
        this.slug = slug;
        this.conductorId = conductorId;
        this.conductorNombre = conductorNombre;
        this.conductorSlug = conductorSlug;
        this.reservas = reservas;
        this.fechaFinRecurrencia = fechaFinRecurrencia;
        this.diasSemana = diasSemana;
        this.viajesRecurrentes = viajesRecurrentes;
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

    public String getCheckin() {
        return checkin;
    }

    public void setCheckin(String checkin) {
        this.checkin = checkin;
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

    public Long getConductorId() {
        return conductorId;
    }

    public void setConductorId(Long conductorId) {
        this.conductorId = conductorId;
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

    public List<ReservaDTO> getReservas() {
        return reservas;
    }

    public void setReservas(List<ReservaDTO> reservas) {
        this.reservas = reservas;
    }

    public LocalDateTime getFechaFinRecurrencia() {
        return fechaFinRecurrencia;
    }

    public void setFechaFinRecurrencia(LocalDateTime fechaFinRecurrencia) {
        this.fechaFinRecurrencia = fechaFinRecurrencia;
    }

    public List<String> getDiasSemana() {
        return diasSemana;
    }

    public void setDiasSemana(List<String> diasSemana) {
        this.diasSemana = diasSemana;
    }

    public List<ViajeRecurrenteDTO> getViajesRecurrentes() {
        return viajesRecurrentes;
    }

    public void setViajesRecurrentes(List<ViajeRecurrenteDTO> viajesRecurrentes) {
        this.viajesRecurrentes = viajesRecurrentes;
    }
}