package com.compicar.pago;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.compicar.reserva.Reserva;
import com.stripe.Stripe;
import com.stripe.exception.InvalidRequestException;
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
                .setCustomer(reserva.getPersona().getStripePasajeroId())
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
     * Se llama si el viaje o la reserva se cancelan. Si el pago ya se capturó, se reembolsa;
     * si solo estaba autorizado, se cancela la autorización y se libera la retención.
     */
    public EstadoPago liberarFondos(String stripePaymentIntentId) throws StripeException {
        PaymentIntent intent = PaymentIntent.retrieve(stripePaymentIntentId);
        String status = intent.getStatus();

        // 1. Si ya se cobró, se realiza un REEMBOLSO
        if ("succeeded".equals(status)) {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(stripePaymentIntentId)
                    .build();
            Refund.create(params);
        } 
        // 2. Si solo está reservado/autorizado (requiere captura o método de pago), se CANCELA la retención
        else if (!"canceled".equals(status)) {
            try {
                intent.cancel();
            } catch (InvalidRequestException e) {
                // Si Stripe devuelve que ya estaba cancelado, lo ignoramos para no bloquear la BD
                if (e.getMessage() != null && e.getMessage().contains("status of canceled")) {
                    System.out.println("[STRIPE] El PaymentIntent " + stripePaymentIntentId + " ya figuraba como cancelado.");
                } else {
                    throw e;
                }
            }
        }

        return EstadoPago.REEMBOLSADO;
    }

    /**
     * Reembolsa solo la parte correspondiente a un viaje cancelado (si el dinero ya se capturó).
     */
    public void reembolsarParcial(String stripePaymentIntentId, BigDecimal montoAReembolsar) throws StripeException {
        long montoCentavos = montoAReembolsar.multiply(new BigDecimal(100)).longValue();
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(stripePaymentIntentId)
                .setAmount(montoCentavos)
                .build();
        Refund.create(params);
    }

    /**
     * Captura un importe inferior al autorizado (si se canceló algún viaje antes de la captura final).
     */
    public void confirmarCapturaParcial(String stripePaymentIntentId, BigDecimal montoACapturar) throws StripeException {
        PaymentIntent intent = PaymentIntent.retrieve(stripePaymentIntentId);
        if ("requires_capture".equals(intent.getStatus())) {
            long montoCentavos = montoACapturar.multiply(new BigDecimal(100)).longValue();
            com.stripe.param.PaymentIntentCaptureParams params = com.stripe.param.PaymentIntentCaptureParams.builder()
                    .setAmountToCapture(montoCentavos)
                    .build();
            intent.capture(params);
        }
    }

    /**
     * Transfiere los fondos de la plataforma a la cuenta conectada del conductor (Stripe Connect)
     */
    public void transferirAConductor(String stripeAccountId, BigDecimal monto) throws StripeException {
        long montoCentavos = monto.multiply(new BigDecimal(100)).longValue();

        com.stripe.param.TransferCreateParams params = com.stripe.param.TransferCreateParams.builder()
                .setAmount(montoCentavos)
                .setCurrency("eur")
                .setDestination(stripeAccountId)
                .build();

        com.stripe.model.Transfer.create(params);
    }
}