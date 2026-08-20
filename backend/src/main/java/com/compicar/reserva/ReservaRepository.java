package com.compicar.reserva;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.compicar.persona.Persona;
import com.compicar.viaje.EstadoViaje;
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
    @Query("SELECT r FROM Reserva r " +
       "LEFT JOIN r.viaje v " +
       "LEFT JOIN v.persona p1 " +
       "LEFT JOIN r.viajeRecurrente vr " +
       "LEFT JOIN vr.persona p2 " +
       "LEFT JOIN vr.viajePadre vp " +
       "LEFT JOIN vp.persona p3 " +
       "WHERE r.estado = com.compicar.reserva.EstadoReserva.PAGADA AND (" +
       "  p1.email = :email " +
       "  OR p2.email = :email " +
       "  OR p3.email = :email" +
       ")")
    List<Reserva> findPendientesParaConductor(@Param("email") String email);

    // 5. Método para el Cron Job (Limpieza de reservas fantasma caducadas)
    List<Reserva> findByEstadoAndFechaHoraReservaBefore(EstadoReserva estado, LocalDateTime fechaLimite);

    // 6. Comprueba si existe una reserva activa (no cancelada) de un pasajero en un viaje
    boolean existsByPersonaIdAndViajeIdAndEstadoNot(Long personaId, Long viajeId, EstadoReserva estado);

    // 7. Buscar todas las reservas activas vinculadas a un ViajeRecurrente específico
    List<Reserva> findByViajeRecurrenteIdAndEstadoNot(Long viajeRecurrenteId, EstadoReserva estado);

    // 8. Buscar todas las reservas asociadas a un pago específico
    List<Reserva> findByPagoId(Long pagoId);

    // 9. Buscar reservas de un pago excluyendo un estado (ej. ignorar las CANCELADAS)
    List<Reserva> findByPagoIdAndEstadoNot(Long pagoId, EstadoReserva estado);

    // 10. Comprobar si un pasajero ya tiene reserva activa en un viaje RECURRENTE concreto
    boolean existsByPersonaIdAndViajeRecurrenteIdAndEstadoNot(Long personaId, Long viajeRecurrenteId, EstadoReserva estado);

    // 11. Próximo viaje simple como pasajero
    @Query("SELECT r FROM Reserva r WHERE r.persona = :persona " +
           "AND r.estado != :estadoReservaCancelado " +
           "AND r.viaje.fechaHoraSalida >= :fechaHora " +
           "AND r.viaje.estado != :estadoViajeCancelado " +
           "ORDER BY r.viaje.fechaHoraSalida ASC LIMIT 1")
    Optional<Reserva> findProximaReservaViaje(
        @Param("persona") Persona persona,
        @Param("fechaHora") LocalDateTime fechaHora,
        @Param("estadoReservaCancelado") EstadoReserva estadoReservaCancelado,
        @Param("estadoViajeCancelado") EstadoViaje estadoViajeCancelado
    );

    // 12. Próximo viaje recurrente como pasajero
    @Query("SELECT r FROM Reserva r WHERE r.persona = :persona " +
           "AND r.estado != :estadoReservaCancelado " +
           "AND r.viajeRecurrente.fechaHoraSalida >= :fechaHora " +
           "AND r.viajeRecurrente.estado != :estadoViajeCancelado " +
           "ORDER BY r.viajeRecurrente.fechaHoraSalida ASC LIMIT 1")
    Optional<Reserva> findProximaReservaViajeRecurrente(
        @Param("persona") Persona persona,
        @Param("fechaHora") LocalDateTime fechaHora,
        @Param("estadoReservaCancelado") EstadoReserva estadoReservaCancelado,
        @Param("estadoViajeCancelado") EstadoViaje estadoViajeCancelado
    );

}

