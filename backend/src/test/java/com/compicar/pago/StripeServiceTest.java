package com.compicar.pago;

import com.compicar.persona.Persona;
import com.compicar.reserva.Reserva;
import com.stripe.Stripe;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.Transfer;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.TransferCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeServiceTest {

    private StripeService stripeService;

    @BeforeEach
    void setUp() {
        stripeService = new StripeService();
        ReflectionTestUtils.setField(stripeService, "secretKey", "sk_test_fake_key");
        stripeService.init();
    }

    @Test
    void init_estableceApiKey() {
        assertEquals("sk_test_fake_key", Stripe.apiKey);
    }

    @Test
    void crearAutorizacion_ok() throws Exception {
        Persona persona = new Persona();
        ReflectionTestUtils.setField(persona, "stripePasajeroId", "cus_123");

        Pago pago = mock(Pago.class);
        when(pago.getImporteTotal()).thenReturn(new BigDecimal("15.50"));

        Reserva reserva = new Reserva();
        reserva.setPersona(persona);
        reserva.setPago(pago);

        PaymentIntent mockIntent = mock(PaymentIntent.class);

        try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(mockIntent);

            PaymentIntent result = stripeService.crearAutorizacion(reserva);

            assertNotNull(result);
            mockedStatic.verify(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)));
        }
    }

    @Test
    void confirmarCaptura_estadoRequiresCapture_capturaExitosamente() throws Exception {
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("requires_capture");

        try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.retrieve("pi_123")).thenReturn(mockIntent);

            stripeService.confirmarCaptura("pi_123");

            verify(mockIntent).capture();
        }
    }

    @Test
    void confirmarCaptura_estadoNoCapturable_lanzaExcepcion() throws Exception {
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("succeeded");

        try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.retrieve("pi_123")).thenReturn(mockIntent);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> stripeService.confirmarCaptura("pi_123"));

            assertTrue(ex.getMessage().contains("El pago no está en un estado captable"));
            verify(mockIntent, never()).capture();
        }
    }

    @Test
    void liberarFondos_estadoSucceeded_creaReembolso() throws Exception {
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("succeeded");

        try (MockedStatic<PaymentIntent> mockedIntent = mockStatic(PaymentIntent.class);
             MockedStatic<Refund> mockedRefund = mockStatic(Refund.class)) {

            mockedIntent.when(() -> PaymentIntent.retrieve("pi_123")).thenReturn(mockIntent);

            EstadoPago resultado = stripeService.liberarFondos("pi_123");

            assertEquals(EstadoPago.REEMBOLSADO, resultado);
            mockedRefund.verify(() -> Refund.create(any(RefundCreateParams.class)));
        }
    }

    @Test
    void liberarFondos_estadoRequiresCapture_cancelaIntent() throws Exception {
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("requires_capture");

        try (MockedStatic<PaymentIntent> mockedIntent = mockStatic(PaymentIntent.class)) {
            mockedIntent.when(() -> PaymentIntent.retrieve("pi_123")).thenReturn(mockIntent);

            EstadoPago resultado = stripeService.liberarFondos("pi_123");

            assertEquals(EstadoPago.REEMBOLSADO, resultado);
            verify(mockIntent).cancel();
        }
    }

    @Test
    void liberarFondos_statusOfCanceledException_ignoraExcepcion() throws Exception {
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("requires_capture");

        InvalidRequestException ex = mock(InvalidRequestException.class);
        when(ex.getMessage()).thenReturn("This PaymentIntent has a status of canceled and cannot be canceled.");

        when(mockIntent.cancel()).thenThrow(ex);

        try (MockedStatic<PaymentIntent> mockedIntent = mockStatic(PaymentIntent.class)) {
            mockedIntent.when(() -> PaymentIntent.retrieve("pi_123")).thenReturn(mockIntent);

            EstadoPago resultado = stripeService.liberarFondos("pi_123");

            assertEquals(EstadoPago.REEMBOLSADO, resultado);
            verify(mockIntent).cancel();
        }
    }

    @Test
    void liberarFondos_otraInvalidRequestException_relanzaExcepcion() throws Exception {
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("requires_capture");

        InvalidRequestException ex = mock(InvalidRequestException.class);
        when(ex.getMessage()).thenReturn("Invalid parameters");

        when(mockIntent.cancel()).thenThrow(ex);

        try (MockedStatic<PaymentIntent> mockedIntent = mockStatic(PaymentIntent.class)) {
            mockedIntent.when(() -> PaymentIntent.retrieve("pi_123")).thenReturn(mockIntent);

            assertThrows(InvalidRequestException.class, () -> stripeService.liberarFondos("pi_123"));
        }
    }

    @Test
    void reembolsarParcial_ok() throws Exception {
        try (MockedStatic<Refund> mockedRefund = mockStatic(Refund.class)) {
            stripeService.reembolsarParcial("pi_123", new BigDecimal("10.00"));

            mockedRefund.verify(() -> Refund.create(any(RefundCreateParams.class)));
        }
    }

    @Test
    void confirmarCapturaParcial_estadoRequiresCapture_capturaParcial() throws Exception {
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("requires_capture");

        try (MockedStatic<PaymentIntent> mockedIntent = mockStatic(PaymentIntent.class)) {
            mockedIntent.when(() -> PaymentIntent.retrieve("pi_123")).thenReturn(mockIntent);

            stripeService.confirmarCapturaParcial("pi_123", new BigDecimal("12.00"));

            verify(mockIntent).capture(any(PaymentIntentCaptureParams.class));
        }
    }

    @Test
    void transferirAConductor_ok() throws Exception {
        try (MockedStatic<Transfer> mockedTransfer = mockStatic(Transfer.class)) {
            stripeService.transferirAConductor("acct_123", new BigDecimal("25.00"));

            mockedTransfer.verify(() -> Transfer.create(any(TransferCreateParams.class)));
        }
    }
}