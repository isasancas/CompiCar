package com.compicar.persona;

import com.compicar.persona.dto.ActualizarPerfilDTO;
import com.compicar.persona.dto.PerfilPersonaDTO;
import com.compicar.autenticacion.registro.Registro;

import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;

public interface PersonaService {

    PerfilPersonaDTO obtenerPerfil(Long personaId);
    ActualizarPerfilDTO actualizarPerfil(Long personaId, ActualizarPerfilDTO perfilActualizado);
    Persona crearPersonaDesdeRegistro(Registro registro, PasswordEncoder passwordEncoder);
    Persona obtenerPersonaPorEmail(String email);
    Persona obtenerPersonaPorNombrePersona(String username);
    PerfilPersonaDTO obtenerPerfilPorSlug(String slug);
    void subirFoto(String email, String fotoBase64);
    Map<String, Object> retirarFondos(String email);
    List<PerfilPersonaDTO> obtenerTopConductores();
}
