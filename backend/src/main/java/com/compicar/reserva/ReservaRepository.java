package com.compicar.reserva;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.compicar.persona.Persona;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    
    @Query("SELECT r FROM Reserva r WHERE r.viaje.id = :viajeId")
    List<Reserva> findByViajeId(@Param("viajeId") Long viajeId);

    @Query("SELECT r FROM Reserva r WHERE r.persona = :persona")
    List<Reserva> findByPersona(@Param("persona") Persona persona);

    @Query("SELECT r FROM Reserva r WHERE r.viaje.persona.email = :email AND r.estado = com.compicar.reserva.EstadoReserva.PENDIENTE")
    List<Reserva> findPendientesParaConductor(@Param("email") String email);

}
