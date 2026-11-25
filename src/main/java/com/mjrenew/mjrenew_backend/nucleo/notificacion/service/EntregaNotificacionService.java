package com.mjrenew.mjrenew_backend.nucleo.notificacion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class EntregaNotificacionService {
    private static final Logger log = LoggerFactory.getLogger(EntregaNotificacionService.class);
    private final ColaNotificacionService cola;
    private final CorreoNotificacionService correo;

    public EntregaNotificacionService(ColaNotificacionService cola, CorreoNotificacionService correo) {
        this.cola = cola; this.correo = correo;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void procesarLote() {
        for (int i = 0; i < 20; i++) {
            var siguiente = cola.reservar();
            if (siguiente.isEmpty()) return;
            boolean enviado = false;
            try {
                correo.enviar(siguiente.get());
                enviado = true;
            } catch (RuntimeException ex) {
                log.warn("No se pudo entregar un correo de notificación; queda pendiente para reintento.");
            }
            cola.concluir(siguiente.get(), enviado);
        }
    }
}
