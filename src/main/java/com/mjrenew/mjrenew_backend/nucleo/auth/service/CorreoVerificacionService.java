package com.mjrenew.mjrenew_backend.nucleo.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class CorreoVerificacionService {
    private final CorreoCuentaService correo;

    public CorreoVerificacionService(CorreoCuentaService correo) {
        this.correo = correo;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviar(VerificacionCorreoEmitida evento) {
        correo.enviarEnlace(evento.destinatario(), "Verifica tu correo — MJ Renew",
                "Confirma tu correo para acceder a MJ Renew:", "/verificar-correo", evento.token(), "24 horas");
    }
}
