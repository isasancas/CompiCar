package com.compicar.scheduler;

import com.compicar.reserva.EstadoReserva;
import com.compicar.reserva.Reserva;
import com.compicar.reserva.ReservaRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class LimpiezaReservasScheduler {

    private final ReservaRepository reservaRepository;

    public LimpiezaReservasScheduler(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    /**
     * 1. Se ejecuta inmediatamente cuando Koyeb DESPIERTA de la hibernación
     *    o cuando arrancas el servidor localmente.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void limpiarAlDespertar() {
        System.out.println("🚀 [KOYEB] Servidor despierto/iniciado. Ejecutando limpieza inicial de reservas fantasma...");
        cancelarReservasFantasma();
    }

    /**
     * 2. Se ejecuta continuamente cada 30 minutos (300.000 ms) mientras la app esté activa.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cancelarReservasFantasma() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(30);

        List<Reserva> caducadas = reservaRepository
                .findByEstadoAndFechaHoraReservaBefore(EstadoReserva.PENDIENTE, limite);

        if (!caducadas.isEmpty()) {
            System.out.println("🧹 [CRON] Encontradas " + caducadas.size() + " reservas fantasma caducadas.");

            for (Reserva reserva : caducadas) {
                reserva.setEstado(EstadoReserva.CANCELADA);
            }

            reservaRepository.saveAll(caducadas);
            System.out.println("✅ [CRON] Reservas fantasma canceladas correctamente.");
        }
    }
}