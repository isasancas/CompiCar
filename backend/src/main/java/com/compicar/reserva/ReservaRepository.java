package com.compicar.reserva;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.compicar.persona.Persona;
import com.compicar.viaje.Viaje;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Query("SELECT r FROM Reserva r WHERE r.viaje.id = :viajeId")
    List<Reserva> findByViajeId(Long viajeId);

    @Query("SELECT r FROM Reserva r WHERE r.persona = :persona")
    List<Reserva> findByPersona(Persona persona);

    @Query("SELECT r FROM Reserva r WHERE r.viaje = :viaje AND r.estado != :estado")
    List<Reserva> findByViajeAndEstadoNot(Viaje viaje, EstadoReserva estado);

    @Query("SELECT r FROM Reserva r WHERE r.viaje.id = :viajeId AND r.persona.id = :personaId AND r.estado != :estado")
    Optional<Reserva> findByViajeIdAndPersonaIdAndEstadoNot(Long viajeId, Long personaId, EstadoReserva estado);
}

