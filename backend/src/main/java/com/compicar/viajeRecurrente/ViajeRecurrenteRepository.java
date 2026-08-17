package com.compicar.viajeRecurrente;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.compicar.persona.Persona;
import com.compicar.viaje.EstadoViaje;

public interface ViajeRecurrenteRepository extends JpaRepository<ViajeRecurrente, Long> {

    Optional<ViajeRecurrente> findBySlug(String slug);
    List<ViajeRecurrente> findByViajePadreId(Long viajePadreId);
    boolean existsBySlug(String slug);
    List<ViajeRecurrente> findByEstadoAndFechaHoraSalidaBefore(EstadoViaje estado, LocalDateTime fechaHora);
    // Próximo viaje recurrente como conductor
    @Query("SELECT vr FROM ViajeRecurrente vr WHERE vr.persona = :persona " +
           "AND vr.fechaHoraSalida >= :fechaHora " +
           "AND vr.estado != :estadoCancelado " +
           "ORDER BY vr.fechaHoraSalida ASC LIMIT 1")
    Optional<ViajeRecurrente> findProximoViajeRecurrenteConductor(
        @Param("persona") Persona persona,
        @Param("fechaHora") LocalDateTime fechaHora,
        @Param("estadoCancelado") EstadoViaje estadoCancelado
    );
    
}
