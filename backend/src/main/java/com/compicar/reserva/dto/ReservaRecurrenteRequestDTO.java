package com.compicar.reserva.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ReservaRecurrenteRequestDTO {

    @NotEmpty(message = "Debes seleccionar al menos un viaje recurrente")
    private List<Long> viajeRecurrenteIds;

    @NotNull(message = "Debes indicar la parada de subida")
    private Long paradaSubidaId;

    @NotNull(message = "Debes indicar la parada de bajada")
    private Long paradaBajadaId;

    @Positive(message = "La cantidad de plazas debe ser mayor a 0")
    private int cantidadPlazas = 1;

    // Getters y Setters
    public List<Long> getViajeRecurrenteIds() {
        return viajeRecurrenteIds;
    }

    public void setViajeRecurrenteIds(List<Long> viajeRecurrenteIds) {
        this.viajeRecurrenteIds = viajeRecurrenteIds;
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

    public int getCantidadPlazas() {
        return cantidadPlazas;
    }

    public void setCantidadPlazas(int cantidadPlazas) {
        this.cantidadPlazas = cantidadPlazas;
    }    
}
