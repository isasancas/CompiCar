package com.compicar.viaje;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import com.compicar.scheduler.ProgramadorCancelacionViajes;
import com.compicar.vehiculo.TipoVehiculo;
import com.compicar.vehiculo.Vehiculo;
import com.compicar.vehiculo.VehiculoRepository;
import com.compicar.viaje.dto.CalcularPrecioTrayectoRequestDTO;
import com.compicar.viaje.dto.PrecioTrayectoResponseDTO;
import com.compicar.viaje.dto.ViajeDTO;
import com.compicar.viajeRecurrente.ViajeRecurrente;
import com.compicar.viajeRecurrente.ViajeRecurrenteRepository;
import com.compicar.viajeRecurrente.ViajeRecurrenteService;
import com.stripe.exception.StripeException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ViajeServiceTest {

    @Mock
    private ViajeRepository viajeRepository;
    @Mock
    private PersonaRepository personaRepository;
    @Mock
    private VehiculoRepository vehiculoRepository;
    @Mock
    private CalculoPrecioIA calculoPrecioIA;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private ViajeRecurrenteRepository viajeRecurrenteRepository;
    @Mock
    private ViajeRecurrenteService viajeRecurrenteService;
    @Mock
    private StripeService stripeService;

    @InjectMocks
    private ViajeServiceImpl viajeService;

    private Persona conductor;
    private Persona otroUsuario;
    private Vehiculo vehiculoConductor;
    private Vehiculo vehiculoOtro;
    private Viaje viajeBase;
    private LocalDateTime salida;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(viajeService, "fallbackFuelPrice", new BigDecimal("1.65"));

        conductor = new Persona();
        ReflectionTestUtils.setField(conductor, "id", 1L);
        conductor.setEmail("driver@compicar.com");

        otroUsuario = new Persona();
        ReflectionTestUtils.setField(otroUsuario, "id", 2L);
        otroUsuario.setEmail("other@compicar.com");

        vehiculoConductor = new Vehiculo();
        ReflectionTestUtils.setField(vehiculoConductor, "id", 10L);
        vehiculoConductor.setMarca("Seat");
        vehiculoConductor.setModelo("Ibiza");
        vehiculoConductor.setMatricula("1234ABC");
        vehiculoConductor.setConsumo(5.0);
        vehiculoConductor.setAnio(2022);
        vehiculoConductor.setTipo(TipoVehiculo.COCHE);
        vehiculoConductor.setPersona(conductor);

        vehiculoOtro = new Vehiculo();
        ReflectionTestUtils.setField(vehiculoOtro, "id", 11L);
        vehiculoOtro.setMarca("Toyota");
        vehiculoOtro.setModelo("Yaris");
        vehiculoOtro.setMatricula("9999ZZZ");
        vehiculoOtro.setConsumo(4.8);
        vehiculoOtro.setAnio(2021);
        vehiculoOtro.setTipo(TipoVehiculo.COCHE);
        vehiculoOtro.setPersona(otroUsuario);

        salida = LocalDateTime.of(2026, 5, 1, 10, 30);

        viajeBase = new Viaje();
        viajeBase.setFechaHoraSalida(salida);
        viajeBase.setEstado(EstadoViaje.PENDIENTE);
        viajeBase.setPlazasDisponibles(3);
        viajeBase.setPrecio(new BigDecimal("8.50"));

        Vehiculo v = new Vehiculo();
        v.setId(vehiculoConductor.getId());
        viajeBase.setVehiculo(v);

        Parada origen = parada(TipoParada.ORIGEN, "Sevilla", null, 1);
        Parada destino = parada(TipoParada.DESTINO, "Cadiz", null, 2);
        viajeBase.setParadas(new ArrayList<>(List.of(origen, destino)));
    }

    @Test
    void crearViaje_ok_asignaConductorVehiculoParadasYSlug() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.findById(vehiculoConductor.getId())).thenReturn(Optional.of(vehiculoConductor));
        when(viajeRepository.existsBySlug("sevilla-cadiz-2026-05-01")).thenReturn(false);
        when(viajeRepository.save(any(Viaje.class))).thenAnswer(inv -> inv.getArgument(0));

        Viaje result = viajeService.crearViaje(conductor.getEmail(), viajeBase);

        assertNotNull(result);
        assertEquals(conductor, result.getPersona());
        assertEquals(vehiculoConductor, result.getVehiculo());
        assertEquals("sevilla-cadiz-2026-05-01", result.getSlug());
        assertEquals(2, result.getParadas().size());
        assertEquals(salida, result.getParadas().get(0).getFechaHora());
        assertEquals(1, result.getParadas().get(0).getOrden());
        assertEquals(2, result.getParadas().get(1).getOrden());
        assertSame(result, result.getParadas().get(0).getViaje());
        assertSame(result, result.getParadas().get(1).getViaje());
        verify(viajeRepository).save(viajeBase);
    }

    @Test
    void crearViaje_slugDuplicado_generaSufijo() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.findById(vehiculoConductor.getId())).thenReturn(Optional.of(vehiculoConductor));
        when(viajeRepository.existsBySlug("sevilla-cadiz-2026-05-01")).thenReturn(true);
        when(viajeRepository.existsBySlug("sevilla-cadiz-2026-05-01-2")).thenReturn(false);
        when(viajeRepository.save(any(Viaje.class))).thenAnswer(inv -> inv.getArgument(0));

        Viaje result = viajeService.crearViaje(conductor.getEmail(), viajeBase);

        assertEquals("sevilla-cadiz-2026-05-01-2", result.getSlug());
    }

    @Test
    void crearViaje_usuarioNoExiste_lanza401() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.crearViaje(conductor.getEmail(), viajeBase));

        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Usuario no encontrado", ex.getReason());
    }

    @Test
    void crearViaje_sinVehiculo_lanza400() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        viajeBase.setVehiculo(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.crearViaje(conductor.getEmail(), viajeBase));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("El viaje debe incluir un vehículo válido", ex.getReason());
    }

    @Test
    void crearViaje_vehiculoSinId_lanza400() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        viajeBase.setVehiculo(new Vehiculo());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.crearViaje(conductor.getEmail(), viajeBase));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("El viaje debe incluir un vehículo válido", ex.getReason());
    }

    @Test
    void crearViaje_menosDeDosParadas_lanza400() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        viajeBase.setParadas(List.of(parada(TipoParada.ORIGEN, "Sevilla", null, 1)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.crearViaje(conductor.getEmail(), viajeBase));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Debes indicar al menos origen y destino", ex.getReason());
    }

    @Test
    void crearViaje_vehiculoNoExiste_lanza400() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.findById(vehiculoConductor.getId())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.crearViaje(conductor.getEmail(), viajeBase));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Vehículo no existe", ex.getReason());
    }

    @Test
    void crearViaje_vehiculoDeOtroUsuario_lanza403() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.findById(vehiculoConductor.getId())).thenReturn(Optional.of(vehiculoOtro));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.crearViaje(conductor.getEmail(), viajeBase));

        assertEquals(403, ex.getStatusCode().value());
        assertEquals("El vehículo no pertenece al usuario autenticado", ex.getReason());
    }

    @Test
    void crearViaje_sinOrigenUnico_lanza400() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.findById(vehiculoConductor.getId())).thenReturn(Optional.of(vehiculoConductor));

        Parada p1 = parada(TipoParada.ORIGEN, "Sevilla", salida, 1);
        Parada p2 = parada(TipoParada.ORIGEN, "Cadiz", salida.plusHours(1), 2);
        viajeBase.setParadas(List.of(p1, p2));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.crearViaje(conductor.getEmail(), viajeBase));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Debe haber exactamente un ORIGEN y un DESTINO", ex.getReason());
    }

    @Test
    void crearViaje_localizacionVacia_lanza400() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.findById(vehiculoConductor.getId())).thenReturn(Optional.of(vehiculoConductor));

        Parada p1 = parada(TipoParada.ORIGEN, " ", salida, 1);
        Parada p2 = parada(TipoParada.DESTINO, "Cadiz", salida.plusHours(1), 2);
        viajeBase.setParadas(List.of(p1, p2));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.crearViaje(conductor.getEmail(), viajeBase));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Todas las paradas deben tener localizacion", ex.getReason());
    }

    @Test
    void crearViaje_ordenDuplicado_lanza400() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.findById(vehiculoConductor.getId())).thenReturn(Optional.of(vehiculoConductor));

        Parada p1 = parada(TipoParada.ORIGEN, "Sevilla", salida, 1);
        Parada p2 = parada(TipoParada.DESTINO, "Cadiz", salida.plusHours(1), 1);
        viajeBase.setParadas(List.of(p1, p2));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.crearViaje(conductor.getEmail(), viajeBase));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("No puede haber dos paradas con el mismo orden", ex.getReason());
    }

   @Test
    void calcularPrecioTrayecto_ok_fuenteGemini() {
        CalcularPrecioTrayectoRequestDTO req = new CalcularPrecioTrayectoRequestDTO();
        req.setVehiculoId(vehiculoConductor.getId());
        req.setDistanciaKm(100.0);

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.findById(vehiculoConductor.getId())).thenReturn(Optional.of(vehiculoConductor));
        when(calculoPrecioIA.pedirEstimacionJson(anyString()))
            .thenReturn("{\"precio_combustible_litro\":1.7,\"detalle\":\"ok\"}");

        PrecioTrayectoResponseDTO resp = viajeService.calcularPrecioTrayecto(conductor.getEmail(), req);

        assertEquals(new BigDecimal("5.00"), resp.getLitrosEstimados());
        assertEquals(new BigDecimal("1.700"), resp.getPrecioCombustibleLitro());
        assertEquals(new BigDecimal("8.50"), resp.getCosteTotalCombustible());
        
        assertEquals(new BigDecimal("18.32"), resp.getPrecioMinimoPasajero());
        assertEquals(new BigDecimal("22.40"), resp.getPrecioMaximoPasajero());
        
        assertEquals("GEMINI", resp.getFuente());
    }

    @Test
    void calcularPrecioTrayecto_geminiFueraDeRango_usaFallback() {
        CalcularPrecioTrayectoRequestDTO req = new CalcularPrecioTrayectoRequestDTO();
        req.setVehiculoId(vehiculoConductor.getId());
        req.setDistanciaKm(100.0);

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.findById(vehiculoConductor.getId())).thenReturn(Optional.of(vehiculoConductor));
        when(calculoPrecioIA.pedirEstimacionJson(anyString()))
            .thenReturn("{\"precio_combustible_litro\":0.5,\"detalle\":\"too low\"}");

        PrecioTrayectoResponseDTO resp = viajeService.calcularPrecioTrayecto(conductor.getEmail(), req);

        assertEquals(new BigDecimal("1.650"), resp.getPrecioCombustibleLitro());
        assertEquals("FALLBACK", resp.getFuente());
        assertEquals("Gemini no disponible, se usa precio fallback", resp.getDetalle());
    }

    @Test
    void calcularPrecioTrayecto_geminiExcepcion_usaFallback() {
        CalcularPrecioTrayectoRequestDTO req = new CalcularPrecioTrayectoRequestDTO();
        req.setVehiculoId(vehiculoConductor.getId());
        req.setDistanciaKm(100.0);

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.findById(vehiculoConductor.getId())).thenReturn(Optional.of(vehiculoConductor));
        when(calculoPrecioIA.pedirEstimacionJson(anyString())).thenThrow(new RuntimeException("boom"));

        PrecioTrayectoResponseDTO resp = viajeService.calcularPrecioTrayecto(conductor.getEmail(), req);

        assertEquals("FALLBACK", resp.getFuente());
        assertEquals(new BigDecimal("1.650"), resp.getPrecioCombustibleLitro());
    }

    @Test
    void calcularPrecioTrayecto_usuarioNoExiste_lanza401() {
        CalcularPrecioTrayectoRequestDTO req = new CalcularPrecioTrayectoRequestDTO();
        req.setVehiculoId(vehiculoConductor.getId());
        req.setDistanciaKm(100.0);

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.calcularPrecioTrayecto(conductor.getEmail(), req));

        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Usuario no encontrado", ex.getReason());
    }

    @Test
    void calcularPrecioTrayecto_vehiculoNoExiste_lanza400() {
        CalcularPrecioTrayectoRequestDTO req = new CalcularPrecioTrayectoRequestDTO();
        req.setVehiculoId(vehiculoConductor.getId());
        req.setDistanciaKm(100.0);

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.findById(vehiculoConductor.getId())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.calcularPrecioTrayecto(conductor.getEmail(), req));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Vehículo no existe", ex.getReason());
    }

    @Test
    void calcularPrecioTrayecto_vehiculoDeOtroUsuario_lanza403() {
        CalcularPrecioTrayectoRequestDTO req = new CalcularPrecioTrayectoRequestDTO();
        req.setVehiculoId(vehiculoOtro.getId());
        req.setDistanciaKm(120.0);

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.findById(vehiculoOtro.getId())).thenReturn(Optional.of(vehiculoOtro));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.calcularPrecioTrayecto(conductor.getEmail(), req));

        assertEquals(403, ex.getStatusCode().value());
        assertEquals("El vehículo no pertenece al usuario autenticado", ex.getReason());
    }

    @Test
    void obtenerViajePorSlug_ok_devuelveDtoMapeado() {
        Viaje viaje = viajeCompleto(100L, "sevilla-cadiz-2026-05-01");
        when(viajeRepository.findBySlug("sevilla-cadiz-2026-05-01")).thenReturn(Optional.of(viaje));

        ViajeDTO dto = viajeService.obtenerViajePorSlug("sevilla-cadiz-2026-05-01");

        assertEquals(100L, dto.getId());
        assertEquals("PENDIENTE", dto.getEstado());
        assertEquals("sevilla-cadiz-2026-05-01", dto.getSlug());
        assertEquals(vehiculoConductor.getId(), dto.getVehiculo().getId());
        assertEquals(2, dto.getParadas().size());
        assertEquals("ORIGEN", dto.getParadas().get(0).getTipo());
    }

    @Test
    void obtenerViajePorSlug_noExiste_lanza404() {
        when(viajeRepository.findBySlug("no-existe")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.obtenerViajePorSlug("no-existe"));

        assertEquals(404, ex.getStatusCode().value());
        assertEquals("Viaje no encontrado", ex.getReason());
    }

    @Test
    void obtenerMisViajes_ok_mapeaLista() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findByPersonaId(1L)).thenReturn(List.of(viajeCompleto(1L, "slug-1"), viajeCompleto(2L, "slug-2")));

        List<ViajeDTO> result = viajeService.obtenerMisViajes(conductor.getEmail());

        assertEquals(2, result.size());
        assertEquals("slug-1", result.get(0).getSlug());
        assertEquals("slug-2", result.get(1).getSlug());
    }

    @Test
    void obtenerMisViajes_usuarioNoEncontrado_lanza401() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.obtenerMisViajes(conductor.getEmail()));

        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Usuario no encontrado", ex.getReason());
    }

    @Test
    void obtenerViajesParticipados_ok_mapeaLista() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findViajesParticipadosByPersonaId(1L)).thenReturn(List.of(viajeCompleto(3L, "slug-3")));

        List<ViajeDTO> result = viajeService.obtenerViajesParticipados(conductor.getEmail());

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getId());
        assertEquals("slug-3", result.get(0).getSlug());
    }

    @Test
    void obtenerViajesParticipados_usuarioNoEncontrado_lanza401() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.obtenerViajesParticipados(conductor.getEmail()));

        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Usuario no encontrado", ex.getReason());
    }

    @Test
    void crearViaje_limite_ordenAutogeneradoConIntermedia() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.findById(vehiculoConductor.getId())).thenReturn(Optional.of(vehiculoConductor));
        when(viajeRepository.existsBySlug(anyString())).thenReturn(false);
        when(viajeRepository.save(any(Viaje.class))).thenAnswer(inv -> inv.getArgument(0));

        Parada origen = parada(TipoParada.ORIGEN, "Sevilla", null, null);
        Parada intermedia = parada(TipoParada.INTERMEDIA, "Jerez", null, null);
        Parada destino = parada(TipoParada.DESTINO, "Cadiz", null, null);
        viajeBase.setParadas(new ArrayList<>(List.of(origen, intermedia, destino)));

        Viaje result = viajeService.crearViaje(conductor.getEmail(), viajeBase);

        assertEquals(1, result.getParadas().get(0).getOrden());
        assertEquals(2, result.getParadas().get(1).getOrden());
        assertEquals(3, result.getParadas().get(2).getOrden());
    }

    @Test
    void cancelarViaje_ok_cancelaViajeYReservasYNotifica() {
        String slug = "sevilla-cadiz-2026-05-01";
        viajeBase.setSlug(slug);
        viajeBase.setPersona(conductor);
        viajeBase.setEstado(EstadoViaje.PENDIENTE);
        viajeBase.setFechaHoraSalida(LocalDateTime.now().plusHours(24));

        Reserva reserva = new Reserva();
        reserva.setPersona(otroUsuario);
        reserva.setViaje(viajeBase);
        Pago pago = new Pago();
        reserva.setPago(pago);

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));
        when(reservaRepository.findByViajeAndEstadoNot(viajeBase, EstadoReserva.CANCELADA))
                .thenReturn(List.of(reserva));

        ViajeDTO result = viajeService.cancelarViaje(conductor.getEmail(), slug);

        assertEquals("CANCELADO", result.getEstado());
        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        assertEquals(EstadoPago.REEMBOLSADO, pago.getEstado());

        verify(viajeRepository).save(viajeBase);
        verify(reservaRepository).save(reserva);
        verify(pagoRepository).save(pago);
        verify(notificacionRepository).save(any(Notificacion.class));
    }

    @Test
    void cancelarViaje_error_usuarioNoEsConductor_lanza403() {
        viajeBase.setPersona(conductor);
        viajeBase.setSlug("slug-test");

        when(personaRepository.findByEmail(otroUsuario.getEmail())).thenReturn(Optional.of(otroUsuario));
        when(viajeRepository.findBySlug("slug-test")).thenReturn(Optional.of(viajeBase));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
            () -> viajeService.cancelarViaje(otroUsuario.getEmail(), "slug-test"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void cancelarViaje_error_viajeYaFinalizado_lanza400() {
        viajeBase.setPersona(conductor);
        viajeBase.setEstado(EstadoViaje.FINALIZADO);

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug("test")).thenReturn(Optional.of(viajeBase));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
            () -> viajeService.cancelarViaje(conductor.getEmail(), "test"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void cancelarViaje_error_viajeYaCancelado_lanza400() {
        viajeBase.setPersona(conductor);
        viajeBase.setEstado(EstadoViaje.CANCELADO);
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug("ya-cancelado")).thenReturn(Optional.of(viajeBase));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.cancelarViaje(conductor.getEmail(), "ya-cancelado"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("No se puede cancelar un viaje en estado CANCELADO"));
    }

    @Test
    void cancelarViaje_conReservaNoPresentado_noCancelaReserva_peroReembolsaYNotifica() {
        String slug = "viaje-con-no-presentado";
        viajeBase.setSlug(slug);
        viajeBase.setPersona(conductor);
        viajeBase.setEstado(EstadoViaje.PENDIENTE);
        viajeBase.setFechaHoraSalida(LocalDateTime.now().plusHours(24));

        Reserva reservaNoPresentado = new Reserva();
        reservaNoPresentado.setPersona(otroUsuario);
        reservaNoPresentado.setViaje(viajeBase);
        reservaNoPresentado.setEstado(EstadoReserva.NO_PRESENTADO);

        Pago pago = new Pago();
        reservaNoPresentado.setPago(pago);

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));
        when(reservaRepository.findByViajeAndEstadoNot(viajeBase, EstadoReserva.CANCELADA))
                .thenReturn(List.of(reservaNoPresentado));

        ViajeDTO result = viajeService.cancelarViaje(conductor.getEmail(), slug);

        assertEquals("CANCELADO", result.getEstado());
        assertEquals(EstadoReserva.NO_PRESENTADO, reservaNoPresentado.getEstado());
        assertEquals(EstadoPago.REEMBOLSADO, pago.getEstado());

        verify(reservaRepository, never()).save(reservaNoPresentado);
        verify(pagoRepository).save(pago);
        verify(notificacionRepository).save(any(Notificacion.class));
        verify(viajeRepository).save(viajeBase);

    }

    @Test
    void cancelarViajesPendientesExpirados_ok_procesaViajesAntiguos() {
        Viaje viajeExpirado = new Viaje();
        viajeExpirado.setEstado(EstadoViaje.PENDIENTE);
        viajeExpirado.setPersona(conductor);
        viajeExpirado.setFechaHoraSalida(LocalDateTime.now().minusHours(13));
        
        when(viajeRepository.findByEstadoAndFechaHoraSalidaBefore(eq(EstadoViaje.PENDIENTE), any()))
                .thenReturn(List.of(viajeExpirado));

        int procesados = viajeService.cancelarViajesPendientesExpirados();

        assertEquals(1, procesados);
        assertEquals(EstadoViaje.CANCELADO, viajeExpirado.getEstado());
        verify(viajeRepository).save(viajeExpirado);
    }

    @Test
    void cancelarViaje_error_usuarioNoEncontrado_lanza401() {
        when(personaRepository.findByEmail("missing@compicar.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.cancelarViaje("missing@compicar.com", "slug-test"));

        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Usuario no encontrado", ex.getReason());
    }

    @Test
    void cancelarViaje_error_viajeNoEncontrado_lanza404() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug("no-existe")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.cancelarViaje(conductor.getEmail(), "no-existe"));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void obtenerMisViajes_listaVacia_ok() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findByPersonaId(1L)).thenReturn(List.of());

        List<ViajeDTO> result = viajeService.obtenerMisViajes(conductor.getEmail());

        assertEquals(0, result.size());
    }

    @Test
    void obtenerViajesParticipados_listaVacia_ok() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findViajesParticipadosByPersonaId(1L)).thenReturn(List.of());

        List<ViajeDTO> result = viajeService.obtenerViajesParticipados(conductor.getEmail());

        assertEquals(0, result.size());
    }

    @Test
    void crearViaje_sinDestino_lanza400() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));

        Parada origen = parada(TipoParada.ORIGEN, "Sevilla", salida, 1);
        viajeBase.setParadas(List.of(origen));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.crearViaje(conductor.getEmail(), viajeBase));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Debes indicar al menos origen y destino", ex.getReason());
    }

    @Test
    void crearViaje_dosDestinos_lanza400() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.findById(vehiculoConductor.getId())).thenReturn(Optional.of(vehiculoConductor));

        Parada p1 = parada(TipoParada.DESTINO, "Cadiz", salida.plusHours(1), 1);
        Parada p2 = parada(TipoParada.DESTINO, "Malaga", salida.plusHours(2), 2);
        viajeBase.setParadas(List.of(p1, p2));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.crearViaje(conductor.getEmail(), viajeBase));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Debe haber exactamente un ORIGEN y un DESTINO", ex.getReason());
    }

    @Test
    void calcularPrecioTrayecto_jsonMalformado_usaFallback() {
        CalcularPrecioTrayectoRequestDTO req = new CalcularPrecioTrayectoRequestDTO();
        req.setVehiculoId(vehiculoConductor.getId());
        req.setDistanciaKm(100.0);

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.findById(vehiculoConductor.getId())).thenReturn(Optional.of(vehiculoConductor));
        when(calculoPrecioIA.pedirEstimacionJson(anyString())).thenReturn("invalid json");

        PrecioTrayectoResponseDTO resp = viajeService.calcularPrecioTrayecto(conductor.getEmail(), req);

        assertEquals("FALLBACK", resp.getFuente());
        assertEquals(new BigDecimal("1.650"), resp.getPrecioCombustibleLitro());
    }

    @Test
    void crearViaje_conIntermediasMultiples_ok() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.findById(vehiculoConductor.getId())).thenReturn(Optional.of(vehiculoConductor));
        when(viajeRepository.existsBySlug(anyString())).thenReturn(false);
        when(viajeRepository.save(any(Viaje.class))).thenAnswer(inv -> inv.getArgument(0));

        Parada origen = parada(TipoParada.ORIGEN, "Sevilla", null, null);
        Parada int1 = parada(TipoParada.INTERMEDIA, "Jerez", null, null);
        Parada int2 = parada(TipoParada.INTERMEDIA, "Algeciras", null, null);
        Parada destino = parada(TipoParada.DESTINO, "Tarifa", null, null);
        viajeBase.setParadas(new ArrayList<>(List.of(origen, int1, int2, destino)));

        Viaje result = viajeService.crearViaje(conductor.getEmail(), viajeBase);

        assertEquals(4, result.getParadas().size());
        assertEquals(1, result.getParadas().get(0).getOrden());
        assertEquals(2, result.getParadas().get(1).getOrden());
        assertEquals(3, result.getParadas().get(2).getOrden());
        assertEquals(4, result.getParadas().get(3).getOrden());
    }

    @Test
    void cancelarViaje_ok_sinReservas() {
        String slug = "viaje-sin-reservas";
        viajeBase.setSlug(slug);
        viajeBase.setPersona(conductor);
        viajeBase.setEstado(EstadoViaje.PENDIENTE);
        viajeBase.setFechaHoraSalida(LocalDateTime.now().plusHours(24));

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));
        when(reservaRepository.findByViajeAndEstadoNot(viajeBase, EstadoReserva.CANCELADA))
                .thenReturn(List.of());

        ViajeDTO result = viajeService.cancelarViaje(conductor.getEmail(), slug);

        assertEquals("CANCELADO", result.getEstado());
        verify(viajeRepository).save(viajeBase);
        verify(reservaRepository).findByViajeAndEstadoNot(viajeBase, EstadoReserva.CANCELADA);
    }

    @Test
    void cancelarViajesPendientesExpirados_sinViajesExpirados() {
        when(viajeRepository.findByEstadoAndFechaHoraSalidaBefore(eq(EstadoViaje.PENDIENTE), any()))
                .thenReturn(List.of());

        int procesados = viajeService.cancelarViajesPendientesExpirados();

        assertEquals(0, procesados);
        verify(viajeRepository, never()).save(any(Viaje.class));
        verify(personaRepository, never()).save(any(Persona.class));
    }

    @Test
    void actualizarViaje_ok_actualizaFechaYPrecio() {
        String slug = "sevilla-cadiz-2026-05-01";
        viajeBase.setSlug(slug);
        viajeBase.setPersona(conductor);
        viajeBase.setFechaHoraSalida(LocalDateTime.now().plusHours(24));
        viajeBase.setPrecio(new BigDecimal("10.00"));

        Viaje viajeEditado = new Viaje();
        viajeEditado.setFechaHoraSalida(LocalDateTime.now().plusHours(26));
        viajeEditado.setPrecio(new BigDecimal("12.00"));

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));
        when(reservaRepository.findByViajeAndEstadoNot(viajeBase, EstadoReserva.CANCELADA)).thenReturn(List.of());
        when(viajeRepository.save(any(Viaje.class))).thenAnswer(inv -> inv.getArgument(0));

        ViajeDTO result = viajeService.actualizarViaje(conductor.getEmail(), slug, viajeEditado);

        assertEquals("12.00", result.getPrecio().toString());
        verify(viajeRepository).save(viajeBase);
    }

    @Test
    void actualizarViaje_error_usuarioNoEsConductor_lanza403() {
        String slug = "sevilla-cadiz-2026-05-01";
        viajeBase.setSlug(slug);
        viajeBase.setPersona(conductor);

        Viaje viajeEditado = new Viaje();
        viajeEditado.setPrecio(new BigDecimal("12.00"));

        when(personaRepository.findByEmail(otroUsuario.getEmail())).thenReturn(Optional.of(otroUsuario));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.actualizarViaje(otroUsuario.getEmail(), slug, viajeEditado));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void actualizarViaje_error_menosDe12Horas_lanza400() {
        String slug = "sevilla-cadiz-2026-05-01";
        viajeBase.setSlug(slug);
        viajeBase.setPersona(conductor);
        viajeBase.setFechaHoraSalida(LocalDateTime.now().plusHours(11));

        Viaje viajeEditado = new Viaje();
        viajeEditado.setPrecio(new BigDecimal("12.00"));

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.actualizarViaje(conductor.getEmail(), slug, viajeEditado));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void actualizarViaje_error_plazasInsuficientes_lanza400() {
        String slug = "sevilla-cadiz-2026-05-01";
        viajeBase.setSlug(slug);
        viajeBase.setPersona(conductor);
        viajeBase.setFechaHoraSalida(LocalDateTime.now().plusHours(24));
        viajeBase.setPlazasDisponibles(2);

        Reserva reserva = new Reserva();
        reserva.setCantidadPlazas(3);
        reserva.setViaje(viajeBase);

        Viaje viajeEditado = new Viaje();
        viajeEditado.setPlazasDisponibles(2);

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));
        when(reservaRepository.findByViajeAndEstadoNot(viajeBase, EstadoReserva.CANCELADA)).thenReturn(List.of(reserva));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.actualizarViaje(conductor.getEmail(), slug, viajeEditado));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void actualizarViaje_ok_aumentaPlazas() {
        String slug = "sevilla-cadiz-2026-05-01";
        viajeBase.setSlug(slug);
        viajeBase.setPersona(conductor);
        viajeBase.setFechaHoraSalida(LocalDateTime.now().plusHours(24));
        viajeBase.setPlazasDisponibles(1);

        Reserva reserva = new Reserva();
        reserva.setCantidadPlazas(2);
        reserva.setViaje(viajeBase);

        Viaje viajeEditado = new Viaje();
        viajeEditado.setPlazasDisponibles(5);

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));
        when(reservaRepository.findByViajeAndEstadoNot(viajeBase, EstadoReserva.CANCELADA)).thenReturn(List.of(reserva));
        when(viajeRepository.save(any(Viaje.class))).thenAnswer(inv -> inv.getArgument(0));

        ViajeDTO result = viajeService.actualizarViaje(conductor.getEmail(), slug, viajeEditado);

        assertEquals(3, viajeBase.getPlazasDisponibles());
        verify(notificacionRepository).save(any(Notificacion.class));
    }

    @Test
    void buscarViajesPublicos_ok_filtroCompleto() {
        Viaje viaje = viajeCompleto(50L, "sevilla-cadiz-2026-05-01");
        viaje.setEstado(EstadoViaje.PENDIENTE);

        when(viajeRepository.buscarViajesPublicosConFecha(
            any(), 
            any(LocalDateTime.class), 
            any(LocalDateTime.class)))
            .thenReturn(List.of(viaje));

        List<ViajeDTO> result = viajeService.buscarViajesPublicos("Sevilla", "Cadiz", LocalDate.of(2026, 5, 1));

        assertEquals(1, result.size());
        assertEquals(50L, result.get(0).getId());
    }

    @Test
    void buscarViajesPublicos_sinFecha_ok() {
        Viaje viaje = viajeCompleto(51L, "madrid-barcelona-2026-05-05");
        viaje.setEstado(EstadoViaje.PENDIENTE);
        viaje.getParadas().get(0).setLocalizacion("Madrid");
        viaje.getParadas().get(1).setLocalizacion("Barcelona");

        when(viajeRepository.buscarViajesPublicosSinFecha(any()))
            .thenReturn(List.of(viaje));

        List<ViajeDTO> result = viajeService.buscarViajesPublicos("Madrid", "Barcelona", null);

        assertEquals(1, result.size());
    }

    @Test
    void buscarViajesPublicos_filtraPorOrigen_ok() {
        Viaje viaje1 = viajeCompleto(52L, "sevilla-cadiz-2026-05-01");
        viaje1.setEstado(EstadoViaje.PENDIENTE);

        Viaje viaje2 = viajeCompleto(53L, "madrid-cadiz-2026-05-01");
        viaje2.setEstado(EstadoViaje.PENDIENTE);
        viaje2.getParadas().get(0).setLocalizacion("Madrid");

        when(viajeRepository.buscarViajesPublicosConFecha(
            any(), 
            any(LocalDateTime.class), 
            any(LocalDateTime.class)))
            .thenReturn(List.of(viaje1, viaje2));

        List<ViajeDTO> result = viajeService.buscarViajesPublicos("Sevilla", "Cadiz", LocalDate.of(2026, 5, 1));

        assertEquals(1, result.size());
        assertEquals(52L, result.get(0).getId());
    }

    @Test
    void buscarViajesPublicos_listaVacia_ok() {
        when(viajeRepository.buscarViajesPublicosSinFecha(any()))
            .thenReturn(List.of());

        List<ViajeDTO> result = viajeService.buscarViajesPublicos("Inexistente", "NoExiste", null);

        assertEquals(0, result.size());
    }

    @Test
    void buscarViajesPublicos_sinOrigin_aceptaTodos_ok() {
        Viaje viaje1 = viajeCompleto(54L, "sevilla-cadiz-2026-05-01");
        viaje1.setEstado(EstadoViaje.PENDIENTE);

        Viaje viaje2 = viajeCompleto(55L, "madrid-cadiz-2026-05-01");
        viaje2.setEstado(EstadoViaje.PENDIENTE);

        when(viajeRepository.buscarViajesPublicosSinFecha(any()))
            .thenReturn(List.of(viaje1, viaje2));

        List<ViajeDTO> result = viajeService.buscarViajesPublicos("", "Cadiz", null);

        assertEquals(2, result.size());
    }

    @Test
    void actualizarViaje_error_viajeNoEncontrado_lanza404() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug("no-existe")).thenReturn(Optional.empty());

        Viaje viajeEditado = new Viaje();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.actualizarViaje(conductor.getEmail(), "no-existe", viajeEditado));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void actualizarViaje_error_usuarioNoEncontrado_lanza401() {
        when(personaRepository.findByEmail("missing@compicar.com")).thenReturn(Optional.empty());

        Viaje viajeEditado = new Viaje();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.actualizarViaje("missing@compicar.com", "slug", viajeEditado));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void cancelarViajesExpirados_delegaEnServicio() {
        ViajeService viajeServiceMock = mock(ViajeService.class);
        ViajeRecurrenteService viajeRecurrenteServiceMock = mock(ViajeRecurrenteService.class);
        ProgramadorCancelacionViajes programador = new ProgramadorCancelacionViajes(viajeServiceMock, viajeRecurrenteServiceMock);

        programador.cancelarViajesExpirados();

        verify(viajeServiceMock, times(1)).cancelarViajesPendientesExpirados();
    }

    @Test
    void cancelarViajeIncompareceConductor_ok_cancelaReembolsaYPenalizaConductor() throws Exception {
        String slug = "sevilla-cadiz-incomparece";
        viajeBase.setSlug(slug);
        viajeBase.setPersona(conductor);
        viajeBase.setEstado(EstadoViaje.PENDIENTE);
        
        // Simular salida hace 30 minutos (dentro del rango de 15 min a 2 horas)
        viajeBase.setFechaHoraSalida(LocalDateTime.now().minusMinutes(30));

        Reserva reservaPasajero = new Reserva();
        reservaPasajero.setPersona(otroUsuario);
        reservaPasajero.setEstado(EstadoReserva.CONFIRMADA);
        reservaPasajero.setViaje(viajeBase);

        Pago pago = new Pago();
        pago.setStripePaymentIntentId("pi_test_123");
        reservaPasajero.setPago(pago);

        viajeBase.setReservas(List.of(reservaPasajero));

        when(personaRepository.findByEmail(otroUsuario.getEmail())).thenReturn(Optional.of(otroUsuario));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));
        when(reservaRepository.findByViajeAndEstadoNot(viajeBase, EstadoReserva.CANCELADA))
                .thenReturn(List.of(reservaPasajero));
        when(stripeService.liberarFondos("pi_test_123")).thenReturn(EstadoPago.REEMBOLSADO);

        int cancelacionesPrevias = conductor.getNumeroCancelaciones();

        ViajeDTO result = viajeService.cancelarViajeIncompareceConductor(otroUsuario.getEmail(), slug);

        assertEquals("CANCELADO", result.getEstado());
        assertEquals(EstadoReserva.CANCELADA, reservaPasajero.getEstado());
        assertEquals(EstadoPago.REEMBOLSADO, pago.getEstado());
        assertEquals(cancelacionesPrevias + 1, conductor.getNumeroCancelaciones());

        verify(viajeRepository).save(viajeBase);
        verify(personaRepository).save(conductor);
    }

    @Test
    void cancelarViajeIncompareceConductor_error_esElConductor_lanza403() {
        String slug = "sevilla-cadiz-test";
        viajeBase.setSlug(slug);
        viajeBase.setPersona(conductor);

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.cancelarViajeIncompareceConductor(conductor.getEmail(), slug));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("El conductor no puede reportar su propia incomparecencia"));
    }

    @Test
    void cancelarViajeIncompareceConductor_error_sinReservaConfirmada_lanza403() {
        String slug = "sevilla-cadiz-test";
        viajeBase.setSlug(slug);
        viajeBase.setPersona(conductor);
        viajeBase.setReservas(List.of()); // Sin reservas

        when(personaRepository.findByEmail(otroUsuario.getEmail())).thenReturn(Optional.of(otroUsuario));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.cancelarViajeIncompareceConductor(otroUsuario.getEmail(), slug));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Debes tener una reserva confirmada"));
    }

    @Test
    void cancelarViajeIncompareceConductor_error_estadoEnCursoOFinalizado_lanza400() {
        String slug = "sevilla-cadiz-test";
        viajeBase.setSlug(slug);
        viajeBase.setPersona(conductor);
        viajeBase.setEstado(EstadoViaje.EN_CURSO);

        Reserva r = new Reserva();
        r.setPersona(otroUsuario);
        r.setEstado(EstadoReserva.CONFIRMADA);
        viajeBase.setReservas(List.of(r));

        when(personaRepository.findByEmail(otroUsuario.getEmail())).thenReturn(Optional.of(otroUsuario));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.cancelarViajeIncompareceConductor(otroUsuario.getEmail(), slug));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void cancelarViajeIncompareceConductor_error_antesDeTiempoMinimo_lanza400() {
        String slug = "sevilla-cadiz-test";
        viajeBase.setSlug(slug);
        viajeBase.setPersona(conductor);
        viajeBase.setEstado(EstadoViaje.PENDIENTE);
        // Salida dentro de 10 minutos (todavía no ha transcurrido el tiempo)
        viajeBase.setFechaHoraSalida(LocalDateTime.now().plusMinutes(10));

        Reserva r = new Reserva();
        r.setPersona(otroUsuario);
        r.setEstado(EstadoReserva.CONFIRMADA);
        viajeBase.setReservas(List.of(r));

        when(personaRepository.findByEmail(otroUsuario.getEmail())).thenReturn(Optional.of(otroUsuario));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.cancelarViajeIncompareceConductor(otroUsuario.getEmail(), slug));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Aún no ha transcurrido el tiempo de espera"));
    }

    @Test
    void cancelarViajeIncompareceConductor_error_despuesDeTiempoMaximo_lanza400() {
        String slug = "sevilla-cadiz-test";
        viajeBase.setSlug(slug);
        viajeBase.setPersona(conductor);
        viajeBase.setEstado(EstadoViaje.PENDIENTE);
        // Salida fue hace 3 horas (excede las 2 horas)
        viajeBase.setFechaHoraSalida(LocalDateTime.now().minusHours(3));

        Reserva r = new Reserva();
        r.setPersona(otroUsuario);
        r.setEstado(EstadoReserva.CONFIRMADA);
        viajeBase.setReservas(List.of(r));

        when(personaRepository.findByEmail(otroUsuario.getEmail())).thenReturn(Optional.of(otroUsuario));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.cancelarViajeIncompareceConductor(otroUsuario.getEmail(), slug));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("El plazo para reportar la incomparecencia del conductor ha expirado"));
    }

    @Test
    void cancelarViajeConjunto_ok_cancelaPadreYOcurrenciasRecurrentes() throws Exception {
        String slugPadre = "viaje-padre-recurrente";
        viajeBase.setSlug(slugPadre);
        viajeBase.setPersona(conductor);
        viajeBase.setEstado(EstadoViaje.PENDIENTE);

        // Viaje recurrente secundario asociado
        ViajeRecurrente vr = new ViajeRecurrente();
        ReflectionTestUtils.setField(vr, "id", 500L);
        vr.setEstado(EstadoViaje.PENDIENTE);
        vr.setPrecio(new BigDecimal("10.00"));
        vr.setFechaHoraSalida(LocalDateTime.now().plusDays(2));

        Reserva reservaRecurrente = new Reserva();
        reservaRecurrente.setCantidadPlazas(1);
        reservaRecurrente.setPersona(otroUsuario);
        Pago pagoRecurrente = new Pago();
        pagoRecurrente.setId(99L);
        pagoRecurrente.setStripePaymentIntentId("pi_rec_123");
        reservaRecurrente.setPago(pagoRecurrente);

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug(slugPadre)).thenReturn(Optional.of(viajeBase));
        when(viajeRecurrenteRepository.findByViaje(viajeBase)).thenReturn(List.of(vr));
        when(reservaRepository.findByViajeRecurrenteIdAndEstadoNot(500L, EstadoReserva.CANCELADA))
                .thenReturn(List.of(reservaRecurrente));
        when(reservaRepository.findByPagoIdAndEstadoNot(99L, EstadoReserva.CANCELADA))
                .thenReturn(List.of()); // No quedan más reservas tras esta cancelación

        ViajeDTO result = viajeService.cancelarViajeConjunto(conductor.getEmail(), slugPadre);

        assertEquals("CANCELADO", result.getEstado());
        assertEquals(EstadoViaje.CANCELADO, vr.getEstado());
        assertEquals(EstadoReserva.CANCELADA, reservaRecurrente.getEstado());

        verify(stripeService).liberarFondos("pi_rec_123");
        verify(viajeRepository).save(viajeBase);
        verify(viajeRecurrenteRepository).save(vr);
        verify(notificacionRepository).save(any(Notificacion.class));
    }

    @Test
    void cancelarViajeConjunto_error_usuarioNoEsConductor_lanza403() {
        String slugPadre = "viaje-padre-recurrente";
        viajeBase.setSlug(slugPadre);
        viajeBase.setPersona(conductor);

        when(personaRepository.findByEmail(otroUsuario.getEmail())).thenReturn(Optional.of(otroUsuario));
        when(viajeRepository.findBySlug(slugPadre)).thenReturn(Optional.of(viajeBase));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.cancelarViajeConjunto(otroUsuario.getEmail(), slugPadre));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void cancelarViajeConjunto_error_padreYaCanceladoOFinalizado_lanza400() {
        String slugPadre = "viaje-padre-recurrente";
        viajeBase.setSlug(slugPadre);
        viajeBase.setPersona(conductor);
        viajeBase.setEstado(EstadoViaje.CANCELADO);

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug(slugPadre)).thenReturn(Optional.of(viajeBase));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> viajeService.cancelarViajeConjunto(conductor.getEmail(), slugPadre));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void cancelarViaje_conReservasActivas_incrementaCancelacionesConductor() {
        String slug = "viaje-con-reservas-activas";
        viajeBase.setSlug(slug);
        viajeBase.setPersona(conductor);
        viajeBase.setEstado(EstadoViaje.PENDIENTE);

        Reserva reservaConfirmada = new Reserva();
        reservaConfirmada.setPersona(otroUsuario);
        reservaConfirmada.setEstado(EstadoReserva.CONFIRMADA);
        viajeBase.setReservas(List.of(reservaConfirmada));

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));
        when(reservaRepository.findByViajeAndEstadoNot(viajeBase, EstadoReserva.CANCELADA))
                .thenReturn(List.of(reservaConfirmada));

        int cancelacionesAntes = conductor.getNumeroCancelaciones();

        viajeService.cancelarViaje(conductor.getEmail(), slug);

        assertEquals(cancelacionesAntes + 1, conductor.getNumeroCancelaciones());
        verify(personaRepository).save(conductor);
    }

    @Test
    void cancelarViaje_error_stripeException_lanzaExcepcion() throws Exception {
        String slug = "viaje-fallo-stripe";
        viajeBase.setSlug(slug);
        viajeBase.setPersona(conductor);
        viajeBase.setEstado(EstadoViaje.PENDIENTE);

        Reserva reserva = new Reserva();
        reserva.setPersona(otroUsuario);
        Pago pago = new Pago();
        pago.setStripePaymentIntentId("pi_error");
        reserva.setPago(pago);

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));
        when(reservaRepository.findByViajeAndEstadoNot(viajeBase, EstadoReserva.CANCELADA))
                .thenReturn(List.of(reserva));
        when(stripeService.liberarFondos("pi_error")).thenThrow(new com.stripe.exception.ApiConnectionException("Error conexion Stripe"));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> viajeService.cancelarViaje(conductor.getEmail(), slug));

        assertTrue(ex.getMessage().contains("Error al reembolsar el pago en Stripe"));
    }

    private Parada parada(TipoParada tipo, String loc, LocalDateTime fecha, Integer orden) {
        Parada p = new Parada();
        p.setTipo(tipo);
        p.setLocalizacion(loc);
        p.setFechaHora(fecha);
        p.setOrden(orden);
        return p;
    }

    private Viaje viajeCompleto(Long id, String slug) {
        Viaje v = new Viaje();
        ReflectionTestUtils.setField(v, "id", id);
        v.setFechaHoraSalida(salida);
        v.setEstado(EstadoViaje.PENDIENTE);
        v.setPlazasDisponibles(3);
        v.setPrecio(new BigDecimal("9.90"));
        v.setVehiculo(vehiculoConductor);
        v.setPersona(conductor);
        v.setSlug(slug);

        Parada o = new Parada();
        ReflectionTestUtils.setField(o, "id", 100L + id);
        o.setTipo(TipoParada.ORIGEN);
        o.setLocalizacion("Sevilla");
        o.setOrden(1);
        o.setViaje(v);

        Parada d = new Parada();
        ReflectionTestUtils.setField(d, "id", 200L + id);
        d.setTipo(TipoParada.DESTINO);
        d.setLocalizacion("Cadiz");
        d.setOrden(2);
        d.setViaje(v);

        v.setParadas(List.of(o, d));
        return v;
    }

    @Test
    void finalizarViaje_ok_capturaPagosYActualizaFondos() throws Exception { // <- CORREGIDO AQUÍ CON throws Exception
        String slug = "sevilla-cadiz-2026-05-01";
        viajeBase.setSlug(slug);
        viajeBase.setPersona(conductor);
        viajeBase.setEstado(EstadoViaje.PENDIENTE);
        viajeBase.setPrecio(new BigDecimal("10.00"));

        Reserva reserva = new Reserva();
        reserva.setPersona(otroUsuario);
        reserva.setViaje(viajeBase);
        reserva.setCantidadPlazas(2);

        Pago pago = new Pago();
        pago.setStripePaymentIntentId("pi_12345");
        pago.setEstado(EstadoPago.PENDIENTE);
        reserva.setPago(pago);

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));
        when(reservaRepository.findByViajeAndEstadoNot(viajeBase, EstadoReserva.CANCELADA))
                .thenReturn(List.of(reserva));

        ViajeDTO result = viajeService.finalizarViaje(conductor.getEmail(), slug);

        assertEquals(EstadoViaje.FINALIZADO, viajeBase.getEstado());
        assertEquals(EstadoPago.CAPTURADO, pago.getEstado());
        assertEquals(new BigDecimal("20.00"), pago.getImporteLiberadoConductor());
        assertEquals(new BigDecimal("20.00"), conductor.getFondosActuales());
        assertEquals(new BigDecimal("20.00"), conductor.getFondosTotales());

        verify(stripeService).confirmarCaptura("pi_12345");
        verify(pagoRepository).save(pago);
        verify(notificacionRepository).save(any(Notificacion.class));
        verify(viajeRepository).save(viajeBase);
        verify(personaRepository).save(conductor);
    }

    @Test
    void finalizarViaje_error_usuarioNoEncontrado_lanza401() {
        when(personaRepository.findByEmail("missing@compicar.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeService.finalizarViaje("missing@compicar.com", "slug"));

        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Usuario no encontrado", ex.getReason());
    }

    @Test
    void finalizarViaje_error_viajeNoEncontrado_lanza404() {
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug("no-existe")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeService.finalizarViaje(conductor.getEmail(), "no-existe"));

        assertEquals(404, ex.getStatusCode().value());
        assertEquals("Viaje no encontrado", ex.getReason());
    }

    @Test
    void finalizarViaje_error_usuarioNoEsConductor_lanza403() {
        viajeBase.setPersona(conductor);
        String slug = "sevilla-cadiz-2026-05-01";

        when(personaRepository.findByEmail(otroUsuario.getEmail())).thenReturn(Optional.of(otroUsuario));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeService.finalizarViaje(otroUsuario.getEmail(), slug));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("Solo el conductor puede finalizar este viaje", ex.getReason());
    }

    @Test
    void finalizarViaje_error_viajeYaFinalizado_lanza400() {
        viajeBase.setPersona(conductor);
        viajeBase.setEstado(EstadoViaje.FINALIZADO);
        String slug = "sevilla-cadiz-2026-05-01";

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeService.finalizarViaje(conductor.getEmail(), slug));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("No se puede finalizar un viaje en estado FINALIZADO"));
    }

    @Test
    void finalizarViaje_error_viajeCancelado_lanza400() {
        viajeBase.setPersona(conductor);
        viajeBase.setEstado(EstadoViaje.CANCELADO);
        String slug = "sevilla-cadiz-2026-05-01";

        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));
        when(viajeRepository.findBySlug(slug)).thenReturn(Optional.of(viajeBase));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeService.finalizarViaje(conductor.getEmail(), slug));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("No se puede finalizar un viaje en estado CANCELADO"));
    }
}