package com.compicar.correo;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${compicar.mail.from:no-reply@compicar.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendCheckInCode(String toEmail, String nombreUsuario, String origen, String destino, String fechaHora, String codigoCheckIn) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("nombreUsuario", nombreUsuario);
            context.setVariable("origen", origen);
            context.setVariable("destino", destino);
            context.setVariable("fechaHora", fechaHora); // <--- Nueva variable
            context.setVariable("codigoCheckIn", codigoCheckIn);

            String htmlContent = templateEngine.process("checkin-code", context);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("CompiCar - Tu código de Check-in para el viaje a " + destino);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Error enviando correo de check-in: " + e.getMessage());
        }
    }
}