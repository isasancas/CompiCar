package com.compicar.viaje;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.compicar.parada.Parada;
import com.compicar.parada.TipoParada;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.vehiculo.TipoVehiculo;
import com.compicar.vehiculo.Vehiculo;
import com.compicar.vehiculo.VehiculoRepository;
import com.compicar.viaje.dto.CalcularPrecioTrayectoRequestDTO;
import com.compicar.viaje.dto.PrecioTrayectoResponseDTO;
import com.compicar.viaje.dto.ViajeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

        Parada origen = parada(TipoParada.ORIGEN, "Sevilla", null, null);
        Parada destino = parada(TipoParada.DESTINO, "Cadiz", null, null);
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
        assertEquals(new BigDecimal("6.80"), resp.getPrecioMinimoPasajero());
        assertEquals(new BigDecimal("10.20"), resp.getPrecioMaximoPasajero());
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
}