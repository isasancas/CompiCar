package com.compicar.reserva.dto;

import java.time.LocalDateTime;

public class ReservaDTO {

    private Long id;
    private String estado;
    private LocalDateTime fechaHoraReserva;
    private String nombrePasajero;
    private Long viajeId;
    private Long personaId;
    private String pasajeroSlug;
    private Long paradaSubidaId;
    private Long paradaBajadaId;
    private Integer cantidadPlazas;

    public ReservaDTO() {}

    public ReservaDTO(Long id, String estado, LocalDateTime fechaHoraReserva,
                      Long viajeId, Long personaId, String nombrePasajero, String pasajeroSlug,
                      Long paradaSubidaId, Long paradaBajadaId, Integer cantidadPlazas) {
        this.id = id;
        this.estado = estado;
        this.fechaHoraReserva = fechaHoraReserva;
        this.viajeId = viajeId;
        this.personaId = personaId;
        this.pasajeroSlug = pasajeroSlug;
        this.nombrePasajero = nombrePasajero;
        this.paradaSubidaId = paradaSubidaId;
        this.paradaBajadaId = paradaBajadaId;
        this.cantidadPlazas = cantidadPlazas;
    }

    // getters y setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaHoraReserva() {
        return fechaHoraReserva;
    }

    public void setFechaHoraReserva(LocalDateTime fechaHoraReserva) {
        this.fechaHoraReserva = fechaHoraReserva;
    }

    public Long getViajeId() {
        return viajeId;
    }

    public void setViajeId(Long viajeId) {
        this.viajeId = viajeId;
    }

    public Long getPersonaId() {
        return personaId;
    }

    public void setPersonaId(Long personaId) {
        this.personaId = personaId;
    }

    public Long getParadaSubidaId() {
        return paradaSubidaId;
    }

    public void setParadaSubidaId(Long paradaSubidaId) {
        this.paradaSubidaId = paradaSubidaId;
    }

    public Long getParadaBajadaId() {
        return paradaBajadaId;
    }

    public void setParadaBajadaId(Long paradaBajadaId) {
        this.paradaBajadaId = paradaBajadaId;
    }

    public Integer getCantidadPlazas() {
        return cantidadPlazas;
    }

    public void setCantidadPlazas(Integer cantidadPlazas) {
        this.cantidadPlazas = cantidadPlazas;
    }

    public String getNombrePasajero() {
        return nombrePasajero;
    }

    public void setNombrePasajero(String nombrePasajero) {
        this.nombrePasajero = nombrePasajero;
    }

    public String getPasajeroSlug() {
        return pasajeroSlug;
    }

    public void setPasajeroSlug(String pasajeroSlug) {
        this.pasajeroSlug = pasajeroSlug;
    }

}
