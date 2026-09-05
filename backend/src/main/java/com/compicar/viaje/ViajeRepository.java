package com.compicar.viaje;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.compicar.persona.Persona;

@Repository
public interface ViajeRepository extends JpaRepository<Viaje, Long> {

    @Query("SELECT v FROM Viaje v")
    List<Viaje> findAllViajes();

    boolean existsByVehiculoId(Long vehiculoId);

    @Query("SELECT v FROM Viaje v LEFT JOIN FETCH v.vehiculo LEFT JOIN FETCH v.persona WHERE v.persona.id = :personaId")
    List<Viaje> findByPersonaId(@Param("personaId") Long personaId);

    @Query("SELECT r.viaje FROM Reserva r WHERE r.persona.id = :personaId AND r.estado != 'CANCELADA' AND r.estado != 'NO_PRESENTADO' AND r.estado != 'RECHAZADA' AND r.viaje.estado = 'FINALIZADO'")
    List<Viaje> findViajesParticipadosByPersonaId(@Param("personaId") Long personaId);

    @Query("SELECT DISTINCT v FROM Viaje v LEFT JOIN v.reservas r "
        + "WHERE v.estado = 'FINALIZADO' AND (v.persona.id = :personaId "
        + "OR (r.persona.id = :personaId AND r.estado != 'CANCELADA' "
        + "AND r.estado != 'NO_PRESENTADO' AND r.estado != 'RECHAZADA'))")
    List<Viaje> findViajesFinalizadosPorUsuarioIncluyendoConductor(@Param("personaId") Long personaId);

    Optional<Viaje> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Viaje> findByPersonaSlugOrderByFechaHoraSalidaDesc(String personaSlug);

    List<Viaje> findByEstadoAndFechaHoraSalidaBefore(EstadoViaje estado, LocalDateTime limite);

    @Query("""
        SELECT DISTINCT v
        FROM Viaje v
        LEFT JOIN FETCH v.paradas
        LEFT JOIN FETCH v.vehiculo
        WHERE v.plazasDisponibles > 0
        AND v.estado IN :estados
        ORDER BY v.fechaHoraSalida ASC
    """)
    List<Viaje> buscarViajesPublicosSinFecha(@Param("estados") Set<EstadoViaje> estados);

    @Query("""
        SELECT DISTINCT v
        FROM Viaje v
        LEFT JOIN FETCH v.paradas
        LEFT JOIN FETCH v.vehiculo
        WHERE v.plazasDisponibles > 0
        AND v.estado IN :estados
        AND v.fechaHoraSalida >= :inicio
        AND v.fechaHoraSalida < :fin
        ORDER BY v.fechaHoraSalida ASC
    """)
    List<Viaje> buscarViajesPublicosConFecha(
        @Param("estados") Set<EstadoViaje> estados,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    // Próximo viaje simple como conductor
    @Query("SELECT v FROM Viaje v WHERE v.persona = :persona " +
           "AND v.fechaHoraSalida >= :fechaHora " +
           "AND v.estado != :estadoCancelado " +
           "ORDER BY v.fechaHoraSalida ASC LIMIT 1")
    Optional<Viaje> findProximoViajeConductor(
        @Param("persona") Persona persona,
        @Param("fechaHora") LocalDateTime fechaHora,
        @Param("estadoCancelado") EstadoViaje estadoCancelado
    );
}