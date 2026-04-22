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
}
