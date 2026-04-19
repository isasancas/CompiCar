package com.compicar.persona;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.compicar.persona.dto.ActualizarPerfilDTO;
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

    @GetMapping("/perfil")
    public ResponseEntity<PerfilPersonaDTO> obtenerMiPerfil() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Persona persona = personaService.obtenerPersonaPorEmail(email);
        PerfilPersonaDTO perfil = personaService.obtenerPerfil(persona.getId());
        return ResponseEntity.ok(perfil);
    }


    @PutMapping("/{personaId}/perfil")
    public ResponseEntity<ActualizarPerfilDTO> actualizarPerfil(
            @PathVariable Long personaId,
            @RequestBody ActualizarPerfilDTO perfilActualizado) {
        ActualizarPerfilDTO perfil = personaService.actualizarPerfil(personaId, perfilActualizado);
        return ResponseEntity.ok(perfil);
    }

    @RequestMapping("/obtenerPorNombrePersona")
    public Persona obtenerPersonaPorNomPersona(@RequestParam String username) {
        return personaService.obtenerPersonaPorNombrePersona(username);
    }

    @RequestMapping("/obtenerPorEmail")
    public Persona obtenerPersonaPorEmail(@RequestParam String email) {
        return personaService.obtenerPersonaPorEmail(email);
    }    

    @GetMapping("/{slug}/perfil-publico")
    public ResponseEntity<PerfilPersonaDTO> obtenerPerfilPublicoPorSlug(@PathVariable String slug) {
        PerfilPersonaDTO perfil = personaService.obtenerPerfilPorSlug(slug);
        return ResponseEntity.ok(perfil);
    }
    
}
