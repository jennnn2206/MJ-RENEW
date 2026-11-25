package com.mjrenew.mjrenew_backend.nucleo.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class CorreoRecuperacionContrasenaService {
    private final CorreoCuentaService correo;

    public CorreoRecuperacionContrasenaService(CorreoCuentaService correo) { this.correo = correo; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviar(RecuperacionContrasenaEmitida evento) {
        correo.enviarEnlace(evento.destinatario(), "Recupera tu contraseña — MJ Renew",
                "Solicitaste cambiar tu contraseña de MJ Renew. Abre este enlace para elegir una nueva:",
                "/restablecer-contrasena", evento.token(), evento.vigenciaMinutos() + " minutos");
    }
}
