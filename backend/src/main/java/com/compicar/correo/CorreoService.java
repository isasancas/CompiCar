package com.compicar.correo;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class CorreoService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${compicar.mail.from:no-reply@compicar.com}")
    private String fromEmail;

    public CorreoService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    // =========================================================================
    // MÉTODO PRIVADO AUXILIAR (Centraliza MimeMessage, Logo y Envío)
    // =========================================================================
    private void enviarCorreoHtml(String toEmail, String asunto, String nombrePlantilla, Context context) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String htmlContent = templateEngine.process(nombrePlantilla, context);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(asunto);
            helper.setText(htmlContent, true);

            // Inserción del logo para todas las plantillas
            helper.addInline("logoCompiCar", new ClassPathResource("static/images/LogoCompletoFondo.png"));

            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Error enviando correo [" + nombrePlantilla + "] a " + toEmail + ": " + e.getMessage());
        }
    }

    // =========================================================================
    // MÉTODOS PÚBLICOS ASÍNCRONOS
    // =========================================================================

    @Async
    public void sendCheckInCode(String toEmail, String nombreUsuario, String origen, String destino, String fechaHora, String codigoCheckIn) {
        Context context = new Context();
        context.setVariable("nombreUsuario", nombreUsuario);
        context.setVariable("origen", origen);
        context.setVariable("destino", destino);
        context.setVariable("fechaHora", fechaHora);
        context.setVariable("codigoCheckIn", codigoCheckIn);

        enviarCorreoHtml(toEmail, "CompiCar - Tu código de Check-in para el viaje a " + destino, "checkin-code", context);
    }

    @Async
    public void sendReservaCanceladaConductor(String toEmail, String nombreConductor, String nombrePasajero, String origen, String destino, String fechaHora, int plazas) {
        Context context = new Context();
        context.setVariable("nombreConductor", nombreConductor);
        context.setVariable("nombrePasajero", nombrePasajero);
        context.setVariable("origen", origen);
        context.setVariable("destino", destino);
        context.setVariable("fechaHora", fechaHora);
        context.setVariable("plazas", plazas);

        enviarCorreoHtml(toEmail, "CompiCar - Un pasajero ha cancelado su reserva", "reserva-cancelada-conductor", context);
    }

    @Async
    public void sendReservaRechazadaPasajero(String toEmail, String nombrePasajero, String nombreConductor, String origen, String destino, String fechaHora) {
        Context context = new Context();
        context.setVariable("nombrePasajero", nombrePasajero);
        context.setVariable("nombreConductor", nombreConductor);
        context.setVariable("origen", origen);
        context.setVariable("destino", destino);
        context.setVariable("fechaHora", fechaHora);

        enviarCorreoHtml(toEmail, "CompiCar - Tu solicitud de reserva no ha sido aceptada", "reserva-rechazada-pasajero", context);
    }

    @Async
    public void sendReservaModificadaConductor(String toEmail, String nombreConductor, String nombrePasajero, String origen, String destino, String fechaHora, int plazas) {
        Context context = new Context();
        context.setVariable("nombreConductor", nombreConductor);
        context.setVariable("nombrePasajero", nombrePasajero);
        context.setVariable("origen", origen);
        context.setVariable("destino", destino);
        context.setVariable("fechaHora", fechaHora);
        context.setVariable("plazas", plazas);

        enviarCorreoHtml(toEmail, "CompiCar - Un pasajero ha modificado su reserva", "reserva-modificada-conductor", context);
    }

    @Async
    public void sendViajeRecurrenteCanceladoPasajero(String toEmail, String nombrePasajero, String nombreConductor, String origen, String destino, String fechaHora, int plazas) {
        Context context = new Context();
        context.setVariable("nombrePasajero", nombrePasajero);
        context.setVariable("nombreConductor", nombreConductor);
        context.setVariable("origen", origen);
        context.setVariable("destino", destino);
        context.setVariable("fechaHora", fechaHora);
        context.setVariable("plazas", plazas);

        enviarCorreoHtml(toEmail, "CompiCar - El conductor ha cancelado un viaje reservado", "viaje-recurrente-cancelado-pasajero", context);
    }

    @Async
    public void sendReservaLoteConductor(String toEmail, String nombreConductor, String nombrePasajero, String origen, String destino, List<String> fechas, int plazas, BigDecimal totalAcumulado) {
        Context context = new Context();
        context.setVariable("nombreConductor", nombreConductor);
        context.setVariable("nombrePasajero", nombrePasajero);
        context.setVariable("origen", origen);
        context.setVariable("destino", destino);
        context.setVariable("fechas", fechas);
        context.setVariable("plazas", plazas);
        context.setVariable("totalAcumulado", totalAcumulado);

        enviarCorreoHtml(toEmail, "CompiCar - Nueva reserva múltiple de un pasajero", "reserva-lote-conductor", context);
    }

    @Async
    public void sendViajeModificadoPasajero(String toEmail, String nombrePasajero, String nombreConductor, String origen, String destino, String fechaHora, BigDecimal precio) {
        Context context = new Context();
        context.setVariable("nombrePasajero", nombrePasajero);
        context.setVariable("nombreConductor", nombreConductor);
        context.setVariable("origen", origen);
        context.setVariable("destino", destino);
        context.setVariable("fechaHora", fechaHora);
        context.setVariable("precio", precio);

        enviarCorreoHtml(toEmail, "CompiCar - Se han actualizado los detalles de tu viaje", "viaje-modificado-pasajero", context);
    }

    @Async
    public void sendViajeFinalizadoPasajero(String toEmail, String nombrePasajero, String nombreConductor, String origen, String destino, String fechaHora) {
        Context context = new Context();
        context.setVariable("nombrePasajero", nombrePasajero);
        context.setVariable("nombreConductor", nombreConductor);
        context.setVariable("origen", origen);
        context.setVariable("destino", destino);
        context.setVariable("fechaHora", fechaHora);

        enviarCorreoHtml(toEmail, "CompiCar - ¡Tu viaje ha finalizado!", "viaje-finalizado-pasajero", context);
    }

    @Async
    public void sendViajeRecurrenteFinalizadoPasajero(String toEmail, String nombrePasajero, String nombreConductor, String origen, String destino, String fechaHora) {
        Context context = new Context();
        context.setVariable("nombrePasajero", nombrePasajero);
        context.setVariable("nombreConductor", nombreConductor);
        context.setVariable("origen", origen);
        context.setVariable("destino", destino);
        context.setVariable("fechaHora", fechaHora);

        enviarCorreoHtml(toEmail, "CompiCar - ¡Tu viaje recurrente ha finalizado!", "viaje-recurrente-finalizado-pasajero", context);
    }

    @Async
    public void sendIncomparecenciaConductorRecurrentePasajero(String toEmail, String nombrePasajero, String nombreConductor, String origen, String destino, String fechaHora) {
        Context context = new Context();
        context.setVariable("nombrePasajero", nombrePasajero);
        context.setVariable("nombreConductor", nombreConductor);
        context.setVariable("origen", origen);
        context.setVariable("destino", destino);
        context.setVariable("fechaHora", fechaHora);

        enviarCorreoHtml(toEmail, "CompiCar - Cancelación por incomparecencia del conductor", "incomparecencia-conductor-recurrente-pasajero", context);
    }

    @Async
    public void sendViajeRecurrenteModificadoPasajero(String toEmail, String nombrePasajero, String nombreConductor, String origen, String destino, String fechaHora, BigDecimal precio) {
        Context context = new Context();
        context.setVariable("nombrePasajero", nombrePasajero);
        context.setVariable("nombreConductor", nombreConductor);
        context.setVariable("origen", origen);
        context.setVariable("destino", destino);
        context.setVariable("fechaHora", fechaHora);
        context.setVariable("precio", precio);

        enviarCorreoHtml(toEmail, "CompiCar - Modificación en tu viaje recurrente", "viaje-recurrente-modificado-pasajero", context);
    }

    @Async
    public void sendNuevaValoracionRecibida(String toEmail, String nombreValorado, String nombreAutor, int puntuacion, String comentario) {
        Context context = new Context();
        context.setVariable("nombreValorado", nombreValorado);
        context.setVariable("nombreAutor", nombreAutor);
        context.setVariable("puntuacion", puntuacion);
        context.setVariable("comentario", comentario);

        enviarCorreoHtml(toEmail, "CompiCar - ¡Has recibido una nueva valoración!", "nueva-valoracion", context);
    }

    @Async
    public void sendPagoLiberadoConductor(String toEmail, String nombreConductor, BigDecimal importeLiberado, String origen, String destino, String fechaHora) {
        Context context = new Context();
        context.setVariable("nombreConductor", nombreConductor);
        context.setVariable("importeLiberado", importeLiberado);
        context.setVariable("origen", origen);
        context.setVariable("destino", destino);
        context.setVariable("fechaHora", fechaHora);

        enviarCorreoHtml(toEmail, "CompiCar - ¡Fondos liberados por tu viaje!", "pago-liberado-conductor", context);
    }
}