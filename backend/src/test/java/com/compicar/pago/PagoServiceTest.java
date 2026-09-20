package com.compicar.pago;

import com.compicar.correo.CorreoService;
import com.compicar.notificacion.NotificacionRepository;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.reserva.EstadoReserva;
import com.compicar.reserva.Reserva;
import com.compicar.reserva.ReservaRepository;
import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;
import com.compicar.viajeRecurrente.ViajeRecurrente;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private StripeService stripeService;

    @Mock
    private ViajeRepository viajeRepository;

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private CorreoService correoService;

    @InjectMocks
    private PagoServiceImpl pagoService;

    private Persona personaPasajero;
    private Persona personaConductor;
    private Viaje viaje;
    private Reserva reserva;
    private Pago pago;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pagoService, "endpointSecret", "whsec_test_secret");

        personaPasajero = new Persona();
        personaPasajero.setId(1L);
        personaPasajero.setNombre("Juan");
        personaPasajero.setEmail("pasajero@test.com");

        personaConductor = new Persona();
        personaConductor.setId(2L);
        personaConductor.setNombre("Carlos");
        personaConductor.setEmail("conductor@test.com");
        personaConductor.setFondosActuales(BigDecimal.ZERO);
        personaConductor.setFondosTotales(BigDecimal.ZERO);
        personaConductor.setStripeConductorId("acct_12345");

        viaje = new Viaje();
        viaje.setId(10L);
        viaje.setPrecio(new BigDecimal("15.00"));
        viaje.setPlazasDisponibles(3);
        viaje.setPersona(personaConductor);

        reserva = new Reserva();
        reserva.setId(100L);
        reserva.setCantidadPlazas(2);
        reserva.setEstado(EstadoReserva.PENDIENTE);
        reserva.setPersona(personaPasajero);
        reserva.setViaje(viaje);

        pago = new Pago();
        pago.setId(50L);
        pago.setReserva(reserva);
        pago.setStripePaymentIntentId("pi_123456");
        pago.setImporteTotal(new BigDecimal("30.00"));
        pago.setComision(new BigDecimal("3.00"));
        pago.setImporteConductor(new BigDecimal("27.00"));
        pago.setImporteLiberadoConductor(BigDecimal.ZERO);
        pago.setEstado(EstadoPago.PENDIENTE);
    }

    @Test
    void testCapturarPago_Exito() throws StripeException {
        when(pagoRepository.findByStripePaymentIntentId("pi_123456")).thenReturn(Optional.of(pago));

        pagoService.capturarPago("pi_123456");

        verify(stripeService).confirmarCaptura("pi_123456");
        assertEquals(EstadoPago.CAPTURADO, pago.getEstado());
        assertNotNull(pago.getFechaPago());
        verify(pagoRepository).save(pago);
    }

    @Test
    void testCapturarPago_PagoNoEncontrado() {
        when(pagoRepository.findByStripePaymentIntentId("pi_inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.capturarPago("pi_inexistente"))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage("Pago no encontrado");
    }

    @Test
    void testCancelarPago_Exito() throws StripeException {
        when(pagoRepository.findByStripePaymentIntentId("pi_123456")).thenReturn(Optional.of(pago));
        when(stripeService.liberarFondos("pi_123456")).thenReturn(EstadoPago.REEMBOLSADO);

        pagoService.cancelarPago("pi_123456");

        assertEquals(EstadoPago.REEMBOLSADO, pago.getEstado());
        verify(pagoRepository).save(pago);
    }

    @Test
    void testCrearIntentoDePago_ReservaSinId_LanzaExcepcion() {
        reserva.setId(null);

        assertThatThrownBy(() -> pagoService.crearIntentoDePago(reserva))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No se puede crear un pago para una reserva que aún no ha sido guardada (ID nulo).");
    }

    @Test
    void testCrearIntentoDePago_NuevaReserva_Exito() throws StripeException {
        PaymentIntent mockIntent = new PaymentIntent();
        mockIntent.setId("pi_new_123");
        mockIntent.setClientSecret("secret_xyz");

        when(stripeService.crearAutorizacion(reserva)).thenReturn(mockIntent);

        String clientSecret = pagoService.crearIntentoDePago(reserva);

        assertEquals("secret_xyz", clientSecret);
        verify(pagoRepository).save(any(Pago.class));
    }

    @Test
    void testObtenerPagosPorPersona_Exito() {
        when(personaRepository.findById(1L)).thenReturn(Optional.of(personaPasajero));
        when(pagoRepository.findByPersona(personaPasajero)).thenReturn(List.of(pago));

        List<Pago> resultado = pagoService.obtenerPagosPorPersona(personaPasajero);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }

    @Test
    void testObtenerPagosPorPersona_PersonaNoEncontrada() {
        when(personaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.obtenerPagosPorPersona(personaPasajero))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Persona no encontrada con ID: 1");
    }

    @Test
    void testObtenerPagoPorId_Exito() {
        when(pagoRepository.findById(50L)).thenReturn(Optional.of(pago));

        Pago result = pagoService.obtenerPagoPorId(50L);

        assertEquals(pago, result);
    }

    @Test
    void testObtenerPagoPorId_NoEncontrado() {
        when(pagoRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.obtenerPagoPorId(50L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Pago no encontrado con ID: 50");
    }

    @Test
    void testActualizarPago_Exito() {
        when(personaRepository.findByEmail("pasajero@test.com")).thenReturn(Optional.of(personaPasajero));
        when(reservaRepository.findById(100L)).thenReturn(Optional.of(reserva));
        when(pagoRepository.findByReserva(reserva)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(i -> i.getArgument(0));

        Pago update = new Pago();
        update.setImporteTotal(new BigDecimal("40.00"));
        update.setImporteConductor(new BigDecimal("32.00"));
        update.setComision(new BigDecimal("8.00"));
        update.setEstado(EstadoPago.CAPTURADO);

        Pago result = pagoService.actualizarPago("pasajero@test.com", 100L, update);

        assertEquals(new BigDecimal("40.00"), result.getImporteTotal());
        assertEquals(new BigDecimal("32.00"), result.getImporteConductor());
        assertEquals(new BigDecimal("8.00"), result.getComision());
        assertEquals(EstadoPago.CAPTURADO, result.getEstado());
    }

    @Test
    void testActualizarPago_PersonaNoEncontrada() {
        when(personaRepository.findByEmail("inexistente@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.actualizarPago("inexistente@test.com", 100L, new Pago()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Usuario no encontrado con email: inexistente@test.com");
    }

    @Test
    void testActualizarPago_ReservaNoEncontrada() {
        when(personaRepository.findByEmail("pasajero@test.com")).thenReturn(Optional.of(personaPasajero));
        when(reservaRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.actualizarPago("pasajero@test.com", 100L, new Pago()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Reserva no encontrada con ID: 100");
    }

    @Test
    void testActualizarPago_ErrorReservaNoPerteneceAUsuario() {
        Persona otroUsuario = new Persona();
        otroUsuario.setId(99L);
        otroUsuario.setEmail("otro@test.com");

        when(personaRepository.findByEmail("otro@test.com")).thenReturn(Optional.of(otroUsuario));
        when(reservaRepository.findById(100L)).thenReturn(Optional.of(reserva));

        Pago pagoUpdate = new Pago();
        pagoUpdate.setEstado(EstadoPago.CAPTURADO);

        assertThatThrownBy(() -> pagoService.actualizarPago("otro@test.com", 100L, pagoUpdate))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("La reserva no pertenece al usuario con email: otro@test.com");
    }

    @Test
    void testActualizarPago_PagoNoEncontrado() {
        when(personaRepository.findByEmail("pasajero@test.com")).thenReturn(Optional.of(personaPasajero));
        when(reservaRepository.findById(100L)).thenReturn(Optional.of(reserva));
        when(pagoRepository.findByReserva(reserva)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.actualizarPago("pasajero@test.com", 100L, new Pago()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Pago no encontrado para la reserva con ID: 100");
    }

    @Test
    void testPagoCompletado_Exito() {
        when(pagoRepository.findById(50L)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(pago)).thenReturn(pago);

        Pago result = pagoService.pagoCompletado(50L);

        assertEquals(EstadoPago.CAPTURADO, result.getEstado());
    }

    @Test
    void testPagoCompletado_NoEncontrado() {
        when(pagoRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.pagoCompletado(50L))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testPagoFallido_Exito() {
        when(pagoRepository.findById(50L)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(pago)).thenReturn(pago);

        Pago result = pagoService.pagoFallido(50L);

        assertEquals(EstadoPago.FALLIDO, result.getEstado());
    }

    @Test
    void testPagoFallido_NoEncontrado() {
        when(pagoRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.pagoFallido(50L))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testPagoReembolsado_Exito() {
        when(pagoRepository.findById(50L)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(pago)).thenReturn(pago);

        Pago result = pagoService.pagoReembolsado(50L);

        assertEquals(EstadoPago.REEMBOLSADO, result.getEstado());
    }

    @Test
    void testPagoReembolsado_NoEncontrado() {
        when(pagoRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.pagoReembolsado(50L))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testProcesarEventoWebhook_BypassAutorizado() {
        String jsonPayload = """
            {
                "type": "payment_intent.amount_capturable_updated",
                "data": {
                    "object": {
                        "id": "pi_123456"
                    }
                }
            }
            """;

        when(pagoRepository.findByStripePaymentIntentId("pi_123456")).thenReturn(Optional.of(pago));
        when(reservaRepository.findByPagoId(pago.getId())).thenReturn(List.of(reserva));

        pagoService.procesarEventoWebhook(jsonPayload, "fake");

        assertEquals(EstadoPago.AUTORIZADO, pago.getEstado());
        assertEquals(EstadoReserva.PAGADA, reserva.getEstado());
        assertEquals(1, viaje.getPlazasDisponibles());
        verify(viajeRepository).save(viaje);
        verify(reservaRepository).save(reserva);
        verify(notificacionRepository).save(any());
    }

    @Test
    void testProcesarEventoWebhook_BypassJsonError() {
        assertThatThrownBy(() -> pagoService.procesarEventoWebhook("invalid json", "fake"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageStartingWith("Error parseando el JSON de prueba");
    }

    @Test
    void testProcesarEventoWebhook_BypassTipoNoPaymentIntent() {
        String jsonPayload = """
            {
                "type": "customer.created",
                "data": { "object": { "id": "cus_123" } }
            }
            """;
        pagoService.procesarEventoWebhook(jsonPayload, "fake");
        verify(pagoRepository, never()).findByStripePaymentIntentId(anyString());
    }

    @Test
    void testProcesarEventoWebhook_FirmaInvalida() {
        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                .thenThrow(new SignatureVerificationException("Sig error", "sig"));

            assertThatThrownBy(() -> pagoService.procesarEventoWebhook("payload", "header_real"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Firma de Webhook inválida");
        }
    }

    @Test
    void testProcesarEventoWebhook_Produccion_PaymentSucceeded() {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getId()).thenReturn("pi_123456");

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(mockIntent));
        when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);

        when(pagoRepository.findByStripePaymentIntentId("pi_123456")).thenReturn(Optional.of(pago));

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(mockEvent);

            pagoService.procesarEventoWebhook("payload", "sig_real");

            assertEquals(EstadoPago.CAPTURADO, pago.getEstado());
            verify(pagoRepository).save(pago);
        }
    }

    @Test
    void testProcesarEventoWebhook_Produccion_DeserializacionFallida() throws Exception {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.empty());
        when(deserializer.deserializeUnsafe()).thenThrow(new EventDataObjectDeserializationException("Error", null));
        when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(mockEvent);

            assertThatThrownBy(() -> pagoService.procesarEventoWebhook("payload", "sig_real"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Error deserializando StripeObject");
        }
    }

    @Test
    void testProcesarEventoWebhook_Produccion_ObjetoNoPaymentIntent() {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

        StripeObject mockStripeObject = mock(StripeObject.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(mockStripeObject));
        when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(mockEvent);

            pagoService.procesarEventoWebhook("payload", "sig_real");

            verify(pagoRepository, never()).findByStripePaymentIntentId(anyString());
        }
    }

    @Test
    void testProcesarEventoWebhook_Produccion_TipoEventoOmitido() {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("charge.succeeded");

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString())).thenReturn(mockEvent);

            pagoService.procesarEventoWebhook("payload", "sig_real");

            verify(pagoRepository, never()).findByStripePaymentIntentId(anyString());
        }
    }

    @Test
    void testProcesarEventoWebhook_BypassSobreaforo_CancelaReserva() {
        viaje.setPlazasDisponibles(1);
        String jsonPayload = """
            {
                "type": "payment_intent.amount_capturable_updated",
                "data": {
                    "object": {
                        "id": "pi_123456"
                    }
                }
            }
            """;

        when(pagoRepository.findByStripePaymentIntentId("pi_123456")).thenReturn(Optional.of(pago));
        when(reservaRepository.findByPagoId(pago.getId())).thenReturn(List.of(reserva));

        pagoService.procesarEventoWebhook(jsonPayload, "fake");

        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        verify(reservaRepository).save(reserva);
        verify(viajeRepository, never()).save(viaje);
    }

    @Test
    void testProcesarEventoWebhook_PaymentFailed_ConReserva() {
        String jsonPayload = """
            {
                "type": "payment_intent.payment_failed",
                "data": { "object": { "id": "pi_123456" } }
            }
            """;

        when(pagoRepository.findByStripePaymentIntentId("pi_123456")).thenReturn(Optional.of(pago));

        pagoService.procesarEventoWebhook(jsonPayload, "fake");

        assertEquals(EstadoPago.FALLIDO, pago.getEstado());
        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        verify(reservaRepository).save(reserva);
    }

    @Test
    void testProcesarEventoWebhook_PaymentFailed_SinReserva() {
        Pago pagoSinReserva = new Pago();
        pagoSinReserva.setId(99L);
        pagoSinReserva.setEstado(EstadoPago.PENDIENTE);
        pagoSinReserva.setStripePaymentIntentId("pi_123456");

        String jsonPayload = """
            {
                "type": "payment_intent.payment_failed",
                "data": { "object": { "id": "pi_123456" } }
            }
            """;

        when(pagoRepository.findByStripePaymentIntentId("pi_123456")).thenReturn(Optional.of(pagoSinReserva));

        pagoService.procesarEventoWebhook(jsonPayload, "fake");

        assertEquals(EstadoPago.FALLIDO, pagoSinReserva.getEstado());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void testProcesarEventoWebhook_ViajeRecurrente_Exito() {
        ViajeRecurrente viajeRecurrente = mock(ViajeRecurrente.class);
        when(viajeRecurrente.getPlazasDisponibles()).thenReturn(5);
        when(viajeRecurrente.getPersona()).thenReturn(personaConductor);

        Reserva reservaRecurrente = new Reserva();
        reservaRecurrente.setId(200L);
        reservaRecurrente.setCantidadPlazas(2);
        reservaRecurrente.setEstado(EstadoReserva.PENDIENTE);
        reservaRecurrente.setPersona(personaPasajero);
        reservaRecurrente.setViajeRecurrente(viajeRecurrente);

        String jsonPayload = """
            {
                "type": "payment_intent.amount_capturable_updated",
                "data": { "object": { "id": "pi_123456" } }
            }
            """;

        when(pagoRepository.findByStripePaymentIntentId("pi_123456")).thenReturn(Optional.of(pago));
        when(reservaRepository.findByPagoId(pago.getId())).thenReturn(List.of(reservaRecurrente));

        pagoService.procesarEventoWebhook(jsonPayload, "fake");

        assertEquals(EstadoReserva.PAGADA, reservaRecurrente.getEstado());
        verify(viajeRecurrente).setPlazasDisponibles(3);
        verify(reservaRepository).save(reservaRecurrente);
    }

    @Test
    void testProcesarEventoWebhook_ViajeRecurrente_Sobreaforo() {
        ViajeRecurrente viajeRecurrente = mock(ViajeRecurrente.class);
        when(viajeRecurrente.getPlazasDisponibles()).thenReturn(1);
        when(viajeRecurrente.getPersona()).thenReturn(personaConductor);

        Reserva reservaRecurrente = new Reserva();
        reservaRecurrente.setId(200L);
        reservaRecurrente.setCantidadPlazas(2);
        reservaRecurrente.setEstado(EstadoReserva.PENDIENTE);
        reservaRecurrente.setPersona(personaPasajero);
        reservaRecurrente.setViajeRecurrente(viajeRecurrente);

        String jsonPayload = """
            {
                "type": "payment_intent.amount_capturable_updated",
                "data": { "object": { "id": "pi_123456" } }
            }
            """;

        when(pagoRepository.findByStripePaymentIntentId("pi_123456")).thenReturn(Optional.of(pago));
        when(reservaRepository.findByPagoId(pago.getId())).thenReturn(List.of(reservaRecurrente));

        pagoService.procesarEventoWebhook(jsonPayload, "fake");

        assertEquals(EstadoReserva.CANCELADA, reservaRecurrente.getEstado());
        verify(reservaRepository).save(reservaRecurrente);
    }

    @Test
    void testLiberarPagoProgresivoPorViaje_Exito() throws StripeException {
        reserva.setEstado(EstadoReserva.PAGADA);
        reserva.setPago(pago);

        when(reservaRepository.findByViajeId(10L)).thenReturn(List.of(reserva));

        pagoService.liberarPagoProgresivoPorViaje(10L);

        verify(stripeService).confirmarCaptura("pi_123456");
        assertEquals(EstadoPago.CAPTURADO, pago.getEstado());
        
        assertThat(pago.getImporteLiberadoConductor()).isEqualByComparingTo("27.00");
        assertThat(personaConductor.getFondosActuales()).isEqualByComparingTo("27.00");
        
        verify(stripeService).transferirAConductor(eq("acct_12345"), any(BigDecimal.class));
        verify(personaRepository).save(personaConductor);
        verify(pagoRepository).save(pago);
        verify(notificacionRepository).save(any());
        verify(correoService).sendPagoLiberadoConductor(anyString(), anyString(), any(BigDecimal.class), anyString(), anyString(), anyString());
    }

    @Test
    void testLiberarPagoProgresivo_ReservaNoPagada_Omitida() throws StripeException {
        reserva.setEstado(EstadoReserva.PENDIENTE);
        when(reservaRepository.findByViajeId(10L)).thenReturn(List.of(reserva));

        pagoService.liberarPagoProgresivoPorViaje(10L);

        verify(stripeService, never()).confirmarCaptura(anyString());
    }

    @Test
    void testLiberarPagoProgresivo_PagoSinStripeId_Omitido() throws StripeException {
        reserva.setEstado(EstadoReserva.PAGADA);
        pago.setStripePaymentIntentId(null);
        reserva.setPago(pago);

        when(reservaRepository.findByViajeId(10L)).thenReturn(List.of(reserva));

        pagoService.liberarPagoProgresivoPorViaje(10L);

        verify(stripeService, never()).confirmarCaptura(anyString());
    }

    @Test
    void testLiberarPagoProgresivo_PagoYaCapturado() throws StripeException {
        reserva.setEstado(EstadoReserva.PAGADA);
        pago.setEstado(EstadoPago.CAPTURADO);
        reserva.setPago(pago);

        when(reservaRepository.findByViajeId(10L)).thenReturn(List.of(reserva));

        pagoService.liberarPagoProgresivoPorViaje(10L);

        verify(stripeService, never()).confirmarCaptura(anyString());
        verify(personaRepository).save(personaConductor);
    }

    @Test
    void testLiberarPagoProgresivo_ViajeRecurrente_ConParadasYCorreo() throws StripeException {
        ViajeRecurrente vr = mock(ViajeRecurrente.class);
        when(vr.getPrecio()).thenReturn(new BigDecimal("20.00"));
        when(vr.getPersona()).thenReturn(personaConductor);
        when(vr.getFechaHoraSalida()).thenReturn(LocalDateTime.of(2026, 9, 14, 10, 0));

        Reserva r = mock(Reserva.class, RETURNS_DEEP_STUBS);
        when(r.getEstado()).thenReturn(EstadoReserva.CONFIRMADA);
        when(r.getCantidadPlazas()).thenReturn(1);
        when(r.getPago()).thenReturn(pago);
        when(r.getViaje()).thenReturn(null);
        when(r.getViajeRecurrente()).thenReturn(vr);
        when(r.getParadaSubida().getLocalizacion()).thenReturn("Madrid, España");
        when(r.getParadaBajada().getLocalizacion()).thenReturn("Barcelona");

        when(reservaRepository.findByViajeId(10L)).thenReturn(List.of(r));

        pagoService.liberarPagoProgresivoPorViaje(10L);

        verify(correoService).sendPagoLiberadoConductor(
            eq("conductor@test.com"),
            eq("Carlos"),
            any(BigDecimal.class),
            eq("Madrid"),
            eq("Barcelona"),
            anyString()
        );
    }

    @Test
    void testLiberarPagoProgresivo_SinViajeNiRecurrente_Omitido() throws StripeException {
        Reserva r = mock(Reserva.class);
        when(r.getEstado()).thenReturn(EstadoReserva.PAGADA);
        when(r.getPago()).thenReturn(pago);
        when(r.getViaje()).thenReturn(null);
        when(r.getViajeRecurrente()).thenReturn(null);

        when(reservaRepository.findByViajeId(10L)).thenReturn(List.of(r));

        pagoService.liberarPagoProgresivoPorViaje(10L);

        verify(personaRepository, never()).save(any());
    }

    @Test
    void testLiberarPagoProgresivo_ConductorSinStripeConnect() throws StripeException {
        personaConductor.setStripeConductorId(null);
        reserva.setEstado(EstadoReserva.PAGADA);
        reserva.setPago(pago);

        when(reservaRepository.findByViajeId(10L)).thenReturn(List.of(reserva));

        pagoService.liberarPagoProgresivoPorViaje(10L);

        verify(stripeService, never()).transferirAConductor(anyString(), any());
        verify(personaRepository).save(personaConductor);
    }
}