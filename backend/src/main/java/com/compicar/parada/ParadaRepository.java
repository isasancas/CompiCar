package com.compicar.parada;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.compicar.viaje.Viaje;

@Repository
public interface ParadaRepository extends JpaRepository<Parada, Long> {

    @Query("SELECT p FROM Parada p WHERE p.viaje = :viaje")
    List<Parada> findByViaje(@Param("viaje") Viaje viaje);

    // Las 5 localizaciones más usadas por los usuarios para subir o bajar de un viaje
    @Query("SELECT p.localizacion, COUNT(p) as count FROM Parada p GROUP BY p.localizacion ORDER BY count DESC")
    List<Object[]> findTop5Localizaciones();

}
