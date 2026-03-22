package com.compicar.autenticacion.inicioSesion;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.compicar.persona.Persona;
import com.compicar.persona.PersonaService;

@Service
public class InicioSesionService {

    private final PersonaService personaService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public InicioSesionService(PersonaService personaService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.personaService = personaService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public String iniciarSesion(InicioSesion request) {
        Persona persona = personaService.obtenerPersonaPorEmail(request.getEmail());

        if (persona == null) {
            throw new RuntimeException("Usuario no encontrado");
        }

        if (!passwordEncoder.matches(request.getContrasena(), persona.getContrasena())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        // Generar token JWT usando el email como subject (identificador único)
        return jwtUtil.generateToken(persona.getEmail());
    }
}

