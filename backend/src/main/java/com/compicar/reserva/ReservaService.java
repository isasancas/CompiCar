package com.compicar.reserva;

import java.util.List;

public interface ReservaService {
    
    Reserva crearReserva(String usuarioEmail, Long viajeId, Integer plazasSolicitadas);
    Reserva cancelarReserva(String usuarioEmail, Long reservaId);
    List<Reserva> obtenerReservasPorPersona(String usuarioEmail);
    List<Reserva> obtenerReservasPorViaje(Long viajeId);
    Reserva actualizarReserva(String usuarioEmail, Long reservaId, Reserva reserva);
    Reserva obtenerReservaPorId(Long reservaId);
    Reserva reservaConfirmada(String conductorEmail, Long reservaId);
    Reserva reservaNoPresentado(Long reservaId);
    List<Reserva> obtenerReservasComoConductor(String conductorEmail);
    Reserva rechazarReserva(String conductorEmail, Long reservaId);
    
}
