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

@Service
public class CorreoCuentaService {
    private static final Logger log = LoggerFactory.getLogger(CorreoCuentaService.class);
    private final JavaMailSender correo;
    private final String remitente;
    private final String baseUrl;

    public CorreoCuentaService(JavaMailSender correo, @Value("${mjrenew.correo.remitente}") String remitente,
                              @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.correo = correo;
        this.remitente = remitente;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void enviarEnlace(String destinatario, String asunto, String introduccion,
                            String ruta, String token, String vigencia) {
        var mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(introduccion + "\n\n" + baseUrl + ruta + "#token=" + token
                + "\n\nEste enlace vence en " + vigencia + " y solo puede usarse una vez."
                + "\nSi no solicitaste esta acción, ignora este mensaje.");
        try {
            correo.send(mensaje);
        } catch (MailException ex) {
            // No registrar destinatario, token ni excepción SMTP, que puede incluir datos sensibles.
            log.warn("No se pudo enviar un correo de cuenta. Revisar SMTP; el usuario puede solicitar otro enlace.");
        }
    }
}
