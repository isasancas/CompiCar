package com.compicar.sugerencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.compicar.correo.CorreoService;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaService;

@ExtendWith(MockitoExtension.class)
class SugerenciaServiceTest {

    @Mock
    private PersonaService personaService;

    @Mock
    private CorreoService correoService;

    @InjectMocks
    private SugerenciaService sugerenciaService;

    private Persona persona;

    @BeforeEach
    void setUp() {
        persona = new Persona();
        persona.setEmail("user@compicar.com");
        persona.setNombre("Pedro");
    }

    @Test
    void enviar_exito() {
        when(personaService.obtenerPersonaPorEmail("user@compicar.com")).thenReturn(persona);

        sugerenciaService.enviar("user@compicar.com", "Me gustaría sugerir una mejora.");

        verify(correoService, times(1)).sendSugerencia("user@compicar.com", "Pedro", "Me gustaría sugerir una mejora.");
    }

    @Test
    void enviar_usuarioNoEncontrado_lanza401() {
        when(personaService.obtenerPersonaPorEmail("desconocido@compicar.com")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> sugerenciaService.enviar("desconocido@compicar.com", "Mensaje"));

        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Usuario no encontrado", ex.getReason());
        verify(correoService, never()).sendSugerencia(anyString(), anyString(), anyString());
    }

    @Test
    void enviar_limiteDiarioExcedido_lanza429() {
        when(personaService.obtenerPersonaPorEmail("user@compicar.com")).thenReturn(persona);

        // Primeros 3 envíos exitosos (límite diario máximo)
        sugerenciaService.enviar("user@compicar.com", "Sugerencia 1");
        sugerenciaService.enviar("user@compicar.com", "Sugerencia 2");
        sugerenciaService.enviar("user@compicar.com", "Sugerencia 3");

        // El cuarto envío debe lanzar 429 TOO_MANY_REQUESTS
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> sugerenciaService.enviar("user@compicar.com", "Sugerencia 4"));

        assertEquals(429, ex.getStatusCode().value());
        assertEquals("Has alcanzado el límite de 3 mensajes diarios", ex.getReason());
        verify(correoService, times(3)).sendSugerencia(eq("user@compicar.com"), eq("Pedro"), anyString());
    }

    @Test
    void enviar_diferentesUsuarios_tienenContadoresIndependientes() {
        Persona persona2 = new Persona();
        persona2.setEmail("user2@compicar.com");
        persona2.setNombre("María");

        when(personaService.obtenerPersonaPorEmail("user@compicar.com")).thenReturn(persona);
        when(personaService.obtenerPersonaPorEmail("user2@compicar.com")).thenReturn(persona2);

        // Usuario 1 agota su límite de 3
        sugerenciaService.enviar("user@compicar.com", "Msg 1");
        sugerenciaService.enviar("user@compicar.com", "Msg 2");
        sugerenciaService.enviar("user@compicar.com", "Msg 3");

        // Usuario 2 todavía puede enviar sin ser afectado por el límite de Usuario 1
        sugerenciaService.enviar("user2@compicar.com", "Mensaje de María");

        verify(correoService, times(3)).sendSugerencia(eq("user@compicar.com"), eq("Pedro"), anyString());
        verify(correoService, times(1)).sendSugerencia("user2@compicar.com", "María", "Mensaje de María");
    }
}