package com.compicar.viajeRecurrente;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.compicar.viaje.EstadoViaje;

public interface ViajeRecurrenteRepository extends JpaRepository<ViajeRecurrente, Long> {

    Optional<ViajeRecurrente> findBySlug(String slug);
    List<ViajeRecurrente> findByViajePadreId(Long viajePadreId);
    boolean existsBySlug(String slug);
    List<ViajeRecurrente> findByEstadoAndFechaHoraSalidaBefore(EstadoViaje estado, LocalDateTime fechaHora);
    
}
