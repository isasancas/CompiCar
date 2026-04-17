package com.compicar.vehiculo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    
    @Query("SELECT v FROM Vehiculo v WHERE v.persona.id = :personaId")
    List<Vehiculo> findByPersonaId(Long personaId);

    @Query("SELECT v FROM Vehiculo v WHERE v.id = :id AND v.persona.id = :personaId")
    Optional<Vehiculo> findByIdAndPersonaId(Long id, Long personaId);

    boolean existsByMatricula(String matricula);
    boolean existsByMatriculaAndIdNot(String matricula, Long id);
}
