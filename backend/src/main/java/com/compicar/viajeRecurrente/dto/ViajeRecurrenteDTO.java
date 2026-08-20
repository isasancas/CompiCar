package com.compicar.viajeRecurrente.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.compicar.parada.dto.ParadaDTO;
import com.compicar.reserva.dto.ReservaDTO;
import com.compicar.vehiculo.dto.VehiculoDTO;

public class ViajeRecurrenteDTO {

    private Long id;
    private String slug;
    private String checkin;
    private LocalDateTime fechaHoraSalida;
    private LocalDateTime fechaHoraFin;
    private String estado;
    private Integer plazasDisponibles;
    private BigDecimal precio;
    private Long viajePadreId;
    
    private Long conductorId;
    private String conductorNombre;
    private String conductorSlug;

    private VehiculoDTO vehiculo;
    private List<ParadaDTO> paradas;
    private List<ReservaDTO> reservas;

    public ViajeRecurrenteDTO() {
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getCheckin() { return checkin; }
    public void setCheckin(String checkin) { this.checkin = checkin; }

    public LocalDateTime getFechaHoraSalida() { return fechaHoraSalida; }
    public void setFechaHoraSalida(LocalDateTime fechaHoraSalida) { this.fechaHoraSalida = fechaHoraSalida; }

    public LocalDateTime getFechaHoraFin() { return fechaHoraFin; }
    public void setFechaHoraFin(LocalDateTime fechaHoraFin) { this.fechaHoraFin = fechaHoraFin; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Integer getPlazasDisponibles() { return plazasDisponibles; }
    public void setPlazasDisponibles(Integer plazasDisponibles) { this.plazasDisponibles = plazasDisponibles; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public Long getViajePadreId() { return viajePadreId; }
    public void setViajePadreId(Long viajePadreId) { this.viajePadreId = viajePadreId; }

    public Long getConductorId() { return conductorId; }
    public void setConductorId(Long conductorId) { this.conductorId = conductorId; }

    public String getConductorNombre() { return conductorNombre; }
    public void setConductorNombre(String conductorNombre) { this.conductorNombre = conductorNombre; }

    public String getConductorSlug() { return conductorSlug; }
    public void setConductorSlug(String conductorSlug) { this.conductorSlug = conductorSlug; }

    public VehiculoDTO getVehiculo() { return vehiculo; }
    public void setVehiculo(VehiculoDTO vehiculo) { this.vehiculo = vehiculo; }

    public List<ParadaDTO> getParadas() { return paradas; }
    public void setParadas(List<ParadaDTO> paradas) { this.paradas = paradas; }

    public List<ReservaDTO> getReservas() { return reservas; }
    public void setReservas(List<ReservaDTO> reservas) { this.reservas = reservas; }
}
