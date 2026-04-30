package com.compicar.reserva.dto;

public record ReservaRequest(
    Long viajeId, 
    Integer plazas, 
    Long paradaSubidaId, 
    Long paradaBajadaId
) {}
