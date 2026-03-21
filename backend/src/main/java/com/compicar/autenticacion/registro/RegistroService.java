package com.compicar.autenticacion.registro;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;

@Service
public class RegistroService {

    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public RegistroService(PersonaRepository personaRepository, PasswordEncoder passwordEncoder) {
        this.personaRepository = personaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    
    public Persona registrarPersona(Registro registro) {
        // Validaciones
        validarDatosRegistro(registro);
        
        // Verificar que el email no esté registrado
        if (personaRepository.existsByEmail(registro.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        
        // Verificar que el teléfono no esté registrado (si se proporciona)
        if (registro.getNumTelefono() != null && !registro.getNumTelefono().isEmpty()) {
            if (personaRepository.existsByTelefono(registro.getNumTelefono())) {
                throw new IllegalArgumentException("El teléfono ya está registrado");
            }
        }
        
        // Crear la nueva persona con los datos del registro
        Persona nuevaPersona = crearPersona(registro);
        
        // Guardar en la base de datos
        return personaRepository.save(nuevaPersona);
    }

    /**
     * Valida que los datos del registro sean válidos
     */
    private void validarDatosRegistro(Registro registro) {
        if (registro.getNombre() == null || registro.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        
        if (registro.getApellido() == null || registro.getApellido().trim().isEmpty()) {
            throw new IllegalArgumentException("El apellido no puede estar vacío");
        }
        
        if (registro.getEmail() == null || registro.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }
        
        if (!esEmailValido(registro.getEmail())) {
            throw new IllegalArgumentException("El email no es válido");
        }
        
        if (registro.getContrasena() == null || registro.getContrasena().trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }
    }

    /**
     * Valida que el email tenga un formato válido
     */
    private boolean esEmailValido(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    /**
     * Crea una nueva instancia de Persona con los datos del registro
     */
    private Persona crearPersona(Registro registro) {
        Persona nuevaPersona = new Persona();
        
        // Establecer datos básicos
        nuevaPersona.setNombre(registro.getNombre());
        nuevaPersona.setPrimerApellido(registro.getApellido());
        nuevaPersona.setEmail(registro.getEmail());
        nuevaPersona.setTelefono(registro.getNumTelefono());
        
        // Encriptar y establecer la contraseña
        String contrasenaEncriptada = passwordEncoder.encode(registro.getContrasena());
        nuevaPersona.setContrasena(contrasenaEncriptada);
        
        return nuevaPersona;
    }
}