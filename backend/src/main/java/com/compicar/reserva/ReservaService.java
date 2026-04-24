package com.compicar.reserva;

import java.util.List;

import com.compicar.persona.Persona;

public interface ReservaService {

    Reserva crearReserva(String usuarioEmail, Long viajeId);
    Reserva cancelarReserva(String usuarioEmail, Long reservaId);
    List<ReservaDTO> obtenerReservasPorPersona(Persona persona);
    List<Reserva> obtenerReservasPorViaje(Long viajeId);
    Reserva actualizReserva(String usuarioEmail, Long reservaId, Reserva reserva);
    Reserva obtenerReservaPorId(Long reservaId);
    Reserva reservaConfirmada(Long reservaId);
    Reserva reservaNoPresentado(Long reservaId);
    Reserva marcarNoPresentadoPorConductor(String usuarioEmail, Long reservaId);
}
