package com.compicar.reserva.dto;

public record ReservaDTO(
    Long id,
    String nombrePasajero,
    Long pasajeroId,
    Integer cantidadPlazas,
    String estado
) {}