package com.mjrenew.mjrenew_backend.nucleo.auth.service;

public record RecuperacionContrasenaEmitida(String destinatario, String token, int vigenciaMinutos) {
    @Override public String toString() { return "RecuperacionContrasenaEmitida[datos reservados]"; }
}
