package com.compicar.viaje;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.compicar.viaje.dto.CalcularPrecioTrayectoRequestDTO;
import com.compicar.viaje.dto.PrecioTrayectoResponseDTO;
import com.compicar.viaje.dto.ViajeDTO;
import com.compicar.viajeBase.ViajeRouterService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/viajes")
public class ViajeController {

    private final ViajeService viajeService;
    private final ViajeRouterService viajeRouterService;

    @Autowired
    public ViajeController(ViajeService viajeService, ViajeRouterService viajeRouterService) {
        this.viajeService = viajeService;
        this.viajeRouterService = viajeRouterService;
    }

    @PostMapping("/crear")
    public Viaje crearViaje(@RequestBody Viaje viaje) {
        String usuarioEmail = getUsuarioAutenticado();
        return viajeService.crearViaje(usuarioEmail, viaje);
    }

    @PostMapping("/precio/calcular")
    public PrecioTrayectoResponseDTO calcularPrecioTrayecto(@Valid @RequestBody CalcularPrecioTrayectoRequestDTO request) {
        getUsuarioAutenticado();
        return viajeService.calcularPrecioTrayecto(getUsuarioAutenticado(), request);
    }

    @GetMapping("/mis-viajes")
    public List<ViajeDTO> obtenerMisViajes() {
        String usuarioEmail = getUsuarioAutenticado();
        return viajeService.obtenerMisViajes(usuarioEmail);
    }

    @GetMapping("/participados")
    public List<ViajeDTO> obtenerViajesParticipados() {
        String usuarioEmail = getUsuarioAutenticado();
        return viajeService.obtenerViajesParticipados(usuarioEmail);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<Object> obtenerViajePorSlug(@PathVariable String slug) {
        return ResponseEntity.ok(viajeRouterService.obtenerPorSlug(slug));
    }

    @GetMapping("/publicos")
    public List<ViajeDTO> buscarViajesPublicos(
        @RequestParam(required = false) String origen,
        @RequestParam(required = false) String destino,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return viajeService.buscarViajesPublicos(origen, destino, fecha);
    }

    @GetMapping("/publicos/conductor/{conductorSlug}")
    public List<ViajeDTO> obtenerViajesPublicosPorConductor(@PathVariable String conductorSlug) {
        return viajeService.obtenerViajesPublicosPorConductor(conductorSlug);
    }

    @GetMapping("/publicos/{slug}")
    public ResponseEntity<Object> obtenerViajePublicoPorSlug(@PathVariable String slug) {
        return ResponseEntity.ok(viajeRouterService.obtenerPorSlug(slug));
    }

    @PutMapping("/{slug}/cancelar")
    public ResponseEntity<Object> cancelarViaje(@PathVariable String slug) {
        String usuarioEmail = getUsuarioAutenticado();
        return ResponseEntity.ok(viajeRouterService.cancelarViaje(usuarioEmail, slug));
    }

    @PutMapping("/{slug}/cancelar-conjunto")
    public ResponseEntity<Object> cancelarViajeConjunto(@PathVariable String slug) {
        String usuarioEmail = getUsuarioAutenticado();
        return ResponseEntity.ok(viajeService.cancelarViajeConjunto(usuarioEmail, slug));
    }

    @PutMapping("/{slug}")
    public ResponseEntity<Object> actualizarViaje(
            @PathVariable String slug, 
            @RequestBody Viaje viajeEditado) {
        String usuarioEmail = getUsuarioAutenticado();
        return ResponseEntity.ok(viajeRouterService.actualizarViaje(usuarioEmail, slug, viajeEditado));
    }

    @PutMapping("/{slug}/finalizar")
    public ResponseEntity<Object> finalizarViaje(@PathVariable String slug) {
        String usuarioEmail = getUsuarioAutenticado();
        return ResponseEntity.ok(viajeRouterService.finalizarViaje(usuarioEmail, slug));
    }

    @PutMapping("/{slug}/iniciar")
    public ResponseEntity<Object> iniciarViaje(@PathVariable String slug) {
        String usuarioEmail = getUsuarioAutenticado();
        return ResponseEntity.ok(viajeRouterService.iniciarViaje(usuarioEmail, slug));
    }

    @PutMapping("/{slug}/checkin")
    public ResponseEntity<Object> confirmarCheckin(@PathVariable String slug, @RequestParam("checkin") String checkin) {
        String usuarioEmail = getUsuarioAutenticado();
        return ResponseEntity.ok(viajeRouterService.confirmarCheckin(usuarioEmail, slug, checkin));
    }


    @GetMapping("/proximo")
    public ResponseEntity<ViajeDTO> obtenerProximoViaje(Authentication authentication) {
        // Extraemos el email del usuario autenticado mediante el JWT / Spring Security
        String email = authentication.getName();
        
        ViajeDTO proximoViaje = viajeService.obtenerProximoViajeUsuario(email);

        // Si el usuario no tiene ningún viaje futuro (ni como conductor ni como pasajero)
        if (proximoViaje == null) {
            return ResponseEntity.noContent().build(); // HTTP 204 No Content
        }

        return ResponseEntity.ok(proximoViaje); // HTTP 200 OK con el ViajeDTO
    }
    
    @PutMapping("/{slug}/en-curso")
    public ResponseEntity<ViajeDTO> ponerEnCurso(Principal principal, @PathVariable String slug) {
        ViajeDTO viaje = viajeService.ponerEnCursoAutomatico(principal.getName(), slug);
        return ResponseEntity.ok(viaje);
    }

    // --- Auxiliar de Autenticación ---

    private String getUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return auth.getName();
    }

    @PutMapping("/{slug}/cancelarIncompareceConductor")
    public ResponseEntity<Object> cancelarViajeIncompareceConductor(@PathVariable String slug) {
        String usuarioEmail = getUsuarioAutenticado();
        return ResponseEntity.ok(viajeRouterService.cancelarViajeIncompareceConductor(usuarioEmail, slug));
    }

}
