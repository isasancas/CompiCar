package com.compicar.correo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@ExtendWith(MockitoExtension.class)
class CorreoServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @InjectMocks
    private CorreoService correoService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(correoService, "fromEmail", "no-reply@compicar.com");
        ReflectionTestUtils.setField(correoService, "feedbackToEmail", "support@compicar.com");

        mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Contenido</html>");
    }

    @Test
    void sendCheckInCode_exito() {
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        correoService.sendCheckInCode("user@compicar.com", "Juan", "Madrid", "Barcelona", "2026-06-10 10:00", "123456");

        verify(templateEngine).process(eq("checkin-code"), contextCaptor.capture());
        verify(mailSender, times(1)).send(mimeMessage);

        Context ctx = contextCaptor.getValue();
        assertEquals("Juan", ctx.getVariable("nombreUsuario"));
        assertEquals("Madrid", ctx.getVariable("origen"));
        assertEquals("Barcelona", ctx.getVariable("destino"));
        assertEquals("2026-06-10 10:00", ctx.getVariable("fechaHora"));
        assertEquals("123456", ctx.getVariable("codigoCheckIn"));
    }

    @Test
    void sendReservaCanceladaConductor_exito() {
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        correoService.sendReservaCanceladaConductor("driver@compicar.com", "Carlos", "Ana", "Sevilla", "Malaga", "2026-06-11 12:00", 2);

        verify(templateEngine).process(eq("reserva-cancelada-conductor"), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context ctx = contextCaptor.getValue();
        assertEquals("Carlos", ctx.getVariable("nombreConductor"));
        assertEquals("Ana", ctx.getVariable("nombrePasajero"));
        assertEquals("Sevilla", ctx.getVariable("origen"));
        assertEquals("Malaga", ctx.getVariable("destino"));
        assertEquals("2026-06-11 12:00", ctx.getVariable("fechaHora"));
        assertEquals(2, ctx.getVariable("plazas"));
    }

    @Test
    void sendReservaRechazadaPasajero_exito() {
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        correoService.sendReservaRechazadaPasajero("user@compicar.com", "Ana", "Carlos", "Valencia", "Alicante", "2026-06-12 09:00");

        verify(templateEngine).process(eq("reserva-rechazada-pasajero"), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context ctx = contextCaptor.getValue();
        assertEquals("Ana", ctx.getVariable("nombrePasajero"));
        assertEquals("Carlos", ctx.getVariable("nombreConductor"));
        assertEquals("Valencia", ctx.getVariable("origen"));
        assertEquals("Alicante", ctx.getVariable("destino"));
        assertEquals("2026-06-12 09:00", ctx.getVariable("fechaHora"));
    }

    @Test
    void sendReservaModificadaConductor_exito() {
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        correoService.sendReservaModificadaConductor("driver@compicar.com", "Carlos", "Ana", "Bilbao", "San Sebastián", "2026-06-13 15:00", 3);

        verify(templateEngine).process(eq("reserva-modificada-conductor"), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context ctx = contextCaptor.getValue();
        assertEquals("Carlos", ctx.getVariable("nombreConductor"));
        assertEquals("Ana", ctx.getVariable("nombrePasajero"));
        assertEquals("Bilbao", ctx.getVariable("origen"));
        assertEquals("San Sebastián", ctx.getVariable("destino"));
        assertEquals("2026-06-13 15:00", ctx.getVariable("fechaHora"));
        assertEquals(3, ctx.getVariable("plazas"));
    }

    @Test
    void sendViajeRecurrenteCanceladoPasajero_exito() {
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        correoService.sendViajeRecurrenteCanceladoPasajero("user@compicar.com", "Ana", "Carlos", "Granada", "Córdoba", "2026-06-14 08:00", 1);

        verify(templateEngine).process(eq("viaje-recurrente-cancelado-pasajero"), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context ctx = contextCaptor.getValue();
        assertEquals("Ana", ctx.getVariable("nombrePasajero"));
        assertEquals("Carlos", ctx.getVariable("nombreConductor"));
        assertEquals("Granada", ctx.getVariable("origen"));
        assertEquals("Córdoba", ctx.getVariable("destino"));
        assertEquals("2026-06-14 08:00", ctx.getVariable("fechaHora"));
        assertEquals(1, ctx.getVariable("plazas"));
    }

    @Test
    void sendReservaLoteConductor_exito() {
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        List<String> fechas = List.of("2026-06-15", "2026-06-16");
        BigDecimal total = new BigDecimal("30.00");

        correoService.sendReservaLoteConductor("driver@compicar.com", "Carlos", "Ana", "Madrid", "Toledo", fechas, 2, total);

        verify(templateEngine).process(eq("reserva-lote-conductor"), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context ctx = contextCaptor.getValue();
        assertEquals("Carlos", ctx.getVariable("nombreConductor"));
        assertEquals("Ana", ctx.getVariable("nombrePasajero"));
        assertEquals("Madrid", ctx.getVariable("origen"));
        assertEquals("Toledo", ctx.getVariable("destino"));
        assertEquals(fechas, ctx.getVariable("fechas"));
        assertEquals(2, ctx.getVariable("plazas"));
        assertEquals(total, ctx.getVariable("totalAcumulado"));
    }

    @Test
    void sendViajeModificadoPasajero_exito() {
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        correoService.sendViajeModificadoPasajero("user@compicar.com", "Ana", "Carlos", "Zaragoza", "Huesca", "2026-06-17 11:00", new BigDecimal("15.50"));

        verify(templateEngine).process(eq("viaje-modificado-pasajero"), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context ctx = contextCaptor.getValue();
        assertEquals("Ana", ctx.getVariable("nombrePasajero"));
        assertEquals("Carlos", ctx.getVariable("nombreConductor"));
        assertEquals("Zaragoza", ctx.getVariable("origen"));
        assertEquals("Huesca", ctx.getVariable("destino"));
        assertEquals("2026-06-17 11:00", ctx.getVariable("fechaHora"));
        assertEquals(new BigDecimal("15.50"), ctx.getVariable("precio"));
    }

    @Test
    void sendViajeFinalizadoPasajero_exito() {
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        correoService.sendViajeFinalizadoPasajero("user@compicar.com", "Ana", "Carlos", "Vigo", "A Coruña", "2026-06-18 18:00");

        verify(templateEngine).process(eq("viaje-finalizado-pasajero"), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context ctx = contextCaptor.getValue();
        assertEquals("Ana", ctx.getVariable("nombrePasajero"));
        assertEquals("Carlos", ctx.getVariable("nombreConductor"));
        assertEquals("Vigo", ctx.getVariable("origen"));
        assertEquals("A Coruña", ctx.getVariable("destino"));
        assertEquals("2026-06-18 18:00", ctx.getVariable("fechaHora"));
    }

    @Test
    void sendViajeRecurrenteFinalizadoPasajero_exito() {
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        correoService.sendViajeRecurrenteFinalizadoPasajero("user@compicar.com", "Ana", "Carlos", "Murcia", "Cartagena", "2026-06-19 19:00");

        verify(templateEngine).process(eq("viaje-recurrente-finalizado-pasajero"), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context ctx = contextCaptor.getValue();
        assertEquals("Ana", ctx.getVariable("nombrePasajero"));
        assertEquals("Carlos", ctx.getVariable("nombreConductor"));
        assertEquals("Murcia", ctx.getVariable("origen"));
        assertEquals("Cartagena", ctx.getVariable("destino"));
        assertEquals("2026-06-19 19:00", ctx.getVariable("fechaHora"));
    }

    @Test
    void sendIncomparecenciaConductorRecurrentePasajero_exito() {
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        correoService.sendIncomparecenciaConductorRecurrentePasajero("user@compicar.com", "Ana", "Carlos", "Salamanca", "Valladolid", "2026-06-20 07:30");

        verify(templateEngine).process(eq("incomparecencia-conductor-recurrente-pasajero"), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context ctx = contextCaptor.getValue();
        assertEquals("Ana", ctx.getVariable("nombrePasajero"));
        assertEquals("Carlos", ctx.getVariable("nombreConductor"));
        assertEquals("Salamanca", ctx.getVariable("origen"));
        assertEquals("Valladolid", ctx.getVariable("destino"));
        assertEquals("2026-06-20 07:30", ctx.getVariable("fechaHora"));
    }

    @Test
    void sendViajeRecurrenteModificadoPasajero_exito() {
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        correoService.sendViajeRecurrenteModificadoPasajero("user@compicar.com", "Ana", "Carlos", "León", "Oviedo", "2026-06-21 16:00", new BigDecimal("12.00"));

        verify(templateEngine).process(eq("viaje-recurrente-modificado-pasajero"), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context ctx = contextCaptor.getValue();
        assertEquals("Ana", ctx.getVariable("nombrePasajero"));
        assertEquals("Carlos", ctx.getVariable("nombreConductor"));
        assertEquals("León", ctx.getVariable("origen"));
        assertEquals("Oviedo", ctx.getVariable("destino"));
        assertEquals("2026-06-21 16:00", ctx.getVariable("fechaHora"));
        assertEquals(new BigDecimal("12.00"), ctx.getVariable("precio"));
    }

    @Test
    void sendNuevaValoracionRecibida_exito() {
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        correoService.sendNuevaValoracionRecibida("user@compicar.com", "Carlos", "Ana", 5, "¡Excelente conductor!");

        verify(templateEngine).process(eq("nueva-valoracion"), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context ctx = contextCaptor.getValue();
        assertEquals("Carlos", ctx.getVariable("nombreValorado"));
        assertEquals("Ana", ctx.getVariable("nombreAutor"));
        assertEquals(5, ctx.getVariable("puntuacion"));
        assertEquals("¡Excelente conductor!", ctx.getVariable("comentario"));
    }

    @Test
    void sendSugerencia_exito_enviaAFeedbackToEmail() {
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        correoService.sendSugerencia("usuario@compicar.com", "Pedro", "Tengo una propuesta de mejora");

        verify(templateEngine).process(eq("sugerencia"), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context ctx = contextCaptor.getValue();
        assertEquals("usuario@compicar.com", ctx.getVariable("emailUsuario"));
        assertEquals("Pedro", ctx.getVariable("nombreUsuario"));
        assertEquals("Tengo una propuesta de mejora", ctx.getVariable("mensaje"));
    }

    @Test
    void sendPagoLiberadoConductor_exito() {
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        correoService.sendPagoLiberadoConductor("driver@compicar.com", "Carlos", new BigDecimal("25.00"), "Madrid", "Segovia", "2026-06-22 14:00");

        verify(templateEngine).process(eq("pago-liberado-conductor"), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context ctx = contextCaptor.getValue();
        assertEquals("Carlos", ctx.getVariable("nombreConductor"));
        assertEquals(new BigDecimal("25.00"), ctx.getVariable("importeLiberado"));
        assertEquals("Madrid", ctx.getVariable("origen"));
        assertEquals("Segovia", ctx.getVariable("destino"));
        assertEquals("2026-06-22 14:00", ctx.getVariable("fechaHora"));
    }

    @Test
    void sendSolicitudNuevaParadaConductor_exito() {
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        correoService.sendSolicitudNuevaParadaConductor("driver@compicar.com", "Carlos", "Ana", "Madrid", "Valencia", "Getafe", "2026-06-23 10:00");

        verify(templateEngine).process(eq("solicitud-nueva-parada-conductor"), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context ctx = contextCaptor.getValue();
        assertEquals("Carlos", ctx.getVariable("nombreConductor"));
        assertEquals("Ana", ctx.getVariable("nombrePasajero"));
        assertEquals("Madrid", ctx.getVariable("origen"));
        assertEquals("Valencia", ctx.getVariable("destino"));
        assertEquals("Getafe", ctx.getVariable("ubicacionParada"));
        assertEquals("2026-06-23 10:00", ctx.getVariable("fechaHora"));
    }

    @Test
    void sendSolicitudParadaAceptadaPasajero_exito() {
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        correoService.sendSolicitudParadaAceptadaPasajero("user@compicar.com", "Ana", "Carlos", "Madrid", "Valencia", "Getafe", "2026-06-23 10:00");

        verify(templateEngine).process(eq("solicitud-parada-aceptada-pasajero"), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context ctx = contextCaptor.getValue();
        assertEquals("Ana", ctx.getVariable("nombrePasajero"));
        assertEquals("Carlos", ctx.getVariable("nombreConductor"));
        assertEquals("Madrid", ctx.getVariable("origen"));
        assertEquals("Valencia", ctx.getVariable("destino"));
        assertEquals("Getafe", ctx.getVariable("ubicacionParada"));
        assertEquals("2026-06-23 10:00", ctx.getVariable("fechaHora"));
    }

    @Test
    void sendSolicitudParadaRechazadaPasajero_exito() {
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        correoService.sendSolicitudParadaRechazadaPasajero("user@compicar.com", "Ana", "Carlos", "Madrid", "Valencia", "Getafe", "2026-06-23 10:00");

        verify(templateEngine).process(eq("solicitud-parada-rechazada-pasajero"), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context ctx = contextCaptor.getValue();
        assertEquals("Ana", ctx.getVariable("nombrePasajero"));
        assertEquals("Carlos", ctx.getVariable("nombreConductor"));
        assertEquals("Madrid", ctx.getVariable("origen"));
        assertEquals("Valencia", ctx.getVariable("destino"));
        assertEquals("Getafe", ctx.getVariable("ubicacionParada"));
        assertEquals("2026-06-23 10:00", ctx.getVariable("fechaHora"));
    }

    @Test
    void enviarCorreoHtml_messagingException_capturaSintomatisandoSinLanzarExcepcion() throws Exception {
        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);
        doThrow(new MessagingException("Error al establecer el destinatario"))
                .when(mockMimeMessage).setRecipient(any(Message.RecipientType.class), any());

        assertDoesNotThrow(() ->
            correoService.sendCheckInCode("user@compicar.com", "Juan", "Madrid", "Barcelona", "2026-06-10", "123456")
        );
    }
}
