package com.compicar.pago;

public enum EstadoPago {

    PENDIENTE,     // Se ha creado la reserva pero no se ha autorizado
    AUTORIZADO,    // El dinero está "congelado" (congelarlo es tu idea)
    CAPTURADO,     // El viaje terminó y el dinero se cobró
    REEMBOLSADO,   // El dinero se devolvió al pasajero
    FALLIDO        // Hubo un error en la tarjeta

}
