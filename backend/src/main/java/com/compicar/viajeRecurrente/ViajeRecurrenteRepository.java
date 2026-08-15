package com.compicar.viajeRecurrente;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.compicar.viaje.EstadoViaje;
import com.compicar.viaje.Viaje;

public interface ViajeRecurrenteRepository extends JpaRepository<ViajeRecurrente, Long> {

    Optional<ViajeRecurrente> findBySlug(String slug);
    List<ViajeRecurrente> findByViajePadreId(Long viajePadreId);
    boolean existsBySlug(String slug);
    List<ViajeRecurrente> findByEstadoAndFechaHoraSalidaBefore(EstadoViaje estado, LocalDateTime fechaHora);
    @Query("""
        SELECT DISTINCT v
        FROM ViajeRecurrente v
        LEFT JOIN FETCH v.paradas
        LEFT JOIN FETCH v.vehiculo
        WHERE v.plazasDisponibles > 0
        AND v.estado IN :estados
        ORDER BY v.fechaHoraSalida ASC
    """)
    List<ViajeRecurrente> buscarViajesPublicosSinFecha(@Param("estados") Set<EstadoViaje> estados);

    @Query("""
        SELECT DISTINCT v
        FROM ViajeRecurrente v
        LEFT JOIN FETCH v.paradas
        LEFT JOIN FETCH v.vehiculo
        WHERE v.plazasDisponibles > 0
        AND v.estado IN :estados
        AND v.fechaHoraSalida >= :inicio
        AND v.fechaHoraSalida < :fin
        ORDER BY v.fechaHoraSalida ASC
    """)
    List<ViajeRecurrente> buscarViajesPublicosConFecha(
        @Param("estados") Set<EstadoViaje> estados,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );
    
}
