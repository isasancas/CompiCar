package com.compicar.valoracion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {

    @Query("SELECT AVG(v.puntuacion) FROM Valoracion v WHERE v.valorado.id = :personaId")
    Double calcularReputacion(@Param("personaId") Long personaId);
    
}
