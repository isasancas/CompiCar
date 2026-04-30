package com.compicar.viaje;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ViajeRepository extends JpaRepository<Viaje, Long> {

    @Query("SELECT v FROM Viaje v")
    List<Viaje> findAllViajes();

    boolean existsByVehiculoId(Long vehiculoId);

    List<Viaje> findByPersonaId(Long personaId);

    @Query("SELECT r.viaje FROM Reserva r WHERE r.persona.id = :personaId")
    List<Viaje> findViajesParticipadosByPersonaId(@Param("personaId") Long personaId);

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
}