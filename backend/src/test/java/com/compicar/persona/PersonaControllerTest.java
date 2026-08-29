package com.compicar.persona;

import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

        perfilDTO = new PerfilPersonaDTO(1L, "Juan", "Perez", "Garcia", "juan@example.com", "123456789", 4.8, "juan-perez-garcia", List.of(), null, null, 0);
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

   @Test
    void testRetirarFondos_Success() throws Exception {
        // 1. Simulamos el contexto de seguridad que tu controlador consulta estáticamente
        org.springframework.security.core.Authentication auth = 
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("juan@example.com", null, java.util.List.of());
        
        org.springframework.security.core.context.SecurityContext securityContext = 
            org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);

        try {
            java.util.Map<String, Object> respuestaMock = java.util.Map.of(
                "status", "SUCCESS",
                "mensaje", "Retiro completado con éxito",
                "transferId", "tr_123456"
            );

            when(personaService.retirarFondos("juan@example.com")).thenReturn(respuestaMock);

            mockMvc.perform(post("/api/personas/retirar-fondos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.mensaje").value("Retiro completado con éxito"))
                    .andExpect(jsonPath("$.transferId").value("tr_123456"));
        } finally {
            // 2. Limpiamos el contexto de seguridad al finalizar el test para no afectar a otros tests
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    void testRetirarFondos_BadRequest_SaldoInsuficiente() throws Exception {
        // Simulamos autenticación
        org.springframework.security.core.Authentication auth = 
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("juan@example.com", null, java.util.List.of());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            // Simulamos que el servicio lanza un ResponseStatusException por saldo menor a 10€
            when(personaService.retirarFondos("juan@example.com"))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, 
                    "Se requiere un saldo mínimo de 10.00€ para realizar la retirada."
                ));

            mockMvc.perform(post("/api/personas/retirar-fondos"))
                    .andExpect(status().isBadRequest()); // Comprobamos que el controlador devuelve el código 400
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}