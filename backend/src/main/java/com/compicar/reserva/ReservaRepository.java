package com.compicar.reserva;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.compicar.persona.Persona;
import com.compicar.viaje.Viaje;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // 1. Reservas de un viaje
    @Query("SELECT r FROM Reserva r WHERE r.viaje.id = :viajeId AND r.estado != com.compicar.reserva.EstadoReserva.PENDIENTE")
    List<Reserva> findByViajeId(@Param("viajeId") Long viajeId);

    // 2. Historial de un pasajero
    @Query("SELECT r FROM Reserva r WHERE r.persona = :persona AND r.estado != com.compicar.reserva.EstadoReserva.PENDIENTE")
    List<Reserva> findByPersona(@Param("persona") Persona persona);

    // 3. Consultas condicionales excluyendo un estado específico
    @Query("SELECT r FROM Reserva r WHERE r.viaje = :viaje AND r.estado != :estado")
    List<Reserva> findByViajeAndEstadoNot(@Param("viaje") Viaje viaje, @Param("estado") EstadoReserva estado);

    @Query("SELECT r FROM Reserva r WHERE r.viaje.id = :viajeId AND r.persona.id = :personaId AND r.estado != :estado")
    Optional<Reserva> findByViajeIdAndPersonaIdAndEstadoNot(@Param("viajeId") Long viajeId, @Param("personaId") Long personaId, @Param("estado") EstadoReserva estado);

    // 4. Reservas que le aparecen al conductor para gestionar
    @Query("SELECT r FROM Reserva r WHERE r.viaje.persona.email = :email AND r.estado = com.compicar.reserva.EstadoReserva.PAGADA")
    List<Reserva> findPendientesParaConductor(@Param("email") String email);

    // 5. Método para el Cron Job (Limpieza de reservas fantasma caducadas)
    List<Reserva> findByEstadoAndFechaHoraReservaBefore(EstadoReserva estado, LocalDateTime fechaLimite);

}

