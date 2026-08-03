package com.compicar.reserva.dto;

public record ReservaCreadaResponse(
    Long reservaId,
    String slug,
    String clientSecret
) {}