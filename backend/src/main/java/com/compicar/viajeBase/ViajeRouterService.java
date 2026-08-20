package com.compicar.viajeBase;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;
import com.compicar.viaje.ViajeService;
import com.compicar.viajeRecurrente.ViajeRecurrenteRepository;
import com.compicar.viajeRecurrente.ViajeRecurrenteService;

@Service
public class ViajeRouterService {

    private final ViajeService viajeService;
    private final ViajeRecurrenteService viajeRecurrenteService;
    private final ViajeRepository viajeRepository;
    private final ViajeRecurrenteRepository viajeRecurrenteRepository;

    public ViajeRouterService(ViajeService viajeService, 
                              ViajeRecurrenteService viajeRecurrenteService,
                              ViajeRepository viajeRepository,
                              ViajeRecurrenteRepository viajeRecurrenteRepository) {
        this.viajeService = viajeService;
        this.viajeRecurrenteService = viajeRecurrenteService;
        this.viajeRepository = viajeRepository;
        this.viajeRecurrenteRepository = viajeRecurrenteRepository;
    }

    public Object obtenerPorSlug(String slug) {
        if (viajeRepository.existsBySlug(slug)) {
            return viajeService.obtenerViajePorSlug(slug);
        } else if (viajeRecurrenteRepository.existsBySlug(slug)) {
            return viajeRecurrenteService.obtenerViajeRecurrentePorSlug(slug);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado");
    }

    public Object iniciarViaje(String usuarioEmail, String slug) {
        if (viajeRepository.existsBySlug(slug)) {
            return viajeService.iniciarViaje(usuarioEmail, slug);
        } else if (viajeRecurrenteRepository.existsBySlug(slug)) {
            return viajeRecurrenteService.iniciarViajeRecurrente(usuarioEmail, slug);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado");
    }

    public Object confirmarCheckin(String usuarioEmail, String slug, String checkin) {
        if (viajeRepository.existsBySlug(slug)) {
            return viajeService.confirmarCheckin(usuarioEmail, slug, checkin);
        } else if (viajeRecurrenteRepository.existsBySlug(slug)) {
            return viajeRecurrenteService.confirmarCheckinRecurrente(usuarioEmail, slug, checkin);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado");
    }

    public Object finalizarViaje(String usuarioEmail, String slug) {
        if (viajeRepository.existsBySlug(slug)) {
            return viajeService.finalizarViaje(usuarioEmail, slug);
        } else if (viajeRecurrenteRepository.existsBySlug(slug)) {
            return viajeRecurrenteService.finalizarViajeRecurrente(usuarioEmail, slug);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado");
    }

    public Object cancelarViaje(String usuarioEmail, String slug) {
        if (viajeRepository.existsBySlug(slug)) {
            return viajeService.cancelarViaje(usuarioEmail, slug);
        } else if (viajeRecurrenteRepository.existsBySlug(slug)) {
            return viajeRecurrenteService.cancelarViajeRecurrente(usuarioEmail, slug);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado");
    }

    public Object cancelarViajeIncompareceConductor(String usuarioEmail, String slug) {
        if (viajeRepository.existsBySlug(slug)) {
            return viajeService.cancelarViajeIncompareceConductor(usuarioEmail, slug);
        } else if (viajeRecurrenteRepository.existsBySlug(slug)) {
            return viajeRecurrenteService.cancelarViajeRecurrenteIncompareceConductor(usuarioEmail, slug);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado");
    }


    public Object actualizarViaje(String usuarioEmail, String slug, Viaje viajeEditado) {
        if (viajeRepository.existsBySlug(slug)) {
            return viajeService.actualizarViaje(usuarioEmail, slug, viajeEditado);
        } else if (viajeRecurrenteRepository.existsBySlug(slug)) {
            return viajeRecurrenteService.actualizarViajeRecurrente(usuarioEmail, slug, viajeEditado);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado");
    }
}