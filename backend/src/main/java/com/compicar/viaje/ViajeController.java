package com.compicar.viaje;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.compicar.viaje.dto.CalcularPrecioTrayectoRequestDTO;
import com.compicar.viaje.dto.PrecioTrayectoResponseDTO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/viajes")
public class ViajeController {

    private final ViajeService viajeService;

    @Autowired
    public ViajeController(ViajeService viajeService) {
        this.viajeService = viajeService;
    }
    
    @PostMapping("/crear")
    public Viaje crearViaje(@RequestBody Viaje viaje) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        String usuarioEmail = auth.getName();
        return viajeService.crearViaje(usuarioEmail, viaje);
    }

    @PostMapping("/precio/calcular")
    public PrecioTrayectoResponseDTO calcularPrecioTrayecto(@Valid @RequestBody CalcularPrecioTrayectoRequestDTO request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "No autenticado"
            );
        }

        String usuarioEmail = auth.getName();
        return viajeService.calcularPrecioTrayecto(usuarioEmail, request);
    }
}
