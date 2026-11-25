package com.mjrenew.mjrenew_backend.nucleo.notificacion.service;

import com.mjrenew.mjrenew_backend.nucleo.notificacion.dto.EnvioNotificacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class CorreoNotificacionService {
    private final JavaMailSender correo;
    private final String remitente;
    private final String baseUrl;

    public CorreoNotificacionService(JavaMailSender correo, @Value("${mjrenew.correo.remitente}") String remitente,
                                     @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.correo = correo; this.remitente = remitente; this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void enviar(EnvioNotificacion envio) {
        var mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente); mensaje.setTo(envio.correo()); mensaje.setSubject(envio.titulo() + " — MJ Renew");
        mensaje.setText(envio.mensaje() + "\n\nConsulta tu panel: " + baseUrl + envio.rutaPanel());
        // El coordinador necesita conocer el fallo para conservar el pendiente.
        correo.send(mensaje);
    }
}
