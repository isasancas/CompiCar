package com.compicar.vehiculo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.compicar.vehiculo.dto.AltaVehiculoRequestDTO;
import com.compicar.vehiculo.dto.VehiculoResponseDTO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculoService;

    @Autowired
    public VehiculoController(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehiculoResponseDTO crearVehiculo(@Valid @RequestBody AltaVehiculoRequestDTO request) {
        String email = obtenerEmailAutenticado();
        return vehiculoService.crearVehiculo(email, request);
    }

    @GetMapping("/propios")
    public List<VehiculoResponseDTO> obtenerMisVehiculos() {
        String email = obtenerEmailAutenticado();
        return vehiculoService.obtenerMisVehiculos(email);
    }

    @PutMapping("/{vehiculoId}")
    public VehiculoResponseDTO editarVehiculo(
        @PathVariable Long vehiculoId,
        @Valid @RequestBody AltaVehiculoRequestDTO request
    ) {
        String email = obtenerEmailAutenticado();
        return vehiculoService.actualizarVehiculo(email, vehiculoId, request);
    }

    @DeleteMapping("/{vehiculoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrarVehiculo(@PathVariable Long vehiculoId) {
        String email = obtenerEmailAutenticado();
        vehiculoService.borrarVehiculo(email, vehiculoId);
    }

    private String obtenerEmailAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return auth.getName();
    }
    
}
