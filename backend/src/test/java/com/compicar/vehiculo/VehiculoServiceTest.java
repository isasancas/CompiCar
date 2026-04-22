package com.compicar.vehiculo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.vehiculo.dto.AltaVehiculoRequestDTO;
import com.compicar.vehiculo.dto.VehiculoResponseDTO;
import com.compicar.viaje.ViajeRepository;
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
class VehiculoServiceTest {

    @Mock
    private VehiculoRepository vehiculoRepository;
    @Mock
    private PersonaRepository personaRepository;
    @Mock
    private ViajeRepository viajeRepository;

    @InjectMocks
    private VehiculoServiceImpl vehiculoService;

    private Persona persona;
    private AltaVehiculoRequestDTO request;

    @BeforeEach
    void setUp() {
        persona = new Persona();
        ReflectionTestUtils.setField(persona, "id", 1L);
        persona.setEmail("driver@compicar.com");

        request = new AltaVehiculoRequestDTO();
        request.setMatricula(" 1234abc ");
        request.setMarca("  Seat ");
        request.setModelo(" Ibiza  ");
        request.setPlazas(4);
        request.setConsumo(5.2);
        request.setAnio(2020);
        request.setTipo(TipoVehiculo.COCHE);
    }

    @Test
    void crearVehiculo_ok_normalizaCamposYGeneraSlug() {
        when(personaRepository.findByEmail(persona.getEmail())).thenReturn(Optional.of(persona));
        when(vehiculoRepository.existsByMatricula("1234ABC")).thenReturn(false);
        when(vehiculoRepository.existsBySlug("seat-ibiza-1234abc")).thenReturn(false);
        when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(inv -> {
            Vehiculo v = inv.getArgument(0);
            ReflectionTestUtils.setField(v, "id", 10L);
            return v;
        });

        VehiculoResponseDTO dto = vehiculoService.crearVehiculo(persona.getEmail(), request);

        assertNotNull(dto);
        assertEquals("1234ABC", dto.getMatricula());
        assertEquals("Seat", dto.getMarca());
        assertEquals("Ibiza", dto.getModelo());
        assertEquals("seat-ibiza-1234abc", dto.getSlug());

        ArgumentCaptor<Vehiculo> captor = ArgumentCaptor.forClass(Vehiculo.class);
        verify(vehiculoRepository).save(captor.capture());
        assertEquals(persona, captor.getValue().getPersona());
    }

    @Test
    void crearVehiculo_slugDuplicado_generaSufijo() {
        when(personaRepository.findByEmail(persona.getEmail())).thenReturn(Optional.of(persona));
        when(vehiculoRepository.existsByMatricula("1234ABC")).thenReturn(false);
        when(vehiculoRepository.existsBySlug("seat-ibiza-1234abc")).thenReturn(true);
        when(vehiculoRepository.existsBySlug("seat-ibiza-1234abc-2")).thenReturn(false);
        when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(inv -> inv.getArgument(0));

        VehiculoResponseDTO dto = vehiculoService.crearVehiculo(persona.getEmail(), request);

        assertEquals("seat-ibiza-1234abc-2", dto.getSlug());
    }

    @Test
    void crearVehiculo_usuarioNoAutenticado_lanza401() {
        when(personaRepository.findByEmail(persona.getEmail())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> vehiculoService.crearVehiculo(persona.getEmail(), request));

        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Usuario no autenticado", ex.getReason());
    }

    @Test
    void crearVehiculo_matriculaDuplicada_lanza409() {
        when(personaRepository.findByEmail(persona.getEmail())).thenReturn(Optional.of(persona));
        when(vehiculoRepository.existsByMatricula("1234ABC")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> vehiculoService.crearVehiculo(persona.getEmail(), request));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals("La matricula ya existe", ex.getReason());
        verify(vehiculoRepository, never()).save(any());
    }

    @Test
    void obtenerMisVehiculos_ok_devuelveListaMapeada() {
        Vehiculo v1 = vehiculo(1L, "1234ABC", "seat-ibiza-1234abc");
        Vehiculo v2 = vehiculo(2L, "5678DEF", "seat-leon-5678def");

        when(personaRepository.findByEmail(persona.getEmail())).thenReturn(Optional.of(persona));
        when(vehiculoRepository.findByPersonaId(1L)).thenReturn(List.of(v1, v2));

        List<VehiculoResponseDTO> result = vehiculoService.obtenerMisVehiculos(persona.getEmail());

        assertEquals(2, result.size());
        assertEquals("1234ABC", result.get(0).getMatricula());
        assertEquals("seat-leon-5678def", result.get(1).getSlug());
    }

    @Test
    void obtenerMisVehiculos_usuarioNoAutenticado_lanza401() {
        when(personaRepository.findByEmail(persona.getEmail())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> vehiculoService.obtenerMisVehiculos(persona.getEmail()));

        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Usuario no autenticado", ex.getReason());
    }

    @Test
    void actualizarVehiculo_ok_actualizaCampos() {
        Vehiculo existente = vehiculo(10L, "1111AAA", "seat-ibiza-1111aaa");

        when(personaRepository.findByEmail(persona.getEmail())).thenReturn(Optional.of(persona));
        when(vehiculoRepository.findByIdAndPersonaId(10L, 1L)).thenReturn(Optional.of(existente));
        when(vehiculoRepository.existsByMatriculaAndIdNot("1234ABC", 10L)).thenReturn(false);
        when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(inv -> inv.getArgument(0));

        VehiculoResponseDTO dto = vehiculoService.actualizarVehiculo(persona.getEmail(), 10L, request);

        assertEquals("1234ABC", dto.getMatricula());
        assertEquals("Seat", dto.getMarca());
        assertEquals("Ibiza", dto.getModelo());
        assertEquals(4, dto.getPlazas());
    }

    @Test
    void actualizarVehiculo_usuarioNoAutenticado_lanza401() {
        when(personaRepository.findByEmail(persona.getEmail())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> vehiculoService.actualizarVehiculo(persona.getEmail(), 10L, request));

        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Usuario no autenticado", ex.getReason());
    }

    @Test
    void actualizarVehiculo_noEncontradoParaUsuario_lanza404() {
        when(personaRepository.findByEmail(persona.getEmail())).thenReturn(Optional.of(persona));
        when(vehiculoRepository.findByIdAndPersonaId(10L, 1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> vehiculoService.actualizarVehiculo(persona.getEmail(), 10L, request));

        assertEquals(404, ex.getStatusCode().value());
        assertEquals("Vehiculo no encontrado", ex.getReason());
    }

    @Test
    void actualizarVehiculo_matriculaDuplicadaEnOtroVehiculo_lanza409() {
        Vehiculo existente = vehiculo(10L, "1111AAA", "seat-ibiza-1111aaa");

        when(personaRepository.findByEmail(persona.getEmail())).thenReturn(Optional.of(persona));
        when(vehiculoRepository.findByIdAndPersonaId(10L, 1L)).thenReturn(Optional.of(existente));
        when(vehiculoRepository.existsByMatriculaAndIdNot("1234ABC", 10L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> vehiculoService.actualizarVehiculo(persona.getEmail(), 10L, request));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals("La matricula ya existe", ex.getReason());
    }

    @Test
    void borrarVehiculo_ok_elimina() {
        Vehiculo existente = vehiculo(10L, "1111AAA", "seat-ibiza-1111aaa");

        when(personaRepository.findByEmail(persona.getEmail())).thenReturn(Optional.of(persona));
        when(vehiculoRepository.findByIdAndPersonaId(10L, 1L)).thenReturn(Optional.of(existente));
        when(viajeRepository.existsByVehiculoId(10L)).thenReturn(false);

        vehiculoService.borrarVehiculo(persona.getEmail(), 10L);

        verify(vehiculoRepository).delete(existente);
    }

    @Test
    void borrarVehiculo_conViajesAsociados_lanza409() {
        Vehiculo existente = vehiculo(10L, "1111AAA", "seat-ibiza-1111aaa");

        when(personaRepository.findByEmail(persona.getEmail())).thenReturn(Optional.of(persona));
        when(vehiculoRepository.findByIdAndPersonaId(10L, 1L)).thenReturn(Optional.of(existente));
        when(viajeRepository.existsByVehiculoId(10L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> vehiculoService.borrarVehiculo(persona.getEmail(), 10L));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals("No se puede borrar el vehiculo porque tiene viajes asociados", ex.getReason());
        verify(vehiculoRepository, never()).delete(any());
    }

    @Test
    void borrarVehiculo_noEncontrado_lanza404() {
        when(personaRepository.findByEmail(persona.getEmail())).thenReturn(Optional.of(persona));
        when(vehiculoRepository.findByIdAndPersonaId(10L, 1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> vehiculoService.borrarVehiculo(persona.getEmail(), 10L));

        assertEquals(404, ex.getStatusCode().value());
        assertEquals("Vehiculo no encontrado", ex.getReason());
    }

    @Test
    void obtenerVehiculoPorSlug_ok() {
        Vehiculo v = vehiculo(10L, "1234ABC", "seat-ibiza-1234abc");
        when(vehiculoRepository.findBySlug("seat-ibiza-1234abc")).thenReturn(Optional.of(v));

        VehiculoResponseDTO dto = vehiculoService.obtenerVehiculoPorSlug("seat-ibiza-1234abc");

        assertEquals(10L, dto.getId());
        assertEquals("1234ABC", dto.getMatricula());
    }

    @Test
    void obtenerVehiculoPorSlug_noExiste_lanza404() {
        when(vehiculoRepository.findBySlug("no-existe")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> vehiculoService.obtenerVehiculoPorSlug("no-existe"));

        assertEquals(404, ex.getStatusCode().value());
        assertEquals("Vehiculo no encontrado", ex.getReason());
    }

    @Test
    void crearVehiculo_limite_matriculaConMinusculasYEspacios() {
        request.setMatricula(" 9999xyz ");
        when(personaRepository.findByEmail(persona.getEmail())).thenReturn(Optional.of(persona));
        when(vehiculoRepository.existsByMatricula("9999XYZ")).thenReturn(false);
        when(vehiculoRepository.existsBySlug(anyString())).thenReturn(false);
        when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(inv -> inv.getArgument(0));

        VehiculoResponseDTO dto = vehiculoService.crearVehiculo(persona.getEmail(), request);

        assertEquals("9999XYZ", dto.getMatricula());
    }

    private Vehiculo vehiculo(Long id, String matricula, String slug) {
        Vehiculo v = new Vehiculo();
        ReflectionTestUtils.setField(v, "id", id);
        v.setMatricula(matricula);
        v.setMarca("Seat");
        v.setModelo("Ibiza");
        v.setPlazas(4);
        v.setConsumo(5.0);
        v.setAnio(2020);
        v.setTipo(TipoVehiculo.COCHE);
        v.setPersona(persona);
        v.setSlug(slug);
        return v;
    }
}
