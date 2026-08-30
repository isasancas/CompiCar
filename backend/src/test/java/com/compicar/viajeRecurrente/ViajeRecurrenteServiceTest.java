package com.compicar.viajeRecurrente;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.compicar.notificacion.Notificacion;
import com.compicar.notificacion.NotificacionRepository;
import com.compicar.pago.EstadoPago;
import com.compicar.pago.Pago;
import com.compicar.pago.PagoRepository;
import com.compicar.pago.StripeService;
import com.compicar.parada.Parada;
import com.compicar.parada.TipoParada;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.reserva.EstadoReserva;
import com.compicar.reserva.Reserva;
import com.compicar.reserva.ReservaRepository;
import com.compicar.vehiculo.Vehiculo;
import com.compicar.viaje.EstadoViaje;
import com.compicar.viaje.Viaje;
import com.compicar.viajeRecurrente.dto.ViajeRecurrenteDTO;
import com.stripe.exception.StripeException;

@ExtendWith(MockitoExtension.class)
class ViajeRecurrenteServiceTest {

    @Mock
    private ViajeRecurrenteRepository viajeRecurrenteRepository;
    @Mock
    private PersonaRepository personaRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private StripeService stripeService;

    @InjectMocks
    private ViajeRecurrenteServiceImpl viajeRecurrenteService;

    private Persona conductor;
    private Viaje viajePadre;
    private ViajeRecurrente viajeRecurrente;

    @BeforeEach
    void setUp() {
        conductor = new Persona();
        ReflectionTestUtils.setField(conductor, "id", 1L);
        conductor.setEmail("conductor@test.com");
        conductor.setNombre("Conductor Test");
        conductor.setSlug("conductor-test");
        conductor.setFondosActuales(BigDecimal.ZERO);
        conductor.setFondosTotales(BigDecimal.ZERO);

        Vehiculo vehiculo = new Vehiculo();
        ReflectionTestUtils.setField(vehiculo, "id", 10L);
        vehiculo.setMarca("Seat");
        vehiculo.setModelo("Ibiza");
        vehiculo.setMatricula("1234ABC");

        viajePadre = new Viaje();
        ReflectionTestUtils.setField(viajePadre, "id", 100L);
        viajePadre.setSlug("viaje-padre-slug");
        viajePadre.setFechaHoraSalida(LocalDateTime.of(2026, 6, 1, 8, 0));
        viajePadre.setFechaFinRecurrencia(LocalDateTime.of(2026, 6, 5, 23, 59));
        viajePadre.setDiasSemana(List.of("LUNES", "MIERCOLES"));
        viajePadre.setPlazasDisponibles(4);
        viajePadre.setPrecio(new BigDecimal("15.00"));
        viajePadre.setPersona(conductor);
        viajePadre.setVehiculo(vehiculo);

        viajeRecurrente = new ViajeRecurrente();
        ReflectionTestUtils.setField(viajeRecurrente, "id", 500L);
        viajeRecurrente.setSlug("viaje-recurrente-slug");
        viajeRecurrente.setEstado(EstadoViaje.PENDIENTE);
        viajeRecurrente.setFechaHoraSalida(LocalDateTime.of(2026, 6, 1, 8, 0));
        viajeRecurrente.setPrecio(new BigDecimal("15.00"));
        viajeRecurrente.setPlazasDisponibles(4);
        viajeRecurrente.setPersona(conductor);
        viajeRecurrente.setVehiculo(vehiculo);
        viajeRecurrente.setCheckin("ABC123");
    }

    // --- MAPEAR A DTO ---

    @Test
    void mapearADTO_nulo_retornaNull() {
        assertNull(viajeRecurrenteService.mapearADTO(null));
    }

    @Test
    void mapearADTO_ok() {
        ViajeRecurrenteDTO dto = viajeRecurrenteService.mapearADTO(viajeRecurrente);

        assertNotNull(dto);
        assertEquals(viajeRecurrente.getId(), dto.getId());
        assertEquals(viajeRecurrente.getSlug(), dto.getSlug());
        assertEquals(viajeRecurrente.getCheckin(), dto.getCheckin());
        assertEquals(conductor.getNombre(), dto.getConductorNombre());
        assertNotNull(dto.getVehiculo());
        assertEquals("Seat", dto.getVehiculo().getMarca());
    }

    @Test
    void mapearADTO_conParadasYReservas() {
        Parada p = mock(Parada.class);
        when(p.getId()).thenReturn(1L);
        when(p.getLocalizacion()).thenReturn("Madrid");
        when(p.getOrden()).thenReturn(1);

        TipoParada tipoMock = mock(TipoParada.class);
        when(tipoMock.toString()).thenReturn("ORIGEN");
        when(p.getTipo()).thenReturn(tipoMock);

        Reserva rActiva = new Reserva();
        ReflectionTestUtils.setField(rActiva, "id", 10L);
        rActiva.setEstado(EstadoReserva.CONFIRMADA);
        rActiva.setFechaHoraReserva(LocalDateTime.now());
        rActiva.setPersona(conductor);
        rActiva.setCantidadPlazas(2);

        Reserva rCancelada = new Reserva();
        ReflectionTestUtils.setField(rCancelada, "id", 11L);
        rCancelada.setEstado(EstadoReserva.CANCELADA);

        viajeRecurrente.setParadas(List.of(p));
        viajeRecurrente.setReservas(List.of(rActiva, rCancelada));
        viajeRecurrente.setViajePadre(viajePadre);

        ViajeRecurrenteDTO dto = viajeRecurrenteService.mapearADTO(viajeRecurrente);

        assertNotNull(dto);
        assertEquals(1, dto.getParadas().size());
        assertEquals("Madrid", dto.getParadas().get(0).getLocalizacion());
        assertEquals("ORIGEN", dto.getParadas().get(0).getTipo());
        assertEquals(1, dto.getReservas().size());
        assertEquals(10L, dto.getReservas().get(0).getId());
        assertEquals(100L, dto.getViajePadreId());
    }

    @Test
    void mapearADTO_vehiculoNull() {
        viajeRecurrente.setVehiculo(null);
        viajeRecurrente.setPersona(null);
        viajeRecurrente.setEstado(null);

        ViajeRecurrenteDTO dto = viajeRecurrenteService.mapearADTO(viajeRecurrente);

        assertNotNull(dto);
        assertNull(dto.getVehiculo());
        assertNull(dto.getConductorId());
        assertNull(dto.getEstado());
    }

    @Test
    void generarOcurrencias_datosIncompletos_retornaVacio() {
        viajePadre.setDiasSemana(null);
        assertTrue(viajeRecurrenteService.generarOcurrencias(viajePadre).isEmpty());
    }

    @Test
    void generarOcurrencias_diasSemanaInvalidos_retornaVacio() {
        viajePadre.setDiasSemana(List.of("INVALIDO"));
        assertTrue(viajeRecurrenteService.generarOcurrencias(viajePadre).isEmpty());
    }

    @Test
    void generarOcurrencias_exitoConParadasYSlugExistente() {
        Parada paradaPadre = new Parada();
        paradaPadre.setLocalizacion("Origen");
        paradaPadre.setOrden(1);
        paradaPadre.setFechaHora(LocalDateTime.of(2026, 6, 1, 8, 0));
        viajePadre.setParadas(List.of(paradaPadre));

        when(viajeRecurrenteRepository.existsBySlug(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        when(viajeRecurrenteRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<ViajeRecurrente> resultado = viajeRecurrenteService.generarOcurrencias(viajePadre);

        assertFalse(resultado.isEmpty());
        verify(viajeRecurrenteRepository).saveAll(anyList());
    }

    @Test
    void obtenerViajeRecurrentePorSlug_ok() {
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));

        ViajeRecurrenteDTO dto = viajeRecurrenteService.obtenerViajeRecurrentePorSlug("viaje-recurrente-slug");

        assertNotNull(dto);
        assertEquals("viaje-recurrente-slug", dto.getSlug());
    }

    @Test
    void obtenerViajeRecurrentePorSlug_noEncontrado_lanza404() {
        when(viajeRecurrenteRepository.findBySlug("inexistente")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.obtenerViajeRecurrentePorSlug("inexistente"));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void iniciarViajeRecurrente_ok() {
        viajeRecurrente.setFechaHoraSalida(LocalDateTime.now().minusHours(1));
        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(viajeRecurrenteRepository.save(any(ViajeRecurrente.class))).thenAnswer(inv -> inv.getArgument(0));

        ViajeRecurrenteDTO dto = viajeRecurrenteService.iniciarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug");

        assertNotNull(dto);
        assertEquals(EstadoViaje.INICIADO.toString(), dto.getEstado());
    }

    @Test
    void iniciarViajeRecurrente_usuarioNoEncontrado_lanza401() {
        when(personaRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.iniciarViajeRecurrente("noexiste@test.com", "slug"));

        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void iniciarViajeRecurrente_noEsConductor_lanza403() {
        Persona otroUsuario = new Persona();
        ReflectionTestUtils.setField(otroUsuario, "id", 99L);
        otroUsuario.setEmail("otro@test.com");

        when(personaRepository.findByEmail("otro@test.com")).thenReturn(Optional.of(otroUsuario));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.iniciarViajeRecurrente("otro@test.com", "viaje-recurrente-slug"));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void iniciarViajeRecurrente_yaIniciado_lanza400() {
        viajeRecurrente.setEstado(EstadoViaje.INICIADO);

        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.iniciarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug"));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void iniciarViajeRecurrente_fechaFutura_lanza400() {
        viajeRecurrente.setFechaHoraSalida(LocalDateTime.now().plusHours(2));

        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.iniciarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug"));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void confirmarCheckinRecurrente_ok() {
        viajeRecurrente.setEstado(EstadoViaje.INICIADO);
        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(viajeRecurrenteRepository.save(any(ViajeRecurrente.class))).thenAnswer(inv -> inv.getArgument(0));

        ViajeRecurrenteDTO dto = viajeRecurrenteService.confirmarCheckinRecurrente("conductor@test.com", "viaje-recurrente-slug", "ABC123");

        assertNotNull(dto);
        assertEquals(EstadoViaje.EN_CURSO.toString(), dto.getEstado());
    }

    @Test
    void confirmarCheckinRecurrente_checkinInvalido_lanza400() {
        viajeRecurrente.setEstado(EstadoViaje.INICIADO);
        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.confirmarCheckinRecurrente("conductor@test.com", "viaje-recurrente-slug", "MAL"));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void confirmarCheckinRecurrente_noIniciado_lanza400() {
        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.confirmarCheckinRecurrente("conductor@test.com", "viaje-recurrente-slug", "ABC123"));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void finalizarViajeRecurrente_ok() {
        viajeRecurrente.setEstado(EstadoViaje.INICIADO);
        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(reservaRepository.findByViajeRecurrenteIdAndEstadoNot(anyLong(), any())).thenReturn(List.of());
        when(viajeRecurrenteRepository.save(any(ViajeRecurrente.class))).thenAnswer(inv -> inv.getArgument(0));

        ViajeRecurrenteDTO dto = viajeRecurrenteService.finalizarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug");

        assertNotNull(dto);
        assertEquals(EstadoViaje.FINALIZADO.toString(), dto.getEstado());
    }

    @Test
    void finalizarViajeRecurrente_pagoNoCapturado_ejecutaCaptura() throws Exception {
        viajeRecurrente.setEstado(EstadoViaje.EN_CURSO);

        Pago pago = new Pago();
        pago.setStripePaymentIntentId("pi_123");
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setImporteLiberadoConductor(BigDecimal.ZERO);

        Reserva reserva = new Reserva();
        ReflectionTestUtils.setField(reserva, "id", 20L);
        reserva.setCantidadPlazas(2);
        reserva.setPago(pago);
        reserva.setPersona(conductor);

        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(reservaRepository.findByViajeRecurrenteIdAndEstadoNot(viajeRecurrente.getId(), EstadoReserva.CANCELADA))
                .thenReturn(List.of(reserva));
        when(viajeRecurrenteRepository.save(any(ViajeRecurrente.class))).thenAnswer(inv -> inv.getArgument(0));

        ViajeRecurrenteDTO dto = viajeRecurrenteService.finalizarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug");

        assertNotNull(dto);
        assertEquals(EstadoViaje.FINALIZADO.toString(), dto.getEstado());
        assertEquals(EstadoPago.CAPTURADO, pago.getEstado());
        verify(stripeService).confirmarCaptura("pi_123");
        verify(pagoRepository).save(pago);
        verify(notificacionRepository).save(any(Notificacion.class));
    }

    @Test
    void finalizarViajeRecurrente_stripeException_lanza402() throws Exception {
        viajeRecurrente.setEstado(EstadoViaje.EN_CURSO);

        Pago pago = new Pago();
        pago.setStripePaymentIntentId("pi_123");
        pago.setEstado(EstadoPago.PENDIENTE);

        Reserva reserva = new Reserva();
        ReflectionTestUtils.setField(reserva, "id", 20L);
        reserva.setCantidadPlazas(2);
        reserva.setPago(pago);

        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(reservaRepository.findByViajeRecurrenteIdAndEstadoNot(viajeRecurrente.getId(), EstadoReserva.CANCELADA))
                .thenReturn(List.of(reserva));

        doThrow(mock(StripeException.class)).when(stripeService).confirmarCaptura("pi_123");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.finalizarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug"));

        assertEquals(402, ex.getStatusCode().value());
    }

    @Test
    void finalizarViajeRecurrente_noEsConductor_lanza403() {
        Persona otroUsuario = new Persona();
        ReflectionTestUtils.setField(otroUsuario, "id", 99L);
        otroUsuario.setEmail("otro@test.com");

        when(personaRepository.findByEmail("otro@test.com")).thenReturn(Optional.of(otroUsuario));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.finalizarViajeRecurrente("otro@test.com", "viaje-recurrente-slug"));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void finalizarViajeRecurrente_estadoYaCancelado_lanza400() {
        viajeRecurrente.setEstado(EstadoViaje.CANCELADO);
        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.finalizarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug"));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void cancelarViajeRecurrente_ok() {
        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(reservaRepository.findByViajeRecurrenteIdAndEstadoNot(anyLong(), any())).thenReturn(List.of());
        when(viajeRecurrenteRepository.save(any(ViajeRecurrente.class))).thenAnswer(inv -> inv.getArgument(0));

        ViajeRecurrenteDTO dto = viajeRecurrenteService.cancelarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug");

        assertNotNull(dto);
        assertEquals(EstadoViaje.CANCELADO.toString(), dto.getEstado());
    }

    @Test
    void cancelarViajeRecurrente_sinMasReservas_reembolsoTotal() throws Exception {
        Pago pago = new Pago();
        ReflectionTestUtils.setField(pago, "id", 50L);
        pago.setStripePaymentIntentId("pi_123");
        pago.setEstado(EstadoPago.PENDIENTE);

        Reserva reserva = new Reserva();
        ReflectionTestUtils.setField(reserva, "id", 20L);
        reserva.setCantidadPlazas(1);
        reserva.setPago(pago);
        reserva.setPersona(conductor);

        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(reservaRepository.findByViajeRecurrenteIdAndEstadoNot(viajeRecurrente.getId(), EstadoReserva.CANCELADA))
                .thenReturn(List.of(reserva));
        when(reservaRepository.findByPagoIdAndEstadoNot(50L, EstadoReserva.CANCELADA)).thenReturn(List.of());
        when(viajeRecurrenteRepository.save(any(ViajeRecurrente.class))).thenAnswer(inv -> inv.getArgument(0));

        ViajeRecurrenteDTO dto = viajeRecurrenteService.cancelarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug");

        assertNotNull(dto);
        assertEquals(EstadoViaje.CANCELADO.toString(), dto.getEstado());
        assertEquals(EstadoPago.REEMBOLSADO, pago.getEstado());
        verify(stripeService).liberarFondos("pi_123");
        verify(pagoRepository).save(pago);
    }

    @Test
    void cancelarViajeRecurrente_conOtrasReservas_reembolsoParcial() throws Exception {
        Pago pago = new Pago();
        ReflectionTestUtils.setField(pago, "id", 50L);
        pago.setStripePaymentIntentId("pi_123");
        pago.setEstado(EstadoPago.CAPTURADO);
        pago.setImporteTotal(new BigDecimal("30.00"));

        Reserva reserva = new Reserva();
        ReflectionTestUtils.setField(reserva, "id", 20L);
        reserva.setCantidadPlazas(1);
        reserva.setPago(pago);
        reserva.setPersona(conductor);

        Reserva reservaOtraFecha = new Reserva();
        ReflectionTestUtils.setField(reservaOtraFecha, "id", 21L);

        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(reservaRepository.findByViajeRecurrenteIdAndEstadoNot(viajeRecurrente.getId(), EstadoReserva.CANCELADA))
                .thenReturn(List.of(reserva));
        when(reservaRepository.findByPagoIdAndEstadoNot(50L, EstadoReserva.CANCELADA)).thenReturn(List.of(reservaOtraFecha));
        when(viajeRecurrenteRepository.save(any(ViajeRecurrente.class))).thenAnswer(inv -> inv.getArgument(0));

        ViajeRecurrenteDTO dto = viajeRecurrenteService.cancelarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug");

        assertNotNull(dto);
        verify(stripeService).reembolsarParcial(eq("pi_123"), eq(new BigDecimal("15.00")));
        verify(pagoRepository).save(pago);
    }

    @Test
    void cancelarViajeRecurrente_stripeException_lanzaRuntimeException() throws Exception {
        Pago pago = new Pago();
        ReflectionTestUtils.setField(pago, "id", 50L);
        pago.setStripePaymentIntentId("pi_123");

        Reserva reserva = new Reserva();
        reserva.setCantidadPlazas(1);
        reserva.setPago(pago);

        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(reservaRepository.findByViajeRecurrenteIdAndEstadoNot(viajeRecurrente.getId(), EstadoReserva.CANCELADA))
                .thenReturn(List.of(reserva));
        when(reservaRepository.findByPagoIdAndEstadoNot(50L, EstadoReserva.CANCELADA)).thenReturn(List.of());

        doThrow(mock(StripeException.class)).when(stripeService).liberarFondos("pi_123");

        assertThrows(RuntimeException.class,
                () -> viajeRecurrenteService.cancelarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug"));
    }

    @Test
    void cancelarViajeRecurrenteIncompareceConductor_esConductor_lanza403() {
        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.cancelarViajeRecurrenteIncompareceConductor("conductor@test.com", "viaje-recurrente-slug"));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void cancelarViajeRecurrenteIncompareceConductor_sinReservaConfirmada_lanza403() {
        Persona pasajero = new Persona();
        ReflectionTestUtils.setField(pasajero, "id", 99L);
        pasajero.setEmail("pasajero@test.com");

        when(personaRepository.findByEmail("pasajero@test.com")).thenReturn(Optional.of(pasajero));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(reservaRepository.findByViajeRecurrenteIdAndEstadoNot(viajeRecurrente.getId(), EstadoReserva.CANCELADA))
                .thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.cancelarViajeRecurrenteIncompareceConductor("pasajero@test.com", "viaje-recurrente-slug"));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void cancelarViajeRecurrenteIncompareceConductor_estadoEnCurso_lanza400() {
        Persona pasajero = new Persona();
        ReflectionTestUtils.setField(pasajero, "id", 99L);
        pasajero.setEmail("pasajero@test.com");

        viajeRecurrente.setEstado(EstadoViaje.EN_CURSO);

        Reserva reserva = new Reserva();
        reserva.setPersona(pasajero);
        reserva.setEstado(EstadoReserva.CONFIRMADA);

        when(personaRepository.findByEmail("pasajero@test.com")).thenReturn(Optional.of(pasajero));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(reservaRepository.findByViajeRecurrenteIdAndEstadoNot(viajeRecurrente.getId(), EstadoReserva.CANCELADA))
                .thenReturn(List.of(reserva));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.cancelarViajeRecurrenteIncompareceConductor("pasajero@test.com", "viaje-recurrente-slug"));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void cancelarViajeRecurrenteIncompareceConductor_antesDeTiempoMinimo_lanza400() {
        Persona pasajero = new Persona();
        ReflectionTestUtils.setField(pasajero, "id", 99L);
        pasajero.setEmail("pasajero@test.com");

        viajeRecurrente.setFechaHoraSalida(LocalDateTime.now().plusHours(1));

        Reserva reserva = new Reserva();
        reserva.setPersona(pasajero);
        reserva.setEstado(EstadoReserva.CONFIRMADA);

        when(personaRepository.findByEmail("pasajero@test.com")).thenReturn(Optional.of(pasajero));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(reservaRepository.findByViajeRecurrenteIdAndEstadoNot(viajeRecurrente.getId(), EstadoReserva.CANCELADA))
                .thenReturn(List.of(reserva));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.cancelarViajeRecurrenteIncompareceConductor("pasajero@test.com", "viaje-recurrente-slug"));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void cancelarViajeRecurrenteIncompareceConductor_despuesDeTiempoMaximo_lanza400() {
        Persona pasajero = new Persona();
        ReflectionTestUtils.setField(pasajero, "id", 99L);
        pasajero.setEmail("pasajero@test.com");

        viajeRecurrente.setFechaHoraSalida(LocalDateTime.now().minusHours(3));

        Reserva reserva = new Reserva();
        reserva.setPersona(pasajero);
        reserva.setEstado(EstadoReserva.CONFIRMADA);

        when(personaRepository.findByEmail("pasajero@test.com")).thenReturn(Optional.of(pasajero));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(reservaRepository.findByViajeRecurrenteIdAndEstadoNot(viajeRecurrente.getId(), EstadoReserva.CANCELADA))
                .thenReturn(List.of(reserva));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.cancelarViajeRecurrenteIncompareceConductor("pasajero@test.com", "viaje-recurrente-slug"));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void cancelarViajeRecurrenteIncompareceConductor_exito() throws Exception {
        Persona pasajero = new Persona();
        ReflectionTestUtils.setField(pasajero, "id", 99L);
        pasajero.setEmail("pasajero@test.com");

        viajeRecurrente.setFechaHoraSalida(LocalDateTime.now().minusMinutes(30));

        Pago pago = new Pago();
        ReflectionTestUtils.setField(pago, "id", 80L);
        pago.setStripePaymentIntentId("pi_456");

        Reserva reserva = new Reserva();
        reserva.setPersona(pasajero);
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reserva.setCantidadPlazas(1);
        reserva.setPago(pago);

        when(personaRepository.findByEmail("pasajero@test.com")).thenReturn(Optional.of(pasajero));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(reservaRepository.findByViajeRecurrenteIdAndEstadoNot(viajeRecurrente.getId(), EstadoReserva.CANCELADA))
                .thenReturn(List.of(reserva));
        when(reservaRepository.findByPagoIdAndEstadoNot(80L, EstadoReserva.CANCELADA)).thenReturn(List.of());
        when(viajeRecurrenteRepository.save(any(ViajeRecurrente.class))).thenAnswer(inv -> inv.getArgument(0));

        ViajeRecurrenteDTO dto = viajeRecurrenteService.cancelarViajeRecurrenteIncompareceConductor("pasajero@test.com", "viaje-recurrente-slug");

        assertNotNull(dto);
        assertEquals(EstadoViaje.CANCELADO.toString(), dto.getEstado());
        verify(stripeService).liberarFondos("pi_456");
        verify(personaRepository).save(conductor);
    }

    @Test
    void cancelarViajesRecurrentesPendientesExpirados_actualizaViajesPasados() {
        viajeRecurrente.setFechaHoraSalida(LocalDateTime.now().minusHours(2));
        when(viajeRecurrenteRepository.findByEstadoAndFechaHoraSalidaBefore(eq(EstadoViaje.PENDIENTE), any()))
                .thenReturn(List.of(viajeRecurrente));

        viajeRecurrenteService.cancelarViajesRecurrentesPendientesExpirados();

        assertEquals(EstadoViaje.CANCELADO, viajeRecurrente.getEstado());
        verify(viajeRecurrenteRepository).saveAll(List.of(viajeRecurrente));
    }

    @Test
    void cancelarViajesRecurrentesPendientesExpirados_sinExpirados_noHaceNada() {
        when(viajeRecurrenteRepository.findByEstadoAndFechaHoraSalidaBefore(eq(EstadoViaje.PENDIENTE), any()))
                .thenReturn(List.of());

        viajeRecurrenteService.cancelarViajesRecurrentesPendientesExpirados();

        verify(viajeRecurrenteRepository, never()).saveAll(anyList());
    }

    @Test
    void actualizarViajeRecurrente_ok_conNotificaciones() {
        viajeRecurrente.setFechaHoraSalida(LocalDateTime.now().plusDays(2));

        Viaje viajeEditado = new Viaje();
        viajeEditado.setFechaHoraSalida(LocalDateTime.now().plusDays(3));
        viajeEditado.setPrecio(new BigDecimal("20.00"));
        viajeEditado.setPlazasDisponibles(5);

        Reserva reservaActiva = new Reserva();
        reservaActiva.setPersona(conductor);
        reservaActiva.setCantidadPlazas(1);

        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(reservaRepository.findByViajeRecurrenteIdAndEstadoNot(viajeRecurrente.getId(), EstadoReserva.CANCELADA))
                .thenReturn(List.of(reservaActiva));
        when(viajeRecurrenteRepository.save(any(ViajeRecurrente.class))).thenAnswer(inv -> inv.getArgument(0));

        ViajeRecurrenteDTO dto = viajeRecurrenteService.actualizarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug", viajeEditado);

        assertNotNull(dto);
        assertEquals(new BigDecimal("20.00"), dto.getPrecio());
        assertEquals(4, dto.getPlazasDisponibles());
        verify(notificacionRepository).save(any(Notificacion.class));
    }

    @Test
    void actualizarViajeRecurrente_menosDe12Horas_lanza400() {
        viajeRecurrente.setFechaHoraSalida(LocalDateTime.now().plusHours(5));
        Viaje viajeEditado = new Viaje();
        viajeEditado.setPrecio(new BigDecimal("25.00"));

        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.actualizarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug", viajeEditado));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void actualizarViajeRecurrente_plazasMenoresAReservadas_lanza400() {
        viajeRecurrente.setFechaHoraSalida(LocalDateTime.now().plusDays(2));
        Viaje viajeEditado = new Viaje();
        viajeEditado.setPlazasDisponibles(1);

        Reserva reservaMock = new Reserva();
        reservaMock.setCantidadPlazas(2);

        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(reservaRepository.findByViajeRecurrenteIdAndEstadoNot(anyLong(), any())).thenReturn(List.of(reservaMock));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.actualizarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug", viajeEditado));

        assertEquals(400, ex.getStatusCode().value());
    }
}