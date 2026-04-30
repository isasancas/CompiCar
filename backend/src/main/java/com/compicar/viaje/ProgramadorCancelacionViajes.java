package com.compicar.viaje;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProgramadorCancelacionViajes {

    private final ViajeService viajeService;

    public ProgramadorCancelacionViajes (ViajeService viajeService) {
        this.viajeService = viajeService;
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void cancelarViajesExpirados() {
        viajeService.cancelarViajesPendientesExpirados();
    }
}
