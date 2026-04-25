package com.compicar.viaje;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.compicar.viaje.dto.CalcularPrecioTrayectoRequestDTO;
import com.compicar.viaje.dto.PrecioTrayectoResponseDTO;
import com.compicar.viaje.dto.ViajeDTO;

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
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        String usuarioEmail = auth.getName();
        return viajeService.crearViaje(usuarioEmail, viaje);
    }

    @PostMapping("/precio/calcular")
    public PrecioTrayectoResponseDTO calcularPrecioTrayecto(@Valid @RequestBody CalcularPrecioTrayectoRequestDTO request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        String usuarioEmail = auth.getName();
        return viajeService.calcularPrecioTrayecto(usuarioEmail, request);
    }

    @GetMapping("/mis-viajes")
    public List<ViajeDTO> obtenerMisViajes() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        String usuarioEmail = auth.getName();
        return viajeService.obtenerMisViajes(usuarioEmail);
    }

    @GetMapping("/participados")
    public List<ViajeDTO> obtenerViajesParticipados() {
       Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        String usuarioEmail = auth.getName();
        return viajeService.obtenerViajesParticipados(usuarioEmail);
    }

    @GetMapping("/{slug}")
    public ViajeDTO obtenerViajePorSlug(@PathVariable String slug) {
        return viajeService.obtenerViajePorSlug(slug);
    }

    @GetMapping("/publicos")
    public List<ViajeDTO> buscarViajesPublicos(
        @RequestParam(required = false) String origen,
        @RequestParam(required = false) String destino,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return viajeService.buscarViajesPublicos(origen, destino, fecha);
    }

    @GetMapping("/publicos/{slug}")
    public ViajeDTO obtenerViajePublicoPorSlug(@PathVariable String slug) {
        return viajeService.obtenerViajePorSlug(slug);
    }

}
