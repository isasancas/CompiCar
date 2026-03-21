package com.compicar.persona;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.compicar.persona.dto.PerfilPersonaDTO;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class PersonaServiceImpl implements PersonaService {

    private final PersonaRepository personaRepository;

    @Autowired
    public PersonaServiceImpl(PersonaRepository personaRepository) {
        this.personaRepository = personaRepository;
    }

    public PerfilPersonaDTO obtenerPerfil(Long personaId) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada"));
        return new PerfilPersonaDTO(persona);
    }

    public PerfilPersonaDTO actualizarPerfil(Long personaId, PerfilPersonaDTO perfilActualizado) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada"));
        
        persona.setNombre(perfilActualizado.getNombre());
        persona.setPrimerApellido(perfilActualizado.getPrimerApellido());
        persona.setSegundoApellido(perfilActualizado.getSegundoApellido());
        persona.setTelefono(perfilActualizado.getTelefono());
        
        Persona personaActualizada = personaRepository.save(persona);
        return new PerfilPersonaDTO(personaActualizada);
    }
    
}
