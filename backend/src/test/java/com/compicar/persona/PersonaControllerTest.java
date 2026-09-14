package com.compicar.persona;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

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
        personaEntidad.setId(1L);
        personaEntidad.setEmail("juan@example.com");
        personaEntidad.setNombre("Juan");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void simularAutenticacion(String email) {
        Authentication auth = new UsernamePasswordAuthenticationToken(email, null, List.of());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    private static String asJsonString(final Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
    void testObtenerMiPerfil_Success() throws Exception {
        simularAutenticacion("juan@example.com");
        when(personaService.obtenerPersonaPorEmail("juan@example.com")).thenReturn(personaEntidad);
        when(personaService.obtenerPerfil(1L)).thenReturn(perfilDTO);

        mockMvc.perform(get("/api/personas/perfil"))
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

    @Test
    void testObtenerPerfilPublicoPorSlug_Success() throws Exception {
        when(personaService.obtenerPerfilPorSlug("juan-perez-garcia")).thenReturn(perfilDTO);

        mockMvc.perform(get("/api/personas/juan-perez-garcia/perfil-publico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("juan-perez-garcia"))
                .andExpect(jsonPath("$.nombre").value("Juan"));
    }

    @Test
    void testSubirFoto_Success() throws Exception {
        simularAutenticacion("juan@example.com");
        String jsonPayload = "{\"foto\":\"base64_string_aqui\"}";

        mockMvc.perform(post("/api/personas/foto")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Foto actualizada correctamente"));

        verify(personaService).subirFoto("juan@example.com", "base64_string_aqui");
    }

    @Test
    void testRetirarFondos_Success() throws Exception {
        simularAutenticacion("juan@example.com");
        
        Map<String, Object> respuestaMock = Map.of(
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
    }

    @Test
    void testRetirarFondos_BadRequest_SaldoInsuficiente() throws Exception {
        simularAutenticacion("juan@example.com");

        when(personaService.retirarFondos("juan@example.com"))
            .thenThrow(new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "Se requiere un saldo mínimo de 10.00€ para realizar la retirada."
            ));

        mockMvc.perform(post("/api/personas/retirar-fondos"))
                .andExpect(status().isBadRequest()); 
    }

    @Test
    void testObtenerTopConductores_Success() throws Exception {
        when(personaService.obtenerTopConductores()).thenReturn(List.of(perfilDTO));

        mockMvc.perform(get("/api/personas/top-conductores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("juan@example.com"));
    }
}