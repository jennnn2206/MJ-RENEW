package com.mjrenew.mjrenew_backend.propietario.dto;

import java.time.OffsetDateTime;

public record CuentaStripeEstadoResponse(
        boolean cuentaCreada,
        boolean detallesEnviados,
        boolean transferenciasHabilitadas,
        boolean payoutsHabilitados,
        String estado,
        OffsetDateTime actualizadaEn
) {
}
