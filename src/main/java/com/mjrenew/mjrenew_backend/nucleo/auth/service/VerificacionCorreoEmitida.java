package com.mjrenew.mjrenew_backend.nucleo.auth.service;

// El token solo vive en memoria y en el correo; nunca registrar este evento en logs.
public record VerificacionCorreoEmitida(String destinatario, String token) {
    @Override public String toString() { return "VerificacionCorreoEmitida[datos reservados]"; }
}
