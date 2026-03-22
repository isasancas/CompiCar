package com.compicar.persona;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    private final PersonaService personaService;

    @Autowired
    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }

    @RequestMapping("/obtenerPorNombrePersona?username={username}")
    public Persona obtenerPersonaPorNomPersona(String username) {
        return personaService.obtenerPersonaPorNombrePersona(username);
    }

    @RequestMapping("/obtenerPorEmail?email={email}")
    public Persona obtenerPersonaPorEmail(String email) {
        return personaService.obtenerPersonaPorEmail(email);
    }    
    
}
