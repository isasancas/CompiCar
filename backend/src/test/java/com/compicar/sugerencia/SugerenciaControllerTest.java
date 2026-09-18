package com.compicar.sugerencia;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SugerenciaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SugerenciaService sugerenciaService;

    @InjectMocks
    private SugerenciaController sugerenciaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sugerenciaController).build();
        SecurityContextHolder.clearContext();
    }

    @Test
    void enviar_exito() throws Exception {
        autenticar("user@compicar.com");

        mockMvc.perform(post("/api/sugerencias")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mensaje\":\"  Excelente aplicación, me gusta mucho.  \"}"))
                .andExpect(status().isOk());

        verify(sugerenciaService).enviar("user@compicar.com", "Excelente aplicación, me gusta mucho.");
    }

    @Test
    void enviar_errorUsuarioNoEncontrado_401() throws Exception {
        autenticar("desconocido@compicar.com");

        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"))
                .when(sugerenciaService).enviar("desconocido@compicar.com", "Mensaje de prueba");

        mockMvc.perform(post("/api/sugerencias")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mensaje\":\"Mensaje de prueba\"}"))
                .andExpect(status().isUnauthorized());

        verify(sugerenciaService).enviar("desconocido@compicar.com", "Mensaje de prueba");
    }

    @Test
    void enviar_limiteDiarioExcedido_429() throws Exception {
        autenticar("user@compicar.com");

        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Has alcanzado el límite de 3 mensajes diarios"))
                .when(sugerenciaService).enviar("user@compicar.com", "Sugerencia recurrente");

        mockMvc.perform(post("/api/sugerencias")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mensaje\":\"Sugerencia recurrente\"}"))
                .andExpect(status().isTooManyRequests());

        verify(sugerenciaService).enviar("user@compicar.com", "Sugerencia recurrente");
    }

    @Test
    void enviar_cuerpoInvalido_400() throws Exception {
        autenticar("user@compicar.com");

        mockMvc.perform(post("/api/sugerencias")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sugerenciaService);
    }

    private void autenticar(String email) {
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(new TestingAuthenticationToken(email, null));
        SecurityContextHolder.setContext(context);
        clearInvocations(sugerenciaService);
    }
}