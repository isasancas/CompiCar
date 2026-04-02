package com.compicar.inicioSesion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.compicar.autenticacion.inicioSesion.InicioSesion;
import com.compicar.autenticacion.inicioSesion.InicioSesionController;
import com.compicar.autenticacion.inicioSesion.InicioSesionService;
import com.compicar.config.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class InicioSesionControllerTests {

    private MockMvc mockMvc;

    @Mock
    private InicioSesionService inicioSesionService;

    @InjectMocks
    private InicioSesionController inicioSesionController;

    private InicioSesion request;
    private String tokenSimulado;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        
        mockMvc = MockMvcBuilders.standaloneSetup(inicioSesionController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
        objectMapper = new ObjectMapper();

        request = new InicioSesion();
        request.setEmail("usuario@example.com");
        request.setContrasena("password123");

        tokenSimulado = "jwt.token.de.prueba";
    }

    @Test
    void testLogin_Success() throws Exception {
        // Given
        when(inicioSesionService.iniciarSesion(any(InicioSesion.class))).thenReturn(tokenSimulado);

        // When & Then
        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(tokenSimulado));
    }

    @Test
    void testLogin_CredencialesInvalidas_Returns401() throws Exception {
        // Given
        when(inicioSesionService.iniciarSesion(any(InicioSesion.class)))
                .thenThrow(new RuntimeException("Email o contraseña incorrectos"));

        // When & Then
        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError()); 
    }

    @Test
    void testLogin_ValidacionFallida_BadRequest() throws Exception {
        // Given
        InicioSesion requestInvalido = new InicioSesion();
        requestInvalido.setEmail(""); 

        // When & Then
        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest());
    }
}
