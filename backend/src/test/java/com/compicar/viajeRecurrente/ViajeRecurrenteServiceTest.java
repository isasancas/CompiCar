package com.compicar.viajeRecurrente;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

import com.compicar.notificacion.NotificacionRepository;
import com.compicar.pago.PagoRepository;
import com.compicar.pago.StripeService;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.reserva.ReservaRepository;
import com.compicar.vehiculo.Vehiculo;
import com.compicar.viaje.EstadoViaje;
import com.compicar.viaje.Viaje;
import com.compicar.viajeRecurrente.dto.ViajeRecurrenteDTO;

@ExtendWith(MockitoExtension.class)
class ViajeRecurrenteServiceImplTest {

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

    @Test
    void mapearADTO_nulo_retornaNull() {
        ViajeRecurrenteDTO dto = viajeRecurrenteService.mapearADTO(null);
        assertNull(dto);
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
    void generarOcurrencias_datosIncompletos_retornaVacio() {
        viajePadre.setDiasSemana(null);
        List<ViajeRecurrente> resultado = viajeRecurrenteService.generarOcurrencias(viajePadre);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void obtenerViajeRecurrentePorSlug_ok() {
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));

        ViajeRecurrenteDTO dto = viajeRecurrenteService.obtenerViajeRecurrentePorSlug("viaje-recurrente-slug");

        assertNotNull(dto);
        assertEquals("viaje-recurrente-slug", dto.getSlug());
        verify(viajeRecurrenteRepository).findBySlug("viaje-recurrente-slug");
    }

    @Test
    void obtenerViajeRecurrentePorSlug_noEncontrado_lanza404() {
        when(viajeRecurrenteRepository.findBySlug("inexistente")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.obtenerViajeRecurrentePorSlug("inexistente"));

        assertEquals(404, ex.getStatusCode().value());
        assertEquals("Viaje recurrente no encontrado", ex.getReason());
    }

    @Test
    void iniciarViajeRecurrente_ok() {
        viajeRecurrente.setFechaHoraSalida(LocalDateTime.now().minusHours(1)); // Ya pasada para cumplir la validación de tiempo
        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(viajeRecurrenteRepository.save(any(ViajeRecurrente.class))).thenAnswer(inv -> inv.getArgument(0));

        ViajeRecurrenteDTO dto = viajeRecurrenteService.iniciarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug");

        assertNotNull(dto);
        assertEquals(EstadoViaje.INICIADO.toString(), dto.getEstado());
        verify(viajeRecurrenteRepository).save(viajeRecurrente);
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
    void confirmarCheckinRecurrente_ok() {
        viajeRecurrente.setEstado(EstadoViaje.INICIADO);
        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(viajeRecurrenteRepository.save(any(ViajeRecurrente.class))).thenAnswer(inv -> inv.getArgument(0));

        ViajeRecurrenteDTO dto = viajeRecurrenteService.confirmarCheckinRecurrente("conductor@test.com", "viaje-recurrente-slug", "ABC123");

        assertNotNull(dto);
        assertEquals(EstadoViaje.EN_CURSO.toString(), dto.getEstado());
        verify(viajeRecurrenteRepository).save(viajeRecurrente);
    }

    @Test
    void confirmarCheckinRecurrente_checkinInvalido_lanza400() {
        viajeRecurrente.setEstado(EstadoViaje.INICIADO);
        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.confirmarCheckinRecurrente("conductor@test.com", "viaje-recurrente-slug", "MAL"));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Checkin inválido", ex.getReason());
    }

    @Test
    void cancelarViajesRecurrentesPendientesExpirados_actualizaViajesPasados() {
        viajeRecurrente.setFechaHoraSalida(LocalDateTime.now().minusHours(2));
        when(viajeRecurrenteRepository.findByEstadoAndFechaHoraSalidaBefore(eq(EstadoViaje.PENDIENTE), any(LocalDateTime.now().getClass())))
                .thenReturn(List.of(viajeRecurrente));

        viajeRecurrenteService.cancelarViajesRecurrentesPendientesExpirados();

        assertEquals(EstadoViaje.CANCELADO, viajeRecurrente.getEstado());
        verify(viajeRecurrenteRepository).saveAll(List.of(viajeRecurrente));
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
        verify(viajeRecurrenteRepository).save(viajeRecurrente);
        verify(personaRepository).save(conductor);
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
        verify(viajeRecurrenteRepository).save(viajeRecurrente);
    }

    @Test
    void actualizarViajeRecurrente_ok() {
        viajeRecurrente.setFechaHoraSalida(LocalDateTime.now().plusDays(2)); // Más de 12 horas para permitir editar
        Viaje viajeEditado = new Viaje();
        viajeEditado.setPrecio(new BigDecimal("20.00"));
        viajeEditado.setPlazasDisponibles(5);

        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(reservaRepository.findByViajeRecurrenteIdAndEstadoNot(anyLong(), any())).thenReturn(List.of());
        when(viajeRecurrenteRepository.save(any(ViajeRecurrente.class))).thenAnswer(inv -> inv.getArgument(0));

        ViajeRecurrenteDTO dto = viajeRecurrenteService.actualizarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug", viajeEditado);

        assertNotNull(dto);
        assertEquals(new BigDecimal("20.00"), dto.getPrecio());
        verify(viajeRecurrenteRepository).save(viajeRecurrente);
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
        assertEquals("Solo el conductor puede finalizar este viaje", ex.getReason());
    }

    @Test
    void finalizarViajeRecurrente_estadoYaCancelado_lanza400() {
        viajeRecurrente.setEstado(EstadoViaje.CANCELADO);
        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.finalizarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug"));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("No se puede finalizar un viaje en estado"));
    }

    @Test
    void cancelarViajeRecurrente_noEsConductor_lanza403() {
        Persona otroUsuario = new Persona();
        ReflectionTestUtils.setField(otroUsuario, "id", 99L);
        otroUsuario.setEmail("otro@test.com");

        when(personaRepository.findByEmail("otro@test.com")).thenReturn(Optional.of(otroUsuario));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.cancelarViajeRecurrente("otro@test.com", "viaje-recurrente-slug"));

        assertEquals(403, ex.getStatusCode().value());
        assertEquals("Solo el conductor puede cancelar este viaje", ex.getReason());
    }

    @Test
    void cancelarViajeRecurrente_estadoFinalizado_lanza400() {
        viajeRecurrente.setEstado(EstadoViaje.FINALIZADO);
        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.cancelarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug"));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("No se puede cancelar un viaje en estado"));
    }

    @Test
    void actualizarViajeRecurrente_menosDe12Horas_lanza400() {
        // Faltan menos de 12 horas para la salida
        viajeRecurrente.setFechaHoraSalida(LocalDateTime.now().plusHours(5));
        Viaje viajeEditado = new Viaje();
        viajeEditado.setPrecio(new BigDecimal("25.00"));

        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.actualizarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug", viajeEditado));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("No se puede editar el viaje a falta de menos de 12 horas para la salida", ex.getReason());
    }

    @Test
    void actualizarViajeRecurrente_plazasMenoresAReservadas_lanza400() {
        viajeRecurrente.setFechaHoraSalida(LocalDateTime.now().plusDays(2));
        Viaje viajeEditado = new Viaje();
        viajeEditado.setPlazasDisponibles(1); // Intentamos poner 1 total, pero simularemos que hay más ocupadas

        com.compicar.reserva.Reserva reservaMock = new com.compicar.reserva.Reserva();
        reservaMock.setCantidadPlazas(2);

        when(personaRepository.findByEmail("conductor@test.com")).thenReturn(Optional.of(conductor));
        when(viajeRecurrenteRepository.findBySlug("viaje-recurrente-slug")).thenReturn(Optional.of(viajeRecurrente));
        when(reservaRepository.findByViajeRecurrenteIdAndEstadoNot(anyLong(), any())).thenReturn(List.of(reservaMock));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRecurrenteService.actualizarViajeRecurrente("conductor@test.com", "viaje-recurrente-slug", viajeEditado));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("El total de plazas no puede ser inferior a las plazas ya reservadas"));
    }
}