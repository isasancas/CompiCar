package com.compicar.inicioSesion;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.compicar.autenticacion.inicioSesion.TokenBlacklistService;

class TokenBlacklistServiceTests {

    private TokenBlacklistService tokenBlacklistService;
    private final String testToken = "eyJhbGciOiJIUzI1NiJ9.token.test";

    @BeforeEach
    void setUp() {
        // Instanciamos directamente ya que no tiene dependencias
        tokenBlacklistService = new TokenBlacklistService();
    }

    @Test
    void testBlacklist_AddsTokenSuccessfully() {
        // When
        tokenBlacklistService.blacklist(testToken);

        // Then
        assertTrue(tokenBlacklistService.isBlacklisted(testToken), 
            "El token debería estar en la lista negra tras añadirlo");
    }

    @Test
    void testIsBlacklisted_ReturnsFalseForNewToken() {
        // Then
        assertFalse(tokenBlacklistService.isBlacklisted("token.no.existente"), 
            "Un token que no ha sido añadido no debería estar bloqueado");
    }

    @Test
    void testIsBlacklisted_ReturnsTrueForNull() {
        // Then
        assertTrue(tokenBlacklistService.isBlacklisted(null), 
            "Por seguridad, un token nulo debe considerarse como bloqueado/inválido");
    }

    @Test
    void testBlacklist_MultipleTokens() {
        // Given
        String token2 = "otro.token.distinto";

        // When
        tokenBlacklistService.blacklist(testToken);
        tokenBlacklistService.blacklist(token2);

        // Then
        assertTrue(tokenBlacklistService.isBlacklisted(testToken));
        assertTrue(tokenBlacklistService.isBlacklisted(token2));
    }
}
