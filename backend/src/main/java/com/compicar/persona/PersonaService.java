package com.compicar.persona;

import com.compicar.autenticacion.registro.Registro;
import org.springframework.security.crypto.password.PasswordEncoder;

public interface PersonaService {

    Persona crearPersonaDesdeRegistro(Registro registro, PasswordEncoder passwordEncoder);

    Persona obtenerPersonaPorEmail(String email);

    Persona obtenerPersonaPorNombrePersona(String username);

}
