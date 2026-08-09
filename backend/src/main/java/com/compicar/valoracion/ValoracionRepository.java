package com.compicar.valoracion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {

    @Query("SELECT AVG(v.puntuacion) FROM Valoracion v WHERE v.valorado.id = :personaId")
    Double calcularReputacion(@Param("personaId") Long personaId);

    @Query("SELECT v FROM Valoracion v WHERE v.autor.id = :autorId")
    List<Valoracion> encontrarPorAutorId(@Param("autorId") Long autorId);

    @Query("SELECT v FROM Valoracion v WHERE v.valorado.id = :valoradoId")
    List<Valoracion> encontrarPorValoradoId(@Param("valoradoId") Long valoradoId);

    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM Valoracion v WHERE v.autor.id = :autorId AND v.viaje.id = :viajeId")
    boolean existePorAutorIdAndViajeId(@Param("autorId") Long autorId, @Param("viajeId") Long viajeId);
    
}
