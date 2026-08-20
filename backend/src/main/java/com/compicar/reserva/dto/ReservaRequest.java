package com.compicar.reserva.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReservaRequest(
    Long viajeId, 
    @JsonProperty("cantidadPlazas") Integer plazas, 
    Long paradaSubidaId, 
    Long paradaBajadaId
) {}
