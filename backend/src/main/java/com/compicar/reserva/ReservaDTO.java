package com.compicar.reserva;

import java.time.LocalDateTime;

public class ReservaDTO {

    private Long id;
    private String estado;
    private LocalDateTime fechaHoraReserva;

    private Long viajeId;
    private Long personaId;

    private Long paradaSubidaId;
    private Long paradaBajadaId;

    public ReservaDTO() {}

    public ReservaDTO(Long id, String estado, LocalDateTime fechaHoraReserva,
                      Long viajeId, Long personaId,
                      Long paradaSubidaId, Long paradaBajadaId) {
        this.id = id;
        this.estado = estado;
        this.fechaHoraReserva = fechaHoraReserva;
        this.viajeId = viajeId;
        this.personaId = personaId;
        this.paradaSubidaId = paradaSubidaId;
        this.paradaBajadaId = paradaBajadaId;
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

    
}
