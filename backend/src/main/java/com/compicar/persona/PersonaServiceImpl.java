package com.compicar.persona;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.compicar.autenticacion.registro.Registro;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class PersonaServiceImpl implements PersonaService {

    private final PersonaRepository personaRepository;

    @Autowired
    public PersonaServiceImpl(PersonaRepository personaRepository) {
        this.personaRepository = personaRepository;
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

}
