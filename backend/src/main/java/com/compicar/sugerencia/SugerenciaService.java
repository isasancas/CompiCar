package com.compicar.sugerencia;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.compicar.correo.CorreoService;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaService;

@Service
public class SugerenciaService {

    private static final int MAX_SUGERENCIAS_DIARIAS = 3;

    private final PersonaService personaService;
    private final CorreoService correoService;
    private final ConcurrentHashMap<String, ContadorDiario> contadores = new ConcurrentHashMap<>();

    public SugerenciaService(PersonaService personaService, CorreoService correoService) {
        this.personaService = personaService;
        this.correoService = correoService;
    }

    public void enviar(String emailUsuario, String mensaje) {
        Persona persona = personaService.obtenerPersonaPorEmail(emailUsuario);
        if (persona == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado");
        }

        String clave = emailUsuario + ":" + LocalDate.now();
        ContadorDiario contador = contadores.computeIfAbsent(clave, ignored -> new ContadorDiario());
        if (contador.cantidad.incrementAndGet() > MAX_SUGERENCIAS_DIARIAS) {
            contador.cantidad.decrementAndGet();
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Has alcanzado el límite de 3 mensajes diarios");
        }

        correoService.sendSugerencia(emailUsuario, persona.getNombre(), mensaje);
    }

    private static class ContadorDiario {
        private final AtomicInteger cantidad = new AtomicInteger();
    }
}