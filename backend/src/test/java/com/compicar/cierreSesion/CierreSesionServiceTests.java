package com.compicar.cierreSesion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.compicar.autenticacion.inicioSesion.JwtUtil;
import com.compicar.autenticacion.inicioSesion.LogoutService;
import com.compicar.autenticacion.inicioSesion.TokenBlacklistService;

@ExtendWith(MockitoExtension.class)
public class CierreSesionServiceTests {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private LogoutService logoutService;

    private String tokenValido;

    @BeforeEach
    void setUp() {
        tokenValido = "eyJhbGciOiJIUzI1NiJ9.valid.token";
    }

    @Test
    void testLogout_Success() {
        // Given
        when(jwtUtil.validateToken(tokenValido)).thenReturn(true);

        // When
        logoutService.logout(tokenValido);

        // Then
        verify(tokenBlacklistService).blacklist(tokenValido);
        verify(jwtUtil).validateToken(tokenValido);
    }

    @Test
    void testLogout_TokenNulo_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            logoutService.logout(null);
        });

        assertEquals("Token inválido o expirado", exception.getMessage());
        verifyNoInteractions(tokenBlacklistService);
    }

    @Test
    void testLogout_TokenInvalido_ThrowsException() {
        // Given
        String tokenInvalido = "token.malformado";
        when(jwtUtil.validateToken(tokenInvalido)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            logoutService.logout(tokenInvalido);
        });

        assertEquals("Token inválido o expirado", exception.getMessage());
        verify(tokenBlacklistService, never()).blacklist(anyString());
    }
}
