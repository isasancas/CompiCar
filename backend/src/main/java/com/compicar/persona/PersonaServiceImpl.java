package com.compicar.persona;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

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

        String tel = registro.getNumTelefono();
        if (tel != null && !tel.isEmpty()) {
            if (!tel.matches("^\\+?[0-9]{7,15}$")) {
                throw new IllegalArgumentException("El formato del teléfono es inválido");
            }
            
            if (personaRepository.existsByTelefono(tel)) {
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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        Persona personaAutenticada = personaRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!personaAutenticada.getId().equals(personaId)) {
            throw new AccessDeniedException("No puedes modificar el perfil de otro usuario");
        }
        
        personaAutenticada.setNombre(perfilActualizado.getNombre());
        personaAutenticada.setPrimerApellido(perfilActualizado.getPrimerApellido());
        personaAutenticada.setSegundoApellido(perfilActualizado.getSegundoApellido());
        personaAutenticada.setTelefono(perfilActualizado.getTelefono());

        if (!personaAutenticada.getEmail().equals(perfilActualizado.getEmail())) {
            if (personaRepository.existsByEmail(perfilActualizado.getEmail())) {
                throw new IllegalArgumentException("El email ya está registrado");
            }
            if (perfilActualizado.getContrasenaActual() == null || perfilActualizado.getContrasenaActual().isBlank()) {
                throw new IllegalArgumentException("Debes introducir tu contraseña actual para cambiar el email");
            }
            if (!passwordEncoder.matches(perfilActualizado.getContrasenaActual(), personaAutenticada.getContrasena())) {
                throw new IllegalArgumentException("La contraseña actual es incorrecta");
            } else {
                personaAutenticada.setEmail(perfilActualizado.getEmail());
            }
        }
        
        Persona personaActualizada = personaRepository.save(personaAutenticada);
        return new ActualizarPerfilDTO(personaActualizada);
    }

    @Override
    public Persona obtenerPersonaPorNombrePersona(String username) {
       Persona persona = personaRepository.findByNombre(username);
         if (persona == null) {
              throw new RuntimeException("Usuario no encontrado");
         }
        return persona;
    }

    @Override
    public Persona obtenerPersonaPorEmail(String email) {
        Persona persona = personaRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return persona;
    }
}
