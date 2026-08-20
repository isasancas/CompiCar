package com.compicar.reserva.dto;

import java.util.List;

public record ReservaRecurrenteRequest(
    List<Long> viajeRecurrenteIds,
    Integer plazas,
    Long paradaSubidaId,
    Long paradaBajadaId
) {}