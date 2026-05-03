package com.compicar.pago;

import java.util.List;

import com.compicar.persona.Persona;
import com.compicar.reserva.Reserva;
import com.stripe.exception.StripeException;

public interface PagoService {
    
    Pago crearPago(String usuarioEmail, Long reservaId);
    List<Pago> obtenerPagosPorPersona(Persona persona);
    Pago obtenerPagoPorId(Long pagoId);
    Pago pagoCompletado(Long pagoId);
    Pago pagoFallido(Long pagoId);
    Pago pagoReembolsado(Long pagoId);
    Pago actualizarPago(String usuarioEmail, Long reservaId, Pago pagoActualizado);
    // 1. Iniciar la "congelación" (Auth)
    String crearIntentoDePago(Reserva reserva) throws StripeException;
    
    // 2. Cobrar definitivamente (Capture) al finalizar el viaje
    void capturarPago(String stripePaymentIntentId) throws StripeException;
    
    // 3. Devolver/Liberar el dinero si el viaje se cancela
    void cancelarPago(String stripePaymentIntentId) throws StripeException;
    
    // 4. Procesar notificaciones automáticas de Stripe (Webhooks)
    void procesarEventoWebhook(String payload, String sigHeader);
}
