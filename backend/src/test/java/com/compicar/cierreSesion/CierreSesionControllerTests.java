package com.compicar.cierreSesion;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.compicar.autenticacion.inicioSesion.LogoutController;
import com.compicar.autenticacion.inicioSesion.LogoutService;

@ExtendWith(MockitoExtension.class)
public class CierreSesionControllerTests {

    private MockMvc mockMvc;

    @Mock
    private LogoutService logoutService;

    @InjectMocks
    private LogoutController logoutController;

    private String tokenValido;
    private String headerValido;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(logoutController).build();
        
        tokenValido = "jwt.token.ejemplo";
        headerValido = "Bearer " + tokenValido;
    }

    @Test
    void testLogout_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/logout")
                .header("Authorization", headerValido))
                .andExpect(status().isOk())
                .andExpect(content().string("Sesión cerrada correctamente"));

        verify(logoutService).logout(tokenValido);
    }

    @Test
    void testLogout_HeaderFaltante_BadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/logout")) // Sin header
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Authorization header missing or malformed"));

        verifyNoInteractions(logoutService);
    }

    @Test
    void testLogout_HeaderMalformado_BadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/logout")
                .header("Authorization", "TokenSinBearer 123"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Authorization header missing or malformed"));
    }

    @Test
    void testLogout_ServiceThrowsIllegalArgumentException() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("Token inválido o expirado"))
            .when(logoutService).logout(tokenValido);

        // When & Then
        mockMvc.perform(post("/api/logout")
                .header("Authorization", headerValido))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Token inválido o expirado"));
    }

    @Test
    void testLogout_InternalServerError() throws Exception {
        // Given
        doThrow(new RuntimeException("Error inesperado"))
            .when(logoutService).logout(tokenValido);

        // When & Then
        mockMvc.perform(post("/api/logout")
                .header("Authorization", headerValido))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error interno en logout"));
    }
}
