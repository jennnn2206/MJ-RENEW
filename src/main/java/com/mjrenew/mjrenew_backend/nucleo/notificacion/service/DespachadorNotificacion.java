package com.mjrenew.mjrenew_backend.nucleo.notificacion.service;

import com.mjrenew.mjrenew_backend.nucleo.notificacion.NotificacionRestauracionEmitida;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class DespachadorNotificacion {
    private final EntregaNotificacionService entregas;
    private final boolean correoHabilitado;

    public DespachadorNotificacion(EntregaNotificacionService entregas,
            @Value("${mjrenew.notificaciones.correo-habilitado:false}") boolean correoHabilitado) {
        this.entregas = entregas; this.correoHabilitado = correoHabilitado;
    }

    @Async
    @TransactionalEventListener
    public void alConfirmar(NotificacionRestauracionEmitida evento) {
        if (correoHabilitado) entregas.procesarLote();
    }

    @Scheduled(initialDelayString = "${mjrenew.notificaciones.intervalo-ms:60000}",
            fixedDelayString = "${mjrenew.notificaciones.intervalo-ms:60000}")
    public void reintentarPendientes() {
        if (correoHabilitado) entregas.procesarLote();
    }
}
