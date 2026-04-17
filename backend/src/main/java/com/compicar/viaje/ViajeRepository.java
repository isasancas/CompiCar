package com.compicar.viaje;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ViajeRepository extends JpaRepository<Viaje, Long> {
    
    @Query("SELECT v FROM Viaje v")
    List<Viaje> findAllViajes();

    boolean existsByVehiculoId(Long vehiculoId);

}
