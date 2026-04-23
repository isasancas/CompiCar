package com.compicar.reserva;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.compicar.persona.Persona;
import com.compicar.viaje.Viaje;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByViajeId(Long viajeId);

    List<Reserva> findByPersona(Persona persona);

    List<Reserva> findByViajeAndEstadoNot(Viaje viaje, EstadoReserva estado);

    Optional<Reserva> findByViajeIdAndPersonaIdAndEstadoNot(Long viajeId, Long personaId, EstadoReserva estado);
}
