package com.compicar.persona;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.compicar.persona.dto.PerfilPersonaDTO;

@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    private final PersonaService personaService;

    @Autowired
    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }


    @GetMapping("/{personaId}/perfil")
    public ResponseEntity<PerfilPersonaDTO> obtenerPerfil(@PathVariable Long personaId) {
        PerfilPersonaDTO perfil = personaService.obtenerPerfil(personaId);
        return ResponseEntity.ok(perfil);
    }


    @PutMapping("/{personaId}/perfil")
    public ResponseEntity<PerfilPersonaDTO> actualizarPerfil(
            @PathVariable Long personaId,
            @RequestBody PerfilPersonaDTO perfilActualizado) {
        PerfilPersonaDTO perfil = personaService.actualizarPerfil(personaId, perfilActualizado);
        return ResponseEntity.ok(perfil);
    }
    
}
