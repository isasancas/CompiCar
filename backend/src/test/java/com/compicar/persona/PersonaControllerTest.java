package com.compicar.persona;

import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.compicar.persona.dto.ActualizarPerfilDTO;
import com.compicar.persona.dto.PerfilPersonaDTO;

@ExtendWith(MockitoExtension.class)
class PersonaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PersonaService personaService;

    @InjectMocks
    private PersonaController personaController;

    private PerfilPersonaDTO perfilDTO;
    private ActualizarPerfilDTO actualizarDTO;
    private Persona personaEntidad;

    @BeforeEach
    void setUp() {
        
        mockMvc = MockMvcBuilders.standaloneSetup(personaController).build();

        perfilDTO = new PerfilPersonaDTO(1L, "Juan", "Perez", "Garcia", "juan@example.com", "123456789", 4.8);
        actualizarDTO = new ActualizarPerfilDTO("Juan", "Perez", "Garcia", "juan@example.com", "123456789", "password123");
        
        personaEntidad = new Persona();
        personaEntidad.setEmail("juan@example.com");
        personaEntidad.setNombre("Juan");
    }

    @Test
    void testObtenerPerfil_Success() throws Exception {
        when(personaService.obtenerPerfil(1L)).thenReturn(perfilDTO);

        mockMvc.perform(get("/api/personas/1/perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Juan"))
                .andExpect(jsonPath("$.email").value("juan@example.com"));
    }

    @Test
    void testActualizarPerfil_Success() throws Exception {
        when(personaService.actualizarPerfil(eq(1L), any(ActualizarPerfilDTO.class))).thenReturn(actualizarDTO);

        mockMvc.perform(put("/api/personas/1/perfil")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(actualizarDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("123456789"));
    }

    @Test
    void testObtenerPersonaPorEmail_Success() throws Exception {
        when(personaService.obtenerPersonaPorEmail("juan@example.com")).thenReturn(personaEntidad);

        mockMvc.perform(get("/api/personas/obtenerPorEmail")
                .param("email", "juan@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan@example.com"));
    }

    @Test
    void testObtenerPersonaPorNombrePersona_Success() throws Exception {
        when(personaService.obtenerPersonaPorNombrePersona("juanito123")).thenReturn(personaEntidad);

        mockMvc.perform(get("/api/personas/obtenerPorNombrePersona")
                .param("username", "juanito123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Juan"));
    }

    private static String asJsonString(final Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}