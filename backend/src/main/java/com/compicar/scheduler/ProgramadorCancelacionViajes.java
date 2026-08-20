package com.compicar.scheduler;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.compicar.viaje.ViajeService;
import com.compicar.viajeRecurrente.ViajeRecurrenteService;

@Component
public class ProgramadorCancelacionViajes {

    private final ViajeService viajeService;
    private final ViajeRecurrenteService viajeRecurrenteService;

    public ProgramadorCancelacionViajes(ViajeService viajeService, 
                                        ViajeRecurrenteService viajeRecurrenteService) {
        this.viajeService = viajeService;
        this.viajeRecurrenteService = viajeRecurrenteService;
    }

    /**
     * 1. Se ejecuta inmediatamente cuando Koyeb DESPIERTA de la hibernación
     *    o cuando arrancas el servidor localmente.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void cancelarAlDespertar() {
        System.out.println("[KOYEB] Servidor despierto/iniciado. Ejecutando limpieza inicial de viajes expirados (normales y recurrentes)...");
        cancelarViajesExpirados();
    }

    /**
     * 2. Se ejecuta continuamente cada 10 minutos (600.000 ms) mientras la app esté activa.
     */
    @Scheduled(fixedRate = 600000)
    public void cancelarViajesExpirados() {
        try {
            viajeService.cancelarViajesPendientesExpirados();
            viajeRecurrenteService.cancelarViajesRecurrentesPendientesExpirados();
        } catch (Exception e) {
            System.err.println("Error durante la limpieza programada de viajes expirados: " + e.getMessage());
        }
    }
}
