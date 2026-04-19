package com.compicar.viaje;

import java.util.List;
import java.util.Optional;

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

}
