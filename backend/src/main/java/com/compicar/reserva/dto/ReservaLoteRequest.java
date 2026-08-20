package com.compicar.reserva.dto;

import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReservaLoteRequest(
    
    // Puede ser null si solo reserva recurrentes
    Long viajeId,
    
    // Puede ser null/vacío si solo reserva el viaje simple
    List<Long> viajeRecurrenteIds,

    @NotNull(message = "Debes indicar la parada de subida")
    Long paradaSubidaId,

    @NotNull(message = "Debes indicar la parada de bajada")
    Long paradaBajadaId,

    @NotNull(message = "La cantidad de plazas es obligatoria")
    @Positive(message = "La cantidad de plazas debe ser mayor a 0")
    Integer cantidadPlazas
) {}