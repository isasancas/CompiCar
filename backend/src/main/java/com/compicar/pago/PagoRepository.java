package com.compicar.pago;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.compicar.persona.Persona;
import com.compicar.reserva.Reserva;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    
    @Query("SELECT DISTINCT p FROM Pago p JOIN p.reservas r WHERE r.persona = :persona")
    List<Pago> findByPersona(@Param("persona") Persona persona);

    @Query("SELECT DISTINCT p FROM Pago p JOIN p.reservas r WHERE r = :reserva")
    Optional<Pago> findByReserva(@Param("reserva") Reserva reserva);

    Optional<Pago> findByStripePaymentIntentId(String stripePaymentIntentId);

    @Query("SELECT DISTINCT p FROM Pago p JOIN p.reservas r WHERE r.id = :reservaId")
    Optional<Pago> findByReservaId(@Param("reservaId") Long reservaId);
}
