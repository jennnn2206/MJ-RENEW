package com.mjrenew.mjrenew_backend.nucleo.exception;

import org.springframework.security.core.AuthenticationException;

public class CorreoNoVerificadoException extends AuthenticationException {
    public CorreoNoVerificadoException() {
        super("Verifica tu correo antes de iniciar sesión. Puedes solicitar un nuevo enlace.");
    }
}
