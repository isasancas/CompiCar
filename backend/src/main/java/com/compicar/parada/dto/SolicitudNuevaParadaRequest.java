package com.compicar.parada.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SolicitudNuevaParadaRequest(
    Long reservaId,
    String localizacion,
    LocalDateTime fechaHora,
    BigDecimal latitud,
    BigDecimal longitud,
    boolean todaRecurrencia
) {
}
