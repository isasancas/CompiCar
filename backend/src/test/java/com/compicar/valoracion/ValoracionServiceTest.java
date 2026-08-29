package com.compicar.valoracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.reserva.Reserva;
import com.compicar.valoracion.dto.ValoracionDTO;
import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;

@ExtendWith(MockitoExtension.class)
class ValoracionServiceTest {

    @Mock
    private ValoracionRepository valoracionRepository;

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private ViajeRepository viajeRepository;

    @InjectMocks
    private ValoracionServiceImpl valoracionService;

    private Persona autorPasajero;
    private Persona conductor;
    private Viaje viaje;

    @BeforeEach
    void setUp() {
        autorPasajero = new Persona();
        autorPasajero.setEmail("pasajero@compicar.com");
        ReflectionTestUtils.setField(autorPasajero, "id", 1L);

        conductor = new Persona();
        conductor.setEmail("conductor@compicar.com");
        ReflectionTestUtils.setField(conductor, "id", 2L);

        viaje = new Viaje();
        ReflectionTestUtils.setField(viaje, "id", 10L);
        viaje.setPersona(conductor);
        
        // Simular que el pasajero tiene una reserva en este viaje
        Reserva reserva = new Reserva();
        reserva.setPersona(autorPasajero);
        List<Reserva> reservas = new ArrayList<>();
        reservas.add(reserva);
        ReflectionTestUtils.setField(viaje, "reservas", reservas);

        mockearSeguridad("pasajero@compicar.com");
    }

    private void mockearSeguridad(String email) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn(email);
        
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void crearValoracion_ok() {
        ValoracionDTO peticion = new ValoracionDTO();
        peticion.setAutorId(1L);
        peticion.setValoradoId(2L);
        peticion.setViajeId(10L);
        peticion.setPuntuacion(5);
        peticion.setComentario("Excelente viaje");

        when(personaRepository.findByEmail("pasajero@compicar.com")).thenReturn(Optional.of(autorPasajero));
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));
        when(valoracionRepository.existePorAutorIdAndViajeId(1L, 10L)).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(autorPasajero));
        
        when(valoracionRepository.save(any(Valoracion.class))).thenAnswer(inv -> {
            Valoracion v = inv.getArgument(0);
            ReflectionTestUtils.setField(v, "id", 100L);
            return v;
        });

        ValoracionDTO resultado = valoracionService.crearValoracion(peticion);

        assertNotNull(resultado);
        assertEquals(5, resultado.getPuntuacion());
        assertEquals("Excelente viaje", resultado.getComentario());
        assertEquals(100L, resultado.getId());
        assertTrue(resultado.getSlug().contains("valoracion-10-1-"));
    }

    @Test
    void crearValoracion_error_noEsAutorAutenticado() {
        ValoracionDTO peticion = new ValoracionDTO();
        peticion.setAutorId(99L); // Intenta crear una valoración a nombre de otro

        when(personaRepository.findByEmail("pasajero@compicar.com")).thenReturn(Optional.of(autorPasajero));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> 
            valoracionService.crearValoracion(peticion));
        
        assertEquals("Solo puedes crear valoraciones con tu propia cuenta", ex.getMessage());
    }

    @Test
    void crearValoracion_error_noEsPasajeroDelViaje() {
        // Vaciamos las reservas para simular que no participó
        ReflectionTestUtils.setField(viaje, "reservas", new ArrayList<>());

        ValoracionDTO peticion = new ValoracionDTO();
        peticion.setAutorId(1L);
        peticion.setViajeId(10L);
        peticion.setValoradoId(2L);

        when(personaRepository.findByEmail("pasajero@compicar.com")).thenReturn(Optional.of(autorPasajero));
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> 
            valoracionService.crearValoracion(peticion));
        
        assertEquals("Solo puedes valorar un viaje en el que hayas participado como pasajero", ex.getMessage());
    }

    @Test
    void crearValoracion_error_yaValoroElViaje() {
        ValoracionDTO peticion = new ValoracionDTO();
        peticion.setAutorId(1L);
        peticion.setViajeId(10L);
        peticion.setValoradoId(2L);

        when(personaRepository.findByEmail("pasajero@compicar.com")).thenReturn(Optional.of(autorPasajero));
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));
        when(valoracionRepository.existePorAutorIdAndViajeId(1L, 10L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> 
            valoracionService.crearValoracion(peticion));
        
        assertEquals("Ya has valorado este viaje anteriormente", ex.getMessage());
    }

    @Test
    void actualizarValoracion_ok() {
        Valoracion existente = new Valoracion(4, "Bueno", autorPasajero, conductor, viaje);
        ReflectionTestUtils.setField(existente, "id", 100L);

        ValoracionDTO peticion = new ValoracionDTO();
        peticion.setPuntuacion(5);
        peticion.setComentario("Muy bueno editado");

        when(valoracionRepository.findById(100L)).thenReturn(Optional.of(existente));
        when(personaRepository.findByEmail("pasajero@compicar.com")).thenReturn(Optional.of(autorPasajero));
        when(valoracionRepository.save(any(Valoracion.class))).thenAnswer(inv -> inv.getArgument(0));

        ValoracionDTO resultado = valoracionService.actualizarValoracion(100L, peticion);

        assertEquals(5, resultado.getPuntuacion());
        assertEquals("Muy bueno editado", resultado.getComentario());
    }

    @Test
    void eliminarValoracion_ok() {
        Valoracion existente = new Valoracion(4, "Bueno", autorPasajero, conductor, viaje);
        ReflectionTestUtils.setField(existente, "id", 100L);

        when(valoracionRepository.findById(100L)).thenReturn(Optional.of(existente));
        when(personaRepository.findByEmail("pasajero@compicar.com")).thenReturn(Optional.of(autorPasajero));

        valoracionService.eliminarValoracion(100L);

        verify(valoracionRepository).delete(existente);
    }

    @Test
    void eliminarValoracion_error_noEsPropietario() {
        Persona otroAutor = new Persona();
        ReflectionTestUtils.setField(otroAutor, "id", 99L);
        Valoracion existente = new Valoracion(4, "Bueno", otroAutor, conductor, viaje);

        when(valoracionRepository.findById(100L)).thenReturn(Optional.of(existente));
        when(personaRepository.findByEmail("pasajero@compicar.com")).thenReturn(Optional.of(autorPasajero));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> 
            valoracionService.eliminarValoracion(100L));
            
        assertEquals("Solo puedes modificar o eliminar tus propias valoraciones", ex.getMessage());
    }

    @Test
    void calcularReputacion_ok() {
        when(valoracionRepository.calcularReputacion(2L)).thenReturn(4.5);
        Double reputacion = valoracionService.calcularReputacion(2L);
        assertEquals(4.5, reputacion);
    }
}
