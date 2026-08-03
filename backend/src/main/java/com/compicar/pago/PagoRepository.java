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
    
    @Query("SELECT p FROM Pago p WHERE p.reserva.persona = :persona")
    List<Pago> findByPersona(@Param("persona") Persona persona);

    @Query("SELECT p FROM Pago p WHERE p.reserva = :reserva")
    Optional<Pago> findByReserva(@Param("reserva") Reserva reserva);

    // Para encontrar el pago cuando Stripe nos envíe una notificación (Webhook)
    Optional<Pago> findByStripePaymentIntentId(String stripePaymentIntentId);
    
    // Para obtener el pago asociado a una reserva específica
    Optional<Pago> findByReservaId(Long reservaId);
}
