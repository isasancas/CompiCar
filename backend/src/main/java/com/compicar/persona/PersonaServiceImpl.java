package com.compicar.persona;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.compicar.persona.dto.ActualizarPerfilDTO;
import com.compicar.persona.dto.PerfilPersonaDTO;
import com.compicar.autenticacion.registro.Registro;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class PersonaServiceImpl implements PersonaService {

    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public PersonaServiceImpl(PersonaRepository personaRepository, PasswordEncoder passwordEncoder) {
        this.personaRepository = personaRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public Persona crearPersonaDesdeRegistro(Registro registro, PasswordEncoder passwordEncoder) {
        if (personaRepository.existsByEmail(registro.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        if (registro.getNumTelefono() != null && !registro.getNumTelefono().isEmpty()) {
            if (personaRepository.existsByTelefono(registro.getNumTelefono())) {
                throw new IllegalArgumentException("El teléfono ya está registrado");
            }
        }

        Persona persona = new Persona();
        persona.setNombre(registro.getNombre());
        persona.setPrimerApellido(registro.getPrimerApellido());
        persona.setSegundoApellido(registro.getSegundoApellido());
        persona.setEmail(registro.getEmail());
        persona.setTelefono(registro.getNumTelefono());

        String contrasenaEncriptada = passwordEncoder.encode(registro.getContrasena());
        persona.setContrasena(contrasenaEncriptada);

        return personaRepository.save(persona);
    }

    @Override
    public PerfilPersonaDTO obtenerPerfil(Long personaId) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada"));
        return new PerfilPersonaDTO(persona);
    }

    @Override
    public ActualizarPerfilDTO actualizarPerfil(Long personaId, ActualizarPerfilDTO perfilActualizado) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada"));
        
        persona.setNombre(perfilActualizado.getNombre());
        persona.setPrimerApellido(perfilActualizado.getPrimerApellido());
        persona.setSegundoApellido(perfilActualizado.getSegundoApellido());
        persona.setTelefono(perfilActualizado.getTelefono());

        if (!persona.getEmail().equals(perfilActualizado.getEmail())) {
            if (personaRepository.existsByEmail(perfilActualizado.getEmail())) {
                throw new IllegalArgumentException("El email ya está registrado");
            }
            if (!passwordEncoder.matches(perfilActualizado.getContrasenaActual(), persona.getContrasena())) {
                throw new IllegalArgumentException("La contraseña actual es incorrecta");
            } else {
                persona.setEmail(perfilActualizado.getEmail());
            }
        }
        
        Persona personaActualizada = personaRepository.save(persona);
        return new ActualizarPerfilDTO(personaActualizada);
    }

}
