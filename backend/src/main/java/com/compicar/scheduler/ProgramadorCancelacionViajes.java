package com.compicar.scheduler;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.compicar.viaje.ViajeService;

@Component
public class ProgramadorCancelacionViajes {

    private final ViajeService viajeService;

    public ProgramadorCancelacionViajes(ViajeService viajeService) {
        this.viajeService = viajeService;
    }

    /**
     * 1. Se ejecuta inmediatamente cuando Koyeb DESPIERTA de la hibernación
     *    o cuando arrancas el servidor localmente.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void cancelarAlDespertar() {
        System.out.println("[KOYEB] Servidor despierto/iniciado. Ejecutando limpieza inicial de viajes expirados...");
        cancelarViajesExpirados();
    }

    /**
     * 2. Se ejecuta continuamente cada 10 minutos (600.000 ms) mientras la app esté activa.
     */
    @Scheduled(fixedRate = 600000)
    public void cancelarViajesExpirados() {
        viajeService.cancelarViajesPendientesExpirados();
    }
}
