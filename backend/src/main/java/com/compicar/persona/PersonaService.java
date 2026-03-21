package com.compicar.persona;

import com.compicar.persona.dto.PerfilPersonaDTO;
import com.compicar.autenticacion.registro.Registro;
import org.springframework.security.crypto.password.PasswordEncoder;

public interface PersonaService {

    PerfilPersonaDTO obtenerPerfil(Long personaId);
    PerfilPersonaDTO actualizarPerfil(Long personaId, PerfilPersonaDTO perfilActualizado);
    Persona crearPersonaDesdeRegistro(Registro registro, PasswordEncoder passwordEncoder);

}
