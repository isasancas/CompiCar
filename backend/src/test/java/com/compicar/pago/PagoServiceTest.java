package com.compicar.pago;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.compicar.notificacion.NotificacionRepository;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.reserva.EstadoReserva;
import com.compicar.reserva.Reserva;
import com.compicar.reserva.ReservaRepository;
import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

import jakarta.persistence.EntityNotFoundException;

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
        // Given
        when(pagoRepository.findByStripePaymentIntentId("pi_123456")).thenReturn(Optional.of(pago));

        // When
        pagoService.capturarPago("pi_123456");

        // Then
        verify(stripeService).confirmarCaptura("pi_123456");
        assertEquals(EstadoPago.CAPTURADO, pago.getEstado());
        assertNotNull(pago.getFechaPago());
        verify(pagoRepository).save(pago);
    }

    @Test
    void testCapturarPago_PagoNoEncontrado() {
        // Given
        when(pagoRepository.findByStripePaymentIntentId("pi_inexistente")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> pagoService.capturarPago("pi_inexistente"))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage("Pago no encontrado");
    }

    @Test
    void testCrearIntentoDePago_ReservaSinId_LanzaExcepcion() {
        // Given
        reserva.setId(null);

        // When & Then
        assertThatThrownBy(() -> pagoService.crearIntentoDePago(reserva))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No se puede crear un pago para una reserva que aún no ha sido guardada (ID nulo).");
    }

    @Test
    void testCrearIntentoDePago_NuevaReserva_Exito() throws StripeException {
        // Given
        PaymentIntent mockIntent = new PaymentIntent();
        mockIntent.setId("pi_new_123");
        mockIntent.setClientSecret("secret_xyz");

        when(stripeService.crearAutorizacion(reserva)).thenReturn(mockIntent);

        // When
        String clientSecret = pagoService.crearIntentoDePago(reserva);

        // Then
        assertEquals("secret_xyz", clientSecret);
        verify(pagoRepository).save(any(Pago.class));
    }

    @Test
    void testObtenerPagosPorPersona_Exito() {
        // Given
        when(personaRepository.findById(1L)).thenReturn(Optional.of(personaPasajero));
        when(pagoRepository.findByPersona(personaPasajero)).thenReturn(List.of(pago));

        // When
        List<Pago> resultado = pagoService.obtenerPagosPorPersona(personaPasajero);

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }

    @Test
    void testActualizarPago_ErrorReservaNoPerteneceAUsuario() {
        // Given
        Persona otroUsuario = new Persona();
        otroUsuario.setId(99L);
        otroUsuario.setEmail("otro@test.com");

        when(personaRepository.findByEmail("otro@test.com")).thenReturn(Optional.of(otroUsuario));
        when(reservaRepository.findById(100L)).thenReturn(Optional.of(reserva));

        Pago pagoUpdate = new Pago();
        pagoUpdate.setEstado(EstadoPago.CAPTURADO);

        // When & Then
        assertThatThrownBy(() -> pagoService.actualizarPago("otro@test.com", 100L, pagoUpdate))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("La reserva no pertenece al usuario con email: otro@test.com");
    }

    @Test
    void testProcesarEventoWebhook_BypassAutorizado() {
        // Given
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

        // When
        pagoService.procesarEventoWebhook(jsonPayload, "fake");

        // Then
        assertEquals(EstadoPago.AUTORIZADO, pago.getEstado());
        assertEquals(EstadoReserva.PAGADA, reserva.getEstado());
        assertEquals(1, viaje.getPlazasDisponibles()); // Tenía 3, reservó 2 -> queda 1
        verify(viajeRepository).save(viaje);
        verify(reservaRepository).save(reserva);
        verify(notificacionRepository).save(any());
    }

    @Test
    void testProcesarEventoWebhook_BypassSobreaforo_CancelaReserva() {
        // Given
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

        // When
        pagoService.procesarEventoWebhook(jsonPayload, "fake");

        // Then
        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        verify(reservaRepository).save(reserva);
        verify(viajeRepository, never()).save(viaje);
    }

    @Test
    void testLiberarPagoProgresivoPorViaje_Exito() throws StripeException {
        // Given
        reserva.setEstado(EstadoReserva.PAGADA);
        reserva.setPago(pago);

        when(reservaRepository.findByViajeId(10L)).thenReturn(List.of(reserva));

        // When
        pagoService.liberarPagoProgresivoPorViaje(10L);

        // Then
        verify(stripeService).confirmarCaptura("pi_123456");
        assertEquals(EstadoPago.CAPTURADO, pago.getEstado());
        
        assertThat(pago.getImporteLiberadoConductor()).isEqualByComparingTo("27.00");
        assertThat(personaConductor.getFondosActuales()).isEqualByComparingTo("27.00");
        
        verify(stripeService).transferirAConductor(eq("acct_12345"), any(BigDecimal.class));
        verify(personaRepository).save(personaConductor);
        verify(pagoRepository).save(pago);
        verify(notificacionRepository).save(any());
    }
}
