package com.compicar.persona;

import com.compicar.persona.dto.ActualizarPerfilDTO;
import com.compicar.persona.dto.PerfilPersonaDTO;
import com.compicar.autenticacion.registro.Registro;
import org.springframework.security.crypto.password.PasswordEncoder;

public interface PersonaService {

    PerfilPersonaDTO obtenerPerfil(Long personaId);
    ActualizarPerfilDTO actualizarPerfil(Long personaId, ActualizarPerfilDTO perfilActualizado);
    Persona crearPersonaDesdeRegistro(Registro registro, PasswordEncoder passwordEncoder);
    Persona obtenerPersonaPorEmail(String email);
    Persona obtenerPersonaPorNombrePersona(String username);

}
