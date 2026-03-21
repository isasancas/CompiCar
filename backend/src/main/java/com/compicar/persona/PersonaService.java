package com.compicar.persona;

import com.compicar.persona.dto.PerfilPersonaDTO;

public interface PersonaService {

    PerfilPersonaDTO obtenerPerfil(Long personaId);
    PerfilPersonaDTO actualizarPerfil(Long personaId, PerfilPersonaDTO perfilActualizado);
    
}
