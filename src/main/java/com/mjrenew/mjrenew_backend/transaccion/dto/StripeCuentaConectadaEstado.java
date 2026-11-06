package com.mjrenew.mjrenew_backend.transaccion.dto;

/** Estado consultado directamente en Stripe, no enviado por el navegador. */
public record StripeCuentaConectadaEstado(
        boolean detallesEnviados,
        boolean transferenciasHabilitadas,
        boolean payoutsHabilitados
) {
}
