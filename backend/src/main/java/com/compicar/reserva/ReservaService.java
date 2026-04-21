package com.compicar.reserva;

import java.util.List;

public interface ReservaService {
    
    Reserva crearReserva(String usuarioEmail, Long viajeId);
    Reserva cancelarReserva(String usuarioEmail, Long reservaId);
    List<Reserva> obtenerReservasPorPersona(String usuarioEmail);
    List<Reserva> obtenerReservasPorViaje(Long viajeId);
    Reserva actualizReserva(String usuarioEmail, Long reservaId, Reserva reserva);
    Reserva obtenerReservaPorId(Long reservaId);
    Reserva reservaConfirmada(Long reservaId);
    Reserva reservaNoPresentado(Long reservaId);
    
}
