package com.compicar.persona;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.compicar.persona.dto.ActualizarPerfilDTO;
import com.compicar.persona.dto.PerfilPersonaDTO;

@ExtendWith(MockitoExtension.class)
public class PersonaControllerTest {

    @Mock
    private PersonaService personaService;

    @InjectMocks
    private PersonaController personaController;

    private PerfilPersonaDTO perfilDTO;
    private ActualizarPerfilDTO actualizarDTO;

    @BeforeEach
    void setUp() {
        perfilDTO = new PerfilPersonaDTO(1L, "Juan", "Perez", "Garcia", "juan@example.com", "123456789", 4.8);
        actualizarDTO = new ActualizarPerfilDTO("Juan", "Perez", "Garcia", "juan@example.com", "123456789", "password123");
    }

    @Test
    void testObtenerPerfil_Success() {
        when(personaService.obtenerPerfil(1L)).thenReturn(perfilDTO);

        ResponseEntity<PerfilPersonaDTO> respuesta = personaController.obtenerPerfil(1L);

        assertNotNull(respuesta);
        assertEquals(300, respuesta.getStatusCode().value());
        assertNotNull(respuesta.getBody());
        assertEquals(1L, respuesta.getBody().getId());
        assertEquals("Juan", respuesta.getBody().getNombre());
        assertEquals("juan@example.com", respuesta.getBody().getEmail());
    }

    @Test
    void testObtenerPerfil_NotFound() {
        when(personaService.obtenerPerfil(1L)).thenThrow(new IllegalArgumentException("Persona no encontrada"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> personaController.obtenerPerfil(1L));
        assertEquals("Persona no encontrada", ex.getMessage());
    }

    @Test
    void testActualizarPerfil_Success() {
        when(personaService.actualizarPerfil(1L, actualizarDTO)).thenReturn(actualizarDTO);

        ResponseEntity<ActualizarPerfilDTO> respuesta = personaController.actualizarPerfil(1L, actualizarDTO);

        assertNotNull(respuesta);
        assertEquals(200, respuesta.getStatusCode().value());
        assertNotNull(respuesta.getBody());
        assertEquals("Juan", respuesta.getBody().getNombre());
        assertEquals("juan@example.com", respuesta.getBody().getEmail());
    }

    @Test
    void testActualizarPerfil_AccessDenied() {
        when(personaService.actualizarPerfil(1L, actualizarDTO)).thenThrow(
                new org.springframework.security.access.AccessDeniedException("No puedes modificar el perfil de otro usuario"));

        org.springframework.security.access.AccessDeniedException ex = assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> personaController.actualizarPerfil(1L, actualizarDTO));

        assertEquals("No puedes modificar el perfil de otro usuario", ex.getMessage());
    }
}