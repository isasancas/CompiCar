package com.compicar.reserva;

import java.util.List;

import com.compicar.persona.Persona;
import com.compicar.reserva.dto.ReservaDTO;
import com.compicar.reserva.dto.ReservaRequest;

public interface ReservaService {
    
    String crearReserva(String usuarioEmail, Long viajeId, Integer plazasSolicitadas, Long paradaSubidaId, Long paradaBajadaId);
    Reserva cancelarReserva(String usuarioEmail, Long reservaId);
    List<ReservaDTO> obtenerReservasPorPersona(Persona persona);
    List<Reserva> obtenerReservasPorViaje(Long viajeId);
    Reserva actualizarReserva(String usuarioEmail, Long reservaId, ReservaRequest reserva);
    Reserva obtenerReservaPorId(Long reservaId);
    Reserva reservaConfirmada(String conductorEmail, Long reservaId);
    Reserva reservaNoPresentado(Long reservaId);
    Reserva marcarNoPresentadoPorConductor(String usuarioEmail, Long reservaId);
    List<Reserva> obtenerReservasComoConductor(String conductorEmail);
    Reserva rechazarReserva(String conductorEmail, Long reservaId);
}
