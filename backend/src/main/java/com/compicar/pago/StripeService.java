package com.compicar.pago;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.compicar.reserva.Reserva;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;

import jakarta.annotation.PostConstruct;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    /**
     * PASO 1: Congelar el dinero (Autorización)
     * Se llama cuando el pasajero solicita la reserva.
     */
    public PaymentIntent crearAutorizacion(Reserva reserva) throws StripeException {
        // Stripe usa centavos: 10.50€ -> 1050
        long montoCentavos = reserva.getPago().getImporteTotal()
                .multiply(new BigDecimal(100)).longValue();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(montoCentavos)
                .setCurrency("eur")
                .setCustomer(reserva.getPersona().getStripeCustomerId())
                .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL) // Clave para congelar
                .build();

        return PaymentIntent.create(params);
    }

    /**
     * PASO 2: Cobrar definitivamente (Captura)
     * Se llama cuando el viaje ha terminado con éxito.
     */
    public void confirmarCaptura(String stripePaymentIntentId) throws StripeException {
        PaymentIntent intent = PaymentIntent.retrieve(stripePaymentIntentId);
        
        // Solo podemos capturar si el estado es 'requires_capture' (está congelado)
        if ("requires_capture".equals(intent.getStatus())) {
            intent.capture();
        } else {
            throw new IllegalStateException("El pago no está en un estado captable: " + intent.getStatus());
        }
    }

    /**
     * PASO 3: Cancelar y liberar fondos
     * Se llama si el viaje se cancela. El dinero vuelve al pasajero sin comisiones.
     */
    public void liberarFondos(String stripePaymentIntentId) throws StripeException {
        PaymentIntent intent = PaymentIntent.retrieve(stripePaymentIntentId);
        
        // Si aún no se ha cobrado, cancelamos la intención de pago
        if ("requires_capture".equals(intent.getStatus())) {
            intent.cancel();
        } else if ("succeeded".equals(intent.getStatus())) {
            // Si ya se había capturado por error, hay que hacer un Reembolso (Refund)
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(stripePaymentIntentId)
                    .build();
            Refund.create(params);
        }
    }
}