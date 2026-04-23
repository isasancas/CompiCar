package com.compicar.persona;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import com.compicar.persona.dto.ActualizarPerfilDTO;
import com.compicar.persona.dto.PerfilPersonaDTO;
import com.compicar.autenticacion.registro.Registro;
import com.compicar.config.SlugUtils;

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

        String baseSlug = SlugUtils.toSlug(
            registro.getNombre() + "-" + registro.getPrimerApellido()
        );
        persona.setSlug(generarSlugUnico(baseSlug));

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
        personaAutenticada.getPreferenciasViaje().clear();
        if (perfilActualizado.getPreferenciasViaje() != null) {
            personaAutenticada.getPreferenciasViaje().addAll(perfilActualizado.getPreferenciasViaje());
        }

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

    @Override
    public PerfilPersonaDTO obtenerPerfilPorSlug(String slug) {
        Persona persona = personaRepository.findBySlug(slug)
            .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada"));
        return new PerfilPersonaDTO(persona);
    }

    @Override
    public void subirFoto(String email, String fotoBase64) {
        Persona persona = personaRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Validar tamaño máximo (5MB en Base64 = ~3.75MB original)
        if (fotoBase64.length() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Foto demasiado grande");
        }
        
        persona.setFoto(fotoBase64);
        personaRepository.save(persona);
    }

    private String generarSlugUnico(String baseSlug) {
        String candidato = baseSlug;
        int sufijo = 2;
        while (personaRepository.existsBySlug(candidato)) {
            candidato = baseSlug + "-" + sufijo;
            sufijo++;
        }
        return candidato;
    }
}
