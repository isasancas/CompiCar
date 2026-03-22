package com.compicar.autenticacion.inicioSesion;

import org.springframework.stereotype.Service;

@Service
public class LogoutService {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    public LogoutService(JwtUtil jwtUtil, TokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public void logout(String token) {
        if (token == null || !jwtUtil.validateToken(token)) {
            throw new IllegalArgumentException("Token inválido o expirado");
        }
        tokenBlacklistService.blacklist(token);
    }
}
