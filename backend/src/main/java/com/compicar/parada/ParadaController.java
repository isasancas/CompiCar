package com.compicar.parada;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;

@RestController
@RequestMapping("/api/paradas")

public class ParadaController {

    private final ParadaService paradaService;
    private final ViajeRepository viajeRepository;

    public ParadaController(ParadaService paradaService, ViajeRepository viajeRepository) {
        this.paradaService = paradaService;
        this.viajeRepository = viajeRepository;
    }

    @PostMapping("/api/viajes/{viajeId}/paradas")
    public List<Parada> anadirParadas(@PathVariable Long viajeId, @RequestBody List<Parada> paradas) {
        return paradaService.anadirParadas(viajeId, paradas).getParadas();
    }

    @PostMapping("/crear")
    public Parada crearParada(@PathVariable Long viajeId, @RequestBody Parada parada) {
        return paradaService.crearParada(viajeId, parada);
    }

    @GetMapping("/viaje/{viajeId}")
    public List<Parada> obtenerParadasPorViaje(@PathVariable Long viajeId) {
        Viaje viaje = viajeRepository.findById(viajeId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));
        return paradaService.obtenerParadasPorViaje(viaje);
    }
}
