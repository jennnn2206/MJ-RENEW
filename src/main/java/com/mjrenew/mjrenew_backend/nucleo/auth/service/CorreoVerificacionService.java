package com.mjrenew.mjrenew_backend.nucleo.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class CorreoVerificacionService {
    private static final Logger log = LoggerFactory.getLogger(CorreoVerificacionService.class);
    private final JavaMailSender correo;
    private final String remitente;
    private final String baseUrl;

    public CorreoVerificacionService(JavaMailSender correo, @Value("${mjrenew.correo.remitente}") String remitente,
                                     @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.correo = correo;
        this.remitente = remitente;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void enviar(VerificacionCorreoEmitida evento) {
        var mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(evento.destinatario());
        mensaje.setSubject("Verifica tu correo — MJ Renew");
        mensaje.setText("Confirma tu correo para acceder a MJ Renew:\n\n"
                + baseUrl + "/verificar-correo#token=" + evento.token()
                + "\n\nEste enlace vence en 24 horas y solo puede usarse una vez."
                + "\nSi no creaste esta cuenta, ignora este mensaje.");
        try {
            correo.send(mensaje);
        } catch (MailException ex) {
            // La cuenta ya está registrada: conservarla pendiente y permitir reenvío.
            // No registrar destinatario, enlace ni excepción SMTP (puede contener credenciales).
            log.warn("No se pudo enviar una verificación de correo. Revisar SMTP; el usuario puede solicitar reenvío.");
        }
    }
}
