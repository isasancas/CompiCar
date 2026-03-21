package com.compicar.autenticacion.registro;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import com.compicar.persona.Persona;
import com.compicar.persona.PersonaService;

@Service
public class RegistroService {

    private final PersonaService personaService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public RegistroService(PersonaService personaService, PasswordEncoder passwordEncoder) {
        this.personaService = personaService;
        this.passwordEncoder = passwordEncoder;
    }

    public Persona registrarPersona(Registro registro) {
        return personaService.crearPersonaDesdeRegistro(registro, passwordEncoder);
    }
}